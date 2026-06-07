package com.silas.omaster.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 水印编辑器管理器
 * 支持 12+ 水印模板，品牌、功能、多种风格
 */
class WatermarkEditorManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    
    // 12+ 水印模板
    val templates = listOf(
        WatermarkTemplate(
            id = "hasselblad_official",
            name = "哈苏认证",
            type = WatermarkType.BRAND,
            description = "OPPO哈苏HNCS官方认证水印",
            features = listOf("HNCS认证", "官方授权", "专业风格")
        ),
        WatermarkTemplate(
            id = "oppo_find",
            name = "Find系列",
            type = WatermarkType.BRAND,
            description = "OPPO Find X系列专属水印",
            features = listOf("Find X8", "影像旗舰", "专业标识")
        ),
        WatermarkTemplate(
            id = "classic_frame",
            name = "经典边框",
            type = WatermarkType.FUNCTIONAL,
            description = "复古胶片风格边框水印",
            features = listOf("胶片感", "复古", "边框")
        ),
        WatermarkTemplate(
            id = "exif_info",
            name = "EXIF信息",
            type = WatermarkType.FUNCTIONAL,
            description = "显示拍摄参数信息",
            features = listOf("ISO", "快门", "光圈", "焦距")
        ),
        WatermarkTemplate(
            id = "location_tag",
            name = "位置标签",
            type = WatermarkType.FUNCTIONAL,
            description = "显示拍摄地点信息",
            features = listOf("GPS", "城市", "地标")
        ),
        WatermarkTemplate(
            id = "date_time",
            name = "时间戳",
            type = WatermarkType.FUNCTIONAL,
            description = "显示拍摄日期时间",
            features = listOf("日期", "时间", "纪念")
        ),
        WatermarkTemplate(
            id = "minimalist",
            name = "极简风格",
            type = WatermarkType.STYLE,
            description = "简洁现代风格水印",
            features = listOf("极简", "现代", "干净")
        ),
        WatermarkTemplate(
            id = "artistic",
            name = "艺术签名",
            type = WatermarkType.STYLE,
            description = "手写风格艺术签名",
            features = listOf("手写", "艺术", "个性")
        ),
        WatermarkTemplate(
            id = "cyberpunk",
            name = "赛博朋克",
            type = WatermarkType.STYLE,
            description = "未来科技风格水印",
            features = listOf("科技", "未来", "霓虹")
        ),
        WatermarkTemplate(
            id = "film_strip",
            name = "胶片条",
            type = WatermarkType.STYLE,
            description = "电影胶片风格水印",
            features = listOf("电影", "胶片", "故事")
        ),
        WatermarkTemplate(
            id = "copyright",
            name = "版权声明",
            type = WatermarkType.FUNCTIONAL,
            description = "版权保护水印",
            features = listOf("版权", "保护", "作者")
        ),
        WatermarkTemplate(
            id = "social_share",
            name = "社交分享",
            type = WatermarkType.FUNCTIONAL,
            description = "社交媒体优化水印",
            features = listOf("社交", "分享", "平台")
        )
    )

    /**
     * 应用水印到图片
     * @param bitmap 原图
     * @param templateId 模板ID
     * @param config 水印配置
     * @return 带水印的图片
     */
    suspend fun applyWatermark(
        bitmap: Bitmap,
        templateId: String,
        config: WatermarkConfig = WatermarkConfig()
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!settingsManager.isWatermarkEditorEnabled) {
            return@withContext bitmap
        }

        val template = templates.find { it.id == templateId } ?: templates.first()
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        
        // 绘制原图
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        
        // 根据模板类型绘制水印
        when (template.type) {
            WatermarkType.BRAND -> drawBrandWatermark(canvas, template, config, bitmap.width, bitmap.height)
            WatermarkType.FUNCTIONAL -> drawFunctionalWatermark(canvas, template, config, bitmap.width, bitmap.height)
            WatermarkType.STYLE -> drawStyleWatermark(canvas, template, config, bitmap.width, bitmap.height)
        }
        
        result
    }

    /**
     * 绘制品牌水印
     */
    private fun drawBrandWatermark(
        canvas: Canvas,
        template: WatermarkTemplate,
        config: WatermarkConfig,
        width: Int,
        height: Int
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            alpha = (config.opacity * 255).toInt()
        }
        
        val text = when (template.id) {
            "hasselblad_official" -> "Hasselblad"
            "oppo_find" -> "OPPO Find X8 Ultra"
            else -> template.name
        }
        
        val x = when (config.position) {
            WatermarkPosition.BOTTOM_LEFT -> 40f
            WatermarkPosition.BOTTOM_RIGHT -> width - paint.measureText(text) - 40f
            WatermarkPosition.TOP_LEFT -> 40f
            WatermarkPosition.TOP_RIGHT -> width - paint.measureText(text) - 40f
            WatermarkPosition.CENTER -> (width - paint.measureText(text)) / 2
        }
        
        val y = when (config.position) {
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> height - 60f
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> 80f
            WatermarkPosition.CENTER -> height / 2f
        }
        
        // 绘制背景
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (0.4f * 255).toInt()
        }
        val textWidth = paint.measureText(text)
        canvas.drawRect(x - 20, y - 60, x + textWidth + 20, y + 20, bgPaint)
        
        // 绘制文字
        canvas.drawText(text, x, y, paint)
        
        // 绘制副标题
        paint.textSize = 24f
        paint.typeface = Typeface.DEFAULT
        val subText = "OPPO | Hasselblad"
        canvas.drawText(subText, x, y + 35, paint)
    }

    /**
     * 绘制功能水印
     */
    private fun drawFunctionalWatermark(
        canvas: Canvas,
        template: WatermarkTemplate,
        config: WatermarkConfig,
        width: Int,
        height: Int
    ) {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            isAntiAlias = true
            alpha = (config.opacity * 255).toInt()
        }
        
        val lines = when (template.id) {
            "exif_info" -> listOf(
                "ISO 100 | 1/200s | f/1.6",
                "23mm | OPPO Find X8"
            )
            "location_tag" -> listOf(
                "📍 上海市",
                "外滩"
            )
            "date_time" -> listOf(
                "2025.01.15",
                "14:30"
            )
            else -> listOf(template.name, template.description)
        }
        
        val x = 40f
        var y = height - 100f
        
        // 绘制背景
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (0.3f * 255).toInt()
        }
        val maxWidth = lines.maxOf { paint.measureText(it) }
        canvas.drawRect(x - 15, y - 40, x + maxWidth + 15, y + lines.size * 40 + 10, bgPaint)
        
        lines.forEach { line ->
            canvas.drawText(line, x, y, paint)
            y += 40
        }
    }

    /**
     * 绘制风格水印
     */
    private fun drawStyleWatermark(
        canvas: Canvas,
        template: WatermarkTemplate,
        config: WatermarkConfig,
        width: Int,
        height: Int
    ) {
        val paint = Paint().apply {
            when (template.id) {
                "cyberpunk" -> color = Color.CYAN
                "artistic" -> color = Color.parseColor("#D4AF37")
                else -> color = Color.WHITE
            }
            textSize = 42f
            isAntiAlias = true
            alpha = (config.opacity * 255).toInt()
        }
        
        val text = when (template.id) {
            "minimalist" -> "SHOT ON OPPO"
            "artistic" -> "Captured"
            "cyberpunk" -> "CYBER_2077"
            "film_strip" -> "CINEMA"
            else -> template.name
        }
        
        val x = (width - paint.measureText(text)) / 2
        val y = height - 80f
        
        canvas.drawText(text, x, y, paint)
    }

    /**
     * 切换水印编辑器开关
     */
    fun toggleWatermarkEditor(enabled: Boolean) {
        settingsManager.isWatermarkEditorEnabled = enabled
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
    val description: String,
    val features: List<String>
)

/**
 * 水印类型
 */
enum class WatermarkType {
    BRAND,      // 品牌水印
    FUNCTIONAL, // 功能水印
    STYLE       // 风格水印
}

/**
 * 水印配置
 */
data class WatermarkConfig(
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.8f,
    val scale: Float = 1.0f,
    val rotation: Float = 0f
)

/**
 * 水印位置
 */
enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}
