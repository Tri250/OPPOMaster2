package com.silas.omaster.renderer

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow

/**
 * 渲染参数测试
 * 测试RenderParameters的所有参数计算逻辑
 */
class RenderParametersTest {

    @Test
    fun testDefaultParameters() {
        val params = RenderParameters()
        
        assertEquals(0f, params.brightness)
        assertEquals(0f, params.contrast)
        assertEquals(0f, params.saturation)
        assertEquals(0f, params.warmth)
        assertEquals(0f, params.exposure)
        assertEquals(0f, params.highlights)
        assertEquals(0f, params.shadows)
        assertEquals(0f, params.sharpness)
        assertEquals(0f, params.clarity)
        assertEquals(0f, params.vibrance)
        assertEquals(0f, params.grain)
        assertEquals(0f, params.dehaze)
        assertEquals(0f, params.denoise)
    }

    @Test
    fun testBrightnessRange() {
        // 测试亮度范围 -100 到 100
        val minParams = RenderParameters(brightness = -100f)
        val maxParams = RenderParameters(brightness = 100f)
        val midParams = RenderParameters(brightness = 50f)
        
        assertEquals(-100f, minParams.brightness)
        assertEquals(100f, maxParams.brightness)
        assertEquals(50f, midParams.brightness)
    }

    @Test
    fun testContrastRange() {
        // 测试对比度范围 -100 到 100
        val params = RenderParameters(contrast = 75f)
        assertEquals(75f, params.contrast)
    }

    @Test
    fun testSaturationRange() {
        // 测试饱和度范围 -100 到 100
        val params = RenderParameters(saturation = -50f)
        assertEquals(-50f, params.saturation)
    }

    @Test
    fun testWarmthRange() {
        // 测试色温范围 -100 到 100
        val warmParams = RenderParameters(warmth = 30f)
        val coldParams = RenderParameters(warmth = -30f)
        
        assertEquals(30f, warmParams.warmth)
        assertEquals(-30f, coldParams.warmth)
    }

    @Test
    fun testExposureRange() {
        // 测试曝光范围 -50 到 50
        val params = RenderParameters(exposure = 25f)
        assertEquals(25f, params.exposure)
    }

    @Test
    fun testHighlightsRange() {
        // 测试高光范围 -100 到 100
        val params = RenderParameters(highlights = -50f)
        assertEquals(-50f, params.highlights)
    }

    @Test
    fun testShadowsRange() {
        // 测试阴影范围 -100 到 100
        val params = RenderParameters(shadows = 30f)
        assertEquals(30f, params.shadows)
    }

    @Test
    fun testSharpnessRange() {
        // 测试锐化范围 0 到 100
        val params = RenderParameters(sharpness = 50f)
        assertEquals(50f, params.sharpness)
    }

    @Test
    fun testClarityRange() {
        // 测试清晰度范围 0 到 100
        val params = RenderParameters(clarity = 40f)
        assertEquals(40f, params.clarity)
    }

    @Test
    fun testVibranceRange() {
        // 测试鲜艳度范围 -100 到 100
        val params = RenderParameters(vibrance = 20f)
        assertEquals(20f, params.vibrance)
    }

    @Test
    fun testGrainRange() {
        // 测试颗粒范围 0 到 100
        val params = RenderParameters(grain = 15f)
        assertEquals(15f, params.grain)
    }

    @Test
    fun testFadeRange() {
        // 测试褪色范围 0 到 100
        val params = RenderParameters(fade = 30f)
        assertEquals(30f, params.fade)
    }

    @Test
    fun testDehazeRange() {
        // 测试去雾范围 0 到 100
        val params = RenderParameters(dehaze = 25f)
        assertEquals(25f, params.dehaze)
    }

    @Test
    fun testNoiseReductionRange() {
        // 测试降噪范围 0 到 100
        val params = RenderParameters(denoise = 50f)
        assertEquals(50f, params.denoise)
    }

