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
 * 【v1.5.8 OPPO 影像算法对标】确有可见效果（或 XDR 开启）的成片进入"画质档"：
 * 纹理保护降噪 v2（ISO 分档）+ Vibrance 选择性饱和 + 场景微色彩（天空/绿植/
 * 肤色，1/8 下采样统计一次）+ Filmic 基础影调 + 多频段锐化（r2 细节 + r6 结构，
 * 光晕钳制）+ 肤色保护阻尼；中性预设保持 legacy 逐位路径（单测契约锁定）。
 *
 * 处理顺序（条带内，纯像素部分见 [PresetPixelEffects.processBandInto]）：
 * 画质档：色彩矩阵 → Vibrance/场景色 → 纹理保护降噪 → XDR → Filmic → 多频段锐化 → 暗角；
 * legacy：色彩矩阵 → 保边降噪（ISO≥400）→ XDR → unsharp 锐化 → 暗角。
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

    /** ISO ≥ 此值启用保边降噪（手动曝光高感场景）——legacy 路径（quality=0）口径 */
    private const val DENOISE_ISO_THRESHOLD = 400

    /**
     * 【v1.5.8】画质档降噪强度（ISO 分档，纹理保护型）。
     *
     * OPPO 自动模式在任何感光度都做降噪（低感轻、高感重），而 legacy 路径
     * 只在 ISO≥400 时以固定 0.55 启用——低感成片噪点完全不管、高感又偏糊。
     * 画质档按 ISO 分档，且强度经"纹理门控 + 肤色阻尼"消耗（[PresetPixelEffects]），
     * 低感轻降噪只碰平坦区噪点，不糊细节。
     */
    private fun qualityDenoiseStrength(iso: Int): Float = when {
        iso <= 0 || iso < 200 -> 0.30f   // Auto/低感：基础降噪（OPPO 自动模式口径）
        iso < 800 -> 0.42f
        iso < 1600 -> 0.55f
        else -> 0.68f                     // 高感：重点压噪
    }

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

        // 【v1.5.8 OPPO 级画质档】确有可见效果（或 XDR 开启）时启用画质档：
        // 纹理保护降噪 v2 / Vibrance + 场景微色彩 / Filmic 影调 / 多频段锐化。
        // 中性预设（无任何效果且 XDR 关）保持 legacy 逐位路径——单测
        // `处理管线_中性预设不改像素` 等契约锁定，不进任何新阶段。
        val quality = if (PresetColorTransform.hasVisibleEffect(preset, applyExposureEv) || xdr) 1f else 0f
        val denoise = if (quality > 0f) {
            qualityDenoiseStrength(iso)
        } else {
            if (iso >= DENOISE_ISO_THRESHOLD) 0.55f else 0f
        }

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

        // 场景统计（1/8 下采样一次，画质档专属；失败/NONE 时场景微色彩关闭）
        val scene = if (quality > 0f) PresetPixelEffects.analyzeSceneStats(work) else PresetPixelEffects.SceneStats.NONE
        // Vibrance：满灰度滤镜（黑白/单色 100%）不重新引入色彩；部分灰度按 (1−a)
        // 保留百分比语义（与 v1.8.8 调色盘百分比契约同向）
        val grayAmount = PresetColorTransform.filterAdjustment(preset.filter)
            .let { if (it.grayscale) it.grayscaleAmount else 0f }
        val chromaBoost = if (quality > 0f) PresetPixelEffects.VIBRANCE_BASE * (1f - grayAmount) else 0f

        return try {
            renderBands(work, matrix, sharpen, denoise, vignette, xdr, quality, scene, chromaBoost)
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
            renderBands(smaller, matrix, sharpen, denoise, vignette, xdr, quality, scene, chromaBoost)
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
        xdr: Boolean,
        quality: Float = 0f,
        scene: PresetPixelEffects.SceneStats = PresetPixelEffects.SceneStats.NONE,
        chromaBoost: Float = 0f
    ): Bitmap {
        val out = Bitmap.createBitmap(work.width, work.height, Bitmap.Config.ARGB_8888)
        try {
            var y = 0
            while (y < work.height) {
                val y1 = minOf(work.height, y + BAND_ROWS)
                PresetPixelEffects.processBitmapBand(
                    work, out, y, y1, matrix, sharpen, denoise, vignette,
                    xdr = if (xdr) 1f else 0f,
                    quality = quality,
                    scene = scene,
                    chromaBoost = chromaBoost
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
