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

    @Test
    fun 多后摄机型选择感光面积最大的主摄() {
        // 【v1.8.3 Reno17 适配】cameraIdList 顺序不保证主摄在前：
        // 超广角面积小（1/4" ≈ 40mm²），主摄最大（1/1.56" ≈ 100mm²），长焦居中
        val ids = listOf("0", "1", "2", "3") // 3=主摄，排在最后
        val areas = listOf<Float?>(40f, null, 60f, 100f)
        assertEquals("3", CameraCapabilityProbe.chooseMainBackCameraId(ids, areas))
        // 面积全部未知 → 取先出现者（保持旧行为，不因缺失元数据误选）
        assertEquals("0", CameraCapabilityProbe.chooseMainBackCameraId(ids, listOf(null, null, null, null)))
        // 面积并列 → 取先出现者
        assertEquals("0", CameraCapabilityProbe.chooseMainBackCameraId(listOf("0", "1"), listOf(100f, 100f)))
        // 无后摄候选 → null，由调用方回退 cameraIdList 首个
        assertEquals(null, CameraCapabilityProbe.chooseMainBackCameraId(emptyList(), emptyList()))
    }

    @Test
    fun 能力卡携带主摄id供拍摄绑定对齐() {
        val cap = CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, 4, null,
            manualIsoOverride = true,
            manualRanges = ManualRanges(isoRange = 100..6400, exposureTimeRange = 100_000L..1_000_000_000L,
                aeModeOffSupported = true),
            cameraId = "3"
        )
        assertEquals("3", cap.cameraId)
        // 未探测到 id 时为空，界面回退 CameraSelector.DEFAULT_BACK_CAMERA
        assertEquals(null, CameraCapabilityProbe.capabilityFromLevel(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, 1, null).cameraId)
    }
}
