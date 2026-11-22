package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PresetCapturePlan 拍摄计划测试（纯 JVM）：
 * 锁定"参数真实分流"契约——ISO/快门写入拍摄、EV 按步进写入、
 * 调色参数拍后套用、不满足条件时如实标记无法套用。
 */
class PresetCapturePlanTest {

    /** 典型 OPPO Reno（LEVEL_3，支持 AE_OFF）手动范围 */
    private val renoRanges = ManualRanges(
        isoRange = 50..3200,
        exposureTimeRange = 100_000L..32_000_000_000L,
        evStepRange = -12..12,
        evStepEv = 1f / 6f,
        aeModeOffSupported = true
    )

    private fun preset(
        iso: String? = null,
        shutter: String? = null,
        ev: String? = null,
        filter: String? = null,
        saturation: Int? = null,
        colorTemperature: Int? = null,
        warmCool: Int? = null,
        cyanMagenta: Int? = null,
        tone: Int? = null
    ) = MasterPreset(
        id = "t", name = "测试", coverPath = "",
        iso = iso, shutterSpeed = shutter, exposureCompensation = ev,
        filter = filter, saturation = saturation, colorTemperature = colorTemperature,
        warmCool = warmCool, cyanMagenta = cyanMagenta, tone = tone
    )

    private fun CapturePlan.statusOf(key: ParamKey) = params.firstOrNull { it.key == key }

    @Test
    fun `ISO快门成对且设备支持时写入拍摄并裁剪范围`() {
        val plan = PresetCapturePlan.build(
            preset(iso = "100", shutter = "1/125"), renoRanges
        )
        assertEquals(100, plan.iso)
        assertEquals(8_000_000L, plan.shutterNs) // 1/125s
        assertTrue(plan.manualExposure)
        assertEquals(ApplyStatus.WRITTEN, plan.statusOf(ParamKey.ISO)?.status)
        assertEquals(ApplyStatus.WRITTEN, plan.statusOf(ParamKey.SHUTTER)?.status)
    }

    @Test
    fun `超范围ISO被裁剪时快门等比补偿保持曝光量`() {
        // ISO 6400 超出上限 3200（-1EV）→ 快门 1/2000 延长到 1/1000（+1EV），曝光量不变
        val plan = PresetCapturePlan.build(
            preset(iso = "6400", shutter = "1/2000"), renoRanges
        )
        assertEquals(3200, plan.iso)
        assertEquals(1_000_000L, plan.shutterNs)
        assertEquals("6400→3200", plan.statusOf(ParamKey.ISO)?.value)
        assertEquals("1/2000→1/1000", plan.statusOf(ParamKey.SHUTTER)?.value)

        // 快门 1/10000 低于设备下限 100us → 取下限，ISO 在范围内不动
        val fast = PresetCapturePlan.build(preset(iso = "50", shutter = "1/10000"), renoRanges)
        assertEquals(50, fast.iso)
        assertEquals(100_000L, fast.shutterNs) // 100us 设备下限
    }

    @Test
    fun `Reno17主摄下限ISO下曝光守恒且芯片如实`() {
        // 【v1.8.3 Reno17 适配】主摄 ISO 下限 100，内置"人文/清新人文"指定 ISO 50 + 1/100：
        // ISO 钳到 100（+1EV）时快门减半到 1/200（-1EV），成片亮度与预设意图一致
        val reno17 = ManualRanges(
            isoRange = 100..6400,
            exposureTimeRange = 100_000L..1_000_000_000L,
            evStepRange = -12..12,
            evStepEv = 1f / 6f,
            aeModeOffSupported = true
        )
        val plan = PresetCapturePlan.build(preset(iso = "50", shutter = "1/100"), reno17)
        assertEquals(100, plan.iso)
        assertEquals(5_000_000L, plan.shutterNs)
        assertEquals(ApplyStatus.WRITTEN, plan.statusOf(ParamKey.ISO)?.status)
        assertEquals("50→100", plan.statusOf(ParamKey.ISO)?.value)
        assertEquals("1/100→1/200", plan.statusOf(ParamKey.SHUTTER)?.value)
        assertTrue(plan.manualExposure)
    }

