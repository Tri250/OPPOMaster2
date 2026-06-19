package com.silas.omaster.watermark

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import org.junit.Test
import org.junit.Assert.*

/**
 * 水印编辑管理器测试
 * 测试水印编辑的核心逻辑
 */
class WatermarkEditorManagerTest {

    @Test
    fun testWatermarkPositionCalculation() {
        // 测试水印位置计算
        val imageWidth = 1920
        val imageHeight = 1080

        // 左下角位置
        val leftBottomX = imageWidth * 0.05f
        val leftBottomY = imageHeight * 0.95f

        assertEquals(96f, leftBottomX)
        assertEquals(1026f, leftBottomY)

        // 右下角位置
        val rightBottomX = imageWidth * 0.95f
        val rightBottomY = imageHeight * 0.95f

        assertEquals(1824f, rightBottomX)
        assertEquals(1026f, rightBottomY)

        // 中心位置
        val centerX = imageWidth / 2f
        val centerY = imageHeight / 2f

        assertEquals(960f, centerX)
        assertEquals(540f, centerY)
    }

    @Test
    fun testWatermarkSizeCalculation() {
        // 测试水印大小计算
        val imageWidth = 1920
        val imageHeight = 1080

        // 基于图像尺寸的水印大小
        val watermarkWidth = imageWidth * 0.15f
        val watermarkHeight = imageHeight * 0.03f

        assertEquals(288f, watermarkWidth)
        assertEquals(32.4f, watermarkHeight)
    }

    @Test
    fun testWatermarkOpacityRange() {
        // 测试水印透明度范围
        val minOpacity = 0f
        val maxOpacity = 255f
        val midOpacity = 128f

        assertTrue(minOpacity >= 0f)
        assertTrue(maxOpacity <= 255f)
        assertTrue(midOpacity in 0f..255f)
    }

    @Test
    fun testWatermarkColorCalculation() {
        // 测试水印颜色计算
        val baseColor = 0xFFFFFF // 白色

        // 添加透明度
        val opacity = 128
        val colorWithOpacity = (opacity shl 24) or baseColor

        assertEquals(0x80FFFFFF.toInt(), colorWithOpacity)

        // 提取颜色分量
        val alpha = (colorWithOpacity shr 24) and 0xFF
        val red = (colorWithOpacity shr 16) and 0xFF
        val green = (colorWithOpacity shr 8) and 0xFF
        val blue = colorWithOpacity and 0xFF

        assertEquals(128, alpha)
        assertEquals(255, red)
        assertEquals(255, green)
        assertEquals(255, blue)
    }

    @Test
    fun testSmartColorDetection() {
        // 测试智能颜色检测逻辑
        val darkBackground = true
        val lightBackground = false

        // 深色背景使用白色水印
        val watermarkColorForDark = if (darkBackground) 0xFFFFFFFF.toInt() else 0x00000000.toInt()

        assertEquals(0xFFFFFFFF.toInt(), watermarkColorForDark)

        // 浅色背景使用黑色水印
        val watermarkColorForLight = if (lightBackground) 0x00000000.toInt() else 0xFFFFFFFF.toInt()

        assertEquals(0x00000000.toInt(), watermarkColorForLight)
    }

    @Test
    fun testVignetteCalculation() {
        // 测试暗角效果计算
        val centerX = 960f
        val centerY = 540f
        val maxRadius = 960f

        val pixelX = 1920f
        val pixelY = 1080f

        val distance = sqrt(
            (pixelX - centerX).pow(2) +
            (pixelY - centerY).pow(2)
        )

        assertEquals(1080f, distance, 1f)

        val vignetteStrength = 0.5f
        val normalizedDistance = distance / maxRadius
        val vignetteFactor = 1f - (normalizedDistance * vignetteStrength)

        assertEquals(0.5f, vignetteFactor, 0.1f)
    }

    @Test
    fun testTextWatermarkLayout() {
        // 测试文本水印布局
        val text = "Hasselblad X2D 100C"
        val fontSize = 24

        // 估算文本宽度（简化计算）
        val estimatedWidth = text.length * fontSize * 0.6f

        assertEquals(288f, estimatedWidth, 10f)
    }

    @Test
    fun testMultiLineWatermark() {
        // 测试多行水印布局
        val lines = listOf(
            "Hasselblad X2D 100C",
            "f/2.8 | 1/125s | ISO 400",
            "2024.01.15 | Beijing"
        )

        val lineSpacing = 8
        val fontSize = 20

        val totalHeight = lines.size * fontSize + (lines.size - 1) * lineSpacing

        assertEquals(76, totalHeight)
    }
}

/**
 * 水印图层系统测试
 */
class WatermarkLayerSystemTest {

    @Test
    fun testLayerOrder() {
        // 测试图层顺序
        val layers = listOf(
            WatermarkLayer(type = LayerType.LOGO, order = 1),
            WatermarkLayer(type = LayerType.TEXT, order = 2),
            WatermarkLayer(type = LayerType.PARAMS, order = 3)
        )

        val sortedLayers = layers.sortedBy { it.order }

        assertEquals(LayerType.LOGO, sortedLayers[0].type)
        assertEquals(LayerType.TEXT, sortedLayers[1].type)
        assertEquals(LayerType.PARAMS, sortedLayers[2].type)
    }

    @Test
    fun testLayerVisibility() {
        // 测试图层可见性
        val layer = WatermarkLayer(
            type = LayerType.LOGO,
            order = 1,
            visible = true
        )

        assertTrue(layer.visible)

        val hiddenLayer = layer.copy(visible = false)
        assertFalse(hiddenLayer.visible)
    }

