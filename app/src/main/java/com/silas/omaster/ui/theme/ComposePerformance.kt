package com.silas.omaster.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Compose 性能配置
 *
 * 提供可复用的 CompositionLocal 和性能相关配置，
 * 减少不必要的重组，提升 UI 渲染性能。
 */

// ==================== CompositionLocals ====================

/**
 * 是否启用高级渲染效果（阴影、模糊等）
 * 低端设备可设置为 false 以提升性能
 */
val LocalRenderEffectsEnabled = staticCompositionLocalOf { true }

/**
 * 是否启用动画
 * 可在低内存时禁用动画以提升流畅度
 */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/**
 * 列表预加载数量
 * 用于 LazyList/LazyColumn 的 beyondViewportPageCount
 */
val LocalPrefetchPageCount = staticCompositionLocalOf { 1 }

/**
 * 图片加载质量（0.0-1.0）
 * 低内存时可降低图片质量
 */
val LocalImageQuality = staticCompositionLocalOf { 1.0f }

/**
 * 当前设备性能等级
 */
@Immutable
enum class DevicePerformanceTier {
    /** 高端设备：启用所有效果 */
    HIGH,
    /** 中端设备：部分效果降级 */
    MEDIUM,
    /** 低端设备：最小化效果 */
    LOW
}

val LocalDevicePerformanceTier = staticCompositionLocalOf { DevicePerformanceTier.HIGH }

/**
 * 性能配置提供者
 * 根据设备性能等级自动配置各项参数
 */
@Composable
fun PerformanceConfigProvider(
    tier: DevicePerformanceTier = DevicePerformanceTier.HIGH,
    content: @Composable () -> Unit
) {
    val renderEffectsEnabled = tier != DevicePerformanceTier.LOW
    val animationsEnabled = tier != DevicePerformanceTier.LOW
    val prefetchPageCount = when (tier) {
        DevicePerformanceTier.HIGH -> 2
        DevicePerformanceTier.MEDIUM -> 1
        DevicePerformanceTier.LOW -> 0
    }
    val imageQuality = when (tier) {
        DevicePerformanceTier.HIGH -> 1.0f
        DevicePerformanceTier.MEDIUM -> 0.8f
        DevicePerformanceTier.LOW -> 0.6f
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalRenderEffectsEnabled provides renderEffectsEnabled,
        LocalAnimationsEnabled provides animationsEnabled,
        LocalPrefetchPageCount provides prefetchPageCount,
        LocalImageQuality provides imageQuality,
        LocalDevicePerformanceTier provides tier,
        content = content
    )
}

// ==================== 可复用 CompositionLocals ====================

/**
 * 可复用的格式化字符串提供者
 * 避免每次重组都创建新的格式化对象
 */
@Immutable
data class FormattedStrings(
    val emptyState: String = "探索哈苏大师配方库",
    val noPresets: String = "暂无预设",
    val loading: String = "加载中...",
    val error: String = "出错了"
)

val LocalFormattedStrings = compositionLocalOf { FormattedStrings() }

/**
 * 可复用的间距配置
 * 避免在 Composable 中直接使用硬编码的 dp 值
 */
@Immutable
data class SpacingConfig(
    val small: Int = 4,
    val medium: Int = 8,
    val large: Int = 16,
    val xlarge: Int = 24,
    val cardPadding: Int = 16,
    val listSpacing: Int = 12
)

val LocalSpacingConfig = staticCompositionLocalOf { SpacingConfig() }

/**
 * 可复用的动画时长配置
 */
@Immutable
data class AnimationConfig(
    val shortDurationMs: Int = 150,
    val mediumDurationMs: Int = 300,
    val longDurationMs: Int = 500,
    val pageTransitionMs: Int = 350
)

val LocalAnimationConfig = staticCompositionLocalOf { AnimationConfig() }

/**
 * 列表性能配置
 */
@Immutable
data class ListPerformanceConfig(
    val prefetchCount: Int = 1,
    val initialLoadSize: Int = 20,
    val pageSize: Int = 20,
    val enablePlaceholder: Boolean = true
)

val LocalListPerformanceConfig = staticCompositionLocalOf { ListPerformanceConfig() }

// ==================== 性能工具函数 ====================

/**
 * 根据设备性能等级获取合适的列表配置
 */
@Composable
fun rememberListPerformanceConfig(): ListPerformanceConfig {
    val tier = LocalDevicePerformanceTier.current
    return remember(tier) {
        when (tier) {
            DevicePerformanceTier.HIGH -> ListPerformanceConfig(
                prefetchCount = 2,
                initialLoadSize = 30,
                pageSize = 30
            )
            DevicePerformanceTier.MEDIUM -> ListPerformanceConfig(
                prefetchCount = 1,
                initialLoadSize = 20,
                pageSize = 20
            )
            DevicePerformanceTier.LOW -> ListPerformanceConfig(
                prefetchCount = 0,
                initialLoadSize = 10,
                pageSize = 10,
                enablePlaceholder = false
            )
        }
    }
}

/**
 * 根据设备性能等级获取合适的图片加载配置
 */
@Composable
fun rememberImagePerformanceConfig(): Pair<Float, Int> {
    val tier = LocalDevicePerformanceTier.current
    return remember(tier) {
        when (tier) {
            DevicePerformanceTier.HIGH -> 1.0f to 0
            DevicePerformanceTier.MEDIUM -> 0.8f to 2
            DevicePerformanceTier.LOW -> 0.6f to 4
        }
    }
}