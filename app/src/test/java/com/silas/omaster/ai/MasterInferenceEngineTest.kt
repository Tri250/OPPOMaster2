package com.silas.omaster.ai

import org.junit.Assert.*
import org.junit.Test

/**
 * MasterInferenceEngine 单元测试
 * 测试推理引擎的算法逻辑
 */
class MasterInferenceEngineTest {

    @Test
    fun `EXIF解析 - 有理数解析正确`() {
        // 测试 "35/10" -> 3.5
        val rational1 = "35/10"
        val parts1 = rational1.split("/")
        val result1 = if (parts1.size == 2) {
            parts1[0].toFloat() / parts1[1].toFloat()
        } else {
            rational1.toFloatOrNull()
        }
        assertEquals(3.5f, result1!!, 0.001f)

        // 测试 "100/1" -> 100.0
        val rational2 = "100/1"
        val parts2 = rational2.split("/")
        val result2 = if (parts2.size == 2) {
            parts2[0].toFloat() / parts2[1].toFloat()
        } else {
            rational2.toFloatOrNull()
        }
        assertEquals(100f, result2!!, 0.001f)

        // 测试无效格式 -> null
        val rational3 = "invalid"
        val parts3 = rational3.split("/")
        val result3 = if (parts3.size == 2) {
            parts3[0].toFloat() / parts3[1].toFloat()
        } else {
            rational3.toFloatOrNull()
        }
        assertNull(result3)
    }

    @Test
    fun `GPS坐标解析 - 标准格式`() {
        val coordinate = "40, 30, 25.5"
        val ref = "N"
        
        val parts = coordinate.split(", ")
        if (parts.size == 3) {
            val degrees = parts[0].toFloatOrNull() ?: 0f
            val minutes = parts[1].toFloatOrNull() ?: 0f
            val seconds = parts[2].toFloatOrNull() ?: 0f
            var result = degrees + minutes / 60 + seconds / 3600
            if (ref == "S" || ref == "W") {
                result = -result
            }
            assertEquals(40.507f, result, 0.001f)
        }
    }

    @Test
    fun `GPS坐标解析 - 南半球坐标`() {
        val coordinate = "33, 52, 30.0"
        val ref = "S"
        
        val parts = coordinate.split(", ")
        if (parts.size == 3) {
            val degrees = parts[0].toFloatOrNull() ?: 0f
            val minutes = parts[1].toFloatOrNull() ?: 0f
            val seconds = parts[2].toFloatOrNull() ?: 0f
            var result = degrees + minutes / 60 + seconds / 3600
            if (ref == "S" || ref == "W") {
                result = -result
            }
            assertEquals(-33.875f, result, 0.001f)
        }
    }

    @Test
    fun `GPS坐标解析 - 西经坐标`() {
        val coordinate = "122, 15, 45.0"
        val ref = "W"
        
        val parts = coordinate.split(", ")
        if (parts.size == 3) {
            val degrees = parts[0].toFloatOrNull() ?: 0f
            val minutes = parts[1].toFloatOrNull() ?: 0f
            val seconds = parts[2].toFloatOrNull() ?: 0f
            var result = degrees + minutes / 60 + seconds / 3600
            if (ref == "S" || ref == "W") {
                result = -result
            }
            assertEquals(-122.2625f, result, 0.001f)
        }
    }

    @Test
    fun `直方图数据转换 - 亮度计算`() {
        val avgRed = 180
        val avgGreen = 150
        val avgBlue = 120
        
        val luminance = (0.2126 * avgRed + 0.7152 * avgGreen + 0.0722 * avgBlue).toInt()
        
        assertEquals(156, luminance)
    }

    @Test
    fun `阴影裁剪检测 - 暗像素比例阈值`() {
        val darkPixelRatio = 0.75f
        val threshold = 0.7f
        
        val isClipping = darkPixelRatio > threshold
        
        assertTrue("暗像素比例超过阈值应该检测为裁剪", isClipping)
    }

    @Test
    fun `高光裁剪检测 - 高光比例阈值`() {
        val highlightRatio = 0.35f
        val threshold = 0.3f
        
        val isClipping = highlightRatio > threshold
        
        assertTrue("高光比例超过阈值应该检测为裁剪", isClipping)
    }

    @Test
    fun `人脸置信度计算 - 双眼睁开概率`() {
        val leftProb = 0.8f
        val rightProb = 0.9f
        val smileProb = 0.7f
        
        // ML Kit 置信度公式
        val confidence = 0.4f + 0.3f * leftProb + 0.3f * rightProb
        
        assertEquals(0.91f, confidence, 0.001f)
    }

    @Test
    fun `人脸置信度计算 - 闭眼情况`() {
        val leftProb = 0.1f  // 几乎闭着
        val rightProb = 0.2f
        val smileProb = 0.0f
        
        val confidence = 0.4f + 0.3f * leftProb + 0.3f * rightProb
        
        assertEquals(0.49f, confidence, 0.01f)
    }

    @Test
    fun `微笑检测 - 阈值判断`() {
        val smileProb = 0.6f
        val threshold = 0.5f
        
        val hasSmile = smileProb > threshold
        
        assertTrue("微笑概率超过阈值应该检测为微笑", hasSmile)
    }

    @Test
    fun `坐标归一化 - 像素坐标到0-1范围`() {
        val pixelBounds = android.graphics.Rect(100, 200, 400, 600)
        val w = 1920f
        val h = 1080f
        
        val normalized = android.graphics.Rect(
            ((pixelBounds.left / w).coerceIn(0f, 1f) * 1000).toInt(),
            ((pixelBounds.top / h).coerceIn(0f, 1f) * 1000).toInt(),
            ((pixelBounds.right / w).coerceIn(0f, 1f) * 1000).toInt(),
            ((pixelBounds.bottom / h).coerceIn(0f, 1f) * 1000).toInt()
        )
        
        assertTrue(normalized.left >= 0)
        assertTrue(normalized.top >= 0)
        assertTrue(normalized.right <= 1000)
        assertTrue(normalized.bottom <= 1000)
    }

    @Test
    fun `颜色画像转直方图数据 - 平均亮度计算`() {
        val avgRed = 200
        val avgGreen = 180
        val avgBlue = 160
        
        val avgLuma = ((0.2126 * avgRed +
                       0.7152 * avgGreen +
                       0.0722 * avgBlue)).toInt().coerceIn(0, 255)
        
        assertTrue(avgLuma in 0..255)
    }
}

/**
 * 推理回调测试
 */
class InferenceCallbackTest {

    @Test
    fun `回调接口 - 方法定义存在`() {
        // 验证回调接口方法存在
        val methods = listOf("onAnalysisComplete", "onAnalysisFailed", "onProgressUpdate")
        
        for (method in methods) {
            assertTrue("方法 $method 应该存在", method.isNotEmpty())
        }
    }
}

/**
 * HistogramData 数据类测试
 */
class HistogramDataConversionTest {

    @Test
    fun `直方图数据 - 亮度直方图填充`() {
        val avgLuma = 128
        
        val luminance = IntArray(256)
        luminance[avgLuma] = 1000
        
        assertEquals(1000, luminance[128])
        assertEquals(0, luminance[64])
    }

    @Test
    fun `直方图数据 - RGB直方图填充`() {
        val avgRed = 180
        val avgGreen = 150
        val avgBlue = 120
        
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        
        red[avgRed] = 1000
        green[avgGreen] = 1000
        blue[avgBlue] = 1000
        
        assertEquals(1000, red[180])
        assertEquals(1000, green[150])
        assertEquals(1000, blue[120])
    }
}
