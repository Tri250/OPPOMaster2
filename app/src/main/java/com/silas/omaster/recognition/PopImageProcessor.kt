package com.silas.omaster.recognition

import android.graphics.Bitmap
import com.silas.omaster.model.MasterPreset

/**
 * 【v1.7.8】POP 即拍全分辨率画质管线（Bitmap 编排层）。
 *
 * 目标画质对标 OPPO Reno17 Pro：保留传感器全分辨率（不再 2048px 降采样），
 * 条带化处理把峰值内存压在堆上限 45% 内，低端机不 OOM；
 * 高 ISO 手动曝光自动启用保边降噪，压噪点同时保边缘。
 *
 * 处理顺序（条带内，纯像素部分见 [PresetPixelEffects.processBandInto]）：
 * 色彩矩阵 → 保边降噪（ISO≥400）→ 亮度域 unsharp 锐化 → 径向暗角。
 *
 * 调用方持有 src 所有权：返回值为全新 Bitmap（或 src 本身，当尺寸过小无需处理），
 * 由调用方决定是否回收 src。
 */
object PopImageProcessor {

    /** 每带中心行数：192 行 @12MP ≈ 3MB/带，峰值内存与处理进度兼顾 */
    private const val BAND_ROWS = 192

    /** ISO ≥ 此值启用保边降噪（手动曝光高感场景） */
    private const val DENOISE_ISO_THRESHOLD = 400

    /**
     * 全分辨率处理入口。
     *
     * @param maxMemoryBytes Runtime.getRuntime().maxMemory()，用于内存守卫
     * @return 处理后的新 Bitmap；尺寸过小（<3px）无法做邻域运算时返回 src 本身
     */
    fun process(src: Bitmap, preset: MasterPreset, maxMemoryBytes: Long): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src

        val matrix = PresetColorTransform.buildColorMatrix(preset)
        val sharpen = PresetColorTransform.sharpnessAmount(preset)
        val vignette = PresetColorTransform.vignetteStrength(preset)
        val iso = PresetCapturePlan.parseIso(preset.iso) ?: 0
        val denoise = if (iso >= DENOISE_ISO_THRESHOLD) 0.55f else 0f

        // 内存守卫：极端分辨率（108MP 模式 / 超大堆外设备）按面积等比收缩
        var work = src
        val guardMax = PresetPixelEffects.chooseStyleMaxSide(w, h, maxMemoryBytes)
        if (guardMax > 0 && maxOf(w, h) > guardMax) {
            val scale = guardMax.toFloat() / maxOf(w, h)
            val sw = (w * scale).toInt().coerceAtLeast(1)
            val sh = (h * scale).toInt().coerceAtLeast(1)
            work = Bitmap.createScaledBitmap(src, sw, sh, true)
        }

        val out = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
        var y = 0
        while (y < work.height) {
            val y1 = minOf(work.height, y + BAND_ROWS)
            PresetPixelEffects.processBitmapBand(
                work, out, y, y1, matrix, sharpen, denoise, vignette
            )
            y = y1
        }
        if (work !== src) work.recycle()
        return out
    }
}
