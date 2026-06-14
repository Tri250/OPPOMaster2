package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

/**
 * Color 系统单元测试
 */
class ColorTest {

    @Test
    fun `HasselbladOrange颜色值正确`() {
        assertEquals(Color(0xFFFF6B35), HasselbladOrange)
    }

    @Test
    fun `PureBlack颜色值正确`() {
        assertEquals(Color(0xFF0A0A0A), PureBlack)
    }

    @Test
    fun `DarkGray颜色值正确`() {
        assertEquals(Color(0xFF1A1A1A), DarkGray)
    }

    @Test
    fun `MediumGray颜色值正确`() {
        assertEquals(Color(0xFF333333), MediumGray)
    }

    @Test
    fun `LightGray颜色值正确`() {
        assertEquals(Color(0xFF999999), LightGray)
    }

    @Test
    fun `OffWhite颜色值正确`() {
        assertEquals(Color(0xFFF5F5F5), OffWhite)
    }

    @Test
    fun `SuccessGreen颜色值正确`() {
        assertEquals(Color(0xFF4CAF50), SuccessGreen)
    }

    @Test
    fun `ErrorRed颜色值正确`() {
        assertEquals(Color(0xFFE53935), ErrorRed)
    }

    @Test
    fun `WarningYellow颜色值正确`() {
        assertEquals(Color(0xFFFFC107), WarningYellow)
    }

    @Test
    fun `CyanAccent颜色值正确`() {
        assertEquals(Color(0xFF00BCD4), CyanAccent)
    }

    @Test
    fun `品牌主色HasselbladOrange为橙色`() {
        // 验证RGB值
        val red = (HasselbladOrange.red * 255).toInt()
        val green = (HasselbladOrange.green * 255).toInt()
        val blue = (HasselbladOrange.blue * 255).toInt()
        
        assertEquals(255, red)
        assertEquals(107, green)
        assertEquals(53, blue)
    }

    @Test
    fun `深色背景PureBlack接近黑色`() {
        // 验证RGB值接近黑色
        val red = (PureBlack.red * 255).toInt()
        val green = (PureBlack.green * 255).toInt()
        val blue = (PureBlack.blue * 255).toInt()
        
        assertTrue(red < 20)
        assertTrue(green < 20)
        assertTrue(blue < 20)
    }

    @Test
    fun `SuccessGreen为绿色`() {
        // 验证绿色通道值最高
        val red = (SuccessGreen.red * 255).toInt()
        val green = (SuccessGreen.green * 255).toInt()
        val blue = (SuccessGreen.blue * 255).toInt()
        
        assertTrue(green > red)
        assertTrue(green > blue)
    }

    @Test
    fun `ErrorRed为红色`() {
        // 验证红色通道值最高
        val red = (ErrorRed.red * 255).toInt()
        val green = (ErrorRed.green * 255).toInt()
        val blue = (ErrorRed.blue * 255).toInt()
        
        assertTrue(red > green)
        assertTrue(red > blue)
    }

    @Test
    fun `WarningYellow为黄色`() {
        // 验证红色和绿色通道值高，蓝色低
        val red = (WarningYellow.red * 255).toInt()
        val green = (WarningYellow.green * 255).toInt()
        val blue = (WarningYellow.blue * 255).toInt()
        
        assertTrue(red > 200)
        assertTrue(green > 180)
        assertTrue(blue < 20)
    }

    @Test
    fun `CyanAccent为青色`() {
        // 验证绿色和蓝色通道值高，红色低
        val red = (CyanAccent.red * 255).toInt()
        val green = (CyanAccent.green * 255).toInt()
        val blue = (CyanAccent.blue * 255).toInt()
        
        assertTrue(red < 20)
        assertTrue(green > 180)
        assertTrue(blue > 200)
    }

    @Test
    fun `所有颜色alpha为1`() {
        // 验证所有颜色完全可见
        assertEquals(1f, HasselbladOrange.alpha)
        assertEquals(1f, PureBlack.alpha)
        assertEquals(1f, DarkGray.alpha)
        assertEquals(1f, MediumGray.alpha)
        assertEquals(1f, LightGray.alpha)
        assertEquals(1f, OffWhite.alpha)
        assertEquals(1f, SuccessGreen.alpha)
        assertEquals(1f, ErrorRed.alpha)
        assertEquals(1f, WarningYellow.alpha)
        assertEquals(1f, CyanAccent.alpha)
    }
}