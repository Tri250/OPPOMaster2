package com.silas.omaster.recognition

import android.graphics.Bitmap
import kotlin.math.sqrt

/**
 * 预设像素级效果（锐化 / 暗角）。
 *
 * 拍后取景与成片共用；性能敏感，均为纯像素运算。
 * 内存策略：输入为可变 Bitmap 时**原地**处理（不额外分配整图），
 * 与 POP 成片管线的降采样（2048px）配合，把 12MP 原图的套色峰值内存
 * 从 ~300MB 压到 ~75MB。
 */
object PresetPixelEffects {

    /** 中心权重 1、四邻 0.25/8…的 3x3 unsharp 近似：dst = src + amount * (src - blur)。src 与 dst 不可为同一数组。 */
    fun sharpenPixelsInto(src: IntArray, dst: IntArray, w: Int, h: Int, amount: Float) {
        System.arraycopy(src, 0, dst, 0, src.size)
        // amount 极小时无感，直接拷贝
        if (amount <= 0.016f) return
        val a = (amount * 1.2f).coerceAtMost(1.0f)

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val i = row + x
                val c = src[i]
                val cr = (c shr 16) and 0xFF
                val cg = (c shr 8) and 0xFF
                val cb = c and 0xFF
                // 四邻平均（blur 近似）
                val l = src[i - 1]; val r = src[i + 1]; val u = src[i - w]; val d = src[i + w]
                val lr = (l shr 16) and 0xFF; val rr = (r shr 16) and 0xFF
                val ur = (u shr 16) and 0xFF; val dr = (d shr 16) and 0xFF
                val lg = (l shr 8) and 0xFF; val rg = (r shr 8) and 0xFF
                val ug = (u shr 8) and 0xFF; val dg = (d shr 8) and 0xFF
                val lb = l and 0xFF; val rb = r and 0xFF
                val ub = u and 0xFF; val db = d and 0xFF
                val blurR = (lr + rr + ur + dr + 2) / 4
                val blurG = (lg + rg + ug + dg + 2) / 4
                val blurB = (lb + rb + ub + db + 2) / 4
                val nr = (cr + a * (cr - blurR)).toInt().coerceIn(0, 255)
                val ng = (cg + a * (cg - blurG)).toInt().coerceIn(0, 255)
                val nb = (cb + a * (cb - blurB)).toInt().coerceIn(0, 255)
                dst[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
    }

    /**
     * 3x3 unsharp 锐化。输入为可变 Bitmap 时原地处理并返回同一实例；不可变时返回新 Bitmap。
     * amount <= 0.016 视为无感，直接返回原图。
     */
    fun applySharpen(src: Bitmap, amount: Float): Bitmap {
        if (amount <= 0.016f) return src
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src

        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)

        // 注意：src/dst 不可别名——锐化要读"原始邻域"，必须用独立输出数组
        val out = IntArray(w * h)
        sharpenPixelsInto(px, out, w, h, amount)

        if (src.isMutable) {
            src.setPixels(out, 0, w, 0, 0, w, h)
            return src
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    // ------------------------------------------------------------------
    // 【v1.7.8】全分辨率画质管线（OPPO Reno17 Pro 级质感）
    //
    // 旧管线把 12MP 成片降采样到 2048px 再套色，成片细节直接损失 4 倍像素。
    // 新管线按"行带（band）"处理全分辨率位图：每带只分配带内缓冲，
    // 整图峰值内存从 ~300MB 降到 ~65MB（12MP），低端机不再 OOM，
    // 同时输出保持传感器全分辨率。
    //
    // 带内顺序（摄影后期惯例）：
    //   ① 4x5 色彩矩阵（滤镜/饱和度/冷暖/影调/色温/EV/柔光）
    //   ② 亮度域高斯模糊 → 高频残差（边缘权重）
    //   ③ 保边降噪（仅高 ISO 启用）：平坦区向邻域均值 blend，边缘不动
    //   ④ 亮度域 unsharp 锐化：结构细节增强，色彩零串染（luma-only）
    //   ⑤ 径向暗角（逐像素定位；取景预览 [FilteredPreviewAnalyzer] 同公式，所见即所得）
    //
    // 带上下各留 [DEFAULT_RADIUS] 行重叠供邻域运算，只写中心行，
    // 任意分带数量结果与整带一致（无接缝，单测锁定）。
    // ------------------------------------------------------------------

    /** 邻域半径：高斯 unsharp / 降噪共用（5x5 邻域，画质与速度平衡点） */
    const val DEFAULT_RADIUS: Int = 2

    /** 高频残差低于此值（0-255 亮度单位）视为平坦区/噪点：锐化权重 0，交给降噪 */
    private const val EDGE_LOW = 6f

    /** 高频残差高于此值视为真实边缘：享受满额锐化并免降噪 */
    private const val EDGE_HIGH = 30f

    /** smoothstep：残差 → 边缘权重（0..1），噪点不锐化、细纹理过渡、强边满额 */
    private fun edgeWeight(residual: Float): Float {
        val t = ((residual - EDGE_LOW) / (EDGE_HIGH - EDGE_LOW)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * 内存守卫：返回"可安全全分辨率处理的最大边长"，0 = 当前尺寸可全分辨率处理。
     *
     * 全分辨率管线峰值 ≈ 2 张整图（输入+输出，各 4B/px）+ ~32MB 瞬态
     * （JPEG 解码缓冲 / 条带数组 / 缩放中间图）。预算取堆上限 45%，
     * 超限时按面积等比收缩，保证任何堆大小的设备都不 OOM。
     *
     * 【v1.8.0 画质】应用已声明 largeHeap（AndroidManifest），主流设备堆上限
     * 提升至 ~512MB，预算 ≈ 198MB → 允许 ≈ 2470 万像素，12MP 成片不再降采样；
     * 此前无 largeHeap 时 192–256MB 堆会把 12MP 缩到 ~3K 长边（约 3–4 倍像素损失，
     * 即"画质压缩感"主因）。
     *
     * @param maxMemoryBytes Runtime.getRuntime().maxMemory()；<=0 时按 256MB 保守估计
     */
    fun chooseStyleMaxSide(w: Int, h: Int, maxMemoryBytes: Long): Int {
        if (w <= 0 || h <= 0) return 0
        val heap = if (maxMemoryBytes > 0) maxMemoryBytes else 256L * 1024 * 1024
        val pixels = w.toLong() * h.toLong()
        val budget = (heap * 0.45f).toLong() - 32L * 1024 * 1024
        if (budget <= 0) return 2048
        val allowedPixels = budget / 8
        if (pixels <= allowedPixels) return 0
        val scale = Math.sqrt(allowedPixels.toDouble() / pixels)
        return (maxOf(w, h) * scale).toInt().coerceAtLeast(1080)
    }

    /**
     * 单条带全效果处理（纯像素，可 JVM 单测；条带间接缝由重叠行消除）。
     *
     * @param band   输入带像素（含上下各 radius 行重叠），长度 bandH*w，处理中被原地修改
     * @param out    输出带像素（仅中心行 [padTop, padTop+centerH) 被写入），长度同 band
     * @param w      图像宽
     * @param imgH   图像高（暗角径向定位用）
     * @param bandTop band 首行对应的整图 y 坐标（暗角定位用）
     * @param bandH  band 总行数（含重叠）
     * @param padTop 上方重叠行数（图像顶部为 0）
     * @param centerH 本带待输出的中心行数
     * @param matrix 4x5 行主序色彩矩阵；null = 跳过矩阵
     * @param sharpenAmount 锐度 0..1
     * @param denoise 保边降噪强度 0..1（0 = 关闭）
     * @param vignette 暗角强度 0..1（0 = 关闭）
     */
    fun processBandInto(
        band: IntArray,
        out: IntArray,
        w: Int,
        imgH: Int,
        bandTop: Int,
        bandH: Int,
        padTop: Int,
        centerH: Int,
        matrix: FloatArray?,
        sharpenAmount: Float,
        denoise: Float,
        vignette: Float,
        radius: Int = DEFAULT_RADIUS
    ) {
        System.arraycopy(band, 0, out, 0, band.size)
        if (w <= 0 || bandH <= 0 || centerH <= 0) return

        // ① 色彩矩阵（整带）
        if (matrix != null) applyMatrixInto(band, w, bandH, matrix)

        // ② 亮度 + 可分离高斯模糊（供边缘权重 / 降噪 / 锐化共用）
        val lum = IntArray(w * bandH)
        for (i in band.indices) {
            val c = band[i]
            lum[i] = (((c shr 16) and 0xFF) * 54 + ((c shr 8) and 0xFF) * 183 + (c and 0xFF) * 19) shr 8
        }
        val blur = IntArray(w * bandH)
        gaussianBlurInto(lum, blur, w, bandH, radius)

        val cx = (w - 1) / 2f
        val cy = (imgH - 1) / 2f
        val maxSide = maxOf(w, imgH).toFloat()
        val vigRadius = maxSide * 0.75f
        val vigDark = (0.62f * vignette).coerceIn(0f, 0.62f)

        val doDenoise = denoise > 0.01f
        val doSharpen = sharpenAmount > 0.02f
        val doVignette = vignette > 0.02f

        // ③④⑤ 仅中心行输出（上下重叠行只参与邻域运算）
        for (ly in padTop until padTop + centerH) {
            val row = ly * w
            val absY = bandTop + ly
            for (x in 0 until w) {
                val i = row + x
                val edgeW = edgeWeight(Math.abs(lum[i] - blur[i]).toFloat())

                var c = band[i]

                // ③ 保边降噪：平坦区（edgeW 低）向四邻均值 blend，边缘保留
                if (doDenoise && edgeW < 0.9f) {
                    val flat = 1f - edgeW
                    val a = denoise * 0.85f * flat
                    val l = if (x > 0) band[i - 1] else c
                    val r = if (x < w - 1) band[i + 1] else c
                    val u = if (ly > 0) band[i - w] else c
                    val dn = if (ly < bandH - 1) band[i + w] else c
                    c = blendPixel(c, l, r, u, dn, a)
                }

                // ④ 亮度域 unsharp：结构增强、色彩零串染；smoothstep 权重保证
                // 平坦区噪点不被放大（权重 0），强边满额（权重 1）
                if (doSharpen) {
                    val eff = sharpenAmount * 1.6f * edgeW
                    val delta = (lum[i] - blur[i]).toFloat() * eff
                    if (delta > 0.5f || delta < -0.5f) c = addLumaDelta(c, delta)
                }

                // ⑤ 径向暗角（0.75*maxSide 半径，0.55→1 渐暗；取景预览同公式）
                if (doVignette) {
                    val dx = x - cx
                    val dy = absY - cy
                    val dist = sqrt(dx * dx + dy * dy)
                    val t = ((dist / vigRadius - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    if (t > 0.001f) c = scalePixel(c, 1f - vigDark * t)
                }

                out[i] = c
            }
        }
    }

    /**
     * Bitmap 层条带调度：从 src 抽取带（含上下重叠行）→ 纯像素处理 → 写回 dst 中心行。
     * 任意分带方式结果一致（接缝由重叠行消除）。
     */
    fun processBitmapBand(
        src: Bitmap,
        dst: Bitmap,
        y0: Int,
        y1: Int,
        matrix: FloatArray?,
        sharpenAmount: Float,
        denoise: Float,
        vignette: Float,
        radius: Int = DEFAULT_RADIUS
    ) {
        val w = src.width
        val h = src.height
        val top = (y0 - radius).coerceAtLeast(0)
        val bot = (y1 + radius).coerceAtMost(h)
        val padTop = y0 - top
        val bandH = bot - top
        if (w <= 0 || bandH <= 0) return
        val band = IntArray(w * bandH)
        src.getPixels(band, 0, w, 0, top, w, bandH)
        val out = IntArray(w * bandH)
        processBandInto(
            band, out, w, h, top, bandH, padTop, y1 - y0,
            matrix, sharpenAmount, denoise, vignette, radius
        )
        // 写回必须跳过带顶 padTop 行重叠：若从 0 开始，除首带外每带都会把
        // padTop 行未完成降噪/锐化/暗角的重叠行写进成片并整体错位 → 每 192 行
        // 出现横向"线条"（v1.7.9 真机缺陷根因）。偏移公式由 [bandWriteSrcOffset]
        // 提供，与单测共用，防止回归。
        dst.setPixels(out, bandWriteSrcOffset(padTop, w), w, 0, y0, w, y1 - y0)
    }

    /**
     * 条带写回源偏移：dst 写回 [y0, y1) 时，源数组必须从带顶 padTop 重叠行之后开始。
     * 单测 `条带写回偏移必须跳过带顶重叠行` 依赖此公式的正确性。
     */
    fun bandWriteSrcOffset(padTop: Int, w: Int): Int = padTop * w

    /** 4x5 行主序矩阵原地应用（ARGB_8888 像素，结果钳位 0..255） */
    fun applyMatrixInto(px: IntArray, w: Int, h: Int, matrix: FloatArray) {
        if (matrix.size < 20) return
        val m0 = matrix[0]; val m1 = matrix[1]; val m2 = matrix[2]
        val m3 = matrix[3]; val m4 = matrix[4]
        val m5 = matrix[5]; val m6 = matrix[6]; val m7 = matrix[7]
        val m8 = matrix[8]; val m9 = matrix[9]
        val m10 = matrix[10]; val m11 = matrix[11]; val m12 = matrix[12]
        val m13 = matrix[13]; val m14 = matrix[14]
        for (i in 0 until w * h) {
            val c = px[i]
            val a = (c ushr 24) and 0xFF
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val nr = (m0 * r + m1 * g + m2 * b + m3 * a + m4).toInt().coerceIn(0, 255)
            val ng = (m5 * r + m6 * g + m7 * b + m8 * a + m9).toInt().coerceIn(0, 255)
            val nb = (m10 * r + m11 * g + m12 * b + m13 * a + m14).toInt().coerceIn(0, 255)
            px[i] = (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
    }

    /** 可分离高斯模糊（二项式核近似，半径 1..4；边界复制） */
    fun gaussianBlurInto(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        if (w <= 0 || h <= 0) return
        if (radius <= 0) {
            System.arraycopy(src, 0, dst, 0, src.size)
            return
        }
        val kernel = binomialKernel(radius)
        val k = kernel.size
        val half = k / 2
        val tmp = IntArray(w * h)
        // 水平
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                var acc = 0f
                for (t in 0 until k) {
                    val xx = (x + t - half).coerceIn(0, w - 1)
                    acc += kernel[t] * src[row + xx]
                }
                tmp[row + x] = (acc + 0.5f).toInt().coerceIn(0, 255)
            }
        }
        // 垂直
        for (y in 0 until h) {
            for (x in 0 until w) {
                var acc = 0f
                for (t in 0 until k) {
                    val yy = (y + t - half).coerceIn(0, h - 1)
                    acc += kernel[t] * tmp[yy * w + x]
                }
                dst[y * w + x] = (acc + 0.5f).toInt().coerceIn(0, 255)
            }
        }
    }

    /** 二项式系数核（Pascal 行 2r），已归一化：r=2 → [1,4,6,4,1]/16，r=1 → [1,2,1]/4 */
    private fun binomialKernel(radius: Int): FloatArray {
        var row = floatArrayOf(1f)
        repeat((radius.coerceIn(1, 4)) * 2) {
            val next = FloatArray(row.size + 1)
            for (i in next.indices) {
                val l = if (i > 0) row[i - 1] else 0f
                val r = if (i < row.size) row[i] else 0f
                next[i] = l + r
            }
            row = next
        }
        var sum = 0f
        for (v in row) sum += v
        return FloatArray(row.size) { row[it] / sum }
    }

    /** 向四邻均值按比例 blend（保边降噪） */
    private fun blendPixel(c: Int, l: Int, r: Int, u: Int, d: Int, a: Float): Int {
        if (a <= 0.001f) return c
        val mr = (ch(l, 16) + ch(r, 16) + ch(u, 16) + ch(d, 16)) / 4f
        val mg = (ch(l, 8) + ch(r, 8) + ch(u, 8) + ch(d, 8)) / 4f
        val mb = (ch(l, 0) + ch(r, 0) + ch(u, 0) + ch(d, 0)) / 4f
        val cr = ch(c, 16).toFloat()
        val cg = ch(c, 8).toFloat()
        val cb = ch(c, 0).toFloat()
        val nr = (cr + (mr - cr) * a).toInt().coerceIn(0, 255)
        val ng = (cg + (mg - cg) * a).toInt().coerceIn(0, 255)
        val nb = (cb + (mb - cb) * a).toInt().coerceIn(0, 255)
        return ((c ushr 24) shl 24) or (nr shl 16) or (ng shl 8) or nb
    }

    /** 取像素指定通道（0..255） */
    private fun ch(px: Int, shift: Int) = (px shr shift) and 0xFF

    /** RGB 三通道同加减同一亮度增量（钳位；alpha 不变）——luma-only 锐化 */
    private fun addLumaDelta(c: Int, delta: Float): Int {
        val a = (c ushr 24) and 0xFF
        val r = (((c shr 16) and 0xFF) + delta).toInt().coerceIn(0, 255)
        val g = (((c shr 8) and 0xFF) + delta).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) + delta).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** RGB 整体缩放（暗角用，钳位；alpha 不变） */
    private fun scalePixel(c: Int, f: Float): Int {
        val a = (c ushr 24) and 0xFF
        val r = (((c shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
        val g = (((c shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
        val b = ((c and 0xFF) * f).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
