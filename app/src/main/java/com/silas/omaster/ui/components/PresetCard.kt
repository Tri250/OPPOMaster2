package com.silas.omaster.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.BorderDefault
import com.silas.omaster.ui.theme.BorderHighlight
import com.silas.omaster.ui.theme.BorderLight
import com.silas.omaster.ui.theme.BorderPressed
import com.silas.omaster.ui.theme.CardTitleStyle
import com.silas.omaster.ui.theme.GlowOrange
import com.silas.omaster.ui.theme.GradientOrangeEnd
import com.silas.omaster.ui.theme.GradientOrangeStart
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.TagTextStyle
import com.silas.omaster.ui.theme.TextPrimary
import com.silas.omaster.ui.theme.TextSecondary
import com.silas.omaster.ui.theme.TextTertiary
import com.silas.omaster.ui.theme.Zinc800
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.perform

// ============================================
// 圆角规范 (设计规范)
// ============================================
private val CardCornerRadius = 16.dp          // 卡片圆角 - 16dp
private val ImageCornerRadius = 16.dp         // 图片圆角 - 16dp (顶部圆角)
private val ButtonCornerRadius = 12.dp        // 按钮圆角 - 12dp
private val BadgeCornerRadius = 6.dp          // 徽章圆角 - 6dp
private val IconCornerRadius = 12.dp          // 图标容器圆角 - 12dp

// ============================================
// 间距规范
// ============================================
private val CardPadding = 16.dp               // 卡片内边距
private val CardContentSpacing = 12.dp        // 卡片内容间距
private val ImageHeightMultiplier = 1f        // 图片高度占比

// ============================================
// 阴影规范
// ============================================
private val DefaultShadowElevation = 4.dp
private val PressedShadowElevation = 8.dp

/**
 * OMaster 预设卡片组件
 *
 * 设计规范:
 * - 圆角: 16dp
 * - 背景: Zinc800
 * - 边框: 1px Zinc700，悬停时变为橙色半透明
 * - 悬停效果: 上移 + 边框高亮
 * - 图片: 顶部圆角 + 缩放动画
 *
 * @param preset 预设数据
 * @param onClick 点击回调
 * @param onFavoriteClick 收藏点击回调
 * @param onDeleteClick 删除点击回调
 * @param showFavoriteButton 是否显示收藏按钮
 * @param showDeleteButton 是否显示删除按钮
 * @param modifier 修饰符
 * @param imageHeight 图片高度
 */
@Composable
fun PresetCard(
    preset: MasterPreset,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    showFavoriteButton: Boolean = false,
    showDeleteButton: Boolean = false,
    modifier: Modifier = Modifier,
    imageHeight: Int = 200
) {
    // 交互状态
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // 动画状态
    var isHovered by remember { mutableStateOf(false) }

    // 边框颜色动画
    val borderColor by animateFloatAsState(
        targetValue = if (isPressed || isHovered) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "borderColor"
    )

    // 阴影缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else if (isHovered) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // 上移动画
    val translateY by animateFloatAsState(
        targetValue = if (isHovered) -8f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "translateY"
    )

    // 计算当前边框颜色
    val currentBorderColor = if (isPressed) {
        BorderPressed
    } else if (isHovered) {
        BorderHighlight
    } else {
        BorderLight
    }

    // 计算当前边框宽度
    val currentBorderWidth = if (isPressed || isHovered) 1.5.dp else 1.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = currentBorderWidth,
                color = currentBorderColor,
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) PressedShadowElevation else DefaultShadowElevation
        )
    ) {
        Column {
            // ========================================
            // 图片区域
            // ========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = CardCornerRadius,
                            topEnd = CardCornerRadius,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
            ) {
                // 预设图片 - 带缩放动画
                PresetImageWithScale(
                    preset = preset,
                    isHovered = isHovered,
                    modifier = Modifier.fillMaxSize()
                )

                // 按钮层 (收藏/删除/新品标签)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    // 删除按钮或新品标签
                    if (showDeleteButton && preset.isCustom) {
                        ActionButton(
                            icon = Icons.Filled.Delete,
                            tint = Color(0xFFEF4444), // 红色
                            onClick = {
                                haptic.perform(HapticFeedbackType.Confirm)
                                onDeleteClick()
                            }
                        )
                    } else if (preset.isNew) {
                        NewBadge()
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 收藏按钮
                    if (showFavoriteButton) {
                        ActionButton(
                            icon = if (preset.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            tint = if (preset.isFavorite) HasselbladOrange else Color.White,
                            hasBackground = preset.isFavorite,
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                onFavoriteClick()
                            }
                        )
                    }
                }

                // 装饰性光晕 (仅悬停时显示)
                if (isHovered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(200.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GlowOrange,
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }

            // ========================================
            // 内容区域
            // ========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = CardPadding,
                        vertical = CardContentSpacing
                    )
            ) {
                // 预设名称
                Text(
                    text = PresetI18n.getLocalizedPresetName(preset.name),
                    style = CardTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 作者信息 (如果有)
                preset.author?.let { author ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 标签 (如果有)
                preset.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                    Spacer(modifier = Modifier.height(CardContentSpacing))
                    TagsRow(tags = tags)
                }
            }
        }
    }
}

/**
 * 带缩放效果的预设图片组件
 */
@Composable
private fun PresetImageWithScale(
    preset: MasterPreset,
    isHovered: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "imageScale"
    )

    Box(
        modifier = modifier.scale(scale)
    ) {
        PresetImage(
            preset = preset,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * 操作按钮组件
 */
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    hasBackground: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "buttonScale"
    )

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .scale(scale),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = if (hasBackground) {
                        HasselbladOrange.copy(alpha = 0.2f)
                    } else {
                        Color.Black.copy(alpha = 0.3f)
                    },
                    shape = RoundedCornerShape(IconCornerRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 新品标签组件
 */
@Composable
private fun NewBadge() {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GradientOrangeStart,
                        GradientOrangeEnd
                    )
                ),
                shape = RoundedCornerShape(BadgeCornerRadius)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.preset_new),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

/**
 * 标签行组件
 */
@Composable
private fun TagsRow(tags: List<String>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
    ) {
        tags.take(3).forEach { tag ->
            TagItem(text = tag)
        }
        if (tags.size > 3) {
            TagItem(text = "+${tags.size - 3}")
        }
    }
}

/**
 * 单个标签组件
 */
@Composable
private fun TagItem(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Zinc800,
                shape = RoundedCornerShape(BadgeCornerRadius)
            )
            .border(
                width = 0.5.dp,
                color = BorderDefault.copy(alpha = 0.3f),
                shape = RoundedCornerShape(BadgeCornerRadius)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = TagTextStyle
        )
    }
}

/**
 * 预设卡片占位符组件
 */
@Composable
fun PresetCardPlaceholder(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .border(
                width = 1.dp,
                color = BorderLight,
                shape = RoundedCornerShape(CardCornerRadius)
            ),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}
