package com.silas.omaster.data.lut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import com.silas.omaster.renderer.ShaderProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 3D LUT GPU 渲染器
 *
 * 使用 OpenGL ES 3.0 将 3D LUT 应用到图片上。
 * 支持两种渲染模式：
 * 1. GPU 模式：通过 ShaderProgram + 2D 编码纹理实现高性能渲染
 * 2. CPU 回退模式：通过 LUT3DData.sampleTrilinear 实现软件渲染
 *
 * GPU 渲染管线：
 * 1. 解析 .cube 文件 → LUT3DData
 * 2. 编码为 2D 纹理 → uploadLUT3DTexture()
 * 3. 绑定着色器 + 纹理 → render()
 * 4. 读取像素 → applyLUTToBitmap()
 */
object LUT3DRenderer {

    private const val TAG = "LUT3DRenderer"

    // 全屏四边形顶点数据
    private val QUAD_VERTICES = floatArrayOf(
        // position    // texcoord
        -1f, -1f,      0f, 0f,
         1f, -1f,      1f, 0f,
        -1f,  1f,      0f, 1f,
         1f,  1f,      1f, 1f
    )

    private var shaderProgram: ShaderProgram? = null
    private var lutTextureId: Int = 0
    private var sourceTextureId: Int = 0
    private var framebufferId: Int = 0
    private var vertexBuffer: FloatBuffer? = null
    private var isInitialized = false

    /**
     * 初始化渲染器
     * 必须在 GL 线程调用
     */
    fun init(context: Context): Boolean {
        if (isInitialized) return true

        try {
            // 创建着色器程序
            shaderProgram = ShaderProgram.createFromAssets(
                context,
                "lut_3d_apply.vert",
                "lut_3d_apply.frag"
            )

            if (shaderProgram == null) {
                Log.e(TAG, "Failed to create LUT 3D shader program")
                return false
            }

            // 创建顶点缓冲区
            val buffer = ByteBuffer.allocateDirect(QUAD_VERTICES.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            buffer.put(QUAD_VERTICES)
            buffer.position(0)
            vertexBuffer = buffer

            // 生成纹理和帧缓冲区
            val textures = IntArray(2)
            GLES30.glGenTextures(2, textures, 0)
            sourceTextureId = textures[0]
            lutTextureId = textures[1]

            val fbos = IntArray(1)
            GLES30.glGenFramebuffers(1, fbos, 0)
            framebufferId = fbos[0]

            isInitialized = true
            Log.d(TAG, "LUT3DRenderer initialized successfully")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LUT3DRenderer", e)
            return false
        }
    }

    /**
     * 上传 3D LUT 数据为 2D 编码纹理
     * 必须在 GL 线程调用
     */
    fun uploadLUT3DTexture(lutData: LUT3DData): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Renderer not initialized")
            return false
        }

        try {
            val encodedData = LUT3DParser.encodeTo2DTexture(lutData.data, lutData.size)
            val width = lutData.size * lutData.size
            val height = lutData.size

            // 将 FloatArray 转换为 ByteBuffer
            val buffer = ByteBuffer.allocateDirect(encodedData.size * 4)
                .order(ByteOrder.nativeOrder())
            val floatBuffer = buffer.asFloatBuffer()
            floatBuffer.put(encodedData)
            floatBuffer.position(0)

            // 上传纹理
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D, 0,
                GLES30.GL_RGBA32F,
                width, height, 0,
                GLES30.GL_RGBA, GLES30.GL_FLOAT,
                buffer
            )

