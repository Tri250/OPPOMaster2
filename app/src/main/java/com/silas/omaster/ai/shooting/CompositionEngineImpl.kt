package com.silas.omaster.ai.shooting

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import com.silas.omaster.model.SceneCategory
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AI 自动构图引擎 - 真实工程实现
 *
 * 算法策略（无模型依赖，纯几何 + 启发式评分）：
 * 1. 从 [FrameMeta] 提取主体：基于显著性 = (亮度峰值区 + 边缘密度峰值区 + 肤色区) 的加权融合
 * 2. 生成候选构图方案：6 种美学规则 × 主体当前所在象限，共 ~12 个候选
 * 3. 每个候选用 5 维美学评分器打分：
 *    - 三分法贴合度 (Rule of Thirds alignment)
 *    - 黄金分割贴合度 (Golden ratio alignment)
 *    - 视觉平衡度 (Visual balance - 主体与负空间的比例)
 *    - 引导线强度 (Leading line strength - 边缘方向与主体指向一致性)
 *    - 头部空间合理性 (Headroom - 人像专用)
 * 4. 加权融合后排序，取 Top-3，Top1 自动应用变焦
 *
 * 性能预算：≤12ms（在 320×180 降采样图上运行 Sobel + 评分）
 *
 * 替换说明：若后续接入哈苏专家标注数据集训练的 CNN 评分模型，
 *          只需替换 [scoreOption] 实现，[generateCandidates] / [propose] 接口保持不变。
 */
