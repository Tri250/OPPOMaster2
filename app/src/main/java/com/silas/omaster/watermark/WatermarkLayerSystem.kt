package com.silas.omaster.watermark

import android.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 水印图层系统 - 从「固定元素」到「自由图层」
 *
 * 核心优势：
 * - 用户可自由增删图层（不再局限于 6 个固定元素）
 * - 每个图层独立控制位置、样式、内容来源
 * - 模板 = 预设图层组合 + 默认样式，用户可在此基础上自由修改
 * - 支持图层排序（上下层关系）
 */

/**
 * 图层定义
 */
@Serializable
data class WatermarkLayerDef(
    val id: String,                                    // 图层唯一ID
    val type: WatermarkLayerType,                      // 图层类型
    val content: String = "",                          // 当前内容（可为空，从 EXIF 填充）
    val defaultContent: String = "",                   // 默认文本
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val style: WatermarkLayerStyle = WatermarkLayerStyle(),
    val isRequired: Boolean = false,                   // 是否必选（不可删除）
    val isVisible: Boolean = true,                     // 是否可见
    val contentSource: ContentSource = ContentSource.MANUAL,
    val sortOrder: Int = 0,                            // 图层排序（越大越靠上）
    val offset: OffsetData = OffsetData()              // 手动偏移
)

/**
 * 偏移数据（用于自定义位置微调）
 */
@Serializable
data class OffsetData(
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * 图层类型枚举
 */
@Serializable
enum class WatermarkLayerType {
    TEXT,        // 自由文本
    BRAND,       // 品牌名
    DEVICE,      // 设备型号（自动）
    PARAMS,      // 拍摄参数（EXIF）
    TIMESTAMP,   // 时间戳（自动）
    LOCATION,    // GPS 位置（自动）
    LOGO,        // Logo 图片
    SHAPE,       // 形状（线条/边框/圆角矩形背景）
    VIGNETTE     // 暗角效果
}

/**
 * 内容来源枚举
 */
@Serializable
enum class ContentSource {
    MANUAL,      // 手动输入
    EXIF,        // 从照片 EXIF 读取
    GPS,         // 从 GPS 坐标反地理编码
    SYSTEM,      // 系统时间
    DEVICE_INFO  // 设备信息
}

/**
 * 图层位置枚举
 */
@Serializable
enum class WatermarkPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT, CENTER_BOTTOM,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
    CUSTOM       // 自定义位置（通过offset控制）
}

/**
 * 图层样式定义
 */
