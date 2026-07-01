package com.silas.omaster.trailsnap.ui

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.AsyncImage
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.MediaType
import com.silas.omaster.trailsnap.model.TrailPhoto
import com.silas.omaster.ui.theme.HasselbladOrange

@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val favorites by repository.favorites.collectAsState()
    val isLoading by repository.isLoading.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TrailSnapTopBar(title = "收藏夹", onBack = onBack)

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "还没有收藏的照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "在照片右上角点击心形图标收藏",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(favorites, key = { it.id }) { photo ->
                    FavoritePhotoItem(
                        photo = photo,
                        onToggleFavorite = {
                            val ok = repository.toggleFavorite(photo.id)
                            if (ok) {
                                Toast.makeText(context, "已取消收藏", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePhotoItem(
    photo: TrailPhoto,
    onToggleFavorite: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "favorite_photo_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    openPhotoViewer(context, photo)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.thumbnailUri ?: photo.uri,
            contentDescription = photo.filename,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (photo.mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "视频",
                tint = HasselbladOrange,
                modifier = Modifier.size(24.dp)
            )
        }

        // 收藏按钮（在收藏夹中显示实心）
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleFavorite()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "已收藏",
                tint = HasselbladOrange,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

