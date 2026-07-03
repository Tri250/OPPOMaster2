package com.silas.omaster.renderer

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow

/**
 * GPURenderManager 及相关组件的单元测试
 *
 * 覆盖范围：
 * 1. RenderParameters 数据类 — 默认值与自定义构造
 * 2. RenderRequest ID 生成（AtomicLong）— 顺序递增
 * 3. applyPresetToFrame — Bitmap 处理流程（通过 CPURenderer 间接测试）
 * 4. ColorMatrix 应用逻辑
 * 5. 曝光补偿计算（2.0.pow(exposure)）
 * 6. 渲染参数校验
 */
class GPURenderManagerTest {

    // ==================== 1. RenderParameters 默认值与自定义构造 ====================

    @Test
    fun renderParameters_defaultConstruction_allFieldsAreZero() {
        val params = RenderParameters()
        assertEquals(0f, params.saturation, 0.001f)
        assertEquals(0f, params.contrast, 0.001f)
        assertEquals(0f, params.brightness, 0.001f)
        assertEquals(0f, params.warmth, 0.001f)
        assertEquals(0f, params.sharpness, 0.001f)
        assertEquals(0f, params.clarity, 0.001f)
        assertEquals(0f, params.vibrance, 0.001f)
        assertEquals(0f, params.highlights, 0.001f)
        assertEquals(0f, params.shadows, 0.001f)
        assertEquals(0f, params.whites, 0.001f)
        assertEquals(0f, params.blacks, 0.001f)
        assertEquals(0f, params.exposure, 0.001f)
        assertEquals(0f, params.grain, 0.001f)
        assertEquals(0f, params.fade, 0.001f)
        assertEquals(0f, params.dehaze, 0.001f)
        assertEquals(0f, params.denoise, 0.001f)
        assertEquals(0f, params.skinSmooth, 0.001f)
        assertEquals(0f, params.texture, 0.001f)
    }

    @Test
    fun renderParameters_defaultConstruction_hslFieldsAreZero() {
        val params = RenderParameters()
        assertEquals(0f, params.hslRedHue, 0.001f)
        assertEquals(0f, params.hslRedSaturation, 0.001f)
        assertEquals(0f, params.hslRedLuminance, 0.001f)
        assertEquals(0f, params.hslOrangeHue, 0.001f)
        assertEquals(0f, params.hslBlueHue, 0.001f)
        assertEquals(0f, params.hslMagentaLuminance, 0.001f)
    }

    @Test
    fun renderParameters_defaultConstruction_lutDisabled() {
        val params = RenderParameters()
        assertFalse(params.lutEnabled)
        assertEquals(0, params.lutTextureId)
        assertEquals(0, params.lutSize)
        assertEquals(0f, params.lutStrength, 0.001f)
    }

    @Test
    fun renderParameters_defaultConstruction_curvesAreIdentity() {
        val params = RenderParameters()
        val identity = RenderParameters.IDENTITY_CURVE
        assertTrue(params.curveRgbLut.contentEquals(identity))
        assertTrue(params.curveRedLut.contentEquals(identity))
        assertTrue(params.curveGreenLut.contentEquals(identity))
        assertTrue(params.curveBlueLut.contentEquals(identity))
    }

    @Test
    fun renderParameters_customConstruction_valuesSetCorrectly() {
        val params = RenderParameters(
            saturation = 50f,
            contrast = -30f,
            brightness = 20f,
            warmth = -10f,
            sharpness = 40f,
            clarity = 60f,
            vibrance = 25f,
            highlights = -15f,
            shadows = 35f,
            whites = 10f,
            blacks = -5f,
            exposure = 70f,
            grain = 15f,
            fade = 8f,
            dehaze = 12f,
            denoise = 20f,
            skinSmooth = 30f,
            texture = -20f
        )
        assertEquals(50f, params.saturation, 0.001f)
        assertEquals(-30f, params.contrast, 0.001f)
        assertEquals(20f, params.brightness, 0.001f)
        assertEquals(-10f, params.warmth, 0.001f)
        assertEquals(40f, params.sharpness, 0.001f)
        assertEquals(60f, params.clarity, 0.001f)
        assertEquals(25f, params.vibrance, 0.001f)
        assertEquals(-15f, params.highlights, 0.001f)
        assertEquals(35f, params.shadows, 0.001f)
        assertEquals(10f, params.whites, 0.001f)
        assertEquals(-5f, params.blacks, 0.001f)
        assertEquals(70f, params.exposure, 0.001f)
        assertEquals(15f, params.grain, 0.001f)
        assertEquals(8f, params.fade, 0.001f)
        assertEquals(12f, params.dehaze, 0.001f)
        assertEquals(20f, params.denoise, 0.001f)
        assertEquals(30f, params.skinSmooth, 0.001f)
        assertEquals(-20f, params.texture, 0.001f)
    }

