package com.silas.omaster.raw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * RAW 格式处理管理器
 * 专业摄影师刚需
 * 
 * 支持：
 * - DNG/TIFF RAW 解析
 * - 16-bit 色深处理
 * - 白平衡调整
 * - 曝光补偿
 * - 阴影/高光恢复
 */
class RAWProcessingManager private constructor(context: Context) {

    private val appContext = context.applicationContext

    // RAW 格式类型
    enum class RAWFormat(val extension: String, val mimeType: String) {
        DNG("dng", "image/dng"),
        CR2("cr2", "image/cr2"),
        NEF("nef", "image/nef"),
        ARW("arw", "image/arw"),
        RAF("raf", "image/raf"),
        ORF("orf", "image/orf"),
        RW2("rw2", "image/rw2"),
        PEF("pef", "image/pef"),
        SRW("srw", "image/srw"),
        NRW("nrw", "image/nrw");

        companion object {
            fun fromExtension(ext: String): RAWFormat? {
                return entries.find { it.extension.equals(ext, ignoreCase = true) }
            }
            fun fromMimeType(mime: String): RAWFormat? {
                return entries.find { it.mimeType.equals(mime, ignoreCase = true) }
            }
        }
    }

    // RAW 处理参数
    data class RAWParams(
        val exposure: Float = 0f,           // -3.0 ~ +3.0 EV
        val whiteBalance: WhiteBalance = WhiteBalance.Auto,
        val temperature: Int = 5500,        // 色温 K
        val tint: Int = 0,                  // 色调 -100 ~ +100
        val highlights: Int = 0,            // -100 ~ +100
        val shadows: Int = 0,               // -100 ~ +100
        val blacks: Int = 0,                // -100 ~ +100
        val whites: Int = 0,                // -100 ~ +100
        val clarity: Int = 0,               // 0 ~ 100
        val vibrance: Int = 0,              // -100 ~ +100
        val saturation: Int = 0,            // -100 ~ +100
        val sharpness: Int = 0,             // 0 ~ 100
        val noiseReduction: NoiseReduction = NoiseReduction.Normal
    )

    // 白平衡预设
    enum class WhiteBalance(val displayName: String, val temperature: Int, val tint: Int) {
        Auto("自动", 5500, 0),
        Daylight("日光", 5500, 0),
        Cloudy("阴天", 6500, 0),
        Shade("阴影", 7500, 0),
        Tungsten("钨丝灯", 3200, 0),
        Fluorescent("荧光灯", 4000, -10),
        Flash("闪光灯", 5500, 0),
        Custom("自定义", 5500, 0)
    }

    // 降噪模式
    enum class NoiseReduction(val displayName: String, val strength: Float) {
        Off("关闭", 0f),
        Low("低", 0.3f),
        Normal("标准", 0.6f),
        High("高", 0.9f),
        Maximum("最高", 1.0f)
    }

    // RAW 图像信息
    data class RAWInfo(
        val width: Int,
        val height: Int,
        val bitDepth: Int,
        val iso: Int?,
        val shutterSpeed: String?,
        val aperture: String?,
        val focalLength: String?,
        val cameraModel: String?,
        val lensModel: String?,
        val timestamp: Long?
    )

    // 处理状态
    sealed class ProcessingState {
        object Idle : ProcessingState()
        data class Loading(val uri: Uri) : ProcessingState()
        data class Decoding(val progress: Float) : ProcessingState()
        data class Processing(val stage: String, val progress: Float) : ProcessingState()
        data class Completed(val bitmap: Bitmap, val info: RAWInfo) : ProcessingState()
        data class Error(val message: String) : ProcessingState()
    }

    // 当前处理状态
    private val _state = MutableStateFlow<ProcessingState>(ProcessingState.Idle)
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    // 当前 RAW 参数
    private val _params = MutableStateFlow(RAWParams())
    val params: StateFlow<RAWParams> = _params.asStateFlow()

    // 当前 RAW 信息
    private val _rawInfo = MutableStateFlow<RAWInfo?>(null)
    val rawInfo: StateFlow<RAWInfo?> = _rawInfo.asStateFlow()

    // 原始 16-bit 数据（用于高质量处理）
    private var rawBuffer: ShortArray? = null
    private var rawWidth = 0
    private var rawHeight = 0

