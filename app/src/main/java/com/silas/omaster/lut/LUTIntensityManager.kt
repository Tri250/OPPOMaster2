package com.silas.omaster.lut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.silas.omaster.data.model.MasterLUT
import com.silas.omaster.mask.AdjustmentMask
import com.silas.omaster.mask.MaskGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * LUT 精细调节管理器
 *
 * P2 13. 3D LUT 精细调节
 * - LUT 强度滑块 (0% - 100%)：控制 LUT 应用程度
 * - LUT 混合：支持叠加多个 LUT，各设权重
 * - LUT 局部应用：结合蒙版系统，LUT 只作用于天空/人物等区域
 * - LUT 导出：当前参数调节结果导出为 .cube 格式 LUT 文件
 *
 * 与现有 LUTRepository / MasterLUT 集成
 */
class LUTIntensityManager(private val context: Context) {

    /**
     * LUT 应用配置
     * @property lut LUT 数据
     * @property intensity 应用强度 [0, 1]
     * @property weight 混合权重（多 LUT 叠加时）
     * @property mask 关联蒙版（局部应用）
     */
    data class LUTConfig(
        val lut: MasterLUT,
        val intensity: Float = 1.0f,
        val weight: Float = 1.0f,
        val mask: AdjustmentMask? = null
    )

    /**
     * 多 LUT 混合配置
     */
    data class BlendedLUTConfig(
        val configs: List<LUTConfig> = emptyList(),
        val globalIntensity: Float = 1.0f
    )

