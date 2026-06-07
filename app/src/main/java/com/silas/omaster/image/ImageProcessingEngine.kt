package com.silas.omaster.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.renderscript.ScriptIntrinsicConvolve3x3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 真实图像处理引擎
 * 基于RenderScript实现高性能图像处理
 * 针对OPPO Find系列优化
 */
class ImageProcessingEngine private constructor(context: Context) {
    
    private val appContext = context.applicationContext
    private var renderScript: RenderScript? = null
    
    init {
        renderScript = RenderScript.create(appContext)
    }
    
    /**
     * 应用完整调色参数
     * @param bitmap 原始图片
     * @param params 调色参数
     * @return 处理后的图片
     */
    suspend fun applyColorGrading(
        bitmap: Bitmap,
        params: ColorGradingParams
    ): Bitmap = withContext(Dispatchers.Default) {
        var result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // 1. 曝光/亮度调整
        if (params.brightness != 0) {
            result = adjustBrightness(result, params.brightness)
        }
        
        // 2. 对比度调整
        if (params.contrast != 0) {
            result = adjustContrast(result, params.contrast)
        }
        
        // 3. 饱和度调整
        if (params.saturation != 0) {
            result = adjustSaturation(result, params.saturation)
        }
        
        // 4. 色温/冷暖调整
        if (params.warmth != 0) {
            result = adjustWarmth(result, params.warmth)
        }
        
        // 5. 色调调整（青品）
        if (params.tint != 0) {
            result = adjustTint(result, params.tint)
        }
        
        // 6. 高光/阴影
        if (params.highlights != 0 || params.shadows != 0) {
            result = adjustHighlightsShadows(result, params.highlights, params.shadows)
        }
        
        // 7. 锐化
        if (params.sharpness > 0) {
            result = applySharpening(result, params.sharpness)
        }
        
        // 8. 清晰度
        if (params.clarity > 0) {
            result = applyClarity(result, params.clarity)
        }
        
        // 9. 暗角
        if (params.vignette > 0) {
            result = applyVignette(result, params.vignette)
        }
        
        // 10. 降噪
        if (params.noiseReduction > 0) {
            result = applyNoiseReduction(result, params.noiseReduction)
        }
        
        result
    }
    
    /**
     * 调整亮度
     */
    private fun adjustBrightness(bitmap: Bitmap, value: Int): Bitmap {
        val matrix = ColorMatrix()
        val brightness = value / 100f
        matrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness * 255,
                0f, 1f, 0f, 0f, brightness * 255,
                0f, 0f, 1f, 0f, brightness * 255,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyColorMatrix(bitmap, matrix)
    }
    
