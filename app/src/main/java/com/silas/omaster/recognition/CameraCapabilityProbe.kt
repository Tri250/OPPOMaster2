package com.silas.omaster.recognition

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.StringRes
import com.silas.omaster.R

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
        /** 设备手动范围（POP 即拍写入拍摄参数的裁剪依据） */
        val manualRanges: ManualRanges = ManualRanges.NONE,
        /** 探测选中的相机 id（CameraX 绑定同一颗，避免探测与拍摄不是同一路摄像头） */
        val cameraId: String? = null
    ) {
        val isManualExposure: Boolean get() = supportsManualIso && supportsManualShutter

        /** 降级提示文案（经资源 ID 本地化，避免硬编码中文） */
        @get:StringRes
        val degradationHintRes: Int
            get() = when (level) {
                Level.L1 -> R.string.pop_hint_l1
                Level.L2 -> R.string.pop_hint_l2
                Level.L3 -> R.string.pop_hint_l3
            }
    }

    /**
     * 探测当前设备主摄能力（不需要 CAMERA 权限，getCameraCharacteristics 可公开调用）。
     *
     * v1.7.5 修复：多摄机型（OPPO Reno 等）的 cameraIdList 常以超广角/微距等副摄开头，
     * 且副摄多被标为 LIMITED / LEGACY。旧实现直接取 first()，导致探测到"无手动曝光能力"
     * 而把预设 ISO/快门一律判为无法套用。现改为择优：后置优先、支持手动传感器优先、
     * 关闭 AE 可用优先、硬件级别高优先，并回传 cameraId 供 CameraX 绑定同一颗摄像头。
     */
    fun probe(context: Context): Capability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return capabilityFromLevel(-1, 0, null)
        }
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return capabilityFromLevel(-1, 0, null)
            val ids = manager.cameraIdList
            if (ids.isEmpty()) return capabilityFromLevel(-1, 0, null)

            val candidates = ids.mapNotNull { id ->
                val c = runCatching { manager.getCameraCharacteristics(id) }.getOrNull()
                    ?: return@mapNotNull null
                val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?.toSet() ?: emptySet()
                Candidate(
                    id = id,
                    chars = c,
                    facing = c.get(CameraCharacteristics.LENS_FACING),
                    level = c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ?: -1,
                    manualSensor = caps.contains(
                        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                    ),
                    aeOff = c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
                        ?.contains(CameraCharacteristics.CONTROL_AE_MODE_OFF) == true
                )
            }
            val back = candidates.filter { it.facing == CameraCharacteristics.LENS_FACING_BACK }
            val pool = if (back.isNotEmpty()) back else candidates
            if (pool.isEmpty()) return capabilityFromLevel(-1, 0, null)
            val best = pool.maxWithOrNull(compareBy<Candidate>(
                { it.manualSensor }, { it.aeOff }, { levelRank(it.level) }
            ))!!

            val chars = best.chars
            val evRangeObj = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val evStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val isoRangeObj = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val exposureRangeObj = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val evRange = if (evRangeObj != null) {
                val step = evStep?.toFloat() ?: 1f
                evRangeObj.lower.toFloat() * step to evRangeObj.upper.toFloat() * step
            } else null
            val manualRanges = ManualRanges(
                isoRange = isoRangeObj?.let { it.lower..it.upper },
                exposureTimeRange = exposureRangeObj?.let { it.lower..it.upper },
                evStepRange = evRangeObj?.let { it.lower..it.upper },
                evStepEv = evStep?.toFloat()?.takeIf { it > 0f },
                aeModeOffSupported = best.aeOff
            )
            capabilityFromLevel(
                best.level, ids.size, evRange, best.aeOff, manualRanges
            ).copy(cameraId = best.id)
        } catch (e: Exception) {
            capabilityFromLevel(-1, 0, null)
        }
    }

    private data class Candidate(
        val id: String,
        val chars: CameraCharacteristics,
        val facing: Int?,
        val level: Int,
        val manualSensor: Boolean,
        val aeOff: Boolean
    )

    private fun levelRank(level: Int): Int = when (level) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> 3
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> 2
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> 1
        else -> 0
    }

    /** 纯逻辑：由硬件级别常量构造 Capability（可 JVM 单测） */
    fun capabilityFromLevel(
        hardwareLevel: Int,
        cameraCount: Int,
        evRange: Pair<Float, Float>?,
        manualIsoOverride: Boolean? = null,
        manualRanges: ManualRanges = ManualRanges.NONE
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
            manualRanges = manualRanges
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
