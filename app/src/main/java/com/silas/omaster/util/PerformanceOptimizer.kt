package com.silas.omaster.util

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.util.Log
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 性能优化工具类
 *
 * 提供启动优化、内存优化、列表滚动优化等功能
 */
object PerformanceOptimizer {

    private const val TAG = "PerformanceOptimizer"

    /**
     * 初始化严格模式（Debug模式启用，Release模式禁用）
     */
    fun initStrictMode(isDebug: Boolean) {
        if (!isDebug) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen() // 在屏幕上闪烁提示
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build()
        )

        Log.d(TAG, "StrictMode 已启用")
    }

    /**
     * 获取设备性能等级
     */
    fun getDevicePerformanceLevel(): PerformanceLevel {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / (1024 * 1024) // MB
        val processors = runtime.availableProcessors()

        return when {
            maxMemory >= 4096 && processors >= 8 -> PerformanceLevel.HIGH
            maxMemory >= 2048 && processors >= 4 -> PerformanceLevel.MEDIUM
            else -> PerformanceLevel.LOW
        }
    }

    /**
     * 根据性能等级获取列表缓存大小
     */
    fun getListCacheSize(): Int {
        return when (getDevicePerformanceLevel()) {
            PerformanceLevel.HIGH -> 10
            PerformanceLevel.MEDIUM -> 5
            PerformanceLevel.LOW -> 3
        }
    }

    /**
     * 获取图片加载质量
     */
    fun getImageQuality(): ImageQuality {
        return when (getDevicePerformanceLevel()) {
            PerformanceLevel.HIGH -> ImageQuality.HIGH
            PerformanceLevel.MEDIUM -> ImageQuality.MEDIUM
            PerformanceLevel.LOW -> ImageQuality.LOW
        }
    }

    /**
     * 是否应该启用动画效果
     */
    fun shouldEnableAnimations(): Boolean {
        return getDevicePerformanceLevel() != PerformanceLevel.LOW
    }

    /**
     * 是否应该启用图片过渡动画
     */
    fun shouldEnableImageTransitions(): Boolean {
        return getDevicePerformanceLevel() == PerformanceLevel.HIGH
    }

    /**
     * 获取推荐的图片采样率
     */
    fun getRecommendedSampleRate(): Int {
        return when (getDevicePerformanceLevel()) {
            PerformanceLevel.HIGH -> 1
            PerformanceLevel.MEDIUM -> 2
            PerformanceLevel.LOW -> 4
        }
    }

    /**
     * 清理内存缓存
     */
    fun trimMemory(context: Context, level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 运行时内存紧张，清理部分缓存
                Log.d(TAG, "运行时内存紧张，清理缓存: level=$level")
                ImageCacheManager.trimCache(context)
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // UI 不可见，可以清理更多缓存
                Log.d(TAG, "UI 隐藏，清理缓存")
                ImageCacheManager.clearMemoryCache(context)
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // 系统内存严重不足，清理所有缓存
                Log.w(TAG, "系统内存严重不足，清理所有缓存: level=$level")
                ImageCacheManager.clearCache(context)
            }
        }
    }
}

/**
 * 设备性能等级
 */
enum class PerformanceLevel {
    HIGH,   // 高端设备：8核+，4GB+内存
    MEDIUM, // 中端设备：4核+，2GB+内存
    LOW     // 低端设备：其他
}

/**
 * 图片质量设置
 */
enum class ImageQuality {
    HIGH,   // 原图质量
    MEDIUM, // 中等压缩
    LOW     // 高压缩
}

/**
 * 列表滚动性能优化
 *
 * 使用 derivedStateOf 和 snapshotFlow 优化滚动状态监听
 */
@OptIn(FlowPreview::class)
@Composable
fun LazyStaggeredGridState.optimizedScrollState(
    onScrollStateChanged: (isScrollingUp: Boolean) -> Unit = {}
) {
    val previousIndex = remember { mutableStateOf(0) }
    val previousScrollOffset = remember { mutableStateOf(0) }

    LaunchedEffect(this) {
        snapshotFlow {
            firstVisibleItemIndex to firstVisibleItemScrollOffset
        }
            .debounce(16) // 约60fps的刷新率
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                val isUp = currentIndex < previousIndex.value ||
                        (currentIndex == previousIndex.value && currentOffset <= previousScrollOffset.value)
                previousIndex.value = currentIndex
                previousScrollOffset.value = currentOffset
                onScrollStateChanged(isUp)
            }
    }
}

/**
 * 优化后的首次可见项索引
 */
@Composable
fun LazyStaggeredGridState.visibleStartIndex(): Int {
    return remember {
        derivedStateOf {
            layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        }
    }.value
}

/**
 * 优化后的首次可见项索引（LazyGrid）
 */
@Composable
fun LazyGridState.visibleStartIndex(): Int {
    return remember {
        derivedStateOf {
            layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
        }
    }.value
}

/**
 * 内存优化组件回调
 */
import android.content.ComponentCallbacks2

class MemoryOptimizer(private val context: Context) : ComponentCallbacks2 {

    override fun onTrimMemory(level: Int) {
        PerformanceOptimizer.trimMemory(context, level)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        // 配置变化时的处理
    }

    override fun onLowMemory() {
        // 低内存时的紧急处理
        ImageCacheManager.clearCache(context)
    }
}
