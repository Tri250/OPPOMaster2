package com.silas.omaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.*

/**
 * ============================================
 * 液态玻璃组件 - ColorOS 16 风格
 * ============================================
 */

/**
 * 基础液态玻璃容器
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(GlassBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.4f))
    ) {
        content()
    }
}

/**
 * 轻量液态玻璃容器
 */
@Composable
fun GlassLightContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(GlassBackgroundLight)
            .border(1.dp, BorderLight, RoundedCornerShape(16.dp))
            .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.25f))
    ) {
        content()
    }
}

/**
 * 重度液态玻璃容器
 */
@Composable
fun GlassHeavyContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(GlassBackgroundHeavy)
            .border(1.dp, BorderMedium, RoundedCornerShape(16.dp))
            .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.5f))
    ) {
        content()
    }
}

/**
 * 液态玻璃卡片
 * 支持悬停效果和动画
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isHovered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = AnimationSpecs.SpringSoftTween,
        label = "card_scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 8.dp,
        animationSpec = AnimationSpecs.LiquidTween,
        label = "card_elevation"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .shadow(elevation, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkGray
        ),
        border = CardDefaults.outlinedCardBorder(
            enabled = true,
            color = if (isHovered) BorderMedium else BorderLight,
            width = 1.dp
        )
    ) {
        Column {
            content()
        }
    }
}

/**
 * 液态玻璃按钮
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(50),
        label = "button_scale"
    )
    
    Button(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier.scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HasselbladOrange.copy(alpha = 0.15f),
            contentColor = HasselbladOrange,
            disabledContainerColor = DarkGray.copy(alpha = 0.5f),
            disabledContentColor = TextMuted
        ),
        border = BorderStroke(1.dp, HasselbladOrange.copy(alpha = 0.3f))
    ) {
        content()
    }
}

/**
 * 液态玻璃芯片
 */
@Composable
fun GlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.05f),
        animationSpec = AnimationSpecs.SpringSoftTween,
        label = "chip_bg"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextTertiary,
        animationSpec = AnimationSpecs.FastTween,
        label = "chip_text"
    )
    
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = backgroundColor,
            selectedContainerColor = HasselbladOrange
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = BorderLight,
            selectedBorderColor = HasselbladOrange.copy(alpha = 0.5f),
            enabled = true,
            selected = selected
        )
    )
}

/**
 * 液态玻璃输入框
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) HasselbladOrange else BorderLight,
        animationSpec = AnimationSpecs.SmoothTween,
        label = "input_border"
    )
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = BorderLight,
            focusedContainerColor = DarkGray.copy(alpha = 0.6f),
            unfocusedContainerColor = DarkGray.copy(alpha = 0.4f),
            cursorColor = HasselbladOrange,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextSecondary
        ),
        leadingIcon = leadingIcon
    )
}

/**
 * 液态玻璃导航栏
 */
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    NavigationBar(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NearBlack.copy(alpha = 0.95f),
                        DarkGray.copy(alpha = 0.8f)
                    )
                )
            )
            .border(1.dp, BorderLight),
        containerColor = Color.Transparent,
        contentColor = TextSecondary,
        tonalElevation = 0.dp
    ) {
        content()
    }
}

/**
 * 液态玻璃底部栏
 */
@Composable
fun GlassBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkGray.copy(alpha = 0.8f),
                        NearBlack.copy(alpha = 0.95f)
                    )
                )
            )
            .border(1.dp, BorderLight),
        color = Color.Transparent,
        contentColor = TextSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            content()
        }
    }
}

/**
 * 液态玻璃徽章
 */
@Composable
fun GlassBadge(
    text: String,
    color: Color = HasselbladOrange,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(color, color.copy(alpha = 0.8f))
                )
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        )
    }
}

/**
 * HNCS 徽章
 */
@Composable
fun HncsBadge(
    modifier: Modifier = Modifier
) {
    GlassBadge(
        text = "HNCS",
        color = HasselbladOrange,
        modifier = modifier
    )
}

/**
 * NEW 徽章
 */
@Composable
fun NewBadge(
    modifier: Modifier = Modifier
) {
    GlassBadge(
        text = "NEW",
        color = SuccessGreen,
        modifier = modifier
    )
}

/**
 * 液态玻璃图标容器
 */
@Composable
fun GlassIconContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassBackgroundLight,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

/**
 * 液态玻璃圆形图标容器
 */
@Composable
fun GlassCircleIcon(
    modifier: Modifier = Modifier,
    backgroundColor: Color = GlassBackgroundLight,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, BorderLight, CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        content()
    }
}

/**
 * 功能卡片 - 带特色色
 */
@Composable
fun FeatureGlassCard(
    featureId: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val featureColor = getFeatureColor(featureId)
    
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 渐变背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                featureColor.copy(alpha = 0.3f),
                                featureColor.copy(alpha = 0.1f),
                                DarkGray.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            )
            
            Text(
                text = subtitle,
                color = TextTertiary,
                style = MaterialTheme.typography.bodySmall
            )
            
            content()
        }
    }
}