package com.silas.omaster.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * 实时滤镜取景分析器（POP 即拍"所见即所得"核心）。
 *
 * 把预设色彩矩阵逐帧应用到相机画面：
 * - 色彩矩阵与拍后套色同源（[PresetColorTransform.buildColorMatrix]）→ 取景与成片观感一致；
 * - 暗角按 [PresetPixelEffects.applyVignette] 同参数径向叠加；
 * - 锐化为拍后专属（预览不做邻域像素运算，保证低功耗流畅）；
 * - ImageAnalysis 目标 1280x720 + KEEP_ONLY_LATEST，逐帧节流约 30fps；
 * - 方向：ImageProxy.toBitmap() 返回的是**传感器方向**像素（CameraX 不自动旋转，
 *   见官方 orientation-rotation 文档与 ImageUtil.createBitmapFromImageProxy 实现），
 *   必须按 [androidx.camera.core.ImageInfo.rotationDegrees] 自行转正，否则竖屏取景横躺 90°。
 *
 * 线程模型：分析器运行在单线程 Executor，通过 [enabled] 控制启停
 * （成片展示时暂停分析，重新取景自动恢复）。
 */
class FilteredPreviewAnalyzer(
    colorMatrix: FloatArray,
    private val vignetteStrength: Float,
    private val onFrame: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    /** false 时跳过处理（仅释放帧），用于成片预览时暂停实时分析 */
    @Volatile
    var enabled: Boolean = true

    private val colorPaint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(colorMatrix))
    }
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val canvas = Canvas()
    private var vignetteWidth = 0
    private var vignetteHeight = 0

    // 双缓冲输出位图交替写入，避免 Compose 读取时撕裂
    private var buffers = arrayOfNulls<Bitmap>(2)
    private var flip = false
    private var lastEmit = 0L

    /**
     * 尺寸变化时被替换下来的旧缓冲。延后一帧回收：
     * Compose 侧可能仍持有上一帧（同一 Bitmap 实例）尚未重绘，
     * 立即 recycle 会触发 "Canvas: trying to use a recycled bitmap" 崩溃。
     */
    private var retired: Bitmap? = null

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (!enabled || now - lastEmit < FRAME_INTERVAL_MS) {
            image.close()
            return
        }
        // toBitmap() 只转像素格式、不应用旋转，方向需按 rotationDegrees 自行转正（v1.8.0 修复）
        val rotation = runCatching { image.imageInfo.rotationDegrees }.getOrDefault(0)
        val src = try {
            rotateToUpright(image.toBitmap(), rotation)
        } catch (e: Exception) {
            null
        } finally {
            image.close()
        } ?: return
        lastEmit = now

        flip = !flip
        val out = obtainBuffer(flip, src.width, src.height) ?: run {
            src.recycle()
            return
        }
        canvas.setBitmap(out)
        canvas.drawBitmap(src, 0f, 0f, colorPaint)
        if (vignetteStrength > 0.02f) drawVignette(out)
        canvas.setBitmap(null)
        src.recycle()
        onFrame(out)
    }

    /** 复用同尺寸缓冲，尺寸变化（首帧/分辨率切换）时重建，旧缓冲延后回收 */
    private fun obtainBuffer(front: Boolean, w: Int, h: Int): Bitmap? {
        val slot = if (front) 0 else 1
        val existing = buffers[slot]
        if (existing != null && !existing.isRecycled && existing.width == w && existing.height == h) {
            return existing
        }
        val created = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            buffers[slot] = null
            null
        } ?: return null
        if (existing != null && !existing.isRecycled) {
            // 上一轮退役缓冲此时已无人绘制，可安全回收
            retired?.recycle()
            retired = existing
        }
        buffers[slot] = created
        return created
    }

    /** 分析器销毁时释放全部缓冲（含延后回收的退役实例） */
    fun release() {
        enabled = false
        buffers.forEach { it?.recycle() }
        buffers[0] = null
        buffers[1] = null
        retired?.recycle()
        retired = null
    }

    /**
     * 径向暗角：与 PresetPixelEffects.applyVignette 严格同参数（所见即所得）——
     * 中心 (w-1)/2、半径 0.75×长边、0.55 处开始线性压暗、边缘 alpha=0.62×强度×255。
     */
    private fun drawVignette(out: Bitmap) {
        val w = out.width
        val h = out.height
        if (vignetteWidth != w || vignetteHeight != h) {
            val cx = (w - 1) / 2f
            val cy = (h - 1) / 2f
            val radius = maxOf(w, h) * 0.75f
            val darkAlpha = (vignetteStrength * 0.62f * 255f).toInt().coerceIn(0, 158)
            vignettePaint.alpha = 0xFF
            vignettePaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0x00000000, darkAlpha shl 24),
                floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
            )
            vignetteWidth = w
            vignetteHeight = h
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)
    }

    companion object {
        /** 约 30fps 节流：滤镜取景够流畅且省电 */
        private const val FRAME_INTERVAL_MS = 33L
    }
}

/**
 * 按 rotationDegrees（顺时针转正角度）把位图转为正立方向，成功旋转后回收原图。
 *
 * CameraX 的 ImageProxy.toBitmap()（YUV/JPEG 皆然）只转换像素格式、不应用旋转，
 * 取景帧与相机成片都必须经此转正（v1.8.0，修复 POP 取景方向错误）。
 */
internal fun rotateToUpright(src: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return src
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    if (rotated !== src) src.recycle()
    return rotated
}
