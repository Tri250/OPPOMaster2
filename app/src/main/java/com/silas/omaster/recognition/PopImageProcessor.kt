package com.silas.omaster.recognition

import android.graphics.Bitmap
import com.silas.omaster.model.MasterPreset

/**
 * 【v1.7.8】POP 即拍全分辨率画质管线（Bitmap 编排层）。
 *
 * 目标画质对标 OPPO Reno17 Pro：保留传感器全分辨率（不再 2048px 降采样），
 * 条带化处理把峰值内存压在堆上限 62% 预算内，低端机不 OOM；
 * 高 ISO 手动曝光自动启用保边降噪，压噪点同时保边缘。
 *
 * 处理顺序（条带内，纯像素部分见 [PresetPixelEffects.processBandInto]）：
 * 色彩矩阵 → 保边降噪（ISO≥400）→ 亮度域 unsharp 锐化 → 径向暗角。
 *
 * 调用方持有 src 所有权：返回值为全新 Bitmap（或 src 本身，当尺寸过小无需处理）。
 * 【v1.5.7 所有权细化】发生内存守卫缩放（或 OOM 兜底再缩放）时，src 已无
 * 使用价值，由本函数内部即时回收——否则"原图 + 缩放图 + 输出图"三者共存，
 * 50MP 级输入峰值 3 张整图必 OOM；未缩放时 src 保持有效，仍由调用方回收
 * （重复 recycle 为幂等 no-op，调用方无需判空，但推荐 isRecycled 守卫）。
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
     * @param xdr Pro XDR 动态范围增强（默认关闭；开启后阴影提亮/高光回收/局部对比，成片更有质感）
     * @param sharpeningScale 锐化强度倍率（机型调校）：
     *        【v1.5.6 Reno15/16/17 机型适配】ColorOS 静态出片自带 HAL 锐化，
     *        该档位传 0.8 降权防双重锐化光晕（RenoDeviceProfile.sharpeningMultiplier）；
     *        默认 1.0 与既有引擎输出逐位一致
     * @param applyExposureEv 是否在拍后矩阵施加预设曝光补偿（默认 true）。
     *        【v1.5.6 P1 修复】相机路径传感器 AE 已写入 EV 时须传 false，
     *        避免"传感器 + 拍后矩阵"双重施加使净曝光翻倍；相册/系统相机路径
     *        无传感器写入，保持 true
     * @return 处理后的新 Bitmap；尺寸过小（<3px）无法做邻域运算时返回 src 本身
     */
    fun process(
        src: Bitmap,
        preset: MasterPreset,
        maxMemoryBytes: Long,
        xdr: Boolean = false,
        sharpeningScale: Float = 1f,
        applyExposureEv: Boolean = true
    ): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src

        val matrix = PresetColorTransform.buildColorMatrix(preset, applyExposureEv)
        val sharpen = PresetColorTransform.sharpnessAmount(preset) * sharpeningScale
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
            // 缩放后立即释放原图：缩放路径峰值 = 缩放图 + 输出图（2 张），
            // 与全分辨率路径一致（旧实现三者共存，50MP 级输入 3 张整图必 OOM）
            if (work !== src) src.recycle()
        }

        return try {
            renderBands(work, matrix, sharpen, denoise, vignette, xdr)
        } catch (e: OutOfMemoryError) {
            // 极端设备兜底（厂商 ROM 堆压缩 / Skia 解码缓冲超预估）：缩到 2048
            // 长边重渲染——宁小不崩，保证成片可保存；work 仍有效（只读）
            val side = maxOf(work.width, work.height)
            if (side <= 2048) throw e
            val scale = 2048f / side
            val sw = (work.width * scale).toInt().coerceAtLeast(1)
            val sh = (work.height * scale).toInt().coerceAtLeast(1)
            val smaller = Bitmap.createScaledBitmap(work, sw, sh, true)
            work.recycle()
            renderBands(smaller, matrix, sharpen, denoise, vignette, xdr)
        }
    }

    /**
     * 条带渲染：分配输出位图并按带全效果处理。
     * OOM 时回收半成品输出再上抛（供 [process] 的 2048 兜底或调用方处理）。
     */
    private fun renderBands(
        work: Bitmap,
        matrix: FloatArray,
        sharpen: Float,
        denoise: Float,
        vignette: Float,
        xdr: Boolean
    ): Bitmap {
        val out = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
        try {
            var y = 0
            while (y < work.height) {
                val y1 = minOf(work.height, y + BAND_ROWS)
                PresetPixelEffects.processBitmapBand(
                    work, out, y, y1, matrix, sharpen, denoise, vignette,
                    xdr = if (xdr) 1f else 0f
                )
                y = y1
            }
        } catch (e: OutOfMemoryError) {
            out.recycle()
            throw e
        }
        return out
    }
}
