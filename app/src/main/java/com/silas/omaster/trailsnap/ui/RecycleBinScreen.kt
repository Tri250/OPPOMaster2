package com.silas.omaster.trailsnap.ui

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.MediaType
import com.silas.omaster.trailsnap.model.TrailPhoto
import com.silas.omaster.ui.theme.HasselbladOrange

@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    var deletedPhotos by remember { mutableStateOf(repository.getDeletedPhotos()) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TrailSnapTopBar(
            title = "回收站",
            onBack = onBack,
            actions = {
                if (deletedPhotos.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text(
                            "清空",
                            color = HasselbladOrange,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        )

        if (deletedPhotos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyHint(text = "回收站为空")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(deletedPhotos, key = { it.id }) { photo ->
                    DeletedPhotoItem(
                        photo = photo,
                        onRestore = {
                            repository.restorePhoto(photo.id)
                            deletedPhotos = repository.getDeletedPhotos()
                            Toast.makeText(context, "已恢复", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            val ok = repository.permanentlyDelete(photo.id)
                            deletedPhotos = repository.getDeletedPhotos()
                            Toast.makeText(
                                context,
                                if (ok) "已彻底删除" else "已从列表移除（系统文件可能保留）",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空回收站") },
            text = { Text("将彻底删除 ${deletedPhotos.size} 张照片，此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        deletedPhotos.forEach { repository.permanentlyDelete(it.id) }
                        deletedPhotos = repository.getDeletedPhotos()
                        confirmClear = false
                        Toast.makeText(context, "回收站已清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text("彻底删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DeletedPhotoItem(
    photo: TrailPhoto,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "deleted_photo_scale"
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

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "恢复",
                tint = HasselbladOrange,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRestore()
                    }
            )
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = "彻底删除",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    }
            )
        }
    }
}

private fun openPhotoViewer(context: android.content.Context, photo: TrailPhoto) {
    val uri = photo.uri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, if (photo.mediaType == MediaType.VIDEO) "video/*" else "image/*")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) { }
}