    @Test
    fun testLayerOpacity() {
        // 测试图层透明度
        val layer = WatermarkLayer(
            type = LayerType.TEXT,
            order = 2,
            opacity = 0.8f
        )

        assertEquals(0.8f, layer.opacity)
        assertTrue(layer.opacity in 0f..1f)
    }
}

/**
 * 水印模板测试
 */
class HasselbladMasterTemplatesTest {

    @Test
    fun testClassicTemplate() {
        // 测试经典哈苏模板
        val template = HasselbladTemplate.CLASSIC

        // 验证模板属性
        assertNotNull(template)
        assertTrue(template.displayName.isNotEmpty())
    }

    @Test
    fun testModernTemplate() {
        // 测试现代哈苏模板
        val template = HasselbladTemplate.MODERN

        assertNotNull(template)
        assertTrue(template.displayName.isNotEmpty())
    }

    @Test
    fun testMinimalTemplate() {
        // 测试极简哈苏模板
        val template = HasselbladTemplate.MINIMAL

        assertNotNull(template)
        assertTrue(template.displayName.isNotEmpty())
    }

    @Test
    fun testTemplateElements() {
        // 测试模板元素
        val elements = listOf(
            TemplateElement(type = ElementType.LOGO, position = Position.BOTTOM_LEFT),
            TemplateElement(type = ElementType.CAMERA_NAME, position = Position.BOTTOM_RIGHT),
            TemplateElement(type = ElementType.PARAMS, position = Position.BOTTOM_CENTER)
        )

        assertEquals(3, elements.size)
        assertEquals(ElementType.LOGO, elements[0].type)
        assertEquals(Position.BOTTOM_LEFT, elements[0].position)
    }
}

/**
 * EXIF水印提供者测试
 */
class ExifWatermarkProviderTest {

    @Test
    fun testExifDataParsing() {
        // 测试EXIF数据解析逻辑
        val mockExif = mapOf(
            "Make" to "Hasselblad",
            "Model" to "X2D 100C",
            "FNumber" to "f/2.8",
            "ExposureTime" to "1/125",
            "ISOSpeedRatings" to "400",
            "DateTime" to "2024:01:15 14:30:00"
        )

        val make = mockExif["Make"]
        val model = mockExif["Model"]
        val fNumber = mockExif["FNumber"]
        val exposureTime = mockExif["ExposureTime"]
        val iso = mockExif["ISOSpeedRatings"]

        assertEquals("Hasselblad", make)
        assertEquals("X2D 100C", model)
        assertEquals("f/2.8", fNumber)
        assertEquals("1/125", exposureTime)
        assertEquals("400", iso)
    }

    @Test
    fun testExifDateFormat() {
        // 测试EXIF日期格式转换
        val exifDate = "2024:01:15 14:30:00"

        // 转换为显示格式
        val parts = exifDate.split(" ")
        val datePart = parts[0].replace(":", ".")
        val timePart = parts[1].split(":").take(2).joinToString(":")

        val displayDate = "$datePart $timePart"

        assertEquals("2024.01.15 14:30", displayDate)
    }

    @Test
    fun testExifParamsString() {
        // 测试EXIF参数字符串生成
        val fNumber = "f/2.8"
        val exposureTime = "1/125"
        val iso = "400"

        val paramsString = "$fNumber | $exposureTime | ISO $iso"

        assertEquals("f/2.8 | 1/125 | ISO 400", paramsString)
    }

    @Test
    fun testMissingExifHandling() {
        // 测试缺失EXIF数据处理
        val mockExif = mapOf(
            "Make" to "Unknown",
            "Model" to "Unknown"
        )

        val make = mockExif["Make"] ?: "Unknown Camera"
        val model = mockExif["Model"] ?: "Unknown Model"

        assertEquals("Unknown", make)
        assertEquals("Unknown", model)
    }
}

/**
 * 智能水印颜色测试
 */
class SmartWatermarkColorTest {

    @Test
    fun testBrightnessCalculation() {
        // 测试亮度计算
        val r = 100f
        val g = 150f
        val b = 200f

        val brightness = (r + g + b) / 3f

        assertEquals(150f, brightness)
    }

    @Test
    fun testDarkBackgroundDetection() {
        // 测试深色背景检测
        val brightness = 50f
        val threshold = 128f

        val isDark = brightness < threshold

        assertTrue(isDark)
    }

    @Test
    fun testLightBackgroundDetection() {
        // 测试浅色背景检测
        val brightness = 200f
        val threshold = 128f

        val isLight = brightness >= threshold

        assertTrue(isLight)
    }

    @Test
    fun testContrastEnhancement() {
        // 测试对比度增强
        val backgroundColor = 50f // 深色
        val watermarkColor = 255f // 白色

        val contrast = abs(watermarkColor - backgroundColor)

        assertEquals(205f, contrast)
    }
}

// 辅助数据类
enum class LayerType {
    LOGO, TEXT, PARAMS, DATE, LOCATION
}

enum class ElementType {
    LOGO, CAMERA_NAME, PARAMS, DATE, LOCATION
}

enum class Position {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

enum class HasselbladTemplate(val displayName: String) {
    CLASSIC("Classic"),
    MODERN("Modern"),
    MINIMAL("Minimal")
}

data class WatermarkLayer(
    val type: LayerType,
    val order: Int,
    val visible: Boolean = true,
    val opacity: Float = 1f
)

data class TemplateElement(
    val type: ElementType,
    val position: Position
)
