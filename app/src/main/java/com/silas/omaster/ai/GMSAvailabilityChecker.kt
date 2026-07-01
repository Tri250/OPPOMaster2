package com.silas.omaster.ai

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Google Mobile Services (GMS) 可用性检测器
 *
 * 用于检测设备是否安装了 Google Play Services，
 * 在无 GMS 设备（华为/荣耀等）上提供本地 TFLite 模型降级方案。
 *
 * 使用方式：
 * ```kotlin
 * val checker = GMSAvailabilityChecker(context)
 * if (checker.isGMSAvailable()) {
 *     // 使用 ML Kit（依赖 GMS）
 * } else {
 *     // 使用本地 TFLite 模型降级
 * }
 * ```
 */
object GMSAvailabilityChecker {

    private const val TAG = "GMSAvailability"

    /**
     * GMS 可用性状态
     */
    enum class Status {
        AVAILABLE,            // 完全可用
        UPDATE_REQUIRED,      // 需要更新
        DISABLED,             // 已禁用
        INVALID,              // 无效
        UNAVAILABLE,          // 不可用（无 GMS 设备）
        UNKNOWN               // 检测失败
    }

    private var cachedStatus: Status? = null
    private var cachedErrorCode: Int = 0

    /**
     * 检测 GMS 可用性（带缓存）
     */
    fun check(context: Context): Status {
        cachedStatus?.let { return it }

        return try {
            val api = GoogleApiAvailability.getInstance()
            val resultCode = api.isGooglePlayServicesAvailable(context.applicationContext)
            cachedErrorCode = resultCode

            cachedStatus = when (resultCode) {
                ConnectionResult.SUCCESS -> {
                    Log.d(TAG, "GMS 可用")
                    Status.AVAILABLE
                }
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> {
                    Log.w(TAG, "GMS 需要更新")
                    Status.UPDATE_REQUIRED
                }
                ConnectionResult.SERVICE_DISABLED -> {
                    Log.w(TAG, "GMS 已禁用")
                    Status.DISABLED
                }
                ConnectionResult.SERVICE_INVALID -> {
                    Log.w(TAG, "GMS 无效")
                    Status.INVALID
                }
                else -> {
                    Log.w(TAG, "GMS 不可用，错误码: $resultCode")
                    Status.UNAVAILABLE
                }
            }
            cachedStatus!!
        } catch (e: Exception) {
            Log.e(TAG, "GMS 检测失败", e)
            cachedStatus = Status.UNKNOWN
            Status.UNKNOWN
        }
    }

    /**
     * GMS 是否完全可用
     */
    fun isGMSAvailable(context: Context): Boolean {
        return check(context) == Status.AVAILABLE
    }

    /**
     * ML Kit 是否可用（GMS 可用或结果可接受）
     */
    fun isMLKitAvailable(context: Context): Boolean {
        val status = check(context)
        return status == Status.AVAILABLE || status == Status.UPDATE_REQUIRED
    }

    /**
     * 获取错误码（用于错误提示）
     */
    fun getErrorCode(): Int = cachedErrorCode

    /**
     * 获取可读的错误描述
     */
    fun getErrorMessage(context: Context): String {
        return when (check(context)) {
            Status.AVAILABLE -> "Google Play Services 可用"
            Status.UPDATE_REQUIRED -> "Google Play Services 需要更新"
            Status.DISABLED -> "Google Play Services 已禁用"
            Status.INVALID -> "Google Play Services 版本无效"
            Status.UNAVAILABLE -> "Google Play Services 不可用，部分 AI 功能将被限制"
            Status.UNKNOWN -> "无法检测 Google Play Services 状态"
        }
    }

    /**
     * 清除缓存（强制重新检测）
     */
    fun reset() {
        cachedStatus = null
        cachedErrorCode = 0
    }
}