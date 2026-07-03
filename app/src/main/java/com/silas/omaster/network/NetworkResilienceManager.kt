package com.silas.omaster.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

/**
 * 网络韧性管理器
 *
 * 功能：
 * - 网络连接状态实时监控
 * - 离线缓存策略支持
 * - 网络恢复时自动重试
 * - 弱网环境降级处理
 * - UC-19: 指数退避重试 + 网络连通性预检 + 并发同步保护
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
     * UC-19: 并发同步保护标志——同一时刻只允许一个同步操作运行
     * 使用 AtomicBoolean 保证多线程安全，避免重复 sync 导致 ANR
     */
    private val isSyncInProgress = AtomicBoolean(false)

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

    /**
     * UC-19: 检查网络连通性——在发起同步前调用
     * @return true 表示有可用网络，可以发起同步
     */
    fun canPerformSync(): Boolean = isCurrentlyOnline()

    /**
     * UC-19: 尝试获取同步锁（防止并发重复同步）
     * @return true 表示成功获取锁，可执行同步；false 表示已有同步在进行中
     */
    fun tryAcquireSyncLock(): Boolean = isSyncInProgress.compareAndSet(false, true)

    /**
     * UC-19: 释放同步锁（同步完成或失败后必须调用）
     */
    fun releaseSyncLock() {
        isSyncInProgress.set(false)
    }

    /**
     * UC-19: 带指数退避的网络请求重试
     * 在弱网/断网环境下自动重试，避免直接在主线程阻塞导致 ANR
     *
     * @param maxRetries 最大重试次数
     * @param baseDelayMs 初始延迟（毫秒）
     * @param block 需要重试的挂起函数（必须在 IO 调度器执行）
     * @return 成功返回结果，失败返回 null
     */
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = 3,
        baseDelayMs: Long = 1000L,
        block: suspend () -> T
    ): T? {
        var lastException: Throwable? = null

        for (attempt in 0..maxRetries) {
            try {
                // 每次重试前检查网络状态
                if (!isCurrentlyOnline()) {
                    Log.w(TAG, "网络不可用，跳过重试 (attempt ${attempt + 1}/$maxRetries)")
                    break
                }
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    val delayMs = baseDelayMs * (2.0.pow(attempt.toDouble())).toLong()
                    Log.w(TAG, "请求失败，${delayMs}ms后重试 (${attempt + 1}/$maxRetries): ${e.message}")
                    delay(delayMs)
                }
            }
        }

        Log.e(TAG, "请求最终失败，已重试 $maxRetries 次", lastException)
        return null
    }
}