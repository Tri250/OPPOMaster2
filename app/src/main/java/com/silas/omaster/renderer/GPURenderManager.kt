package com.silas.omaster.renderer

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.data.lut.LUT3DParser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * Bitmap 对象池，减少 GC 压力
 * 复用相同尺寸的 Bitmap，避免频繁分配和回收
 */
class BitmapPool(private val maxPoolSize: Int = 8) {
    private val pool = java.util.LinkedList<Bitmap>()

    @Synchronized
    fun obtain(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        // 防御性检查：如果池超出最大容量，清理多余条目
        while (pool.size > maxPoolSize) {
            pool.removeLast()?.let { if (!it.isRecycled) it.recycle() }
        }
        val iterator = pool.iterator()
        while (iterator.hasNext()) {
            val bitmap = iterator.next()
            if (bitmap.width == width && bitmap.height == height && bitmap.config == config) {
                iterator.remove()
                bitmap.eraseColor(0)
                return bitmap
            }
        }
        return Bitmap.createBitmap(width, height, config)
    }

    @Synchronized
    fun recycle(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        if (pool.size < maxPoolSize) {
            pool.add(bitmap)
        } else {
            bitmap.recycle()
        }
    }

    @Synchronized
    fun clear() {
        pool.forEach { it.recycle() }
        pool.clear()
    }

    @Synchronized
    fun size(): Int = pool.size
}

/**
 * 帧时序指标
 * 追踪渲染帧的处理时间，用于性能分析和优化
 */
object FrameTimingMetrics {
    private const val MAX_SAMPLES = 120
    private val frameTimes = ArrayDeque<Long>(MAX_SAMPLES)
    private var lastFrameTime: Long = 0L
    private var frameCount: Long = 0L

    // P95 缓存，避免每次调用都排序
    private var p95Cache: Long = 0L
    private var p95CacheValid: Boolean = false

    fun recordFrame(processingTimeMs: Long) {
        synchronized(frameTimes) {
            frameTimes.addLast(processingTimeMs)
            if (frameTimes.size > MAX_SAMPLES) {
                frameTimes.removeFirst()
            }
            // 新帧到来，使缓存失效
            p95CacheValid = false
        }
        lastFrameTime = processingTimeMs
        frameCount++
    }

    fun getAverageFrameTime(): Double {
        synchronized(frameTimes) {
            if (frameTimes.isEmpty()) return 0.0
            return frameTimes.average()
        }
    }

    fun getP95FrameTime(): Long {
        synchronized(frameTimes) {
            if (frameTimes.isEmpty()) return 0L
            if (!p95CacheValid) {
                val sorted = frameTimes.sorted()
                val index = (sorted.size * 0.95).toInt().coerceIn(0, sorted.size - 1)
                p95Cache = sorted[index]
                p95CacheValid = true
            }
            return p95Cache
        }
    }

    fun getLastFrameTime(): Long = lastFrameTime
    fun getFrameCount(): Long = frameCount

    fun getReport(): String {
        synchronized(frameTimes) {
            val avg = if (frameTimes.isEmpty()) 0.0 else frameTimes.average()
            val max = frameTimes.maxOrNull() ?: 0L
            val min = frameTimes.minOrNull() ?: 0L
            return "帧时序: 平均=${"%.1f".format(avg)}ms, " +
                    "P95=${getP95FrameTime()}ms, " +
                    "最大=${max}ms, 最小=${min}ms, " +
                    "总帧数=$frameCount"
        }
    }

    fun reset() {
        synchronized(frameTimes) {
            frameTimes.clear()
            lastFrameTime = 0L
            frameCount = 0L
            p95Cache = 0L
            p95CacheValid = false
        }
    }
}

/**
 * 渲染质量自适应管理器
 * 根据内存压力自动降级渲染质量
 */
class RenderQualityManager(private val context: Context) : ComponentCallbacks2 {

    /** 当前渲染质量等级 */
    @Volatile
    var currentQuality: RenderQuality = RenderQuality.STANDARD
        private set

    /** 是否允许自适应降级 */
    @Volatile
    var adaptiveQualityEnabled: Boolean = true

    /** 最低渲染质量（不会低于此级别） */
    @Volatile
    var minQuality: RenderQuality = RenderQuality.PREVIEW

    /** 质量预设对应的位图最大尺寸 */
    data class QualityPreset(
        val quality: RenderQuality,
        val maxWidth: Int,
        val maxHeight: Int,
        val sampleSize: Int
    )

    @Volatile
    var qualityPresets: Map<RenderQuality, QualityPreset> = mapOf(
        RenderQuality.ULTRA to QualityPreset(RenderQuality.ULTRA, 4096, 4096, 1),
        RenderQuality.HIGH to QualityPreset(RenderQuality.HIGH, 2048, 2048, 1),
        RenderQuality.STANDARD to QualityPreset(RenderQuality.STANDARD, 1024, 1024, 2),
        RenderQuality.PREVIEW to QualityPreset(RenderQuality.PREVIEW, 512, 512, 4)
    )

    init {
        registerMemoryCallbacks()
    }

    private var registered = false

    private fun registerMemoryCallbacks() {
        if (!registered) {
            context.applicationContext.registerComponentCallbacks(this)
            registered = true
        }
    }

