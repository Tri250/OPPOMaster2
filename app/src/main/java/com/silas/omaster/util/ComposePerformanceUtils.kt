package com.silas.omaster.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Compose 性能优化工具集
 *
 * 包含常用的性能优化模式和工具方法
 */
object ComposePerformanceUtils {

    /**
     * 延迟初始化块
     * 仅在首次访问时执行初始化逻辑
     */
    @Composable
    inline fun <T> lazyInit(crossinline initializer: () -> T): T {
        return remember { initializer() }
    }

    /**
     * 节流点击处理
     * 防止快速重复点击
     */
    @Composable
    fun throttledClick(
        throttleTimeMs: Long = 300,
        onClick: () -> Unit
    ): () -> Unit {
        var lastClickTime = remember { 0L }
        return {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= throttleTimeMs) {
                lastClickTime = currentTime
                onClick()
            }
        }
    }

    /**
     * 防抖点击处理
     * 仅在停止点击一段时间后执行
     */
    @Composable
    fun debouncedClick(
        debounceTimeMs: Long = 300,
        coroutineScope: CoroutineScope,
        onClick: () -> Unit
    ): () -> Unit {
        var lastJob = remember { kotlinx.coroutines.Job() }
        return {
            lastJob.cancel()
            lastJob = coroutineScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(debounceTimeMs)
                onClick()
            }
        }
    }
}

/**
 * 记忆化计算
 * 仅当依赖项变化时重新计算
 */
@Composable
inline fun <T> memo(vararg dependencies: Any?, crossinline calculation: () -> T): T {
    return remember(dependencies) { calculation() }
}

/**
 * 条件性记忆化
 * 仅在条件满足时记忆值
 */
@Composable
inline fun <T> rememberIf(condition: Boolean, crossinline calculation: () -> T): T {
    return if (condition) {
        remember { calculation() }
    } else {
        calculation()
    }
}