            val error = GLES30.glGetError()
            if (error != GLES30.GL_NO_ERROR) {
                Log.e(TAG, "Failed to upload LUT 3D texture, GL error: $error")
                // 回退到 RGBA8 纹理
                uploadLUT3DTextureAsRGBA8(lutData)
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            Log.d(TAG, "LUT 3D texture uploaded: ${width}x${height}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload LUT 3D texture", e)
            return false
        }
    }

    /**
     * 回退方案：使用 RGBA8 纹理上传（兼容性更好）
     */
    private fun uploadLUT3DTextureAsRGBA8(lutData: LUT3DData) {
        val bitmap = LUT3DParser.encodeToBitmap(lutData.data, lutData.size)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        Log.d(TAG, "LUT 3D texture uploaded as RGBA8 fallback")
    }

    /**
     * 上传源图像纹理
     */
    fun uploadSourceBitmap(bitmap: Bitmap): Boolean {
        if (!isInitialized) return false

        try {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sourceTextureId)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload source bitmap", e)
            return false
        }
    }

    /**
     * 执行 GPU 渲染
     */
    fun render(width: Int, height: Int, lutSize: Int, strength: Float): Boolean {
        val program = shaderProgram ?: return false

        try {
            program.use()

            // 设置源图像纹理
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sourceTextureId)
            program.setUniform1i("uTexture", 0)

            // 设置 LUT 3D 纹理
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
            program.setUniform1i("uLUT3D", 1)

            // 设置参数
            program.setUniform1f("uLUTSize", lutSize.toFloat())
            program.setUniform1f("uLUTStrength", strength.coerceIn(0f, 1f))

            // 设置顶点属性
            val posLoc = program.getAttribLocation("aPosition")
            val texLoc = program.getAttribLocation("aTexCoord")

            vertexBuffer?.let { vb ->
                vb.position(0)
                GLES30.glVertexAttribPointer(posLoc, 2, GLES30.GL_FLOAT, false, 16, vb)
                GLES30.glEnableVertexAttribArray(posLoc)

                vb.position(2)
                GLES30.glVertexAttribPointer(texLoc, 2, GLES30.GL_FLOAT, false, 16, vb)
                GLES30.glEnableVertexAttribArray(texLoc)
            }

            // 绘制
            GLES30.glViewport(0, 0, width, height)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "GPU render failed", e)
            return false
        }
    }

    /**
     * 释放所有资源
     * 必须在 GL 线程调用
     */
    fun release() {
        shaderProgram?.release()
        shaderProgram = null

        if (sourceTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(sourceTextureId), 0)
            sourceTextureId = 0
        }
        if (lutTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
            lutTextureId = 0
        }
        if (framebufferId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }

        isInitialized = false
    }

    /**
     * CPU 回退：使用三线性插值将 LUT 应用到 Bitmap
     *
     * 当 GPU 不可用或 GPU 渲染失败时使用此方法。
     * 性能低于 GPU 但兼容性最好。
     *
     * @param sourceBitmap 原始图片
     * @param lutData 3D LUT 数据
     * @param strength LUT 强度 [0, 1]
     * @return 应用 LUT 后的 Bitmap
     */
    suspend fun applyLUTCPU(
        sourceBitmap: Bitmap,
        lutData: LUT3DData,
        strength: Float = 1.0f
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val s = strength.coerceIn(0f, 1f)

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]
            val r = (pixel shr 16 and 0xFF) / 255f
            val g = (pixel shr 8 and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            // 三线性插值采样
            val mapped = lutData.sampleTrilinear(r, g, b)

            // 按强度混合
            val outR = ((r * (1f - s) + mapped[0] * s).coerceIn(0f, 1f) * 255).toInt()
            val outG = ((g * (1f - s) + mapped[1] * s).coerceIn(0f, 1f) * 255).toInt()
            val outB = ((b * (1f - s) + mapped[2] * s).coerceIn(0f, 1f) * 255).toInt()
            val outA = pixel ushr 24 and 0xFF

            dstPixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
        }

        result.setPixels(dstPixels, 0, width, 0, 0, width, height)
        result
    }

    /**
     * 生成 LUT 预览缩略图
     *
     * 对小尺寸缩略图应用 LUT，用于 LUT 列表预览
     *
     * @param sourceBitmap 原始缩略图（建议 200x200 以内）
     * @param lutData 3D LUT 数据
     * @return 应用 LUT 后的缩略图
     */
    suspend fun generatePreview(
        sourceBitmap: Bitmap,
        lutData: LUT3DData
    ): Bitmap = applyLUTCPU(sourceBitmap, lutData, 1.0f)
}