    /**
     * 应用单个 LUT 到图像
     * @param source 原图
     * @param config LUT 配置
     * @return 应用 LUT 后的图像
     */
    suspend fun applyLUT(
        source: Bitmap,
        config: LUTConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        val lut = config.lut
        val intensity = config.intensity.coerceIn(0f, 1f)

        if (intensity == 0f) return@withContext source

        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        // 蒙版（如果有）
        val maskBitmap = config.mask?.let {
            MaskGenerator.generate(width, height, it)
        }
        val maskPixels = if (maskBitmap != null) {
            val mp = IntArray(width * height)
            maskBitmap.getPixels(mp, 0, width, 0, 0, width, height)
            mp
        } else null

        // LUT 3D 表（模拟：使用内置色彩映射）
        for (i in srcPixels.indices) {
            val color = srcPixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val a = Color.alpha(color)

            // 应用 LUT 映射（简化版：基于 LUT 名称的预设映射）
            val (nr, ng, nb) = applyLUTMapping(r, g, b, lut)

            // 强度混合
            val fr = (r * (1f - intensity) + nr * intensity).toInt().coerceIn(0, 255)
            val fg = (g * (1f - intensity) + ng * intensity).toInt().coerceIn(0, 255)
            val fb = (b * (1f - intensity) + nb * intensity).toInt().coerceIn(0, 255)

            // 蒙版应用
            val finalR: Int
            val finalG: Int
            val finalB: Int
            if (maskPixels != null) {
                val maskStrength = Color.alpha(maskPixels[i]) / 255f
                finalR = (r * (1f - maskStrength) + fr * maskStrength).toInt()
                finalG = (g * (1f - maskStrength) + fg * maskStrength).toInt()
                finalB = (b * (1f - maskStrength) + fb * maskStrength).toInt()
            } else {
                finalR = fr
                finalG = fg
                finalB = fb
            }

            outPixels[i] = Color.argb(a, finalR, finalG, finalB)
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * 应用多个 LUT 混合
     * @param source 原图
     * @param config 混合配置
     * @return 混合后的图像
     */
    suspend fun applyBlendedLUT(
        source: Bitmap,
        config: BlendedLUTConfig
    ): Bitmap = withContext(Dispatchers.Default) {
        if (config.configs.isEmpty()) return@withContext source

        val width = source.width
        val height = source.height
        var current = source

        // 归一化权重
        val totalWeight = config.configs.sumOf { it.weight.toDouble() }.toFloat()
        val normalizedConfigs = config.configs.map { it.copy(weight = it.weight / totalWeight) }

        // 逐个应用 LUT，按权重混合
        for (lutConfig in normalizedConfigs) {
            val lutApplied = applyLUT(current, lutConfig.copy(intensity = 1f))

            // 按权重混合
            current = blendBitmaps(current, lutApplied, lutConfig.weight)
        }

        // 应用全局强度
        if (config.globalIntensity < 1f) {
            current = blendBitmaps(source, current, config.globalIntensity)
        }

        current
    }

    /**
     * 混合两张图像
     */
    private fun blendBitmaps(base: Bitmap, overlay: Bitmap, alpha: Float): Bitmap {
        val width = base.width
        val height = base.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val basePixels = IntArray(width * height)
        val overlayPixels = IntArray(width * height)
        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(width * height)
        for (i in basePixels.indices) {
            val bc = basePixels[i]
            val oc = overlayPixels[i]
            val r = (Color.red(bc) * (1f - alpha) + Color.red(oc) * alpha).toInt()
            val g = (Color.green(bc) * (1f - alpha) + Color.green(oc) * alpha).toInt()
            val b = (Color.blue(bc) * (1f - alpha) + Color.blue(oc) * alpha).toInt()
            outPixels[i] = Color.argb(Color.alpha(bc), r, g, b)
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * LUT 映射（基于 LUT 类型的预设映射）
     * 实际项目会使用 3D LUT 表查表
     */
    private fun applyLUTMapping(r: Int, g: Int, b: Int, lut: MasterLUT): Triple<Int, Int, Int> {
        // 根据 LUT 名称/类型应用不同的色彩映射
        val name = lut.name.lowercase()

        return when {
            name.contains("film") || name.contains("胶片") -> {
                // 胶片模拟：增加对比度，轻微偏暖
                val nr = (r * 1.1f + 10).toInt().coerceIn(0, 255)
                val ng = (g * 1.05f + 5).toInt().coerceIn(0, 255)
                val nb = (b * 0.95f).toInt().coerceIn(0, 255)
                Triple(nr, ng, nb)
            }
            name.contains("vintage") || name.contains("复古") -> {
                // 复古：降低饱和度，偏黄
                val gray = (r * 0.299f + g * 0.587f + b * 0.114f).toInt()
                val nr = (r * 0.7f + gray * 0.3f + 20).toInt().coerceIn(0, 255)
                val ng = (g * 0.7f + gray * 0.3f + 10).toInt().coerceIn(0, 255)
                val nb = (b * 0.7f + gray * 0.3f).toInt().coerceIn(0, 255)
                Triple(nr, ng, nb)
            }
            name.contains("cold") || name.contains("冷") -> {
                // 冷色调：偏蓝
                val nr = (r * 0.9f).toInt().coerceIn(0, 255)
                val ng = g
                val nb = (b * 1.1f + 10).toInt().coerceIn(0, 255)
                Triple(nr, ng, nb)
            }
            name.contains("warm") || name.contains("暖") -> {
                // 暖色调：偏橙
                val nr = (r * 1.1f + 10).toInt().coerceIn(0, 255)
                val ng = (g * 1.05f + 5).toInt().coerceIn(0, 255)
                val nb = (b * 0.9f).toInt().coerceIn(0, 255)
                Triple(nr, ng, nb)
            }
            name.contains("hasselblad") || name.contains("哈苏") -> {
                // 哈苏风格：高对比度，自然饱和度
                val nr = (r * 1.15f - 10).toInt().coerceIn(0, 255)
                val ng = (g * 1.1f - 5).toInt().coerceIn(0, 255)
                val nb = (b * 1.05f).toInt().coerceIn(0, 255)
                Triple(nr, ng, nb)
            }
            else -> {
                // 默认：轻微增强
                Triple(r, g, b)
            }
        }
    }

    /**
     * 导出当前参数为 .cube LUT 文件
     * .cube 是 Adobe 的标准 3D LUT 格式
     *
     * @param params 当前参数
     * @param outputFile 输出文件
     * @param lutSize LUT 尺寸（通常 32 或 64）
     */
    fun exportToCube(
        params: LUTExportParams,
        outputFile: File,
        lutSize: Int = 32
    ): Boolean {
        return try {
            FileOutputStream(outputFile).use { fos ->
                // 写入头部
                fos.write("TITLE \"${params.title}\"\n".toByteArray())
                fos.write("# Created by OMaster\n".toByteArray())
                fos.write("# Version 2.0\n".toByteArray())
                fos.write("\n".toByteArray())
                fos.write("LUT_3D_SIZE $lutSize\n".toByteArray())
                fos.write("\n".toByteArray())

                // 写入数据域
                for (b in 0 until lutSize) {
                    for (g in 0 until lutSize) {
                        for (r in 0 until lutSize) {
                            // 归一化输入
                            val ri = r.toFloat() / (lutSize - 1)
                            val gi = g.toFloat() / (lutSize - 1)
                            val bi = b.toFloat() / (lutSize - 1)

                            // 应用参数变换
                            val (ro, go, bo) = applyParamsToColor(ri, gi, bi, params)

                            // 写入输出
                            fos.write("$ro $go $bo\n".toByteArray())
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 应用参数到颜色
     */
    private fun applyParamsToColor(
        r: Float, g: Float, b: Float,
        params: LUTExportParams
    ): Triple<String, String, String> {
        var ro = r
        var go = g
        var bo = b

        // 对比度
        val c = 1f + params.contrast / 100f
        ro = (ro - 0.5f) * c + 0.5f
        go = (go - 0.5f) * c + 0.5f
        bo = (bo - 0.5f) * c + 0.5f

        // 饱和度
        val gray = ro * 0.299f + go * 0.587f + bo * 0.114f
        val s = 1f + params.saturation / 100f
        ro = gray + (ro - gray) * s
        go = gray + (go - gray) * s
        bo = gray + (bo - gray) * s

        // 亮度
        ro += params.brightness / 100f
        go += params.brightness / 100f
        bo += params.brightness / 100f

        // 色温
        if (params.colorTemp != 5500) {
            val tempFactor = (params.colorTemp - 5500) / 10000f
            ro += tempFactor * 0.1f
            bo -= tempFactor * 0.1f
        }

        // 裁剪
        ro = ro.coerceIn(0f, 1f)
        go = go.coerceIn(0f, 1f)
        bo = bo.coerceIn(0f, 1f)

        return Triple(
            String.format("%.6f", ro),
            String.format("%.6f", go),
            String.format("%.6f", bo)
        )
    }

    companion object {
        @Volatile
        private var instance: LUTIntensityManager? = null

        fun getInstance(context: Context): LUTIntensityManager {
            return instance ?: synchronized(this) {
                instance ?: LUTIntensityManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * LUT 导出参数
 */
data class LUTExportParams(
    val title: String = "OMaster Custom LUT",
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val brightness: Float = 0f,
    val colorTemp: Int = 5500,
    val vibrance: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f
)
