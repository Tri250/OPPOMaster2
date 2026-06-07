package com.silas.omaster.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.animation.OMasterAnimations
import com.silas.omaster.ui.theme.*

/**
 * =====================================================
 * OMaster 设计系统 v2.0 - UI组件
 * =====================================================
 * 组件规范：
 * - 按钮：圆角设计，渐变背景，悬停动画效果
 * - 卡片：圆角卡片式布局，深色背景，边框分隔
 * - 图标：线性图标，2px描边风格
 */

// ==================== 按钮组件 ====================

/**
 * 主按钮 - 哈苏橙渐变背景
 * 用于：主要操作、CTA按钮
 */
@Composable
fun OMasterPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = OMasterAnimations.tapScaleSpec()
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 8.dp,
        animationSpec = tween(100)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (enabled) shadowElevation else 0.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = HasselbladOrange50
            )
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (enabled) {
                        listOf(HasselbladOrangeLight, HasselbladOrange, HasselbladOrangeDark)
                    } else {
                        listOf(BorderPrimary, BorderSecondary)
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                style = ButtonTypography.Primary,
                color = TextPrimary
            )
        }
    }
}

/**
 * 次按钮 - 边框样式
 * 用于：次要操作、取消按钮
 */
@Composable
fun OMasterSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = OMasterAnimations.tapScaleSpec()
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) BackgroundTertiary else Color.Transparent,
        animationSpec = tween(150)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (enabled) BorderPrimary else BorderSecondary,
                shape = RoundedCornerShape(12.dp)
            )
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ButtonTypography.Secondary,
            color = if (enabled) TextSecondary else TextQuaternary
        )
    }
}

/**
 * 幽灵按钮 - 透明背景
 * 用于：链接、文字按钮
 */
@Composable
fun OMasterGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = HasselbladOrange
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = OMasterAnimations.tapScaleSpec()
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ButtonTypography.Secondary,
            color = color
        )
    }
}

// ==================== 卡片组件 ====================

/**
 * 标准卡片
 * 用于：内容展示、列表项
 */
@Composable
fun OMasterCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 8.dp else 0.dp,
        animationSpec = OMasterAnimations.hoverElevationSpec()
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isHovered) HasselbladOrange30 else BorderPrimary50,
        animationSpec = tween(200)
    )
    
    val offsetY by animateDpAsState(
        targetValue = if (isHovered) (-4).dp else 0.dp,
        animationSpec = OMasterAnimations.hoverYSpec()
    )
    
    Column(
        modifier = modifier
            .offset(y = offsetY)
            .shadow(elevation, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(BackgroundTertiary)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * 功能卡片 - 带图标
 * 用于：首页功能入口
 */
@Composable
fun OMasterFeatureCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    tags: List<String> = emptyList(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OMasterCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 图标区域
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = GradientHasselblad
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            
            // 内容区域
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = CardTypography.Title,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = CardTypography.Description,
                    color = TextTertiary
                )
                if (tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tags.forEach { tag ->
                            OMasterTag(text = tag)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 预设卡片
 * 用于：预设列表展示
 */
@Composable
fun OMasterPresetCard(
    name: String,
    device: String,
    author: String,
    rating: Float,
    downloads: String,
    tags: List<String>,
    isHNCS: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OMasterCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            // 标题区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = CardTypography.Title,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = device,
                        style = CardTypography.Description,
                        color = TextQuaternary
                    )
                }
                if (isHNCS) {
                    OMasterHNCSShield()
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 标签
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    OMasterTag(text = tag)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 底部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = author,
                    style = CardTypography.Tag,
                    color = TextQuaternary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 评分
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = WarningYellow,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = rating.toString(),
                            style = BadgeTypography.Rating,
                            color = TextSecondary
                        )
                    }
                    // 下载量
                    Text(
                        text = downloads,
                        style = CardTypography.Tag,
                        color = TextQuaternary
                    )
                }
            }
        }
    }
}

// ==================== 标签组件 ====================

/**
 * 标准标签
 */
@Composable
fun OMasterTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BorderPrimary50)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = CardTypography.Tag,
            color = TextSecondary
        )
    }
}

/**
 * HNCS认证盾牌
 */
@Composable
fun OMasterHNCSShield(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HasselbladOrange20)
            .border(1.dp, HasselbladOrange30, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = HasselbladOrange,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "HNCS",
            style = BadgeTypography.HNCS,
            color = HasselbladOrange
        )
    }
}

// ==================== 输入框组件 ====================

/**
 * 标准输入框
 */
@Composable
fun OMasterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        placeholder = {
            Text(
                text = placeholder,
                color = TextQuaternary
            )
        },
        leadingIcon = leadingIcon,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = BackgroundTertiary,
            unfocusedContainerColor = BackgroundTertiary,
            focusedBorderColor = HasselbladOrange,
            unfocusedBorderColor = BorderPrimary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = HasselbladOrange
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// ==================== 分隔线 ====================

@Composable
fun OMasterDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier.fillMaxWidth(),
        color = BorderPrimary50,
        thickness = 1.dp
    )
}
