package com.silas.omaster.param

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * HSL 独立调节管理器
 * 专业用户强需求，Lightroom 标配
 * 
 * 支持 8 色独立调节：
 * - 红色、橙色、黄色、绿色、青色、蓝色、紫色、洋红
 * - 每色可独立调节色相、饱和度、明度
 */
class HSLAdjustmentManager private constructor(context: Context) {

    // 8 色定义（对应 Lightroom HSL 面板）
    enum class ColorChannel(
        val displayName: String,
        val hueRange: ClosedFloatingPointRange<Float>,
        val icon: String
    ) {
        RED("红色", 0f..30f, "🔴"),
        ORANGE("橙色", 30f..60f, "🟠"),
        YELLOW("黄色", 60f..90f, "🟡"),
        GREEN("绿色", 90f..150f, "🟢"),
        CYAN("青色", 150f..210f, "🔵"),
        BLUE("蓝色", 210f..270f, "🔷"),
        PURPLE("紫色", 270f..300f, "🟣"),
        MAGENTA("洋红", 300f..360f, "💗");

        companion object {
            fun fromHue(hue: Float): ColorChannel {
                val normalizedHue = if (hue < 0) hue + 360f else hue
                return entries.find { normalizedHue in it.hueRange } ?: RED
            }
        }
    }

    // HSL 参数范围
    companion object {
        const val HUE_MIN = -180
        const val HUE_MAX = 180
        const val SATURATION_MIN = -100
        const val SATURATION_MAX = 100
        const val LIGHTNESS_MIN = -100
        const val LIGHTNESS_MAX = 100
    }

    // 单色 HSL 调整值
    data class HSLAdjustment(
        val hue: Int = 0,        // -180 ~ 180
        val saturation: Int = 0,  // -100 ~ 100
        val lightness: Int = 0    // -100 ~ 100
    ) {
        fun isZero(): Boolean = hue == 0 && saturation == 0 && lightness == 0
    }

    // 所有颜色的 HSL 调整值
    private val _hslAdjustments = MutableStateFlow<Map<ColorChannel, HSLAdjustment>>(
        ColorChannel.entries.associateWith { HSLAdjustment() }
    )
    val hslAdjustments: StateFlow<Map<ColorChannel, HSLAdjustment>> = _hslAdjustments.asStateFlow()

    // 当前选中的颜色通道
    private val _selectedChannel = MutableStateFlow(ColorChannel.RED)
    val selectedChannel: StateFlow<ColorChannel> = _selectedChannel.asStateFlow()