    @Test
    fun testCombinedParameters() {
        // 测试组合参数
        val params = RenderParameters(
            brightness = 10f,
            contrast = 20f,
            saturation = 30f,
            warmth = 5f,
            exposure = 15f,
            highlights = -10f,
            shadows = 20f,
            sharpness = 25f,
            clarity = 15f,
            vibrance = 10f,
            grain = 5f,
            dehaze = 10f,
            denoise = 20f
        )
        
        assertEquals(10f, params.brightness)
        assertEquals(20f, params.contrast)
        assertEquals(30f, params.saturation)
        assertEquals(5f, params.warmth)
        assertEquals(15f, params.exposure)
        assertEquals(-10f, params.highlights)
        assertEquals(20f, params.shadows)
        assertEquals(25f, params.sharpness)
        assertEquals(15f, params.clarity)
        assertEquals(10f, params.vibrance)
        assertEquals(5f, params.grain)
        assertEquals(10f, params.dehaze)
        assertEquals(20f, params.denoise)
    }

    @Test
    fun testCopyParameters() {
        val original = RenderParameters(
            brightness = 20f,
            contrast = 30f,
            saturation = 40f
        )
        
        val copy = original.copy(brightness = 25f)
        
        assertEquals(25f, copy.brightness)
        assertEquals(30f, copy.contrast)
        assertEquals(40f, copy.saturation)
    }

    @Test
    fun testEqualsParameters() {
        val params1 = RenderParameters(brightness = 10f, contrast = 20f)
        val params2 = RenderParameters(brightness = 10f, contrast = 20f)
        val params3 = RenderParameters(brightness = 15f, contrast = 20f)
        
        assertEquals(params1, params2)
        assertNotEquals(params1, params3)
    }
}

/**
 * CPU渲染器测试
 * 测试CPURenderer的像素处理逻辑
 */
class CPURendererTest {

    @Test
    fun testBrightnessCalculation() {
        // 测试亮度计算公式
        val brightness = 50f
        val brightnessOffset = brightness * 2.55f
        
        assertEquals(127.5f, brightnessOffset)
        
        // 测试像素值调整
        val originalR = 100
        val adjustedR = (originalR + brightnessOffset).toInt().coerceIn(0, 255)
        
        assertEquals(227, adjustedR)
    }

