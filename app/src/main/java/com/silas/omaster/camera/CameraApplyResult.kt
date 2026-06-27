package com.silas.omaster.camera

sealed class CameraApplyResult {
    abstract val userMessage: String

    data class Success(
        val method: ApplyMethod,
        val appliedParams: Map<String, Any>,
        override val userMessage: String = ""
    ) : CameraApplyResult()

    data class PartialSuccess(
        val method: ApplyMethod,
        val appliedParams: Map<String, Any>,
        val failedParams: List<String>,
        override val userMessage: String = ""
    ) : CameraApplyResult()

    data class Failed(
        val reason: String,
        val suggestion: String,
        override val userMessage: String = ""
    ) : CameraApplyResult()
}

/**
 * 将 [CameraApplyResult] 转换为面向用户的提示文案。
 *
 * @param capability 当前设备能力检测结果，用于生成更精准的降级说明
 */
fun CameraApplyResult.toUserMessage(capability: DeviceCapability): String {
    return when (this) {
        is CameraApplyResult.Success -> {
            when (method) {
                ApplyMethod.CONTENT_PROVIDER -> "已成功将参数写入 OPPO 大师模式相机"
                ApplyMethod.SYSTEM_SETTINGS -> "已通过系统设置将参数应用到 OPPO 相机"
                ApplyMethod.CAMERA_INTENT -> "已启动 OPPO 相机大师模式并带入参数"
                ApplyMethod.CLIPBOARD_FALLBACK -> "当前设备/系统版本不支持直接写入相机，参数已复制到剪贴板，请进入相机大师模式手动粘贴设置"
            }
        }
        is CameraApplyResult.PartialSuccess -> {
            val base = when (method) {
                ApplyMethod.CONTENT_PROVIDER -> "部分参数已通过 ContentProvider 写入相机"
                ApplyMethod.SYSTEM_SETTINGS -> "部分参数已通过系统设置应用"
                ApplyMethod.CAMERA_INTENT -> "部分参数已带入 OPPO 相机"
                ApplyMethod.CLIPBOARD_FALLBACK -> "参数已复制到剪贴板"
            }
            "$base，未成功项：${failedParams.joinToString(", ")}"
        }
        is CameraApplyResult.Failed -> userMessage.ifBlank { suggestion.ifBlank { reason } }
    } + capability.userFacingHint.takeIf { it.isNotBlank() && method == ApplyMethod.CLIPBOARD_FALLBACK }
        ?.let { "（$it）" }
        .orEmpty()
}

enum class ApplyMethod {
    CONTENT_PROVIDER,   // OPPO Master Mode ContentProvider
    SYSTEM_SETTINGS,    // Settings.System/Global
    CAMERA_INTENT,      // OPPO Camera Intent
    CLIPBOARD_FALLBACK  // Clipboard copy
}

data class DeviceCapability(
    val manufacturer: String,
    val model: String,
    val isOppoDevice: Boolean,
    val isFindXSeries: Boolean,
    val supportsMasterMode: Boolean,
    val supportsContentProvider: Boolean,
    val userFacingHint: String
)
