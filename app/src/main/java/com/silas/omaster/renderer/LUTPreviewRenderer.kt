package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES10
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.data.lut.LUT3DData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * CameraX 取景器实时 LUT 预览渲染器（OpenGL ES 3.0）
 *
 * 将 Camera2/CameraX 预览帧通过 SurfaceTexture 传入，
 * 在 GPU 上实时应用 LUT + 基础色彩调整，输出到 Surface。
 *
 * Phase 2 核心组件：实现取景器内 WYSIWYG（所见即所得）。
 */
class LUTPreviewRenderer(context: Context) {

    companion object {
        private const val TAG = "LUTPreviewRenderer"
        private const val EGL_CONTEXT_CLIENT_VERSION = 0x3098

        // 全屏四边形顶点（NDC -1..+1）
        private val VERTICES = floatArrayOf(
            -1f, -1f, 0f, 0f,  // 左下
             1f, -1f, 1f, 0f,  // 右下
            -1f,  1f, 0f, 1f,  // 左上
             1f,  1f, 1f, 1f   // 右上
        )
    }

    private val appContext = context.applicationContext

    // EGL
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    // GL 资源
    private var program: Int = 0
    private var cameraTextureId: Int = 0
    private var lutTextureId: Int = 0
    private var vertexBuffer: FloatBuffer? = null
    private var cameraTexture: SurfaceTexture? = null
    private var surface: Surface? = null

    // 着色器 uniform 位置
    private var uCameraTextureLoc: Int = -1
    private var uLUTTextureLoc: Int = -1
    private var uLUTStrengthLoc: Int = -1
    private var uColorMatrixLoc: Int = -1
    private var uHasLUTLoc: Int = -1

    // 状态
    private var lutStrength: Float = 0.0f
    private var hasLUT: Boolean = false
    private val colorMatrix = FloatArray(16)

    // 渲染线程
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null

    private val vertexShaderCode = """
        #version 300 es
        in vec4 aPosition;
        in vec2 aTexCoord;
        out vec2 vTexCoord;
        void main() {
            gl_Position = aPosition;
            vTexCoord = aTexCoord;
        }
    """.trimIndent()

    private val fragmentShaderCode = """
        #version 300 es
        #extension GL_OES_EGL_image_external_essl3 : require
        precision highp float;

        in vec2 vTexCoord;
        uniform samplerExternalOES uCameraTexture;
        uniform sampler2D uLUTTexture;
        uniform float uLUTStrength;
        uniform mat4 uColorMatrix;
        uniform int uHasLUT;

        out vec4 fragColor;

        vec3 applyLUT(vec3 color) {
            // 3D LUT 2D 编码解码：64x64 网格，每格 64x64
            float blueIndex = color.b * 63.0;
            vec2 lutCoord = vec2(
                color.r * 63.0 / 64.0 + mod(floor(blueIndex), 8.0) / 8.0,
                color.g * 63.0 / 64.0 + floor(blueIndex / 8.0) / 8.0
            );
            vec3 lutColor = texture(uLUTTexture, lutCoord).rgb;
            return mix(color, lutColor, uLUTStrength);
        }

        void main() {
            vec3 color = texture(uCameraTexture, vTexCoord).rgb;

            // 基础 ColorMatrix 调整
            vec4 adjusted = uColorMatrix * vec4(color, 1.0);
            color = adjusted.rgb;

            // 3D LUT 叠加
            if (uHasLUT > 0) {
                color = applyLUT(color);
            }

            fragColor = vec4(color, 1.0);
        }
    """.trimIndent()

    /**
     * 初始化 EGL + OpenGL 环境。
     * 必须在 Surface 可用后调用（如 TextureView.SurfaceTextureAvailable 回调中）。
     * @return true 表示初始化成功，false 表示失败（调用方应降级到 CPU 预览路径）
     */
    fun init(surface: Surface): Boolean {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            Log.w(TAG, "Already initialized")
            return true
        }

        this.surface = surface

