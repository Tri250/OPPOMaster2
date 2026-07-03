package com.silas.omaster.engine

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Test

/**
 * HistogramAnalyzer 单元测试
 *
 * 测试直方图分析器的核心功能：
 * - RGB/L 四通道直方图统计
 * - 均值亮度计算
 * - 阴影/高光裁剪检测
 */
class HistogramAnalyzerTest {

    private val analyzer = HistogramAnalyzer()

    @Test
    fun `analyze returns valid histogram for solid color bitmap`() {
        // 创建纯红色 100x100 Bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)

        val result = analyzer.analyze(bitmap)

        // 验证直方图维度
        assertEquals(256, result.luminance.size)
        assertEquals(256, result.red.size)
        assertEquals(256, result.green.size)
        assertEquals(256, result.blue.size)

        // 验证均值亮度（红色亮度约 76）
        assertTrue(result.meanLuminance > 70f)
        assertTrue(result.meanLuminance < 80f)

        // 验证红色通道峰值
        assertTrue(result.red[255] > 0)
        assertTrue(result.green[0] > 0)
        assertTrue(result.blue[0] > 0)

        bitmap.recycle()
    }

    @Test
    fun `analyzeLuminanceOnly returns valid histogram`() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val luminance = analyzer.analyzeLuminanceOnly(bitmap)

        assertEquals(256, luminance.size)
        // 白色亮度应为 255
        assertTrue(luminance[255] > 0)

        bitmap.recycle()
    }

    @Test
    fun `analyze detects shadow clipping for dark image`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLACK)

        val result = analyzer.analyze(bitmap)

        // 黑色图像应检测到阴影裁剪
        assertTrue(result.shadowClipping)

        bitmap.recycle()
    }

    @Test
    fun `analyze detects highlight clipping for bright image`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)

        val result = analyzer.analyze(bitmap)

        // 白色图像应检测到高光裁剪
        assertTrue(result.highlightClipping)

        bitmap.recycle()
    }

    @Test
    fun `analyze with custom sampleStep`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)

        // 使用不同采样步长
        val result1 = analyzer.analyze(bitmap, sampleStep = 2)
        val result2 = analyzer.analyze(bitmap, sampleStep = 8)

        // 两种采样结果应相似（蓝色亮度约 29）
        assertTrue(result1.meanLuminance > 25f)
        assertTrue(result2.meanLuminance > 25f)

        bitmap.recycle()
    }
}