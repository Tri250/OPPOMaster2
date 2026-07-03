package com.silas.omaster.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.theme.ColorOS16Palette
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.LiquidGlassConfig
import com.silas.omaster.infrastructure.utils.perform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

/**
 * 液态玻璃设计系统组件库
 * 基于 ColorOS 16 设计规范，提供完整的玻璃态组件
 *
 * 包含：
 * - GlassCard: 玻璃卡片
 * - GlassButton: 玻璃按钮
 * - GlassIconButton: 玻璃图标按钮
 * - GlassTopBar: 玻璃顶部栏
 * - GlassChip: 玻璃标签
 */

// ==================== 液态玻璃 Modifier ====================

/**
 * 增强版液态玻璃效果 Modifier
 * 包含：半透明背景、渐变边框、顶部高光、模糊效果（API 31+）
 *
 * @param cornerRadius 圆角半径
 * @param backgroundColor 背景色
 * @param borderColor 边框色
 * @param showHighlight 是否显示顶部高光
 * @param blurRadius 模糊半径（dp），仅 API 31+ 生效
 */
@Composable
fun Modifier.liquidGlass(
    cornerRadius: Dp = Dp(LiquidGlassConfig.CornerRadius),
    backgroundColor: Color = ColorOS16Palette.GlassDarkSurface,
    borderColor: Color = ColorOS16Palette.GlassDarkBorder,
    showHighlight: Boolean = true,
    blurRadius: Dp = Dp(LiquidGlassConfig.BlurRadius)
): Modifier = this
    .then(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(
                    blurRadius.toPx(),
                    blurRadius.toPx(),
                    Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        } else {
            Modifier
        }
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .then(
        if (showHighlight) {
            Modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ColorOS16Palette.GlassDarkHighlight,
                        Color.Transparent
                    ),
                    endY = 0.3f
                )
            )
        } else {
            Modifier
        }
    )
    .border(
        width = Dp(LiquidGlassConfig.BorderWidth),
        color = borderColor,
        shape = RoundedCornerShape(cornerRadius)
    )

/**
 * 哈苏橙主题液态玻璃效果
 * 使用哈苏橙作为边框高亮色
 */
@Composable
fun Modifier.hasselbladGlass(
    cornerRadius: Dp = 24.dp,
    intensity: Float = 1f
): Modifier = this.liquidGlass(
    cornerRadius = cornerRadius,
    backgroundColor = ColorOS16Palette.GlassDarkSurface,
    borderColor = HasselbladOrange.copy(alpha = 0.3f * intensity)
)

// ==================== GlassCard 玻璃卡片 ====================

/**
 * 玻璃态卡片组件
 *
 * @param onClick 点击回调，null 则不可点击
 * @param modifier 修饰符
 * @param shape 形状
 * @param elevation 高度
 * @param content 内容
 */
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = AnimationSpecs.ColorOS16MicroSpring,
        label = "glass_card_scale"
    )

    Card(
        onClick = {
            onClick?.let {
                haptic.perform(HapticFeedbackType.LongPress)
                it()
            }
        },
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = ColorOS16Palette.GlassDarkSurface
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = ColorOS16Palette.GlassDarkBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        interactionSource = interactionSource
    ) {
        Box {
            content()
            // 顶部高光
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ColorOS16Palette.GlassDarkHighlight,
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

// ==================== GlassButton 玻璃按钮 ====================

/**
 * 玻璃态主按钮
 *
 * @param text 按钮文字
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param icon 左侧图标
 * @param isEnabled 是否启用
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = AnimationSpecs.ColorOS16BouncySpring,
        label = "glass_button_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isEnabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            HasselbladOrange.copy(alpha = 0.9f),
                            HasselbladOrange.copy(alpha = 0.7f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            ColorOS16Palette.GlassDarkSurface,
                            ColorOS16Palette.GlassDarkSurface
                        )
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) Color.White else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 玻璃态次按钮
 */
@Composable
fun GlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    isEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = AnimationSpecs.ColorOS16BouncySpring,
        label = "glass_secondary_button_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlass(cornerRadius = 16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                it()
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                }
            )
        }
    }
}

// ==================== GlassIconButton 玻璃图标按钮 ====================

/**
 * 玻璃态图标按钮
 *
 * @param icon 图标
 * @param contentDescription 内容描述
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param size 尺寸
 */
@Composable
fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = AnimationSpecs.ColorOS16MicroSpring,
        label = "glass_icon_button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(ColorOS16Palette.GlassDarkSurface)
            .border(
                width = 0.5.dp,
                color = ColorOS16Palette.GlassDarkBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

// ==================== GlassChip 玻璃标签 ====================

/**
 * 玻璃态标签组件
 *
 * @param text 标签文字
 * @param selected 是否选中
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AnimationSpecs.ColorOS16MicroSpring,
        label = "glass_chip_scale"
    )

    val backgroundColor = if (selected) {
        HasselbladOrange.copy(alpha = 0.2f)
    } else {
        ColorOS16Palette.GlassDarkSurface
    }
    val borderColor = if (selected) {
        HasselbladOrange.copy(alpha = 0.5f)
    } else {
        ColorOS16Palette.GlassDarkBorder
    }
    val textColor = if (selected) {
        HasselbladOrange
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ==================== GlassTopBar 玻璃顶部栏 ====================

/**
 * 玻璃态顶部导航栏
 *
 * @param title 标题
 * @param onBack 返回回调
 * @param modifier 修饰符
 * @param actions 右侧操作
 */
@Composable
fun GlassTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorOS16Palette.GlassDarkSurface)
            .border(
                width = 0.5.dp,
                color = ColorOS16Palette.GlassDarkBorder,
                shape = RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            onBack?.let {
                GlassIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    onClick = it,
                    size = 40.dp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
        // 底部渐变分隔
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            HasselbladOrange.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ==================== GlassListItem 玻璃列表项 ====================

/**
 * 玻璃态列表项组件
 *
 * @param headline 主标题
 * @param modifier 修饰符
 * @param supportingText 副标题
 * @param leadingIcon 前置图标
 * @param trailingContent 尾部内容
 * @param onClick 点击回调
 */
@Composable
fun GlassListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.99f else 1f,
        animationSpec = AnimationSpecs.ColorOS16MicroSpring,
        label = "glass_list_item_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(ColorOS16Palette.GlassDarkSurface)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            haptic.perform(HapticFeedbackType.LongPress)
                            onClick()
                        }
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = 0.5.dp,
                color = ColorOS16Palette.GlassDarkBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                it()
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                supportingText?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailingContent?.let {
                Spacer(modifier = Modifier.width(16.dp))
                it()
            }
        }
    }
}

// ==================== GlassDivider 玻璃分隔线 ====================

/**
 * 玻璃态分隔线
 */
@Composable
fun GlassDivider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ColorOS16Palette.GlassDarkBorder,
                        Color.Transparent
                    )
                )
            )
    )
}