        try {
            // EGL 初始化
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                Log.e(TAG, "eglGetDisplay failed")
                releaseEGLPartial()
                return false
            }

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                Log.e(TAG, "eglInitialize failed")
                releaseEGLPartial()
                return false
            }

            // 选择 EGLConfig（OpenGL ES 3.0）
            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) || numConfigs[0] == 0) {
                Log.e(TAG, "eglChooseConfig failed")
                releaseEGLPartial()
                return false
            }

            // 创建 EGLContext（OpenGL ES 3.0）
            val contextAttribs = intArrayOf(
                EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                Log.e(TAG, "eglCreateContext failed")
                releaseEGLPartial()
                return false
            }

            // 创建 Window Surface
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                Log.e(TAG, "eglCreateWindowSurface failed")
                releaseEGLPartial()
                return false
            }

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                Log.e(TAG, "eglMakeCurrent failed")
                releaseEGLPartial()
                return false
            }

            // 编译着色器
            program = createProgram(vertexShaderCode, fragmentShaderCode)
            if (program == 0) {
                Log.e(TAG, "Shader program creation failed")
                releaseEGLPartial()
                return false
            }
            GLES30.glUseProgram(program)

            // 获取 uniform 位置
            uCameraTextureLoc = GLES30.glGetUniformLocation(program, "uCameraTexture")
            uLUTTextureLoc = GLES30.glGetUniformLocation(program, "uLUTTexture")
            uLUTStrengthLoc = GLES30.glGetUniformLocation(program, "uLUTStrength")
            uColorMatrixLoc = GLES30.glGetUniformLocation(program, "uColorMatrix")
            uHasLUTLoc = GLES30.glGetUniformLocation(program, "uHasLUT")

            // 创建顶点缓冲
            vertexBuffer = ByteBuffer.allocateDirect(VERTICES.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(VERTICES)
                    position(0)
                }

            // 创建 Camera 纹理（External OES，供 SurfaceTexture 绑定）
            cameraTextureId = createOESTexture()
            lutTextureId = createTexture()

            // 初始化 ColorMatrix 为单位矩阵
            android.opengl.Matrix.setIdentityM(colorMatrix, 0)

            Log.i(TAG, "LUTPreviewRenderer initialized, program=$program")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "LUTPreviewRenderer init failed", e)
            releaseEGLPartial()
            return false
        }
    }

    /**
     * 初始化失败时的部分清理：仅释放已分配的 EGL 资源，不触碰 GL 纹理/程序。
     */
    private fun releaseEGLPartial() {
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglTerminate(eglDisplay)
            eglDisplay = EGL14.EGL_NO_DISPLAY
        }
        surface?.release()
        surface = null
    }

    /**
     * 更新 LUT 数据。
     * @param lutData 3D LUT 数据（64x64x64）
     * @param strength LUT 应用强度 0.0~1.0
     */
    fun updateLUT(lutData: LUT3DData?, strength: Float) {
        lutStrength = strength.coerceIn(0f, 1f)
        hasLUT = lutData != null && lutStrength > 0.01f

        if (lutData == null || lutStrength <= 0.01f) {
            hasLUT = false
            return
        }

        // 将 3D LUT 编码为 2D 纹理（64x64 网格，每格 64x64 = 512x512）
        val texSize = 512
        val pixels = IntArray(texSize * texSize)
        for (by in 0 until 64) {
            for (bx in 0 until 64) {
                for (gy in 0 until 8) {
                    for (gx in 0 until 8) {
                        val r = gx + bx * 8
                        val g = gy + by * 8
                        val b = (bx + by * 8)
                        val pixelIndex = r + g * texSize
                        if (pixelIndex < pixels.size) {
                            val rgb = lutData.sampleTrilinear(r / 63f, g / 63f, b / 63f)
                            val ir = (rgb[0] * 255).toInt().coerceIn(0, 255)
                            val ig = (rgb[1] * 255).toInt().coerceIn(0, 255)
                            val ib = (rgb[2] * 255).toInt().coerceIn(0, 255)
                            pixels[pixelIndex] = (255 shl 24) or (ir shl 16) or (ig shl 8) or ib
                        }
                    }
                }
            }
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            texSize, texSize, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE,
            IntBuffer.wrap(pixels)
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        Log.d(TAG, "LUT updated, strength=$lutStrength")
    }

    /**
     * 更新 ColorMatrix（基础色彩调整）。
     */
    fun updateColorMatrix(params: HasselbladParams) {
        val cm = HasselbladParamMapper.buildColorMatrix(params)
        cm.getArray().copyInto(colorMatrix)
    }

    /**
     * 从 Bitmap 更新 LUT（简化接口）。
     */
    fun updateLUTFromBitmap(bitmap: Bitmap, strength: Float) {
        if (bitmap.isRecycled) {
            hasLUT = false
            return
        }
        lutStrength = strength.coerceIn(0f, 1f)
        hasLUT = strength > 0.01f

        try {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        } catch (e: Exception) {
            Log.e(TAG, "从 Bitmap 更新 LUT 失败", e)
            hasLUT = false
        }
    }

    /**
     * 绘制一帧。
     */
    fun drawFrame() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || program == 0) return

        GLES30.glUseProgram(program)

        // 设置顶点数据
        val posLoc = GLES30.glGetAttribLocation(program, "aPosition")
        val texLoc = GLES30.glGetAttribLocation(program, "aTexCoord")

        vertexBuffer?.position(0)
        GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)
        GLES30.glEnableVertexAttribArray(posLoc)

        vertexBuffer?.position(2)
        GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 16, vertexBuffer)
        GLES30.glEnableVertexAttribArray(texLoc)

        // 绑定 Camera 纹理（External OES）到 unit 0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        GLES30.glUniform1i(uCameraTextureLoc, 0)

        // 绑定 LUT 纹理到 unit 1
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
        GLES30.glUniform1i(uLUTTextureLoc, 1)

        // 设置 uniform
        GLES30.glUniform1f(uLUTStrengthLoc, lutStrength)
        GLES30.glUniform1i(uHasLUTLoc, if (hasLUT) 1 else 0)
        GLES30.glUniformMatrix4fv(uColorMatrixLoc, 1, false, colorMatrix, 0)

        // 绘制
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        // 交换 buffer
        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    /**
     * 获取 Camera 纹理 ID（供外部 SurfaceTexture 绑定）。
     */
    fun getCameraTextureId(): Int = cameraTextureId

    /**
     * 释放所有 EGL/OpenGL 资源。
     */
    fun release() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return

        try {
            GLES30.glDeleteProgram(program)
            GLES30.glDeleteTextures(2, intArrayOf(cameraTextureId, lutTextureId), 0)

            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        } catch (e: Exception) {
            Log.e(TAG, "释放 EGL/OpenGL 资源失败", e)
        }

        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE

        surface?.release()
        surface = null
        cameraTexture?.release()
        cameraTexture = null

        Log.i(TAG, "LUTPreviewRenderer released")
    }

    // === 内部工具方法 ===

    private fun createProgram(vertexCode: String, fragmentCode: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(program)
            GLES30.glDeleteProgram(program)
            throw RuntimeException("Program link failed: $error")
        }

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
        return program
    }

    private fun loadShader(type: Int, code: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, code)
        GLES30.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            throw RuntimeException("Shader compile failed: $error")
        }
        return shader
    }

    private fun createTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return id
    }

    private fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        return id
    }
}
