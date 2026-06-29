package com.silas.omaster.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 裁剪比例预设
 */
enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("自由", null),
    ORIGINAL("原始", -1f),
    SQUARE("1:1", 1f),
    RATIO_4_3("4:3", 4f / 3f),
    RATIO_3_2("3:2", 3f / 2f),
    RATIO_16_9("16:9", 16f / 9f),
    RATIO_9_16("9:16", 9f / 16f),
    RATIO_5_4("5:4", 5f / 4f),
    RATIO_2_35_1("2.35:1", 2.35f)
}

/**
 * 裁剪旋转引擎
 * 提供几何校正能力：裁剪、旋转、翻转
 *
 * 操作链路：
 * 1. 用户选择裁剪比例
 * 2. 在图片上拖动/缩放裁剪框
 * 3. 调整旋转角度
 * 4. 应用后生成新的 Bitmap
 */
class CropRotateEngine {

    /**
     * 裁剪并旋转图片
     * @param bitmap 原图
     * @param cropRect 归一化裁剪区域 [0,1]（相对于原图），null 表示不裁剪
     * @param rotationDegrees 旋转角度（顺时针）
     * @param flipHorizontal 水平翻转
     * @param flipVertical 垂直翻转
     * @return 处理后的 Bitmap
     */
    fun cropAndRotate(
        bitmap: Bitmap,
        cropRect: RectF? = null,
        rotationDegrees: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false
    ): Bitmap {
        val matrix = Matrix()

        // 1. 翻转（在裁剪前应用，使翻转基于原图）
        val sx = if (flipHorizontal) -1f else 1f
        val sy = if (flipVertical) -1f else 1f
        if (flipHorizontal || flipVertical) {
            matrix.postScale(sx, sy, bitmap.width / 2f, bitmap.height / 2f)
        }

        // 2. 旋转
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees, bitmap.width / 2f, bitmap.height / 2f)
        }

        // 3. 先应用矩阵变换，得到中间图
        val transformedBitmap = if (rotationDegrees != 0f || flipHorizontal || flipVertical) {
            // 计算变换后的边界
            val tempRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
            matrix.mapRect(tempRect)
            val transWidth = tempRect.width().toInt()
            val transHeight = tempRect.height().toInt()

            val transBitmap = Bitmap.createBitmap(transWidth, transHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(transBitmap)
            // 平移使内容居中
            canvas.translate(-tempRect.left, -tempRect.top)
            canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
            transBitmap
        } else {
            bitmap
        }

        // 4. 裁剪
        return if (cropRect != null) {
            val srcWidth = transformedBitmap.width
            val srcHeight = transformedBitmap.height
            val x = (cropRect.left * srcWidth).toInt().coerceIn(0, srcWidth)
            val y = (cropRect.top * srcHeight).toInt().coerceIn(0, srcHeight)
            val width = ((cropRect.right - cropRect.left) * srcWidth).toInt().coerceIn(1, srcWidth - x)
            val height = ((cropRect.bottom - cropRect.top) * srcHeight).toInt().coerceIn(1, srcHeight - y)

            Bitmap.createBitmap(transformedBitmap, x, y, width, height)
        } else {
            // 如果没有指定裁剪但做了变换，返回变换后的图
            if (transformedBitmap !== bitmap) transformedBitmap else bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    /**
     * 根据比例和约束计算初始裁剪框
     * @param imageWidth 图片宽
     * @param imageHeight 图片高
     * @param aspectRatio 目标比例（宽/高），null=自由比例，-1f=原始比例
     * @return 归一化裁剪框 RectF [0,1]
     */
    fun calculateInitialCropRect(
        imageWidth: Int,
        imageHeight: Int,
        aspectRatio: Float?
    ): RectF {
        val imgRatio = imageWidth.toFloat() / imageHeight

        val targetRatio = when {
            aspectRatio == null -> imgRatio // 自由比例：使用整张图
            aspectRatio < 0 -> imgRatio     // 原始比例
            else -> aspectRatio
        }

        var cropW: Float
        var cropH: Float

        if (imgRatio > targetRatio) {
            // 图片更宽，以高度为基准
            cropH = 1f
            cropW = targetRatio / imgRatio
        } else {
            // 图片更高或相等，以宽度为基准
            cropW = 1f
            cropH = imgRatio / targetRatio
        }

        // 居中
        val left = (1f - cropW) / 2f
        val top = (1f - cropH) / 2f
        return RectF(left, top, left + cropW, top + cropH)
    }

    /**
     * 约束裁剪框在图片范围内，并保持比例
     * @param rect 待约束的裁剪框（归一化）
     * @param aspectRatio 目标比例（null=不约束比例）
     * @return 约束后的裁剪框
     */
    fun constrainCropRect(rect: RectF, aspectRatio: Float?): RectF {
        val result = RectF(rect)

        // 先确保在 [0,1] 范围内
        result.left = result.left.coerceIn(0f, 0.99f)
        result.top = result.top.coerceIn(0f, 0.99f)
        result.right = result.right.coerceIn(result.left + 0.01f, 1f)
        result.bottom = result.bottom.coerceIn(result.top + 0.01f, 1f)

        // 保持比例
        if (aspectRatio != null && aspectRatio > 0) {
            val currentRatio = (result.width()) / (result.height())
            if (abs(currentRatio - aspectRatio) > 0.001f) {
                // 以宽度为基准调整高度
                val newHeight = result.width() / aspectRatio
                if (result.top + newHeight <= 1f) {
                    result.bottom = result.top + newHeight
                } else {
                    // 以高度为基准调整宽度
                    val newWidth = result.height() * aspectRatio
                    result.right = result.left + newWidth
                }
            }
        }

        return result
    }

    /**
     * 快速旋转 90/180/270 度（无裁剪，保持原尺寸）
     */
    fun rotateByMultiple90(bitmap: Bitmap, times: Int): Bitmap {
        val t = ((times % 4) + 4) % 4
        if (t == 0) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val matrix = Matrix().apply {
            postRotate(t * 90f, bitmap.width / 2f, bitmap.height / 2f)
        }

        val tempRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        matrix.mapRect(tempRect)

        val result = Bitmap.createBitmap(tempRect.width().toInt(), tempRect.height().toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.translate(-tempRect.left, -tempRect.top)
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

    /**
     * 水平翻转
     */
    fun flipHorizontal(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 垂直翻转
     */
    fun flipVertical(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply { postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
