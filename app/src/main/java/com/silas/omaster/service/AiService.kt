package com.silas.omaster.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import com.silas.omaster.model.AiAdjustmentParams
import com.silas.omaster.model.CameraParams
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.SceneType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * AI服务 - 真实图像分析和智能优化
 *
 * 实现原理：
 * 1. AI场景识别 - 基于真实图像像素分析（亮度、色彩分布、对比度等）
 * 2. AI智能优化 - 基于图像特征自动计算最优参数
 * 3. 样式迁移 - 基于风格特征计算调整参数
 */
class AiService(private val context: Context? = null) {

    /**
     * AI场景识别 - 真实图像分析
     * 响应时间 ≤300ms（标准），≤500ms（夜景），≤200ms（运动）
     */
    suspend fun detectScene(imageUri: String? = null): SceneType = withContext(Dispatchers.IO) {
        if (imageUri == null || context == null) {
            return@withContext SceneType.UNKNOWN
        }

        try {
            // 加载并缩放图像用于分析
            val bitmap = loadAndScaleBitmap(imageUri, maxSize = 256)
                ?: return@withContext SceneType.UNKNOWN

            // 提取图像特征
            val features = analyzeImageFeatures(bitmap)
            bitmap.recycle()

            // 基于特征判断场景
            classifyScene(features)
        } catch (e: Exception) {
            SceneType.UNKNOWN
        }
    }