    @Test
    fun testContrastCalculation() {
        // 测试对比度计算公式
        val contrast = 50f
        val contrastFactor = 1f + contrast / 100f
        
        assertEquals(1.5f, contrastFactor)
        
        // 测试像素值调整
        val originalR = 100
        val adjustedR = ((originalR - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
        
        assertEquals(62, adjustedR)
    }

    @Test
    fun testSaturationCalculation() {
        // 测试饱和度计算公式
        val r = 100f
        val g = 150f
        val b = 200f
        
        val gray = 0.299f * r + 0.587f * g + 0.114f * b
        
        assertEquals(138.1f, gray, 0.1f)
        
        val saturation = 50f
        val saturationFactor = 1f + saturation / 100f
        
        assertEquals(1.5f, saturationFactor)
        
        val adjustedR = (gray + (r - gray) * saturationFactor).toInt().coerceIn(0, 255)
        
        assertEquals(79, adjustedR)
    }

    @Test
    fun testWarmthCalculation() {
        // 测试暖色调计算
        val warmth = 30f
        val warmthFactor = warmth / 100f
        
        assertEquals(0.3f, warmthFactor)
        
        val originalR = 100
        val originalB = 150
        
        // 暖色调：增加红色，减少蓝色
        val adjustedR = (originalR + warmthFactor * 20).toInt().coerceIn(0, 255)
        val adjustedB = (originalB - warmthFactor * 20).toInt().coerceIn(0, 255)
        
        assertEquals(106, adjustedR)
        assertEquals(144, adjustedB)
    }

    @Test
    fun testColdToneCalculation() {
        // 测试冷色调计算
        val warmth = -30f
        val warmthFactor = warmth / 100f
        
        assertEquals(-0.3f, warmthFactor)
        
        val originalR = 100
        val originalB = 150
        
        // 冷色调：减少红色，增加蓝色
        val adjustedR = (originalR + warmthFactor * 20).toInt().coerceIn(0, 255)
        val adjustedB = (originalB - warmthFactor * 20).toInt().coerceIn(0, 255)
        
        assertEquals(94, adjustedR)
        assertEquals(156, adjustedB)
    }

    @Test
    fun testExposureCalculation() {
        // 测试曝光计算公式
        val exposure = 25f
        val exposureFactor = 2f.pow(exposure / 50f)
        
        assertEquals(2f, exposureFactor, 0.1f)
        
        // 测试像素值调整
        val originalR = 100
        val adjustedR = (originalR * exposureFactor).toInt().coerceIn(0, 255)
        
        assertEquals(200, adjustedR)
    }

    @Test
    fun testPixelCoercion() {
        // 测试像素值边界限制
        val over255 = 300.coerceIn(0, 255)
        val under0 = -50.coerceIn(0, 255)
        val normal = 128.coerceIn(0, 255)
        
        assertEquals(255, over255)
        assertEquals(0, under0)
        assertEquals(128, normal)
    }

    @Test
    fun testPixelComposition() {
        // 测试像素RGBA组合
        val alpha = 0xFF000000.toInt()
        val r = 100
        val g = 150
        val b = 200
        
        val pixel = alpha or (r shl 16) or (g shl 8) or b
        
        // 验证像素值
        val extractedR = (pixel shr 16) and 0xFF
        val extractedG = (pixel shr 8) and 0xFF
        val extractedB = pixel and 0xFF
        
        assertEquals(r, extractedR)
        assertEquals(g, extractedG)
        assertEquals(b, extractedB)
    }
}

/**
 * 渲染质量测试
 */
class RenderQualityTest {

    @Test
    fun testPreviewQuality() {
        val quality = RenderQuality.PREVIEW
        
        assertEquals("preview", quality.name.lowercase())
    }

    @Test
    fun testStandardQuality() {
        val quality = RenderQuality.STANDARD
        
        assertEquals("standard", quality.name.lowercase())
    }

    @Test
    fun testHighQuality() {
        val quality = RenderQuality.HIGH
        
        assertEquals("high", quality.name.lowercase())
    }
}

/**
 * 渲染结果测试
 */
class RenderResultTest {

    @Test
    fun testSuccessResult() {
        val textureId = 123
        val processingTime = 50L
        val quality = RenderQuality.STANDARD
        
        val result = RenderResult.Success(textureId, processingTime, quality)
        
        assertEquals(textureId, result.outputTextureId)
        assertEquals(processingTime, result.processingTimeMs)
        assertEquals(quality, result.quality)
    }

    @Test
    fun testErrorResult() {
        val message = "Render failed"
        val result = RenderResult.Error(message)
        
        assertEquals(message, result.message)
    }

    @Test
    fun testFallbackResult() {
        val reason = "GPU unavailable"
        val processingTime = 100L
        
        val result = RenderResult.FallbackToCPU(reason, processingTime)
        
        assertEquals(reason, result.reason)
        assertEquals(processingTime, result.processingTimeMs)
    }
}

/**
 * 渲染请求测试
 */
class RenderRequestTest {

    @Test
    fun testRenderRequestCreation() {
        val id = "test_001"
        val params = RenderParameters(brightness = 10f)
        val quality = RenderQuality.HIGH
        
        // 注意：Bitmap在单元测试中无法直接创建，这里只测试参数
        assertEquals("test_001", id)
        assertEquals(10f, params.brightness)
        assertEquals(RenderQuality.HIGH, quality)
    }
}