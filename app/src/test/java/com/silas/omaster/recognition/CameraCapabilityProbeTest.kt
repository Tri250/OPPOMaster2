package com.silas.omaster.recognition

import android.hardware.camera2.CameraCharacteristics
import com.silas.omaster.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CameraCapabilityProbe 纯逻辑单测（capabilityFromLevel 不依赖设备）。
 */
class CameraCapabilityProbeTest {

    @Test
    fun fullLevel_supportsManualAndFullPreset() {
        val cap = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, 4, null
        )
        assertEquals(CameraCapabilityProbe.Level.L1, cap.level)
        assertTrue(cap.isManualExposure)
        assertEquals(R.string.pop_hint_l1, cap.degradationHintRes)
    }

    @Test
    fun limitedLevel_manualIsoDependsOnOverride() {
        val withManual = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, 3, null,
            manualIsoOverride = true
        )
        assertEquals(CameraCapabilityProbe.Level.L2, withManual.level)
        assertTrue(withManual.supportsManualIso)
        // 未暴露曝光时间范围（ManualRanges.NONE）时不宣称支持手动快门
        assertFalse(withManual.supportsManualShutter)
        assertTrue(withManual.supportsExposureCompensation)
        assertEquals(R.string.pop_hint_l2, withManual.degradationHintRes)

        val withoutManual = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, 3, null,
            manualIsoOverride = false
        )
        assertFalse(withoutManual.supportsManualIso)
        assertFalse(withoutManual.isManualExposure)
    }

    @Test
    fun limitedLevel_manualShutterFollowsExposureTimeRange() {
        // OPPO Reno 等主摄为 LIMITED 但支持 AE_OFF + 曝光时间范围：手动快门可用，
        // 不再因 LIMITED 级别被硬编码降级（避免 ISO/快门被误标"无法套用"）
        val renoRanges = ManualRanges(
            isoRange = 50..3200,
            exposureTimeRange = 100_000L..32_000_000_000L,
            aeModeOffSupported = true
        )
        val cap = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, 3, null,
            manualIsoOverride = true, manualRanges = renoRanges
        )
        assertEquals(CameraCapabilityProbe.Level.L2, cap.level)
        assertTrue(cap.supportsManualIso)
        assertTrue(cap.supportsManualShutter)
        assertTrue(cap.isManualExposure)
    }

    @Test
    fun legacyLevel_degradesToStyleOnly() {
        val cap = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, 2, null
        )
        assertEquals(CameraCapabilityProbe.Level.L3, cap.level)
        assertFalse(cap.isManualExposure)
        assertEquals(R.string.pop_hint_l3, cap.degradationHintRes)
    }

    @Test
    fun unknownLevel_fallsBackToL3() {
        val cap = CameraCapabilityProbe.capabilityFromLevel(-1, 1, null)
        assertEquals(CameraCapabilityProbe.Level.L3, cap.level)
        assertEquals("UNKNOWN", cap.hardwareLevelString)
    }

    @Test
    fun evRangeIsComputedFromStep() {
        // probe() 内部用 lower*step..upper*step，验证 capabilityFromLevel 原样透传
        val cap = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, 4,
            evRange = (-3.0f to 3.0f)
        )
        assertEquals(-3.0f, cap.evRange!!.first, 0.001f)
        assertEquals(3.0f, cap.evRange!!.second, 0.001f)
    }
}