    @Test
    fun `ISO在设备范围内时不补偿快门且展示原值`() {
        val plan = PresetCapturePlan.build(preset(iso = "200", shutter = "1/100"), renoRanges)
        assertEquals(200, plan.iso)
        assertEquals(10_000_000L, plan.shutterNs)
        assertEquals("200", plan.statusOf(ParamKey.ISO)?.value)
        assertEquals("1/100", plan.statusOf(ParamKey.SHUTTER)?.value)
    }

    @Test
    fun `设备不支持手动曝光时ISO快门标记无法套用`() {
        val plan = PresetCapturePlan.build(preset(iso = "100", shutter = "1/125"), ManualRanges.NONE)
        assertNull(plan.iso)
        assertFalse(plan.manualExposure)
        assertEquals(
            UnsupportedReason.MANUAL_EXPOSURE_UNAVAILABLE,
            plan.statusOf(ParamKey.ISO)?.reason
        )
        assertEquals(ApplyStatus.UNSUPPORTED, plan.statusOf(ParamKey.SHUTTER)?.status)
    }

    @Test
    fun `仅提供ISO缺快门时无法套用手动曝光`() {
        val plan = PresetCapturePlan.build(preset(iso = "100"), renoRanges)
        assertEquals(UnsupportedReason.NEED_ISO_SHUTTER_PAIR, plan.statusOf(ParamKey.ISO)?.reason)
    }

    @Test
    fun `EV在自动曝光开启时按设备步进写入`() {
        val plan = PresetCapturePlan.build(preset(ev = "+0.7"), renoRanges)
        // 0.7EV / (1/6EV) ≈ 4 步
        assertEquals(4, plan.evSteps)
        assertEquals(ApplyStatus.WRITTEN, plan.statusOf(ParamKey.EV)?.status)
    }

    @Test
    fun `手动曝光激活时EV标记不适用且归因正确`() {
        val plan = PresetCapturePlan.build(
            preset(iso = "100", shutter = "1/125", ev = "-1.0"), renoRanges
        )
        assertNull(plan.evSteps)
        // 归因必须是"手动曝光接管"而非"设备不支持手动曝光"（设备恰恰支持）
        assertEquals(
            UnsupportedReason.MANUAL_EXPOSURE_OVERRIDES,
            plan.statusOf(ParamKey.EV)?.reason
        )
    }

    @Test
    fun `EV超出设备补偿范围被裁剪`() {
        val plan = PresetCapturePlan.build(preset(ev = "+3.5"), renoRanges)
        assertEquals(12, plan.evSteps) // 上限 12 步（±2EV）
    }

    @Test
    fun `EV为零或设备无步进信息时不写入`() {
        // EV=0：无需套用，不产生参数项
        assertNull(PresetCapturePlan.build(preset(ev = "0"), renoRanges).statusOf(ParamKey.EV))
        // 设备无步进信息：无法套用
        val none = PresetCapturePlan.build(preset(ev = "+1"), ManualRanges.NONE)
        assertEquals(ApplyStatus.UNSUPPORTED, none.statusOf(ParamKey.EV)?.status)
        assertEquals(UnsupportedReason.EV_UNAVAILABLE, none.statusOf(ParamKey.EV)?.reason)
        assertNull(none.evSteps)
    }

