package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max

/**
 * CameraX 帧格式转换器
 *
 * CameraX ImageAnalysis 默认输出 YUV_420_888 格式，无法直接用 BitmapFactory 解码。
 * 本工具类提供可靠的 YUV → RGB → Bitmap 转换，同时处理旋转，供实时预览、
 * AI 场景识别、GPU 渲染等后续链路统一使用。
 */
object CameraFrameConverter {

    private const val TAG = "CameraFrameConverter"

    /**
     * 将 [ImageProxy] 转换为 ARGB_8888 [Bitmap]。
     *
     * @param imageProxy CameraX 分析帧
     * @return 转换后的 Bitmap（调用方负责回收），失败返回 null
     */
    fun toBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        return try {
            val bitmap = yuv420ToBitmap(image) ?: return null
            val rotation = imageProxy.imageInfo.rotationDegrees
            if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "ImageProxy 转 Bitmap 失败", e)
            null
        } finally {
            imageProxy.close()
        }
    }

    /**
     * 将 [Image]（YUV_420_888）转换为 [Bitmap]（ARGB_8888）。
     *
     * 兼容 NV21（semi-planar）与 I420（planar）两种常见 YUV_420_888 布局，
     * 通过读取各平面的 pixelStride / rowStride 自动判断并转换。
     */
    fun yuv420ToBitmap(image: Image): Bitmap? {
        if (image.format != ImageFormat.YUV_420_888) {
            android.util.Log.e(TAG, "不支持的图像格式: ${image.format}，无法转换为 Bitmap")
            return null
        }

        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        // 检测 NV12 vs NV21：当 U/V plane pixelStride 均为 2 时为 semi-planar 布局
        // NV21: planes[1]=V 在前，planes[2]=U 在后（VU 交错）
        // NV12: planes[1]=U 在前，planes[2]=V 在后（UV 交错）
        // 通过比较 U/V buffer 的基址偏移来判断：
        // - 若 uPlane.buffer 基址 <= vPlane.buffer 基址，则为 NV12（U 在前）
        // - 否则为 NV21（V 在前）
        val isNV12 = (uvPixelStride == 2 && vPixelStride == 2) &&
            (uBuffer.position() <= vBuffer.position())

        val argb = IntArray(width * height)

        var yp = 0
        for (j in 0 until height) {
            val yRowStart = j * yRowStride
            val uvRowStart = (j shr 1) * uvRowStride
            val vRowStart = (j shr 1) * vRowStride
            for (i in 0 until width) {
                val yIndex = yRowStart + i
                val y = (0xff and yBuffer[yIndex].toInt()) - 16

                val uvCol = i shr 1
                val u: Int
                val v: Int
                if (uvPixelStride == 2 && vPixelStride == 2) {
                    // Semi-planar 布局：UV 或 VU 交错
                    val uvIndex = uvRowStart + uvCol * 2
                    if (isNV12) {
                        // NV12 布局：U/V 交错，U 在前
                        u = (0xff and uBuffer[uvIndex].toInt()) - 128
                        v = (0xff and uBuffer[uvIndex + 1].toInt()) - 128
                    } else {
                        // NV21 布局：V/U 交错，V 在前
                        v = (0xff and uBuffer[uvIndex].toInt()) - 128
                        u = (0xff and uBuffer[uvIndex + 1].toInt()) - 128
                    }
                } else {
                    // I420 布局：U、V 分平面
                    u = (0xff and uBuffer[uvRowStart + uvCol].toInt()) - 128
                    v = (0xff and vBuffer[vRowStart + uvCol].toInt()) - 128
                }

                val y1192 = 1192 * y
                var r = y1192 + 1634 * v
                var g = y1192 - 833 * v - 400 * u
                var b = y1192 + 2066 * u

                r = clampRgb(r)
                g = clampRgb(g)
                b = clampRgb(b)

                argb[yp++] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(argb, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun clampRgb(value: Int): Int {
        return value.coerceIn(0, 262143).shr(10) and 0xFF
    }

    /**
     * 按目标最大边长缩放 Bitmap，用于 AI 分析或实时预览降采样。
     */
    fun scaleToMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
