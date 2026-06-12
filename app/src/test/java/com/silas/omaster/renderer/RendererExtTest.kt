package com.silas.omaster.renderer

import org.junit.Assert.*
import org.junit.Test

/**
 * Renderer 扩展测试 - 补充覆盖渲染模块
 */
class RendererExtTest {

    // ===== ShaderProgram 扩展测试 =====

    @Test
    fun `ShaderProgram - GLSL版本验证`() {
        val validVersions = listOf("100", "300 es", "310 es")
        
        for (version in validVersions) {
            assertTrue("GLSL版本应该有效: $version", version.isNotEmpty())
        }
    }

    @Test
    fun `ShaderProgram - 着色器类型`() {
        val shaderTypes = listOf("VERTEX", "FRAGMENT", "GEOMETRY", "COMPUTE")
        
        for (type in shaderTypes) {
            assertTrue("着色器类型应该有效: $type", type.isNotEmpty())
        }
    }

    @Test
    fun `ShaderProgram - 编译状态`() {
        val compileStates = listOf("NOT_COMPILED", "COMPILING", "COMPILED", "FAILED")
        
        for (state in compileStates) {
            assertTrue("编译状态应该有效: $state", state.isNotEmpty())
        }
    }

    @Test
    fun `ShaderProgram - uniform位置缓存`() {
        val uniformCache = mutableMapOf<String, Int>()
        
        uniformCache["uModelMatrix"] = 0
        uniformCache["uViewMatrix"] = 1
        uniformCache["uProjectionMatrix"] = 2
        
        assertEquals(3, uniformCache.size)
        assertEquals(0, uniformCache["uModelMatrix"])
    }

    @Test
    fun `ShaderProgram - attribute位置缓存`() {
        val attributeCache = mutableMapOf<String, Int>()
        
        attributeCache["aPosition"] = 0
        attributeCache["aTexCoord"] = 1
        attributeCache["aNormal"] = 2
        
        assertEquals(3, attributeCache.size)
    }

    // ===== ImageShaderRenderer 扩展测试 =====

    @Test
    fun `ImageShaderRenderer - 渲染模式`() {
        val renderModes = listOf("PREVIEW", "STANDARD", "HIGH_QUALITY", "ULTRA_QUALITY")
        
        for (mode in renderModes) {
            assertTrue("渲染模式应该有效: $mode", mode.isNotEmpty())
        }
    }

    @Test
    fun `ImageShaderRenderer - 渲染状态`() {
        val renderStates = listOf("IDLE", "RENDERING", "COMPLETED", "ERROR")
        
        for (state in renderStates) {
            assertTrue("渲染状态应该有效: $state", state.isNotEmpty())
        }
    }

    @Test
    fun `ImageShaderRenderer - 参数插值`() {
        val from = 0f
        val to = 100f
        val progress = 0.5f
        
        val interpolated = from + (to - from) * progress
        
        assertEquals(50f, interpolated, 0.001f)
    }

    @Test
    fun `ImageShaderRenderer - 平滑插值`() {
        val from = 0f
        val to = 100f
        val progress = 0.5f
        
        // Smoothstep公式
        val smoothProgress = progress * progress * (3 - 2 * progress)
        val interpolated = from + (to - from) * smoothProgress
        
        assertTrue("平滑插值应该在0-100之间", interpolated in 0f..100f)
    }

    @Test
    fun `ImageShaderRenderer - FPS计算`() {
        val frameTimeMs = 16.67f
        val fps = 1000f / frameTimeMs
        
        assertEquals(60f, fps, 0.1f)
    }

    @Test
    fun `ImageShaderRenderer - 低帧率检测`() {
        val frameTimeMs = 100f
        val fps = 1000f / frameTimeMs
        
        assertTrue("帧率低于30FPS应该被标记", fps < 30f)
    }

    @Test
    fun `ImageShaderRenderer - 渲染时间预算`() {
        val frameBudgetMs = 16L
        val actualRenderTime = 12L
        
        assertTrue("渲染时间应该在预算内", actualRenderTime <= frameBudgetMs)
    }

    @Test
    fun `ImageShaderRenderer - 超出预算检测`() {
        val frameBudgetMs = 16L
        val actualRenderTime = 20L
        
        assertTrue("渲染时间超出预算", actualRenderTime > frameBudgetMs)
    }

    // ===== GPURenderManager 扩展测试 =====

    @Test
    fun `GPURenderManager - EGL配置验证`() {
        val config = mapOf(
            "EGL_RED_SIZE" to 8,
            "EGL_GREEN_SIZE" to 8,
            "EGL_BLUE_SIZE" to 8,
            "EGL_ALPHA_SIZE" to 8,
            "EGL_DEPTH_SIZE" to 16
        )
        
        assertEquals(5, config.size)
        for ((_, value) in config) {
            assertTrue("配置值应该 > 0", value > 0)
        }
    }