    @Test
    fun `调色参数恒为拍后套用`() {
        val plan = PresetCapturePlan.build(
            preset(filter = "胶片", saturation = -14, colorTemperature = 4300), renoRanges
        )
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.FILTER)?.status)
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.SATURATION)?.status)
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.COLOR_TEMP)?.status)
        // 拍后参数不受设备能力影响
        val planNoManual = PresetCapturePlan.build(
            preset(filter = "胶片", saturation = -14, colorTemperature = 4300), ManualRanges.NONE
        )
        assertEquals(ApplyStatus.POST, planNoManual.statusOf(ParamKey.FILTER)?.status)
    }

    @Test
    fun `未识别滤镜名如实标记无法套用`() {
        // 引擎滤镜表外的新机型独占滤镜：不得"假装 POST"
        val plan = PresetCapturePlan.build(preset(filter = "经典负片", saturation = -14), renoRanges)
        val filterStatus = plan.statusOf(ParamKey.FILTER)
        assertEquals(ApplyStatus.UNSUPPORTED, filterStatus?.status)
        assertEquals(UnsupportedReason.FILTER_UNRECOGNIZED, filterStatus?.reason)
        // 其余数值参数仍正常 POST
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.SATURATION)?.status)
        // 带强度百分比的已识别滤镜仍 POST
        assertEquals(ApplyStatus.POST,
            PresetCapturePlan.build(preset(filter = "复古 75%"), renoRanges).statusOf(ParamKey.FILTER)?.status)
        // 显式"原图/标准"是有意选择，视为可套用
        assertEquals(ApplyStatus.POST,
            PresetCapturePlan.build(preset(filter = "标准 0%"), renoRanges).statusOf(ParamKey.FILTER)?.status)
        // 显式 0% 强度（如"黑白 0%"）= 主动不加滤镜，同样不得误标 FILTER_UNRECOGNIZED
        assertEquals(ApplyStatus.POST,
            PresetCapturePlan.build(preset(filter = "黑白 0%"), renoRanges).statusOf(ParamKey.FILTER)?.status)
        // null 滤镜不产生任何参数项
        assertNull(PresetCapturePlan.build(preset(), renoRanges).statusOf(ParamKey.FILTER))
    }

    @Test
    fun `sections 预设经桥接后调色参数全部拍后套用`() {
        // 模拟内置预设：只有 sections（展示参数），经 SectionParamsParser 桥接后
        // 引擎应拿到完整的拍后套用参数（P0 修复的端到端契约）
        val preset = SectionParamsParser.enrich(
            com.silas.omaster.model.MasterPreset(
                coverPath = "", name = "胶片感",
                sections = listOf(
                    com.silas.omaster.model.PresetSection(
                        items = listOf(
                            com.silas.omaster.model.PresetItem(label = "@string/param_filter", value = "复古 75%"),
                            com.silas.omaster.model.PresetItem(label = "@string/param_saturation", value = "+20"),
                            com.silas.omaster.model.PresetItem(label = "@string/param_vignette", value = "开")
                        )
                    )
                )
            )
        )
        val plan = PresetCapturePlan.build(preset, renoRanges)
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.FILTER)?.status)
        assertEquals("复古 75%", plan.statusOf(ParamKey.FILTER)?.value)
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.SATURATION)?.status)
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.VIGNETTE)?.status)
    }

    @Test
    fun `ISO区间写法取中值`() {
        assertEquals(300, PresetCapturePlan.parseIso("200-400"))
        assertEquals(100, PresetCapturePlan.parseIso("100"))
        assertNull(PresetCapturePlan.parseIso("Auto"))
        assertNull(PresetCapturePlan.parseIso(null))
        assertNull(PresetCapturePlan.parseIso("abc"))
    }

    @Test
    fun `中性取值的拍后参数不产生芯片`() {
        // 【v1.8.0 审计修复】与 EV=0 契约对齐：取值存在但效果中性 → 不标记"拍后套用"
        val plan = PresetCapturePlan.build(
            preset(saturation = 0, colorTemperature = 5500), renoRanges
        )
        assertNull(plan.statusOf(ParamKey.SATURATION))
        assertNull(plan.statusOf(ParamKey.COLOR_TEMP))
        // 非中性取值仍正常 POST
        val plan2 = PresetCapturePlan.build(
            preset(saturation = -14, colorTemperature = 4300), renoRanges
        )
        assertEquals(ApplyStatus.POST, plan2.statusOf(ParamKey.SATURATION)?.status)
        assertEquals(ApplyStatus.POST, plan2.statusOf(ParamKey.COLOR_TEMP)?.status)
    }

    @Test
    fun `快门写法解析为纳秒`() {
        assertEquals(8_000_000L, PresetCapturePlan.parseShutterNs("1/125"))
        assertEquals(16_666_666L, PresetCapturePlan.parseShutterNs("1/60"))
        assertEquals(2_000_000_000L, PresetCapturePlan.parseShutterNs("2"))
        assertEquals(500_000_000L, PresetCapturePlan.parseShutterNs("0.5"))
        assertNull(PresetCapturePlan.parseShutterNs("auto"))
        assertNull(PresetCapturePlan.parseShutterNs(null))
        // 纳秒 → 展示写法（钳制补偿后的实际写入值）
        assertEquals("1/200", PresetCapturePlan.formatShutterNs(5_000_000L))
        assertEquals("1/100", PresetCapturePlan.formatShutterNs(10_000_000L))
        assertEquals("2s", PresetCapturePlan.formatShutterNs(2_000_000_000L))
    }

    @Test
    fun `快门带不等号前缀仍按分数解析`() {
        // 【v1.8.3 审计】内置"清新人文"真实写法 "<1/100"（下限语义）→ 1/100s
        assertEquals(10_000_000L, PresetCapturePlan.parseShutterNs("<1/100"))
    }

    @Test
    fun `Auto曝光值不产生误导芯片`() {
        // 【v1.8.3 审计修复】"Auto" = 交给相机 AE 的正常创作意图，不得标记
        // "取值无法解析"虚报错误（内置 5 条预设含 ISO/快门 Auto 写法）
        val plan = PresetCapturePlan.build(preset(iso = "Auto", shutter = "Auto"), renoRanges)
        assertNull(plan.statusOf(ParamKey.ISO))
        assertNull(plan.statusOf(ParamKey.SHUTTER))
        assertNull(plan.iso)
        assertFalse(plan.manualExposure)
        // 真正无法解析的取值仍如实标记
        val bad = PresetCapturePlan.build(preset(iso = "abc", shutter = "xyz"), renoRanges)
        assertEquals(UnsupportedReason.VALUE_UNPARSEABLE, bad.statusOf(ParamKey.ISO)?.reason)
        assertEquals(UnsupportedReason.VALUE_UNPARSEABLE, bad.statusOf(ParamKey.SHUTTER)?.reason)
    }

    @Test
    fun `灰度滤镜下饱和度冷暖青品不产生虚报芯片`() {
        // 【v1.8.3 审计修复】黑白滤镜矩阵强制归零饱和度/冷暖/青品（真灰度输出），
        // 芯片不得标记"拍后套用"（内置"高对比黑白"冷暖+100/青品-39、
        // "梦幻黑白"冷暖-80 此前被虚报）
        val plan = PresetCapturePlan.build(
            preset(filter = "黑白", saturation = 0, warmCool = 100, cyanMagenta = -39), renoRanges
        )
        assertNull(plan.statusOf(ParamKey.SATURATION))
        assertNull(plan.statusOf(ParamKey.WARM_COOL))
        assertNull(plan.statusOf(ParamKey.CYAN_MAGENTA))
        // 滤镜本身仍如实 POST
        assertEquals(ApplyStatus.POST, plan.statusOf(ParamKey.FILTER)?.status)
        // 彩色滤镜下冷暖仍正常 POST
        val color = PresetCapturePlan.build(preset(filter = "复古", warmCool = 100), renoRanges)
        assertEquals(ApplyStatus.POST, color.statusOf(ParamKey.WARM_COOL)?.status)
    }
}
