package com.silas.omaster.curve

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 色调曲线管理器
 * 专业调色必备
 * 
 * 支持：
 * - RGB 复合曲线
 * - R/G/B 独立通道曲线
 * - 控制点拖拽
 * - 曲线预设
 */
class ToneCurveManager private constructor(context: Context) {

    // 曲线通道
    enum class CurveChannel(val displayName: String, val color: Int) {
        RGB("RGB", Color.WHITE),
        RED("红色", Color.RED),
        GREEN("绿色", Color.GREEN),
        BLUE("蓝色", Color.BLUE)
    }

    // 控制点
    data class ControlPoint(
        val x: Float,  // 0.0 ~ 1.0
        val y: Float   // 0.0 ~ 1.0
    ) {
        fun toByte(): Int = (y * 255).roundToInt().coerceIn(0, 255)
    }

    // 曲线数据（包含控制点）
    data class Curve(
        val channel: CurveChannel,
        val controlPoints: List<ControlPoint> = listOf(
            ControlPoint(0f, 0f),    // 黑点
            ControlPoint(1f, 1f)     // 白点
        )
    ) {
        // 生成 256 级查找表
        fun generateLUT(): IntArray {
            val lut = IntArray(256)

            if (controlPoints.size < 2) {
                for (i in 0..255) lut[i] = i
                return lut
            }

            // 排序控制点
            val sortedPoints = controlPoints.sortedBy { it.x }

            // 分段线性插值
            for (i in 0..255) {
                val x = i / 255f
                var y = x

                for (j in 0 until sortedPoints.size - 1) {
                    val p1 = sortedPoints[j]
                    val p2 = sortedPoints[j + 1]

                    if (x in p1.x..p2.x) {
                        val t = if (p2.x == p1.x) 0f else (x - p1.x) / (p2.x - p1.x)
                        y = p1.y + t * (p2.y - p1.y)
                        break
                    }
                }

                lut[i] = (y * 255).roundToInt().coerceIn(0, 255)
            }

            return lut
        }
    }

    // 各通道曲线
    private val _curves = MutableStateFlow<Map<CurveChannel, Curve>>(
        CurveChannel.entries.associateWith { Curve(it) }
    )
    val curves: StateFlow<Map<CurveChannel, Curve>> = _curves.asStateFlow()

    // 当前选中的通道
    private val _selectedChannel = MutableStateFlow(CurveChannel.RGB)
    val selectedChannel: StateFlow<CurveChannel> = _selectedChannel.asStateFlow()

