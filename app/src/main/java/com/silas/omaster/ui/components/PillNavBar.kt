package com.silas.omaster.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.ui.animation.adaptiveSpringSpec
import com.silas.omaster.util.perform

private val NavBarBackground = Color(0xFF1A1A1A)
private val NavBarBorder = Color(0xFF2A2A2A)

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@Composable
fun PillNavBar(
    visible: Boolean,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val homeTitle = stringResource(R.string.nav_home)
    val featuredTitle = stringResource(R.string.nav_featured)
    val featuresTitle = stringResource(R.string.nav_core_features)
    val aboutTitle = stringResource(R.string.nav_about)

    val navItems = remember(homeTitle, featuredTitle, featuresTitle, aboutTitle) {
        listOf(
            NavItem("home", homeTitle, Icons.Default.Home),
            NavItem("subscription", featuredTitle, Icons.Default.Star),
            NavItem("features", featuresTitle, Icons.Default.AutoAwesome),
            NavItem("about", aboutTitle, Icons.Default.Info)
        )
    }

    // 使用动画控制底部导航栏显示/隐藏，固定在底部
    // 将 modifier（包含 align）应用在外层 Box 上，确保定位不受动画影响
    Box(
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
        // 使用 fillMaxWidth(0.9f) 替代固定 maxWidth，适配不同屏幕
        val navBarModifier = Modifier.fillMaxWidth(0.92f)
        // 外层阴影效果
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f),
                    spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
                )
        ) {
            // 磨砂玻璃背景层 - 使用自适应宽度
            Box(
                modifier = Modifier
                    .then(navBarModifier)
                .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NavBarBackground.copy(alpha = 0.85f),
                                NavBarBackground.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // 顶部高光线条
            Box(
                modifier = Modifier
                    .then(navBarModifier)
                .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // 边框
            Box(
                modifier = Modifier
                    .then(navBarModifier)
                .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                NavBarBorder.copy(alpha = 0.5f),
                                NavBarBorder.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(1.dp)
            ) {
                // 内部背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NavBarBackground.copy(alpha = 0.9f),
                                    NavBarBackground.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // 导航项 - 使用 weight 均分宽度，避免小屏设备固定宽度溢出
            Row(
                modifier = Modifier
                    .then(navBarModifier)
                    .height(64.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.route

                    NavItemButton(
                        item = item,
                        selected = selected,
                        onClick = { onNavigate(item.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    }
    }
}


@Composable
private fun NavItemButton(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = adaptiveSpringSpec(),
        label = "scale"
    )

    val backgroundColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = adaptiveSpringSpec(),
        label = "iconScale"
    )

    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "indicatorAlpha"
    )

    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = item.title,
                onClick = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            modifier = Modifier
                .size(if (selected) 22.dp else 20.dp)
                .scale(iconScale),
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal
        )
        // 选中指示器 - 提供更强的选中状态视觉反馈
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
                .graphicsLayer { alpha = indicatorAlpha }
        )
    }
}
