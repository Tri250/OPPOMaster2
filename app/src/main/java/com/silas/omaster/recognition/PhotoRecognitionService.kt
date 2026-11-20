package com.silas.omaster.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.StringRes
import com.silas.omaster.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.silas.omaster.util.ExifInfo
import com.silas.omaster.util.ExifReader

/**
 * 照片识别服务（Android 层组装入口）：Uri → 解码 Bitmap → EXIF + 风格分析 → RecognitionResult。
 *
 * 隐私：全本地处理；仅解码用户主动选择的单张图；不读取/留存 GPS；
 * 原图不写入预设（coverPath 置空），分析结果不含任何 PII。
 */
object PhotoRecognitionService {

    /** 识别失败的结构化异常（文案经资源 ID 本地化，避免硬编码中文） */
    sealed class RecognitionError(@StringRes val hintRes: Int) {
        object DecodeFailed : RecognitionError(R.string.recog_error_decode_failed)
        object TooSmall : RecognitionError(R.string.recog_error_too_small)
        object TooLarge : RecognitionError(R.string.recog_error_too_large)
    }

    sealed class Result {
        data class Success(val result: RecognitionResult, val bitmap: Bitmap) : Result()
        data class Failure(val error: RecognitionError) : Result()
    }

    suspend fun recognize(context: Context, uri: Uri): Result = withContext(Dispatchers.IO) {
        try {
            // 1. 读取 EXIF（内部已过滤 PII）
            val exif: ExifInfo = ExifReader.read(context, uri)

            // 2. 解码 Bitmap（先读边界，超大图降采样）
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext Result.Failure(RecognitionError.DecodeFailed)
            }
            if (bounds.outWidth < 100 || bounds.outHeight < 100) {
                return@withContext Result.Failure(RecognitionError.TooSmall)
            }
            if (bounds.outWidth.toLong() * bounds.outHeight > 200_000_000L) {
                return@withContext Result.Failure(RecognitionError.TooLarge)
            }

            val sampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@withContext Result.Failure(RecognitionError.DecodeFailed)

            // 3. 风格分析
            val analysis = StyleAnalyzer.analyze(bitmap)

            // 4. 组装引擎输入
            val input = RecognitionInput(
                hasExif = exif.hasPhotoParams,
                aperture = exif.aperture,
                shutterSpeed = exif.shutterSpeed,
                iso = exif.iso,
                whiteBalance = exif.whiteBalance,
                colorTemperature = exif.colorTemperature,
                exposureBias = exif.exposureBias,
                focusMm = exif.focalLength,
                sourceLabel = extractSourceLabel(exif),
                styleAnalysis = analysis
            )

            Result.Success(PhotoRecognitionEngine.recognize(input), bitmap)
        } catch (e: OutOfMemoryError) {
            Result.Failure(RecognitionError.TooLarge)
        } catch (e: Exception) {
            Result.Failure(RecognitionError.DecodeFailed)
        }
    }

    /** 机型来源标签：只保留营销机型名，过滤序列号等设备标识 */
    private fun extractSourceLabel(exif: ExifInfo): String? {
        val make = exif.make?.trim()
        val model = exif.model?.trim()
        return when {
            !make.isNullOrBlank() && !model.isNullOrBlank() && !model.equals(make, true) ->
                "$make $model".take(30)
            !model.isNullOrBlank() -> model.take(30)
            !make.isNullOrBlank() -> make.take(30)
            else -> null
        }
    }

    /** 长边 >1600 时按比例降采样，控制内存与耗时 */
    private fun computeInSampleSize(w: Int, h: Int): Int {
        val maxEdge = 1600
        val longest = maxOf(w, h)
        return if (longest <= maxEdge) 1 else (longest / maxEdge).coerceAtLeast(1)
    }
}