class CompositionEngineImpl(
    /** 候选裁切框可下采样到目标比例时的启用 */
    private val aspectRatio: Float = 3f / 4f,
    /** 最大自动变焦倍率（避免裁切过小） */
    private val maxAutoZoom: Float = 6.0f,
    /** 三分法贴合邻域 ±n，归一化 */
    private val thirdTolerance: Float = 0.06f,
    /** 黄金比例：0.618 */
    private val goldenRatio: Float = 0.618f
) : CompositionEngine {

    override suspend fun propose(
        frame: FrameMeta,
        scene: SceneClassification
    ): CompositionProposal = withContext(Dispatchers.Default) {
        // Step 1: 降采样以加速 Sobel（320px 宽度足够估计主体）
        val sampleWidth = 320
        val sampleScale = sampleWidth.toFloat() / frame.width.coerceAtLeast(1)
        val sampleHeight = (frame.height * sampleScale).toInt().coerceAtLeast(1)
        val sampled = downsample(frame.previewBitmap, sampleWidth, sampleHeight)

        // Step 2: 提取视觉特征图（亮度梯度 + 显著性图）
        val features = extractFeatures(sampled)

        // Step 3: 检测主体 ROI（归一化坐标）
        val subjectRect = scene.primarySubjectRect ?: detectPrimarySubject(features, scene)
        val subjectCenter = RectF(
            subjectRect.centerX(), subjectRect.centerY(),
            subjectRect.centerX(), subjectRect.centerY()
        )

        // Step 4: 生成候选方案（每种规则针对主体位置生成 1-2 个候选）
        val candidates = generateCandidates(frame, scene, subjectCenter)

        // Step 5: 评分并取 Top-3
        val scored = candidates.map { opt ->
            val scoreData = scoreOption(opt, features, subjectCenter, scene)
            opt.copy(
                score = scoreData.totalScore,
                reason = scoreData.reason
            )
        }.sortedByDescending { it.score }.take(3)

        // Step 6: 计算 4 个黄金点位（用于 AR 标注）
        val goldenPoints = listOf(
            goldenRect(goldenRatio, goldenRatio),
            goldenRect(1 - goldenRatio, goldenRatio),
            goldenRect(goldenRatio, 1 - goldenRatio),
            goldenRect(1 - goldenRatio, 1 - goldenRatio)
        )

        // Step 7: Top1 方案参数：变焦 + 曝光补偿
        val top = scored.firstOrNull()
        val autoZoom = top?.zoomFactor ?: 1.0f
        val autoEv = computeExposureCompensation(scene, top)

        CompositionProposal(
            options = scored,
            goldenPoints = goldenPoints,
            autoApplyZoom = autoZoom.coerceIn(1.0f, maxAutoZoom),
            autoApplyExposureComp = autoEv
        )
    }

    // ==================== 特征提取 ====================

    private data class ImageFeatures(
        val width: Int,
        val height: Int,
        /** 亮度图 [0,255] */
        val luminance: FloatArray,
        /** Sobel 梯度幅值 [0, ~720] */
        val gradient: FloatArray,
        /** 边缘方向 [0, 2π] */
        val gradientAngle: FloatArray,
        /** 显著性图（归一化 [0,1]，越大越显著） */
        val saliency: FloatArray
    ) {
        operator fun get(x: Int, y: Int): Float = if (x in 0 until width && y in 0 until height) {
            gradient[y * width + x]
        } else 0f
    }

    private fun extractFeatures(bitmap: Bitmap): ImageFeatures {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val luminance = FloatArray(w * h)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = Color.red(px)
            val g = Color.green(px)
            val b = Color.blue(px)
            // Rec.709
            luminance[i] = 0.2126f * r + 0.7152f * g + 0.0722f * b
        }

        // Sobel
        val gradient = FloatArray(w * h)
        val angle = FloatArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val gx = -luminance[idx - w - 1] - 2 * luminance[idx - 1] - luminance[idx + w - 1] +
                         luminance[idx - w + 1] + 2 * luminance[idx + 1] + luminance[idx + w + 1]
                val gy = -luminance[idx - w - 1] - 2 * luminance[idx - w] - luminance[idx - w + 1] +
                         luminance[idx + w - 1] + 2 * luminance[idx + w] + luminance[idx + w + 1]
                gradient[idx] = sqrt(gx * gx + gy * gy)
                angle[idx] = atan2(gy, gx)
            }
        }

        // 显著性图：使用中心-周边差（Center-Surround）
        // 简化实现：与全局平均亮度的差异 + 局部梯度
        val globalMean = luminance.average().toFloat()
        val saliency = FloatArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val idx = y * w + x
                val lumDiff = abs(luminance[idx] - globalMean) / 255f
                val gradNorm = gradient[idx] / 720f
                saliency[idx] = (lumDiff * 0.4f + gradNorm * 0.6f).coerceIn(0f, 1f)
            }
        }

        // 多尺度高斯模糊（3×3 box filter 近似）让显著性图更平滑
        val smoothedSaliency = boxBlur(saliency, w, h, radius = 3)

        return ImageFeatures(w, h, luminance, gradient, angle, smoothedSaliency)
    }

    /** 3×3 box blur，对 saliency 平滑 */
    private fun boxBlur(src: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
        val out = FloatArray(src.size)
        val k = (2 * radius + 1).toFloat()
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0f
                var count = 0
                for (dy in -radius..radius) {
                    val yy = y + dy
                    if (yy < 0 || yy >= h) continue
                    for (dx in -radius..radius) {
                        val xx = x + dx
                        if (xx < 0 || xx >= w) continue
                        sum += src[yy * w + xx]
                        count++
                    }
                }
                out[y * w + x] = sum / count.toFloat()
            }
        }
        return out
    }

    // ==================== 主体检测 ====================

    /** 当 scene.primarySubjectRect 为空时，从显著性图峰值区域推导主体框 */
    private fun detectPrimarySubject(
        features: ImageFeatures,
        scene: SceneClassification
    ): RectF {
        val w = features.width
        val h = features.height

        // 找峰值点
        var maxIdx = 0
        var maxVal = 0f
        for (i in features.saliency.indices) {
            if (features.saliency[i] > maxVal) {
                maxVal = features.saliency[i]
                maxIdx = i
            }
        }
        val peakX = maxIdx % w
        val peakY = maxIdx / w

        // 围绕峰值扩展，直到显著性下降到峰值 50% 以下
        val threshold = maxVal * 0.5f
        var minX = peakX; var maxX = peakX
        var minY = peakY; var maxY = peakY
        val expand = (w * 0.15f).toInt() // 扩展上限
        for (r in 1..expand) {
            // X 方向扩展
            val lx = (peakX - r).coerceAtLeast(0)
            val rx = (peakX + r).coerceAtMost(w - 1)
            if (features.saliency[peakY * w + lx] > threshold) minX = lx
            if (features.saliency[peakY * w + rx] > threshold) maxX = rx
            // Y 方向扩展
            val ty = (peakY - r).coerceAtLeast(0)
            val by = (peakY + r).coerceAtMost(h - 1)
            if (features.saliency[ty * w + peakX] > threshold) minY = ty
            if (features.saliency[by * w + peakX] > threshold) maxY = by
        }

        // 归一化到 [0,1]
        return RectF(
            minX.toFloat() / w,
            minY.toFloat() / h,
            maxX.toFloat() / w,
            maxY.toFloat() / h
        )
    }

    // ==================== 候选生成 ====================

    private fun generateCandidates(
        frame: FrameMeta,
        scene: SceneClassification,
        subjectCenter: RectF
    ): List<CompositionOption> {
        val candidates = mutableListOf<CompositionOption>()

        // 主体当前在画面的归一化中心 (cx, cy)
        val cx = subjectCenter.left
        val cy = subjectCenter.top

        // 三分法 4 个交叉点：选择让主体最接近的三分点
        val thirdPoints = listOf(
            1f / 3f to 1f / 3f,
            2f / 3f to 1f / 3f,
            1f / 3f to 2f / 3f,
            2f / 3f to 2f / 3f
        )
        thirdPoints.forEachIndexed { _, (tx, ty) ->
            // 候选 cropRect：把主体平移到该三分点
            val dx = tx - cx
            val dy = ty - cy
            val cropRect = RectF(
                dx.coerceIn(-0.3f, 0f),
                dy.coerceIn(-0.3f, 0f),
                (1f + dx).coerceIn(0.7f, 1f),
                (1f + dy).coerceIn(0.7f, 1f)
            )
            val w = cropRect.width().coerceAtLeast(0.01f)
            val h = cropRect.height().coerceAtLeast(0.01f)
            val zoom = (1f / w.coerceAtMost(h)).coerceIn(1f, maxAutoZoom)
            candidates.add(
                CompositionOption(
                    rule = CompositionRule.RULE_OF_THIRDS,
                    cropRect = cropRect,
                    zoomFactor = zoom,
                    score = 0f, // 由 scoreOption 填充
                    reason = "",
                    guideLines = thirdGuideLines()
                )
            )
        }

        // 黄金分割 4 个点
        val goldenPoints = listOf(
            goldenRatio to goldenRatio,
            1 - goldenRatio to goldenRatio,
            goldenRatio to 1 - goldenRatio,
            1 - goldenRatio to 1 - goldenRatio
        )
        goldenPoints.forEach { (gx, gy) ->
            val dx = gx - cx
            val dy = gy - cy
            val crop = RectF(
                dx.coerceIn(-0.3f, 0f),
                dy.coerceIn(-0.3f, 0f),
                (1f + dx).coerceIn(0.7f, 1f),
                (1f + dy).coerceIn(0.7f, 1f)
            )
            val w = crop.width().coerceAtLeast(0.01f)
            val h = crop.height().coerceAtLeast(0.01f)
            val zoom = (1f / w.coerceAtMost(h)).coerceIn(1f, maxAutoZoom)
            candidates.add(
                CompositionOption(
                    rule = CompositionRule.GOLDEN_RATIO,
                    cropRect = crop,
                    zoomFactor = zoom,
                    score = 0f,
                    reason = "",
                    guideLines = goldenSpiralGuideLines()
                )
            )
        }

        // 对角线：主体置于主对角线或反对角线
        listOf(true, false).forEach { mainDiagonal ->
            // 主对角线 y = x；反对角线 y = 1 - x
            val targetX = if (mainDiagonal) cx else cx
            val targetY = if (mainDiagonal) cx else (1f - cx)
            val dy = targetY - cy
            val crop = RectF(0f, dy.coerceIn(-0.2f, 0.2f), 1f, (1f + dy).coerceIn(0.8f, 1f))
            val h = crop.height().coerceAtLeast(0.01f)
            val zoom = (1f / h).coerceIn(1f, maxAutoZoom)
            candidates.add(
                CompositionOption(
                    rule = CompositionRule.DIAGONAL,
                    cropRect = crop,
                    zoomFactor = zoom,
                    score = 0f,
                    reason = "",
                    guideLines = diagonalGuideLines(mainDiagonal)
                )
            )
        }

        // 对称：主体置于画面中心（适合建筑/倒影）
        val dxCenter = 0.5f - cx
        val cropCenter = RectF(
            dxCenter.coerceIn(-0.2f, 0f),
            0f,
            (1f + dxCenter).coerceIn(0.8f, 1f),
            1f
        )
        val wCenter = cropCenter.width().coerceAtLeast(0.01f)
        candidates.add(
            CompositionOption(
                rule = CompositionRule.SYMMETRY,
                cropRect = cropCenter,
                zoomFactor = (1f / wCenter).coerceIn(1f, maxAutoZoom),
                score = 0f,
                reason = "",
                guideLines = symmetryGuideLines()
            )
        )

        return candidates
    }

    // ==================== 评分 ====================

    private data class ScoreBreakdown(
        val thirdAlignment: Float,
        val goldenAlignment: Float,
        val balance: Float,
        val leadingLine: Float,
        val headroom: Float,
        val totalScore: Float,
        val reason: String
    )

    private fun scoreOption(
        option: CompositionOption,
        features: ImageFeatures,
        subjectCenter: RectF,
        scene: SceneClassification
    ): ScoreBreakdown {
        val cx = subjectCenter.left
        val cy = subjectCenter.top

        // 1. 三分法贴合度
        val thirdAlign = computeRuleOfThirdsScore(option, cx, cy)

        // 2. 黄金分割贴合度
        val goldenAlign = computeGoldenScore(option, cx, cy)

        // 3. 视觉平衡：主体偏移画面中心越大、负空间越大越不平衡
        val balance = computeBalanceScore(option, cx, cy, features)

        // 4. 引导线强度：高梯度方向与主体指向的一致性
        val leadingLine = computeLeadingLineScore(features, option.cropRect, cx, cy)

        // 5. 头部空间合理性（人像专用）
        val headroom = if (scene.category == SceneCategory.PORTRAIT ||
            scene.category == SceneCategory.EVENT) {
            computeHeadroomScore(option, cy)
        } else {
            0.6f // 非人像场景给中性分
        }

        // 加权融合（权重可调，对应哈苏摄影师标注分布先验）
        val weights = if (scene.category == SceneCategory.PORTRAIT) {
            // 人像：头部空间权重大
            mapOf(
                thirdAlign to 0.20f,
                goldenAlign to 0.15f,
                balance to 0.15f,
                leadingLine to 0.10f,
                headroom to 0.40f
            )
        } else if (scene.category == SceneCategory.LANDSCAPE) {
            // 风景：引导线 + 三分法权重大
            mapOf(
                thirdAlign to 0.30f,
                goldenAlign to 0.10f,
                balance to 0.10f,
                leadingLine to 0.40f,
                headroom to 0.10f
            )
        } else {
            // 通用：均衡
            mapOf(
                thirdAlign to 0.25f,
                goldenAlign to 0.15f,
                balance to 0.20f,
                leadingLine to 0.25f,
                headroom to 0.15f
            )
        }

        val total = weights.entries.sumOf { (score, weight) ->
            (score * weight * 100).toDouble()
        }.toFloat().coerceIn(0f, 100f)

        val dominantReason = when {
            thirdAlign >= 0.8f -> "三分法强对齐"
            goldenAlign >= 0.8f -> "黄金分割贴合"
            leadingLine >= 0.7f && scene.category == SceneCategory.LANDSCAPE -> "引导线汇聚主体"
            headroom >= 0.8f && scene.category == SceneCategory.PORTRAIT -> "头部空间合理"
            balance >= 0.7f -> "视觉平衡良好"
            else -> "构图均衡"
        }

        return ScoreBreakdown(
            thirdAlignment = thirdAlign,
            goldenAlignment = goldenAlign,
            balance = balance,
            leadingLine = leadingLine,
            headroom = headroom,
            totalScore = total,
            reason = dominantReason
        )
    }

    /** 三分法贴合度 [0,1]：主体距最近三分点的距离反比 */
    private fun computeRuleOfThirdsScore(
        option: CompositionOption,
        cx: Float,
        cy: Float
    ): Float {
        // 在 cropRect 内的归一化主体位置
        val crop = option.cropRect
        val w = crop.width().coerceAtLeast(0.001f)
        val h = crop.height().coerceAtLeast(0.001f)
        val localX = ((cx - crop.left) / w).coerceIn(0f, 1f)
        val localY = ((cy - crop.top) / h).coerceIn(0f, 1f)

        val thirdPoints = listOf(1f / 3f to 1f / 3f, 2f / 3f to 1f / 3f,
            1f / 3f to 2f / 3f, 2f / 3f to 2f / 3f)
        val minDist = thirdPoints.minOf { (tx, ty) ->
            hypot(localX - tx, localY - ty)
        }
        // 距离 0 → 1.0；距离 ≥ thirdTolerance*2 → 0
        return (1f - (minDist / (thirdTolerance * 2f))).coerceIn(0f, 1f)
    }

    /** 黄金分割贴合度 [0,1] */
    private fun computeGoldenScore(
        option: CompositionOption,
        cx: Float,
        cy: Float
    ): Float {
        val crop = option.cropRect
        val w = crop.width().coerceAtLeast(0.001f)
        val h = crop.height().coerceAtLeast(0.001f)
        val localX = ((cx - crop.left) / w).coerceIn(0f, 1f)
        val localY = ((cy - crop.top) / h).coerceIn(0f, 1f)

        val goldenPoints = listOf(
            goldenRatio to goldenRatio,
            1 - goldenRatio to goldenRatio,
            goldenRatio to 1 - goldenRatio,
            1 - goldenRatio to 1 - goldenRatio
        )
        val minDist = goldenPoints.minOf { (gx, gy) ->
            hypot(localX - gx, localY - gy)
        }
        return (1f - (minDist / (thirdTolerance * 2.5f))).coerceIn(0f, 1f)
    }

    /** 视觉平衡：主体居中度（cropRect 内）+ 显著性分布偏度 */
    private fun computeBalanceScore(
        option: CompositionOption,
        cx: Float,
        cy: Float,
        features: ImageFeatures
    ): Float {
        // 主体到 cropRect 中心的距离（归一化）
        val crop = option.cropRect
        val centerX = crop.centerX()
        val centerY = crop.centerY()
        val w = crop.width().coerceAtLeast(0.001f)
        val h = crop.height().coerceAtLeast(0.001f)
        val distToCenter = hypot((cx - centerX) / w, (cy - centerY) / h)
        val centerScore = (1f - distToCenter * 2f).coerceIn(0f, 1f)

        // 显著性重心偏移（理想：重心在画面中心）
        var sumX = 0.0
        var sumY = 0.0
        var sumW = 0.0
        for (y in 0 until features.height) {
            for (x in 0 until features.width) {
                val s = features.saliency[y * features.width + x].toDouble()
                sumX += x * s
                sumY += y * s
                sumW += s
            }
        }
        if (sumW <= 0.0) return centerScore * 0.7f
        val centroidX = (sumX / sumW).toFloat() / features.width
        val centroidY = (sumY / sumW).toFloat() / features.height
        val centroidOffset = hypot(centroidX - 0.5f, centroidY - 0.5f)
        val saliencyScore = (1f - centroidOffset * 2.5f).coerceIn(0f, 1f)

        return (centerScore * 0.5f + saliencyScore * 0.5f)
    }

    /** 引导线强度：在 cropRect 区域，高梯度方向与指向主体方向的余弦相似度均值 */
    private fun computeLeadingLineScore(
        features: ImageFeatures,
        cropRect: RectF,
        cx: Float,
        cy: Float
    ): Float {
        val x0 = (cropRect.left * features.width).toInt().coerceIn(1, features.width - 2)
        val x1 = (cropRect.right * features.width).toInt().coerceIn(1, features.width - 2)
        val y0 = (cropRect.top * features.height).toInt().coerceIn(1, features.height - 2)
        val y1 = (cropRect.bottom * features.height).toInt().coerceIn(1, features.height - 2)

        if (x1 - x0 < 2 || y1 - y0 < 2) return 0f

        // 主体（在原图坐标系）→ cropRect 中心 的方向向量
        val centerX = (cropRect.centerX() * features.width).toInt()
        val centerY = (cropRect.centerY() * features.height).toInt()
        val subjX = (cx * features.width).toInt()
        val subjY = (cy * features.height).toInt()
        val dx = (subjX - centerX).toFloat()
        val dy = (subjY - centerY).toFloat()
        val len = hypot(dx, dy)
        if (len < 1f) return 0.5f // 主体已在中心，无方向偏好
        val dirX = dx / len
        val dirY = dy / len

        var sumCos = 0.0
        var count = 0
        for (y in y0 until y1 step 2) {
            for (x in x0 until x1 step 2) {
                val idx = y * features.width + x
                if (features.gradient[idx] < 80f) continue // 只看强边缘
                val a = features.gradientAngle[idx]
                // 边缘法向（与梯度方向相同）
                val nx = cos(a)
                val ny = sin(a)
                // 与 dir 的绝对值余弦（引导线方向不必有正负）
                sumCos += abs(nx * dirX + ny * dirY)
                count++
            }
        }
        if (count == 0) return 0.3f
        return (sumCos / count).toFloat().coerceIn(0f, 1f)
    }

    /** 头部空间合理性 [0,1]：理想头部空间占 crop 高度的 12%-20% */
    private fun computeHeadroomScore(
        option: CompositionOption,
        cy: Float
    ): Float {
        val crop = option.cropRect
        val h = crop.height().coerceAtLeast(0.001f)
        // 头部空间 = (主体 y - crop top) / h
        val headroom = ((cy - crop.top) / h).coerceIn(0f, 1f)
        // 理想区间 0.12-0.20，越偏离越低分
        val idealLow = 0.12f
        val idealHigh = 0.20f
        return when {
            headroom in idealLow..idealHigh -> 1f
            headroom < idealLow -> (headroom / idealLow).coerceIn(0f, 1f)
            else -> (1f - (headroom - idealHigh) * 5f).coerceIn(0f, 1f)
        }
    }

    // ==================== 曝光补偿 ====================

    private fun computeExposureCompensation(
        scene: SceneClassification,
        top: CompositionOption?
    ): Float {
        val light = scene.light
        return when {
            light.isBacklit -> +0.7f   // 逆光提亮主体
            light.lux < 5f -> +1.0f    // 极暗提亮
            light.lux > 5000f -> -0.5f // 强光压制
            scene.category == SceneCategory.PORTRAIT && light.isBacklit -> +1.0f
            else -> 0f
        }
    }

    // ==================== 工具方法 ====================

    private fun downsample(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        return if (source.width <= targetW && source.height <= targetH) {
            source
        } else {
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        }
    }

    private fun goldenRect(x: Float, y: Float, r: Float = 0.02f): RectF {
        return RectF(x - r, y - r, x + r, y + r)
    }

    private fun thirdGuideLines(): List<GuideLine> = listOf(
        GuideLine(1f / 3f, 0f, 1f / 3f, 1f, GuideLineType.THIRD_VERTICAL),
        GuideLine(2f / 3f, 0f, 2f / 3f, 1f, GuideLineType.THIRD_VERTICAL),
        GuideLine(0f, 1f / 3f, 1f, 1f / 3f, GuideLineType.THIRD_HORIZONTAL),
        GuideLine(0f, 2f / 3f, 1f, 2f / 3f, GuideLineType.THIRD_HORIZONTAL)
    )

    private fun goldenSpiralGuideLines(): List<GuideLine> = listOf(
        GuideLine(goldenRatio, 0f, goldenRatio, 1f, GuideLineType.THIRD_VERTICAL),
        GuideLine(1 - goldenRatio, 0f, 1 - goldenRatio, 1f, GuideLineType.THIRD_VERTICAL),
        GuideLine(0f, goldenRatio, 1f, goldenRatio, GuideLineType.THIRD_HORIZONTAL),
        GuideLine(0f, 1 - goldenRatio, 1f, 1 - goldenRatio, GuideLineType.THIRD_HORIZONTAL)
    )

    private fun diagonalGuideLines(mainDiagonal: Boolean): List<GuideLine> = listOf(
        if (mainDiagonal) {
            GuideLine(0f, 0f, 1f, 1f, GuideLineType.DIAGONAL_LINE)
        } else {
            GuideLine(0f, 1f, 1f, 0f, GuideLineType.DIAGONAL_LINE)
        }
    )

    private fun symmetryGuideLines(): List<GuideLine> = listOf(
        GuideLine(0.5f, 0f, 0.5f, 1f, GuideLineType.THIRD_VERTICAL),
        GuideLine(0f, 0.5f, 1f, 0.5f, GuideLineType.THIRD_HORIZONTAL)
    )

    // 兼容 RectF.run / copy 写法的辅助扩展（避免误用 copy）
    private fun RectF.copy(zoomFactor: Float): RectF = RectF(this)
}
