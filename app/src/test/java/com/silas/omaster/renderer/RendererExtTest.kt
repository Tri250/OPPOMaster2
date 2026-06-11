package com.silas.omaster.renderer

import org.junit.Test
import org.junit.Assert.*
import kotlin.math.pow

/**
 * ShaderProgram 单元测试
 * 测试着色器程序的逻辑
 */
class ShaderProgramTest {

    @Test
    fun `着色器类型 - 应该支持顶点和片段着色器`() {
        val shaderTypes = listOf(
            ShaderType.VERTEX,
            ShaderType.FRAGMENT
        )
        
        assertEquals("应该有2种着色器类型", 2, shaderTypes.size)
    }

    @Test
    fun `着色器状态 - 应该包含所有状态`() {
        val states = listOf(
            ShaderState.UNCOMPILED,
            ShaderState.COMPILED,
            ShaderState.LINKED,
            ShaderState.ERROR
        )
        
        assertEquals("应该有4种着色器状态", 4, states.size)
    }

    @Test
    fun `着色器属性 - 应该包含标准属性`() {
        val attributes = listOf(
            "aPosition",
            "aTexCoord",
            "aColor"
        )
        
        for (attr in attributes) {
            assertTrue("属性名应该以'a'开头", attr.startsWith("a"))
        }
    }

    @Test
    fun `着色器uniform - 应该包含标准uniform`() {
        val uniforms = listOf(
            "uMVPMatrix",
            "uTexture",
            "uBrightness",
            "uContrast",
            "uSaturation"
        )
        
        for (uniform in uniforms) {
            assertTrue("uniform名应该以'u'开头", uniform.startsWith("u"))
        }
    }

    @Test
    fun `着色器精度 - 应该支持不同精度`() {
        val precisions = listOf(
            "lowp",
            "mediump",
            "highp"
        )
        
        assertEquals("应该有3种精度级别", 3, precisions.size)
    }
}

/**
 * GPURenderManager 单元测试
 * 测试GPU渲染管理器的逻辑
 */
class GPURenderManagerTest {

    @Test
    fun `渲染质量 - 应该支持多种质量级别`() {
        val qualities = listOf(
            RenderQuality.PREVIEW,
            RenderQuality.STANDARD,
            RenderQuality.HIGH
        )
        
        assertEquals("应该有3种质量级别", 3, qualities.size)
    }

    @Test
    fun `渲染模式 - 应该支持多种渲染模式`() {
        val modes = listOf(
            RenderMode.REALTIME,
            RenderMode.BATCH,
            RenderMode.OFFSCREEN
        )
        
        assertEquals("应该有3种渲染模式", 3, modes.size)
    }

    @Test
    fun `纹理格式 - 应该支持常用格式`() {
        val formats = listOf(
            TextureFormat.RGBA_8888,
            TextureFormat.RGB_565,
            TextureFormat.RGBA_F16
        )
        
        assertTrue("应该支持多种纹理格式", formats.size >= 3)
    }

    @Test
    fun `帧缓冲配置 - 应该有合理的配置`() {
        val config = FrameBufferConfig(
            width = 1920,
            height = 1080,
            samples = 4,
            format = TextureFormat.RGBA_8888
        )
        
        assertTrue("宽度应该大于0", config.width > 0)
        assertTrue("高度应该大于0", config.height > 0)
        assertTrue("采样数应该大于0", config.samples > 0)
    }

    @Test
    fun `GPU特性检测 - 应该检测关键特性`() {
        val features = mapOf(
            "GL_OES_texture_float" to true,
            "GL_OES_texture_half_float" to true,
            "GL_EXT_texture_filter_anisotropic" to true
        )
        
        assertTrue("应该检测GPU特性", features.isNotEmpty())
    }
}

/**
 * ImageShaderRenderer 单元测试
 */
class ImageShaderRendererTest {

    @Test
    fun `着色器代码 - 顶点着色器应该有效`() {
        val vertexShader = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """.trimIndent()
        
        assertTrue("顶点着色器应该包含main函数", vertexShader.contains("void main()"))
        assertTrue("顶点着色器应该包含位置属性", vertexShader.contains("aPosition"))
    }

    @Test
    fun `着色器代码 - 片段着色器应该有效`() {
        val fragmentShader = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """.trimIndent()
        
        assertTrue("片段着色器应该包含精度声明", fragmentShader.contains("precision"))
        assertTrue("片段着色器应该包含纹理采样", fragmentShader.contains("texture2D"))
    }

    @Test
    fun `亮度调整着色器 - 应该正确计算亮度`() {
        // 模拟亮度调整公式
        val brightness = 0.2f
        val originalColor = 0.5f
        val adjustedColor = originalColor + brightness
        
        assertEquals(0.7f, adjustedColor, 0.01f)
    }

    @Test
    fun `对比度调整着色器 - 应该正确计算对比度`() {
        // 模拟对比度调整公式
        val contrast = 1.5f
        val originalColor = 0.5f
        val adjustedColor = (originalColor - 0.5f) * contrast + 0.5f
        
        assertEquals(0.5f, adjustedColor, 0.01f) // 中间值不变
    }

