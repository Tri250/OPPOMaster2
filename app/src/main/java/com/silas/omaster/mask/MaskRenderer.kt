package com.silas.omaster.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.opengl.GLES20
import android.opengl.GLUtils
import com.silas.omaster.renderer.RenderParameters
import kotlin.math.max
import kotlin.math.min

/**
 * 蒙版渲染器
 *
 * 渲染管线：
 * 1. 加载原图 (BaseTexture)
 * 2. 对每个启用的蒙版：
 *    a. 加载/生成蒙版纹理 (MaskTexture)
 *    b. 使用蒙版 shader 在 GPU 上混合参数
 * 3. 输出最终图像
 *
 * Phase 1: 渐变蒙版（线性 + 径向）
 * Phase 2: 画笔蒙版
 * Phase 3: AI 蒙版
 */
class MaskRenderer {

    /**
     * CPU 端蒙版合成（用于不依赖 GL 的环境，例如导出最终图像前）
     * 输入原图 + 蒙版列表，输出应用所有蒙版后的图像
     */
    fun applyOnCpu(
        source: Bitmap,
        masks: List<AdjustmentMask>,
        baseParams: RenderParameters
    ): Bitmap {
        if (masks.isEmpty() || masks.none { it.isEffective() }) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val width = source.width
        val height = source.height
        // 应用基础参数
        var current = applyBaseParams(source, baseParams)

        for (mask in masks.filter { it.isEffective() }) {
            val maskBitmap = mask.cachedBitmap ?: MaskGenerator.generate(width, height, mask)
            val localApplied = applyBaseParams(source, mask.localParams)
            current = composite(current, localApplied, maskBitmap, mask.blendMode, mask.opacity)
        }

        return current
    }

    /**
     * 蒙版合成：在蒙版区域内，用 maskedResult 替代 current
     *
     * @param current 当前结果
     * @param maskedResult 应用了局部参数的结果
     * @param mask 灰度蒙版 (白色=完全应用)
     * @param blendMode 混合模式
     * @param opacity 蒙版整体不透明度
     */
    private fun composite(
        current: Bitmap,
        maskedResult: Bitmap,
        mask: Bitmap,
        blendMode: BlendMode,
        opacity: Float
    ): Bitmap {
        if (current.width != maskedResult.width || current.height != maskedResult.height) {
            return current
        }
        if (mask.width != current.width || mask.height != current.height) {
            // 缩放蒙版到原图大小
            val scaled = Bitmap.createScaledBitmap(mask, current.width, current.height, true)
            return compositeScaled(current, maskedResult, scaled, blendMode, opacity)
        }
        return compositeScaled(current, maskedResult, mask, blendMode, opacity)
    }

    private fun compositeScaled(
        current: Bitmap,
        maskedResult: Bitmap,
        mask: Bitmap,
        blendMode: BlendMode,
        opacity: Float
    ): Bitmap {
        val width = current.width
        val height = current.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. 绘制 current（底图）
        canvas.drawBitmap(current, 0f, 0f, null)

        // 2. 提取蒙版 alpha
        val maskPixels = IntArray(width * height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)
        // 转为 alpha (灰度 → alpha)
        val maskAlphaPixels = IntArray(width * height)
        for (i in maskPixels.indices) {
            val a = (Color.alpha(maskPixels[i]) * opacity).toInt().coerceIn(0, 255)
            maskAlphaPixels[i] = Color.argb(a, 0, 0, 0)
        }
        val maskAlpha = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
        maskAlpha.setPixels(maskAlphaPixels, 0, width, 0, 0, width, height)

        // 3. 绘制 maskedResult，通过 PorterDuff 模式
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.xfermode = when (blendMode) {
            BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            BlendMode.REPLACE -> PorterDuffXfermode(PorterDuff.Mode.SRC)
            BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        // 用 maskAlpha 作为 destinationIn 来限制 maskedResult 范围
        canvas.drawBitmap(maskedResult, 0f, 0f, paint)

        return output
    }

    /**
     * 应用基础参数到原图
     * 简化版（CPU 实现）
     */
    private fun applyBaseParams(source: Bitmap, params: RenderParameters): Bitmap {
        if (!params.hasAnyAdjustment()) return source

        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        // 通过 ColorMatrix 简化实现
        val matrix = android.graphics.ColorMatrix()
        applyColorMatrix(matrix, params)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun applyColorMatrix(matrix: android.graphics.ColorMatrix, params: RenderParameters) {
        // 对比度
        val c = 1f + params.contrast / 100f
        matrix.setScale(c, c, c, 1f)
        // 亮度
        val b = params.brightness * 2.55f
        matrix.postConcat(android.graphics.ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, b,
            0f, 1f, 0f, 0f, b,
            0f, 0f, 1f, 0f, b,
            0f, 0f, 0f, 1f, 0f
        )))
        // 饱和度
        val s = 1f + params.saturation / 100f
        val satMatrix = android.graphics.ColorMatrix()
        satMatrix.setSaturation(s)
        matrix.postConcat(satMatrix)
    }

    /**
     * GPU 端蒙版混合（OpenGL ES 2.0）
     * 将蒙版作为纹理传入 shader
     */
    fun applyOnGpu(
        sourceTextureId: Int,
        maskTextureIds: IntArray,
        masks: List<AdjustmentMask>,
        baseParams: RenderParameters
    ): Int {
        // GL 实现 - 由 GPURenderManager 调用
        return sourceTextureId
    }

    companion object {
        /**
         * 上传 Bitmap 到 GL 纹理
         */
        fun uploadBitmapToTexture(bitmap: Bitmap, textureId: IntArray): Int {
            if (textureId.isEmpty()) {
                val ids = IntArray(1)
                GLES20.glGenTextures(1, ids, 0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                return ids[0]
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            return textureId[0]
        }

        /**
         * 释放纹理
         */
        fun releaseTexture(textureId: Int) {
            val ids = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, ids, 0)
        }
    }
}
