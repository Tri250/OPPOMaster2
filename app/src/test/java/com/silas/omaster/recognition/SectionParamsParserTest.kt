package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * sections → typed 字段桥接（P0 修复回归）。
 *
 * 构造数据与 assets/presets.json 真实写法一致（label 为 @string/param_* 资源引用），
 * 覆盖：滤镜强度百分比、柔光别称、区间曝光、色温 K 值、不覆盖已有 typed 字段等。
 */
class SectionParamsParserTest {

    private fun section(vararg items: Pair<String, String>) =
        listOf(PresetSection(items = items.map { PresetItem(label = it.first, value = it.second) }))

    @Test
    fun `资源引用 label 正确映射到 typed 字段`() {
        val preset = MasterPreset(
            coverPath = "",
            name = "胶片感",
            sections = section(
                "@string/param_filter" to "复古 75%",
                "@string/param_soft_light" to "柔美",
                "@string/param_tone_curve" to "-5",
                "@string/param_saturation" to "+20",
                "@string/param_warm_cool" to "-3",
                "@string/param_cyan_magenta" to "+4",
                "@string/param_sharpness" to "7",
                "@string/param_vignette" to "开"
            )
        )
        val enriched = SectionParamsParser.enrich(preset)
        assertEquals("复古 75%", enriched.filter)
        assertEquals("柔美", enriched.softLight)
        assertEquals(-5, enriched.tone)
        assertEquals(20, enriched.saturation)
        assertEquals(-3, enriched.warmCool)
        assertEquals(4, enriched.cyanMagenta)
        assertEquals(7, enriched.sharpness)
        assertEquals("开", enriched.vignette)
        assertTrue(PresetColorTransform.hasVisibleEffect(enriched))
    }

    @Test
    fun `大师模式参数映射 ISO 快门 曝光区间 色温 色调`() {
        val preset = MasterPreset(
            coverPath = "",
            name = "大师模式",
            sections = section(
                "@string/param_iso" to "50",
                "@string/param_shutter" to "1/100",
                "@string/param_exposure" to "-0.3~-1.0",
                "@string/param_color_temp" to "5200K",
                "@string/param_tone" to "+3"
            )
        )
        val enriched = SectionParamsParser.enrich(preset)
        assertEquals("50", enriched.iso)
        assertEquals("1/100", enriched.shutterSpeed)
        assertEquals("-0.3~-1.0", enriched.exposureCompensation)
        assertEquals(5200, enriched.colorTemperature)
        assertEquals(3, enriched.colorHue)
        // 区间曝光取中值 -0.65
        assertEquals(-0.65f, PresetColorTransform.parseExposureEv(enriched.exposureCompensation), 0.001f)
    }

    @Test
    fun `已有 typed 字段不被 sections 覆盖`() {
        val preset = MasterPreset(
            coverPath = "",
            name = "识别草稿",
            filter = "胶片",
            saturation = -14,
            sections = section("@string/param_filter" to "黑白 100%", "@string/param_saturation" to "+20")
        )
        val enriched = SectionParamsParser.enrich(preset)
        assertEquals("胶片", enriched.filter)
        assertEquals(-14, enriched.saturation)
    }

    @Test
    fun `无 sections 或空 sections 返回原对象`() {
        val preset = MasterPreset(coverPath = "", name = "无参数")
        assertTrue(SectionParamsParser.enrich(preset) === preset)
        val empty = preset.copy(sections = emptyList())
        assertTrue(SectionParamsParser.enrich(empty) === empty)
    }

    @Test
    fun `原始中文与英日文 label 均可识别`() {
        val zh = MasterPreset(coverPath = "", name = "旧版", sections = section("滤镜" to "胶片", "饱和度" to "+10"))
        assertEquals("胶片", SectionParamsParser.enrich(zh).filter)
        val en = MasterPreset(coverPath = "", name = "EN", sections = section("Filter" to "胶片", "Saturation" to "+10"))
        assertEquals("胶片", SectionParamsParser.enrich(en).filter)
        val ja = MasterPreset(coverPath = "", name = "JA", sections = section("フィルター" to "胶片", "彩度" to "+10"))
        assertEquals("胶片", SectionParamsParser.enrich(ja).filter)
    }

    @Test
    fun `未知 label 与空值被安全跳过`() {
        val preset = MasterPreset(
            coverPath = "",
            name = "未知",
            sections = section(
                "@string/param_contrast" to "+10",
                "完全未知的项" to "值",
                "@string/param_filter" to "",
                "@string/param_saturation" to "  "
            )
        )
        val enriched = SectionParamsParser.enrich(preset)
        assertNull(enriched.filter)
        assertNull(enriched.saturation)
        assertFalse(PresetColorTransform.hasVisibleEffect(enriched))
    }

    @Test
    fun `带正号整数与色温 K 解析`() {
        assertEquals(20, SectionParamsParser.parseIntOrNullSigned("+20"))
        assertEquals(-5, SectionParamsParser.parseIntOrNullSigned("-5"))
        assertEquals(0, SectionParamsParser.parseIntOrNullSigned("+0"))
        assertNull(SectionParamsParser.parseIntOrNullSigned("abc"))
        assertNull(SectionParamsParser.parseIntOrNullSigned(null))
        assertEquals(5200, SectionParamsParser.parseKelvin("5200K"))
        assertEquals(5000, SectionParamsParser.parseKelvin("5000 k"))
        assertEquals(5200, SectionParamsParser.parseKelvin("5200"))
        assertNull(SectionParamsParser.parseKelvin("auto"))
        assertNull(SectionParamsParser.parseKelvin(null))
    }
}