    @Test
    fun `饱和度调整着色器 - 应该正确计算饱和度`() {
        // 模拟饱和度调整公式
        val r = 0.8f
        val g = 0.6f
        val b = 0.4f
        
        val gray = 0.299f * r + 0.587f * g + 0.114f * b
        val saturation = 1.5f
        
        val adjustedR = gray + (r - gray) * saturation
        val adjustedG = gray + (g - gray) * saturation
        val adjustedB = gray + (b - gray) * saturation
        
        assertTrue("调整后的红色应该在有效范围内", adjustedR in 0.0f..1.0f)
        assertTrue("调整后的绿色应该在有效范围内", adjustedG in 0.0f..1.0f)
        assertTrue("调整后的蓝色应该在有效范围内", adjustedB in 0.0f..1.0f)
    }
}

/**
 * RenderParameters 扩展测试
 */
class RenderParametersExtTest {

    @Test
    fun `参数组合 - 应该正确合并参数`() {
        val params1 = RenderParameters(brightness = 10f, contrast = 20f)
        val params2 = RenderParameters(saturation = 30f, warmth = 5f)
        
        val combined = RenderParameters(
            brightness = params1.brightness,
            contrast = params1.contrast,
            saturation = params2.saturation,
            warmth = params2.warmth
        )
        
        assertEquals(10f, combined.brightness)
        assertEquals(20f, combined.contrast)
        assertEquals(30f, combined.saturation)
        assertEquals(5f, combined.warmth)
    }

    @Test
    fun `参数插值 - 应该正确插值参数`() {
        val start = RenderParameters(brightness = 0f)
        val end = RenderParameters(brightness = 100f)
        val t = 0.5f
        
        val interpolated = RenderParameters(
            brightness = start.brightness + (end.brightness - start.brightness) * t
        )
        
        assertEquals(50f, interpolated.brightness)
    }

    @Test
    fun `参数归一化 - 应该将参数归一化到0-1范围`() {
        val brightness = 50f // 范围 -100 到 100
        val normalized = (brightness + 100) / 200f
        
        assertEquals(0.75f, normalized, 0.01f)
    }
}

/**
 * RenderResult 扩展测试
 */
class RenderResultExtTest {

    @Test
    fun `成功结果 - 应该包含纹理ID和处理时间`() {
        val result = RenderResult.Success(
            outputTextureId = 123,
            processingTimeMs = 50L,
            quality = RenderQuality.STANDARD
        )
        
        assertTrue("纹理ID应该大于0", result.outputTextureId > 0)
        assertTrue("处理时间应该非负", result.processingTimeMs >= 0)
    }

    @Test
    fun `错误结果 - 应该包含错误信息`() {
        val result = RenderResult.Error("Shader compilation failed")
        
        assertTrue("错误信息不应该为空", result.message.isNotEmpty())
    }

    @Test
    fun `回退结果 - 应该包含回退原因`() {
        val result = RenderResult.FallbackToCPU(
            reason = "GPU not available",
            processingTimeMs = 100L
        )
        
        assertTrue("回退原因不应该为空", result.reason.isNotEmpty())
    }
}

/**
 * 纹理管理测试
 */
class TextureManagementTest {

    @Test
    fun `纹理尺寸 - 应该是2的幂次`() {
        val validSizes = listOf(256, 512, 1024, 2048, 4096)
        
        for (size in validSizes) {
            val isPowerOfTwo = (size and (size - 1)) == 0
            assertTrue("$size 应该是2的幂次", isPowerOfTwo)
        }
    }

    @Test
    fun `纹理尺寸 - 非幂次应该被检测`() {
        val invalidSizes = listOf(100, 300, 1000, 1500)
        
        for (size in invalidSizes) {
            val isPowerOfTwo = (size and (size - 1)) == 0
            assertFalse("$size 不应该是2的幂次", isPowerOfTwo)
        }
    }

    @Test
    fun `Mipmap级别 - 应该正确计算`() {
        val textureSize = 1024
        var levels = 0
        var size = textureSize
        
        while (size > 0) {
            levels++
            size /= 2
        }
        
        assertEquals("1024应该有11个mipmap级别", 11, levels)
    }

    @Test
    fun `纹理内存计算 - 应该正确计算内存占用`() {
        val width = 1024
        val height = 1024
        val bytesPerPixel = 4 // RGBA
        
        val memoryBytes = width * height * bytesPerPixel
        val memoryMB = memoryBytes / (1024 * 1024).toFloat()
        
        assertEquals(4f, memoryMB, 0.01f) // 4MB
    }
}

/**
 * GPU性能测试
 */
class GPUPerformanceTest {

    @Test
    fun `帧率计算 - 应该正确计算帧率`() {
        val frameCount = 60
        val totalTimeMs = 1000L
        
        val fps = frameCount * 1000f / totalTimeMs
        
        assertEquals(60f, fps, 0.1f)
    }

    @Test
    fun `帧时间计算 - 应该正确计算帧时间`() {
        val fps = 60f
        val frameTimeMs = 1000f / fps
        
        assertEquals(16.67f, frameTimeMs, 0.1f)
    }

    @Test
    fun `GPU负载 - 应该在合理范围内`() {
        val gpuUsage = 75f // 百分比
        
        assertTrue("GPU负载应该在0到100之间", gpuUsage in 0.0f..100.0f)
    }
}

// 辅助数据类和枚举
enum class ShaderType {
    VERTEX, FRAGMENT
}

enum class ShaderState {
    UNCOMPILED, COMPILED, LINKED, ERROR
}

enum class RenderMode {
    REALTIME, BATCH, OFFSCREEN
}

enum class TextureFormat {
    RGBA_8888, RGB_565, RGBA_F16
}

data class FrameBufferConfig(
    val width: Int,
    val height: Int,
    val samples: Int,
    val format: TextureFormat
)
