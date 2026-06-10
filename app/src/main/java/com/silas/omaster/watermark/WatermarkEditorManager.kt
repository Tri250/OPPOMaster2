package com.silas.omaster.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 水印编辑器管理器
 *
 * WM-001: 基础水印添加 - OPPO哈苏风模板
 * WM-002: 水印元素自由编辑 - 拖拽编辑、元素开关、模板另存
 */
class WatermarkEditorManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)

    // 水印模板库
    val templates = listOf(
        // 品牌水印
        WatermarkTemplate("hasselblad_official", "哈苏认证", WatermarkType.BRAND),
        WatermarkTemplate("oppo_find", "Find系列", WatermarkType.BRAND),
        WatermarkTemplate("oneplus_leica", "一加哈苏", WatermarkType.BRAND),
        WatermarkTemplate("realme_dart", "realme风格", WatermarkType.BRAND),

        // 功能水印
        WatermarkTemplate("classic_frame", "经典边框", WatermarkType.FUNCTIONAL),
        WatermarkTemplate("exif_info", "EXIF信息", WatermarkType.FUNCTIONAL),
        WatermarkTemplate("location_tag", "位置标签", WatermarkType.FUNCTIONAL),
        WatermarkTemplate("timestamp", "时间戳", WatermarkType.FUNCTIONAL),
        WatermarkTemplate("minimal_corner", "极简角标", WatermarkType.FUNCTIONAL),

        // 风格水印
        WatermarkTemplate("art_signature", "艺术签名", WatermarkType.STYLE),
        WatermarkTemplate("cyberpunk", "赛博朋克", WatermarkType.STYLE),
        WatermarkTemplate("film_strip", "胶片条", WatermarkType.STYLE),
        WatermarkTemplate("copyright", "版权声明", WatermarkType.STYLE),
        WatermarkTemplate("social_share", "社交分享", WatermarkType.STYLE)
    )

    // 当前选中的模板
    private val _selectedTemplate = MutableStateFlow<WatermarkTemplate?>(null)
    val selectedTemplate: StateFlow<WatermarkTemplate?> = _selectedTemplate.asStateFlow()

    // 水印配置（可编辑）
    private val _watermarkConfig = MutableStateFlow(WatermarkConfig())
    val watermarkConfig: StateFlow<WatermarkConfig> = _watermarkConfig.asStateFlow()

    // 自定义模板列表
    private val _customTemplates = MutableStateFlow<List<WatermarkTemplate>>(emptyList())
    val customTemplates: StateFlow<List<WatermarkTemplate>> = _customTemplates.asStateFlow()

    // 水印元素配置
    private val _elementConfig = MutableStateFlow(WatermarkElementConfig())
    val elementConfig: StateFlow<WatermarkElementConfig> = _elementConfig.asStateFlow()

    /**
     * WM-001: 选择水印模板
     */
    fun selectTemplate(templateId: String) {
        val template = templates.find { it.id == templateId }
            ?: customTemplates.value.find { it.id == templateId }
        _selectedTemplate.value = template

        // 根据模板设置默认配置
        when (templateId) {
            "hasselblad_official" -> {
                _elementConfig.value = WatermarkElementConfig(
                    showBrand = true,
                    showModel = true,
                    showParams = true,
                    brandName = "HASSELBLAD",
                    brandPosition = WatermarkPosition.TOP_CENTER,
                    modelPosition = WatermarkPosition.CENTER_BOTTOM,
                    paramsPosition = WatermarkPosition.BOTTOM,
                    brandColor = Color.WHITE,
                    brandAlpha = 0.9f,
                    modelColor = Color.WHITE,
                    modelAlpha = 0.8f,
                    paramsColor = Color.WHITE,
                    paramsAlpha = 0.7f
                )
            }
            "oppo_find" -> {
                _elementConfig.value = WatermarkElementConfig(
                    showBrand = true,
                    showModel = true,
                    showParams = true,
                    brandName = "OPPO Find X8 Pro",
                    brandPosition = WatermarkPosition.TOP_CENTER,
                    modelPosition = WatermarkPosition.CENTER_BOTTOM,
                    paramsPosition = WatermarkPosition.BOTTOM,
                    brandColor = Color.WHITE,
                    brandAlpha = 0.95f,
                    modelColor = Color.WHITE,
                    modelAlpha = 0.85f,
                    paramsColor = Color.WHITE,
                    paramsAlpha = 0.75f
                )
            }
            else -> {
                // 默认配置
                _elementConfig.value = WatermarkElementConfig()
            }
        }
    }

    /**
     * WM-002: 更新元素配置
     */
    fun updateElementConfig(config: WatermarkElementConfig) {
        _elementConfig.value = config
    }

    /**
     * WM-002: 切换元素显示
     */
    fun toggleElement(element: WatermarkElement) {
        val current = _elementConfig.value
        _elementConfig.value = when (element) {
            WatermarkElement.BRAND -> current.copy(showBrand = !current.showBrand)
            WatermarkElement.MODEL -> current.copy(showModel = !current.showModel)
            WatermarkElement.PARAMS -> current.copy(showParams = !current.showParams)
            WatermarkElement.TIMESTAMP -> current.copy(showTimestamp = !current.showTimestamp)
            WatermarkElement.LOCATION -> current.copy(showLocation = !current.showLocation)
            WatermarkElement.VIGNETTE -> current.copy(showVignette = !current.showVignette)
        }
    }

    /**
     * WM-002: 更新元素位置
     */
    fun updateElementPosition(element: WatermarkElement, position: WatermarkPosition) {
        val current = _elementConfig.value
        _elementConfig.value = when (element) {
            WatermarkElement.BRAND -> current.copy(brandPosition = position)
            WatermarkElement.MODEL -> current.copy(modelPosition = position)
            WatermarkElement.PARAMS -> current.copy(paramsPosition = position)
            WatermarkElement.TIMESTAMP -> current.copy(timestampPosition = position)
            WatermarkElement.LOCATION -> current.copy(locationPosition = position)
            WatermarkElement.VIGNETTE -> current
        }
    }

    /**
     * WM-002: 更新元素颜色
     */
    fun updateElementColor(element: WatermarkElement, color: Int) {
        val current = _elementConfig.value
        _elementConfig.value = when (element) {
            WatermarkElement.BRAND -> current.copy(brandColor = color)
            WatermarkElement.MODEL -> current.copy(modelColor = color)
            WatermarkElement.PARAMS -> current.copy(paramsColor = color)
            WatermarkElement.TIMESTAMP -> current.copy(timestampColor = color)
            WatermarkElement.LOCATION -> current.copy(locationColor = color)
            WatermarkElement.VIGNETTE -> current
        }
    }

    /**
     * WM-002: 更新元素透明度
     */
    fun updateElementAlpha(element: WatermarkElement, alpha: Float) {
        val current = _elementConfig.value
        _elementConfig.value = when (element) {
            WatermarkElement.BRAND -> current.copy(brandAlpha = alpha)
            WatermarkElement.MODEL -> current.copy(modelAlpha = alpha)
            WatermarkElement.PARAMS -> current.copy(paramsAlpha = alpha)
            WatermarkElement.TIMESTAMP -> current.copy(timestampAlpha = alpha)
            WatermarkElement.LOCATION -> current.copy(locationAlpha = alpha)
            WatermarkElement.VIGNETTE -> current.copy(vignetteAlpha = alpha)
        }
    }

    /**
     * WM-002: 保存为自定义模板
     * 模板名不允许与系统模板同名
     */
    fun saveAsCustomTemplate(name: String): Boolean {
        // 检查是否与系统模板同名
        if (templates.any { it.name == name }) {
            return false
        }

        val customTemplate = WatermarkTemplate(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            type = WatermarkType.CUSTOM,
            elementConfig = _elementConfig.value
        )

        val current = _customTemplates.value.toMutableList()
        current.add(customTemplate)
        _customTemplates.value = current

        return true
    }

    /**
     * WM-001: 应用水印到图片
     * 生成带水印图，结构：顶部HASSELBLAD，中下部手机型号，底部参数栏
     */
    suspend fun applyWatermark(
        bitmap: Bitmap,
        deviceModel: String = "OPPO Find X8 Pro",
        params: String = "f/1.6 1/500s ISO100"
    ): Bitmap = withContext(Dispatchers.Default) {
        val config = _watermarkConfig.value
        val elementConfig = _elementConfig.value

        // 创建可变位图
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // 水印画笔
        val brandPaint = Paint().apply {
            color = elementConfig.brandColor
            alpha = (elementConfig.brandAlpha * 255).toInt()
            textSize = bitmap.width * 0.04f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }

        val modelPaint = Paint().apply {
            color = elementConfig.modelColor
            alpha = (elementConfig.modelAlpha * 255).toInt()
            textSize = bitmap.width * 0.025f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }

        val paramsPaint = Paint().apply {
            color = elementConfig.paramsColor
            alpha = (elementConfig.paramsAlpha * 255).toInt()
            textSize = bitmap.width * 0.02f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            setShadowLayer(1f, 1f, 1f, Color.BLACK)
        }

        // 根据配置绘制水印
        if (elementConfig.showBrand) {
            drawTextAtPosition(canvas, elementConfig.brandName, brandPaint, elementConfig.brandPosition, bitmap)
        }

        if (elementConfig.showModel) {
            drawTextAtPosition(canvas, deviceModel, modelPaint, elementConfig.modelPosition, bitmap)
        }

        if (elementConfig.showParams) {
            drawTextAtPosition(canvas, params, paramsPaint, elementConfig.paramsPosition, bitmap)
        }

        if (elementConfig.showTimestamp) {
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            val timestampPaint = Paint().apply {
                color = elementConfig.timestampColor
                alpha = (elementConfig.timestampAlpha * 255).toInt()
                textSize = bitmap.width * 0.018f
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            drawTextAtPosition(canvas, timestamp, timestampPaint, elementConfig.timestampPosition, bitmap)
        }

        if (elementConfig.showVignette) {
            drawVignette(canvas, bitmap, elementConfig.vignetteAlpha)
        }

        result
    }

    /**
     * 在指定位置绘制文本
     */
    private fun drawTextAtPosition(
        canvas: Canvas,
        text: String,
        paint: Paint,
        position: WatermarkPosition,
        bitmap: Bitmap
    ) {
        val textWidth = paint.measureText(text)
        val textHeight = paint.textSize

        val (x, y) = when (position) {
            WatermarkPosition.TOP_LEFT -> Pair(bitmap.width * 0.05f, textHeight + bitmap.height * 0.03f)
            WatermarkPosition.TOP_CENTER -> Pair((bitmap.width - textWidth) / 2, textHeight + bitmap.height * 0.03f)
            WatermarkPosition.TOP_RIGHT -> Pair(bitmap.width - textWidth - bitmap.width * 0.05f, textHeight + bitmap.height * 0.03f)
            WatermarkPosition.TOP -> Pair((bitmap.width - textWidth) / 2, textHeight + bitmap.height * 0.03f)
            WatermarkPosition.CENTER_LEFT -> Pair(bitmap.width * 0.05f, bitmap.height / 2f)
            WatermarkPosition.CENTER -> Pair((bitmap.width - textWidth) / 2, bitmap.height / 2f)
            WatermarkPosition.CENTER_RIGHT -> Pair(bitmap.width - textWidth - bitmap.width * 0.05f, bitmap.height / 2f)
            WatermarkPosition.CENTER_BOTTOM -> Pair((bitmap.width - textWidth) / 2, bitmap.height * 0.65f)
            WatermarkPosition.BOTTOM_LEFT -> Pair(bitmap.width * 0.05f, bitmap.height - bitmap.height * 0.03f)
            WatermarkPosition.BOTTOM -> Pair((bitmap.width - textWidth) / 2, bitmap.height - bitmap.height * 0.03f)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(bitmap.width - textWidth - bitmap.width * 0.05f, bitmap.height - bitmap.height * 0.03f)
            WatermarkPosition.CUSTOM -> Pair(bitmap.width * 0.05f, bitmap.height - bitmap.height * 0.03f)
        }

        canvas.drawText(text, x, y, paint)
    }

    /**
     * 绘制暗角效果
     */
    private fun drawVignette(canvas: Canvas, bitmap: Bitmap, alpha: Float) {
        val centerX = bitmap.width / 2f
        val centerY = bitmap.height / 2f
        val radius = maxOf(bitmap.width, bitmap.height) * 0.8f

        val gradientPaint = Paint().apply {
            shader = android.graphics.RadialGradient(
                centerX, centerY, radius,
                intArrayOf(Color.TRANSPARENT, Color.argb((alpha * 180).toInt(), 0, 0, 0)),
                floatArrayOf(0.5f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }

        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), gradientPaint)
    }

    /**
     * WM-001: 导出为JPG
     */
    suspend fun exportAsJpg(bitmap: Bitmap, quality: Int = 95): ByteArray = withContext(Dispatchers.Default) {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    }

    /**
     * WM-001: 导出为PNG
     */
    suspend fun exportAsPng(bitmap: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.toByteArray()
    }

    /**
     * 保存水印偏好
     */
    fun savePreference(templateId: String) {
        settingsManager.lastWatermarkTemplate = templateId
    }

    /**
     * 加载上次使用的水印模板
     */
    fun loadLastTemplate() {
        val lastTemplate = settingsManager.lastWatermarkTemplate
        if (lastTemplate != null) {
            selectTemplate(lastTemplate)
        }
    }

    /**
     * 重置配置
     */
    fun resetConfig() {
        _selectedTemplate.value = null
        _watermarkConfig.value = WatermarkConfig()
        _elementConfig.value = WatermarkElementConfig()
    }

    companion object {
        @Volatile
        private var instance: WatermarkEditorManager? = null

        fun getInstance(context: Context): WatermarkEditorManager {
            return instance ?: synchronized(this) {
                instance ?: WatermarkEditorManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * 水印模板
 */
data class WatermarkTemplate(
    val id: String,
    val name: String,
    val type: WatermarkType,
    val elementConfig: WatermarkElementConfig? = null
)

/**
 * 水印类型
 */
enum class WatermarkType {
    BRAND,      // 品牌水印
    FUNCTIONAL, // 功能水印
    STYLE,      // 风格水印
    CUSTOM      // 自定义水印
}

/**
 * 水印元素
 */
enum class WatermarkElement {
    BRAND,
    MODEL,
    PARAMS,
    TIMESTAMP,
    LOCATION,
    VIGNETTE
}

/**
 * 水印配置
 */
data class WatermarkConfig(
    val opacity: Float = 1.0f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f
)

/**
 * 水印元素配置
 */
data class WatermarkElementConfig(
    // 显示控制
    val showBrand: Boolean = true,
    val showModel: Boolean = true,
    val showParams: Boolean = true,
    val showTimestamp: Boolean = false,
    val showLocation: Boolean = false,
    val showVignette: Boolean = false,

    // 品牌配置
    val brandName: String = "HASSELBLAD",
    val brandPosition: WatermarkPosition = WatermarkPosition.TOP_CENTER,
    val brandColor: Int = Color.WHITE,
    val brandAlpha: Float = 0.9f,

    // 型号配置
    val modelPosition: WatermarkPosition = WatermarkPosition.CENTER_BOTTOM,
    val modelColor: Int = Color.WHITE,
    val modelAlpha: Float = 0.8f,

    // 参数配置
    val paramsPosition: WatermarkPosition = WatermarkPosition.BOTTOM,
    val paramsColor: Int = Color.WHITE,
    val paramsAlpha: Float = 0.7f,

    // 时间戳配置
    val timestampPosition: WatermarkPosition = WatermarkPosition.TOP_RIGHT,
    val timestampColor: Int = Color.WHITE,
    val timestampAlpha: Float = 0.8f,

    // 位置配置
    val locationPosition: WatermarkPosition = WatermarkPosition.TOP_LEFT,
    val locationColor: Int = Color.WHITE,
    val locationAlpha: Float = 0.8f,

    // 暗角配置
    val vignetteAlpha: Float = 0.5f
)