    /**
     * 加载并缩放Bitmap用于分析
     */
    private fun loadAndScaleBitmap(uriString: String, maxSize: Int): Bitmap? {
        val uri = Uri.parse(uriString)
        return try {
            val inputStream: InputStream? = when (uri.scheme) {
                "android.resource", "file", "content" -> context?.contentResolver?.openInputStream(uri)
                else -> context?.contentResolver?.openInputStream(uri)
            }

            inputStream?.use { stream ->
                // 第一次采样 - 获取图像尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)

                // 计算采样率
                var sampleSize = 1
                val (width, height) = options.outWidth to options.outHeight
                while (max(width, height) / sampleSize > maxSize) {
                    sampleSize *= 2
                }

                // 第二次采样 - 加载缩略图
                val loadOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                context?.contentResolver?.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, loadOptions)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 图像特征数据结构
     */
    private data class ImageFeatures(
        val avgBrightness: Float,      // 平均亮度 0-255
        val brightnessStdDev: Float,   // 亮度标准差
        val avgSaturation: Float,      // 平均饱和度 0-1
        val warmColorRatio: Float,     // 暖色调比例
        val coolColorRatio: Float,     // 冷色调比例
        val greenRatio: Float,         // 绿色比例
        val blueRatio: Float,          // 蓝色比例
        val redRatio: Float,           // 红色比例
        val contrast: Float,           // 对比度
        val edgeDensity: Float,        // 边缘密度
        val darkRatio: Float,          // 暗像素比例
        val brightRatio: Float         // 亮像素比例
    )

    /**
     * 分析图像特征
     */
    private fun analyzeImageFeatures(bitmap: Bitmap): ImageFeatures {
        val width = bitmap.width
        val height = bitmap.height
        val sampleStep = max(1, (width * height) / 5000) // 采样约5000个像素

        var totalBrightness = 0L
        var totalSaturation = 0f
        var warmCount = 0
        var coolCount = 0
        var greenCount = 0
        var blueCount = 0
        var redCount = 0
        var darkCount = 0
        var brightCount = 0
        var pixelCount = 0
        val brightnessList = mutableListOf<Int>()

        // 边缘检测相关
        var edgeCount = 0

        var x = 0
        while (x < width) {
            var y = 0
            while (y < height) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // 计算亮度
                val brightness = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                totalBrightness += brightness
                brightnessList.add(brightness)

                // 计算饱和度
                val maxRgb = kotlin.math.max(r, kotlin.math.max(g, b))
                val minRgb = kotlin.math.min(r, kotlin.math.min(g, b))
                val saturation = if (maxRgb == 0) 0f else (maxRgb - minRgb).toFloat() / maxRgb
                totalSaturation += saturation

                // 颜色判断
                if (r > b + 20 && r > g + 20) {
                    warmCount++
                    redCount++
                } else if (b > r + 20 && b > g + 20) {
                    coolCount++
                    blueCount++
                } else if (g > r + 10 && g > b + 10) {
                    greenCount++
                }

                // 亮暗判断
                if (brightness < 50) darkCount++
                if (brightness > 200) brightCount++

                pixelCount++
                y += sampleStep
            }
            x += sampleStep
        }

        val avgBrightness = if (pixelCount > 0) totalBrightness.toFloat() / pixelCount else 0f
        val avgSaturation = if (pixelCount > 0) totalSaturation / pixelCount else 0f

        // 计算亮度标准差
        val variance = if (brightnessList.isNotEmpty()) {
            brightnessList.map { (it - avgBrightness).let { d -> d * d } }.average().toFloat()
        } else 0f
        val brightnessStdDev = kotlin.math.sqrt(variance)

        // 对比度 = 亮度标准差归一化
        val contrast = brightnessStdDev / 128f

        // 边缘密度估算（基于亮度变化）
        val edgeDensity = if (brightnessList.size > 1) {
            var changes = 0
            for (i in 1 until brightnessList.size) {
                if (kotlin.math.abs(brightnessList[i] - brightnessList[i - 1]) > 30) changes++
            }
            changes.toFloat() / brightnessList.size
        } else 0f

        return ImageFeatures(
            avgBrightness = avgBrightness,
            brightnessStdDev = brightnessStdDev,
            avgSaturation = avgSaturation,
            warmColorRatio = warmCount.toFloat() / max(pixelCount, 1),
            coolColorRatio = coolCount.toFloat() / max(pixelCount, 1),
            greenRatio = greenCount.toFloat() / max(pixelCount, 1),
            blueRatio = blueCount.toFloat() / max(pixelCount, 1),
            redRatio = redCount.toFloat() / max(pixelCount, 1),
            contrast = contrast,
            edgeDensity = edgeDensity,
            darkRatio = darkCount.toFloat() / max(pixelCount, 1),
            brightRatio = brightCount.toFloat() / max(pixelCount, 1)
        )
    }

    /**
     * 基于特征分类场景
     */
    private fun classifyScene(features: ImageFeatures): SceneType {
        val f = features

        // 异常场景检测
        when {
            f.avgBrightness < 40 -> return SceneType.TOO_DARK
            f.avgBrightness > 230 -> return SceneType.TOO_BRIGHT
            f.brightnessStdDev < 15 -> return SceneType.TOO_BLURRY
        }

        // 夜景场景
        if (f.avgBrightness < 80 && f.contrast > 0.3) {
            return SceneType.NIGHT
        }

        // 星空场景
        if (f.avgBrightness < 60 && f.brightnessStdDev > 50) {
            return SceneType.STARRY_NIGHT
        }

        // 室内暖光
        if (f.warmColorRatio > 0.4 && f.avgBrightness in 80.0..150.0) {
            return SceneType.INDOOR_WARM
        }

        // 花卉/植物场景
        if (f.greenRatio > 0.3 && f.avgSaturation > 0.25) {
            return SceneType.FLOWER
        }

        // 风景场景
        if (f.greenRatio > 0.15 && f.blueRatio > 0.1 && f.avgSaturation > 0.2) {
            return SceneType.LANDSCAPE
        }

        // 城市建筑
        if (f.edgeDensity > 0.4 && f.contrast > 0.4) {
            return SceneType.CITYSCAPE
        }

        // 美食（暖色调+高饱和度）
        if (f.warmColorRatio > 0.3 && f.avgSaturation > 0.3 && f.avgBrightness in 100.0..180.0) {
            return SceneType.FOOD
        }

        // 日落/晚霞
        if (f.warmColorRatio > 0.5 && f.redRatio > 0.2 && f.avgBrightness in 80.0..180.0) {
            return SceneType.SUNSET
        }

        // 人像（基于肤色和柔和度）
        if (f.contrast < 0.3 && f.avgSaturation in 0.1..0.3 && f.edgeDensity < 0.3) {
            return SceneType.PORTRAIT
        }

        // 微距
        if (f.edgeDensity > 0.5 && f.contrast > 0.4) {
            return SceneType.MACRO
        }

        // 雨雾
        if (f.contrast < 0.25 && f.avgSaturation < 0.15) {
            return SceneType.RAINY_FOGGY
        }

        // 静物
        if (f.avgBrightness in 100.0..180.0 && f.contrast < 0.35) {
            return SceneType.STILL_LIFE
        }

        return SceneType.UNKNOWN
    }

    /**
     * 获取推荐预设 - 基于场景
     */
    suspend fun getRecommendedPresets(scene: SceneType, allPresets: List<MasterPreset>): List<MasterPreset> {
        val keywords = scene.getRecommendedPresetKeywords()

        // 根据关键词匹配
        val matchedPresets = allPresets.filter { preset ->
            keywords.any { keyword ->
                preset.name.contains(keyword) ||
                preset.tags?.any { it.contains(keyword, ignoreCase = true) } == true
            }
        }

        // 如果没有匹配到，返回前3个
        return if (matchedPresets.isNotEmpty()) matchedPresets.take(3) else allPresets.take(3)
    }

    /**
     * 获取场景对应的相机参数
     */
    fun getCameraParamsForScene(scene: SceneType): CameraParams {
        return when (scene) {
            SceneType.PORTRAIT, SceneType.MIXED_LANDSCAPE -> CameraParams(
                mode = "哈苏人像模式",
                iso = 100,
                shutter = "1/125",
                ev = "+0.3",
                wb = "5200K",
                focalLength = "85mm",
                aperture = "f/1.8",
                portraitMode = true,
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladMasterStyle = "Portrait Pro",
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "自然",
                sharpness = 45,
                contrast = 50,
                saturation = 55,
                sensorSize = "1英寸双大底"
            )

            SceneType.NIGHT, SceneType.STARRY_NIGHT, SceneType.NIGHT_PORTRAIT -> CameraParams(
                mode = "哈苏夜景模式",
                iso = 3200,
                shutter = "1/30",
                ev = "+0.7",
                wb = "4000K",
                focalLength = "24mm",
                aperture = "f/1.8",
                nightMode = true,
                aiOptimization = true,
                opticalStabilization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladMasterStyle = "Night Pro",
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "电影感",
                sharpness = 50,
                contrast = 55,
                saturation = 50,
                noiseReduction = 60,
                sensorSize = "1英寸双大底"
            )

            SceneType.LANDSCAPE, SceneType.CITYSCAPE, SceneType.RAINY_FOGGY -> CameraParams(
                mode = "哈苏风景模式",
                iso = 64,
                shutter = "1/250",
                ev = "+0.7",
                wb = "6500K",
                focalLength = "23mm",
                aperture = "f/8.0",
                hdr = true,
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladMasterStyle = "Landscape",
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "鲜明",
                sharpness = 60,
                contrast = 55,
                saturation = 58,
                sensorSize = "1英寸双大底"
            )

            SceneType.FOOD, SceneType.MIXED_FOOD -> CameraParams(
                mode = "哈苏美食模式",
                iso = 200,
                shutter = "1/125",
                ev = "+0.3",
                wb = "5000K",
                focalLength = "50mm",
                aperture = "f/2.8",
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "美食",
                sharpness = 50,
                contrast = 50,
                saturation = 65,
                sensorSize = "1英寸双大底"
            )

            SceneType.MACRO, SceneType.FLOWER, SceneType.INSECT, SceneType.OBJECT_DETAIL -> CameraParams(
                mode = "哈苏微距模式",
                iso = 100,
                shutter = "1/160",
                ev = "+0.0",
                wb = "5200K",
                focalLength = "微距",
                aperture = "f/4.0",
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "鲜明",
                sharpness = 65,
                contrast = 55,
                saturation = 60,
                detailEnhancement = 70,
                sensorSize = "1英寸双大底"
            )

            SceneType.MOTION -> CameraParams(
                mode = "哈苏运动模式",
                iso = 400,
                shutter = "1/2000",
                ev = "+0.0",
                wb = "5500K",
                focalLength = "200mm",
                aperture = "f/4.0",
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "专业",
                sharpness = 55,
                contrast = 50,
                saturation = 50,
                sensorSize = "1英寸双大底"
            )

            SceneType.SUNSET, SceneType.FLOWERS_SUNSET -> CameraParams(
                mode = "哈苏日落模式",
                iso = 64,
                shutter = "1/500",
                ev = "+0.7",
                wb = "6000K",
                focalLength = "24mm",
                aperture = "f/5.6",
                hdr = true,
                aiOptimization = true,
                hasselblad_hncs = true,
                hasselbladNaturalColor = true,
                hasselbladColorScience = "HNCS 3.0",
                colorProfile = "暖调",
                sharpness = 55,
                contrast = 58,
                saturation = 65,
                colorTemperature = 6000,
                sensorSize = "1英寸双大底"
            )

            SceneType.TOO_DARK, SceneType.TOO_BRIGHT, SceneType.TOO_BLURRY,
            SceneType.STILL_LIFE, SceneType.INDOOR_WARM -> CameraParams.defaultHasselbladMaster()

            else -> CameraParams.defaultHasselbladMaster()
        }
    }

    /**
     * AI图片微调 - 基于真实图像特征计算最优参数
     * 处理时间 ≤3秒
     */
    suspend fun fineTuneImage(imageUri: String, preset: MasterPreset?): AiAdjustmentParams = withContext(Dispatchers.IO) {
        if (context == null) {
            return@withContext AiAdjustmentParams.DEFAULT
        }

        try {
            val bitmap = loadAndScaleBitmap(imageUri, maxSize = 512)
            if (bitmap == null) {
                return@withContext AiAdjustmentParams.DEFAULT
            }

            val features = analyzeImageFeatures(bitmap)
            bitmap.recycle()

            // 基于图像特征智能计算优化参数
            calculateOptimalAdjustments(features, preset)
        } catch (e: Exception) {
            AiAdjustmentParams.DEFAULT
        }
    }

    /**
     * 基于图像特征计算最优调整参数
     */
    private fun calculateOptimalAdjustments(features: ImageFeatures, preset: MasterPreset?): AiAdjustmentParams {
        val f = features

        // 亮度调整：过暗增加亮度，过亮降低亮度
        val brightness = when {
            f.avgBrightness < 80 -> 15f
            f.avgBrightness < 120 -> 8f
            f.avgBrightness > 200 -> -12f
            f.avgBrightness > 170 -> -5f
            else -> 3f
        }

        // 对比度：低对比度增强，高对比度降低
        val contrast = when {
            f.contrast < 0.2 -> 12f
            f.contrast < 0.35 -> 8f
            f.contrast > 0.6 -> -5f
            else -> 5f
        }

        // 饱和度：基于当前饱和度调整到最佳水平
        val saturation = when {
            f.avgSaturation < 0.15 -> 18f
            f.avgSaturation < 0.25 -> 12f
            f.avgSaturation > 0.6 -> -8f
            else -> 8f
        }

        // 暖度：基于主色调调整
        val warmth = when {
            f.warmColorRatio > f.coolColorRatio + 0.2 -> -5f
            f.coolColorRatio > f.warmColorRatio + 0.2 -> 8f
            else -> 3f
        }

        // 高光：处理过曝区域
        val highlights = when {
            f.brightRatio > 0.3 -> -15f
            f.brightRatio > 0.15 -> -8f
            else -> -5f
        }

        // 阴影：提亮暗部细节
        val shadows = when {
            f.darkRatio > 0.4 -> 20f
            f.darkRatio > 0.2 -> 12f
            f.darkRatio > 0.1 -> 8f
            else -> 5f
        }

        // 清晰度：基于边缘密度调整
        val clarity = when {
            f.edgeDensity < 0.2 -> 18f
            f.edgeDensity < 0.4 -> 12f
            f.edgeDensity > 0.7 -> -3f
            else -> 8f
        }

        // 暗角：基于场景调整
        val vignette = when {
            f.avgBrightness < 100 -> 12f
            f.avgBrightness > 180 -> 2f
            else -> 6f
        }

        // 色调：基于主色调微调
        val tint = if (f.greenRatio > 0.3) 2f else 0f

        return AiAdjustmentParams(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth,
            tint = tint,
            highlights = highlights,
            shadows = shadows,
            clarity = clarity,
            vignette = vignette
        )
    }

    /**
     * 批量AI微调
     */
    suspend fun batchFineTuneImages(imageUris: List<String>, preset: MasterPreset?): List<AiAdjustmentParams> {
        val results = mutableListOf<AiAdjustmentParams>()

        for (uri in imageUris) {
            val result = fineTuneImage(uri, preset)
            results.add(result)
        }

        return results
    }

    /**
     * 应用样式迁移 - 基于真实参数映射
     */
    suspend fun applyStyleTransfer(
        imageUri: String,
        styleName: String,
        intensity: Float = 1.0f
    ): AiAdjustmentParams = withContext(Dispatchers.IO) {
        // 加载图像以验证可用性
        val features = if (context != null) {
            loadAndScaleBitmap(imageUri, maxSize = 256)?.let { bitmap ->
                val f = analyzeImageFeatures(bitmap)
                bitmap.recycle()
                f
            }
        } else null

        // 基于样式名称获取基础参数
        val baseParams = when {
            styleName.contains("自然", ignoreCase = true) -> AiAdjustmentParams(
                brightness = 5f, contrast = 5f, saturation = 8f, warmth = 3f,
                tint = 0f, highlights = -5f, shadows = 8f, clarity = 8f, vignette = 3f
            )
            styleName.contains("鲜艳", ignoreCase = true) -> AiAdjustmentParams(
                brightness = 8f, contrast = 12f, saturation = 20f, warmth = 5f,
                tint = 2f, highlights = -8f, shadows = 10f, clarity = 15f, vignette = 5f
            )
            styleName.contains("黑白", ignoreCase = true) || styleName.contains("bw", ignoreCase = true) -> AiAdjustmentParams(
                brightness = 5f, contrast = 18f, saturation = -100f, warmth = 0f,
                tint = 0f, highlights = -10f, shadows = 15f, clarity = 12f, vignette = 8f
            )
            styleName.contains("人像", ignoreCase = true) -> AiAdjustmentParams(
                brightness = 10f, contrast = 6f, saturation = 8f, warmth = 8f,
                tint = 2f, highlights = -8f, shadows = 12f, clarity = 6f, vignette = 8f
            )
            styleName.contains("风景", ignoreCase = true) -> AiAdjustmentParams(
                brightness = 5f, contrast = 12f, saturation = 15f, warmth = 0f,
                tint = -2f, highlights = -12f, shadows = 18f, clarity = 18f, vignette = 5f
            )
            else -> AiAdjustmentParams.DEFAULT
        }

        // 根据图像特征微调
        if (features != null) {
            val adjustments = calculateOptimalAdjustments(features, null)
            blendParams(baseParams, adjustments, intensity)
        } else {
            baseParams
        }
    }

    /**
     * 混合两个参数集
     */
    private fun blendParams(base: AiAdjustmentParams, adjust: AiAdjustmentParams, intensity: Float): AiAdjustmentParams {
        val factor = intensity.coerceIn(0f, 1f)
        return AiAdjustmentParams(
            brightness = blend(base.brightness, adjust.brightness, factor),
            contrast = blend(base.contrast, adjust.contrast, factor),
            saturation = blend(base.saturation, adjust.saturation, factor),
            warmth = blend(base.warmth, adjust.warmth, factor),
            tint = blend(base.tint, adjust.tint, factor),
            highlights = blend(base.highlights, adjust.highlights, factor),
            shadows = blend(base.shadows, adjust.shadows, factor),
            clarity = blend(base.clarity, adjust.clarity, factor),
            vignette = blend(base.vignette, adjust.vignette, factor)
        )
    }

    private fun blend(a: Float, b: Float, factor: Float): Float = a + (b - a) * factor

    companion object {
        @Volatile
        private var INSTANCE: AiService? = null

        fun getInstance(context: Context): AiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * 智能蒙版结果
 */
data class SmartMaskResult(
    val maskType: String,
    val detectedAreas: List<String>,
    val accuracy: Float,
    val edgeSmoothness: Float
)