    // 曲线预设
    val curvePresets = listOf(
        CurvePreset(
            id = "linear",
            name = "线性",
            description = "无调整",
            curves = CurveChannel.entries.associateWith { Curve(it) }
        ),
        CurvePreset(
            id = "high_contrast",
            name = "高对比度",
            description = "S型曲线增强对比",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.25f, 0.15f),
                    ControlPoint(0.75f, 0.85f),
                    ControlPoint(1f, 1f)
                ))
            )
        ),
        CurvePreset(
            id = "low_contrast",
            name = "低对比度",
            description = "反S型曲线降低对比",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.25f, 0.35f),
                    ControlPoint(0.75f, 0.65f),
                    ControlPoint(1f, 1f)
                ))
            )
        ),
        CurvePreset(
            id = "brighten",
            name = "提亮",
            description = "整体提亮",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 0.1f),
                    ControlPoint(1f, 1f)
                ))
            )
        ),
        CurvePreset(
            id = "darken",
            name = "压暗",
            description = "整体压暗",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(1f, 0.9f)
                ))
            )
        ),
        CurvePreset(
            id = "negative",
            name = "反相",
            description = "色彩反转",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 1f),
                    ControlPoint(1f, 0f)
                ))
            )
        ),
        CurvePreset(
            id = "solarize",
            name = "中途曝光",
            description = "经典中途曝光效果",
            curves = mapOf(
                CurveChannel.RGB to Curve(CurveChannel.RGB, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.5f, 1f),
                    ControlPoint(1f, 0f)
                ))
            )
        ),
        CurvePreset(
            id = "warm_shadows",
            name = "暖调阴影",
            description = "阴影偏暖",
            curves = mapOf(
                CurveChannel.RED to Curve(CurveChannel.RED, listOf(
                    ControlPoint(0f, 0.05f),
                    ControlPoint(0.3f, 0.35f),
                    ControlPoint(1f, 1f)
                )),
                CurveChannel.BLUE to Curve(CurveChannel.BLUE, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.3f, 0.25f),
                    ControlPoint(1f, 1f)
                ))
            )
        ),
        CurvePreset(
            id = "cool_highlights",
            name = "冷调高光",
            description = "高光偏冷",
            curves = mapOf(
                CurveChannel.RED to Curve(CurveChannel.RED, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.7f, 0.7f),
                    ControlPoint(1f, 0.95f)
                )),
                CurveChannel.BLUE to Curve(CurveChannel.BLUE, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.7f, 0.7f),
                    ControlPoint(1f, 1.05f)
                ))
            )
        ),
        CurvePreset(
            id = "cross_process",
            name = "正片负冲",
            description = "胶片正片负冲效果",
            curves = mapOf(
                CurveChannel.RED to Curve(CurveChannel.RED, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.2f, 0.1f),
                    ControlPoint(0.5f, 0.45f),
                    ControlPoint(0.8f, 0.85f),
                    ControlPoint(1f, 1f)
                )),
                CurveChannel.GREEN to Curve(CurveChannel.GREEN, listOf(
                    ControlPoint(0f, 0f),
                    ControlPoint(0.2f, 0.15f),
                    ControlPoint(0.5f, 0.5f),
                    ControlPoint(0.8f, 0.8f),
                    ControlPoint(1f, 1f)
                )),
                CurveChannel.BLUE to Curve(CurveChannel.BLUE, listOf(
                    ControlPoint(0f, 0.05f),
                    ControlPoint(0.2f, 0.25f),
                    ControlPoint(0.5f, 0.55f),
                    ControlPoint(0.8f, 0.75f),
                    ControlPoint(1f, 0.95f)
                ))
            )
        )
    )

    // LUT 缓存
    private var lutCache: Map<CurveChannel, IntArray>? = null

    /**
     * 选择通道
     */
    fun selectChannel(channel: CurveChannel) {
        _selectedChannel.value = channel
    }

    /**
     * 添加控制点
     */
    fun addControlPoint(channel: CurveChannel, point: ControlPoint) {
        val current = _curves.value[channel] ?: return
        val newPoints = (current.controlPoints + point).sortedBy { it.x }
        updateCurve(channel, newPoints)
    }

    /**
     * 移动控制点
     */
    fun moveControlPoint(channel: CurveChannel, index: Int, newPoint: ControlPoint) {
        val current = _curves.value[channel] ?: return
        if (index < 0 || index >= current.controlPoints.size) return

        // 首尾点不能移动到范围外
        val clampedX = when (index) {
            0 -> 0f
            current.controlPoints.size - 1 -> 1f
            else -> newPoint.x.coerceIn(0f, 1f)
        }
        val clampedY = newPoint.y.coerceIn(0f, 1f)

        val newPoints = current.controlPoints.toMutableList()
        newPoints[index] = ControlPoint(clampedX, clampedY)
        updateCurve(channel, newPoints.sortedBy { it.x })
    }

    /**
     * 删除控制点
     */
    fun removeControlPoint(channel: CurveChannel, index: Int) {
        val current = _curves.value[channel] ?: return
        // 不能删除首尾点
        if (index <= 0 || index >= current.controlPoints.size - 1) return

        val newPoints = current.controlPoints.toMutableList()
        newPoints.removeAt(index)
        updateCurve(channel, newPoints)
    }

    /**
     * 重置曲线
     */
    fun resetCurve(channel: CurveChannel) {
        updateCurve(channel, listOf(ControlPoint(0f, 0f), ControlPoint(1f, 1f)))
    }

    /**
     * 重置所有曲线
     */
    fun resetAll() {
        _curves.value = CurveChannel.entries.associateWith { Curve(it) }
        lutCache = null
    }

    /**
     * 应用曲线预设
     */
    fun applyPreset(preset: CurvePreset) {
        val newCurves = _curves.value.toMutableMap()
        preset.curves.forEach { (channel, curve) ->
            newCurves[channel] = curve
        }
        _curves.value = newCurves
        lutCache = null
    }

    /**
     * 应用曲线到 Bitmap
     */
    suspend fun applyToBitmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        // 生成 LUT
        val luts = getLUTs()

        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val rgbLut = luts[CurveChannel.RGB]!!
        val rLut = luts[CurveChannel.RED]!!
        val gLut = luts[CurveChannel.GREEN]!!
        val bLut = luts[CurveChannel.BLUE]!!

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val a = Color.alpha(pixel)

                // 先应用 RGB 曲线，再应用各通道曲线
                val newR = rLut[rgbLut[r]]
                val newG = gLut[rgbLut[g]]
                val newB = bLut[rgbLut[b]]

                result.setPixel(x, y, Color.argb(a, newR, newG, newB))
            }
        }

        result
    }

    /**
     * 获取 LUT（带缓存）
     */
    private fun getLUTs(): Map<CurveChannel, IntArray> {
        if (lutCache == null) {
            lutCache = _curves.value.mapValues { (_, curve) -> curve.generateLUT() }
        }
        return lutCache!!
    }

    /**
     * 更新曲线
     */
    private fun updateCurve(channel: CurveChannel, points: List<ControlPoint>) {
        val current = _curves.value.toMutableMap()
        current[channel] = Curve(channel, points)
        _curves.value = current
        lutCache = null
    }

    companion object {
        @Volatile
        private var instance: ToneCurveManager? = null

        fun getInstance(context: Context): ToneCurveManager {
            return instance ?: synchronized(this) {
                instance ?: ToneCurveManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 曲线预设
 */
data class CurvePreset(
    val id: String,
    val name: String,
    val description: String,
    val curves: Map<ToneCurveManager.CurveChannel, ToneCurveManager.Curve>
)
