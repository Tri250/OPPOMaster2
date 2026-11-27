package com.silas.omaster.recognition

import android.graphics.Bitmap
import kotlin.math.sqrt

/**
 * 预设像素级效果（锐化 / 暗角 / 降噪 / XDR）。
 *
 * 拍后取景与成片共用；性能敏感，均为纯像素运算。
 * 内存策略：输入为可变 Bitmap 时**原地**处理（不额外分配整图），
 * 与 POP 成片管线的全分辨率条带处理（[PopImageProcessor]）配合，
 * 把 12MP 原图的套色峰值内存压在堆上限 62% 预算内，低端机不 OOM。
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
    // v1.7.7 及以前把 12MP 成片降采样到 2048px 再套色，成片细节直接损失 4 倍像素。
    // 新管线按"行带（band）"处理全分辨率位图：每带只分配带内缓冲，
    // 峰值 = 输入/输出 2 张整图 + ~24MB 瞬态，由内存守卫（62% 堆预算）兜底，
    // 主流 12MP 成片零降采样且低端机不 OOM。
    //
    // 【v1.5.8 OPPO 影像算法对标】新增"画质档"（quality > 0，仅非中性预设/XDR 开启时启用），
    // 按 OPPO ColorOS 影像链路逐级复刻其可落地阶段：
    //   ① 4x5 色彩矩阵（滤镜/饱和度/冷暖/影调/色温/EV/柔光）
    //   ② Vibrance 选择性饱和 + 场景微色彩（天空/绿植/肤色，低彩度优先 + 肤色保护）
    //   ③ 亮度域高斯模糊 → 高频残差（边缘权重）
    //   ④ 纹理保护降噪（仅高 ISO 启用）：模糊目标 + 纹理门控 + 色度压噪 + 肤色阻尼
    //   ⑤ Pro XDR 动态范围增强（阴影提亮 + 高光回收 + 局部对比，luma-only）
    //   ⑥ Filmic 基础影调（S 曲线 + 黑白场保护，luma-only）
    //   ⑦ 多频段 unsharp 锐化（r2 细节 + r6 结构 + 光晕钳制 + 肤色阻尼，luma-only）
    //   ⑧ 径向暗角（逐像素定位；取景预览 [FilteredPreviewAnalyzer] 同公式，所见即所得）
    //
    // 带上下各留 [DEFAULT_RADIUS] 行重叠供邻域运算，只写中心行，
    // 任意分带数量结果与整带一致（无接缝，单测锁定）。
    //
    // 【画质档门控】quality 缺省 0 = 旧路径逐位不变（所有既有契约测试零改动通过）；
    // quality > 0 时②④⑥⑦切换到 OPPO 级实现。取景预览只承载矩阵 + 影调 LUT
    // （XDR 全局 + Filmic 合成单一事实来源 [buildCombinedToneLut]），
    // 细节层（降噪/锐化/清晰度/Vibrance/场景色）与既有定位一致为拍后专属。
    // ------------------------------------------------------------------

    /** 邻域半径：高斯 unsharp / 降噪共用（5x5 邻域，画质与速度平衡点） */
    const val DEFAULT_RADIUS: Int = 2

    /** 高频残差低于此值（0-255 亮度单位）视为平坦区/噪点：锐化权重 0，交给降噪 */
    private const val EDGE_LOW = 4f

    /** 高频残差高于此值视为真实边缘：享受满额锐化并免降噪 */
    private const val EDGE_HIGH = 26f

    // ════════════════════════════════════════════════════════════════
    // 【v1.5.8】OPPO 级画质档（quality > 0 时启用的全部新阶段）
    //
    // 设计基线 = OPPO ColorOS 影像链路中"单帧 JPEG 域可实现"的阶段
    // （RAW/NPU/10bit P3 为架构性差距，见 CHANGELOG v1.5.8 缺口清单）：
    // 纹理保护降噪 / Vibrance 选择性饱和 / 场景微色彩 / 肤色保护 /
    // 多频段锐化 / Filmic 影调。全部纯像素、条带内可算、无整图分配。
    // ════════════════════════════════════════════════════════════════

    // ── Vibrance 选择性饱和（OPPO"鲜明"质感：低彩度优先，已饱和少动）──
    /** 基础增强量：低彩度像素的色度提升比例（OPPO 自动增强量级，非夸张） */
    const val VIBRANCE_BASE: Float = 0.12f
    /** 彩度低于此值视为无色相信息（纯灰/满灰度滤镜输出）：不增强，保黑白身份 */
    private const val VIB_CHROMA_MIN = 6
    /** 高彩度门控：彩度 96→180 平滑收敛到 0（已很艳的像素不提，防"炸"） */
    private const val VIB_GATE_LO = 96f
    private const val VIB_GATE_HI = 180f
    /** 肤色区 Vibrance 减半（防肤色"塑料感"，与 OPPO 肤色保护同向） */
    private const val VIB_SKIN_KEEP = 0.5f

    // ── 场景微色彩（OPPO AI 场景色彩的可落地简化版）──
    /** 天空判定：蓝主导且亮（B−R ∈ 10..120，luma > 100） */
    private const val SKY_BLUE_LO = 10
    private const val SKY_BLUE_HI = 120
    private const val SKY_LUMA_MIN = 100
    /** 绿植判定：绿主导且中间调（G−R/G−B > 6，luma ∈ 40..210） */
    private const val GREEN_GAP = 6
    private const val GREEN_LUMA_LO = 40
    private const val GREEN_LUMA_HI = 210
    /** 场景增强量（绝对色度比例，OPPO 场景色彩量级）：天空蓝 / 绿植绿 / 肤色血色。
     *  仅在场景统计命中（≥6% 像素）后施加于对应区域；按 (amount/[VIBRANCE_BASE])
     *  缩放——满灰度（amount=0）整体关闭，部分灰度按保留比例缩放 */
    private const val SCENE_SKY_BOOST = 0.10f
    private const val SCENE_GREEN_BOOST = 0.09f
    private const val SCENE_SKIN_BOOST = 0.08f
    /** 场景统计命中门控：像素占比 6%→18% 平滑启用（小面积色块不触发） */
    private const val SCENE_GATE_LO = 0.06f
    private const val SCENE_GATE_HI = 0.18f

    // ── 肤色保护（OPPO 人像链路核心：肤色不塑料化）──
    private const val SKIN_R_MIN = 70
    private const val SKIN_CHROMA_LO = 15
    private const val SKIN_CHROMA_HI = 90
    private const val SKIN_RB_LO = 15
    private const val SKIN_RB_HI = 110
    /** 肤色区降噪/锐化阻尼：保留系数 0.4（OPPO 对肤色细节的保守处理） */
    const val SKIN_DETAIL_KEEP: Float = 0.4f

    // ── 纹理保护降噪 v2（替代四邻均值：模糊目标 + flat 门控 + 肤色阻尼）──
    /** 降噪强度上限（防过抹：即使平坦区也不超过此 blend 比例） */
    private const val DENOISE_V2_MAX = 0.75f
    /** 色度压噪比例（彩色噪点：色度向四邻色度均值 blend，亮度向模糊目标） */
    private const val CHROMA_DENOISE_RATIO = 0.6f
    /**
     * 二项式模糊核中心权重（5x5 可分离，中心 = (6/16)² = 0.140625）。
     * 降噪亮度目标 = **中心排除**的模糊 T = (blur − kc×y)/(1−kc)，代数上等于
     * "去掉中心像素后的 24 邻二项式加权均值"：既不含中心（噪声压缩优于四邻
     * 均值：方差比 0.30 vs 0.34），又比四邻均值更贴局部结构、对弱边更温和。
     * v1.5.8 首版直接向含中心的 blur blend——中心权重把中心噪点留在目标里，
     * 实测压噪反而弱于旧路径（0.427 vs 0.40），修正为中心排除口径。
     */
    private const val BLUR_CENTER_WEIGHT = 0.140625f
    private const val BLUR_CENTER_INV = 1f / (1f - BLUR_CENTER_WEIGHT)

    // ── 多频段锐化（r2 细节 + r6 结构，光晕钳制）──
    /** r2 细节频段倍率（与 v1.5.7 的 2.2 同量级，略降给结构频段让位） */
    private const val SHARP_R2_SCALE = 2.0f
    /** r6 结构频段倍率（中频对比，OPPO"质感/punch"来源；低于细节频段，防中频过冲） */
    private const val SHARP_R6_SCALE = 0.7f
    /** r6 频段门控：残差 6→40 平滑过渡（该频段噪点能量天然低，强边满额） */
    private const val EDGE6_LOW = 6f
    private const val EDGE6_HIGH = 40f
    /** 光晕钳制：单像素锐化增量上限 ±30 亮度（防硬边白/黑边，OPPO 光晕控制）。
     *  取值保证内置预设锐度量程（锐度 ≤15 → 有效锐化 ≤22）不被钳住，
     *  仅调色盘高锐度（>25）才触及上限——高锐度仍可感知且无光晕 */
    private const val SHARP_OVERSHOOT = 30f

    // ── Filmic 基础影调（S 曲线 + 黑白场保护，luma-only）──
    /** S 曲线强度：out = l + A×(smoothstep(t)−t)×255，A=0.16 时最大 ±3.9 亮度、
     *  斜率 ∈[0.84,1.08] 恒正（无影调翻转/色带）。对标 OPPO 输出 tone curve 的
     *  "微妙但可感知"量级——质感来源而非滤镜感 */
    private const val FILMIC_CONTRAST = 0.16f
    /** 黑场保护：luma<10 区最多压暗 1.5（真黑"沉得下去"，与 XDR 黑场锚点同向） */
    private const val FILMIC_BLACK = 1.5f
    private const val FILMIC_BLACK_END = 10f
    /** 白场保护：luma>244 区最多提亮 1.5（白墙/反光通透不发灰） */
    private const val FILMIC_WHITE = 1.5f
    private const val FILMIC_WHITE_START = 244f

    /** smoothstep：值 → 0..1 平滑权重（与 [edgeWeight] 同曲线，复用语义） */
    private fun smoothstep(lo: Float, hi: Float, v: Float): Float {
        val t = ((v - lo) / (hi - lo)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * 场景统计（一次下采样得到；NONE = 无场景命中 → 场景微色彩全部关闭）。
     *
     * 由 [analyzeSceneStats] 在 1/8 缩略图上按像素规则统计占比：
     * 天空 / 绿植 / 肤色各 0..1。统计只用于**门控**场景增强是否启用
     * （小面积色块不触发），具体哪些像素命中由逐像素规则判定。
     */
    data class SceneStats(
        val sky: Float = 0f,
        val green: Float = 0f,
        val skin: Float = 0f
    ) {
        companion object {
            val NONE = SceneStats()
        }
    }

    /** 天空像素规则：蓝主导且亮（B−R ∈ 10..120 且 luma > 100） */
    fun isSkyPixel(r: Int, g: Int, b: Int): Boolean {
        val blue = b - r
        return blue in SKY_BLUE_LO..SKY_BLUE_HI &&
            ((r * 54 + g * 183 + b * 19) shr 8) > SKY_LUMA_MIN
    }

    /** 绿植像素规则：绿主导且中间调（G−R>6 且 G−B>6 且 luma ∈ 40..210） */
    fun isGreenPixel(r: Int, g: Int, b: Int): Boolean {
        val luma = (r * 54 + g * 183 + b * 19) shr 8
        return g - r > GREEN_GAP && g - b > GREEN_GAP &&
            luma in GREEN_LUMA_LO..GREEN_LUMA_HI
    }

    /**
     * 肤色像素规则（保守口径：宁可漏检也不错检）：
     * R>70、R≥G+8、G≥B、彩度 ∈15..90、R−B ∈15..110。
     * 用于肤色区降噪/锐化阻尼与 Vibrance 减半（OPPO 肤色保护的可落地近似）。
     */
    fun isSkinPixel(r: Int, g: Int, b: Int): Boolean {
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val chroma = mx - mn
        return r > SKIN_R_MIN && r >= g + 8 && g >= b &&
            chroma in SKIN_CHROMA_LO..SKIN_CHROMA_HI &&
            (r - b) in SKIN_RB_LO..SKIN_RB_HI
    }

    /** r6 结构频段边缘权重（低频段噪点能量天然低，门控区间 6..40） */
    private fun edgeWeight6(residual: Float): Float = smoothstep(EDGE6_LOW, EDGE6_HIGH, residual)

    /**
     * 256 级亮度 → Filmic 影调调整量表（luma delta，可正可负）。
     *
     * 影调结构（摄影 output tone curve 惯例，luma-only 无色偏）：
     * - S 曲线：out = l + A×(smoothstep(t)−t)×255（A=[FILMIC_CONTRAST]），
     *   暗部稍压、亮部稍提、中间调对比增强——OPPO 成片"质感"的核心来源；
     *   斜率恒正（f' ∈ [1−A, 1+A/2]），无影调翻转/色带（单测逐级锁定）；
     * - 黑场保护：luma<12 最多再压 3（真黑"沉得下去"）；
     * - 白场保护：luma>236 最多再提 3（白场通透）。
     * 强度 ≤0 返回全零表（关闭）。
     */
    fun buildFilmicLut(strength: Float): FloatArray {
        val lut = FloatArray(256)
        if (strength <= 0.01f) return lut
        for (l in 0..255) {
            val t = l / 255f
            val s = t * t * (3f - 2f * t)
            var out = l + strength * FILMIC_CONTRAST * (s - t) * 255f
            if (l < FILMIC_BLACK_END) {
                out -= strength * FILMIC_BLACK * (1f - l / FILMIC_BLACK_END)
            }
            if (l > FILMIC_WHITE_START) {
                out += strength * FILMIC_WHITE * ((l - FILMIC_WHITE_START) / (255f - FILMIC_WHITE_START))
            }
            lut[l] = out.coerceIn(0f, 255f) - l
        }
        return lut
    }

    /**
     * 合成影调查找表：XDR 全局项 + Filmic（取景预览用，单一事实来源）。
     *
     * 拍后 [processBandInto] 对同一亮度施加 `xdrGlobalLumaAdjust(l, xdr) + filmicLut[l]`，
     * 本表逐项相等——预览所见 = 拍后所得（清晰度/锐化/降噪仍为拍后专属）。
     * 两项强度任一 ≤0 时该项全零（关闭）。
     */
    fun buildCombinedToneLut(xdr: Float, filmic: Float): FloatArray {
        val xdrLut = buildXdrToneLut(xdr)
        val filmicLut = buildFilmicLut(filmic)
        return FloatArray(256) { xdrLut[it] + filmicLut[it] }
    }

    /**
     * 场景统计：1/8 缩略图一次采样（内存峰值 +0.8MB@12MP，统计后即时回收）。
     * 缩略图 averaging 天然抑制噪点，占比口径更稳。不可用 → NONE（增强关闭）。
     */
    fun analyzeSceneStats(src: Bitmap): SceneStats {
        val w = src.width
        val h = src.height
        if (w < 16 || h < 16) return SceneStats.NONE
        val small = try {
            Bitmap.createScaledBitmap(src, maxOf(1, w / 8), maxOf(1, h / 8), true)
        } catch (e: Exception) {
            return SceneStats.NONE
        }
        try {
            val sw = small.width
            val sh = small.height
            val px = IntArray(sw * sh)
            small.getPixels(px, 0, sw, 0, 0, sw, sh)
            var sky = 0
            var green = 0
            var skin = 0
            for (c in px) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                when {
                    isSkyPixel(r, g, b) -> sky++
                    isGreenPixel(r, g, b) -> green++
                    isSkinPixel(r, g, b) -> skin++
                }
            }
            val n = px.size.toFloat()
            return SceneStats(sky / n, green / n, skin / n)
        } finally {
            small.recycle()
        }
    }

    /**
     * Vibrance 选择性饱和 + 场景微色彩（整带原地，逐像素无色域分配）。
     *
     * 与色彩矩阵的全局线性饱和分工：矩阵承载用户滑杆意图（1:1），
     * 本阶段承载 OPPO 自动增强——低彩度优先提升、高彩度不动、
     * 肤色减半、天空/绿植/肤色命中按场景统计量追加微幅增强。
     * 彩度 < [VIB_CHROMA_MIN] 的像素（纯灰/满灰度输出）逐位不动。
     *
     * @param amount 基础增强量（0 = 关闭；满灰度预设由调用方按 (1−灰度强度) 折算后传入）
     */
    fun vibranceInto(px: IntArray, amount: Float, scene: SceneStats) {
        if (amount <= 0.01f) return
        val skyGate = smoothstep(SCENE_GATE_LO, SCENE_GATE_HI, scene.sky)
        val greenGate = smoothstep(SCENE_GATE_LO, SCENE_GATE_HI, scene.green)
        val skinGate = smoothstep(SCENE_GATE_LO, SCENE_GATE_HI, scene.skin)
        // 场景增强为绝对色度比例，按 (amount/VIBRANCE_BASE) 缩放：满灰度（amount=0，
        // 调用方已门控不会进入）整体关闭，部分灰度按保留比例生效
        val sceneScale = amount / VIBRANCE_BASE
        for (i in px.indices) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val chroma = maxOf(r, g, b) - minOf(r, g, b)
            if (chroma < VIB_CHROMA_MIN) continue
            val gate = smoothstep(VIB_GATE_LO, VIB_GATE_HI, chroma.toFloat())
            var boost = amount * (1f - gate)
            val skin = isSkinPixel(r, g, b)
            if (skin) boost *= VIB_SKIN_KEEP
            if (isSkyPixel(r, g, b)) boost += SCENE_SKY_BOOST * skyGate * sceneScale
            else if (isGreenPixel(r, g, b)) boost += SCENE_GREEN_BOOST * greenGate * sceneScale
            else if (skin) boost += SCENE_SKIN_BOOST * skinGate * sceneScale
            if (boost <= 0.005f) continue
            val f = 1f + boost
            // 保持亮度：c' = y + (c − y) × f（色度残差等比放大）
            val y = (r * 54 + g * 183 + b * 19) shr 8
            val nr = (y + (r - y) * f).toInt().coerceIn(0, 255)
            val ng = (y + (g - y) * f).toInt().coerceIn(0, 255)
            val nb = (y + (b - y) * f).toInt().coerceIn(0, 255)
            px[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
    }

    /**
     * 纹理保护降噪 v2（画质档专用，替代四邻均值）。
     *
     * 与旧路径的分工：
     * - 旧路径：平坦区（edgeW 低）向**四邻均值** blend——均值目标只含 4 个
     *   邻居、不贴结构，细纹理区（残差 4~26）也被部分糊掉（v1.5.6"模糊"
     *   反馈的成因之一）；
     * - 新路径：亮度向**中心排除的结构感知模糊目标** blend——
     *   T = (blur − kc×y)/(1−kc) = 去掉中心后的 24 邻二项式加权均值：
     *   不含中心像素（噪声方差压缩 0.30 vs 四邻均值 0.34，实测更强），
     *   24 邻加权比 4 邻均值更贴局部结构、对弱边更温和；
     * - 彩色噪点：只 blend **色度残差**（c−y）向四邻色度残差均值
     *   （[CHROMA_DENOISE_RATIO]）——灰像素色度为 0 天然不动；
     * - 肤色区按 [SKIN_DETAIL_KEEP] 阻尼（不塑料化）。
     *
     * 诚实边界：单像素尺度上二值噪点与细纹理统计特征相似（OPPO 靠 AI 分割
     * 区分），故纹理保护来自 flat 门控（残差 >26 的边不降、4→26 平滑收敛）
     * 与模糊目标的低梯度破坏性，而非理想化的"纹理检测"。
     *
     * @param aLuma 亮度 blend 比例（已含 flat 门控/肤色阻尼，调用方计算）
     * @param aChroma 色度 blend 比例（aLuma × [CHROMA_DENOISE_RATIO]）
     */
    private fun denoisePixelV2(
        c: Int, l: Int, r: Int, u: Int, d: Int,
        blurLuma: Int, aLuma: Float, aChroma: Float
    ): Int {
        // ① 色度压噪：只 blend 色度残差（c − y）——灰像素色度为 0 天然不动，
        // 不会被"整通道向均值混合"二次平滑亮度（v1.5.8 首版按整通道 blend，
        // 细纹理反而比旧路径更糊，实测修正为色度残差口径）
        var p = c
        if (aChroma > 0.002f) {
            val cr = ch(p, 16); val cg = ch(p, 8); val cb = ch(p, 0)
            val y = (cr * 54 + cg * 183 + cb * 19) shr 8
            fun chromaOf(px: Int, out: IntArray) {
                val r = ch(px, 16); val g = ch(px, 8); val b = ch(px, 0)
                val yy = (r * 54 + g * 183 + b * 19) shr 8
                out[0] = r - yy; out[1] = g - yy; out[2] = b - yy
            }
            val cl = IntArray(3); val cn = IntArray(3); val cu = IntArray(3); val cd = IntArray(3)
            chromaOf(l, cl); chromaOf(r, cn); chromaOf(u, cu); chromaOf(d, cd)
            val mr = (cl[0] + cn[0] + cu[0] + cd[0]) / 4f
            val mg = (cl[1] + cn[1] + cu[1] + cd[1]) / 4f
            val mb = (cl[2] + cn[2] + cu[2] + cd[2]) / 4f
            val nr = (y + ((cr - y) + (mr - (cr - y)) * aChroma)).toInt().coerceIn(0, 255)
            val ng = (y + ((cg - y) + (mg - (cg - y)) * aChroma)).toInt().coerceIn(0, 255)
            val nb = (y + ((cb - y) + (mb - (cb - y)) * aChroma)).toInt().coerceIn(0, 255)
            p = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
        }
        // ② 亮度压噪：向**中心排除**的结构感知模糊目标 blend（色度结构不动）
        if (aLuma > 0.002f) {
            val cr = ch(p, 16).toFloat(); val cg = ch(p, 8).toFloat(); val cb = ch(p, 0).toFloat()
            val y = (cr * 54f + cg * 183f + cb * 19f) / 256f
            val target = (blurLuma - BLUR_CENTER_WEIGHT * y) * BLUR_CENTER_INV
            val delta = (target - y) * aLuma
            if (delta > 0.5f || delta < -0.5f) p = addLumaDelta(p, delta)
        }
        return p
    }

    /**
     * Box 模糊（running sum，h+v 各一趟；边界复制）。
     *
     * 用于 r6 结构频段：单盒半径 [STRUCTURE_BOX_RADIUS]（9 抽头）的
     * h+v 可分离盒 ≈ σ≈3.65 的低通——若用二项式核要到 19 抽头，
     * CPU 成本高一个量级；盒式 running sum 每像素 4 次加法，与半径无关。
     * 盒式频响有零点，但仅取"原图−模糊"的残差做 unsharp，观感等价。
     */
    fun boxBlurInto(src: IntArray, dst: IntArray, w: Int, h: Int, radius: Int) {
        if (w <= 0 || h <= 0) return
        val r = radius.coerceAtLeast(1)
        val win = 2 * r + 1
        // 水平：src → dst（行内环形缓冲读原值，写 dst 不污染）
        for (y in 0 until h) {
            val row = y * w
            var sum = 0
            // 初始窗口（x=0，左侧边界复制）
            for (t in 0 until win) {
                val xx = (t - r).coerceIn(0, w - 1)
                sum += src[row + xx]
            }
            for (x in 0 until w) {
                dst[row + x] = (sum + win / 2) / win
                val outX = (x - r).coerceIn(0, w - 1)
                val inX = (x + r + 1).coerceIn(0, w - 1)
                sum += src[row + inX] - src[row + outX]
            }
        }
        // 垂直：dst → dst（列内环形缓冲；dst 行已含水平结果，读原列值需缓存）
        val col = IntArray(win)
        for (x in 0 until w) {
            var sum = 0
            for (t in 0 until win) {
                val yy = (t - r).coerceIn(0, h - 1)
                col[t] = dst[yy * w + x]
                sum += col[t]
            }
            var head = 0
            for (y in 0 until h) {
                val idx = y * w + x
                val v = dst[idx]
                dst[idx] = (sum + win / 2) / win
                // 环形缓冲推进：移出 col[head]，读入下一行原值
                val outY = (y - r).coerceIn(0, h - 1)
                val inY = (y + r + 1).coerceIn(0, h - 1)
                val inV = dst[inY * w + x]
                sum += inV - col[head]
                col[head] = inV
                head = (head + 1) % win
            }
        }
    }

    /** r6 结构频段盒半径：9 抽头 h+v 盒 ≈ σ3.65 低通（中频对比/质感频段） */
    const val STRUCTURE_BOX_RADIUS: Int = 4

    /**
     * 画质档降噪强度计算（flat 门控 + 肤色阻尼后）。
     *
     * 设计说明（OPPO"AI 纹理保护降噪"的可落地近似，诚实边界）：
     * 单像素尺度上二值噪点与细纹理的统计特征相似（v1.5.8 首版试过方向相干
     * 门控 resH/resV，量化零点造成假"方向性"，实测劣于旧路径，已回退）——
     * OPPO 靠 AI 分割区分二者，本项目无此算力。故纹理保护来自三道保守机制：
     * - flat = 1−edgeW：模糊域残差 >26 的真实边缘（含斜边）完全不降，
     *   且随残差能量 4→26 平滑收敛——纹理幅度越大保护越强（与旧路径同哲学）；
     * - 模糊目标（5x5 二项式，中心权重 6/16）比四邻均值更贴近局部结构，
     *   同强度下对梯度的破坏更小（细纹理对比保留更多）；
     * - 肤色区按 [SKIN_DETAIL_KEEP] 阻尼（不塑料化）。
     */
    private fun qualityDenoiseAlphas(
        edgeW: Float, denoise: Float, skinKeep: Float
    ): Pair<Float, Float> {
        val aLuma = (denoise * 0.85f * (1f - edgeW) * skinKeep).coerceAtMost(DENOISE_V2_MAX)
        return aLuma to (aLuma * CHROMA_DENOISE_RATIO)
    }

    // ── Pro XDR 动态范围增强（成片质感核心，luma-only 无色偏）──
    // 影调设计遵循摄影惯例：真黑留黑（黑场锚点防"灰蒙蒙"）、
    // 白点单调肩部收敛（缓出二次曲线，防"高光发灰"且亮部梯度无色带）、
    // 提亮为三角窗平滑过渡；清晰度仅作用于中间调并与锐化分工。
    // 【v1.5.7 通透度调校】阴影提亮上限 30→18、高光回收 14→10：
    // v1.5.6 用户反馈成片"不通透"——阴影 +30 / 高光 −14 的双向压缩把
    // 黑位抬灰、白场压平，中间调对比被抽空（灰蒙蒙的典型成因）。
    // 收紧为 +18/−10 后：真黑仍沉得下去、白墙/反光保持通透，
    // 动态范围扩展保留但不再以牺牲对比为代价。

    /** 阴影提亮上限（满额区最大 +18 亮度） */
    private const val XDR_SHADOW_LIFT = 18f
    /** 黑场锚点：亮度低于此值的纯黑不提亮，保证画面有"沉下去"的黑 */
    private const val XDR_BLACK_ANCHOR = 6f
    /** 阴影满额起点：此值以上提亮满额，向下平滑收敛至黑场锚点 */
    private const val XDR_SHADOW_FULL = 34f
    /** 阴影过渡上界：提亮量自此平滑衰减至 0 */
    private const val XDR_SHADOW_END = 112f
    /** 高光回收上限（白点最大 −10 亮度：肩部收敛防"高光发灰"，保通透） */
    private const val XDR_HIGHLIGHT_PULL = 10f
    /** 高光过渡起点：亮度高于此值开始沿单调肩部曲线回收 */
    private const val XDR_HIGHLIGHT_START = 208f
    /** 高光区间宽度（208→255）：肩部曲线的归一化分母 */
    private const val XDR_HIGHLIGHT_RANGE = 47f
    /** 局部对比（清晰度）权重：复用高频残差，强度低于锐化（⑤）避免光晕。
     *  【v1.5.7】0.30→0.45：局部对比是"通透/清晰"观感的核心来源，
     *  原权重下成片发糊发平；经中间调掩膜 + edgeW 双门控，不放大噪点 */
    private const val XDR_CLARITY = 0.45f
    /** 清晰度中间调掩膜：暗部 <24 / 亮部 >208 不增强（护暗部噪点与高光通透） */
    private const val XDR_CLARITY_LO = 24f
    private const val XDR_CLARITY_LO_FULL = 64f
    private const val XDR_CLARITY_HI = 160f
    private const val XDR_CLARITY_HI_END = 208f

    /** smoothstep：残差 → 边缘权重（0..1），噪点不锐化、细纹理过渡、强边满额 */
    private fun edgeWeight(residual: Float): Float {
        val t = ((residual - EDGE_LOW) / (EDGE_HIGH - EDGE_LOW)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    // ── Pro XDR 全局影调（拍后与取景预览共用的单一事实来源）──

    /**
     * XDR 全局项对给定亮度的调整量（阴影三角窗提亮 + 高光缓出肩部回收）。
     *
     * 【v1.8.9 审计修复】拍后 [processBandInto] 与取景预览
     * [FilteredPreviewAnalyzer] 共用本函数，保证"所见即所得"：
     * 预览按整像素亮度查表（[buildXdrToneLut]）后以同一语义施加，
     * 与拍后全局项逐位一致。清晰度（局部对比，需邻域运算）仍为拍后专属，
     * 与锐化定位相同。
     *
     * 影调结构（摄影惯例，luma-only 无色偏）：
     * - 黑场锚点 ≤6 不抬（真黑"沉得下去"）；
     * - 6→34 平滑升至满额 +18，34→112 平滑收敛至 0；
     * - ≥208 沿单调缓出肩部回收，白点 −10（镜面高光通透不发灰）。
     */
    fun xdrGlobalLumaAdjust(luma: Float, xdr: Float): Float {
        var adj = 0f
        if (luma > XDR_BLACK_ANCHOR && luma < XDR_SHADOW_END) {
            val wUp = ((luma - XDR_BLACK_ANCHOR) / (XDR_SHADOW_FULL - XDR_BLACK_ANCHOR)).coerceIn(0f, 1f)
            val wDn = ((XDR_SHADOW_END - luma) / (XDR_SHADOW_END - XDR_SHADOW_FULL)).coerceIn(0f, 1f)
            adj += xdr * XDR_SHADOW_LIFT * minOf(wUp, wDn)
        }
        if (luma > XDR_HIGHLIGHT_START) {
            val u = ((luma - XDR_HIGHLIGHT_START) / XDR_HIGHLIGHT_RANGE).coerceIn(0f, 1f)
            adj -= xdr * XDR_HIGHLIGHT_PULL * (1f - (1f - u) * (1f - u))
        }
        return adj
    }

    /** 清晰度（局部对比）中间调掩膜：暗部 <24 / 亮部 >208 不增强（护暗部噪点与高光通透） */
    private fun clarityMask(luma: Float): Float {
        val cLo = ((luma - XDR_CLARITY_LO) / (XDR_CLARITY_LO_FULL - XDR_CLARITY_LO)).coerceIn(0f, 1f)
        val cHi = ((XDR_CLARITY_HI_END - luma) / (XDR_CLARITY_HI_END - XDR_CLARITY_HI)).coerceIn(0f, 1f)
        return minOf(cLo, cHi)
    }

    /**
     * 256 级亮度 → XDR 全局调整量表（取景预览用，纯 JVM 可单测）。
     * 与拍后 [xdrGlobalLumaAdjust] 逐位同源；强度 ≤0 返回全零表（关闭）。
     */
    fun buildXdrToneLut(xdr: Float): FloatArray =
        FloatArray(256) { xdrGlobalLumaAdjust(it.toFloat(), xdr) }

    /**
     * 对 ARGB_8888 像素数组原地施加影调查找表（取景预览帧用）。
     *
     * 表由 [buildCombinedToneLut] 合成（XDR 全局 + Filmic），与拍后
     * [processBandInto] 的全局影调项**逐位一致**（同一查表语义 + 同一
     * (通道+增量).toInt() 截断钳位，拍后把两项合并进单次 [addLumaDelta]
     * 保证单次钳位）——预览所见即拍后所得（清晰度/锐化/降噪为拍后专属，
     * 不在本表内）。强度全零时逐位不动（关闭）。
     */
    fun applyXdrToneLutInto(px: IntArray, lut: FloatArray) {
        for (i in px.indices) {
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = ((r * 54 + g * 183 + b * 19) shr 8).coerceIn(0, 255)
            val d = lut[l]
            if (d > 0.5f || d < -0.5f) {
                val nr = (r + d).toInt().coerceIn(0, 255)
                val ng = (g + d).toInt().coerceIn(0, 255)
                val nb = (b + d).toInt().coerceIn(0, 255)
                px[i] = (c and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
    }

    /**
     * 内存守卫：返回"可安全全分辨率处理的最大边长"，0 = 当前尺寸可全分辨率处理。
     *
     * 全分辨率管线峰值 = 2 张整图（输入+输出，各 4B/px）+ ~24MB 瞬态
     * （JPEG 解码缓冲 + 条带 IntArray×5 + Skia 压缩缓冲）。预算取堆上限 62% − 24MB，
     * 超限时按面积等比收缩，保证任何堆大小的设备都不 OOM。
     *
     * 【v1.5.7 分辨率保证】预算系数 45%→62%（瞬态预估 32MB→24MB，条带数组按
     * 192 行 × 守卫后宽度封顶后实测 ≤24MB）：v1.5.6 用户反馈"清晰度不够，
     * 要保证原设备的相机分辨率"——旧预算下 256MB 堆只允许 10.4MP，
     * 主流 12.2MP（4032×3024）成片被缩到 ~3725px 再保存，成片分辨率
     * 低于相机输出（且低于"无效果直通"路径的原始 JPEG，同一次拍摄
     * 两路径分辨率不一致）。新预算下 256MB 堆允许 16.8MP、512MB 堆
     * 允许 36.7MP，主流手机相机分辨率（≤13MP）不再有任何降采样；
     * 仅 20MP+ 高像素模式或超小堆设备才按面积收缩（OOM 安全阀）。
     *
     * 【v1.8.0 画质】应用已声明 largeHeap（AndroidManifest），主流设备堆上限
     * 提升至 ~512MB，预算 ≈ 293MB → 允许 ≈ 36.7MP，12MP 成片不再降采样。
     *
     * @param maxMemoryBytes Runtime.getRuntime().maxMemory()；<=0 时按 256MB 保守估计
     */
    fun chooseStyleMaxSide(w: Int, h: Int, maxMemoryBytes: Long): Int {
        if (w <= 0 || h <= 0) return 0
        val heap = if (maxMemoryBytes > 0) maxMemoryBytes else 256L * 1024 * 1024
        val pixels = w.toLong() * h.toLong()
        val budget = (heap * 0.62f).toLong() - 24L * 1024 * 1024
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
     * @param xdr Pro XDR 强度 0..1（0 = 关闭；阴影提亮 + 高光回收 + 局部对比）
     * @param radius 邻域半径
     * @param quality OPPO 级画质档强度 0..1（0 = 旧路径逐位不变：纹理保护降噪 v2 /
     *        Filmic 影调 / 多频段锐化全部关闭；>0 启用，见类文档画质档清单）
     * @param scene 场景统计（[analyzeSceneStats] 一次下采样得到；NONE = 场景微色彩关闭）
     * @param chromaBoost Vibrance 基础增强量（0 = 关闭；满灰度预设由调用方按 (1−灰度强度) 折算）
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
        xdr: Float = 0f,
        radius: Int = DEFAULT_RADIUS,
        quality: Float = 0f,
        scene: SceneStats = SceneStats.NONE,
        chromaBoost: Float = 0f
    ) {
        System.arraycopy(band, 0, out, 0, band.size)
        if (w <= 0 || bandH <= 0 || centerH <= 0) return

        val q = quality > 0.01f

        // ① 色彩矩阵（整带）
        if (matrix != null) applyMatrixInto(band, w, bandH, matrix)

        // ② Vibrance + 场景微色彩（整带原地；画质档专属。chromaBoost=0 时整段跳过，
        // 与旧路径逐位一致——所有既有契约测试零改动通过的关键门控）
        if (q && chromaBoost > 0.01f) vibranceInto(band, chromaBoost, scene)

        // ③ 亮度 + 可分离高斯模糊（供边缘权重 / 降噪 / 锐化共用）
        val lum = IntArray(w * bandH)
        for (i in band.indices) {
            val c = band[i]
            lum[i] = (((c shr 16) and 0xFF) * 54 + ((c shr 8) and 0xFF) * 183 + (c and 0xFF) * 19) shr 8
        }
        val blur = IntArray(w * bandH)
        gaussianBlurInto(lum, blur, w, bandH, radius)
        // 画质档：r6 结构频段（box blur σ≈3.65，中频对比/质感来源）
        val blur6 = if (q) {
            IntArray(w * bandH).also { boxBlurInto(lum, it, w, bandH, STRUCTURE_BOX_RADIUS) }
        } else null

        val cx = (w - 1) / 2f
        val cy = (imgH - 1) / 2f
        val maxSide = maxOf(w, imgH).toFloat()
        val vigRadius = maxSide * 0.75f
        val vigDark = (0.62f * vignette).coerceIn(0f, 0.62f)

        val doDenoise = denoise > 0.01f
        val doSharpen = sharpenAmount > 0.02f
        val doVignette = vignette > 0.02f
        val doXdr = xdr > 0.01f
        // 画质档 Filmic 影调表（luma delta；强度 ≤0 时全零——q 已保证 >0.01）
        val filmicLut = if (q) buildFilmicLut(quality) else null
        // 肤色保护系数仅画质档的降噪/锐化需要（Vibrance 内部自带肤色减半）
        val needSkin = q && (doDenoise || doSharpen)

        // ④⑤⑥⑦⑧ 仅中心行输出（上下重叠行只参与邻域运算）
        for (ly in padTop until padTop + centerH) {
            val row = ly * w
            val absY = bandTop + ly
            for (x in 0 until w) {
                val i = row + x
                val edgeW = edgeWeight(Math.abs(lum[i] - blur[i]).toFloat())

                var c = band[i]

                // 肤色保护（OPPO 人像链路核心：肤色区细节阻尼，不塑料化）
                val skinKeep = if (needSkin && isSkinPixel(
                        (c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF
                    )
                ) SKIN_DETAIL_KEEP else 1f

                // ④ 纹理保护降噪
                // 画质档：亮度向中心排除的结构感知模糊目标 + 色度压噪 + 肤色阻尼（v2）；
                // 旧路径：平坦区（edgeW 低）向四邻均值 blend，边缘保留。
                if (doDenoise) {
                    val l = if (x > 0) band[i - 1] else c
                    val r = if (x < w - 1) band[i + 1] else c
                    val u = if (ly > 0) band[i - w] else c
                    val dn = if (ly < bandH - 1) band[i + w] else c
                    if (q) {
                        val (aLuma, aChroma) = qualityDenoiseAlphas(edgeW, denoise, skinKeep)
                        if (aLuma > 0.002f || aChroma > 0.002f) {
                            c = denoisePixelV2(c, l, r, u, dn, blur[i], aLuma, aChroma)
                        }
                    } else if (edgeW < 0.9f) {
                        val flat = 1f - edgeW
                        val a = denoise * 0.85f * flat
                        c = blendPixel(c, l, r, u, dn, a)
                    }
                }

                // ⑤ Pro XDR + ⑥ Filmic 影调（合并为一次亮度增量施加，单次钳位——
                // 与取景预览 [buildCombinedToneLut] 逐位一致；清晰度需邻域运算，拍后专属）
                if (doXdr || filmicLut != null) {
                    val l = lum[i].toFloat()
                    var adj = 0f
                    if (doXdr) {
                        adj += xdrGlobalLumaAdjust(l, xdr) +
                            (lum[i] - blur[i]) * xdr * XDR_CLARITY * edgeW * clarityMask(l)
                    }
                    if (filmicLut != null) adj += filmicLut[lum[i]]
                    if (adj > 0.5f || adj < -0.5f) c = addLumaDelta(c, adj)
                }

                // ⑦ 锐化（luma-only 无色偏）
                // 画质档：多频段（r2 细节 + r6 结构）+ 光晕钳制 ±[SHARP_OVERSHOOT] + 肤色阻尼；
                // 旧路径：单频段 r2 unsharp，smoothstep 门控保平坦噪点。
                if (doSharpen) {
                    if (q) {
                        val res2 = (lum[i] - blur[i]).toFloat()
                        val res6 = (lum[i] - blur6!![i]).toFloat()
                        val w2 = edgeWeight(Math.abs(res2))
                        val w6 = edgeWeight6(Math.abs(res6))
                        var delta = res2 * sharpenAmount * SHARP_R2_SCALE * w2 +
                            res6 * sharpenAmount * SHARP_R6_SCALE * w6
                        delta *= skinKeep
                        delta = delta.coerceIn(-SHARP_OVERSHOOT, SHARP_OVERSHOOT)
                        if (delta > 0.5f || delta < -0.5f) c = addLumaDelta(c, delta)
                    } else {
                        val eff = sharpenAmount * 2.2f * edgeW
                        val delta = (lum[i] - blur[i]).toFloat() * eff
                        if (delta > 0.5f || delta < -0.5f) c = addLumaDelta(c, delta)
                    }
                }

                // ⑧ 径向暗角（0.75*maxSide 半径，0.55→1 渐暗；取景预览同公式）
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
        xdr: Float = 0f,
        radius: Int = DEFAULT_RADIUS,
        quality: Float = 0f,
        scene: SceneStats = SceneStats.NONE,
        chromaBoost: Float = 0f
    ) {
        val w = src.width
        val h = src.height
        // 重叠行数：取景预览与拍后共用 radius；画质档的 r6 结构频段盒模糊半径
        // [STRUCTURE_BOX_RADIUS] 更大——重叠行必须覆盖最大邻域半径，否则多带渲染
        // 时盒模糊在带顶/底触界（clamp 到带内边缘），与整带结果出现接缝
        // （v1.5.8 条带契约测试 `画质档条带无接缝` 锁定）
        val overlap = maxOf(radius, if (quality > 0.01f) STRUCTURE_BOX_RADIUS else 0)
        val top = (y0 - overlap).coerceAtLeast(0)
        val bot = (y1 + overlap).coerceAtMost(h)
        val padTop = y0 - top
        val bandH = bot - top
        if (w <= 0 || bandH <= 0) return
        val band = IntArray(w * bandH)
        src.getPixels(band, 0, w, 0, top, w, bandH)
        val out = IntArray(w * bandH)
        processBandInto(
            band, out, w, h, top, bandH, padTop, y1 - y0,
            matrix, sharpenAmount, denoise, vignette, xdr, radius,
            quality, scene, chromaBoost
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