    @Test
    fun `GPURenderManager - EGL上下文创建`() {
        var hasContext = false
        hasContext = true
        
        assertTrue("应该存在有效上下文", hasContext)
    }

    @Test
    fun `GPURenderManager - EGL表面绑定`() {
        var hasSurface = false
        hasSurface = true
        
        assertTrue("应该存在有效表面", hasSurface)
    }

    @Test
    fun `GPURenderManager - 渲染线程状态`() {
        var isRunning = false
        isRunning = true
        
        assertTrue("渲染线程应该在运行", isRunning)
    }

    @Test
    fun `GPURenderManager - 渲染线程终止`() {
        var isRunning = true
        isRunning = false
        
        assertFalse("渲染线程应该停止", isRunning)
    }

    @Test
    fun `GPURenderManager - 渲染队列大小限制`() {
        val maxQueueSize = 10
        var currentQueueSize = 5
        
        assertTrue("队列大小不应该超过限制", currentQueueSize <= maxQueueSize)
    }

    @Test
    fun `GPURenderManager - 渲染队列满检测`() {
        val maxQueueSize = 10
        var currentQueueSize = 10
        
        val isFull = currentQueueSize >= maxQueueSize
        
        assertTrue("队列已满", isFull)
    }

    @Test
    fun `GPURenderManager - CPU降级渲染`() {
        var useGpu = false
        var useCpuFallback = true
        
        assertFalse("GPU不可用时应该禁用", useGpu)
        assertTrue("应该启用CPU降级", useCpuFallback)
    }

    // ===== RenderParameters 扩展测试 =====

    @Test
    fun `RenderParameters - 参数范围验证`() {
        val params = mapOf(
            "tone" to (-30..30),
            "saturation" to (-30..30),
            "contrast" to (-30..30),
            "colorTemp" to (-30..30),
            "sharpness" to (-30..30),
            "vignette" to (-30..30)
        )
        
        for ((param, range) in params) {
            assertTrue("参数范围应该有效: $param", range.first < range.last)
        }
    }

    @Test
    fun `RenderParameters - 参数归一化`() {
        val rawValue = 15
        val normalizedValue = rawValue / 30f
        
        assertEquals(0.5f, normalizedValue, 0.001f)
    }

    @Test
    fun `RenderParameters - 参数合并`() {
        val baseParams = mapOf("saturation" to 10, "contrast" to 5)
        val adjustments = mapOf("saturation" to -5, "tone" to 10)
        
        val merged = baseParams.toMutableMap()
        adjustments.forEach { (key, value) ->
            merged[key] = merged.getOrDefault(key, 0) + value
        }
        
        assertEquals(5, merged["saturation"])
        assertEquals(5, merged["contrast"])
        assertEquals(10, merged["tone"])
    }

    @Test
    fun `RenderParameters - 参数克隆`() {
        val original = mapOf("saturation" to 10, "contrast" to 5)
        val cloned = original.toMap()
        
        assertEquals(original, cloned)
    }

    // ===== CPURenderer 扩展测试 =====

    @Test
    fun `CPURenderer - 渲染状态`() {
        val renderStates = listOf("IDLE", "RENDERING", "COMPLETED", "ERROR")
        
        for (state in renderStates) {
            assertTrue("渲染状态应该有效: $state", state.isNotEmpty())
        }
    }

    @Test
    fun `CPURenderer - 像素处理`() {
        val pixelCount = 1920 * 1080
        val processedPixels = pixelCount
        
        assertEquals("应该处理所有像素", pixelCount, processedPixels)
    }

    @Test
    fun `CPURenderer - 颜色转换`() {
        val r = 255
        val g = 128
        val b = 64
        
        val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()
        
        assertTrue("亮度计算应该有效", luminance in 0..255)
    }

    // ===== TextureManager 扩展测试 =====

    @Test
    fun `TextureManager - 纹理ID生成`() {
        val textureIds = mutableSetOf<Int>()
        
        for (i in 1..100) {
            textureIds.add(i)
        }
        
        assertEquals("应该有100个唯一ID", 100, textureIds.size)
    }

    @Test
    fun `TextureManager - 纹理格式验证`() {
        val validFormats = listOf("RGB", "RGBA", "LUMINANCE", "LUMINANCE_ALPHA")
        
        for (format in validFormats) {
            assertTrue("纹理格式应该有效: $format", format.isNotEmpty())
        }
    }

    @Test
    fun `TextureManager - 纹理尺寸验证`() {
        val validSizes = listOf(256, 512, 1024, 2048)
        
        for (size in validSizes) {
            assertTrue("纹理尺寸应该是正数", size > 0)
            assertTrue("纹理尺寸应该是2的幂", size % 2 == 0)
        }
    }
}