    /**
     * 调整对比度
     */
    private fun adjustContrast(bitmap: Bitmap, value: Int): Bitmap {
        val matrix = ColorMatrix()
        val contrast = (value + 100) / 100f
        matrix.set(
            floatArrayOf(
                contrast, 0f, 0f, 0f, (1 - contrast) * 128,
                0f, contrast, 0f, 0f, (1 - contrast) * 128,
                0f, 0f, contrast, 0f, (1 - contrast) * 128,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyColorMatrix(bitmap, matrix)
    }
    
    /**
     * 调整饱和度
     */
    private fun adjustSaturation(bitmap: Bitmap, value: Int): Bitmap {
        val matrix = ColorMatrix()
        val saturation = (value + 100) / 100f
        matrix.setSaturation(saturation)
        return applyColorMatrix(bitmap, matrix)
    }
    
    /**
     * 调整色温（暖冷）
     */
    private fun adjustWarmth(bitmap: Bitmap, value: Int): Bitmap {
        val matrix = ColorMatrix()
        val warmth = value / 100f
        
        // 暖调：增加红色，减少蓝色
        // 冷调：减少红色，增加蓝色
        matrix.set(
            floatArrayOf(
                1f + warmth * 0.2f, 0f, 0f, 0f, warmth * 20,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - warmth * 0.2f, 0f, -warmth * 20,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyColorMatrix(bitmap, matrix)
    }
    
    /**
     * 调整色调（青品）
     */
    private fun adjustTint(bitmap: Bitmap, value: Int): Bitmap {
        val matrix = ColorMatrix()
        val tint = value / 100f
        
        // 青色：增加绿色和蓝色
        // 品红：增加红色和蓝色
        matrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, tint * 15,
                0f, 1f, 0f, 0f, -tint * 10,
                0f, 0f, 1f, 0f, tint * 10,
                0f, 0f, 0f, 1f, 0f
            )
        )
        return applyColorMatrix(bitmap, matrix)
    }
    
    /**
     * 调整高光和阴影
     */
    private fun adjustHighlightsShadows(
        bitmap: Bitmap,
        highlights: Int,
        shadows: Int
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        val highlightThreshold = 200
        val shadowThreshold = 50
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val brightness = (r + g + b) / 3
            
            var newR = r
            var newG = g
            var newB = b
            
            when {
                brightness > highlightThreshold && highlights != 0 -> {
                    val factor = highlights / 100f
                    newR = (r + (255 - r) * factor).toInt().coerceIn(0, 255)
                    newG = (g + (255 - g) * factor).toInt().coerceIn(0, 255)
                    newB = (b + (255 - b) * factor).toInt().coerceIn(0, 255)
                }
                brightness < shadowThreshold && shadows != 0 -> {
                    val factor = shadows / 100f
                    newR = (r * (1 + factor)).toInt().coerceIn(0, 255)
                    newG = (g * (1 + factor)).toInt().coerceIn(0, 255)
                    newB = (b * (1 + factor)).toInt().coerceIn(0, 255)
                }
            }
            
            pixels[i] = android.graphics.Color.rgb(newR, newG, newB)
        }
        
        result.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return result
    }
    
    /**
     * 应用锐化
     */
    private fun applySharpening(bitmap: Bitmap, value: Int): Bitmap {
        val rs = renderScript ?: return bitmap
        
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        
        val sharpen = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
        
        val intensity = value / 100f
        val center = 1 + 4 * intensity
        val edge = -intensity
        
        val coefficients = floatArrayOf(
            0f, edge, 0f,
            edge, center, edge,
            0f, edge, 0f
        )
        
        sharpen.setCoefficients(coefficients)
        sharpen.setInput(input)
        sharpen.forEach(output)
        
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        output.copyTo(result)
        
        input.destroy()
        output.destroy()
        sharpen.destroy()
        
        return result
    }
    
    /**
     * 应用清晰度（局部对比度增强）
     */
    private fun applyClarity(bitmap: Bitmap, value: Int): Bitmap {
        // 清晰度 = 轻微锐化 + 对比度增强
        val sharpened = applySharpening(bitmap, value / 2)
        return adjustContrast(sharpened, value / 3)
    }
    
    /**
     * 应用暗角
     */
    private fun applyVignette(bitmap: Bitmap, value: Int): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val maxRadius = maxOf(bitmap.width, bitmap.height) / 2f
        val intensity = value / 100f
        
        val paint = Paint().apply {
            isAntiAlias = true
        }
        
        // 创建径向渐变暗角
        for (i in 0 until 100) {
            val radius = maxRadius * (i / 100f)
            val alpha = (intensity * 255 * (i / 100f).pow(2)).toInt()
            
            paint.color = android.graphics.Color.argb(alpha, 0, 0, 0)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxRadius / 100f + 1
            
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        
        return result
    }
    
    /**
     * 应用降噪
     */
    private fun applyNoiseReduction(bitmap: Bitmap, value: Int): Bitmap {
        val rs = renderScript ?: return bitmap
        
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        
        // 使用高斯模糊进行降噪
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setInput(input)
        
        // 根据降噪强度调整模糊半径
        val radius = (value / 100f * 5f).coerceIn(0f, 25f)
        blur.setRadius(radius)
        blur.forEach(output)
        
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        output.copyTo(result)
        
        input.destroy()
        output.destroy()
        blur.destroy()
        
        return result
    }
    
    /**
     * 应用ColorMatrix
     */
    private fun applyColorMatrix(bitmap: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(matrix)
            isAntiAlias = true
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * 哈苏色彩科学 - 自然色彩解决方案
     */
    suspend fun applyHasselbladNaturalColor(bitmap: Bitmap, style: HasselbladStyle): Bitmap = 
        withContext(Dispatchers.Default) {
            val params = when (style) {
                HasselbladStyle.RICH -> ColorGradingParams(
                    saturation = 12,
                    contrast = 10,
                    brightness = 3,
                    warmth = 5,
                    clarity = 15
                )
                HasselbladStyle.NATURAL -> ColorGradingParams(
                    saturation = 5,
                    contrast = 8,
                    brightness = 0,
                    warmth = 2,
                    clarity = 10
                )
                HasselbladStyle.PORTRAIT -> ColorGradingParams(
                    saturation = 8,
                    contrast = 5,
                    brightness = 5,
                    warmth = 8,
                    clarity = 8,
                    skinSmooth = 20
                )
            }
            applyColorGrading(bitmap, params)
        }
    
    /**
     * 智能优化 - 自动分析并优化图片
     */
    suspend fun smartOptimize(bitmap: Bitmap, mode: SmartOptimizeMode): Bitmap = 
        withContext(Dispatchers.Default) {
            val params = when (mode) {
                SmartOptimizeMode.AUTO -> analyzeAndOptimize(bitmap)
                SmartOptimizeMode.HDR -> ColorGradingParams(
                    contrast = 20,
                    highlights = -30,
                    shadows = 25,
                    clarity = 15
                )
                SmartOptimizeMode.NIGHT -> ColorGradingParams(
                    contrast = 15,
                    highlights = -15,
                    shadows = 20,
                    noiseReduction = 35,
                    brightness = 10
                )
                SmartOptimizeMode.PORTRAIT -> ColorGradingParams(
                    skinSmooth = 25,
                    warmth = 5,
                    saturation = -5,
                    clarity = 10
                )
                SmartOptimizeMode.LANDSCAPE -> ColorGradingParams(
                    saturation = 15,
                    contrast = 12,
                    clarity = 20,
                    sharpness = 15
                )
                SmartOptimizeMode.FOOD -> ColorGradingParams(
                    saturation = 15,
                    warmth = 20,
                    contrast = 8,
                    clarity = 12
                )
            }
            applyColorGrading(bitmap, params)
        }
    
    /**
     * 自动分析并优化
     */
    private fun analyzeAndOptimize(bitmap: Bitmap): ColorGradingParams {
        // 分析图片特征
        var totalBrightness = 0L
        var totalSaturation = 0L
        var sampleCount = 0
        
        val step = 10
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                
                val brightness = (r + g + b) / 3
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val saturation = if (max == 0) 0 else (max - min) * 255 / max
                
                totalBrightness += brightness
                totalSaturation += saturation
                sampleCount++
            }
        }
        
        val avgBrightness = (totalBrightness / sampleCount).toInt()
        val avgSaturation = (totalSaturation / sampleCount).toInt()
        
        // 根据分析结果生成优化参数
        return ColorGradingParams(
            brightness = when {
                avgBrightness < 80 -> 15
                avgBrightness > 200 -> -10
                else -> 5
            },
            contrast = when {
                avgBrightness < 100 -> 10
                else -> 5
            },
            saturation = when {
                avgSaturation < 50 -> 15
                avgSaturation > 180 -> -5
                else -> 8
            },
            clarity = 10,
            sharpness = 10
        )
    }
    
