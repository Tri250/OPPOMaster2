package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * LUT 导出器
 *
 * 将当前 RenderParameters 效果导出为标准 .cube 3D LUT 文件
 * 可在其他软件（DaVinci Resolve, Premiere, Photoshop）中使用
 */
class LUTExporter {

    companion object {
        private const val TAG = "LUTExporter"
        const val DEFAULT_LUT_SIZE = 33  // 33x33x33
    }

    /**
     * 将 RenderParameters 效果导出为 .cube 文件
     *
     * 原理：生成一张 identity 颜色表图像（每个像素 = LUT 输入色），
     * 通过 GPU/CPU 渲染管线处理后读取输出色，即可得到完整的 LUT 映射。
     */
    suspend fun exportToCube(
        context: Context,
        params: RenderParameters,
        outputFile: File,
        lutSize: Int = DEFAULT_LUT_SIZE,
        gpuRenderManager: GPURenderManager? = null
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            // 1. 生成 identity 颜色表
            val identityChart = generateIdentityChart(lutSize)

            // 2. 通过渲染管线处理
            val rendered = renderIdentityChart(context, identityChart, params, gpuRenderManager)

            if (rendered == null) {
                Log.e(TAG, "Failed to render identity chart")
                identityChart.recycle()
                return@withContext false
            }

            // 3. 从渲染结果提取 LUT 数据并写入 .cube 文件
            val success = writeCubeFile(rendered, lutSize, outputFile)

            // 清理
            if (rendered !== identityChart) {
                rendered.recycle()
            }
            identityChart.recycle()

            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export LUT", e)
            return@withContext false
        }
    }

    /**
     * 通过 GPU/CPU 渲染管线处理 identity 颜色表
     */
    private suspend fun renderIdentityChart(
        context: Context,
        identityChart: Bitmap,
        params: RenderParameters,
        gpuRenderManager: GPURenderManager?
    ): Bitmap? {
        // 优先使用 GPU 渲染
        if (gpuRenderManager != null && gpuRenderManager.isInitialized.value && gpuRenderManager.isGpuAvailable.value) {
            val result = gpuRenderManager.renderSync(
                identityChart,
                params,
                RenderQuality.HIGH
            )
            when (result) {
                is RenderResult.Success -> {
                    if (result.outputBitmap != null) return result.outputBitmap
                    // outputBitmap 为 null 时回退到 CPU 从 textureId 读取
                }
                is RenderResult.FallbackToCPU -> return result.outputBitmap
                is RenderResult.Error -> {
                    Log.w(TAG, "GPU render failed: ${result.message}, falling back to CPU")
                }
            }
        }

        // CPU 回退：使用 CPURenderer
        return try {
            val cpuRenderer = CPURenderer()
            cpuRenderer.render(identityChart, params)
        } catch (e: Exception) {
            Log.e(TAG, "CPU render also failed", e)
            null
        }
    }

    /**
     * 生成 Identity 颜色表 Bitmap
     * 宽度 = lutSize * lutSize，高度 = lutSize
     * 每个像素 = (R/size, G/size, B/size) 的对应色
     */
    fun generateIdentityChart(lutSize: Int): Bitmap {
        val width = lutSize * lutSize
        val height = lutSize
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)

        for (b in 0 until lutSize) {
            for (g in 0 until lutSize) {
                for (r in 0 until lutSize) {
                    val x = g * lutSize + r
                    val y = b
                    val rf = r.toFloat() / (lutSize - 1)
                    val gf = g.toFloat() / (lutSize - 1)
                    val bf = b.toFloat() / (lutSize - 1)
                    val ri = (rf * 255f + 0.5f).toInt().coerceIn(0, 255)
                    val gi = (gf * 255f + 0.5f).toInt().coerceIn(0, 255)
                    val bi = (bf * 255f + 0.5f).toInt().coerceIn(0, 255)
                    val alpha = 0xFF
                    pixels[y * width + x] = (alpha shl 24) or (ri shl 16) or (gi shl 8) or bi
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * 从渲染后的 Bitmap 提取 LUT 数据并写入 .cube 文件
     *
     * .cube 格式规范：
     * - R 变化最快（内层循环），然后 G，然后 B（外层循环）
     * - 每行一个 RGB 三元组，值域 0.0~1.0
     */
    fun writeCubeFile(rendered: Bitmap, lutSize: Int, outputFile: File): Boolean {
        val width = lutSize * lutSize
        val height = lutSize

        // 验证 rendered bitmap 尺寸
        if (rendered.width < width || rendered.height < height) {
            Log.e(TAG, "Rendered bitmap too small: ${rendered.width}x${rendered.height}, need ${width}x${height}")
            return false
        }

        // 一次性读取全部像素，避免逐像素 getPixel 调用
        val pixels = IntArray(rendered.width * rendered.height)
        rendered.getPixels(pixels, 0, rendered.width, 0, 0, rendered.width, rendered.height)

        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(outputFile), "UTF-8")).use { writer ->
                // 写入 .cube 文件头
                writer.write("TITLE \"OMaster Export\"\n")
                writer.write("LUT_3D_SIZE $lutSize\n")
                writer.write("DOMAIN_MIN 0.0 0.0 0.0\n")
                writer.write("DOMAIN_MAX 1.0 1.0 1.0\n")
                writer.write("\n")

                // 写入 LUT 数据
                // .cube 格式遍历顺序：R 变化最快，然后 G，然后 B
                for (b in 0 until lutSize) {
                    for (g in 0 until lutSize) {
                        for (r in 0 until lutSize) {
                            val x = g * lutSize + r
                            val y = b
                            val pixel = pixels[y * rendered.width + x]

                            val ri = (pixel shr 16) and 0xFF
                            val gi = (pixel shr 8) and 0xFF
                            val bi = pixel and 0xFF

                            val rf = ri.toFloat() / 255f
                            val gf = gi.toFloat() / 255f
                            val bf = bi.toFloat() / 255f

                            writer.write(String.format("%.6f %.6f %.6f\n", rf, gf, bf))
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write .cube file", e)
            return false
        }
    }
}
