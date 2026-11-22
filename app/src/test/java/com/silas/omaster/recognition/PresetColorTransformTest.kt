package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PresetColorTransform 套色契约测试（纯 JVM）：
 * 验证预设的相机滤镜与详情参数（色温/色调/EV/柔光/暗角/锐度）真正进入变换。
 */
class PresetColorTransformTest {

    private fun preset(
        filter: String? = null,
        saturation: Int? = null,
        warmCool: Int? = null,
        cyanMagenta: Int? = null,
        tone: Int? = null,
        colorTemperature: Int? = null,
        colorHue: Int? = null,
        exposureCompensation: String? = null,
        softLight: String? = null,
        vignette: String? = null,
        sharpness: Int? = null
    ) = MasterPreset(
        id = "t", name = "测试", coverPath = "",
        filter = filter, saturation = saturation, warmCool = warmCool,
        cyanMagenta = cyanMagenta, tone = tone,
        colorTemperature = colorTemperature, colorHue = colorHue,
        exposureCompensation = exposureCompensation, softLight = softLight,
        vignette = vignette, sharpness = sharpness
    )

    @Test
    fun `中性预设输出基准矩阵`() {
        val m = PresetColorTransform.buildColorMatrix(preset())
        // 每行通道权重和 ≈ 1（保亮度），lift = 0
        assertEquals(1f, m[0] + m[1] + m[2], 0.01f)
        assertEquals(1f, m[5] + m[6] + m[7], 0.01f)
        assertEquals(1f, m[10] + m[11] + m[12], 0.01f)
        assertEquals(0f, m[4], 0.01f)
        assertFalse(PresetColorTransform.hasVisibleEffect(preset()))
    }

    @Test
    fun `黑白滤镜产生真灰度且屏蔽冷暖偏移`() {
        val m = PresetColorTransform.buildColorMatrix(preset(filter = "黑白", warmCool = 80))
        // 三行通道权重完全一致 → 纯灰度
        assertEquals(m[0], m[5], 0.001f)
        assertEquals(m[1], m[6], 0.001f)
        assertEquals(m[2], m[7], 0.001f)
        assertEquals(m[10], m[5], 0.001f)
        assertEquals(m[4], m[9], 0.001f)
        assertEquals(m[14], m[9], 0.001f)
    }

    @Test
    fun `灰度滤镜下影调走暗房语义`() {
        // 【v1.8.3 审计修复】内置"高对比黑白"（影调 -61）必须渲染成强反差低调，
        // 而非彩色路径的"负影调 → 降对比"灰蒙输出
        val bwBase = PresetColorTransform.buildColorMatrix(preset(filter = "黑白", softLight = "柔美"))
        val bwLow = PresetColorTransform.buildColorMatrix(preset(filter = "黑白", tone = -61, softLight = "柔美"))
        val bwHigh = PresetColorTransform.buildColorMatrix(preset(filter = "黑白", tone = 61, softLight = "柔美"))
        // 负影调 = 低调摄影：压黑位（lift 下降）+ 增强反差（R 增益上升）
        assertTrue("负影调应增强灰度反差", bwLow[0] > bwBase[0])
        assertTrue("负影调应压低黑位", bwLow[9] < bwBase[9])
        // 正影调 = 高调摄影：提黑位 + 柔化反差
        assertTrue("正影调应柔化灰度反差", bwHigh[0] < bwBase[0])
        assertTrue("正影调应提亮黑位", bwHigh[9] > bwBase[9])
        // 彩色路径语义不变：负影调仍降对比
        val colorLow = PresetColorTransform.buildColorMatrix(preset(tone = -61))
        assertTrue(colorLow[0] < PresetColorTransform.buildColorMatrix(preset())[0])
    }

    @Test
    fun `胶片滤镜提黑降对比`() {
        val neutral = PresetColorTransform.buildColorMatrix(preset())
        val film = PresetColorTransform.buildColorMatrix(preset(filter = "胶片"))
        assertTrue(film[4] > neutral[4]) // lift 提升（黑位发灰）
        assertTrue(film[0] < neutral[0]) // 对比下降
    }

    @Test
    fun `滤镜名带后缀仍能匹配`() {
        assertEquals(PresetColorTransform.filterAdjustment("黑白 高对比"),
            PresetColorTransform.filterAdjustment("黑白"))
    }

    @Test
    fun `滤镜强度百分比按比例缩放`() {
        val full = PresetColorTransform.filterAdjustment("复古")
        val half = PresetColorTransform.filterAdjustment("复古 50%")
        // 半数强度：各调整量减半（保留方向与极性）
        assertEquals(full.saturation / 2f, half.saturation, 1e-4f)
        assertEquals(full.warmCool / 2f, half.warmCool, 1e-4f)
        assertEquals(full.lift / 2f, half.lift, 1e-4f)
        assertTrue(half.saturation < 0f)
        // 100% 与无百分比等价；0% 完全中性
        assertEquals(full, PresetColorTransform.filterAdjustment("复古 100%"))
        assertEquals(PresetColorTransform.NEUTRAL, PresetColorTransform.filterAdjustment("标准 0%"))
        assertEquals(PresetColorTransform.NEUTRAL, PresetColorTransform.filterAdjustment("复古 0%"))
        // 百分比解析
        assertEquals(0.75f, PresetColorTransform.parseFilterPercent("复古 75%")!!, 1e-4f)
        assertEquals(1f, PresetColorTransform.parseFilterPercent("胶片 100%")!!, 1e-4f)
        assertTrue(PresetColorTransform.parseFilterPercent("胶片") == null)
    }