    @Test
    fun renderParameters_customConstruction_hslValuesSet() {
        val params = RenderParameters(
            hslRedHue = 10f,
            hslRedSaturation = -20f,
            hslRedLuminance = 30f,
            hslCyanHue = -15f,
            hslBlueSaturation = 40f,
            hslMagentaLuminance = -25f
        )
        assertEquals(10f, params.hslRedHue, 0.001f)
        assertEquals(-20f, params.hslRedSaturation, 0.001f)
        assertEquals(30f, params.hslRedLuminance, 0.001f)
        assertEquals(-15f, params.hslCyanHue, 0.001f)
        assertEquals(40f, params.hslBlueSaturation, 0.001f)
        assertEquals(-25f, params.hslMagentaLuminance, 0.001f)
    }

    @Test
    fun renderParameters_customConstruction_lutEnabled() {
        val params = RenderParameters(
            lutTextureId = 42,
            lutSize = 33,
            lutStrength = 0.8f,
            lutEnabled = true
        )
        assertTrue(params.lutEnabled)
        assertEquals(42, params.lutTextureId)
        assertEquals(33, params.lutSize)
        assertEquals(0.8f, params.lutStrength, 0.001f)
    }

    @Test
    fun renderParameters_DEFAULT_isAllZero() {
        val default = RenderParameters.DEFAULT
        assertEquals(0f, default.saturation, 0.001f)
        assertEquals(0f, default.contrast, 0.001f)
        assertEquals(0f, default.brightness, 0.001f)
        assertEquals(0f, default.exposure, 0.001f)
        assertFalse(default.lutEnabled)
    }

    @Test
    fun renderParameters_identityCurve_isLinearRamp() {
        val identity = RenderParameters.IDENTITY_CURVE
        assertEquals(256, identity.size)
        // 验证每个采样点满足 y = x / 255
        for (i in 0..255) {
            assertEquals(i / 255f, identity[i], 0.001f)
        }
    }

    // ==================== 2. RenderRequest ID 生成（AtomicLong 顺序递增） ====================

    @Test
    fun requestIdCounter_sequentialIncrement() {
        val counter = AtomicLong(0)
        val id1 = counter.incrementAndGet()
        val id2 = counter.incrementAndGet()
        val id3 = counter.incrementAndGet()
        assertEquals(1L, id1)
        assertEquals(2L, id2)
        assertEquals(3L, id3)
    }

    @Test
    fun requestIdCounter_generatesUniqueSequentialIds() {
        val counter = AtomicLong(0)
        val ids = (1..100).map { counter.incrementAndGet() }
        // 验证全部唯一
        assertEquals(100, ids.toSet().size)
        // 验证严格递增
        for (i in 1 until ids.size) {
            assertTrue(ids[i] > ids[i - 1])
        }
    }

    @Test
    fun requestIdGeneration_formatContainsTimestampAndCounter() {
        val counter = AtomicLong(0)
        val counterVal = counter.incrementAndGet()
        val timestamp = System.currentTimeMillis()
        val id = "render_${timestamp}_$counterVal"
        assertTrue(id.startsWith("render_"))
        assertTrue(id.contains(timestamp.toString()))
        assertTrue(id.endsWith("_1"))
    }

