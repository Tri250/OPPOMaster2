package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * GPU渲染管理器
 * 单例模式，管理EGLContext和渲染线程
 * 
 * 功能：
 * - 管理EGLContext和OpenGL ES渲染环境
 * - 异步渲染，不阻塞UI线程
 * - 支持GPU Delegate加速
 * - 提供降级策略（GPU失败→CPU渲染）
 * - 支持实时预览
 */
class GPURenderManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "GPURenderManager"
        
        @Volatile
        private var instance: GPURenderManager? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(context: Context): GPURenderManager {
            return instance ?: synchronized(this) {
                instance ?: GPURenderManager(context.applicationContext).also { instance = it }
            }
        }
        
        // 渲染超时时间
        private const val RENDER_TIMEOUT_MS = 5000L
        
        // 最大重试次数
        private const val MAX_RETRY_COUNT = 3
    }
    
    // 渲染线程
    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    
    // EGL配置
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglConfig: EGLConfig? = null
    private var eglSurface: EGLSurface? = null
    
    // 图像渲染器
    private var imageRenderer: ImageShaderRenderer? = null
    
    // 状态管理
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    
    private val _isGpuAvailable = MutableStateFlow(false)
    val isGpuAvailable: StateFlow<Boolean> = _isGpuAvailable.asStateFlow()
    
    private val _renderQueueSize = MutableStateFlow(0)
    val renderQueueSize: StateFlow<Int> = _renderQueueSize.asStateFlow()
    
    // 渲染队列（用于异步渲染）
    private val renderChannel = Channel<RenderRequest>(capacity = 64)
    
    // 协程作用域
    private var renderScope: CoroutineScope? = null
    
    // CPU降级渲染器（备用）
    private var cpuFallbackRenderer: CPURenderer? = null
    
    /**
     * 初始化GPU渲染管理器
     * 在后台线程执行，不阻塞UI
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Default) {
        if (_isInitialized.value) {
            return@withContext true
        }
        
        try {
            // 创建渲染线程
            renderThread = HandlerThread("GPURenderThread")
            renderThread?.start()
            renderHandler = Handler(renderThread?.looper ?: Looper.getMainLooper())
            
            // 在渲染线程初始化EGL
            val initResult = initEGLOnRenderThread()
            
            if (!initResult) {
                Log.w(TAG, "EGL initialization failed, will use CPU fallback")
                _isGpuAvailable.value = false
            } else {
                _isGpuAvailable.value = true
                
                // 初始化图像渲染器
                imageRenderer = ImageShaderRenderer(context)
                val rendererInit = initRendererOnRenderThread()
                
                if (!rendererInit) {
                    Log.w(TAG, "ImageRenderer initialization failed")
                    _isGpuAvailable.value = false
                }
            }
            
            // 初始化CPU降级渲染器
            cpuFallbackRenderer = CPURenderer()
            
            // 启动渲染协程
            renderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            startRenderProcessor()
            
            _isInitialized.value = true
            Log.d(TAG, "GPURenderManager initialized, GPU available: ${_isGpuAvailable.value}")
            
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize GPURenderManager", e)
            _isInitialized.value = false
            _isGpuAvailable.value = false
            return@withContext false
        }
    }
    
    /**
     * 在渲染线程初始化EGL
     */
    private suspend fun initEGLOnRenderThread(): Boolean {
        return runOnRenderThreadBlocking {
            try {
                // 获取EGL显示
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                    Log.e(TAG, "Failed to get EGL display")
                    return@runOnRenderThreadBlocking false
                }
                
                // 初始化EGL
                val version = IntArray(2)
                if (!EGL14.eglInitialize(eglDisplay!!, version, 0, version, 1)) {
                    Log.e(TAG, "Failed to initialize EGL")
                    return@runOnRenderThreadBlocking false
                }
                
                // 选择EGL配置
                val configSpec = intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, // 使用ES2作为兼容
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                
                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                
                if (!EGL14.eglChooseConfig(
                    eglDisplay!!,
                    configSpec, 0,
                    configs, 0, 1,
                    numConfigs, 0
                ) || numConfigs[0] == 0) {
                    Log.e(TAG, "Failed to choose EGL config")
                    return@runOnRenderThreadBlocking false
                }
                
                eglConfig = configs[0]
                
                // 创建EGL上下文
                val contextAttribs = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL14.EGL_NONE
                )
                
                eglContext = EGL14.eglCreateContext(
                    eglDisplay!!,
                    eglConfig!!,
                    EGL14.EGL_NO_CONTEXT,
                    contextAttribs, 0
                )
                
                if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) {
                    Log.e(TAG, "Failed to create EGL context")
                    return@runOnRenderThreadBlocking false
                }
                
                // 创建PBuffer表面（离屏渲染）
                val surfaceAttribs = intArrayOf(
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
                )
                
                eglSurface = EGL14.eglCreatePbufferSurface(
                    eglDisplay!!,
                    eglConfig!!,
                    surfaceAttribs, 0
                )
                
                if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) {
                    Log.e(TAG, "Failed to create EGL surface")
                    return@runOnRenderThreadBlocking false
                }
                
                // 绑定上下文
                if (!EGL14.eglMakeCurrent(eglDisplay!!, eglSurface!!, eglSurface!!, eglContext!!)) {
                    Log.e(TAG, "Failed to make EGL context current")
                    return@runOnRenderThreadBlocking false
                }
                
                Log.d(TAG, "EGL initialized successfully")
                return@runOnRenderThreadBlocking true
                
            } catch (e: Exception) {
                Log.e(TAG, "EGL initialization error", e)
                return@runOnRenderThreadBlocking false
            }
        }
    }
    
    /**
     * 在渲染线程初始化渲染器
     */
    private suspend fun initRendererOnRenderThread(): Boolean {
        return runOnRenderThreadBlocking {
            imageRenderer?.initialize() ?: false
        }
    }
    
    /**
     * 启动渲染处理器
     */
    private fun startRenderProcessor() {
        renderScope?.launch {
            for (request in renderChannel) {
                processRenderRequest(request)
            }
        }
    }
    
    /**
     * 处理渲染请求
     */
    private suspend fun processRenderRequest(request: RenderRequest) {
        _renderQueueSize.value = _renderQueueSize.value.coerceAtLeast(1) - 1
        
        val result = if (_isGpuAvailable.value) {
            // GPU渲染
            renderWithGPU(request)
        } else {
            // CPU降级渲染
            renderWithCPU(request)
        }
        
        // 发送结果
        request.resultCallback?.invoke(result)
    }
    
    /**
     * GPU渲染
     */
    private suspend fun renderWithGPU(request: RenderRequest): RenderResult {
        return runOnRenderThreadBlocking {
            try {
                val startTime = System.currentTimeMillis()
                
                // 更新输入纹理
                imageRenderer?.updateInputTexture(request.inputBitmap)
                
                // 渲染
                val renderResult = imageRenderer?.render(request.params, request.quality)
                
                val processingTime = System.currentTimeMillis() - startTime
                
                when (renderResult) {
                    is RenderResult.Success -> {
                        // 读取输出
                        val outputBitmap = imageRenderer?.readOutputToBitmap()
                        if (outputBitmap != null) {
                            RenderResult.Success(renderResult.outputTextureId, processingTime, request.quality)
                        } else {
                            RenderResult.Error("Failed to read output bitmap")
                        }
                    }
                    is RenderResult.Error -> renderResult
                    else -> RenderResult.Error("Unknown render result")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "GPU render failed", e)
                // 尝试CPU降级
                renderWithCPU(request)
            }
        }
    }
    
    /**
     * CPU降级渲染（异步）
     * 修复 P0-5: 改为suspend函数，避免在主线程执行
     */
    private suspend fun renderWithCPU(request: RenderRequest): RenderResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val outputBitmap = cpuFallbackRenderer?.render(
                request.inputBitmap,
                request.params
            )
            
            val processingTime = System.currentTimeMillis() - startTime
            
            return if (outputBitmap != null) {
                RenderResult.FallbackToCPU("GPU unavailable or failed", processingTime)
            } else {
                RenderResult.Error("CPU render failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "CPU render failed", e)
            return RenderResult.Error("Both GPU and CPU render failed", e)
        }
    }
    
    /**
     * 异步渲染请求
     * @param inputBitmap 输入图像
     * @param params 渲染参数
     * @param quality 渲染质量
     * @param resultCallback 结果回调（可选）
     * @return 渲染请求ID
     */
    fun requestRenderAsync(
        inputBitmap: Bitmap,
        params: RenderParameters,
        quality: RenderQuality = RenderQuality.STANDARD,
        resultCallback: ((RenderResult) -> Unit)? = null
    ): String {
        val requestId = generateRequestId()

        val scope = renderScope
        if (scope == null || !scope.isActive) {
            resultCallback?.invoke(RenderResult.Error("RenderManager not initialized or already released"))
            return requestId
        }

        val request = RenderRequest(
            id = requestId,
            inputBitmap = inputBitmap,
            params = params,
            quality = quality,
            resultCallback = resultCallback
        )

        scope.launch {
            try {
                renderChannel.send(request)
                _renderQueueSize.value += 1
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enqueue render request", e)
                resultCallback?.invoke(RenderResult.Error("Failed to enqueue: ${e.message}"))
            }
        }

        return requestId
    }
    
    /**
     * 同步渲染（阻塞调用）
     * 用于需要立即获取结果的场景
     */
    suspend fun renderSync(
        inputBitmap: Bitmap,
        params: RenderParameters,
        quality: RenderQuality = RenderQuality.STANDARD
    ): RenderResult {
        return withTimeoutOrNull(RENDER_TIMEOUT_MS) {
            if (_isGpuAvailable.value) {
                renderWithGPU(RenderRequest("", inputBitmap, params, quality, null))
            } else {
                renderWithCPU(RenderRequest("", inputBitmap, params, quality, null))
            }
        } ?: RenderResult.Error("Render timeout")
    }
    
    /**
     * 快速预览渲染
     * 使用较低质量，适合实时预览
     */
    suspend fun renderPreview(
        inputBitmap: Bitmap,
        params: RenderParameters
    ): Bitmap? {
        val result = renderSync(inputBitmap, params, RenderQuality.PREVIEW)
        
        return when (result) {
            is RenderResult.Success -> imageRenderer?.readOutputToBitmap()
            is RenderResult.FallbackToCPU -> cpuFallbackRenderer?.render(inputBitmap, params)
            else -> null
        }
    }
    
    /**
     * 在渲染线程执行阻塞操作（使用suspendCancellableCoroutine替代runBlocking）
     */
    private suspend fun <T> runOnRenderThreadBlocking(block: () -> T): T {
        if (renderThread == null || renderHandler == null) {
            return block()
        }
        
        return suspendCancellableCoroutine { continuation ->
            renderHandler?.post {
                try {
                    val result = block()
                    if (continuation.isActive) {
                        continuation.resume(result) {}
                    }
                } catch (e: Exception) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
            }
            
            // 设置超时取消
            continuation.invokeOnCancellation {
                Log.w(TAG, "Render thread operation cancelled")
            }
        }
    }
    
    /**
     * 生成请求ID
     */
    private fun generateRequestId(): String {
        return "render_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    /**
     * 检查GPU是否可用
     */
    fun isGPUAvailable(): Boolean = _isGpuAvailable.value
    
    /**
     * 获取渲染队列大小
     */
    fun getQueueSize(): Int = _renderQueueSize.value
    
    /**
     * 清空渲染队列
     */
    fun clearQueue() {
        while (renderChannel.tryReceive().isSuccess) {
            // drain the channel
        }
        _renderQueueSize.value = 0
    }
    
    /**
     * 释放资源
     * 修复 P0-5: 移除双重runBlocking，改为在渲染线程直接执行
     * 避免在已经取消的协程作用域中使用runBlocking导致死锁
     */
    fun release() {
        // 停止渲染协程
        renderScope?.cancel()
        renderScope = null

        // 先清空队列，再关闭channel
        clearQueue()
        renderChannel.close()
        
        // 在渲染线程直接执行释放（避免runBlocking嵌套）
        renderHandler?.post {
            try {
                imageRenderer?.release()
                imageRenderer = null
                
                // 销毁EGL上下文
                val currentDisplay = eglDisplay
                val currentContext = eglContext
                val currentSurface = eglSurface
                
                if (currentDisplay != null && currentContext != null) {
                    EGL14.eglMakeCurrent(
                        currentDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                    EGL14.eglDestroyContext(currentDisplay, currentContext)
                    eglContext = null
                }
                
                // 销毁EGL表面
                if (currentDisplay != null && currentSurface != null) {
                    EGL14.eglDestroySurface(currentDisplay, currentSurface)
                    eglSurface = null
                }
                
                // 终止EGL显示
                if (currentDisplay != null) {
                    EGL14.eglTerminate(currentDisplay)
                    eglDisplay = null
                }
                
                Log.d(TAG, "EGL resources released on render thread")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing EGL resources", e)
            }
        }
        
        // 等待渲染线程处理完释放任务（带超时）
        renderThread?.let { thread ->
            try {
                thread.quitSafely()
                // 最多等待2秒
                thread.join(2000)
                if (thread.isAlive) {
                    Log.w(TAG, "Render thread did not terminate in time, forcing quit")
                    thread.interrupt()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while waiting for render thread", e)
            }
        }
        renderThread = null
        renderHandler = null
        
        _isInitialized.value = false
        _isGpuAvailable.value = false
        
        Log.d(TAG, "GPURenderManager released")
    }
}

/**
 * 渲染请求
 */
data class RenderRequest(
    val id: String,
    val inputBitmap: Bitmap,
    val params: RenderParameters,
    val quality: RenderQuality,
    val resultCallback: ((RenderResult) -> Unit)?
)

/**
 * CPU降级渲染器
 * 当GPU不可用时的备用渲染方案
 * 
 * 修复 P0-5: 改为异步渲染，避免在主线程处理大图片导致ANR
 */
class CPURenderer {
    
    /**
     * CPU渲染实现（异步）
     * 使用像素级处理实现基本效果
     * 修复：改为suspend函数，在后台线程执行
     */
    suspend fun render(inputBitmap: Bitmap, params: RenderParameters): Bitmap? = withContext(Dispatchers.Default) {
        try {
            // 大图片分块处理，避免单次处理时间过长
            val outputBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val width = outputBitmap.width
            val height = outputBitmap.height
            val totalPixels = width * height
            
            // 如果像素数超过阈值，使用分块处理
            val chunkSize = 500_000 // 每次处理50万像素
            
            if (totalPixels > chunkSize) {
                // 分块异步处理
                renderInChunks(outputBitmap, width, height, params, chunkSize)
            } else {
                // 小图片直接处理
                renderFullImage(outputBitmap, width, height, params)
            }
            
        } catch (e: Exception) {
            Log.e("CPURenderer", "CPU渲染失败", e)
            null
        }
    }
    
    /**
     * 分块渲染，避免ANR
     */
    private suspend fun renderInChunks(
        outputBitmap: Bitmap,
        width: Int,
        height: Int,
        params: RenderParameters,
        chunkSize: Int
    ): Bitmap? {
        val totalPixels = width * height
        val numChunks = (totalPixels + chunkSize - 1) / chunkSize
        
        for (chunkIndex in 0 until numChunks) {
            // 每处理一块就yield，让出线程
            yield()
            
            val startPixel = chunkIndex * chunkSize
            val endPixel = minOf(startPixel + chunkSize, totalPixels)
            
            processPixelChunk(outputBitmap, width, height, params, startPixel, endPixel)
        }
        
        return outputBitmap
    }
    
    /**
     * 处理指定范围的像素块
     */
    private fun processPixelChunk(
        outputBitmap: Bitmap,
        width: Int,
        height: Int,
        params: RenderParameters,
        startPixel: Int,
        endPixel: Int
    ) {
        val pixels = IntArray(endPixel - startPixel)
        
        // 将线性索引转换为二维坐标
        for (i in pixels.indices) {
            val pixelIndex = startPixel + i
            val x = pixelIndex % width
            val y = pixelIndex / width
            pixels[i] = outputBitmap.getPixel(x, y)
        }
        
        // 处理像素
        processPixels(pixels, params)
        
        // 写回
        for (i in pixels.indices) {
            val pixelIndex = startPixel + i
            val x = pixelIndex % width
            val y = pixelIndex / width
            outputBitmap.setPixel(x, y, pixels[i])
        }
    }
    
    /**
     * 完整图片渲染（小图片）
     */
    private fun renderFullImage(
        outputBitmap: Bitmap,
        width: Int,
        height: Int,
        params: RenderParameters
    ): Bitmap {
        val pixels = IntArray(width * height)
        outputBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        processPixels(pixels, params)
        outputBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }
    
    /**
     * 处理像素数组
     */
    private fun processPixels(pixels: IntArray, params: RenderParameters) {
        for (i in pixels.indices) {
            var pixel = pixels[i]
            
            // 解析RGBA
            var r = (pixel shr 16) and 0xFF
            var g = (pixel shr 8) and 0xFF
            var b = pixel and 0xFF
            
            // 应用亮度
            if (params.brightness != 0f) {
                val brightnessOffset = params.brightness * 2.55f
                r = (r + brightnessOffset).toInt().coerceIn(0, 255)
                g = (g + brightnessOffset).toInt().coerceIn(0, 255)
                b = (b + brightnessOffset).toInt().coerceIn(0, 255)
            }
            
            // 应用对比度
            if (params.contrast != 0f) {
                val contrastFactor = 1f + params.contrast / 100f
                r = ((r - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                g = ((g - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
                b = ((b - 128) * contrastFactor + 128).toInt().coerceIn(0, 255)
            }
            
            // 应用饱和度
            if (params.saturation != 0f) {
                val gray = 0.299f * r + 0.587f * g + 0.114f * b
                val saturationFactor = 1f + params.saturation / 100f
                r = (gray + (r - gray) * saturationFactor).toInt().coerceIn(0, 255)
                g = (gray + (g - gray) * saturationFactor).toInt().coerceIn(0, 255)
                b = (gray + (b - gray) * saturationFactor).toInt().coerceIn(0, 255)
            }
            
            // 应用色温
            if (params.warmth != 0f) {
                val warmthFactor = params.warmth / 100f
                if (warmthFactor > 0) {
                    r = (r + warmthFactor * 20).toInt().coerceIn(0, 255)
                    b = (b - warmthFactor * 20).toInt().coerceIn(0, 255)
                } else {
                    r = (r + warmthFactor * 20).toInt().coerceIn(0, 255)
                    b = (b - warmthFactor * 20).toInt().coerceIn(0, 255)
                }
            }
            
            // 应用曝光
            if (params.exposure != 0f) {
                val exposureFactor = kotlin.math.pow(2.0, (params.exposure / 50f).toDouble()).toFloat()
                r = (r * exposureFactor).toInt().coerceIn(0, 255)
                g = (g * exposureFactor).toInt().coerceIn(0, 255)
                b = (b * exposureFactor).toInt().coerceIn(0, 255)
            }
            
            // 组合像素
            pixels[i] = (pixel and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
        }
    }
}