@Serializable
data class WatermarkLayerStyle(
    val fontSize: Float = 14f,              // 字体大小 (sp)
    val fontFamily: String = "default",     // 字体家族
    val fontWeight: Int = 400,              // 字体粗细 (100-900)
    val colorHex: String = "#FFFFFF",       // 颜色 (十六进制)
    val opacity: Float = 0.8f,              // 透明度 (0-1)
    val letterSpacing: Float = 0f,          // 字间距
    val rotation: Float = 0f,               // 旋转角度
    val shadowEnabled: Boolean = true,      // 阴影开关
    val shadowBlur: Float = 4f,             // 阴影模糊度
    val shadowColorHex: String = "#000000", // 阴影颜色
    val backgroundColorHex: String = "transparent", // 背景颜色
    val backgroundOpacity: Float = 0f,      // 背景透明度
    val padding: Float = 8f,                // 内边距
    val cornerRadius: Float = 0f,           // 圆角半径
    val lineHeight: Float = 1.5f            // 行高倍数
) {
    /**
     * 获取颜色对象
     */
    fun getColor(): Int {
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    /**
     * 获取阴影颜色
     */
    fun getShadowColor(): Int {
        return try {
            Color.parseColor(shadowColorHex)
        } catch (e: Exception) {
            Color.BLACK
        }
    }

    /**
     * 获取背景颜色
     */
    fun getBackgroundColor(): Int? {
        return if (backgroundColorHex == "transparent" || backgroundOpacity == 0f) {
            null
        } else {
            try {
                Color.parseColor(backgroundColorHex)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * 水印模板定义
 * 模板 = 预设图层组合 + 默认样式
 */
@Serializable
data class WatermarkTemplateDef(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "全部",          // 分类：全部/品牌/极简/技术/个人/社交
    val layers: List<WatermarkLayerDef>,    // 图层列表
    val isSystem: Boolean = true,           // 是否系统模板（不可删除）
    val previewImagePath: String = ""       // 预览图路径
)

/**
 * 水印配置（完整配置）
 */
@Serializable
data class WatermarkConfigDef(
    val id: String = "",
    val name: String = "自定义水印",
    val layers: List<WatermarkLayerDef> = emptyList(),
    val baseTemplateId: String? = null,     // 基于哪个模板创建
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取可见图层（按排序顺序）
     */
    fun getVisibleLayers(): List<WatermarkLayerDef> {
        return layers
            .filter { it.isVisible }
            .sortedByDescending { it.sortOrder }
    }

    /**
     * 添加图层
     */
    fun addLayer(layer: WatermarkLayerDef): WatermarkConfigDef {
        return copy(
            layers = layers + layer,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 移除图层
     */
    fun removeLayer(layerId: String): WatermarkConfigDef {
        return copy(
            layers = layers.filter { it.id != layerId || it.isRequired },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 更新图层
     */
    fun updateLayer(layerId: String, newLayer: WatermarkLayerDef): WatermarkConfigDef {
        return copy(
            layers = layers.map { if (it.id == layerId) newLayer else it },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 调整图层顺序
     */
    fun reorderLayers(layerId: String, newSortOrder: Int): WatermarkConfigDef {
        return copy(
            layers = layers.map { 
                if (it.id == layerId) it.copy(sortOrder = newSortOrder) else it 
            },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * 序列化为JSON
     */
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    /**
     * 从JSON反序列化
     */
    companion object {
        fun fromJson(json: String): WatermarkConfigDef {
            return Json.decodeFromString(json)
        }
    }
}

/**
 * 系统预设模板
 */
object SystemWatermarkTemplates {

    /**
     * 获取所有系统模板
     */
    fun getAll(): List<WatermarkTemplateDef> = listOf(
        // 经典水印
        classicTemplate(),
        // 哈苏大师印记
        hasselbladTemplate(),
        // 徕卡风格
        leicaTemplate(),
        // 极简水印
        minimalTemplate(),
        // 详细参数水印
        detailedTemplate(),
        // 地理位置水印
        geoTemplate(),
        // 社交媒体水印
        socialTemplate()
    )

    /**
     * 经典水印模板
     */
    private fun classicTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "classic",
            name = "经典",
            description = "经典水印风格，包含品牌、设备、参数、日期",
            category = "品牌",
            layers = listOf(
                WatermarkLayerDef(
                    id = "brand",
                    type = WatermarkLayerType.BRAND,
                    content = "HASSELBLAD",
                    defaultContent = "HASSELBLAD",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 16f, fontWeight = 700),
                    isRequired = false,
                    contentSource = ContentSource.MANUAL,
                    sortOrder = 4
                ),
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 12f),
                    contentSource = ContentSource.DEVICE_INFO,
                    sortOrder = 3
                ),
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 11f, opacity = 0.7f),
                    contentSource = ContentSource.EXIF,
                    sortOrder = 2
                ),
                WatermarkLayerDef(
                    id = "timestamp",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 10f, opacity = 0.6f),
                    contentSource = ContentSource.SYSTEM,
                    sortOrder = 1
                )
            )
        )
    }

    /**
     * 哈苏大师印记模板
     */
    private fun hasselbladTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "hasselblad_classic",
            name = "哈苏",
            description = "哈苏大师印记，专业摄影风格",
            category = "品牌",
            layers = listOf(
                WatermarkLayerDef(
                    id = "brand",
                    type = WatermarkLayerType.BRAND,
                    content = "HASSELBLAD",
                    defaultContent = "HASSELBLAD",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(
                        fontSize = 18f,
                        fontWeight = 700,
                        letterSpacing = 2f,
                        shadowEnabled = true,
                        shadowBlur = 6f
                    ),
                    isRequired = true,
                    contentSource = ContentSource.MANUAL,
                    sortOrder = 5
                ),
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 13f, fontWeight = 500),
                    contentSource = ContentSource.DEVICE_INFO,
                    sortOrder = 4
                ),
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 12f, opacity = 0.85f),
                    contentSource = ContentSource.EXIF,
                    sortOrder = 3
                ),
                WatermarkLayerDef(
                    id = "timestamp",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 11f, opacity = 0.75f),
                    contentSource = ContentSource.SYSTEM,
                    sortOrder = 2
                ),
                WatermarkLayerDef(
                    id = "vignette",
                    type = WatermarkLayerType.VIGNETTE,
                    position = WatermarkPosition.CENTER,
                    style = WatermarkLayerStyle(opacity = 0.3f),
                    sortOrder = 0
                )
            )
        )
    }

    /**
     * 徕卡风格模板
     */
    private fun leicaTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "leica_style",
            name = "徕卡",
            description = "徕卡红点风格，简约专业",
            category = "品牌",
            layers = listOf(
                WatermarkLayerDef(
                    id = "brand",
                    type = WatermarkLayerType.BRAND,
                    content = "LEICA",
                    defaultContent = "LEICA",
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(
                        fontSize = 14f,
                        fontWeight = 700,
                        colorHex = "#FF0000", // 红色
                        letterSpacing = 1f
                    ),
                    contentSource = ContentSource.MANUAL,
                    sortOrder = 3
                ),
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(fontSize = 11f),
                    contentSource = ContentSource.EXIF,
                    sortOrder = 2
                )
            )
        )
    }

    /**
     * 极简水印模板
     */
    private fun minimalTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "minimal",
            name = "极简",
            description = "简约水印，仅显示设备型号",
            category = "极简",
            layers = listOf(
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(fontSize = 12f, opacity = 0.6f),
                    contentSource = ContentSource.DEVICE_INFO,
                    sortOrder = 1
                )
            )
        )
    }

    /**
     * 详细参数水印模板
     */
    private fun detailedTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "detailed",
            name = "详细",
            description = "完整参数水印，包含所有EXIF信息",
            category = "技术",
            layers = listOf(
                WatermarkLayerDef(
                    id = "brand",
                    type = WatermarkLayerType.BRAND,
                    content = "HASSELBLAD",
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 16f, fontWeight = 700),
                    sortOrder = 6
                ),
                WatermarkLayerDef(
                    id = "device",
                    type = WatermarkLayerType.DEVICE,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 13f),
                    contentSource = ContentSource.DEVICE_INFO,
                    sortOrder = 5
                ),
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 12f),
                    contentSource = ContentSource.EXIF,
                    sortOrder = 4
                ),
                WatermarkLayerDef(
                    id = "timestamp",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 11f),
                    contentSource = ContentSource.SYSTEM,
                    sortOrder = 3
                ),
                WatermarkLayerDef(
                    id = "location",
                    type = WatermarkLayerType.LOCATION,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 10f, opacity = 0.7f),
                    contentSource = ContentSource.GPS,
                    sortOrder = 2
                ),
                WatermarkLayerDef(
                    id = "vignette",
                    type = WatermarkLayerType.VIGNETTE,
                    position = WatermarkPosition.CENTER,
                    style = WatermarkLayerStyle(opacity = 0.25f),
                    sortOrder = 0
                )
            )
        )
    }

    /**
     * 地理位置水印模板
     */
    private fun geoTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "geo",
            name = "地理",
            description = "带地理位置的水印",
            category = "个人",
            layers = listOf(
                WatermarkLayerDef(
                    id = "location",
                    type = WatermarkLayerType.LOCATION,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 14f, fontWeight = 500),
                    contentSource = ContentSource.GPS,
                    sortOrder = 3
                ),
                WatermarkLayerDef(
                    id = "timestamp",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 12f),
                    contentSource = ContentSource.SYSTEM,
                    sortOrder = 2
                ),
                WatermarkLayerDef(
                    id = "params",
                    type = WatermarkLayerType.PARAMS,
                    position = WatermarkPosition.BOTTOM_LEFT,
                    style = WatermarkLayerStyle(fontSize = 11f, opacity = 0.7f),
                    contentSource = ContentSource.EXIF,
                    sortOrder = 1
                )
            )
        )
    }

    /**
     * 社交媒体水印模板
     */
    private fun socialTemplate(): WatermarkTemplateDef {
        return WatermarkTemplateDef(
            id = "social",
            name = "社交",
            description = "社交媒体风格水印",
            category = "社交",
            layers = listOf(
                WatermarkLayerDef(
                    id = "brand",
                    type = WatermarkLayerType.TEXT,
                    content = "@YourName",
                    defaultContent = "@YourName",
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(fontSize = 14f, fontWeight = 600),
                    contentSource = ContentSource.MANUAL,
                    sortOrder = 2
                ),
                WatermarkLayerDef(
                    id = "timestamp",
                    type = WatermarkLayerType.TIMESTAMP,
                    position = WatermarkPosition.BOTTOM_RIGHT,
                    style = WatermarkLayerStyle(fontSize = 10f, opacity = 0.5f),
                    contentSource = ContentSource.SYSTEM,
                    sortOrder = 1
                )
            )
        )
    }

    /**
     * 根据ID获取模板
     */
    fun getById(id: String): WatermarkTemplateDef? {
        return getAll().find { it.id == id }
    }

    /**
     * 根据分类获取模板
     */
    fun getByCategory(category: String): List<WatermarkTemplateDef> {
        return if (category == "全部") {
            getAll()
        } else {
            getAll().filter { it.category == category }
        }
    }
}

