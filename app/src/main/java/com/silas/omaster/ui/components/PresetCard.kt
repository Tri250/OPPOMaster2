package com.silas.omaster.ui.components

import androidx.compose.ui.unit.Dp
import com.silas.omaster.ui.theme.LiquidGlassConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.hapticClickable
import com.silas.omaster.util.perform

/**
 * 统一预设卡片组件（对齐Web端样式）
 *
 * @param preset 预设数据
 * @param onClick 卡片点击回调
 * @param onFavoriteClick 收藏按钮点击回调
 * @param onDeleteClick 删除按钮点击回调
 * @param showFavoriteButton 是否显示收藏按钮，默认 true
 * @param showDeleteButton 是否显示删除按钮，默认 false
 * @param showDetailInfo 是否显示详细信息（作者、评分、下载量等），默认 true
 * @param imageHeight 图片高度（像素值，会转为 dp）
 * @param modifier 修饰符
 */
@Composable
fun PresetCard(
    preset: MasterPreset,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    showFavoriteButton: Boolean = true,
    showDeleteButton: Boolean = false,
    showDetailInfo: Boolean = true,
    modifier: Modifier = Modifier,
    imageHeight: Int = 200
) {
    val cardContentDescription = stringResource(
        R.string.preset_card_description,
        PresetI18n.getLocalizedPresetName(preset.name),
        preset.author
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .hapticClickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))
            )
            .semantics {
                contentDescription = cardContentDescription
            },
        shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight.dp)
        ) {
            // 图片 - 使用 PresetImage 组件确保正确加载
            PresetImage(
                preset = preset,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))),
                contentScale = ContentScale.Crop,
                showDownloadIndicator = true
            )

            // 渐变遮罩（对齐Web端）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // HNCS角标（对齐Web端：橙色渐变，右上角）
            if (preset.isHncs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 48.dp, top = 8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(HasselbladOrange, WarningYellow)
                            ),
                            shape = RoundedCornerShape(Dp(LiquidGlassConfig.TinyCornerRadius))
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "👑",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.badge_hncs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // NEW角标（对齐Web端：绿色，左上角，仅当不是HNCS时显示）
            if (preset.isNew && !preset.isHncs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = SuccessGreen,
                            shape = RoundedCornerShape(Dp(LiquidGlassConfig.TinyCornerRadius))
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "✨",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.badge_new),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // 收藏按钮（对齐Web端：右上角，触摸区域扩大到 48dp）
            if (showFavoriteButton) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (preset.isFavorite)
                                    Color.Red.copy(alpha = 0.2f)
                                else
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))
                            ),
                        contentAlignment = Alignment.Center
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
                            tint = if (preset.isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 删除按钮（对齐Web端：左上角，仅自定义预设且启用时显示，触摸区域扩大到 48dp）
            if (showDeleteButton && preset.isCustom) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.preset_delete),
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 内容信息（对齐Web端：底部）
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = PresetI18n.getLocalizedPresetName(preset.name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (showDetailInfo) {
                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 统计数据（对齐Web端，仅展示真实数据）
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 评分（仅在有数据时展示）
                        preset.rating?.let { rating ->
                            Text(
                                text = "⭐ ${String.format("%.1f", rating)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        // 下载量（仅在有数据时展示）
                        preset.downloads?.let { downloads ->
                            val downloadsText = if (downloads >= 10000) {
                                "${(downloads / 10000).toInt()}w"
                            } else {
                                "$downloads"
                            }
                            Text(
                                text = "📥 $downloadsText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                        // 品牌
                        preset.brand?.let { brand ->
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))
            ),
        shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * 可滑动删除的预设卡片包装器
 * 支持向左滑动显示删除操作
 */
@Composable
fun SwipeablePresetCard(
    preset: MasterPreset,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    imageHeight: Int = 200,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteAction by remember { mutableStateOf(false) }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 删除背景操作
        if (showDeleteAction) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .background(
                        color = Color.Red.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(Dp(LiquidGlassConfig.CardCornerRadius))
                    ),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.perform(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onDeleteClick()
                        showDeleteAction = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 预设卡片
        PresetCard(
            preset = preset,
            onClick = onClick,
            onFavoriteClick = onFavoriteClick,
            imageHeight = imageHeight,
            modifier = Modifier
                .offset {
                    if (showDeleteAction) {
                        androidx.compose.ui.unit.IntOffset(-80, 0)
                    } else {
                        androidx.compose.ui.unit.IntOffset.Zero
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            showDeleteAction = !showDeleteAction
                        },
                        onDragCancel = {
                            showDeleteAction = false
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount < -50) {
                                showDeleteAction = true
                            } else if (dragAmount > 50) {
                                showDeleteAction = false
                            }
                        }
                    )
                }
        )
    }
}
