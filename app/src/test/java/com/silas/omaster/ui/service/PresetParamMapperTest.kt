package com.silas.omaster.ui.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetSection
import com.silas.omaster.util.IconFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * FD-03① 悬浮窗参数映射单测（Robolectric：PresetI18n 需 Context）
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PresetParamMapperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun oppoSections() = listOf(
        PresetSection(
            title = "参数",
            items = listOf(
                PresetItem(label = "滤镜", value = "100"),
                PresetItem(label = "饱和", value = "50"),
                PresetItem(label = "锐度", value = "30")
            )
        )
    )

    private fun realmeGRSections() = listOf(
        PresetSection(
            title = "参数",
            items = listOf(
                PresetItem(label = "滤镜", value = "100"),
                PresetItem(label = "对比度（亮部）", value = "20"),
                PresetItem(label = "颗粒尺寸", value = "10")
            )
        )
    )

    @Test
    fun `OPPO 预设按标准映射提取 且只含存在的参数`() {
        val params = PresetParamMapper.extractDynamicParams(context, oppoSections())

        assertEquals(3, params.size)
        assertEquals(IconFont.FILTER, params[0].second)
        assertEquals(IconFont.SATURATION, params[1].second)
        assertEquals(IconFont.SHARPNESS, params[2].second)
        assertTrue(params.all { it.first != "-" })
    }

    @Test
    fun `Realme GR 预设被正确识别且走文字映射`() {
        assertTrue(PresetParamMapper.isRealmePreset(realmeGRSections()))
        assertFalse(PresetParamMapper.isRealmePreset(oppoSections()))

        val params = PresetParamMapper.extractDynamicParams(context, realmeGRSections())
        // Realme GR 全部使用文字标签："对比度（亮部）"先命中"对比度"条目 → CONTRAST
        assertTrue(params.all {
            it.second == IconFont.FILTER_TEXT ||
                it.second == IconFont.CONTRAST ||
                it.second == IconFont.GRAIN_SIZE_TEXT
        })
    }

    @Test
    fun `空 sections 返回空参数序列`() {
        assertTrue(PresetParamMapper.extractDynamicParams(context, emptyList()).isEmpty())
    }

    @Test
    fun `图标映射覆盖主要参数标签`() {
        assertEquals(IconFont.FILTER, PresetParamMapper.getIconForLabel("滤镜"))
        assertEquals(IconFont.VIGNETTE, PresetParamMapper.getIconForLabel("Vignette"))
        assertEquals("📸", PresetParamMapper.getIconForLabel("ISO"))
        assertEquals("💡", PresetParamMapper.getIconForLabel("拍摄建议"))
        assertEquals("⚙️", PresetParamMapper.getIconForLabel("未知标签"))
    }
}
