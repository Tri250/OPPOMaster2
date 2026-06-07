package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color
import com.silas.omaster.R

enum class BrandTheme(
    val id: String,
    val brandNameResId: Int,
    val colorNameResId: Int,
    val primaryColor: Color,
    val hexCode: String
) {
    Hasselblad("hasselblad", R.string.brand_hasselblad, R.string.color_hasselblad_orange, HasselbladOrange, "#FFB347"),
    Zeiss("zeiss", R.string.brand_zeiss, R.string.color_zeiss_blue, ZeissBlue, "#0066CC"),
    Leica("leica", R.string.brand_leica, R.string.color_leica_red, LeicaRed, "#EF4444"),
    Ricoh("ricoh", R.string.brand_ricoh, R.string.color_ricoh_green, RicohGreen, "#22C55E"),
    Fujifilm("fujifilm", R.string.brand_fujifilm, R.string.color_fujifilm_green, AuroraGreen, "#22C55E"),
    Canon("canon", R.string.brand_canon, R.string.color_canon_red, SunsetRed, "#EF4444"),
    Nikon("nikon", R.string.brand_nikon, R.string.color_nikon_yellow, OppoSunriseGold, "#FFB300"),
    Sony("sony", R.string.brand_sony, R.string.color_sony_orange, HasselbladOrangePro, "#FF8C42"),
    PhaseOne("phaseone", R.string.brand_phaseone, R.string.color_phaseone_grey, ColorOSGrey500, "#71717A");

    companion object {
        fun fromId(id: String): BrandTheme {
            return entries.find { it.id == id } ?: Hasselblad
        }
    }
}

// 兼容其他品牌主题色
private val ZeissBlue = DeepOceanBlue
private val LeicaRed = SunsetRed
private val RicohGreen = AuroraGreen
private val FujifilmGreen = AuroraGreen
private val CanonRed = SunsetRed
private val NikonYellow = OppoSunriseGold
private val SonyOrange = HasselbladOrangePro
private val PhaseOneGrey = ColorOSGrey500
