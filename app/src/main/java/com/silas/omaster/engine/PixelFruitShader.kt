package com.silas.omaster.engine

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * PixelFruit GPU 实时预览 Shader
 *
 * 将 WebGLRenderer.js 的 Fragment Shader 移植到 Android OpenGL ES 2.0
 * 用于高性能 60fps 实时调色预览
 *
 * Shader 处理管道（对齐 WebGLRenderer.js）：
 * 1. 白平衡/色调  2. 曝光(EV)  3. 亮度  4. 对比度
 * 5. 饱和度       6. 阴影提升  7. 高光压缩  8. 白场调整
 */
class PixelFruitShader {

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D uTexture;

            uniform float uBrightness;
            uniform float uContrast;
            uniform float uSaturation;
            uniform float uExposure;
            uniform float uShadows;
            uniform float uHighlights;
            uniform float uWhites;
            uniform vec3 uWB;

            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);

                // 1. 白平衡
                color.rgb *= uWB;

                // 2. 曝光 (2^exposure)
                color.rgb *= pow(2.0, uExposure);

                // 3. 亮度
                color.rgb *= uBrightness;

                // 4. 对比度（以0.5为中心）
                color.rgb = (color.rgb - 0.5) * uContrast + 0.5;

                // 5. 饱和度
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                color.rgb = mix(vec3(gray), color.rgb, uSaturation);

                // 6. 阴影提升
                float shadowBoost = uShadows;
                vec3 shadowMask = step(color.rgb, vec3(0.25));
                color.rgb += shadowMask * (vec3(0.25) - color.rgb) * shadowBoost;

                // 7. 高光压缩
                vec3 over = max(vec3(0.0), color.rgb - vec3(0.75));
                color.rgb -= over * uHighlights;

                // 8. 白场调整（smoothstep 软蒙版）
                vec3 whiteMask = smoothstep(vec3(0.9), vec3(1.0), color.rgb);
                color.rgb = mix(color.rgb, color.rgb * uWhites, whiteMask);

                gl_FragColor = clamp(color, 0.0, 1.0);
            }
        """

        private val FULL_RECTANGLE_COORDS = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        private val FULL_RECTANGLE_TEX_COORDS = floatArrayOf(0f, 1f, 1f, 1f, 0f, 0f, 1f, 0f)
    }

    private var program = 0
    private var textureId = 0

    // Uniform 位置缓存
    private var uBrightness = 0
    private var uContrast = 0
    private var uSaturation = 0
    private var uExposure = 0
    private var uShadows = 0
    private var uHighlights = 0
    private var uWhites = 0
    private var uWB = 0

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer

    init {
        vertexBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_COORDS.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.put(FULL_RECTANGLE_COORDS).position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(FULL_RECTANGLE_TEX_COORDS.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        texCoordBuffer.put(FULL_RECTANGLE_TEX_COORDS).position(0)
    }

    /** 初始化 Shader Program */
    fun initialize() {
        if (program != 0) return
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        if (program == 0) throw RuntimeException("Failed to create PixelFruit shader program")

        uBrightness = GLES20.glGetUniformLocation(program, "uBrightness")
        uContrast = GLES20.glGetUniformLocation(program, "uContrast")
        uSaturation = GLES20.glGetUniformLocation(program, "uSaturation")
        uExposure = GLES20.glGetUniformLocation(program, "uExposure")
        uShadows = GLES20.glGetUniformLocation(program, "uShadows")
        uHighlights = GLES20.glGetUniformLocation(program, "uHighlights")
        uWhites = GLES20.glGetUniformLocation(program, "uWhites")
        uWB = GLES20.glGetUniformLocation(program, "uWB")
    }

    /** 根据 PixelFruitParams 更新所有 Shader Uniform */
    fun updateUniforms(params: PixelFruitParams) {
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uBrightness, params.brightness)
        GLES20.glUniform1f(uContrast, params.contrast + 1f) // 对比度传入时 +1
        GLES20.glUniform1f(uSaturation, params.saturation / 100f)
        GLES20.glUniform1f(uExposure, params.exposure)
        GLES20.glUniform1f(uShadows, params.shadows / 100f)
        GLES20.glUniform1f(uHighlights, -params.highlights / 100f)
        GLES20.glUniform1f(uWhites, params.whites / 100f)

        val redMul = 1f + params.redTint / 100f
        val greenMul = 1f + params.greenTint / 100f
        val blueMul = 1f + params.blueTint / 100f
        GLES20.glUniform3f(uWB, redMul, greenMul, blueMul)
    }

    /** 渲染到 Bitmap（需在 GL 线程执行） */
    fun renderToBitmap(inputBitmap: Bitmap, width: Int, height: Int): Bitmap {
        GLES20.glViewport(0, 0, width, height)
        val aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        val aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")

        // 加载纹理
        if (textureId == 0) {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            textureId = textures[0]
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, inputBitmap, 0)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)

        val buffer = ByteBuffer.allocateDirect(width * height * 4)
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        result.copyPixelsFromBuffer(buffer)
        return result
    }

    fun release() {
        if (program != 0) GLES20.glDeleteProgram(program)
        if (textureId != 0) GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        program = 0; textureId = 0
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (vertexShader == 0 || fragmentShader == 0) return 0
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vertexShader)
        GLES20.glAttachShader(prog, fragmentShader)
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) { GLES20.glDeleteProgram(prog); return 0 }
        return prog
    }

    private fun loadShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) { GLES20.glDeleteShader(shader); return 0 }
        return shader
    }
}