    // 预设 HSL 配置（参考 Lightroom 经典预设）
    val hslPresets = listOf(
        HSLPreset(
            id = "vivid_red",
            name = "鲜艳红",
            description = "增强红色饱和度，适合人像唇色",
            adjustments = mapOf(
                ColorChannel.RED to HSLAdjustment(hue = 0, saturation = 30, lightness = 10),
                ColorChannel.ORANGE to HSLAdjustment(hue = 0, saturation = 15, lightness = 5)
            )
        ),
        HSLPreset(
            id = "golden_hour",
            name = "金色时刻",
            description = "暖调金色风格，日落场景",
            adjustments = mapOf(
                ColorChannel.ORANGE to HSLAdjustment(hue = -10, saturation = 25, lightness = 10),
                ColorChannel.YELLOW to HSLAdjustment(hue = -5, saturation = 20, lightness = 15),
                ColorChannel.RED to HSLAdjustment(hue = 5, saturation = 15, lightness = 5)
            )
        ),
        HSLPreset(
            id = "teal_orange",
            name = "青橙电影",
            description = "电影感青橙色调",
            adjustments = mapOf(
                ColorChannel.CYAN to HSLAdjustment(hue = 15, saturation = 30, lightness = -10),
                ColorChannel.BLUE to HSLAdjustment(hue = 20, saturation = 20, lightness = -15),
                ColorChannel.ORANGE to HSLAdjustment(hue = -5, saturation = 25, lightness = 10),
                ColorChannel.YELLOW to HSLAdjustment(hue = -10, saturation = 15, lightness = 5)
            )
        ),
        HSLPreset(
            id = "lush_green",
            name = "翠绿风景",
            description = "增强绿色植被，风景专用",
            adjustments = mapOf(
                ColorChannel.GREEN to HSLAdjustment(hue = -10, saturation = 35, lightness = 15),
                ColorChannel.YELLOW to HSLAdjustment(hue = 5, saturation = 20, lightness = 10),
                ColorChannel.CYAN to HSLAdjustment(hue = 0, saturation = 15, lightness = 5)
            )
        ),
        HSLPreset(
            id = "deep_blue",
            name = "深邃蓝",
            description = "增强天空与水面蓝色",
            adjustments = mapOf(
                ColorChannel.BLUE to HSLAdjustment(hue = -10, saturation = 40, lightness = -20),
                ColorChannel.CYAN to HSLAdjustment(hue = -5, saturation = 25, lightness = -10)
            )
        ),
        HSLPreset(
            id = "skin_optimize",
            name = "肤色优化",
            description = "优化肤色表现，人像专用",
            adjustments = mapOf(
                ColorChannel.ORANGE to HSLAdjustment(hue = 5, saturation = -15, lightness = 10),
                ColorChannel.YELLOW to HSLAdjustment(hue = 5, saturation = -10, lightness = 5),
                ColorChannel.RED to HSLAdjustment(hue = 0, saturation = -5, lightness = 5)
            )
        ),
        HSLPreset(
            id = "pastel",
            name = "柔和粉彩",
            description = "低饱和柔和风格",
            adjustments = ColorChannel.entries.associateWith {
                HSLAdjustment(hue = 0, saturation = -30, lightness = 15)
            }
        ),
        HSLPreset(
            id = "vintage",
            name = "复古胶片",
            description = "复古胶片色调",
            adjustments = mapOf(
                ColorChannel.RED to HSLAdjustment(hue = 10, saturation = -20, lightness = 5),
                ColorChannel.ORANGE to HSLAdjustment(hue = 5, saturation = -15, lightness = 10),
                ColorChannel.YELLOW to HSLAdjustment(hue = 0, saturation = -10, lightness = 5),
                ColorChannel.BLUE to HSLAdjustment(hue = -5, saturation = -25, lightness = -10)
            )
        )
    )

    /**
     * 设置当前选中的颜色通道
     */
    fun selectChannel(channel: ColorChannel) {
        _selectedChannel.value = channel
    }

    /**
     * 调整指定颜色的色相
     * @param channel 颜色通道
     * @param value 色相偏移值 -180 ~ 180
     */
    fun adjustHue(channel: ColorChannel, value: Int) {
        val clampedValue = value.coerceIn(HUE_MIN, HUE_MAX)
        updateAdjustment(channel) { it.copy(hue = clampedValue) }
    }

    /**
     * 调整指定颜色的饱和度
     * @param channel 颜色通道
     * @param value 饱和度调整值 -100 ~ 100
     */
    fun adjustSaturation(channel: ColorChannel, value: Int) {
        val clampedValue = value.coerceIn(SATURATION_MIN, SATURATION_MAX)
        updateAdjustment(channel) { it.copy(saturation = clampedValue) }
    }

    /**
     * 调整指定颜色的明度
     * @param channel 颜色通道
     * @param value 明度调整值 -100 ~ 100
     */
    fun adjustLightness(channel: ColorChannel, value: Int) {
        val clampedValue = value.coerceIn(LIGHTNESS_MIN, LIGHTNESS_MAX)
        updateAdjustment(channel) { it.copy(lightness = clampedValue) }
    }

    /**
     * 批量设置 HSL 调整值
     */
    fun setAdjustment(channel: ColorChannel, adjustment: HSLAdjustment) {
        updateAdjustment(channel) { adjustment }
    }

    /**
     * 应用 HSL 预设
     */
    fun applyPreset(preset: HSLPreset) {
        val current = _hslAdjustments.value.toMutableMap()
        preset.adjustments.forEach { (channel, adjustment) ->
            current[channel] = adjustment
        }
        _hslAdjustments.value = current
    }

    /**
     * 重置指定颜色通道
     */
    fun resetChannel(channel: ColorChannel) {
        updateAdjustment(channel) { HSLAdjustment() }
    }