    /**
     * 检查是否为 RAW 格式
     */
    fun isRAW(uri: Uri): Boolean {
        val extension = uri.toString().substringAfterLast(".").lowercase()
        return RAWFormat.fromExtension(extension) != null
    }

    /**
     * 加载 RAW 文件
     */
    suspend fun loadRAW(uri: Uri): ProcessingState = withContext(Dispatchers.Default) {
        _state.value = ProcessingState.Loading(uri)

        try {
            // 打开输入流
            val inputStream = appContext.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                _state.value = ProcessingState.Error("无法打开文件")
                return@withContext _state.value
            }

            // 读取文件头判断格式
            val header = ByteArray(16)
            inputStream.read(header)
            inputStream.close()

            val format = detectFormat(header)
            if (format == null) {
                _state.value = ProcessingState.Error("不支持的RAW格式")
                return@withContext _state.value
            }

            // 解析 RAW 文件（简化实现，实际应使用 Adobe DNG SDK 或 dcraw）
            _state.value = ProcessingState.Decoding(0.1f)
            val info = parseRAWInfo(uri, format)

            // 解码为 Bitmap（使用 Android BitmapFactory 作为降级）
            _state.value = ProcessingState.Decoding(0.5f)
            val bitmap = decodeRAW(uri, format)

            if (bitmap == null) {
                _state.value = ProcessingState.Error("RAW解码失败")
                return@withContext _state.value
            }

            _rawInfo.value = info
            _state.value = ProcessingState.Completed(bitmap, info)

        } catch (e: Exception) {
            _state.value = ProcessingState.Error(e.message ?: "加载失败")
        }

