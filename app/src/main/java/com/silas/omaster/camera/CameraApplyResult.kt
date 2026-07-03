package com.silas.omaster.camera

sealed class CameraApplyResult {
    data class Success(val method: ApplyMethod, val appliedParams: Map<String, Any>) : CameraApplyResult()
    data class PartialSuccess(val method: ApplyMethod, val appliedParams: Map<String, Any>, val failedParams: List<String>) : CameraApplyResult()
    data class Failed(val reason: String, val suggestion: String) : CameraApplyResult()
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
    val brand: DeviceBrand = DeviceBrand.GENERIC
)
