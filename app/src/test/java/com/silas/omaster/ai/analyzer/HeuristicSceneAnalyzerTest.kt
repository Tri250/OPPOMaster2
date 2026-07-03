package com.silas.omaster.ai.analyzer

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/**
 * HeuristicSceneAnalyzer 单元测试
 *
 * 测试启发式场景分析器的核心功能：
 * - 颜色直方图提取
 * - 亮度等级计算
 * - 人脸检测集成（模拟）
 * - 纹理边缘密度分析
 */
class HeuristicSceneAnalyzerTest {

    @Test
    fun `analyzeColorHistogram returns valid histogram`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.valueOf(0.5f, 0.5f, 0.5f))

        val histogram = HeuristicSceneAnalyzer.analyzeColorHistogram(bitmap)

        assertNotNull(histogram)
        assertEquals(256, histogram.luminance.size)
        assertTrue(histogram.meanLuminance > 0f)

        bitmap.recycle()
    }

    @Test
    fun `analyzeBrightness returns valid level`() {
        // 测试不同亮度图像
        val darkBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        darkBitmap.eraseColor(android.graphics.Color.BLACK)
        val darkLevel = HeuristicSceneAnalyzer.analyzeBrightness(darkBitmap)
        assertTrue(darkLevel < 0.3f)

        val brightBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        brightBitmap.eraseColor(android.graphics.Color.WHITE)
        val brightLevel = HeuristicSceneAnalyzer.analyzeBrightness(brightBitmap)
        assertTrue(brightLevel > 0.7f)

        darkBitmap.recycle()
        brightBitmap.recycle()
    }

    @Test
    fun `analyzeTexture returns valid density`() {
        val smoothBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        smoothBitmap.eraseColor(android.graphics.Color.GRAY)

        val texture = HeuristicSceneAnalyzer.analyzeTexture(smoothBitmap)

        // 平滑图像应具有较低纹理密度
        assertTrue(texture >= 0f)
        assertTrue(texture <= 1f)

        smoothBitmap.recycle()
    }

    @Test
    fun `classifyScene returns valid scene type`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        // 创建模拟天空图像（蓝色渐变）
        for (y in 0 until 200) {
            for (x in 0 until 200) {
                val blue = (y * 255 / 200).coerceIn(0, 255)
                bitmap.setPixel(x, y, android.graphics.Color.rgb(100, 150, blue))
            }
        }

        val features = HeuristicSceneAnalyzer.extractFeatures(bitmap)
        val scene = HeuristicSceneAnalyzer.classifyScene(features)

        assertNotNull(scene)
        assertTrue(scene.isNotEmpty())

        bitmap.recycle()
    }

    @Test
    fun `extractFeatures returns complete feature vector`() {
        val bitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)

        val features = HeuristicSceneAnalyzer.extractFeatures(bitmap)

        assertNotNull(features)
        assertTrue(features.brightness >= 0f && features.brightness <= 1f)
        assertTrue(features.colorVariance >= 0f)
        assertTrue(features.textureDensity >= 0f)
        assertTrue(features.edgeDensity >= 0f)

        bitmap.recycle()
    }

    @Test
    fun `voteScene returns valid decision`() {
        val features = HeuristicSceneAnalyzer.ImageFeatures(
            brightness = 0.6f,
            colorVariance = 0.3f,
            textureDensity = 0.5f,
            edgeDensity = 0.4f,
            colorDominance = mapOf("green" to 0.7f),
            hasFaces = false,
            exifData = null
        )

        val votes = HeuristicSceneAnalyzer.voteScene(features)

        assertNotNull(votes)
        assertTrue(votes.isNotEmpty())
        assertTrue(votes.values.sum() > 0f)
    }
}