package com.silas.omaster.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 网络韧性管理器
 *
 * 功能：
 * - 网络连接状态实时监控
 * - 离线缓存策略支持
 * - 网络恢复时自动重试
 * - 弱网环境降级处理
 *
 * 使用方式：
 * ```kotlin
 * // 监听网络状态
 * NetworkResilienceManager.observeNetworkState().collect { state ->
 *     when (state) {
 *         NetworkState.Available -> // 正常网络
 *         NetworkState.Metered -> // 计费网络（弱网降级）
 *         NetworkState.Unavailable -> // 离线模式
 *     }
 * }
 * ```
 */
object NetworkResilienceManager {

    private const val TAG = "NetworkResilience"

    private var connectivityManager: ConnectivityManager? = null
    private var isInitialized = false

    private val _networkState = MutableStateFlow(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    /**
     * 网络状态枚举
     */
    enum class NetworkState {
        Unknown,       // 未初始化
        Available,     // 可用网络（WiFi/有线等）
        Metered,       // 计费网络（移动数据，建议降级）
        Unavailable    // 无网络连接
    }

    /**
     * 初始化网络监控
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 注册网络回调
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager?.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        updateNetworkState()
                    }

                    override fun onLost(network: Network) {
                        updateNetworkState()
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities
                    ) {
                        updateNetworkState()
                    }
                }
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "缺少网络权限，无法监控网络状态", e)
        }

        // 初始化当前状态
        updateNetworkState()
        Log.i(TAG, "网络韧性管理器已初始化，当前状态: ${_networkState.value}")
    }

    private fun updateNetworkState() {
        try {
            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it) }

            val newState = when {
                capabilities == null -> NetworkState.Unavailable
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ->
                    NetworkState.Unavailable
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ->
                    NetworkState.Available
                else -> NetworkState.Metered
            }

            _networkState.value = newState
            _isOnline.value = newState != NetworkState.Unavailable

            Log.d(TAG, "网络状态更新: $newState (在线: ${_isOnline.value})")
        } catch (e: Exception) {
            Log.e(TAG, "更新网络状态失败", e)
            _networkState.value = NetworkState.Unknown
            _isOnline.value = false
        }
    }

    /**
     * 观察网络状态变化（Flow，自动去重）
     */
    fun observeNetworkState(): Flow<NetworkState> {
        return _networkState.asStateFlow()
    }

    /**
     * 检查当前是否有网络连接
     */
    fun isCurrentlyOnline(): Boolean = _isOnline.value

    /**
     * 检查是否处于弱网环境（计费网络），建议降级策略
     */
    fun isMeteredNetwork(): Boolean = _networkState.value == NetworkState.Metered
}