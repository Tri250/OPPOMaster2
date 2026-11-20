package com.silas.omaster.recognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.hypot

/**
 * 实时滤镜取景分析器（POP 即拍"所见即所得"核心）。
 *
 * 把预设色彩矩阵逐帧应用到相机画面：
 * - 色彩矩阵与拍后套色同源（[PresetColorTransform.buildColorMatrix]）→ 取景与成片观感一致；
 * - 暗角按 [PresetPixelEffects.applyVignette] 同参数径向叠加；
 * - 锐化为拍后专属（预览不做邻域像素运算，保证低功耗流畅）；
 * - ImageAnalysis 默认 640x480 + KEEP_ONLY_LATEST，逐帧节流约 30fps。
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
        val src = try {
            // CameraX 1.3+：YUV/JPEG 均已应用 imageInfo.rotationDegrees，方向无需自转
            image.toBitmap()
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
        if (vignetteStrength > 0f) drawVignette(out)
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

    /** 径向暗角（与 PresetPixelEffects.applyVignette 同参数），按尺寸缓存着色器 */
    private fun drawVignette(out: Bitmap) {
        val w = out.width
        val h = out.height
        if (vignetteWidth != w || vignetteHeight != h) {
            val cx = w / 2f
            val cy = h / 2f
            val radius = hypot(cx, cy)
            vignettePaint.shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(0x00000000, 0xFF000000.toInt()),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP
            )
            vignettePaint.alpha = (vignetteStrength * 170f).toInt().coerceIn(1, 255)
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
