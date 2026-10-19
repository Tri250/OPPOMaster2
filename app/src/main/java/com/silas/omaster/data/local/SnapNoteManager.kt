package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 【v1.6.14 发现 2.0】AiSnapNote 快拍笔记管理器
 * 拍摄 / 选图后端上取色（Palette），生成"拍摄笔记"：场景主色 + 色板 + 推荐预设。
 * 图片压缩为最长边 720px 的缩略图存入 filesDir/snap_notes/，笔记上限 30 条（超出时随记录清理图片）。
 */
class SnapNoteManager(context: Context) {

    private val context = context.applicationContext
    private val prefs: SharedPreferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _notesFlow = MutableStateFlow(loadNotes())
    val notesFlow: StateFlow<List<SnapNote>> = _notesFlow.asStateFlow()

    val notesDir: File get() = File(this.context.filesDir, NOTES_DIR).apply { if (!exists()) mkdirs() }

    /**
     * 保存笔记：压缩原图为缩略图并落盘，记录主色 / 色板 / 推荐预设。
     * @param dominantHex 主色（#RRGGBB），@param swatchHexes 色板（3-5 色）
     */
    fun saveNote(
        sourceImageUri: android.net.Uri,
        dominantHex: String,
        swatchHexes: List<String>,
        matchedPresetId: String? = null
    ): SnapNote? {
        val bitmap = decodeScaledBitmap(sourceImageUri) ?: return null
        val fileName = "snap_${System.currentTimeMillis()}.jpg"
        val file = File(notesDir, fileName)
        runCatching {
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 86, out) }
            bitmap.recycle()
        }
        if (!file.exists()) return null

        val note = SnapNote(
            id = "note_${System.currentTimeMillis()}",
            imagePath = fileName,
            dominantHex = dominantHex,
            swatchHexes = swatchHexes,
            matchedPresetId = matchedPresetId,
            timestamp = System.currentTimeMillis()
        )
        val updated = listOf(note) + _notesFlow.value
        persistTrimmed(updated)
        return note
    }

    fun deleteNote(noteId: String) {
        val target = _notesFlow.value.firstOrNull { it.id == noteId } ?: return
        File(notesDir, target.imagePath).delete()
        persistTrimmed(_notesFlow.value - target)
    }

    fun getImageFile(note: SnapNote): File = File(notesDir, note.imagePath)

    /**
     * 一体化处理：解码 → Palette 取色（主色 + 按人群占比排序的色板）→ 压缩落盘保存。
     * 相机临时文件（file:// 来源）在保存后清理。
     */
    fun processAndSaveNote(context: Context, sourceUri: android.net.Uri): SnapNote? {
        val bitmap = decodeForPalette(context, sourceUri) ?: return null
        return runCatching {
            val palette = androidx.palette.graphics.Palette.from(bitmap).maximumColorCount(24).generate()
            val dominant = palette.dominantSwatch
            val dominantHex = dominant?.let { String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and it.rgb) }
                ?: "#888888"
            val swatches = palette.swatches
                .filter { it != dominant }
                .sortedByDescending { it.population }
                .take(4)
                .map { String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and it.rgb) }
            val note = saveNote(sourceUri, dominantHex, swatches)
            if (note != null && sourceUri.scheme == "file") File(sourceUri.path ?: "").delete()
            note
        }.getOrNull().also { if (it == null) bitmap.recycle() }
    }

    /** 供取色用的降采样解码（最长边约 512px，避免大图 OOM） */
    private fun decodeForPalette(context: Context, uri: android.net.Uri): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sample = 1
            while (maxSide / (sample * 2) >= 512) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull()
    }

    private fun persistTrimmed(list: List<SnapNote>) {
        val kept = list.take(MAX_NOTES)
        // 超限记录的图片随记录一并清理
        list.drop(MAX_NOTES).forEach { File(notesDir, it.imagePath).delete() }
        prefs.edit().putString(KEY_NOTES, json.encodeToString(kept)).apply()
        _notesFlow.value = kept
    }

    private fun loadNotes(): List<SnapNote> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SnapNote>>(raw) }
            .getOrDefault(emptyList())
            .filter { File(notesDir, it.imagePath).exists() }
    }

    /** 解码并缩放原图到最长边 720px，避免 OOM 与存储膨胀 */
    private fun decodeScaledBitmap(uri: android.net.Uri): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            this@SnapNoteManager.context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sample = 1
            while (maxSide / (sample * 2) >= TARGET_MAX_SIDE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            this@SnapNoteManager.context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }?.let { bmp ->
                val w = bmp.width
                val h = bmp.height
                val scale = TARGET_MAX_SIDE.toFloat() / maxOf(w, h)
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(bmp, (w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), true)
                        .also { if (it !== bmp) bmp.recycle() }
                } else bmp
            }
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "snap_note_prefs"
        private const val KEY_NOTES = "notes"
        private const val NOTES_DIR = "snap_notes"
        private const val MAX_NOTES = 30
        private const val TARGET_MAX_SIDE = 720
    }
}

@Serializable
data class SnapNote(
    val id: String,
    val imagePath: String,
    val dominantHex: String,
    val swatchHexes: List<String> = emptyList(),
    val matchedPresetId: String? = null,
    val timestamp: Long = 0
)
