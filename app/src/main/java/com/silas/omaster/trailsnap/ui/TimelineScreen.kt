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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.MediaType
import com.silas.omaster.trailsnap.model.TimelineSection
import com.silas.omaster.trailsnap.model.TrailPhoto
import com.silas.omaster.ui.theme.HasselbladOrange
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val photos by repository.photos.collectAsState()
    val sections = remember(photos) { repository.getTimelineSections() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TrailSnapTopBar(title = "时间轴", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            sections.forEach { section ->
                item {
                    TimelineDateHeader(date = section.date.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", Locale.CHINA)))
                }
                items(section.photos.chunked(3)) { rowPhotos ->
                    TimelinePhotoRow(photos = rowPhotos)
                }
            }
        }
    }
}

@Composable
private fun TimelineDateHeader(date: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50))
                .background(HasselbladOrange)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TimelinePhotoRow(photos: List<TrailPhoto>) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photos.forEach { photo ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "timeline_photo_scale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .height(112.dp)
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
            }
        }
        repeat(3 - photos.size) {
            Spacer(modifier = Modifier.weight(1f))
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
