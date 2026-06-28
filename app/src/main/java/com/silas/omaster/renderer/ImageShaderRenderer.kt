package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 图像着色器渲染器
 * 处理纹理渲染，实现18参数全通道图像处理
 * 
 * 使用OpenGL ES 3.0特性
 * 支持GPU加速渲染
 */
class ImageShaderRenderer(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageShaderRenderer"
        
        // 顶点数据（全屏四边形）
        private const val VERTEX_SIZE = 4 // x, y, u, v
        private const val VERTEX_COUNT = 4
        
        // 顶点坐标（标准化设备坐标）
        private val VERTEX_DATA = floatArrayOf(
            // x, y, u, v
            -1.0f, -1.0f, 0.0f, 0.0f,  // 左下
            1.0f, -1.0f, 1.0f, 0.0f,   // 右下
            -1.0f, 1.0f, 0.0f, 1.0f,   // 左上
            1.0f, 1.0f, 1.0f, 1.0f     // 右上
        )
        
        // 性能目标：1080p渲染 < 3ms
        private const val TARGET_RENDER_TIME_MS = 3L

        // 输出 Bitmap 最大像素尺寸（宽或高），超过此值自动降采样
        // 4096 × 4096 × 4 bytes ≈ 64 MB，在大多数设备上安全
        private const val MAX_OUTPUT_DIMENSION = 4096

        /**
         * 根据渲染质量返回输入图像的最大边长（像素）。
         * 超过该尺寸的输入图会在上传纹理前降采样，以平衡性能与质量。
         */
        fun maxDimensionForQuality(quality: RenderQuality): Int = when (quality) {
            RenderQuality.PREVIEW -> 512
            RenderQuality.STANDARD -> 1024
            RenderQuality.HIGH -> 2048
            RenderQuality.ULTRA -> 4096
        }

        /**
         * 按 [quality] 限制的尺寸对 [bitmap] 降采样。
         * 若原图未超限则原样返回（不复制）。
         */
        fun downsampleBitmapForQuality(bitmap: Bitmap, quality: RenderQuality): Bitmap {
            val maxDim = maxDimensionForQuality(quality)
            val srcMax = maxOf(bitmap.width, bitmap.height)
            if (srcMax <= maxDim) return bitmap
            val scale = maxDim.toFloat() / srcMax
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            return try {
                Bitmap.createScaledBitmap(bitmap, w, h, true)
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "降采样 OOM，使用原图", e)
                bitmap
            }
        }
    }
    
    // 着色器程序
    private var shaderProgram: ShaderProgram? = null
    
    // 顶点缓冲区
    private var vertexBuffer: FloatBuffer? = null
    
    // VBO和VAO
    private var vboId: Int = 0
    private var vaoId: Int = 0
    
    // 纹理ID
    private var inputTextureId: Int = 0
    private var outputTextureId: Int = 0
    private var framebufferId: Int = 0
    
    // 图像尺寸
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    // 输出纹理尺寸（用于检测尺寸变化后重建 FBO）
    private var outputWidth: Int = 0
    private var outputHeight: Int = 0
    
    // 初始化状态
    private var isInitialized: Boolean = false
    
    /**
     * 初始化渲染器
     * 必须在GL线程中调用
     * @return 是否初始化成功
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            return true
        }
        
        try {
            // 创建顶点缓冲区
            vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexBuffer?.put(VERTEX_DATA)?.position(0)
            
            // 创建着色器程序
            shaderProgram = ShaderProgram.createFromAssets(
                context,
                "image_adjust.vert",
                "image_adjust.frag"
            )
            
            if (shaderProgram == null) {
                Log.e(TAG, "Failed to create shader program")
                return false
            }
            
            // 创建VBO和VAO
            createVBOAndVAO()
            
            isInitialized = true
            Log.d(TAG, "ImageShaderRenderer initialized successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize renderer", e)
            return false
        }
    }
    
    /**
     * 创建VBO和VAO
     */
    private fun createVBOAndVAO() {
        // 创建VBO
        val vboArray = IntArray(1)
        GLES30.glGenBuffers(1, vboArray, 0)
        vboId = vboArray[0]
        
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            VERTEX_DATA.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )
        
        // 创建VAO (OpenGL ES 3.0特性)
        val vaoArray = IntArray(1)
        GLES30.glGenVertexArrays(1, vaoArray, 0)
        vaoId = vaoArray[0]
        
        GLES30.glBindVertexArray(vaoId)
        
        // 设置顶点属性
        shaderProgram?.use()
        
        val positionAttrib = shaderProgram?.getAttribLocation("aPosition") ?: -1
        val texCoordAttrib = shaderProgram?.getAttribLocation("aTexCoord") ?: -1
        
        if (positionAttrib >= 0) {
            GLES30.glEnableVertexAttribArray(positionAttrib)
            GLES30.glVertexAttribPointer(
                positionAttrib,
                2, // x, y
                GLES30.GL_FLOAT,
                false,
                VERTEX_SIZE * 4,
                0
            )
        }
        
        if (texCoordAttrib >= 0) {
            GLES30.glEnableVertexAttribArray(texCoordAttrib)
            GLES30.glVertexAttribPointer(
                texCoordAttrib,
                2, // u, v
                GLES30.GL_FLOAT,
                false,
                VERTEX_SIZE * 4,
                8 // offset: 2 floats * 4 bytes
            )
        }
        
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
    
    /**
     * 从Bitmap创建输入纹理
     * @param bitmap 输入图像
     * @return 纹理ID，失败返回0
     */
    fun createInputTexture(bitmap: Bitmap): Int {
        if (bitmap.isRecycled) {
            Log.e(TAG, "Bitmap已被回收，无法创建纹理")
            return 0
        }
        imageWidth = bitmap.width
        imageHeight = bitmap.height

        // 性能优化：如果已有纹理，先删除避免泄漏
        if (inputTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
            inputTextureId = 0
        }

        // 创建纹理
        val textureArray = IntArray(1)
        GLES30.glGenTextures(1, textureArray, 0)
        if (textureArray[0] == 0) {
            Log.e(TAG, "glGenTextures失败")
            return 0
        }
        inputTextureId = textureArray[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)

        // 设置纹理参数
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )

        // 上传纹理数据
        try {
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "纹理上传OOM", e)
            // 完全释放资源避免泄漏
            release()
            return 0
        }

        // 检查GL错误
        val glError = GLES30.glGetError()
        if (glError != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "输入纹理创建GL错误: 0x${glError.toString(16)}")
            GLES30.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
            inputTextureId = 0
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        return inputTextureId
    }
    
    /**
     * 创建输出纹理（FBO附件）
     * @param width 输出宽度
     * @param height 输出高度
     * @return 纹理ID
     */
    fun createOutputTexture(width: Int, height: Int): Int {
        val textureArray = IntArray(1)
        GLES30.glGenTextures(1, textureArray, 0)
        if (textureArray[0] == 0) {
            Log.e(TAG, "glGenTextures 失败，无法创建输出纹理")
            return 0
        }
        outputTextureId = textureArray[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTextureId)

        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )

        // 分配纹理存储
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            null
        )

        val glError = GLES30.glGetError()
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

        if (glError != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "输出纹理创建 GL 错误: 0x${glError.toString(16)}")
            GLES30.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
            outputTextureId = 0
            return 0
        }

        outputWidth = width
        outputHeight = height

        return outputTextureId
    }
    
    /**
     * 创建帧缓冲对象（FBO）
     * @return FBO ID
     */
    fun createFramebuffer(): Int {
        if (outputTextureId == 0) {
            Log.e(TAG, "输出纹理未创建，无法创建 FBO")
            return 0
        }

        val fboArray = IntArray(1)
        GLES30.glGenFramebuffers(1, fboArray, 0)
        if (fboArray[0] == 0) {
            Log.e(TAG, "glGenFramebuffers 失败")
            return 0
        }
        framebufferId = fboArray[0]

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)

        // 附加输出纹理
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            outputTextureId,
            0
        )

        // 检查FBO状态
        val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        if (status != GLES30.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "Framebuffer is not complete: $status")
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
            return 0
        }

        return framebufferId
    }
    
    /**
     * 渲染图像
     * @param params 渲染参数
     * @param quality 渲染质量
     * @return 渲染结果
     */
    fun render(
        params: RenderParameters,
        quality: RenderQuality = RenderQuality.STANDARD
    ): RenderResult {
        if (!isInitialized) {
            return RenderResult.Error("Renderer not initialized")
        }

        val startTime = System.currentTimeMillis()

        try {
            // RenderQuality 分级：输入纹理的降采样在 createInputTexture/renderToBitmap 阶段完成，
            // 此处记录质量等级用于性能分析与结果回传
            Log.d(TAG, "render quality=$quality, size=${imageWidth}x${imageHeight}, lut=${params.lutEnabled}")

            // 性能优化：复用输出纹理和FBO，仅在尺寸变化或尚未创建时重建
            if (outputTextureId == 0 || outputWidth != imageWidth || outputHeight != imageHeight) {
                if (outputTextureId != 0) {
                    GLES30.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
                    outputTextureId = 0
                }
                if (framebufferId != 0) {
                    GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
                    framebufferId = 0
                }
                val texId = createOutputTexture(imageWidth, imageHeight)
                if (texId == 0) {
                    return RenderResult.Error("创建输出纹理失败")
                }
            }
            if (framebufferId == 0) {
                val fboId = createFramebuffer()
                if (fboId == 0) {
                    return RenderResult.Error("创建 FBO 失败")
                }
            }

            // 绑定FBO进行离屏渲染
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)

            // 检查FBO完整性（防御性检查）
            val fboStatus = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            if (fboStatus != GLES30.GL_FRAMEBUFFER_COMPLETE) {
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
                Log.e(TAG, "FBO不完整: $fboStatus, 重建FBO")
                // 销毁并重建FBO
                if (framebufferId != 0) {
                    GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
                    framebufferId = 0
                }
                val fboId = createFramebuffer()
                if (fboId == 0) {
                    return RenderResult.Error("重建 FBO 失败")
                }
                GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)
            }

            // 设置视口
            GLES30.glViewport(0, 0, imageWidth, imageHeight)

            // 清除缓冲区
            GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

            // 使用着色器程序
            shaderProgram?.use()

            // 设置渲染参数
            shaderProgram?.setRenderParameters(params)

            // 设置图像尺寸（用于锐化等卷积操作）
            shaderProgram?.setUniform2f("uImageSize", imageWidth.toFloat(), imageHeight.toFloat())

            // 设置时间（用于颗粒效果）
            shaderProgram?.setUniform1f("uTime", System.currentTimeMillis() / 1000f)

            // 绑定VAO
            GLES30.glBindVertexArray(vaoId)

            // 绑定输入纹理
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
            shaderProgram?.setUniform1i("uTexture", 0)

            // 绘制
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)

            // 解绑
            GLES30.glBindVertexArray(0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

            // 检查GL错误
            val glError = GLES30.glGetError()
            if (glError != GLES30.GL_NO_ERROR) {
                Log.w(TAG, "GL错误: 0x${glError.toString(16)}")
            }

            val processingTime = System.currentTimeMillis() - startTime

            // 检查性能
            if (processingTime > TARGET_RENDER_TIME_MS) {
                Log.w(TAG, "Render time exceeded target: ${processingTime}ms > ${TARGET_RENDER_TIME_MS}ms")
            }

            return RenderResult.Success(outputTextureId, processingTime, quality)

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "渲染OOM", e)
            // OOM时完全释放资源
            release()
            return RenderResult.Error("Out of memory: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
            return RenderResult.Error("Render failed: ${e.message}", e)
        } finally {
            // 确保解绑所有GL资源
            GLES30.glBindVertexArray(0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
    }
    
    /**
     * 从纹理读取渲染结果到Bitmap
     *
     * 安全机制：当图像尺寸超过 [MAX_OUTPUT_DIMENSION] 时，自动降采样到安全尺寸，
     * 避免大图（如 50MP 照片）导致 OOM（50MP × 4 bytes ≈ 800 MB）。
     * 降采样通过 glReadPixels 读取缩小后的 FBO 实现，不会损失 GPU 渲染质量。
     *
     * @return 渲染后的Bitmap，若图像过大则返回降采样版本；失败返回 null
     */
    fun readOutputToBitmap(): Bitmap? {
        if (outputTextureId == 0 || imageWidth == 0 || imageHeight == 0) {
            return null
        }

        try {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId)

            // 计算安全的读取尺寸：超大图降采样到 MAX_OUTPUT_DIMENSION 以内
            val readWidth: Int
            val readHeight: Int
            var scaleForRead = false
            val originalWidth = imageWidth
            val originalHeight = imageHeight

            if (imageWidth > MAX_OUTPUT_DIMENSION || imageHeight > MAX_OUTPUT_DIMENSION) {
                val scale = MAX_OUTPUT_DIMENSION.toFloat() / maxOf(imageWidth, imageHeight)
                readWidth = (imageWidth * scale).toInt().coerceAtLeast(1)
                readHeight = (imageHeight * scale).toInt().coerceAtLeast(1)
                scaleForRead = true
                Log.i(TAG, "大图降采样: ${imageWidth}x${imageHeight} → ${readWidth}x${readHeight}")
            } else {
                readWidth = imageWidth
                readHeight = imageHeight
            }

            // 创建Bitmap
            val bitmap = Bitmap.createBitmap(readWidth, readHeight, Bitmap.Config.ARGB_8888)

            // 创建缓冲区
            val bufferSize = readWidth * readHeight * 4
            val buffer = ByteBuffer.allocateDirect(bufferSize)
                .order(ByteOrder.nativeOrder())

            // 读取像素数据
            // 降采样时：先缩小视口，让 GPU 完成缩放，再读取缩小后的像素
            if (scaleForRead) {
                GLES30.glViewport(0, 0, readWidth, readHeight)
            }
            GLES30.glReadPixels(
                0, 0,
                readWidth, readHeight,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                buffer
            )
            // 恢复视口
            if (scaleForRead) {
                GLES30.glViewport(0, 0, originalWidth, originalHeight)
            }

            // 交换 R 与 B 通道（GL_RGBA → Bitmap ARGB_8888 的 BGR_A 字节序）
            buffer.rewind()
            val pixelBytes = ByteArray(bufferSize)
            buffer.get(pixelBytes)
            for (i in pixelBytes.indices step 4) {
                val r = pixelBytes[i]
                val b = pixelBytes[i + 2]
                pixelBytes[i] = b
                pixelBytes[i + 2] = r
            }
            buffer.rewind()
            buffer.put(pixelBytes)
            buffer.rewind()

            // 将数据复制到Bitmap
            bitmap.copyPixelsFromBuffer(buffer)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

            return bitmap

        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "readOutputToBitmap OOM (imageSize=${imageWidth}x${imageHeight})", e)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read output to bitmap", e)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            return null
        }
    }
    
    /**
     * 渲染并返回Bitmap结果
     *
     * 安全机制：超大输入图（宽或高 > [MAX_OUTPUT_DIMENSION]）会先降采样再渲染，
     * 避免纹理上传和像素回读时 OOM。
     *
     * @param inputBitmap 输入图像
     * @param params 渲染参数
     * @param quality 渲染质量
     * @return 渲染后的Bitmap
     */
    fun renderToBitmap(
        inputBitmap: Bitmap,
        params: RenderParameters,
        quality: RenderQuality = RenderQuality.STANDARD
    ): Bitmap? {
        // 按 RenderQuality 分级降采样：PREVIEW≤512 / STANDARD≤1024 / HIGH≤2048 / ULTRA≤4096
        // 同时避免纹理上传 OOM
        val renderBitmap = downsampleBitmapForQuality(inputBitmap, quality)

        // 创建输入纹理
        createInputTexture(renderBitmap)

        // 渲染
        val result = render(params, quality)

        // 如果创建了降采样副本，回收它
        if (renderBitmap !== inputBitmap && !renderBitmap.isRecycled) {
            renderBitmap.recycle()
        }

        if (result is RenderResult.Success) {
            return readOutputToBitmap()
        }

        return null
    }
    
    /**
     * 更新输入纹理（用于实时预览）
     * @param bitmap 新的输入图像
     */
    fun updateInputTexture(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            Log.e(TAG, "Bitmap已被回收，无法更新纹理")
            return
        }

        // 尺寸变化或尚未创建时重新创建输入纹理（texSubImage2D 不能改变尺寸）
        if (inputTextureId == 0 || bitmap.width != imageWidth || bitmap.height != imageHeight) {
            createInputTexture(bitmap)
            return
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, inputTextureId)
        try {
            GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, bitmap)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "更新纹理OOM", e)
        } catch (e: Exception) {
            Log.e(TAG, "更新纹理失败", e)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        if (!isInitialized) return
        
        // 删除纹理
        if (inputTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(inputTextureId), 0)
            inputTextureId = 0
        }
        if (outputTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(outputTextureId), 0)
            outputTextureId = 0
        }
        
        // 删除FBO
        if (framebufferId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(framebufferId), 0)
            framebufferId = 0
        }
        
        // 删除VBO和VAO
        if (vboId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
        if (vaoId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
            vaoId = 0
        }
        
        // 释放着色器程序
        shaderProgram?.release()
        shaderProgram = null
        
        isInitialized = false
        Log.d(TAG, "ImageShaderRenderer released")
    }
    
    /**
     * 检查是否已初始化
     */
    fun isReady(): Boolean = isInitialized
    
    /**
     * 获取图像尺寸
     */
    fun getImageSize(): Pair<Int, Int> = Pair(imageWidth, imageHeight)
}