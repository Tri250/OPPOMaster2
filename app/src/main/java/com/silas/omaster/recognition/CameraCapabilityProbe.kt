package com.silas.omaster.recognition

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/**
 * 相机能力探测（决定 POP 即拍模式的可达等级）。
 *
 * 对应 PRD 第四/五章可达性分级：
 * - L1 FULL / LEVEL_3：Camera2 支持手动传感器（ISO/快门）→ 参数可写入拍摄管线；
 * - L2 LIMITED：仅支持部分手动（如 EV/对焦）→ 条件可达，参数部分生效；
 * - L3 LEGACY：仅自动模式 → 降级为"悬浮窗照着调 + 拍后套色"。
 *
 * 说明：Android 第三方 App 无法改写 OPPO 原生相机内部参数，本探测只针对
 * App 自研 Camera2 拍摄管线；系统相机参数回写属不可达边界（PRD 2.4）。
 */
object CameraCapabilityProbe {

    enum class Level { L1, L2, L3 }

    data class Capability(
        val level: Level,
        val hardwareLevelString: String,
        val supportsManualIso: Boolean,
        val supportsManualShutter: Boolean,
        val supportsExposureCompensation: Boolean,
        val evRange: Pair<Float, Float>?,
        val cameraCount: Int,
        val summary: String
    ) {
        val isManualExposure: Boolean get() = supportsManualIso && supportsManualShutter
        val degradationHint: String
            get() = when (level) {
                Level.L1 -> "支持手动参数，可完整套用预设"
                Level.L2 -> "部分手动可用，未生效项将提示"
                Level.L3 -> "相机能力受限，已切换为悬浮窗照着调 + 拍后套色"
            }
    }

    /** 探测当前设备主摄能力（不需要 CAMERA 权限，getCameraCharacteristics 可公开调用） */
    fun probe(context: Context): Capability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return capabilityFromLevel(-1, 0, null)
        }
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return capabilityFromLevel(-1, 0, null)
            val ids = manager.cameraIdList
            if (ids.isEmpty()) return capabilityFromLevel(-1, 0, null)
            val chars = manager.getCameraCharacteristics(ids.first())
            val level = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1
            val manualIso = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                ?.contains(CameraCharacteristics.CONTROL_AE_MODE_OFF) == true
            val evRangeObj = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val evStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val evRange = if (evRangeObj != null) {
                val step = evStep?.toFloat() ?: 1f
                evRangeObj.lower.toFloat() * step to evRangeObj.upper.toFloat() * step
            } else null
            capabilityFromLevel(level, ids.size, evRange, manualIso)
        } catch (e: Exception) {
            capabilityFromLevel(-1, 0, null)
        }
    }

    /** 纯逻辑：由硬件级别常量构造 Capability（可 JVM 单测） */
    fun capabilityFromLevel(
        hardwareLevel: Int,
        cameraCount: Int,
        evRange: Pair<Float, Float>?,
        manualIsoOverride: Boolean? = null
    ): Capability {
        val (level, supportsIso, supportsShutter, supportsEv) = when (hardwareLevel) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL,
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> {
                Quad(Level.L1, true, true, true)
            }
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> {
                Quad(Level.L2, manualIsoOverride ?: false, false, true)
            }
            else -> Quad(Level.L3, false, false, false)
        }
        val levelName = when (hardwareLevel) {
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
            else -> "UNKNOWN"
        }
        return Capability(
            level = level,
            hardwareLevelString = levelName,
            supportsManualIso = supportsIso,
            supportsManualShutter = supportsShutter,
            supportsExposureCompensation = supportsEv,
            evRange = evRange,
            cameraCount = cameraCount,
            summary = "相机等级 $levelName · 摄像头 $cameraCount"
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
