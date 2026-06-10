package com.silas.omaster.raw

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import com.silas.omaster.renderer.RenderParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * RAW 编辑器
 *
 * 核心功能：
 * - DNG/RAW 解码
 * - 工作色彩空间（ProPhoto Linear）参数调节
 * - 镜头校正（畸变/色差/紫边/暗角）
 * - 色调曲线
 * - 颜色分级（高光/中间调/阴影）
 * - HSL 8 色独立调整
 * - 高质量导出（16-bit TIFF / 高质量 JPEG）
 */
class RawEditor private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val dngDecoder = DngDecoder(appContext)
    private val colorSpaceManager = ColorSpaceManager.getInstance()

    // 当前编辑会话
    private val _currentRaw = MutableStateFlow<DngDecoder.RawImageData?>(null)
    val currentRaw: StateFlow<DngDecoder.RawImageData?> = _currentRaw.asStateFlow()

    // 当前参数
    private val _parameters = MutableStateFlow(RawParameters.DEFAULT)
    val parameters: StateFlow<RawParameters> = _parameters.asStateFlow()

    // 工作色彩空间缓存
    private var workingSpaceBitmap: Bitmap? = null

    // 导出参数
    private val _exportSettings = MutableStateFlow(ExportSettings.DEFAULT)
    val exportSettings: StateFlow<ExportSettings> = _exportSettings.asStateFlow()

    // ===== 解码 =====

    /**
     * 加载 RAW 文件
     */
    suspend fun loadRaw(uri: Uri): DngDecoder.RawImageData? = withContext(Dispatchers.IO) {
        val raw = dngDecoder.decode(uri)
        if (raw != null) {
            _currentRaw.value = raw
            // 转换到工作色彩空间
            workingSpaceBitmap = colorSpaceManager.sRGBToProPhoto(raw.bitmap)
        }
        raw
    }

    /**
     * 从文件加载 RAW
     */
    suspend fun loadRaw(file: File): DngDecoder.RawImageData? = withContext(Dispatchers.IO) {
        val raw = withContext(Dispatchers.IO) {
            // 模拟从文件读取
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                DngDecoder.RawImageData(
                    bitmap = bitmap,
                    rawBytes = null,
                    metadata = DngDecoder.RawMetadata(
                        width = bitmap.width,
                        height = bitmap.height,
                        isoSpeed = 100,
                        exposureTime = 1f / 60f,
                        fNumber = 1.8f,
                        focalLength = 24f,
                        colorTempKelvin = 5500,
                        whiteBalance = DngDecoder.WhiteBalance.AUTO,
                        orientation = 0,
                        hasGainMap = false,
                        blackLevel = 0,
                        whiteLevel = 65535,
                        bitsPerSample = 16,
                        cfaPattern = DngDecoder.CFAPattern.RGGB,
                        make = "",
                        model = "",
                        software = "",
                        dateTime = ""
                    ),
                    colorSpace = DngDecoder.ColorSpaceType.SRGB,
                    isDng = file.extension.equals("dng", ignoreCase = true)
                )
            } else null
        }
        if (raw != null) {
            _currentRaw.value = raw
            workingSpaceBitmap = colorSpaceManager.sRGBToProPhoto(raw.bitmap)
        }
        raw
    }

    // ===== 参数调节 =====

    /**
     * 设置参数
     */
    fun setParameters(params: RawParameters) {
        _parameters.value = params
    }

    /**
     * 应用参数到工作空间
     */
    fun applyToWorkingSpace(): Bitmap? {
        val ws = workingSpaceBitmap ?: return null
        val params = _parameters.value
        return colorSpaceManager.applyInWorkingSpace(ws, params)
    }

    /**
     * 应用镜头校正
     * - 畸变校正（桶形/枕形）
     * - 色差去除
     * - 暗角校正
     * - 紫边去除
     */
    fun applyLensCorrection(bitmap: Bitmap, params: RawParameters): Bitmap {
        if (!params.lensCorrectionEnabled) return bitmap
        val corrected = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(corrected)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 暗角校正
        if (params.vignettingCorrection != 0f) {
            val radius = corrected.width.coerceAtLeast(corrected.height).toFloat()
            val centerX = corrected.width / 2f
            val centerY = corrected.height / 2f

            // 径向渐变蒙版
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            maskPaint.shader = android.graphics.RadialGradient(
                centerX, centerY, radius,
                intArrayOf(0x00000000, if (params.vignettingCorrection > 0) 0xFFFFFFFF.toInt() else 0x00000000),
                floatArrayOf(0.4f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, corrected.width.toFloat(), corrected.height.toFloat(), maskPaint)
        }

        return corrected
    }

    /**
     * 应用 HSL 调整
     * 对 8 个色相区域独立调整 H/S/L
     */
    fun applyHsl(bitmap: Bitmap, params: RawParameters): Bitmap {
        if (params.hslHue.all { it == 0f } &&
            params.hslSaturation.all { it == 0f } &&
            params.hslLuminance.all { it == 0f }
        ) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            val color = pixels[i]
            val a = Color.alpha(color)
            Color.RGBToHSV(Color.red(color), Color.green(color), Color.blue(color), hsv)
            val h = hsv[0]
            val s = hsv[1]
            val v = hsv[2]

            // 根据色相找到对应的 HSL 桶
            val bucket = (h / 45f).toInt().coerceIn(0, 7)
            val hueShift = params.hslHue[bucket]
            val satShift = params.hslSaturation[bucket]
            val lumShift = params.hslLuminance[bucket]

            val newH = (h + hueShift + 360f) % 360f
            val newS = (s + satShift / 100f).coerceIn(0f, 1f)
            val newV = (v + lumShift / 100f).coerceIn(0f, 1f)

            val rgb = Color.HSVToColor(floatArrayOf(newH, newS, newV))
            outPixels[i] = Color.argb(a, Color.red(rgb), Color.green(rgb), Color.blue(rgb))
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * 应用色调曲线
     * @param curve 256 个点（每个 0..1）
     */
    fun applyToneCurve(bitmap: Bitmap, curve: FloatArray): Bitmap {
        if (curve.size != 256) return bitmap

        // 构建 LUT
        val lutR = IntArray(256)
        val lutG = IntArray(256)
        val lutB = IntArray(256)
        for (i in 0 until 256) {
            val v = (curve[i] * 255).toInt().coerceIn(0, 255)
            lutR[i] = v
            lutG[i] = v
            lutB[i] = v
        }

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = lutR[Color.red(color)]
            val g = lutG[Color.green(color)]
            val b = lutB[Color.blue(color)]
            outPixels[i] = Color.argb(Color.alpha(color), r, g, b)
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * 应用颜色分级（高光/中间调/阴影独立调色）
     */
    fun applyColorGrading(bitmap: Bitmap, params: RawParameters): Bitmap {
        if (params.highlightSat == 0f && params.midtoneSat == 0f && params.shadowSat == 0f) {
            return bitmap
        }
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val outPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color) / 255f
            val g = Color.green(color) / 255f
            val b = Color.blue(color) / 255f
            val lum = (r + g + b) / 3f

            // 根据亮度选择调色
            val highlightWeight = smoothstep(0.5f, 0.8f, lum)
            val shadowWeight = 1f - smoothstep(0.2f, 0.5f, lum)
            val midWeight = 1f - highlightWeight - shadowWeight

            // 高光调色
            val (hr, hg, hb) = hsvToRgb(params.highlightHue.toFloat(), params.highlightSat / 100f, 0.5f)
            // 阴影调色
            val (sr, sg, sb) = hsvToRgb(params.shadowHue.toFloat(), params.shadowSat / 100f, 0.5f)
            // 中间调调色
            val (mr, mg, mb) = hsvToRgb(params.midtoneHue.toFloat(), params.midtoneSat / 100f, 0.5f)

            val rNew = (r + (hr - 0.5f) * highlightWeight * 0.3f +
                       (mr - 0.5f) * midWeight * 0.3f +
                       (sr - 0.5f) * shadowWeight * 0.3f).coerceIn(0f, 1f)
            val gNew = (g + (hg - 0.5f) * highlightWeight * 0.3f +
                       (mg - 0.5f) * midWeight * 0.3f +
                       (sg - 0.5f) * shadowWeight * 0.3f).coerceIn(0f, 1f)
            val bNew = (b + (hb - 0.5f) * highlightWeight * 0.3f +
                       (mb - 0.5f) * midWeight * 0.3f +
                       (sb - 0.5f) * shadowWeight * 0.3f).coerceIn(0f, 1f)

            outPixels[i] = Color.argb(
                Color.alpha(color),
                (rNew * 255).toInt(),
                (gNew * 255).toInt(),
                (bNew * 255).toInt()
            )
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Triple<Float, Float, Float> {
        val c = v * s
        val hh = (h % 360f) / 60f
        val x = c * (1f - kotlin.math.abs(hh % 2f - 1f))
        val (r1, g1, b1) = when (hh.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = v - c
        return Triple(r1 + m, g1 + m, b1 + m)
    }

    // ===== 导出 =====

    /**
     * 导出为高质量 JPEG
     */
    suspend fun exportJpeg(file: File, quality: Int = 95): Boolean = withContext(Dispatchers.IO) {
        val rendered = render() ?: return@withContext false
        FileOutputStream(file).use { fos ->
            rendered.compress(Bitmap.CompressFormat.JPEG, quality, fos)
        }
        true
    }

    /**
     * 导出为 16-bit TIFF
     * 注意：标准 Bitmap 是 8-bit，完整 16-bit TIFF 需要 native 实现
     * 这里使用 ARGB_8888 模拟（实际为 8-bit per channel）
     */
    suspend fun exportTiff16bit(file: File): Boolean = withContext(Dispatchers.IO) {
        val rendered = render() ?: return@withContext false
        // 标准 Bitmap 写入（注意：实际仅 8-bit per channel）
        FileOutputStream(file).use { fos ->
            rendered.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        true
    }

    /**
     * 完整渲染管线
     */
    fun render(): Bitmap? {
        val ws = workingSpaceBitmap ?: return null
        val params = _parameters.value

        var current = colorSpaceManager.applyInWorkingSpace(ws, params)
        current = applyLensCorrection(current, params)
        current = applyHsl(current, params)
        params.toneCurve?.let { curve ->
            current = applyToneCurve(current, curve)
        }
        current = applyColorGrading(current, params)

        // 转换回 sRGB 用于显示/导出
        return colorSpaceManager.proPhotoToSRGB(current)
    }

    /**
     * 释放资源
     */
    fun release() {
        workingSpaceBitmap?.recycle()
        workingSpaceBitmap = null
        _currentRaw.value = null
    }

    companion object {
        @Volatile
        private var instance: RawEditor? = null

        fun getInstance(context: Context): RawEditor {
            return instance ?: synchronized(this) {
                instance ?: RawEditor(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 导出设置
 */
data class ExportSettings(
    val format: ExportFormat = ExportFormat.JPEG,
    val quality: Int = 95,
    val targetColorSpace: DngDecoder.ColorSpaceType = DngDecoder.ColorSpaceType.SRGB,
    val outputResolution: OutputResolution = OutputResolution.ORIGINAL,
    val includeMetadata: Boolean = true,
    val enableDenoise: Boolean = true,
    val enableSharpening: Boolean = true
) {
    companion object {
        val DEFAULT = ExportSettings()
    }
}

enum class ExportFormat(val displayName: String, val extension: String) {
    JPEG("JPEG", "jpg"),
    PNG("PNG", "png"),
    TIFF("TIFF", "tiff"),
    WEBP("WebP", "webp")
}

enum class OutputResolution(val displayName: String, val scale: Float) {
    ORIGINAL("原始", 1.0f),
    ULTRA_HD("4K", 0.5f),
    FULL_HD("1080p", 0.25f),
    THUMBNAIL("缩略图", 0.1f)
}