    /**
     * 重置所有 HSL 调整
     */
    fun resetAll() {
        _hslAdjustments.value = ColorChannel.entries.associateWith { HSLAdjustment() }
    }

    /**
     * 获取指定颜色的调整值
     */
    fun getAdjustment(channel: ColorChannel): HSLAdjustment {
        return _hslAdjustments.value[channel] ?: HSLAdjustment()
    }

    /**
     * 应用 HSL 调整到 Bitmap
     * 真实像素级处理
     */
    suspend fun applyToBitmap(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val adjustments = _hslAdjustments.value

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                val newPixel = applyHSLToPixel(pixel, adjustments)
                result.setPixel(x, y, newPixel)
            }
        }

        result
    }

    /**
     * 对单个像素应用 HSL 调整
     */
    private fun applyHSLToPixel(
        pixel: Int,
        adjustments: Map<ColorChannel, HSLAdjustment>
    ): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val a = Color.alpha(pixel)

        // RGB -> HSL
        val (h, s, l) = rgbToHsl(r, g, b)

        // 找到对应的颜色通道
        val channel = ColorChannel.fromHue(h)

        // 获取该通道的调整值
        val adjustment = adjustments[channel] ?: return pixel

        // 应用调整
        val newH = (h + adjustment.hue).let { if (it < 0) it + 360f else if (it >= 360) it - 360f else it }
        val newS = (s + adjustment.saturation / 100f * s).coerceIn(0f, 1f)
        val newL = (l + adjustment.lightness / 100f * (1f - abs(2f * l - 1f))).coerceIn(0f, 1f)

        // HSL -> RGB
        val (newR, newG, newB) = hslToRgb(newH, newS, newL)

        return Color.argb(a, newR, newG, newB)
    }

    /**
     * RGB 转 HSL
     */
    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rF = r / 255f
        val gF = g / 255f
        val bF = b / 255f

        val max = maxOf(rF, gF, bF)
        val min = minOf(rF, gF, bF)
        val l = (max + min) / 2f

        if (max == min) {
            return Triple(0f, 0f, l)
        }

        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)

        val h = when (max) {
            rF -> 60f * ((gF - bF) / d + if (gF < bF) 6f else 0f)
            gF -> 60f * ((bF - rF) / d + 2f)
            else -> 60f * ((rF - gF) / d + 4f)
        }

        return Triple(h, s, l)
    }

    /**
     * HSL 转 RGB
     */
    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Int, Int, Int> {
        if (s == 0f) {
            val v = (l * 255f).roundToInt()
            return Triple(v, v, v)
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val hNorm = h / 360f

        fun hueToRgb(p: Float, q: Float, t: Float): Float {
            var tNorm = t
            if (tNorm < 0f) tNorm += 1f
            if (tNorm > 1f) tNorm -= 1f
            return when {
                tNorm < 1f/6f -> p + (q - p) * 6f * tNorm
                tNorm < 1f/2f -> q
                tNorm < 2f/3f -> p + (q - p) * (2f/3f - tNorm) * 6f
                else -> p
            }
        }

        val r = (hueToRgb(p, q, hNorm + 1f/3f) * 255f).roundToInt().coerceIn(0, 255)
        val g = (hueToRgb(p, q, hNorm) * 255f).roundToInt().coerceIn(0, 255)
        val b = (hueToRgb(p, q, hNorm - 1f/3f) * 255f).roundToInt().coerceIn(0, 255)

        return Triple(r, g, b)
    }

    /**
     * 更新调整值的辅助方法
     */
    private inline fun updateAdjustment(
        channel: ColorChannel,
        update: (HSLAdjustment) -> HSLAdjustment
    ) {
        val current = _hslAdjustments.value.toMutableMap()
        current[channel] = update(current[channel] ?: HSLAdjustment())
        _hslAdjustments.value = current
    }

    companion object {
        @Volatile
        private var instance: HSLAdjustmentManager? = null

        fun getInstance(context: Context): HSLAdjustmentManager {
            return instance ?: synchronized(this) {
                instance ?: HSLAdjustmentManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * HSL 预设
 */
data class HSLPreset(
    val id: String,
    val name: String,
    val description: String,
    val adjustments: Map<HSLAdjustmentManager.ColorChannel, HSLAdjustmentManager.HSLAdjustment>
)