/**
 * 图层管理器
 */
class WatermarkLayerManager {

    private var currentConfig: WatermarkConfigDef = WatermarkConfigDef()
    private val customTemplates = mutableListOf<WatermarkTemplateDef>()

    /**
     * 应用模板
     */
    fun applyTemplate(templateId: String): WatermarkConfigDef {
        val template = SystemWatermarkTemplates.getById(templateId)
        if (template != null) {
            currentConfig = WatermarkConfigDef(
                id = generateConfigId(),
                name = template.name,
                layers = template.layers.map { it.copy(id = generateLayerId()) },
                baseTemplateId = templateId
            )
        }
        return currentConfig
    }

    /**
     * 添加图层
     */
    fun addLayer(type: WatermarkLayerType, content: String = ""): WatermarkLayerDef {
        val layer = WatermarkLayerDef(
            id = generateLayerId(),
            type = type,
            content = content,
            position = WatermarkPosition.BOTTOM_LEFT,
            sortOrder = currentConfig.layers.size + 1,
            contentSource = getDefaultContentSource(type)
        )
        currentConfig = currentConfig.addLayer(layer)
        return layer
    }

    /**
     * 移除图层
     */
    fun removeLayer(layerId: String): Boolean {
        val layer = currentConfig.layers.find { it.id == layerId }
        if (layer?.isRequired == true) {
            return false // 必选图层不可删除
        }
        currentConfig = currentConfig.removeLayer(layerId)
        return true
    }

