package com.silas.omaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.icons.OMasterIcons
import com.silas.omaster.ui.theme.*
import com.silas.omaster.ui.animation.ColorOS16Animations

/**
 * =====================================================
 * ColorOS 16 专业摄影导航栏
 * =====================================================
 * 设计标准：Aquatic Design 水感设计
 * 图标风格：专业摄影图标，符合高端用户审美
 * 动画效果：ColorOS 16 弹性动画
 */

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Composable
fun ColorOS16NavBar(
    visible: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 专业摄影导航项
    val navItems = listOf(
        NavItem(
            route = "home",
            title = "首页",
            icon = OMasterIcons.Home,
            selectedIcon = OMasterIcons.Home
        ),
        NavItem(
            route = "ai",
            title = "AI",
            icon = OMasterIcons.AI,
            selectedIcon = OMasterIcons.AI
        ),
        NavItem(
            route = "watermark",
            title = "水印",
            icon = OMasterIcons.Watermark,
            selectedIcon = OMasterIcons.Watermark
        ),
        NavItem(
            route = "settings",
            title = "设置",
            icon = OMasterIcons.Settings,
            selectedIcon = OMasterIcons.Settings
        )
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(ColorOS16Animations.Duration.NORMAL.toInt())
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(ColorOS16Animations.Duration.NORMAL.toInt())
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // 胶囊导航栏 - ColorOS 16风格
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(72.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(36.dp),
                        ambientColor = Color.Black.copy(alpha = 0.4f),
                        spotColor = Color.Black.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(36.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SurfacePrimary.copy(alpha = 0.95f),
                                SurfaceSecondary.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                // 顶部高光
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // 导航项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val selected = currentRoute == item.route
                        ProfessionalNavItem(
                            item = item,
                            selected = selected,
                            onClick = { onNavigate(item.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfessionalNavItem(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // ColorOS 16 弹性动画
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            selected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    // 图标缩放
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    // 背景色
    val backgroundColor = when {
        selected -> HasselbladOrange.copy(alpha = 0.15f)
        isPressed -> SurfaceSecondary
        else -> Color.Transparent
    }

    // 图标颜色
    val iconColor = when {
        selected -> HasselbladOrange
        isPressed -> TextSecondary
        else -> TextTertiary
    }

    // 文字颜色
    val textColor = when {
        selected -> HasselbladOrange
        isPressed -> TextSecondary
        else -> TextTertiary
    }

    Column(
        modifier = Modifier
            .width(64.dp)
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    onClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 专业摄影图标
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier
                .size(if (selected) 26.dp else 24.dp)
                .scale(iconScale),
            tint = iconColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 导航文字
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )

        // 选中指示点
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                HasselbladOrange,
                                HasselbladOrangeLight
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
    }
}

// 兼容旧版本
@Composable
fun PillNavBar(
    visible: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ColorOS16NavBar(
        visible = visible,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        modifier = modifier
    )
}