    fun unregister() {
        if (registered) {
            context.applicationContext.unregisterComponentCallbacks(this)
            registered = false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        if (adaptiveQualityEnabled) {
            downgradeQuality(RenderQuality.PREVIEW)
            Log.w("RenderQualityManager", "onLowMemory: 降级到 PREVIEW")
        }
    }

    override fun onTrimMemory(level: Int) {
        if (!adaptiveQualityEnabled) return

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                downgradeQuality(RenderQuality.PREVIEW)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                downgradeQuality(RenderQuality.STANDARD)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                if (currentQuality == RenderQuality.ULTRA) {
                    downgradeQuality(RenderQuality.HIGH)
                }
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                downgradeQuality(RenderQuality.PREVIEW)
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                downgradeQuality(RenderQuality.PREVIEW)
            }
        }
    }

    private fun downgradeQuality(target: RenderQuality) {
        val targetQuality = if (target.ordinal < minQuality.ordinal) minQuality else target
        // 修复：lower ordinal = higher quality，降级意味从低 ord 移向高 ord
        // 只有当 currentQuality.ordinal > targetQuality.ordinal 时才需要降级
        if (currentQuality.ordinal > targetQuality.ordinal) {
            currentQuality = targetQuality
            Log.w("RenderQualityManager", "质量降级: $currentQuality → $targetQuality")
        }
    }

    fun restoreQuality() {
        if (adaptiveQualityEnabled) {
            currentQuality = RenderQuality.STANDARD
            Log.d("RenderQualityManager", "质量恢复: STANDARD")
        }
    }

    fun getCurrentPreset(): QualityPreset {
        return qualityPresets[currentQuality] ?: qualityPresets.getValue(RenderQuality.STANDARD)
    }
}

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

        private val refCount = java.util.concurrent.atomic.AtomicInteger(0)

        /**
         * 获取单例实例（不增加引用计数）。
         * 若需要持有并在生命周期结束时释放，请使用 [acquire]。
         */
        fun getInstance(context: Context): GPURenderManager {
            return instance ?: synchronized(this) {
                instance ?: GPURenderManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * 获取单例实例并增加引用计数。
         * 调用者必须在生命周期结束时调用 [GPURenderManager.release] 配对释放，
         * 当引用计数归零时才会真正销毁底层 EGL/线程资源。
         */
        fun acquire(context: Context): GPURenderManager {
            val manager = getInstance(context)
            val count = refCount.incrementAndGet()
            Log.d(TAG, "acquire refCount=$count")
            return manager
        }
        
        // 渲染超时时间
        private const val RENDER_TIMEOUT_MS = 5000L
        
        // 最大重试次数
        private const val MAX_RETRY_COUNT = 3

        // 渲染线程退出超时时间
        private const val THREAD_QUIT_TIMEOUT_MS = 2000L
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
    private val renderChannel = Channel<RenderRequest>(capacity = Channel.UNLIMITED)
    
    // 协程作用域
    private var renderScope: CoroutineScope? = null
    
    // CPU降级渲染器（备用）
    private var cpuFallbackRenderer: CPURenderer? = null

    // 当前已上传的 3D LUT 纹理 ID（0 表示无），用于主渲染管线内的 GPU LUT 路径
    private var lutTextureId: Int = 0

    // ==================== 性能优化组件 ====================

    /** Bitmap 对象池，减少 GC 压力 */
    val bitmapPool: BitmapPool = BitmapPool(maxPoolSize = 8)

    /** 渲染质量自适应管理器 */
    val qualityManager: RenderQualityManager = RenderQualityManager(context)
    
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

            // 等待渲染线程的 Looper 准备完成，避免回退到主线程
            var looperRetry = 0
            while (renderThread?.looper == null && looperRetry < 100) {
                delay(10)
                looperRetry++
            }
            val looper = renderThread?.looper
            if (looper == null) {
                Log.e(TAG, "Failed to prepare render thread looper")
                _isInitialized.value = false
                _isGpuAvailable.value = false
                return@withContext false
            }
            renderHandler = Handler(looper)
            
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
                val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                if (display == EGL14.EGL_NO_DISPLAY) {
                    Log.e(TAG, "Failed to get EGL display")
                    return@runOnRenderThreadBlocking false
                }
                eglDisplay = display
                
                // 初始化EGL
                val version = IntArray(2)
                if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
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
                    display,
                    configSpec, 0,
                    configs, 0, 1,
                    numConfigs, 0
                ) || numConfigs[0] == 0) {
                    Log.e(TAG, "Failed to choose EGL config")
                    return@runOnRenderThreadBlocking false
                }

                val config = configs[0]
                if (config == null) {
                    Log.e(TAG, "EGL config is null")
                    return@runOnRenderThreadBlocking false
                }
                eglConfig = config

                // 创建EGL上下文
                val contextAttribs = intArrayOf(
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                    EGL14.EGL_NONE
                )

                val context = EGL14.eglCreateContext(
                    display,
                    config,
                    EGL14.EGL_NO_CONTEXT,
                    contextAttribs, 0
                )

                if (context == null || context == EGL14.EGL_NO_CONTEXT) {
                    Log.e(TAG, "Failed to create EGL context")
                    return@runOnRenderThreadBlocking false
                }
                eglContext = context

                // 创建PBuffer表面（离屏渲染）
                val surfaceAttribs = intArrayOf(
                    EGL14.EGL_WIDTH, 1,
                    EGL14.EGL_HEIGHT, 1,
                    EGL14.EGL_NONE
                )

                val surface = EGL14.eglCreatePbufferSurface(
                    display,
                    config,
                    surfaceAttribs, 0
                )

                if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
                    Log.e(TAG, "Failed to create EGL surface")
                    return@runOnRenderThreadBlocking false
                }
                eglSurface = surface

                // 绑定上下文
                if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
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
        _renderQueueSize.value = (_renderQueueSize.value - 1).coerceAtLeast(0)
        
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
        return try {
            runOnRenderThreadBlocking {
                val startTime = SystemClock.elapsedRealtime()

                // 使用自适应质量（如果启用）
                val effectiveQuality = if (qualityManager.adaptiveQualityEnabled) {
                    qualityManager.currentQuality
                } else {
                    request.quality
                }

                // 按 RenderQuality 分级降采样输入图，平衡性能与质量
                val sourceBitmap = request.inputBitmap
                val renderBitmap = ImageShaderRenderer.downsampleBitmapForQuality(sourceBitmap, effectiveQuality)

                // 更新输入纹理
                imageRenderer?.updateInputTexture(renderBitmap)

                // 渲染（setRenderParameters 内部会根据 params.lutEnabled 绑定 LUT 纹理到 GL_TEXTURE2）
                val renderResult = imageRenderer?.render(request.params, effectiveQuality)

                // 回收降采样产生的临时副本（原始 request.inputBitmap 由调用方管理）
                if (renderBitmap !== sourceBitmap && !renderBitmap.isRecycled) {
                    renderBitmap.recycle()
                }

                val processingTime = SystemClock.elapsedRealtime() - startTime

                // 记录帧时序
                FrameTimingMetrics.recordFrame(processingTime)

                when (renderResult) {
                    is RenderResult.Success -> {
                        // 读取输出位图并封装进结果
                        val outputBitmap = imageRenderer?.readOutputToBitmap()
                        if (outputBitmap != null) {
                            RenderResult.Success(
                                outputTextureId = renderResult.outputTextureId,
                                processingTimeMs = processingTime,
                                quality = effectiveQuality,
                                outputBitmap = outputBitmap
                            )
                        } else {
                            RenderResult.Error("Failed to read output bitmap")
                        }
                    }
                    is RenderResult.Error -> renderResult
                    else -> RenderResult.Error("Unknown render result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "GPU render failed", e)
            // 尝试CPU降级
            renderWithCPU(request)
        }
    }
    
    /**
     * CPU降级渲染（异步）
     * 修复 P0-5: 改为suspend函数，避免在主线程执行
     */
    private suspend fun renderWithCPU(request: RenderRequest): RenderResult {
        val startTime = SystemClock.elapsedRealtime()
        
        try {
            val outputBitmap = cpuFallbackRenderer?.render(
                request.inputBitmap,
                request.params
            )
            
            val processingTime = SystemClock.elapsedRealtime() - startTime

            // 记录帧时序
            FrameTimingMetrics.recordFrame(processingTime)
            
            return if (outputBitmap != null) {
                RenderResult.FallbackToCPU(
                    reason = "GPU unavailable or failed",
                    processingTimeMs = processingTime,
                    outputBitmap = outputBitmap
                )
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
        
        val request = RenderRequest(
            id = requestId,
            inputBitmap = inputBitmap,
            params = params,
            quality = quality,
            resultCallback = resultCallback
        )
        
        renderScope?.launch {
            renderChannel.send(request)
            _renderQueueSize.value += 1
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
            is RenderResult.Success -> result.outputBitmap
            is RenderResult.FallbackToCPU -> result.outputBitmap
            else -> null
        }
    }

    /**
     * 上传 3D LUT 数据为 GL 纹理，返回纹理 ID。
     *
     * 必须在 GPU 可用后调用。内部在渲染线程执行纹理上传，
     * 返回的纹理 ID 可设置到 [RenderParameters.lutTextureId]，
     * 由主渲染管线内的 [ShaderProgram.setLUT3DParams] 绑定到 GL_TEXTURE2。
     *
     * @param lutData 3D LUT 数据
     * @return 纹理 ID，失败返回 0
     */
    suspend fun uploadLUT3DTexture(lutData: LUT3DData): Int {
        if (!_isGpuAvailable.value) {
            Log.w(TAG, "GPU 不可用，无法上传 LUT 纹理")
            return 0
        }
        return runOnRenderThreadBlocking {
            try {
                // 释放上一次上传的 LUT 纹理，避免泄漏
                if (lutTextureId != 0) {
                    GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                    lutTextureId = 0
                }

                // 编码为 2D 纹理 Bitmap（RGBA8，兼容性最佳）
                val bitmap = LUT3DParser.encodeToBitmap(lutData.data, lutData.size)

                val textures = IntArray(1)
                GLES30.glGenTextures(1, textures, 0)
                if (textures[0] == 0) {
                    Log.e(TAG, "glGenTextures 失败 for LUT")
                    bitmap.recycle()
                    return@runOnRenderThreadBlocking 0
                }
                lutTextureId = textures[0]

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, lutTextureId)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

                bitmap.recycle()

                val error = GLES30.glGetError()
                if (error != GLES30.GL_NO_ERROR) {
                    Log.e(TAG, "LUT 纹理上传 GL 错误: 0x${error.toString(16)}")
                    GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                    lutTextureId = 0
                    return@runOnRenderThreadBlocking 0
                }

                Log.d(TAG, "LUT 纹理上传成功: id=$lutTextureId, size=${lutData.size}")
                lutTextureId
            } catch (e: Exception) {
                Log.e(TAG, "上传 LUT 纹理失败", e)
                0
            }
        }
    }

    /**
     * 释放当前已上传的 3D LUT 纹理。
     * 在渲染线程执行，确保 GL 资源正确释放。
     */
    suspend fun releaseLUT3DTexture() {
        if (lutTextureId == 0) return
        runOnRenderThreadBlocking {
            if (lutTextureId != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                lutTextureId = 0
                Log.d(TAG, "LUT 纹理已释放")
            }
        }
    }

    /**
     * 获取当前已上传的 LUT 纹理 ID（0 表示无）
     */
    fun getLUT3DTextureId(): Int = lutTextureId
    
    /**
     * 在渲染线程执行阻塞操作（使用suspendCancellableCoroutine替代runBlocking）
     *
     * 修复：若 renderHandler 在检查非空后、post 前被 release 置为 null，
     * 或 Handler 对应的 Looper 已 quit 导致 post 失败，
     * 必须在当前线程立即执行 block，避免 continuation 永远挂起。
     */
    private suspend fun <T> runOnRenderThreadBlocking(block: () -> T): T {
        val handler = renderHandler
        if (renderThread == null || handler == null) {
            return block()
        }

        return suspendCancellableCoroutine { continuation ->
            val posted = try {
                handler.post {
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
            } catch (e: Exception) {
                Log.e(TAG, "Handler post failed", e)
                false
            }

            if (!posted) {
                // Handler 已失效，立即在当前线程执行避免协程泄漏
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

            continuation.invokeOnCancellation {
                Log.w(TAG, "Render thread operation cancelled")
            }
        }
    }
    
    /**
     * 生成请求ID
     * 基于时间戳+计数器（避免 Random 在 Compose Recomposition 中产生不同的 ID）
     */
    private val requestIdCounter = java.util.concurrent.atomic.AtomicLong(0)
    private fun generateRequestId(): String {
        val counter = requestIdCounter.incrementAndGet()
        return "render_${System.currentTimeMillis()}_$counter"
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
     * 在渲染线程上清理 EGL 资源
     */
    private fun cleanupEGLOnRenderThread() {
        try {
            // 释放 LUT 纹理
            if (lutTextureId != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                lutTextureId = 0
            }

            imageRenderer?.release()
            imageRenderer = null

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

            if (currentDisplay != null && currentSurface != null) {
                EGL14.eglDestroySurface(currentDisplay, currentSurface)
                eglSurface = null
            }

            if (currentDisplay != null) {
                EGL14.eglTerminate(currentDisplay)
                eglDisplay = null
            }

            Log.d(TAG, "EGL resources released on render thread")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing EGL resources on render thread", e)
        }
    }

    /**
     * 在当前线程清理 EGL 资源（超时兜底）
     */
    private fun cleanupEGLOnCurrentThread() {
        try {
            Log.w(TAG, "Cleaning up EGL resources on current thread (timeout fallback)")

            // 释放 LUT 纹理
            if (lutTextureId != 0) {
                GLES30.glDeleteTextures(1, intArrayOf(lutTextureId), 0)
                lutTextureId = 0
            }

            imageRenderer?.release()
            imageRenderer = null

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

            if (currentDisplay != null && currentSurface != null) {
                EGL14.eglDestroySurface(currentDisplay, currentSurface)
                eglSurface = null
            }

            if (currentDisplay != null) {
                EGL14.eglTerminate(currentDisplay)
                eglDisplay = null
            }

            Log.d(TAG, "EGL resources released on current thread (fallback)")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing EGL resources on current thread", e)
        }
    }

    /**
     * 释放资源（引用计数版）。
     * 仅当引用计数归零时才会真正销毁底层 EGL/线程资源。
     * 修复 P0-5: 移除双重runBlocking，改为在渲染线程直接执行
     * 避免在已经取消的协程作用域中使用runBlocking导致死锁
     *
     * 修复 null safety: 先捕获 handler 引用，确保 EGL 清理在 quit 前投递
     * 修复 EGL 清理: 加入超时兜底，若渲染线程超时未退出则在当前线程清理
     * 修复 Channel 关闭: 先取消协程作用域再关闭 Channel
     */
    fun release() {
        val count = refCount.decrementAndGet()
        Log.d(TAG, "release refCount=$count")
        if (count > 0) {
            // 仍有其他持有者，不销毁资源
            return
        }
        if (count < 0) {
            Log.w(TAG, "release called more times than acquire, resetting refCount")
            refCount.set(0)
            return
        }

        // 1. 先取消渲染协程作用域，确保 startRenderProcessor 的协程停止
        renderScope?.cancel()
        renderScope = null

        // 2. 清空渲染队列
        clearQueue()

        // 3. 关闭 Channel（协程已取消，不会再有新的生产/消费）
        renderChannel.close()

        // 4. 清理 BitmapPool
        bitmapPool.clear()

        // 5. 注销质量管理器
        qualityManager.unregister()

        // 6. 捕获 handler 引用，避免在 quit 后被置 null 导致 EGL 泄漏
        val handler = renderHandler

        if (handler != null) {
            // 在渲染线程上执行 EGL 清理（先投递，再 quit，quitSafely 会处理完挂起消息）
            handler.post {
                cleanupEGLOnRenderThread()
            }
        } else {
            // renderHandler 为 null，在当前线程直接清理 EGL
            cleanupEGLOnCurrentThread()
        }

        // 7. 退出渲染线程（带超时兜底）
        renderThread?.let { thread ->
            try {
                thread.quitSafely()
                thread.join(THREAD_QUIT_TIMEOUT_MS)
                if (thread.isAlive) {
                    Log.w(TAG, "Render thread did not terminate in ${THREAD_QUIT_TIMEOUT_MS}ms, cleaning up EGL on current thread")
                    // 超时兜底：在当前线程清理 EGL 资源
                    cleanupEGLOnCurrentThread()
                    thread.interrupt()
                }
            } catch (e: InterruptedException) {
                Log.e(TAG, "Interrupted while waiting for render thread", e)
                // 被中断时也在当前线程清理
                cleanupEGLOnCurrentThread()
                thread.interrupt()
            }
        }
        renderThread = null
        renderHandler = null

        _isInitialized.value = false
        _isGpuAvailable.value = false

        // 真正销毁单例引用，下次 acquire 会重新创建
        synchronized(GPURenderManager::class.java) {
            instance = null
        }

        Log.d(TAG, "GPURenderManager fully released")
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
     *
     * 按照着色器（image_adjust.frag）的执行顺序组织处理流程：
     * 1. 降噪（卷积）
     * 2. 肤色平滑（卷积）
     * 3. 逐像素基础/色彩/光影调整 + 清晰度 + 效果（去霾/褪色/颗粒）
     * 4. 纹理增强（卷积）
     * 5. 锐化（卷积）
     */
    suspend fun render(inputBitmap: Bitmap, params: RenderParameters): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val outputBitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val width = outputBitmap.width
            val height = outputBitmap.height

            // ========== 1. 降噪（卷积，最先执行） ==========
            if (params.denoise > 0.01f) {
                applyDenoisePass(outputBitmap, width, height, params.denoise / 100f)
                yield()
            }

            // ========== 2. 肤色平滑（卷积） ==========
            if (params.skinSmooth > 0.01f) {
                applySkinSmoothPass(outputBitmap, width, height, params.skinSmooth / 100f)
                yield()
            }

            // ========== 3. 逐像素操作 ==========
            // 包含：曝光、亮度、对比度、饱和度、鲜艳度、色温、
            //       高光、阴影、白色、黑色、清晰度、去霾、褪色、颗粒
            val totalPixels = width * height
            val chunkSize = 500_000 // 每次处理50万像素

            if (totalPixels > chunkSize) {
                // 分块异步处理
                renderInChunks(outputBitmap, width, height, params, chunkSize)
            } else {
                // 小图片直接处理
                renderFullImage(outputBitmap, width, height, params)
            }

            // ========== 4. 纹理增强（卷积，读取当前工作图） ==========
            if (params.texture != 0f) {
                applyTexturePass(outputBitmap, width, height, params.texture / 100f)
                yield()
            }

            // ========== 5. 锐化（卷积，读取当前工作图） ==========
            if (params.sharpness > 0.01f) {
                applySharpenPass(outputBitmap, width, height, params.sharpness / 100f)
                yield()
            }

            outputBitmap

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
     * 处理像素数组（逐像素操作）
     *
     * 按照着色器（image_adjust.frag）的执行顺序实现以下参数：
     * - 曝光、亮度、对比度（基础调整）
     * - 饱和度、鲜艳度、色温（色彩调整）
     * - 高光、阴影、白色、黑色（光影调整）
     * - 清晰度（逐像素自适应对比度增强）
     * - 去霾、褪色、颗粒（效果处理）
     *
     * 注：降噪、肤色平滑、纹理增强、锐化为卷积操作，在 [render] 中以独立 pass 执行。
     */
    private fun processPixels(pixels: IntArray, params: RenderParameters) {
        // 归一化参数到 [-1, 1] 或 [0, 1]（与着色器 uniform 一致）
        val exposure = params.exposure / 100f
        val brightness = params.brightness / 100f
        val contrast = params.contrast / 100f
        val saturation = params.saturation / 100f
        val vibrance = params.vibrance / 100f
        val warmth = params.warmth / 100f
        val tint = params.tint / 100f
        val highlights = params.highlights / 100f
        val shadows = params.shadows / 100f
        val whites = params.whites / 100f
        val blacks = params.blacks / 100f
        val clarity = params.clarity / 100f
        val dehaze = params.dehaze / 100f
        val fade = params.fade / 100f
        val grain = params.grain / 100f
        val vignette = params.vignette / 100f
        val vignetteMidpoint = params.vignetteMidpoint / 100f

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = (pixel ushr 24) and 0xFF
            var r = ((pixel ushr 16) and 0xFF) / 255f
            var g = ((pixel ushr 8) and 0xFF) / 255f
            var b = (pixel and 0xFF) / 255f

            // ========== 3. 基础调整 ==========

            // 曝光: color * pow(2.0, uExposure)
            if (abs(exposure) > 0.01f) {
                val factor = 2.0.pow(exposure.toDouble()).toFloat()
                r *= factor; g *= factor; b *= factor
            }

            // 亮度: color + uBrightness * 0.5
            if (abs(brightness) > 0.01f) {
                val offset = brightness * 0.5f
                r += offset; g += offset; b += offset
            }

            // 对比度: mid + (color - mid) * (1.0 + uContrast)
            if (abs(contrast) > 0.01f) {
                val factor = 1f + contrast
                r = 0.5f + (r - 0.5f) * factor
                g = 0.5f + (g - 0.5f) * factor
                b = 0.5f + (b - 0.5f) * factor
            }

            // ========== 4. 色彩调整 ==========

            // 饱和度（HSL 空间）
            if (abs(saturation) > 0.01f) {
                val hsl = rgb2hsl(r, g, b)
                hsl[1] = (hsl[1] + saturation).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // 鲜艳度：饱和度越低，调整越强（保护已饱和色）
            if (abs(vibrance) > 0.01f) {
                val hsl = rgb2hsl(r, g, b)
                val vibranceAmount = (1f - hsl[1]) * vibrance
                hsl[1] = (hsl[1] + vibranceAmount * 0.5f).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // 色温: r += warmth * 0.1, b -= warmth * 0.1
            if (abs(warmth) > 0.01f) {
                r += warmth * 0.1f
                b -= warmth * 0.1f
            }

            // 色调（Tint）: g += tint * 0.1, b -= tint * 0.1
            if (abs(tint) > 0.01f) {
                g += tint * 0.1f
                b -= tint * 0.1f
            }

            // ========== 5. 光影调整（基于亮度遮罩） ==========

            // 高光：只调整高亮区域（亮度 > 0.5）
            if (abs(highlights) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = smoothstep(0.5f, 1.0f, lum)
                val adjR = r * (1f + highlights * mask)
                val adjG = g * (1f + highlights * mask)
                val adjB = b * (1f + highlights * mask)
                r = mix(r, adjR, mask); g = mix(g, adjG, mask); b = mix(b, adjB, mask)
            }

            // 阴影：只调整暗部区域（亮度 < 0.5）
            if (abs(shadows) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = smoothstep(0.5f, 0.0f, lum)
                val adjR = r + shadows * mask * 0.3f
                val adjG = g + shadows * mask * 0.3f
                val adjB = b + shadows * mask * 0.3f
                r = mix(r, adjR, mask); g = mix(g, adjG, mask); b = mix(b, adjB, mask)
            }

            // 白色色阶：调整最亮区域（亮度 > 0.7）
            if (abs(whites) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = smoothstep(0.7f, 1.0f, lum)
                val adjR = 1f - (1f - r) * (1f - whites * mask)
                val adjG = 1f - (1f - g) * (1f - whites * mask)
                val adjB = 1f - (1f - b) * (1f - whites * mask)
                r = mix(r, adjR, mask); g = mix(g, adjG, mask); b = mix(b, adjB, mask)
            }

            // 黑色色阶：调整最暗区域（亮度 < 0.3）
            if (abs(blacks) > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val mask = smoothstep(0.3f, 0.0f, lum)
                val adjR = r * (1f + blacks * mask)
                val adjG = g * (1f + blacks * mask)
                val adjB = b * (1f + blacks * mask)
                r = mix(r, adjR, mask); g = mix(g, adjG, mask); b = mix(b, adjB, mask)
            }

            // ========== 6. 清晰度增强（逐像素，基于亮度自适应对比度） ==========
            if (clarity > 0.01f) {
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val adaptiveStrength = clarity * (1f - abs(lum - 0.5f) * 0.5f)
                val newR = 0.5f + (r - 0.5f) * (1f + adaptiveStrength * 2f)
                val newG = 0.5f + (g - 0.5f) * (1f + adaptiveStrength * 2f)
                val newB = 0.5f + (b - 0.5f) * (1f + adaptiveStrength * 2f)
                r = mix(r, newR, clarity); g = mix(g, newG, clarity); b = mix(b, newB, clarity)
            }

            // ========== 7. 效果处理 ==========

            // 去霾：基于雾度增加对比度和饱和度
            if (dehaze > 0.01f) {
                val hsl = rgb2hsl(r, g, b)
                val fogLevel = hsl[2] * (1f - hsl[1])
                val ds = dehaze * fogLevel
                r = 0.5f + (r - 0.5f) * (1f + ds)
                g = 0.5f + (g - 0.5f) * (1f + ds)
                b = 0.5f + (b - 0.5f) * (1f + ds)
                val hsl2 = rgb2hsl(r, g, b)
                hsl2[1] = (hsl2[1] + ds * 0.5f).coerceIn(0f, 1f)
                val rgb = hsl2rgb(hsl2[0], hsl2[1], hsl2[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // 褪色：降低对比度 + 提亮暗部 + 降低饱和度
            if (fade > 0.01f) {
                r = 0.5f + (r - 0.5f) * (1f - fade * 0.3f)
                g = 0.5f + (g - 0.5f) * (1f - fade * 0.3f)
                b = 0.5f + (b - 0.5f) * (1f - fade * 0.3f)
                r = mix(r, r + 0.1f * fade, fade)
                g = mix(g, g + 0.1f * fade, fade)
                b = mix(b, b + 0.1f * fade, fade)
                val hsl = rgb2hsl(r, g, b)
                hsl[1] = hsl[1] * (1f - fade * 0.2f)
                val rgb = hsl2rgb(hsl[0], hsl[1], hsl[2])
                r = rgb[0]; g = rgb[1]; b = rgb[2]
            }

            // 胶片颗粒：添加伪随机噪声（暗部颗粒更多）
            if (grain > 0.01f) {
                val noiseRaw = abs((sin(i * 12.9898 + 78.233) * 43758.5453) % 1.0)
                val noise = noiseRaw.toFloat() * 2f - 1f
                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val gs = grain * (1f + (1f - lum) * 0.5f)
                r += noise * gs * 0.15f
                g += noise * gs * 0.15f
                b += noise * gs * 0.15f
            }

            // 暗角（Vignette）：基于到中心的距离压暗边缘
            if (vignette > 0.01f) {
                val x = (i % width).toFloat() / width
                val y = (i / width).toFloat() / height
                val dx = x - 0.5f
                val dy = y - 0.5f
                val dist = kotlin.math.sqrt(dx * dx + dy * dy) * 1.41421356f // 归一化到 [0,1]
                val vignetteMask = smoothstep(vignetteMidpoint, 1.0f, dist) * vignette * 0.8f
                r *= (1f - vignetteMask)
                g *= (1f - vignetteMask)
                b *= (1f - vignetteMask)
            }

            // 钳制并写回
            pixels[i] = (a shl 24) or
                    ((r.coerceIn(0f, 1f) * 255f).toInt() shl 16) or
                    ((g.coerceIn(0f, 1f) * 255f).toInt() shl 8) or
                    (b.coerceIn(0f, 1f) * 255f).toInt()
        }
    }

    // ==================== 卷积 pass（降噪 / 肤色平滑 / 纹理 / 锐化） ====================

    /**
     * 降噪 pass：盒式模糊后按强度混合回工作图
     */
    private fun applyDenoisePass(bitmap: Bitmap, width: Int, height: Int, strength: Float) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val blurred = boxBlurPixels(pixels, width, height, 2)

        for (i in pixels.indices) {
            val p = pixels[i]
            val bp = blurred[i]
            val r = mix(((p ushr 16) and 0xFF) / 255f, ((bp ushr 16) and 0xFF) / 255f, strength).coerceIn(0f, 1f)
            val g = mix(((p ushr 8) and 0xFF) / 255f, ((bp ushr 8) and 0xFF) / 255f, strength).coerceIn(0f, 1f)
            val b = mix((p and 0xFF) / 255f, (bp and 0xFF) / 255f, strength).coerceIn(0f, 1f)
            val a = (p ushr 24) and 0xFF
            pixels[i] = (a shl 24) or ((r * 255f).toInt() shl 16) or ((g * 255f).toInt() shl 8) or (b * 255f).toInt()
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 肤色平滑 pass：对肤色区域进行模糊并混合
     */
    private fun applySkinSmoothPass(bitmap: Bitmap, width: Int, height: Int, strength: Float) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val blurred = boxBlurPixels(pixels, width, height, 3)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((p ushr 16) and 0xFF) / 255f
            val g = ((p ushr 8) and 0xFF) / 255f
            val b = (p and 0xFF) / 255f

            if (isSkinColor(r, g, b)) {
                val bp = blurred[i]
                val br = ((bp ushr 16) and 0xFF) / 255f
                val bg = ((bp ushr 8) and 0xFF) / 255f
                val bb = (bp and 0xFF) / 255f
                val t = strength * 0.5f
                val nr = mix(r, br, t).coerceIn(0f, 1f)
                val ng = mix(g, bg, t).coerceIn(0f, 1f)
                val nb = mix(b, bb, t).coerceIn(0f, 1f)
                val a = (p ushr 24) and 0xFF
                pixels[i] = (a shl 24) or ((nr * 255f).toInt() shl 16) or ((ng * 255f).toInt() shl 8) or (nb * 255f).toInt()
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    /**
     * 纹理增强 pass：高通滤波提取细节并叠加（strength 可为负值以平滑）
     */
    private fun applyTexturePass(bitmap: Bitmap, width: Int, height: Int, strength: Float) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = pixels.copyOf()

        // 3x3 高斯加权核
        val weights = floatArrayOf(
            0.0625f, 0.125f, 0.0625f,
            0.125f, 0.25f, 0.125f,
            0.0625f, 0.125f, 0.0625f
        )

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val center = pixels[i]
                val cr = ((center ushr 16) and 0xFF) / 255f
                val cg = ((center ushr 8) and 0xFF) / 255f
                val cb = (center and 0xFF) / 255f

                var sr = 0f; var sg = 0f; var sb = 0f
                var wi = 0
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val np = pixels[ny * width + nx]
                        val w = weights[wi++]
                        sr += ((np ushr 16) and 0xFF) / 255f * w
                        sg += ((np ushr 8) and 0xFF) / 255f * w
                        sb += (np and 0xFF) / 255f * w
                    }
                }

                // 高频细节 = 中心 - 模糊
                val nr = (cr + (cr - sr) * strength * 2f).coerceIn(0f, 1f)
                val ng = (cg + (cg - sg) * strength * 2f).coerceIn(0f, 1f)
                val nb = (cb + (cb - sb) * strength * 2f).coerceIn(0f, 1f)
                val a = (center ushr 24) and 0xFF
                output[i] = (a shl 24) or ((nr * 255f).toInt() shl 16) or ((ng * 255f).toInt() shl 8) or (nb * 255f).toInt()
            }
        }
        bitmap.setPixels(output, 0, width, 0, 0, width, height)
    }

    /**
     * 锐化 pass：Laplacian 锐化（与着色器 sharpen 一致：center + s * (center*8 - neighbors)）
     */
    private fun applySharpenPass(bitmap: Bitmap, width: Int, height: Int, strength: Float) {
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val output = pixels.copyOf()
        val s = strength * 0.5f // 与着色器一致: uSharpness * 0.5

        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val center = pixels[i]
                val cr = ((center ushr 16) and 0xFF) / 255f
                val cg = ((center ushr 8) and 0xFF) / 255f
                val cb = (center and 0xFF) / 255f

                var sr = 0f; var sg = 0f; var sb = 0f
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        if (dx == 0 && dy == 0) continue
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val np = pixels[ny * width + nx]
                        sr += ((np ushr 16) and 0xFF) / 255f
                        sg += ((np ushr 8) and 0xFF) / 255f
                        sb += (np and 0xFF) / 255f
                    }
                }

                val nr = (cr + s * (cr * 8f - sr)).coerceIn(0f, 1f)
                val ng = (cg + s * (cg * 8f - sg)).coerceIn(0f, 1f)
                val nb = (cb + s * (cb * 8f - sb)).coerceIn(0f, 1f)
                val a = (center ushr 24) and 0xFF
                output[i] = (a shl 24) or ((nr * 255f).toInt() shl 16) or ((ng * 255f).toInt() shl 8) or (nb * 255f).toInt()
            }
        }
        bitmap.setPixels(output, 0, width, 0, 0, width, height)
    }

    // ==================== 辅助函数 ====================

    /**
     * 盒式模糊（朴素实现，用于降噪/肤色平滑）
     */
    private fun boxBlurPixels(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val output = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sr = 0; var sg = 0; var sb = 0; var sa = 0; var count = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val np = pixels[ny * width + nx]
                        sr += (np ushr 16) and 0xFF
                        sg += (np ushr 8) and 0xFF
                        sb += np and 0xFF
                        sa += (np ushr 24) and 0xFF
                        count++
                    }
                }
                output[y * width + x] = ((sa / count) shl 24) or
                        ((sr / count) shl 16) or
                        ((sg / count) shl 8) or
                        (sb / count)
            }
        }
        return output
    }

    /**
     * YCbCr 肤色检测（BT.601），覆盖更广人种（深肤色、偏黄/偏红肤色）。
     *
     * 相比简化 RGB 范围，YCbCr 将亮度与色度分离，对光照变化更鲁棒，
     * 能更准确地识别不同人种肤色，确保 GPU 降级路径下的磨皮效果真实生效。
     */
    private fun isSkinColor(r: Float, g: Float, b: Float): Boolean {
        val y = 0.299f * r + 0.587f * g + 0.114f * b
        val cb = 0.564f * (b - y)
        val cr = 0.713f * (r - y)
        return y in 0.18f..0.95f &&
                cb in -0.18f..0.10f &&
                cr in 0.02f..0.28f &&
                cr > cb + 0.015f
    }

    /** RGB→HSL（与着色器 rgb2hsl 一致），返回 [h, s, l] 均在 [0, 1] */
    private fun rgb2hsl(r: Float, g: Float, b: Float): FloatArray {
        val maxC = maxOf(r, g, b)
        val minC = minOf(r, g, b)
        val delta = maxC - minC
        val l = (maxC + minC) / 2f
        var h = 0f
        var s = 0f
        if (delta > 0.0001f) {
            s = if (l < 0.5f) delta / (maxC + minC) else delta / (2f - maxC - minC)
            h = when {
                r >= maxC -> (g - b) / delta
                g >= maxC -> 2f + (b - r) / delta
                else -> 4f + (r - g) / delta
            }
            h /= 6f
            if (h < 0f) h += 1f
        }
        return floatArrayOf(h, s, l)
    }

    /** HSL→RGB（与着色器 hsl2rgb 一致），返回 [r, g, b] 均在 [0, 1] */
    private fun hsl2rgb(h: Float, s: Float, l: Float): FloatArray {
        if (s < 0.0001f) return floatArrayOf(l, l, l)
        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q
        return floatArrayOf(
            hue2rgb(p, q, h + 1f / 3f),
            hue2rgb(p, q, h),
            hue2rgb(p, q, h - 1f / 3f)
        )
    }

    private fun hue2rgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        if (t < 1f / 6f) return p + (q - p) * 6f * t
        if (t < 1f / 2f) return q
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
        return p
    }

    /** GLSL smoothstep */
    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** GLSL mix */
    private fun mix(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}