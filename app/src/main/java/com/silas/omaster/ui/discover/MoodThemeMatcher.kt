package com.silas.omaster.ui.discover

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.silas.omaster.data.local.Mood
import com.silas.omaster.ui.theme.BrandTheme
import kotlin.math.abs

/**
 * 【v1.6.14 发现 2.0】心情 → 全局品牌主题匹配
 *
 * 以心情渐变主色的 HSV 色相与各品牌主色比对（环形色相差 + 饱和度差小权重），
 * 取最接近的品牌主题，实现"心情驱动全局个性化"（ColorOS 17 个性化特色）。
 * 纯本地计算，无网络依赖。
 */
internal fun matchBrandThemeForMood(mood: Mood): BrandTheme {
    val moodArgb = mood.gradient.first().toInt()
    val moodHsv = FloatArray(3).also { Color.colorToHSV(moodArgb, it) }

    return BrandTheme.entries.minByOrNull { theme ->
        val themeArgb = theme.primaryColor.toArgb()
        val themeHsv = FloatArray(3).also { Color.colorToHSV(themeArgb, it) }
        hueDistance(moodHsv[0], themeHsv[0]) + abs(moodHsv[1] - themeHsv[1]) * 0.35f
    } ?: BrandTheme.Hasselblad
}

/** 环形色相差（0~180） */
private fun hueDistance(h1: Float, h2: Float): Float {
    val diff = abs(h1 - h2) % 360f
    return if (diff > 180f) 360f - diff else diff
}
