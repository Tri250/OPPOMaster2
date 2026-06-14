package com.silas.omaster.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.*
import org.junit.Test

/**
 * Typography 系统单元测试
 */
class TypographyTest {

    @Test
    fun `Typography包含所有Material3样式`() {
        val typography = Typography
        
        // Display样式
        assertNotNull(typography.displayLarge)
        assertNotNull(typography.displayMedium)
        assertNotNull(typography.displaySmall)
        
        // Headline样式
        assertNotNull(typography.headlineLarge)
        assertNotNull(typography.headlineMedium)
        assertNotNull(typography.headlineSmall)
        
        // Title样式
        assertNotNull(typography.titleLarge)
        assertNotNull(typography.titleMedium)
        assertNotNull(typography.titleSmall)
        
        // Body样式
        assertNotNull(typography.bodyLarge)
        assertNotNull(typography.bodyMedium)
        assertNotNull(typography.bodySmall)
        
        // Label样式
        assertNotNull(typography.labelLarge)
        assertNotNull(typography.labelMedium)
        assertNotNull(typography.labelSmall)
    }

    @Test
    fun `displayLarge字体大小为26sp`() {
        val typography = Typography
        
        assertEquals(26.sp, typography.displayLarge.fontSize)
    }

    @Test
    fun `displayMedium字体大小为24sp`() {
        val typography = Typography
        
        assertEquals(24.sp, typography.displayMedium.fontSize)
    }

    @Test
    fun `displaySmall字体大小为20sp`() {
        val typography = Typography
        
        assertEquals(20.sp, typography.displaySmall.fontSize)
    }

    @Test
    fun `titleLarge字体大小为22sp`() {
        val typography = Typography
        
        assertEquals(22.sp, typography.titleLarge.fontSize)
    }

    @Test
    fun `titleMedium字体大小为16sp`() {
        val typography = Typography
        
        assertEquals(16.sp, typography.titleMedium.fontSize)
    }

    @Test
    fun `titleSmall字体大小为14sp`() {
        val typography = Typography
        
        assertEquals(14.sp, typography.titleSmall.fontSize)
    }

    @Test
    fun `bodyLarge字体大小为16sp`() {
        val typography = Typography
        
        assertEquals(16.sp, typography.bodyLarge.fontSize)
    }

    @Test
    fun `bodyMedium字体大小为14sp`() {
        val typography = Typography
        
        assertEquals(14.sp, typography.bodyMedium.fontSize)
    }

    @Test
    fun `bodySmall字体大小为12sp`() {
        val typography = Typography
        
        assertEquals(12.sp, typography.bodySmall.fontSize)
    }

    @Test
    fun `labelLarge字体大小为14sp`() {
        val typography = Typography
        
        assertEquals(14.sp, typography.labelLarge.fontSize)
    }

    @Test
    fun `labelMedium字体大小为12sp`() {
        val typography = Typography
        
        assertEquals(12.sp, typography.labelMedium.fontSize)
    }

    @Test
    fun `labelSmall字体大小为11sp`() {
        val typography = Typography
        
        assertEquals(11.sp, typography.labelSmall.fontSize)
    }

    @Test
    fun `标题样式使用Bold字体`() {
        val typography = Typography
        
        assertEquals(FontWeight.Bold, typography.titleLarge.fontWeight)
        assertEquals(FontWeight.Bold, typography.titleMedium.fontWeight)
        assertEquals(FontWeight.Bold, typography.titleSmall.fontWeight)
    }

    @Test
    fun `正文样式使用Normal字体`() {
        val typography = Typography
        
        assertEquals(FontWeight.Normal, typography.bodyLarge.fontWeight)
        assertEquals(FontWeight.Normal, typography.bodyMedium.fontWeight)
        assertEquals(FontWeight.Normal, typography.bodySmall.fontWeight)
    }

    @Test
    fun `标签样式使用SemiBold或Medium字体`() {
        val typography = Typography
        
        assertEquals(FontWeight.SemiBold, typography.labelLarge.fontWeight)
        assertEquals(FontWeight.Medium, typography.labelMedium.fontWeight)
        assertEquals(FontWeight.Medium, typography.labelSmall.fontWeight)
    }

    @Test
    fun `displayLarge使用Bold字体`() {
        val typography = Typography
        
        assertEquals(FontWeight.Bold, typography.displayLarge.fontWeight)
    }

    @Test
    fun `headlineLarge使用Bold字体`() {
        val typography = Typography
        
        assertEquals(FontWeight.Bold, typography.headlineLarge.fontWeight)
    }

    @Test
    fun `所有样式使用默认字体`() {
        val typography = Typography
        
        // 验证所有样式使用系统默认字体
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, typography.displayLarge.fontFamily)
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, typography.titleLarge.fontFamily)
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, typography.bodyLarge.fontFamily)
        assertEquals(androidx.compose.ui.text.font.FontFamily.Default, typography.labelLarge.fontFamily)
    }
}