    @Test
    fun `内置预设的真实滤镜值全部可识别`() {
        // assets/presets.json 实际出现的滤镜写法（含强度百分比）
        listOf("复古 100%", "清新 100%", "浓郁 100%", "明艳 100%", "复古 75%",
            "童话 73%", "黑白 100%", "通透 100%", "复古 60%", "人文 100%", "人文 80%", "标准 0%")
            .forEach { assertTrue("滤镜应可识别: $it", PresetCapturePlan.isFilterApplicable(it)) }
    }

    @Test
    fun `低色温偏暖高色温偏冷`() {
        val warm = PresetColorTransform.buildColorMatrix(preset(colorTemperature = 3000))
        val cool = PresetColorTransform.buildColorMatrix(preset(colorTemperature = 7500))
        assertTrue(warm[0] > cool[0]) // R 增益更高
        assertTrue(warm[4] > cool[4]) // R 偏移更暖
        assertTrue(warm[14] < cool[14]) // B 偏移被压低
    }

    @Test
    fun `色调正值偏品红`() {
        val base = PresetColorTransform.buildColorMatrix(preset())
        val magenta = PresetColorTransform.buildColorMatrix(preset(colorHue = 100))
        assertNotEquals(base[12], magenta[12], 1e-3f)
        assertTrue(magenta[12] < base[12]) // B 对角增益下降 → 偏品红
        // 【v1.8.0 审计修复】青品轴必须同时作用于 G 行，否则绿色内容完全不受色调影响
        assertTrue("偏品红应压低 G 行增益", magenta[6] < base[6])
        val green = PresetColorTransform.buildColorMatrix(preset(colorHue = -100))
        assertTrue("偏绿应抬升 G 行增益", green[6] > base[6])
    }

    @Test
    fun `中性取值的详情参数不判定为可见效果`() {
        // 5500K 色温 / 0 色调的矩阵增量为 0，与 EV=0、饱和度 0 同口径
        assertFalse(PresetColorTransform.hasVisibleEffect(preset(colorTemperature = 5500)))
        assertFalse(PresetColorTransform.hasVisibleEffect(preset(colorHue = 0)))
        assertFalse(PresetColorTransform.hasVisibleEffect(preset(saturation = 0)))
        assertFalse(PresetColorTransform.hasVisibleEffect(preset(softLight = "无")))
        // 非中性取值仍判定为可见效果
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(colorTemperature = 4000)))
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(colorHue = 100)))
    }

    @Test
    fun `曝光补偿按2的EV次方增益`() {
        val base = PresetColorTransform.buildColorMatrix(preset())
        val plus1 = PresetColorTransform.buildColorMatrix(preset(exposureCompensation = "+1.0"))
        assertEquals(base[6] * 2f, plus1[6], 0.01f)
    }

    @Test
    fun `EV字符串解析`() {
        assertEquals(0.7f, PresetColorTransform.parseExposureEv("+0.7"), 1e-4f)
        assertEquals(-1f, PresetColorTransform.parseExposureEv("-1.0"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseExposureEv(null), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseExposureEv("auto"), 1e-4f)
        assertEquals(3f, PresetColorTransform.parseExposureEv("5"), 1e-4f) // 限幅
    }

    @Test
    fun `EV区间取中值`() {
        // 内置预设真实写法：区间 EV（大师模式）
        assertEquals(-0.65f, PresetColorTransform.parseExposureEv("-0.3~-1.0"), 1e-4f)
        assertEquals(-0.5f, PresetColorTransform.parseExposureEv("-1~0"), 1e-4f)
        assertEquals(-0.35f, PresetColorTransform.parseExposureEv("0~-0.7"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseExposureEv("-0.3~0.3"), 1e-4f)
    }

    @Test
    fun `柔光强度解析支持文字与数字`() {
        assertEquals(0.3f, PresetColorTransform.parseSoftLight("弱"), 1e-4f)
        assertEquals(0.5f, PresetColorTransform.parseSoftLight("柔美"), 1e-4f) // 内置预设真实写法
        assertEquals(0.8f, PresetColorTransform.parseSoftLight("梦幻"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseSoftLight("无"), 1e-4f)
        assertEquals(0.5f, PresetColorTransform.parseSoftLight("50"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseSoftLight(null), 1e-4f)
    }

    @Test
    fun `暗角与锐度解析`() {
        assertEquals(0.5f, PresetColorTransform.parseVignette("开"), 1e-4f)
        assertEquals(0.8f, PresetColorTransform.parseVignette("强"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseVignette("关"), 1e-4f)
        assertEquals(0f, PresetColorTransform.parseVignette(null), 1e-4f)
        assertEquals(0.3f, PresetColorTransform.sharpnessAmount(preset(sharpness = 30)), 1e-4f)
        assertEquals(1f, PresetColorTransform.sharpnessAmount(preset(sharpness = 150)), 1e-4f)
    }

    @Test
    fun `仅滤镜或仅详情参数也判定为可见效果`() {
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(filter = "胶片")))
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(colorTemperature = 4000)))
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(vignette = "开")))
        assertTrue(PresetColorTransform.hasVisibleEffect(preset(sharpness = 20)))
        assertFalse(PresetColorTransform.hasVisibleEffect(preset(filter = "原图")))
    }
}
