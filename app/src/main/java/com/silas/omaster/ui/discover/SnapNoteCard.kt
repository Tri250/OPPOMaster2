package com.silas.omaster.ui.discover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.silas.omaster.R
import com.silas.omaster.data.local.SnapNote
import com.silas.omaster.data.local.SnapNoteManager
import com.silas.omaster.ui.animation.fluidPressScale
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 【v1.6.14 发现 2.0】AiSnapNote 快拍笔记卡
 * 拍摄 / 相册选图 → 端上 Palette 取色 → 生成"光影笔记"（场景主色 + 色板），
 * 支持笔记回顾面板浏览与删除。全程本地处理，不上传任何数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapNoteCard(
    snapNoteManager: SnapNoteManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val notes by snapNoteManager.notesFlow.collectAsState()

    var showPickerSheet by remember { mutableStateOf(false) }
    var showGallerySheet by remember { mutableStateOf(false) }

    // 保存成功后自动打开回顾面板
    var pendingOpenGallery by remember { mutableStateOf(false) }
    if (pendingOpenGallery) {
        pendingOpenGallery = false
        showGallerySheet = true
    }

    /** 取色 → 保存 → 标记打开回顾（解码/取色/写盘在 IO 协程执行，避免阻塞主线程） */
    fun processAndSave(uri: Uri) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch(Dispatchers.IO) {
            val saved = snapNoteManager.processAndSaveNote(context, uri)
            // 相机临时文件兜底清理（保存成功时 processAndSaveNote 已删除对应源文件）
            runCatching { snapTempDir(context).listFiles()?.forEach { it.delete() } }
            withContext(Dispatchers.Main) {
                if (saved != null) pendingOpenGallery = true
            }
        }
    }

    // 拍摄目标文件：TakePicture 只写入启动时传入的 URI，回调必须复用同一文件
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    // 系统相机：临时文件 → TakePicture(uri)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val target = pendingCameraFile
        pendingCameraFile = null
        if (ok && target != null) processAndSave(Uri.fromFile(target))
    }
    // 相册选择：Photo Picker（无需存储权限）
    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) processAndSave(uri)
    }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fluidPressScale(interactionSource)
            .border(1.dp, themedBorderLight(), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = { showPickerSheet = true },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddAPhoto, null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.snapnote_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (notes.isEmpty()) {
                        stringResource(R.string.snapnote_card_desc)
                    } else {
                        stringResource(R.string.snapnote_card_count, notes.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
                )
                // 最近笔记主色点行（最多 5 个）
                if (notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        notes.take(5).forEach { note ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(note.dominantHex))
                            )
                        }
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = themedTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    // ── 拍摄 / 选图入口 ─────────────────────────────────────────
    if (showPickerSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            sheetState = sheetState
        ) {
            SnapSourcePickerSheet(
                hasNotes = notes.isNotEmpty(),
                onTakePhoto = {
                    showPickerSheet = false
                    runCatching {
                        val tmp = snapTempFile(context)
                        pendingCameraFile = tmp
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context, context.packageName + ".fileprovider", tmp
                        )
                        cameraLauncher.launch(uri)
                    }
                },
                onPickImage = {
                    showPickerSheet = false
                    pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onOpenGallery = {
                    showPickerSheet = false
                    showGallerySheet = true
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── 笔记回顾 ────────────────────────────────────────────────
    if (showGallerySheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showGallerySheet = false },
            sheetState = sheetState
        ) {
            SnapNoteGallery(
                snapNoteManager = snapNoteManager,
                notes = notes,
                onDelete = { snapNoteManager.deleteNote(it) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 来源选择面板（纯 UI，拍摄/选图动作由父级闭包执行） */
@Composable
private fun SnapSourcePickerSheet(
    hasNotes: Boolean,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.snapnote_pick_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.snapnote_pick_desc),
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(14.dp))

        SnapSourceRow(
            icon = Icons.Default.AddAPhoto,
            gradient = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
            title = stringResource(R.string.snapnote_take_photo),
            onClick = onTakePhoto
        )
        Spacer(modifier = Modifier.height(10.dp))
        SnapSourceRow(
            icon = Icons.Default.PhotoLibrary,
            gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
            title = stringResource(R.string.snapnote_pick_image),
            onClick = onPickImage
        )
        if (hasNotes) {
            Spacer(modifier = Modifier.height(10.dp))
            SnapSourceRow(
                icon = Icons.Default.Photo,
                gradient = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                title = stringResource(R.string.snapnote_view_gallery),
                onClick = onOpenGallery,
                iconTint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SnapSourceRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
            .border(1.dp, themedBorderLight(), RoundedCornerShape(14.dp))
            .fluidPressScale(interactionSource, pressedScale = 0.97f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = themedTextPrimary(),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.ChevronRight, null,
            tint = themedTextSecondary().copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 笔记回顾面板：缩略图网格 + 色板条 + 删除 */
@Composable
private fun SnapNoteGallery(
    snapNoteManager: SnapNoteManager,
    notes: List<SnapNote>,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.snapnote_gallery_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.snapnote_gallery_desc),
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (notes.isEmpty()) {
            Text(
                text = stringResource(R.string.snapnote_gallery_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextSecondary(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                notes.chunked(2).forEach { rowNotes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowNotes.forEach { note ->
                            SnapNoteItem(
                                snapNoteManager = snapNoteManager,
                                note = note,
                                onDelete = onDelete,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowNotes.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun SnapNoteItem(
    snapNoteManager: SnapNoteManager,
    note: SnapNote,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, themedBorderLight(), RoundedCornerShape(14.dp))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = snapNoteManager.getImageFile(note),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            )
            IconButton(
                onClick = { onDelete(note.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        // 色板条：主色 + 色板
        Row(modifier = Modifier.fillMaxWidth().height(10.dp)) {
            (listOf(note.dominantHex) + note.swatchHexes).distinct().take(5).forEach { hex ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(parseHexColor(hex))
                )
            }
        }
        val locale = androidx.compose.ui.platform.LocalLocale.current.platformLocale
        Text(
            text = SimpleDateFormat("MM.dd HH:mm", locale).format(Date(note.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = themedTextSecondary(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

// ── 工具 ─────────────────────────────────────────────────────

private fun snapTempDir(context: Context): File =
    File(context.cacheDir, "snap").apply { if (!exists()) mkdirs() }

private fun snapTempFile(context: Context): File =
    File(snapTempDir(context), "snap_tmp_${System.currentTimeMillis()}.jpg")

internal fun parseHexColor(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
}
