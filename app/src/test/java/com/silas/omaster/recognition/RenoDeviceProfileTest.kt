package com.silas.omaster.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RenoDeviceProfile 纯逻辑单测（JVM，无设备依赖）：
 * 锁定 Reno15/16/17 机型识别与质感调校契约——
 * 识别失败一律降级 null（通用调校，与旧版行为逐位一致），绝不误判。
 */
class RenoDeviceProfileTest {

    // ═══════════════ 机型识别 ═══════════════

    @Test
    fun `营销名识别Reno三代际`() {
        assertEquals(
            RenoDeviceProfile.Family.RENO15,
            RenoDeviceProfile.detectFamily("OPPO", "PJD110", "OPPO Reno15 5G")
        )
        assertEquals(
            RenoDeviceProfile.Family.RENO16,
            RenoDeviceProfile.detectFamily("OPPO", "PJE110", "OPPO Reno16 Pro 5G")
        )
        assertEquals(
            RenoDeviceProfile.Family.RENO17,
            RenoDeviceProfile.detectFamily("OPPO", "PJD110", "Reno17 Pro+")
        )
    }

    @Test
    fun `机型名直接识别（全球版Build.MODEL含营销名）`() {
        assertEquals(
            RenoDeviceProfile.Family.RENO15,
            RenoDeviceProfile.detectFamily("OPPO", "OPPO Reno15 Pro 5G", null)
        )
        assertEquals(
            RenoDeviceProfile.Family.RENO17,
            RenoDeviceProfile.detectFamily("OPPO", "reno17", null)
        )
    }

    @Test
    fun `容忍空格变体且排除Reno150误匹配`() {
        assertEquals(
            RenoDeviceProfile.Family.RENO17,
            RenoDeviceProfile.detectFamily("OPPO", null, "OPPO Reno 17 Pro")
        )
        // "Reno150" 类文本不得匹配出代际（负向环视）
        assertNull(RenoDeviceProfile.detectFamily("OPPO", "Reno150", null))
        assertNull(RenoDeviceProfile.detectFamily("OPPO", null, "Reno160 Case"))
    }

    @Test
    fun `营销名优先于机型名`() {
        // 国行代号机型：模型名无信息，营销名命中；反之营销名不可得时模型名兜底
        assertEquals(
            RenoDeviceProfile.Family.RENO16,
            RenoDeviceProfile.detectFamily("OPPO", "PJD110", "OPPO Reno16")
        )
    }

    @Test
    fun `非OPPO厂商不识别`() {
        // 一加/realme/小米 即便文本含 Reno 也不算 Reno 档（品牌隔离）
        assertNull(RenoDeviceProfile.detectFamily("OnePlus", "Reno15", "Reno15"))
        assertNull(RenoDeviceProfile.detectFamily("realme", null, "realme Reno17"))
        assertNull(RenoDeviceProfile.detectFamily("Xiaomi", "Reno16", null))
    }

    @Test
    fun `OPPO非Reno15-17机型不识别`() {
        // Reno13/14（旧代际）与 Find 系列不误判
        assertNull(RenoDeviceProfile.detectFamily("OPPO", "OPPO Reno13 Pro 5G", "OPPO Reno13 Pro"))
        assertNull(RenoDeviceProfile.detectFamily("OPPO", "OPPO Reno14", null))
        assertNull(RenoDeviceProfile.detectFamily("OPPO", "CPH2617", "Find X10"))
    }

    @Test
    fun `空输入安全降级`() {
        assertNull(RenoDeviceProfile.detectFamily(null, null, null))
        assertNull(RenoDeviceProfile.detectFamily("", "", ""))
        assertNull(RenoDeviceProfile.detectFamily("OPPO", null, null))
    }

    // ═══════════════ 质感调校 ═══════════════

    @Test
    fun `Reno15-16-17档拍后锐化降权防双重锐化`() {
        // ColorOS 静态出片自带 HAL 锐化：Reno 档 ×0.8 降权
        assertEquals(0.8f, RenoDeviceProfile.sharpeningMultiplier(RenoDeviceProfile.Family.RENO15), 1e-6f)
        assertEquals(0.8f, RenoDeviceProfile.sharpeningMultiplier(RenoDeviceProfile.Family.RENO16), 1e-6f)
        assertEquals(0.8f, RenoDeviceProfile.sharpeningMultiplier(RenoDeviceProfile.Family.RENO17), 1e-6f)
    }

    @Test
    fun `未识别机型保持锐化1倍与旧引擎逐位一致`() {
        assertEquals(1.0f, RenoDeviceProfile.sharpeningMultiplier(null), 1e-6f)
    }

    @Test
    fun `慢快门稳拍提示边界`() {
        // null（AE 自动接管）不提示
        assertFalse(RenoDeviceProfile.needsSteadyHint(null))
        // 1/60s（16.7ms）快于安全线：不提示
        assertFalse(RenoDeviceProfile.needsSteadyHint(16_666_666L))
        // 恰好 1/30s：不提示（慢于才提示）
        assertFalse(RenoDeviceProfile.needsSteadyHint(RenoDeviceProfile.STEADY_SHUTTER_THRESHOLD_NS))
        // 1/25s（40ms）慢于安全线：提示
        assertTrue(RenoDeviceProfile.needsSteadyHint(40_000_000L))
        // 1/8s 长曝光：提示
        assertTrue(RenoDeviceProfile.needsSteadyHint(125_000_000L))
    }
}