    /**
     * 更新图层内容
     */
    fun updateLayerContent(layerId: String, content: String) {
        currentConfig.layers.find { it.id == layerId }?.let { layer ->
            currentConfig = currentConfig.updateLayer(layerId, layer.copy(content = content))
        }
    }

    /**
     * 更新图层位置
     */
    fun updateLayerPosition(layerId: String, position: WatermarkPosition) {
        currentConfig.layers.find { it.id == layerId }?.let { layer ->
            currentConfig = currentConfig.updateLayer(layerId, layer.copy(position = position))
        }
    }

    /**
     * 更新图层样式
     */
    fun updateLayerStyle(layerId: String, style: WatermarkLayerStyle) {
        currentConfig.layers.find { it.id == layerId }?.let { layer ->
            currentConfig = currentConfig.updateLayer(layerId, layer.copy(style = style))
        }
    }

    /**
     * 更新图层可见性
     */
    fun toggleLayerVisibility(layerId: String) {
        currentConfig.layers.find { it.id == layerId }?.let { layer ->
            currentConfig = currentConfig.updateLayer(layerId, layer.copy(isVisible = !layer.isVisible))
        }
    }

    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): WatermarkConfigDef = currentConfig

    /**
     * 保存为自定义模板
     */
    fun saveAsTemplate(name: String): WatermarkTemplateDef {
        val template = WatermarkTemplateDef(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            category = "个人",
            layers = currentConfig.layers,
            isSystem = false
        )
        customTemplates.add(template)
        return template
    }

    /**
     * 获取所有模板（系统 + 自定义）
     */
    fun getAllTemplates(): List<WatermarkTemplateDef> {
        return SystemWatermarkTemplates.getAll() + customTemplates
    }

    /**
     * 删除自定义模板
     */
    fun deleteCustomTemplate(templateId: String): Boolean {
        val template = customTemplates.find { it.id == templateId }
        if (template?.isSystem == true) {
            return false // 系统模板不可删除
        }
        customTemplates.removeIf { it.id == templateId }
        return true
    }

    /**
     * 根据图层类型获取默认内容来源
     */
    private fun getDefaultContentSource(type: WatermarkLayerType): ContentSource {
        return when (type) {
            WatermarkLayerType.DEVICE -> ContentSource.DEVICE_INFO
            WatermarkLayerType.PARAMS -> ContentSource.EXIF
            WatermarkLayerType.TIMESTAMP -> ContentSource.SYSTEM
            WatermarkLayerType.LOCATION -> ContentSource.GPS
            else -> ContentSource.MANUAL
        }
    }

    /**
     * 生成配置ID
     */
    private fun generateConfigId(): String {
        return "config_${System.currentTimeMillis()}"
    }

    /**
     * 生成图层ID
     */
    private fun generateLayerId(): String {
        return "layer_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}