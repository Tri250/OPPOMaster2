package com.silas.omaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import com.silas.omaster.ui.animation.FluidPressSpring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import com.silas.omaster.ui.components.glass.GlassButton
import com.silas.omaster.ui.components.glass.GlassEffectConfig
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.CardBorderHighlight
import com.silas.omaster.ui.theme.CardBorderLight
import com.silas.omaster.ui.theme.GlassColors
import com.silas.omaster.ui.theme.OffWhite
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.util.PresetI18n
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform

/**
 * 预设卡片
 *
 * 【v1.6.14】新增：
 * - 长按弹出 ColorOS 17 风格快捷菜单（应用至悬浮窗 / 复制参数 / 分享 / 收藏），
 *   把"进详情才能操作"压缩为一步直达（对齐系统长按交互范式）；
 * - 一键应用浮钮（[onApplyToFloat] 非 null 时显示），直接把参数写入悬浮窗。
 *
 * @param onApplyToFloat 应用至悬浮窗回调；传 null 则不显示应用按钮与菜单项
 * @param onCopyParams 复制参数回调；传 null 则不显示该菜单项
 * @param onSharePreset 分享预设回调；传 null 则不显示该菜单项
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
    imageHeight: Int = 200,
    usePremiumGlass: Boolean = true,
    onApplyToFloat: (() -> Unit)? = null,
    onCopyParams: (() -> Unit)? = null,
    onSharePreset: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // 长按快捷菜单（ColorOS 17 长按范式）；无任何可选项时不启用长按
    val hasContextMenu = onApplyToFloat != null || onCopyParams != null || onSharePreset != null ||
            showFavoriteButton || (showDeleteButton && preset.isCustom)
    var showContextMenu by remember { mutableStateOf(false) }

    // 【v1.6.13】ColorOS 17 液态按压反馈：按下轻微缩小，松手细腻回弹
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = FluidPressSpring,
        label = "presetCardPress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    if (hasContextMenu) {
                        haptic.perform(HapticFeedbackType.LongPress)
                        showContextMenu = true
                    }
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box {
            // 图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .border(
                        width = if (isPressed) 1.5.dp else 1.dp,
                        color = if (isPressed)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        else
                            themedBorderLight(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp))
            ) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showFavoriteButton) {
                    // 统一使用 GlassButton 组件，根据 usePremiumGlass 参数切换模式
                    GlassButton(
                        isActive = preset.isFavorite,
                        glowColor = MaterialTheme.colorScheme.primary,
                        isPremium = usePremiumGlass,
                        size = 36.dp,
                        config = GlassEffectConfig.DefaultButton,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        onClick = {
                            haptic.perform(HapticFeedbackType.ToggleOn)
                            onFavoriteClick()
                        }
                    ) {
                        Icon(
                            imageVector = if (preset.isFavorite)
                                Icons.Filled.Favorite
                            else
                                Icons.Outlined.FavoriteBorder,
                            contentDescription = if (preset.isFavorite)
                                stringResource(R.string.preset_favorited)
                            else
                                stringResource(R.string.preset_favorite),
                            tint = if (preset.isFavorite)
                                    MaterialTheme.colorScheme.primary
                                else
                                    OffWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showDeleteButton && preset.isCustom) {
                    // 统一使用 GlassButton 组件，根据 usePremiumGlass 参数切换模式
                    GlassButton(
                        isActive = false,
                        glowColor = MaterialTheme.colorScheme.error,
                        isPremium = usePremiumGlass,
                        size = 36.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            onDeleteClick()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.preset_delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (preset.isNew) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                                    )
                                ),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = themedBorderLight(),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.preset_new),
                            fontSize = 10.sp,
                            color = OffWhite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // 玻璃质感文字区域 - 叠加在图片底部
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 【v1.6.13】机型适配徽章（哈苏 / 2亿）—— Find X10 专属预设一眼可辨
                Row(verticalAlignment = Alignment.CenterVertically) {
                    preset.deviceProfile?.let { dp ->
                        if (dp.isProImaging || dp.totalPixelsMp >= 200) {
                            val badge = listOfNotNull(
                                if (dp.isProImaging) stringResource(R.string.device_badge_hasselblad) else null,
                                if (dp.totalPixelsMp >= 200) stringResource(R.string.device_badge_200mp) else null
                            ).joinToString(" · ")
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = badge,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    fontSize = 9.sp,
                                    color = OffWhite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Text(
                        text = PresetI18n.getLocalizedPresetName(preset.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OffWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 【v1.6.14】一键应用至悬浮窗（卡片右下角浮钮，直达核心使用场景）
            if (onApplyToFloat != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PureBlack.copy(alpha = 0.55f))
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            onApplyToFloat()
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.preset_apply_float),
                        style = MaterialTheme.typography.labelSmall,
                        color = OffWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 【v1.6.14】长按快捷菜单（ColorOS 17 长按交互范式）
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                if (onApplyToFloat != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.preset_apply_float)) },
                        onClick = {
                            showContextMenu = false
                            onApplyToFloat()
                        }
                    )
                }
                if (onCopyParams != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.preset_copy_params)) },
                        onClick = {
                            showContextMenu = false
                            onCopyParams()
                        }
                    )
                }
                if (onSharePreset != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.preset_share)) },
                        onClick = {
                            showContextMenu = false
                            onSharePreset()
                        }
                    )
                }
                if (showFavoriteButton) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (preset.isFavorite) R.string.preset_favorited
                                    else R.string.preset_favorite
                                )
                            )
                        },
                        onClick = {
                            showContextMenu = false
                            haptic.perform(HapticFeedbackType.ToggleOn)
                            onFavoriteClick()
                        }
                    )
                }
                if (showDeleteButton && preset.isCustom) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.preset_delete)) },
                        onClick = {
                            showContextMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

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
                color = themedBorderLight(),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextSecondary()
            )
        }
    }
}
