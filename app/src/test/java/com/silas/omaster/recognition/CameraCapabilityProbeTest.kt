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
