package com.silas.omaster.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale

/**
 * ColorOS 风格的点击反馈动画
 */
fun Modifier.clickableWithColorOSFeedback(
    onClick: () -> Unit,
    enabled: Boolean = true
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    
    // 监听按压状态
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    scale.animateTo(
                        0.96f,
                        animationSpec = tween(100, easing = FastOutSlowInEasing)
                    )
                }
                is PressInteraction.Release -> {
                    scale.animateTo(
                        1f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
                }
                is PressInteraction.Cancel -> {
                    scale.animateTo(
                        1f,
                        animationSpec = tween(150, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }
    
    val scaleValue by scale.asState()
    
    this
        .scale(scaleValue)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * OPPO 动画系统配置
 */
object OppoAnimationSystem {
    // 标准动画时长
    const val DURATION_SHORT = 100
    const val DURATION_MEDIUM = 200
    const val DURATION_LONG = 300
    
    // 弹性动画参数
    const val STIFFNESS_LOW = 100f
    const val STIFFNESS_MEDIUM = 200f
    const val STIFFNESS_HIGH = 400f
    
    // 缩放范围
    const val SCALE_PRESSED = 0.96f
    const val SCALE_RELEASED = 1f
    
    // 透明度变化
    const val ALPHA_VISIBLE = 1f
    const val ALPHA_DIMMED = 0.6f
    const val ALPHA_HIDDEN = 0f
}

/**
 * AnimationConfig - 动画配置
 */
data class AnimationConfig(
    val duration: Int = OppoAnimationSystem.DURATION_MEDIUM,
    val easing: Easing = FastOutSlowInEasing,
    val stiffness: Float = OppoAnimationSystem.STIFFNESS_MEDIUM,
    val dampingRatio: Float = 0.8f
) {
    companion object {
        val QUICK = AnimationConfig(
            duration = OppoAnimationSystem.DURATION_SHORT,
            easing = LinearEasing
        )
        
        val STANDARD = AnimationConfig(
            duration = OppoAnimationSystem.DURATION_MEDIUM,
            easing = FastOutSlowInEasing
        )
        
        val SMOOTH = AnimationConfig(
            duration = OppoAnimationSystem.DURATION_LONG,
            easing = FastOutSlowInEasing
        )
        
        val BOUNCE = AnimationConfig(
            duration = OppoAnimationSystem.DURATION_LONG,
            stiffness = OppoAnimationSystem.STIFFNESS_LOW,
            dampingRatio = 0.6f
        )
    }
    
    fun toTweenSpec(): TweenSpec<Float> = tween(
        durationMillis = duration,
        easing = easing
    )
    
    fun toSpringSpec(): SpringSpec<Float> = spring(
        stiffness = stiffness,
        dampingRatio = dampingRatio
    )
}