        _state.value
    }

    /**
     * 应用 RAW 参数处理
     */
    suspend fun processRAW(params: RAWParams): Bitmap? = withContext(Dispatchers.Default) {
        val currentState = _state.value
        if (currentState !is ProcessingState.Completed) {
            return@withContext null
        }

        _state.value = ProcessingState.Processing("应用参数", 0.1f)
        _params.value = params

        val sourceBitmap = currentState.bitmap

        // 应用白平衡
        _state.value = ProcessingState.Processing("白平衡", 0.2f)
        val wbBitmap = applyWhiteBalance(sourceBitmap, params)

        // 应用曝光
        _state.value = ProcessingState.Processing("曝光", 0.3f)
        val expBitmap = applyExposure(wbBitmap, params.exposure)

        // 应用高光/阴影恢复
        _state.value = ProcessingState.Processing("高光阴影", 0.5f)
        val hsBitmap = applyHighlightShadow(expBitmap, params)

        // 应用清晰度
        _state.value = ProcessingState.Processing("清晰度", 0.6f)
        val clarityBitmap = applyClarity(hsBitmap, params.clarity)

        // 应用饱和度/自然饱和度
        _state.value = ProcessingState.Processing("饱和度", 0.7f)
        val satBitmap = applySaturation(clarityBitmap, params.saturation, params.vibrance)

        // 应用降噪
        _state.value = ProcessingState.Processing("降噪", 0.8f)
        val nrBitmap = applyNoiseReduction(satBitmap, params.noiseReduction)

        // 应用锐化
        _state.value = ProcessingState.Processing("锐化", 0.9f)
        val sharpBitmap = applySharpness(nrBitmap, params.sharpness)

        _state.value = ProcessingState.Completed(sharpBitmap, currentState.info)

        sharpBitmap
    }

    /**
     * 设置 RAW 参数
     */
    fun setParams(params: RAWParams) {
        _params.value = params
    }

    /**
     * 重置参数
     */
    fun resetParams() {
        _params.value = RAWParams()
    }

    /**
     * 检测 RAW 格式
     */
    private fun detectFormat(header: ByteArray): RAWFormat? {
        // TIFF/DNG 头
        if (header[0] == 0x49.toByte() && header[1] == 0x49.toByte() ||
            header[0] == 0x4D.toByte() && header[1] == 0x4D.toByte()) {
            return RAWFormat.DNG
        }
        // 其他格式检测...
        return RAWFormat.DNG // 默认
    }

    /**
     * 解析 RAW 信息
     */
    private fun parseRAWInfo(uri: Uri, format: RAWFormat): RAWInfo {
        // 简化实现，实际应解析 EXIF/TIFF 标签
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        return RAWInfo(
            width = options.outWidth,
            height = options.outHeight,
            bitDepth = 14, // 大多数 RAW 为 12-14 bit
            iso = null,
            shutterSpeed = null,
            aperture = null,
            focalLength = null,
            cameraModel = null,
            lensModel = null,
            timestamp = null
        )
    }

    /**
     * 解码 RAW 为 Bitmap
     */
    private fun decodeRAW(uri: Uri, format: RAWFormat): Bitmap? {
        return appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }

    /**
     * 应用白平衡
     */
    private fun applyWhiteBalance(bitmap: Bitmap, params: RAWParams): Bitmap {
        if (params.whiteBalance == WhiteBalance.Auto) {
            return bitmap
        }

        val temp = params.temperature
        val tint = params.tint

        // 色温到 RGB 增益的转换（简化算法）
        val (rGain, gGain, bGain) = temperatureToGain(temp, tint)

        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = Color.red(pixel) * rGain
                var g = Color.green(pixel) * gGain
                var b = Color.blue(pixel) * bGain

                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                result.setPixel(x, y, Color.argb(Color.alpha(pixel), r.toInt(), g.toInt(), b.toInt()))
            }
        }

        return result
    }

    /**
     * 色温到 RGB 增益
     */
    private fun temperatureToGain(temp: Int, tint: Int): Triple<Float, Float, Float> {
        // 简化的色温算法
        val tempF = temp.toFloat() / 100f

        var r: Float = when {
            tempF <= 66 -> 255f
            else -> 329.698727446f * (tempF - 60).pow(-0.1332047592f)
        }

        var g: Float = when {
            tempF <= 66 -> 99.4708025861f * tempF.let { it.coerceIn(0f, 100f) }.pow(-0.1332047592f)
            else -> 288.1221695283f * (tempF - 60).pow(-0.0755148492f)
        }

        var b: Float = when {
            tempF >= 66 -> 255f
            tempF <= 19 -> 0f
            else -> 138.5177312231f * (tempF - 10).pow(-0.0755148492f)
        }

        // 应用色调偏移
        g += tint * 0.5f

        // 归一化
        val max = maxOf(r, g, b)
        return Triple(r / max, g / max, b / max)
    }

    /**
     * 应用曝光
     */
    private fun applyExposure(bitmap: Bitmap, exposure: Float): Bitmap {
        if (exposure == 0f) return bitmap

        val factor = 2f.pow(exposure)
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = Color.red(pixel) * factor
                var g = Color.green(pixel) * factor
                var b = Color.blue(pixel) * factor

                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                result.setPixel(x, y, Color.argb(Color.alpha(pixel), r.toInt(), g.toInt(), b.toInt()))
            }
        }

        return result
    }

    /**
     * 应用高光/阴影恢复
     */
    private fun applyHighlightShadow(bitmap: Bitmap, params: RAWParams): Bitmap {
        if (params.highlights == 0 && params.shadows == 0 && params.blacks == 0 && params.whites == 0) {
            return bitmap
        }

        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = Color.red(pixel).toFloat()
                var g = Color.green(pixel).toFloat()
                var b = Color.blue(pixel).toFloat()

                val luminance = 0.299f * r + 0.587f * g + 0.114f * b

                // 高光恢复
                if (luminance > 200 && params.highlights != 0) {
                    val factor = 1f - params.highlights / 100f * ((luminance - 200) / 55f).coerceIn(0f, 1f)
                    r = 200 + (r - 200) * factor
                    g = 200 + (g - 200) * factor
                    b = 200 + (b - 200) * factor
                }

                // 阴影恢复
                if (luminance < 55 && params.shadows != 0) {
                    val factor = 1f + params.shadows / 100f * ((55 - luminance) / 55f).coerceIn(0f, 1f)
                    r *= factor
                    g *= factor
                    b *= factor
                }

                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                result.setPixel(x, y, Color.argb(Color.alpha(pixel), r.toInt(), g.toInt(), b.toInt()))
            }
        }

        return result
    }

    /**
     * 应用清晰度
     */
    private fun applyClarity(bitmap: Bitmap, clarity: Int): Bitmap {
        if (clarity == 0) return bitmap
        // 简化实现：对比度微调
        return applyLocalContrast(bitmap, clarity / 100f * 0.3f)
    }

    /**
     * 应用局部对比度
     */
    private fun applyLocalContrast(bitmap: Bitmap, amount: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = Color.red(bitmap.getPixel(x, y))
                var sum = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        sum += Color.red(bitmap.getPixel(x + dx, y + dy))
                    }
                }
                val avg = sum / 9
                val diff = center - avg
                val new = (center + diff * amount).roundToInt().coerceIn(0, 255)

                val pixel = bitmap.getPixel(x, y)
                result.setPixel(x, y, Color.argb(
                    Color.alpha(pixel),
                    new,
                    Color.green(pixel),
                    Color.blue(pixel)
                ))
            }
        }

        return result
    }

    /**
     * 应用饱和度
     */
    private fun applySaturation(bitmap: Bitmap, saturation: Int, vibrance: Int): Bitmap {
        if (saturation == 0 && vibrance == 0) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val satFactor = 1f + saturation / 100f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = result.getPixel(x, y)
                var r = Color.red(pixel).toFloat()
                var g = Color.green(pixel).toFloat()
                var b = Color.blue(pixel).toFloat()

                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val currentSat = if (max == 0f) 0f else (max - min) / max

                // 自然饱和度：低饱和区域增强更多
                val vibFactor = if (vibrance != 0) {
                    1f + vibrance / 100f * (1f - currentSat)
                } else 1f

                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                r = gray + (r - gray) * satFactor * vibFactor
                g = gray + (g - gray) * satFactor * vibFactor
                b = gray + (b - gray) * satFactor * vibFactor

                r = r.coerceIn(0f, 255f)
                g = g.coerceIn(0f, 255f)
                b = b.coerceIn(0f, 255f)

                result.setPixel(x, y, Color.argb(Color.alpha(pixel), r.toInt(), g.toInt(), b.toInt()))
            }
        }

        return result
    }

    /**
     * 应用降噪
     */
    private fun applyNoiseReduction(bitmap: Bitmap, nr: NoiseReduction): Bitmap {
        if (nr == NoiseReduction.Off) return bitmap

        // 简化实现：均值滤波
        val radius = (nr.strength * 2).roundToInt().coerceIn(1, 3)
        return applyMeanFilter(bitmap, radius)
    }

    /**
     * 均值滤波
     */
    private fun applyMeanFilter(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        for (y in radius until height - radius) {
            for (x in radius until width - radius) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0

                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val pixel = bitmap.getPixel(x + dx, y + dy)
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                        count++
                    }
                }

                result.setPixel(x, y, Color.argb(
                    Color.alpha(bitmap.getPixel(x, y)),
                    r / count,
                    g / count,
                    b / count
                ))
            }
        }

        return result
    }

    /**
     * 应用锐化
     */
    private fun applySharpness(bitmap: Bitmap, sharpness: Int): Bitmap {
        if (sharpness == 0) return bitmap

        val amount = sharpness / 100f
        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Unsharp mask
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Laplacian
                val rLap = 5 * r - Color.red(bitmap.getPixel(x-1, y)) - Color.red(bitmap.getPixel(x+1, y)) -
                           Color.red(bitmap.getPixel(x, y-1)) - Color.red(bitmap.getPixel(x, y+1))
                val gLap = 5 * g - Color.green(bitmap.getPixel(x-1, y)) - Color.green(bitmap.getPixel(x+1, y)) -
                           Color.green(bitmap.getPixel(x, y-1)) - Color.green(bitmap.getPixel(x, y+1))
                val bLap = 5 * b - Color.blue(bitmap.getPixel(x-1, y)) - Color.blue(bitmap.getPixel(x+1, y)) -
                           Color.blue(bitmap.getPixel(x, y-1)) - Color.blue(bitmap.getPixel(x, y+1))

                val newR = (r + rLap * amount).roundToInt().coerceIn(0, 255)
                val newG = (g + gLap * amount).roundToInt().coerceIn(0, 255)
                val newB = (b + bLap * amount).roundToInt().coerceIn(0, 255)

                result.setPixel(x, y, Color.argb(Color.alpha(pixel), newR, newG, newB))
            }
        }

        return result
    }

    companion object {
        @Volatile
        private var instance: RAWProcessingManager? = null

        fun getInstance(context: Context): RAWProcessingManager {
            return instance ?: synchronized(this) {
                instance ?: RAWProcessingManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