    fun release() {
        renderScript?.destroy()
        renderScript = null
    }
    
    companion object {
        @Volatile
        private var instance: ImageProcessingEngine? = null
        
        fun getInstance(context: Context): ImageProcessingEngine {
            return instance ?: synchronized(this) {
                instance ?: ImageProcessingEngine(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * 调色参数数据类
 */
data class ColorGradingParams(
    val saturation: Int = 0,        // 饱和度 -100~100
    val contrast: Int = 0,          // 对比度 -100~100
    val brightness: Int = 0,        // 亮度 -100~100
    val warmth: Int = 0,            // 冷暖 -100~100
    val tint: Int = 0,              // 色调（青品）-100~100
    val highlights: Int = 0,        // 高光 -100~100
    val shadows: Int = 0,           // 阴影 -100~100
    val sharpness: Int = 0,         // 锐度 0~100
    val clarity: Int = 0,           // 清晰度 0~100
    val noiseReduction: Int = 0,    // 降噪 0~100
    val vignette: Int = 0,          // 暗角 0~100
    val skinSmooth: Int = 0         // 美肤 0~100
)

enum class HasselbladStyle {
    RICH,       // 哈苏浓郁
    NATURAL,    // 哈苏自然
    PORTRAIT    // 哈苏人像
}

enum class SmartOptimizeMode {
    AUTO,       // 智能自动
    HDR,        // HDR增强
    NIGHT,      // 夜景优化
    PORTRAIT,   // 人像优化
    LANDSCAPE,  // 风景优化
    FOOD        // 美食优化
}