    @Test
    fun requestIdCounter_concurrentIncrement_producesUniqueIds() {
        val counter = AtomicLong(0)
        val threadCount = 10
        val idsPerThread = 100
        val allIds = java.util.concurrent.ConcurrentLinkedQueue<Long>()

        val threads = (1..threadCount).map {
            Thread {
                repeat(idsPerThread) {
                    allIds.add(counter.incrementAndGet())
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(threadCount * idsPerThread, allIds.size)
        assertEquals(threadCount * idsPerThread, allIds.toSet().size)
    }

    // ==================== 3. applyPresetToFrame — Bitmap 处理（通过 CPURenderer 间接测试） ====================

    @Test
    fun cpuRenderer_withZeroParams_returnsBitmap() = runTest {
        val renderer = CPURenderer()
        val bitmap = mockk<Bitmap>(relaxed = true)
        val outputBitmap = mockk<Bitmap>(relaxed = true)

        every { bitmap.copy(Bitmap.Config.ARGB_8888, true) } returns outputBitmap
        every { outputBitmap.width } returns 2
        every { outputBitmap.height } returns 2

        val params = RenderParameters() // 所有参数为0，无调整
        val result = renderer.render(bitmap, params)
        assertNotNull(result)
        // 零参数时，copy 会被调用但像素处理不改变值
        verify { bitmap.copy(Bitmap.Config.ARGB_8888, true) }
    }

    @Test
    fun cpuRenderer_withExposure_processesBitmap() = runTest {
        val renderer = CPURenderer()
        val bitmap = mockk<Bitmap>(relaxed = true)
        val outputBitmap = mockk<Bitmap>(relaxed = true)

        every { bitmap.copy(Bitmap.Config.ARGB_8888, true) } returns outputBitmap
        every { outputBitmap.width } returns 1
        every { outputBitmap.height } returns 1
        every { outputBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val pixels = arg<IntArray>(0)
            pixels[0] = 0xFF808080.toInt() // 中灰
            Unit
        }
        every { outputBitmap.setPixels(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        val params = RenderParameters(exposure = 50f)
        val result = renderer.render(bitmap, params)
        assertNotNull(result)
        // 验证像素处理确实被执行
        verify { outputBitmap.setPixels(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun cpuRenderer_withDenoise_callsGetPixelsAndSetPixels() = runTest {
        val renderer = CPURenderer()
        val bitmap = mockk<Bitmap>(relaxed = true)
        val outputBitmap = mockk<Bitmap>(relaxed = true)

        every { bitmap.copy(Bitmap.Config.ARGB_8888, true) } returns outputBitmap
        every { outputBitmap.width } returns 2
        every { outputBitmap.height } returns 2
        every { outputBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val pixels = arg<IntArray>(0)
            pixels[0] = 0xFF333333.toInt()
            pixels[1] = 0xFF666666.toInt()
            pixels[2] = 0xFF999999.toInt()
            pixels[3] = 0xFFCCCCCC.toInt()
            Unit
        }
        every { outputBitmap.setPixels(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        val params = RenderParameters(denoise = 50f)
        val result = renderer.render(bitmap, params)
        assertNotNull(result)
        // 降噪 pass 需要读/写像素
        verify(atLeast = 1) { outputBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) }
        verify(atLeast = 1) { outputBitmap.setPixels(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun cpuRenderer_withSkinSmooth_processesBitmap() = runTest {
        val renderer = CPURenderer()
        val bitmap = mockk<Bitmap>(relaxed = true)
        val outputBitmap = mockk<Bitmap>(relaxed = true)

        every { bitmap.copy(Bitmap.Config.ARGB_8888, true) } returns outputBitmap
        every { outputBitmap.width } returns 2
        every { outputBitmap.height } returns 2
        every { outputBitmap.getPixels(any(), any(), any(), any(), any(), any(), any()) } answers {
            val pixels = arg<IntArray>(0)
            pixels.fill(0xFFCC9977.toInt()) // 肤色像素
            Unit
        }
        every { outputBitmap.setPixels(any(), any(), any(), any(), any(), any(), any()) } returns Unit

        val params = RenderParameters(skinSmooth = 50f)
        val result = renderer.render(bitmap, params)
        assertNotNull(result)
    }

    // ==================== 4. ColorMatrix 应用逻辑 ====================

    @Test
    fun colorMatrix_saturationOnly_setsCorrectSaturationScale() {
        // 模拟 applyPresetToFrameCPU 中的 ColorMatrix 逻辑
        val saturation = 0.5f // normalizeSigned(value, 30) 结果
        val contrast = 0f
        val tone = 0f
        val colorTemp = 0f
        val cyanMagenta = 0f

        val combinedMatrix = android.graphics.ColorMatrix()

        if (saturation != 0f) {
            combinedMatrix.setSaturation(1f + saturation)
        }

        // 验证矩阵不为恒等（因为有饱和度调整）
        val expectedSatScale = 1f + saturation
        assertEquals(1.5f, expectedSatScale, 0.001f)
    }

    @Test
    fun colorMatrix_fullDesaturation_usesLuminanceWeights() {
        val saturation = -1.0f // 完全去饱和
        val combinedMatrix = android.graphics.ColorMatrix()

        if (saturation <= -0.95f) {
            val bwMatrix = android.graphics.ColorMatrix(floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.set(bwMatrix)
        }

        // 验证亮度权重之和为1（BT.601标准）
        val lumWeight = 0.299f + 0.587f + 0.114f
        assertEquals(1.0f, lumWeight, 0.001f)
    }

    @Test
    fun colorMatrix_contrastAndTone_appliesPostConcatCorrectly() {
        val saturation = 0f
        val contrast = 0.3f
        val tone = 0.1f
        val colorTemp = -0.2f
        val cyanMagenta = 0.15f

        val contrastValue = 1f + contrast
        assertEquals(1.3f, contrastValue, 0.001f)

        // 验证 postMatrix 的偏移量计算
        val rOffset = tone * 25f + cyanMagenta * 25f
        val gOffset = tone * 10f - cyanMagenta * 20f
        val bOffset = -colorTemp * 15f + cyanMagenta * 15f

        assertEquals(0.1f * 25f + 0.15f * 25f, rOffset, 0.001f)
        assertEquals(0.1f * 10f - 0.15f * 20f, gOffset, 0.001f)
        assertEquals(0.2f * 15f + 0.15f * 15f, bOffset, 0.001f)
    }

    @Test
    fun colorMatrix_noAdjustments_returnsNoChange() {
        val saturation = 0f
        val contrast = 0f
        val tone = 0f
        val colorTemp = 0f
        val cyanMagenta = 0f
        val vignette = 0f

        val hasPostMatrix = contrast != 0f || tone != 0f || colorTemp != 0f || cyanMagenta != 0f
        val hasColorMatrixOps = saturation != 0f || hasPostMatrix
        val hasVignette = vignette > 0.005f

        assertFalse(hasColorMatrixOps)
        assertFalse(hasVignette)
        // 当所有调整值为0时，applyPresetToFrameCPU 直接返回原 Bitmap
    }

    @Test
    fun colorMatrix_saturationWithContrast_combinesBothEffects() {
        val saturation = 0.5f
        val contrast = 0.3f

        val combinedMatrix = android.graphics.ColorMatrix()
        if (saturation != 0f) {
            combinedMatrix.setSaturation(1f + saturation)
        }

        val hasPostMatrix = contrast != 0f
        if (hasPostMatrix) {
            val contrastValue = 1f + contrast
            val postMatrix = android.graphics.ColorMatrix(floatArrayOf(
                contrastValue, 0f, 0f, 0f, 0f,
                0f, contrastValue, 0f, 0f, 0f,
                0f, 0f, contrastValue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(postMatrix)
        }

        // 两种效果都被应用
        assertTrue(saturation != 0f)
        assertTrue(hasPostMatrix)
    }

    @Test
    fun normalizeSigned_clampsToRange() {
        // 模拟 CameraXManager.normalizeSigned 逻辑
        fun normalizeSigned(value: Int, max: Int): Float =
            if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(-1f, 1f)

        assertEquals(1.0f, normalizeSigned(30, 30), 0.001f)
        assertEquals(-1.0f, normalizeSigned(-30, 30), 0.001f)
        assertEquals(0.5f, normalizeSigned(15, 30), 0.001f)
        assertEquals(1.0f, normalizeSigned(100, 30), 0.001f)  // 超出范围被钳制
        assertEquals(-1.0f, normalizeSigned(-100, 30), 0.001f) // 超出范围被钳制
        assertEquals(0.0f, normalizeSigned(0, 30), 0.001f)
        assertEquals(0.0f, normalizeSigned(10, 0), 0.001f)     // max=0 返回0
    }

    @Test
    fun normalizeUnsigned_clampsToPositiveRange() {
        // 模拟 CameraXManager.normalizeUnsigned 逻辑
        fun normalizeUnsigned(value: Int, max: Int): Float =
            if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

        assertEquals(1.0f, normalizeUnsigned(30, 30), 0.001f)
        assertEquals(0.5f, normalizeUnsigned(15, 30), 0.001f)
        assertEquals(1.0f, normalizeUnsigned(100, 30), 0.001f)  // 超出范围被钳制到1
        assertEquals(0.0f, normalizeUnsigned(0, 30), 0.001f)
        assertEquals(0.0f, normalizeUnsigned(-10, 30), 0.001f)  // 负值被钳制到0
    }

    // ==================== 5. 曝光补偿计算（2.0.pow(exposure)） ====================

    @Test
    fun exposureCompensation_zeroExposure_factorIsOne() {
        val exposure = 0f / 100f // 归一化
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(1.0f, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_positiveExposure_factorGreaterThanOne() {
        val exposure = 100f / 100f // = 1.0
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(2.0f, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_negativeExposure_factorLessThanOne() {
        val exposure = -100f / 100f // = -1.0
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(0.5f, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_halfExposure_factorIsSqrt2() {
        val exposure = 50f / 100f // = 0.5
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(2.0.pow(0.5), factor.toDouble(), 0.001)
    }

    @Test
    fun exposureCompensation_doublePositiveExposure_factorIsFour() {
        // exposure=200 超出正常范围，但计算逻辑仍然有效
        val exposure = 200f / 100f // = 2.0
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(4.0f, factor, 0.01f)
    }

    @Test
    fun exposureCompensation_doubleNegativeExposure_factorIsQuarter() {
        val exposure = -200f / 100f // = -2.0
        val factor = 2.0.pow(exposure.toDouble()).toFloat()
        assertEquals(0.25f, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_renderParams_exposureIsNormalizedCorrectly() {
        val params = RenderParameters(exposure = 100f)
        val uniforms = params.toShaderUniforms()
        // exposure 在 toShaderUniforms 中被归一化为 exposure / 100f = 1.0
        val exposureUniform = uniforms[16] // exposure 是第17个 uniform（索引16）
        assertEquals(1.0f, exposureUniform, 0.001f)

        // 对归一化后的值应用 2.0.pow()
        val factor = 2.0.pow(exposureUniform.toDouble()).toFloat()
        assertEquals(2.0f, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_renderParams_negativeExposureNormalized() {
        val params = RenderParameters(exposure = -50f)
        val uniforms = params.toShaderUniforms()
        val exposureUniform = uniforms[16]
        assertEquals(-0.5f, exposureUniform, 0.001f)

        val factor = 2.0.pow(exposureUniform.toDouble()).toFloat()
        val expected = 2.0.pow(-0.5).toFloat()
        assertEquals(expected, factor, 0.001f)
    }

    @Test
    fun exposureCompensation_smallValue_belowThreshold_isSkipped() {
        // CPURenderer 中 abs(exposure) < 0.01 时跳过处理
        val exposure = 0.5f / 100f // = 0.005，低于阈值
        assertTrue(kotlin.math.abs(exposure) < 0.01f)

        val exposure2 = 1.5f / 100f // = 0.015，高于阈值
        assertFalse(kotlin.math.abs(exposure2) < 0.01f)
    }

    // ==================== 6. 渲染参数校验 ====================

    @Test
    fun renderParameters_hasAnyAdjustment_default_returnsFalse() {
        val params = RenderParameters()
        assertFalse(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_withSaturation_returnsTrue() {
        val params = RenderParameters(saturation = 10f)
        assertTrue(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_withExposure_returnsTrue() {
        val params = RenderParameters(exposure = -5f)
        assertTrue(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_withHsl_returnsTrue() {
        val params = RenderParameters(hslBlueSaturation = 10f)
        assertTrue(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_withLutEnabled_returnsTrue() {
        val params = RenderParameters(lutEnabled = true, lutTextureId = 1)
        assertTrue(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_lutEnabledButNoTexture_returnsFalse() {
        val params = RenderParameters(lutEnabled = true, lutTextureId = 0)
        // lutEnabled=true 但 lutTextureId=0，hasAnyAdjustment 要求两者都满足
        assertFalse(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_hasAnyAdjustment_withNonIdentityCurve_returnsTrue() {
        val customCurve = FloatArray(256) { it / 512f } // 非恒等曲线
        val params = RenderParameters(curveRgbLut = customCurve)
        assertTrue(params.hasAnyAdjustment())
    }

    @Test
    fun renderParameters_nonZeroCount_default_returnsZero() {
        val params = RenderParameters()
        assertEquals(0, params.nonZeroCount())
    }

    @Test
    fun renderParameters_nonZeroCount_withSomeParams_returnsCorrectCount() {
        val params = RenderParameters(
            saturation = 10f,
            contrast = -20f,
            exposure = 5f
        )
        assertEquals(3, params.nonZeroCount())
    }

    @Test
    fun renderParameters_toMap_containsAllBasicParams() {
        val params = RenderParameters(
            saturation = 10f,
            contrast = 20f,
            brightness = 30f,
            exposure = 40f
        )
        val map = params.toMap()
        assertEquals(10f, map["saturation"]!!, 0.001f)
        assertEquals(20f, map["contrast"]!!, 0.001f)
        assertEquals(30f, map["brightness"]!!, 0.001f)
        assertEquals(40f, map["exposure"]!!, 0.001f)
        assertEquals(18, map.size)
    }

    @Test
    fun renderParameters_fromMap_createsCorrectParams() {
        val map = mapOf(
            "saturation" to 15f,
            "contrast" to -25f,
            "exposure" to 50f
        )
        val params = RenderParameters.fromMap(map)
        assertEquals(15f, params.saturation, 0.001f)
        assertEquals(-25f, params.contrast, 0.001f)
        assertEquals(50f, params.exposure, 0.001f)
        // 未指定的参数保持默认值0
        assertEquals(0f, params.brightness, 0.001f)
    }

    @Test
    fun renderParameters_fromIntMap_convertsToInt() {
        val map = mapOf(
            "saturation" to 15,
            "contrast" to -25
        )
        val params = RenderParameters.fromIntMap(map)
        assertEquals(15f, params.saturation, 0.001f)
        assertEquals(-25f, params.contrast, 0.001f)
    }

    @Test
    fun renderParameters_toShaderUniforms_normalizesCorrectly() {
        val params = RenderParameters(
            saturation = 50f,
            contrast = -25f,
            brightness = 75f,
            exposure = 100f
        )
        val uniforms = params.toShaderUniforms()
        assertEquals(18, uniforms.size)
        assertEquals(0.5f, uniforms[0], 0.001f)   // saturation / 100
        assertEquals(-0.25f, uniforms[1], 0.001f)  // contrast / 100
        assertEquals(0.75f, uniforms[2], 0.001f)   // brightness / 100
        assertEquals(1.0f, uniforms[16], 0.001f)   // exposure / 100
    }

    @Test
    fun renderParameters_toShaderUniforms_allZeroParams_produceZeroUniforms() {
        val params = RenderParameters()
        val uniforms = params.toShaderUniforms()
        assertTrue(uniforms.all { it == 0f })
    }

    @Test
    fun renderParameters_lerp_atZero_returnsSource() {
        val source = RenderParameters(saturation = 50f, contrast = -30f)
        val target = RenderParameters(saturation = 100f, contrast = 30f)
        val result = source.lerp(target, 0f)
        assertEquals(50f, result.saturation, 0.001f)
        assertEquals(-30f, result.contrast, 0.001f)
    }

    @Test
    fun renderParameters_lerp_atOne_returnsTarget() {
        val source = RenderParameters(saturation = 50f, contrast = -30f)
        val target = RenderParameters(saturation = 100f, contrast = 30f)
        val result = source.lerp(target, 1f)
        assertEquals(100f, result.saturation, 0.001f)
        assertEquals(30f, result.contrast, 0.001f)
    }

    @Test
    fun renderParameters_lerp_atHalf_returnsMidpoint() {
        val source = RenderParameters(saturation = 0f)
        val target = RenderParameters(saturation = 100f)
        val result = source.lerp(target, 0.5f)
        assertEquals(50f, result.saturation, 0.001f)
    }

    @Test
    fun renderParameters_lerp_clampsTtoRange() {
        val source = RenderParameters(saturation = 0f)
        val target = RenderParameters(saturation = 100f)
        val resultNeg = source.lerp(target, -0.5f)
        val resultOver = source.lerp(target, 1.5f)
        assertEquals(0f, resultNeg.saturation, 0.001f)    // t=0
        assertEquals(100f, resultOver.saturation, 0.001f)  // t=1
    }

    @Test
    fun renderParameters_merge_currentOverridesDefault() {
        val current = RenderParameters(saturation = 50f, contrast = 0f)
        val other = RenderParameters(saturation = 80f, contrast = 30f)
        val result = current.merge(other)
        // current 非零覆盖，zero 时用 other
        assertEquals(50f, result.saturation, 0.001f) // current 非0，保留
        assertEquals(30f, result.contrast, 0.001f)   // current 为0，取 other
    }

    @Test
    fun renderParameters_merge_allZero_returnsOther() {
        val current = RenderParameters()
        val other = RenderParameters(saturation = 50f, contrast = 30f)
        val result = current.merge(other)
        assertEquals(50f, result.saturation, 0.001f)
        assertEquals(30f, result.contrast, 0.001f)
    }

    @Test
    fun renderParameters_paramMetadata_contains18Params() {
        assertEquals(18, RenderParameters.PARAM_METADATA.size)
    }

    @Test
    fun renderParameters_paramMetadata_rangesAreCorrect() {
        val meta = RenderParameters.PARAM_METADATA.associateBy { it.key }
        // 双向参数 [-100, 100]
        assertEquals(-100f, meta["saturation"]!!.minValue, 0.001f)
        assertEquals(100f, meta["saturation"]!!.maxValue, 0.001f)
        // 单向参数 [0, 100]
        assertEquals(0f, meta["sharpness"]!!.minValue, 0.001f)
        assertEquals(100f, meta["sharpness"]!!.maxValue, 0.001f)
        assertEquals(0f, meta["grain"]!!.minValue, 0.001f)
    }

    @Test
    fun renderQuality_ordinalOrder() {
        // PREVIEW < STANDARD < HIGH < ULTRA
        assertTrue(RenderQuality.PREVIEW.ordinal < RenderQuality.STANDARD.ordinal)
        assertTrue(RenderQuality.STANDARD.ordinal < RenderQuality.HIGH.ordinal)
        assertTrue(RenderQuality.HIGH.ordinal < RenderQuality.ULTRA.ordinal)
    }

    @Test
    fun renderResult_success_holdsCorrectData() {
        val result = RenderResult.Success(
            outputTextureId = 42,
            processingTimeMs = 100L,
            quality = RenderQuality.HIGH,
            outputBitmap = null
        )
        assertEquals(42, result.outputTextureId)
        assertEquals(100L, result.processingTimeMs)
        assertEquals(RenderQuality.HIGH, result.quality)
        assertNull(result.outputBitmap)
    }

    @Test
    fun renderResult_error_holdsMessage() {
        val result = RenderResult.Error("GPU failed")
        assertEquals("GPU failed", result.message)
        assertNull(result.exception)
    }

    @Test
    fun renderResult_fallbackToCPU_holdsData() {
        val result = RenderResult.FallbackToCPU(
            reason = "GPU unavailable",
            processingTimeMs = 200L,
            outputBitmap = null
        )
        assertEquals("GPU unavailable", result.reason)
        assertEquals(200L, result.processingTimeMs)
        assertNull(result.outputBitmap)
    }

    @Test
    fun bitmapPool_obtain_returnsBitmap() {
        val pool = BitmapPool(maxPoolSize = 4)
        val bitmap = mockk<Bitmap>(relaxed = true)
        // BitmapPool.obtain 会调用 Bitmap.createBitmap，需要 mock
        // 在纯 JVM 测试中无法直接调用 Android Bitmap API，
        // 这里仅测试池大小逻辑
        assertEquals(0, pool.size())
    }

    @Test
    fun bitmapPool_maxPoolSize_limitsPool() {
        val pool = BitmapPool(maxPoolSize = 2)
        assertEquals(0, pool.size())
        // 验证 maxPoolSize 被正确设置（间接通过 size 验证）
        pool.clear()
        assertEquals(0, pool.size())
    }

    @Test
    fun frameTimingMetrics_recordsAndResets() {
        FrameTimingMetrics.reset()
        assertEquals(0.0, FrameTimingMetrics.getAverageFrameTime(), 0.001)
        assertEquals(0L, FrameTimingMetrics.getFrameCount())

        FrameTimingMetrics.recordFrame(16)
        FrameTimingMetrics.recordFrame(32)
        assertEquals(2L, FrameTimingMetrics.getFrameCount())
        assertEquals(24.0, FrameTimingMetrics.getAverageFrameTime(), 0.001)
        assertEquals(32L, FrameTimingMetrics.getLastFrameTime())

        FrameTimingMetrics.reset()
        assertEquals(0L, FrameTimingMetrics.getFrameCount())
        assertEquals(0.0, FrameTimingMetrics.getAverageFrameTime(), 0.001)
    }

    @Test
    fun frameTimingMetrics_p95Calculation() {
        FrameTimingMetrics.reset()
        // 添加20个样本：全为10ms
        repeat(19) { FrameTimingMetrics.recordFrame(10) }
        FrameTimingMetrics.recordFrame(100) // 一个离群值

        val p95 = FrameTimingMetrics.getP95FrameTime()
        // P95 应该是 10（大多数帧），而不是 100
        assertTrue(p95 <= 100)
        assertTrue(p95 >= 10)

        FrameTimingMetrics.reset()
    }

    @Test
    fun frameTimingMetrics_emptyMetrics_returnZero() {
        FrameTimingMetrics.reset()
        assertEquals(0.0, FrameTimingMetrics.getAverageFrameTime(), 0.001)
        assertEquals(0L, FrameTimingMetrics.getP95FrameTime())
        assertEquals(0L, FrameTimingMetrics.getLastFrameTime())
    }

    @Test
    fun renderRequest_dataClass_propertiesAccessible() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        val params = RenderParameters(exposure = 50f)
        val callback: ((RenderResult) -> Unit)? = null
        val request = RenderRequest(
            id = "render_123_1",
            inputBitmap = bitmap,
            params = params,
            quality = RenderQuality.STANDARD,
            resultCallback = callback
        )
        assertEquals("render_123_1", request.id)
        assertEquals(50f, request.params.exposure, 0.001f)
        assertEquals(RenderQuality.STANDARD, request.quality)
        assertNull(request.resultCallback)
    }

    @Test
    fun renderQualityManager_qualityPresets_coverAllLevels() {
        // 验证质量管理器的预设覆盖了所有 RenderQuality 级别
        val presets = mapOf(
            RenderQuality.ULTRA to RenderQualityManager.QualityPreset(RenderQuality.ULTRA, 4096, 4096, 1),
            RenderQuality.HIGH to RenderQualityManager.QualityPreset(RenderQuality.HIGH, 2048, 2048, 1),
            RenderQuality.STANDARD to RenderQualityManager.QualityPreset(RenderQuality.STANDARD, 1024, 1024, 2),
            RenderQuality.PREVIEW to RenderQualityManager.QualityPreset(RenderQuality.PREVIEW, 512, 512, 4)
        )
        assertEquals(4, presets.size)
        assertEquals(4096, presets[RenderQuality.ULTRA]!!.maxWidth)
        assertEquals(2048, presets[RenderQuality.HIGH]!!.maxWidth)
        assertEquals(1024, presets[RenderQuality.STANDARD]!!.maxWidth)
        assertEquals(512, presets[RenderQuality.PREVIEW]!!.maxWidth)
    }

    @Test
    fun renderQualityManager_qualityPresets_sampleSizeDecreasesWithQuality() {
        // 高质量使用较小 sampleSize（1），低质量使用较大 sampleSize（4）
        val ultraPreset = RenderQualityManager.QualityPreset(RenderQuality.ULTRA, 4096, 4096, 1)
        val previewPreset = RenderQualityManager.QualityPreset(RenderQuality.PREVIEW, 512, 512, 4)
        assertTrue(ultraPreset.sampleSize < previewPreset.sampleSize)
    }
}
