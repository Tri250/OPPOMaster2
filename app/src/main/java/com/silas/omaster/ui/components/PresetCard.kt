package com.silas.omaster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.theme.CardBorderLight
import com.silas.omaster.ui.theme.DarkGray
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.SuccessGreen
import com.silas.omaster.ui.theme.WarningYellow
import com.silas.omaster.util.PresetI18n
import com.silas.omaster.util.hapticClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.silas.omaster.util.perform

/**
 * 预设卡片组件（Web风格）
 * 合并了原PresetCard和PresetCardWebStyle的功能，支持Web风格和简洁风格。
 *
 * @param preset 预设数据
 * @param onClick 卡片点击回调
 * @param onFavoriteClick 收藏按钮点击回调
 * @param onDeleteClick 删除按钮点击回调
 * @param modifier 修饰符
 * @param imageHeight 图片高度（dp）
 * @param showFavoriteButton 是否显示收藏按钮
 * @param showDeleteButton 是否显示删除按钮
 * @param showGradientOverlay 是否显示渐变遮罩（Web风格）
 * @param showAuthorInfo 是否显示作者信息（Web风格）
 * @param showBadges 是否显示角标（HNCS/NEW）
 * @param showStats 是否显示统计数据（评分/下载量/品牌）
 */
@Composable
fun PresetCard(
    preset: MasterPreset,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    imageHeight: Int = 200,
    showFavoriteButton: Boolean = true,
    showDeleteButton: Boolean = false,
    showGradientOverlay: Boolean = true,
    showAuthorInfo: Boolean = true,
    showBadges: Boolean = true,
    showStats: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    // 图片加载状态跟踪
    var imageLoadState by remember { mutableStateOf(ImageLoadState.LOADING) }
    val hasCoverImage = preset.coverPath.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .hapticClickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
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
            // 图片区域
            if (hasCoverImage) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    showDownloadIndicator = true,
                    onLoadState = { state -> imageLoadState = state }
                )
            }

            // 图片加载失败或URL为空时显示占位图
            if (!hasCoverImage || imageLoadState == ImageLoadState.ERROR) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = stringResource(R.string.image_load_failed),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            PresetI18n.getLocalizedPresetName(preset.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 渐变遮罩（Web风格）
            if (showGradientOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            }

            // HNCS角标（Web风格：橙色渐变）
            if (showBadges && preset.isHncs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(HasselbladOrange, WarningYellow)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDC51",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = stringResource(R.string.badge_hncs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // NEW角标（Web风格：绿色）
            if (showBadges && preset.isNew && !preset.isHncs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = SuccessGreen,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "\u2728",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.badge_new),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // 收藏按钮（右上角）
            if (showFavoriteButton) {
                IconButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onFavoriteClick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (preset.isFavorite)
                                    Color.Red.copy(alpha = 0.2f)
                                else
                                    Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(14.dp)
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
                            tint = if (preset.isFavorite) Color.Red else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 删除按钮（左上角，仅自定义预设）
            if (showDeleteButton && preset.isCustom) {
                IconButton(
                    onClick = {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onDeleteClick()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
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

            // 内容信息（Web风格：底部）
            if (showAuthorInfo) {
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
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = preset.author,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 统计数据（Web风格，仅展示真实数据）
                    if (showStats) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 评分
                            preset.rating?.let { rating ->
                                Text(
                                    text = "\u2B50 ${String.format("%.1f", rating)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            // 下载量
                            preset.downloads?.let { downloads ->
                                val downloadsText = if (downloads >= 10000) {
                                    "${(downloads / 10000).toInt()}w"
                                } else {
                                    "$downloads"
                                }
                                Text(
                                    text = "\uD83D\uDCE5 $downloadsText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                            // 品牌
                            preset.brand?.let { brand ->
                                Text(
                                    text = brand,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // 简洁模式：仅显示名称
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = PresetI18n.getLocalizedPresetName(preset.name),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                color = CardBorderLight,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkGray
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.empty_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
