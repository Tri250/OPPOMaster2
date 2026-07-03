package com.silas.omaster.data.local

/**
 * 2.2.0 新增：权限状态数据模型
 *
 * 用于在 UI 层展示权限授予情况。
 * 配合 [com.silas.omaster.infrastructure.utils.PermissionChecker] 使用。
 */
enum class PermissionStatus {
    /** 已授予 */
    GRANTED,

    /** 被拒绝 */
    DENIED,

    /** 当前 Android 版本不需要此权限 */
    NOT_REQUIRED
}

/**
 * 单个权限的状态数据
 */
data class PermissionState(
    val key: String,
    val displayName: String,
    val status: PermissionStatus,
    val isRequired: Boolean = true,
    val description: String = ""
) {
    val isGranted: Boolean get() = status == PermissionStatus.GRANTED
    val isMissing: Boolean get() = status == PermissionStatus.DENIED
    val isOptional: Boolean get() = status == PermissionStatus.NOT_REQUIRED || !isRequired
}
