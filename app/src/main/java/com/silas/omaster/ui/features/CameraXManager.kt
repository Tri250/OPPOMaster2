package com.silas.omaster.ui.features

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.ai.scene.RealtimeSceneResult
import com.silas.omaster.ai.scene.SceneRecognitionManager
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.HistogramData
import com.silas.omaster.renderer.GPURenderManager
import com.silas.omaster.renderer.LUTPreviewRenderer
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.renderer.RenderQuality
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CameraX 实时取景管理器 - 2026 哈苏之眼 2.0
 *
 * 核心能力：
 * - 实时相机预览（Preview）
 * - 高质量拍照（ImageCapture）
 * - 实时图像分析（ImageAnalysis）用于：
 *     · AI 一键扫描场景识别
 *     · 实时滤镜/色彩效果叠加
 *     · 主体追踪、构图评分、AR 引导
 * - 前后摄像头切换、闪光灯、变焦、曝光补偿
 * - 生命周期感知：onPause 自动释放，onResume 自动恢复
 */
class CameraXManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {
    companion object {
        private const val TAG = "CameraXManager"

        /** 帧跳过间隔：每 N 帧处理一帧色彩/分析，减少 CPU 负载（运行时按模式动态调整） */
        @Volatile
        private var FRAME_SKIP_INTERVAL = 3

        /** 场景识别间隔：每 N 帧做一次 AI 场景识别 */
        private const val SCENE_SCAN_INTERVAL = 5
    }

    // 相机执行器（单线程顺序处理，避免并发回收问题）
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // 协程作用域：用于场景识别等异步任务
    private val managerScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // CameraX 组件
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    // 当前镜头方向
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    // 帧跳过计数器（线程安全：仅在 cameraExecutor 单线程中访问）
    private var frameSkipCounter = 0
    private var sceneScanCounter = 0

    // 释放标记：防止 release 后仍有 pending 回调将 Bitmap 泄漏
    @Volatile
    private var isReleased = false

    // 实时分析帧回调（始终在主线程触发，方便 Compose state 直接更新）
    // 回调方接收 Bitmap 所有权，使用完毕后必须调用 bitmap.recycle()
    private var onFrameAnalyzed: ((Bitmap) -> Unit)? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // 状态
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _currentLensFacing = MutableStateFlow(lensFacing)
    val currentLensFacing: StateFlow<Int> = _currentLensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(flashMode)
    val flashModeState: StateFlow<Int> = _flashMode.asStateFlow()

    // 当前应用的预设参数
    @Volatile
    private var currentPresetParams: HasselbladParams = HasselbladParams()

    // 参数锁定标记：锁定后忽略 AI 推荐参数覆盖
    @Volatile
    private var isParamsLocked: Boolean = false

    fun setParamsLocked(locked: Boolean) {
        isParamsLocked = locked
    }

    // 上次应用的参数，用于防抖（与上次相同则不重复应用）
    @Volatile
    private var lastAppliedParams: HasselbladParams? = null

    // AI 场景识别
    private val sceneRecognitionManager = SceneRecognitionManager.acquire(context)

    // GPU 渲染管理器（用于实时滤镜/ LUT 预览）
    private val gpuRenderManager = GPURenderManager.acquire(context)

    private val _sceneResult = MutableStateFlow<RealtimeSceneResult?>(null)
    val sceneResult: StateFlow<RealtimeSceneResult?> = _sceneResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Phase 2.2：亮度分布统计（供反模式检测使用）
    data class BrightnessStats(
        val upperBrightnessRatio: Float = 0f, // 上半部分亮度占比 0~1
        val overallBrightness: Float = 0f     // 整体平均亮度 0~255
    )
    private val _brightnessStats = MutableStateFlow(BrightnessStats())
    val brightnessStats: StateFlow<BrightnessStats> = _brightnessStats.asStateFlow()

    // Phase 2.2：陀螺仪稳定性（供反模式检测使用）
    private val _gyroscopeStable = MutableStateFlow(true)
    val gyroscopeStable: StateFlow<Boolean> = _gyroscopeStable.asStateFlow()

    // 是否启用 AI 一键扫描（默认开启）
    @Volatile
    private var sceneScanEnabled = true

    // 是否启用实时滤镜效果（默认开启）
    @Volatile
    private var realtimeFilterEnabled = true

    // 当前 3D LUT 状态（运行时）
    @Volatile
    private var lutTextureId: Int = 0
    @Volatile
    private var lutSize: Int = 0
    @Volatile
    private var lutStrength: Float = 1.0f
    @Volatile
    private var lutEnabled: Boolean = false

    // LUT 实时预览渲染器（Phase 2.1）
    @Volatile
    private var lutPreviewRenderer: LUTPreviewRenderer? = null
    private var lutInputSurfaceTexture: android.graphics.SurfaceTexture? = null
    private var lutInputSurface: android.view.Surface? = null

    // 保存的 PreviewView，用于生命周期恢复
    private var savedPreviewView: PreviewView? = null

    // 正在运行的场景识别 Job，避免并发
    private var sceneScanJob: Job? = null

    // ===== 拍摄模式系统 =====
    private val _captureMode = MutableStateFlow(CaptureMode.AI_AUTO)
    val captureMode: StateFlow<CaptureMode> = _captureMode.asStateFlow()

    // 各高级模式管理器（懒加载，按需初始化）
    private val arCompositionManager by lazy { ARCompositionManager(context) }
    private val portraitModeManager by lazy { PortraitModeManager(context) }
    private val nightModeManager by lazy { NightModeManager(context) }
    private val lightPaintingManager by lazy { LightPaintingManager(context) }
    private val proModeManager by lazy { ProModeManager(context) }
    private val specializedModeManager by lazy { SpecializedModeManager(context) }

    private val oppoCameraManager: com.silas.omaster.camera.OPPOCameraManager by lazy {
        com.silas.omaster.camera.OPPOCameraManager.getInstance(context)
    }

    // 实时 AR 构图结果
    private val _arCompositionResult = MutableStateFlow<ARCompositionResult?>(null)
    val arCompositionResult: StateFlow<ARCompositionResult?> = _arCompositionResult.asStateFlow()

    // 专业模式参数
    private val _proModeParams = MutableStateFlow(ProModeParams())
    val proModeParams: StateFlow<ProModeParams> = _proModeParams.asStateFlow()

    // 直方图与斑马纹数据（专业模式）
    private val _histogramData = MutableStateFlow<HistogramData?>(null)
    val histogramData: StateFlow<HistogramData?> = _histogramData.asStateFlow()

    // 光绘模式实时预览帧
    private val _lightPaintingFrame = MutableStateFlow<Bitmap?>(null)
    val lightPaintingFrame: StateFlow<Bitmap?> = _lightPaintingFrame.asStateFlow()

    // 夜景模式状态（暴露给 UI 层）
    val nightModeState: StateFlow<NightModeState> = nightModeManager.state

    // 光绘模式状态（暴露给 UI 层）
    val lightPaintingState: StateFlow<LightPaintingState> = lightPaintingManager.state

    // 追焦提示结果（转发 specializedModeManager 的 trackingResult）
    val trackingResult: StateFlow<SpecializedModeManager.TrackingResult?>
        get() = specializedModeManager.trackingResult

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        savedPreviewView?.let { startCamera(it) }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        releaseCamera()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        release()
    }

    /**
     * 启动相机，绑定到 PreviewView
     *
     * 仅在 Lifecycle 处于 STARTED 或 RESUMED 状态时启动，
     * 避免在后台或销毁状态下触发相机绑定导致崩溃。
     */
    fun startCamera(previewView: PreviewView) {
        savedPreviewView = previewView

        val currentState = lifecycleOwner.lifecycle.currentState
        if (!currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.d(TAG, "Lifecycle 未处于 STARTED 状态（当前: $currentState），推迟相机启动")
            return
        }

        isReleased = false
        frameSkipCounter = 0
        sceneScanCounter = 0
        registerGyroscope()

        // 异步初始化 GPU 渲染管线（不阻塞相机绑定）
        managerScope.launch {
            try {
                gpuRenderManager.initialize()
            } catch (e: Exception) {
                Log.w(TAG, "GPU 渲染管线初始化失败，将使用 CPU 降级", e)
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(previewView)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 绑定 CameraX 用例
     */
    private fun bindCameraUseCases(previewView: PreviewView) {
        val cameraProvider = cameraProvider ?: return

        // 解绑所有用例
        cameraProvider.unbindAll()

        // 选择摄像头
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // 构建 Preview
        preview = Preview.Builder()
            .setTargetResolution(Size(1920, 1080))
            .build()
            .also { pv ->
                val lutRenderer = lutPreviewRenderer
                if (lutRenderer != null && lutInputSurface != null) {
                    // Phase 2.1：LUT 实时预览路径
                    // 将相机预览输出到 LUTPreviewRenderer 的输入 Surface
                    pv.setSurfaceProvider { request ->
                        val surface = lutInputSurface!!
                        request.provideSurface(surface, ContextCompat.getMainExecutor(context)) { result ->
                            Log.d(TAG, "LUT preview surface result: ${result.resultCode}")
                        }
                    }
                } else {
                    pv.setSurfaceProvider(previewView.surfaceProvider)
                }
            }

        // 构建 ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetResolution(Size(1920, 1080))
            .setFlashMode(flashMode)
            .build()

        // 构建 ImageAnalysis（实时分析）
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 360))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeFrame(imageProxy)
                }
            }

        try {
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            _isCameraReady.value = true
            // 绑定到专业模式管理器，确保手动参数立即生效
            proModeManager.bindCamera(camera)
        } catch (e: Exception) {
            Log.e(TAG, "绑定相机失败: ${e.message}", e)
            _isCameraReady.value = false
            proModeManager.bindCamera(null)
        }
    }

    /**
     * Phase 2.1：设置 LUT 实时预览渲染器。
     * 传入非 null 的 renderer 和 outputSurface 时，相机会重新绑定到 LUT 预览路径。
     * UI 层应在 TextureView/SurfaceView 可用后调用本方法。
     */
    fun setLUTPreviewRenderer(renderer: LUTPreviewRenderer?, outputSurface: android.view.Surface?) {
        // 清理旧资源
        lutInputSurfaceTexture?.release()
        lutInputSurfaceTexture = null
        lutInputSurface?.release()
        lutInputSurface = null
        lutPreviewRenderer?.release()

        lutPreviewRenderer = renderer

        if (renderer != null && outputSurface != null) {
            renderer.init(outputSurface)
            val textureId = renderer.getCameraTextureId()
            val st = android.graphics.SurfaceTexture(textureId)
            lutInputSurfaceTexture = st
            lutInputSurface = android.view.Surface(st)

            st.setOnFrameAvailableListener {
                renderer.drawFrame()
            }

            // 重新绑定相机以切换预览路径
            savedPreviewView?.let { startCamera(it) }
        } else {
            // 关闭 LUT 预览，恢复 PreviewView 路径
            savedPreviewView?.let { startCamera(it) }
        }
    }

    /**
     * 实时分析帧：
     * - 每 FRAME_SKIP_INTERVAL 帧做色彩处理并回调
     * - 每 SCENE_SCAN_INTERVAL 帧做一次 AI 场景识别
     * - 根据当前 [captureMode] 分发到对应模式管理器
     *
     * 线程安全：本方法在 cameraExecutor 单线程中执行，只做轻量分发，
     * 耗时处理（AI 推理、像素混合、人像美颜等）均投递到 [managerScope]。
     */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            frameSkipCounter++
            sceneScanCounter++

            val mode = _captureMode.value
            val shouldProcessFrame = frameSkipCounter % FRAME_SKIP_INTERVAL == 0
            val shouldScanScene = sceneScanEnabled && sceneScanCounter % SCENE_SCAN_INTERVAL == 0
            val shouldAnalyzeComposition = shouldProcessFrame

            if (!shouldProcessFrame && !shouldScanScene) {
                return
            }

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap == null || bitmap.isRecycled) {
                return
            }

            // 为 AR 构图分析准备独立副本，避免与模式处理共用同一张 Bitmap
            val arBitmap = if (shouldAnalyzeComposition) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else null

            // 场景识别 + 亮度统计（同步计算，避免额外遍历）
            if (shouldScanScene) {
                triggerSceneScan(bitmap)
                _brightnessStats.value = computeBrightnessStats(bitmap)
            }

            // 模式分发：所有耗时操作均在协程中执行
            when (mode) {
                CaptureMode.NIGHT -> {
                    nightModeManager.submitFrame(bitmap)
                    // 夜景取景器仍需基础色彩回调，原帧由 dispatch 回收
                    dispatchProcessedFrame(bitmap)
                }
                CaptureMode.LIGHT_PAINTING -> {
                    // 光绘需要独占一份输入帧，原帧继续走色彩管线
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)?.let { lpCopy ->
                        lightPaintingManager.submitFrame(lpCopy)
                    }
                    dispatchProcessedFrame(bitmap)
                }
                CaptureMode.PORTRAIT -> {
                    processPortraitFrame(bitmap)
                }
                CaptureMode.PRO -> {
                    processProFrame(bitmap)
                }
                CaptureMode.FOOD, CaptureMode.STREET, CaptureMode.PET -> {
                    // 特化模式需要一份拷贝进入 0 延迟循环缓冲区，原帧继续走色彩管线
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)?.let { copy ->
                        specializedModeManager.submitPreviewFrame(copy)
                    }
                    dispatchProcessedFrame(bitmap)
                }
                else -> {
                    // AI_AUTO 默认路径
                    dispatchProcessedFrame(bitmap)
                }
            }

            // AR 构图分析（所有模式共用）
            if (arBitmap != null && !arBitmap.isRecycled) {
                processARComposition(arBitmap, mode)
            } else {
                arBitmap?.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "帧分析失败: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
    }

    /**
     * 将原始帧投递到色彩处理管线，结果通过 [onFrameAnalyzed] 回调给 UI。
     * 调用方继续拥有 [source] 的所有权；方法内部会复制一份进行处理。
     */
    private fun dispatchProcessedFrame(source: Bitmap, recycleSource: Boolean = true) {
        if (!realtimeFilterEnabled) {
            if (recycleSource && !source.isRecycled) source.recycle()
            return
        }
        managerScope.launch {
            try {
                // 复制一份避免与后续分析共用一个 Bitmap
                val workBitmap = if (recycleSource) {
                    source
                } else {
                    source.copy(Bitmap.Config.ARGB_8888, false) ?: source
                }
                val processedBitmap = applyPresetToFrame(workBitmap, currentPresetParams)
                postFrameToCallback(processedBitmap)
                if (processedBitmap !== workBitmap && !workBitmap.isRecycled) {
                    workBitmap.recycle()
                }
            } catch (e: Exception) {
                Log.e(TAG, "实时滤镜处理失败", e)
                if (recycleSource && !source.isRecycled) source.recycle()
            }
        }
    }

    /**
     * 人像模式实时处理：虚化 + 美颜 + 姿势引导。
     * [bitmap] 所有权已移交给 [portraitModeManager]，本方法不再访问原图。
     */
    private fun processPortraitFrame(bitmap: Bitmap) {
        managerScope.launch {
            try {
                val result = portraitModeManager.processFrame(bitmap)
                val processed = result.processedFrame
                if (processed != null && !processed.isRecycled) {
                    postFrameToCallback(processed)
                }
                // 更新 AR 构图提示（利用姿势引导信息）
                result.poseGuide?.let { guide ->
                    _arCompositionResult.value = (_arCompositionResult.value ?: emptyARResult()).copy(
                        tips = "${guide.title} · ${guide.description}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "人像模式实时处理失败", e)
            }
        }
    }

    /**
     * 专业模式实时处理：直方图 + 斑马纹 + 对焦峰值。
     * 处理完成后主动回收 [bitmap]，避免分析帧泄漏。
     */
    private fun processProFrame(bitmap: Bitmap) {
        managerScope.launch {
            try {
                val (histogram, zebraPeaking) = proModeManager.processFrame(bitmap)
                _histogramData.value = histogram
                // 将斑马纹/对焦峰值叠加到预览帧（透明度混合）
                val overlay = overlayZebraPeaking(bitmap, zebraPeaking)
                postFrameToCallback(overlay)
            } catch (e: Exception) {
                Log.e(TAG, "专业模式实时处理失败", e)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    /**
     * AR 构图分析：主体检测、构图评分、引导线生成。
     */
    private fun processARComposition(bitmap: Bitmap, mode: CaptureMode) {
        managerScope.launch {
            try {
                val rotation = imageAnalysis?.targetRotation?.let { rotationToDegrees(it) } ?: 0
                val result = arCompositionManager.analyzeFrame(bitmap, rotation, mode)
                _arCompositionResult.value = result
            } catch (e: Exception) {
                Log.e(TAG, "AR 构图分析失败", e)
            }
        }
    }

    /**
     * 将处理后的帧安全地回调到主线程。
     */
    private fun postFrameToCallback(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        mainHandler.post {
            if (isReleased) {
                bitmap.recycle()
            } else {
                onFrameAnalyzed?.invoke(bitmap)
            }
        }
    }

    /**
     * 将斑马纹与对焦峰值掩码叠加到原帧，返回新 Bitmap。
     */
    private suspend fun overlayZebraPeaking(
        source: Bitmap,
        zebraPeaking: ZebraPeakingResult
    ): Bitmap {
        val copied = source.copy(Bitmap.Config.ARGB_8888, false) ?: source
        val base = applyPresetToFrame(copied, currentPresetParams)
        val zebra = zebraPeaking.zebraBitmap
        val peaking = zebraPeaking.peakingBitmap
        if ((zebra == null || zebra.isRecycled) && (peaking == null || peaking.isRecycled)) {
            if (base !== copied && !copied.isRecycled) copied.recycle()
            return base
        }

        val width = base.width
        val height = base.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawBitmap(base, 0f, 0f, null)

        val paint = android.graphics.Paint().apply {
            alpha = 180
            isAntiAlias = false
        }
        zebra?.takeIf { !it.isRecycled }?.let { canvas.drawBitmap(it, 0f, 0f, paint) }
        peaking?.takeIf { !it.isRecycled }?.let { canvas.drawBitmap(it, 0f, 0f, paint) }

        if (base !== copied && !base.isRecycled) base.recycle()
        if (copied !== source && !copied.isRecycled) copied.recycle()
        return output
    }

    private fun rotationToDegrees(rotation: Int): Int {
        return when (rotation) {
            android.view.Surface.ROTATION_0 -> 0
            android.view.Surface.ROTATION_90 -> 90
            android.view.Surface.ROTATION_180 -> 180
            android.view.Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    private fun emptyARResult(): ARCompositionResult {
        return ARCompositionResult(
            guideLines = emptyList(),
            subjectBounds = null,
            compositionScore = 0,
            levelIndicator = 0f,
            tips = ""
        )
    }

    /**
     * 触发 AI 场景识别（异步，不阻塞当前帧处理）。
     *
     * 注意：传入的 bitmap 所有权仍归调用方；本方法内部会复制一份小图用于识别，
     * 并在识别完成后回收副本。
     */
    private fun triggerSceneScan(sourceBitmap: Bitmap) {
        sceneScanJob?.cancel()
        sceneScanJob = managerScope.launch {
            var scanBitmap: Bitmap? = null
            try {
                _isScanning.value = true
                // 复制一份小图用于识别，避免阻塞原始 bitmap 的回收
                scanBitmap = Bitmap.createScaledBitmap(sourceBitmap, 224, 224, true)
                val result = sceneRecognitionManager.analyzeFrame(scanBitmap ?: sourceBitmap)
                _sceneResult.value = result
                // 自动应用推荐参数（如果用户未锁定）
                applyRecommendedParams(result.recommendedParams)
            } catch (e: Exception) {
                Log.e(TAG, "场景识别失败", e)
            } finally {
                _isScanning.value = false
                if (scanBitmap != null && scanBitmap !== sourceBitmap && !scanBitmap.isRecycled) {
                    scanBitmap.recycle()
                }
            }
        }
    }

    /**
     * 应用 AI 推荐的参数。
     * 参数锁定时忽略 AI 推荐；与上次相同则不重复应用（防抖）。
     */
    private fun applyRecommendedParams(params: HasselbladParams) {
        if (isParamsLocked) {
            Log.d(TAG, "参数已锁定，忽略 AI 推荐")
            return
        }
        // 防抖：与上次相同则不重复应用
        if (params == lastAppliedParams) return
        lastAppliedParams = params
        currentPresetParams = params
    }

    /**
     * 注册帧分析回调
     *
     * 回调方接收 Bitmap 所有权，使用完毕后必须调用 bitmap.recycle()。
     * 传入 null 可取消回调。
     */
    fun setOnFrameAnalyzed(callback: ((Bitmap) -> Unit)?) {
        onFrameAnalyzed = callback
    }

    /**
     * 更新当前预设参数
     */
    fun updatePresetParams(params: HasselbladParams) {
        currentPresetParams = params
    }

    /**
     * 切换当前拍摄模式。
     *
     * 模式切换时会自动清理上一个模式的中间状态（夜景采集、光绘录制、特化帧缓冲），
     * 并将新模式同步到对应的专业/特化管理器。
     */
    fun setCaptureMode(mode: CaptureMode) {
        val previousMode = _captureMode.value
        if (previousMode == mode) return

        // 清理旧模式状态
        when (previousMode) {
            CaptureMode.NIGHT -> nightModeManager.cancelCapture()
            CaptureMode.LIGHT_PAINTING -> lightPaintingManager.reset()
            CaptureMode.FOOD, CaptureMode.STREET, CaptureMode.PET -> specializedModeManager.clearFrameBuffer()
            else -> { /* 无需清理 */ }
        }

        _captureMode.value = mode
        _arCompositionResult.value = null
        _histogramData.value = null
        _lightPaintingFrame.value = null

        // 同步到子管理器
        specializedModeManager.setActiveMode(mode)

        // 根据模式预配置参数
        when (mode) {
            CaptureMode.PORTRAIT -> {
                currentPresetParams = SceneToHasselbladMapping.getParams("portrait-standard")
            }
            CaptureMode.NIGHT -> {
                currentPresetParams = SceneToHasselbladMapping.getParams("night-city")
                camera?.let { nightModeManager.bindCamera(it) }
            }
            CaptureMode.LIGHT_PAINTING -> {
                camera?.let { lightPaintingManager.bindCamera(it) }
            }
            CaptureMode.FOOD, CaptureMode.STREET, CaptureMode.PET -> {
                currentPresetParams = specializedModeManager.getRecommendedParams(mode)
            }
            CaptureMode.PRO -> {
                // 专业模式保留用户手动参数，由 ProModeManager 单独管理
            }
            else -> {
                // AI_AUTO / LIGHT_PAINTING 使用当前或 AI 推荐参数
            }
        }

        // 根据模式动态调整帧处理频率
        when (mode) {
            CaptureMode.NIGHT, CaptureMode.LIGHT_PAINTING -> {
                // 夜景/光绘需要更多帧，降低跳帧
                FRAME_SKIP_INTERVAL = 1
            }
            CaptureMode.PRO -> {
                // 专业模式直方图需要较高帧率
                FRAME_SKIP_INTERVAL = 2
            }
            else -> {
                FRAME_SKIP_INTERVAL = 3
            }
        }

        Log.d(TAG, "拍摄模式切换: $previousMode -> $mode")
    }

    /**
     * 设置是否启用 AI 一键扫描
     */
    fun setSceneScanEnabled(enabled: Boolean) {
        sceneScanEnabled = enabled
        if (!enabled) {
            sceneScanJob?.cancel()
            _sceneResult.value = null
            _isScanning.value = false
        }
    }

    /**
     * 设置是否启用实时滤镜效果
     */
    fun setRealtimeFilterEnabled(enabled: Boolean) {
        realtimeFilterEnabled = enabled
    }

    // ===== 专业模式参数设置 =====

    /**
     * 设置专业模式 ISO 值
     */
    fun setProModeIso(iso: Int) {
        val current = _proModeParams.value
        _proModeParams.value = current.copy(iso = iso)
        proModeManager.setParams(_proModeParams.value)
    }

    /**
     * 设置专业模式快门速度（纳秒）
     */
    fun setProModeShutter(shutterSpeedNs: Long) {
        val current = _proModeParams.value
        _proModeParams.value = current.copy(shutterSpeedNs = shutterSpeedNs)
        proModeManager.setParams(_proModeParams.value)
    }

    /**
     * 设置专业模式对焦距离（0-1）
     */
    fun setProModeFocus(focusDistance: Float) {
        val current = _proModeParams.value
        _proModeParams.value = current.copy(focusDistance = focusDistance)
        proModeManager.setParams(_proModeParams.value)
    }

    /**
     * 设置专业模式白平衡色温（K）
     */
    fun setProModeWhiteBalance(kelvin: Int) {
        val current = _proModeParams.value
        _proModeParams.value = current.copy(whiteBalanceTemperature = kelvin)
        proModeManager.setParams(_proModeParams.value)
    }

    /**
     * 上传并启用 3D LUT 实时叠加。
     * 必须在 GPU 渲染管线初始化成功后调用；否则 LUT 不会生效。
     *
     * @param lutData 3D LUT 数据
     * @param strength 叠加强度 [0, 1]
     */
    fun setActiveLUT3D(lutData: com.silas.omaster.data.lut.LUT3DData, strength: Float = 1.0f) {
        managerScope.launch {
            try {
                // 确保 GPU 已初始化
                if (!gpuRenderManager.isInitialized.value) {
                    gpuRenderManager.initialize()
                }
                val textureId = gpuRenderManager.uploadLUT3DTexture(lutData)
                if (textureId != 0) {
                    lutTextureId = textureId
                    lutSize = lutData.size
                    lutStrength = strength.coerceIn(0f, 1f)
                    lutEnabled = true
                    Log.d(TAG, "3D LUT 已激活: size=${lutData.size}, texture=$textureId")
                } else {
                    Log.w(TAG, "3D LUT 上传失败，禁用 LUT 叠加")
                    clearActiveLUT3D()
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置 3D LUT 失败", e)
                clearActiveLUT3D()
            }
        }
    }

    /**
     * 清除当前 3D LUT 叠加效果并释放 GPU 纹理。
     */
    fun clearActiveLUT3D() {
        lutEnabled = false
        lutStrength = 1.0f
        lutSize = 0
        val previousTextureId = lutTextureId
        lutTextureId = 0
        if (previousTextureId != 0) {
            managerScope.launch {
                try {
                    gpuRenderManager.releaseLUT3DTexture()
                } catch (e: Exception) {
                    Log.e(TAG, "释放 LUT 纹理失败", e)
                }
            }
        }
    }

    /**
     * 点击对焦：将屏幕坐标转换为测光点并触发对焦+测光
     */
    fun tapToFocus(x: Float, y: Float) {
        val camera = camera ?: return
        val previewView = savedPreviewView ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).build()
        camera.cameraControl.startFocusAndMetering(action)
    }

    /**
     * 设置变焦倍数（自动钳制到设备支持范围）
     */
    fun setZoomRatio(ratio: Float) {
        val camera = camera ?: return
        val maxRatio = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
        val clamped = ratio.coerceIn(1f, maxRatio)
        camera.cameraControl.setZoomRatio(clamped)
    }

    /**
     * 设置曝光补偿索引（自动钳制到设备支持范围）
     */
    fun setExposureCompensation(index: Int) {
        val camera = camera ?: return
        val exposureState = camera.cameraInfo.exposureState
        if (!exposureState.isExposureCompensationSupported) return
        val range = exposureState.exposureCompensationRange
        val clamped = index.coerceIn(range.lower, range.upper)
        camera.cameraControl.setExposureCompensationIndex(clamped)
    }

    /**
     * 获取曝光补偿支持范围，不支持时返回 0..0
     */
    fun getExposureCompensationRange(): IntRange {
        val camera = camera ?: return 0..0
        val exposureState = camera.cameraInfo.exposureState
        return if (exposureState.isExposureCompensationSupported) {
            val range = exposureState.exposureCompensationRange
            range.lower..range.upper
        } else {
            0..0
        }
    }

    /**
     * 获取设备支持的最大变焦倍数
     */
    fun getMaxZoomRatio(): Float {
        val camera = camera ?: return 1f
        return camera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
    }

    /**
     * 获取当前变焦倍数
     */
    fun getCurrentZoomRatio(): Float {
        val camera = camera ?: return 1f
        return camera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
    }

    /**
     * 拍照
     *
     * 根据当前 [captureMode] 分发到对应模式处理链路：
     * - AI_AUTO / FOOD：常规拍照 + 哈苏色彩
     * - PORTRAIT：常规拍照 + 人像虚化/美颜 + 哈苏色彩
     * - NIGHT：触发夜景多帧合成
     * - LIGHT_PAINTING：开始/停止光绘录制
     * - PRO：常规拍照（保留专业参数）+ 哈苏色彩
     * - STREET / PET：0 延迟快门
     *
     * 所有耗时处理均放到独立协程，不占用 cameraExecutor。
     */
    fun takePhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        if (!_isCameraReady.value) {
            onError("相机未就绪")
            return
        }

        when (_captureMode.value) {
            CaptureMode.NIGHT -> takeNightPhoto(onPhotoSaved, onError)
            CaptureMode.LIGHT_PAINTING -> takeLightPaintingPhoto(onPhotoSaved, onError)
            CaptureMode.STREET, CaptureMode.PET -> takeZeroLagPhoto(onPhotoSaved, onError)
            CaptureMode.PORTRAIT -> takePortraitPhoto(onPhotoSaved, onError)
            CaptureMode.PRO -> takeStandardPhoto(onPhotoSaved, onError, preserveProParams = true)
            else -> takeStandardPhoto(onPhotoSaved, onError)
        }
    }

    /**
     * 标准拍照链路：CameraX -> 哈苏色彩 -> 保存。
     */
    private fun takeStandardPhoto(
        onPhotoSaved: (Uri) -> Unit,
        onError: (String) -> Unit,
        preserveProParams: Boolean = false
    ) {
        val imageCapture = imageCapture ?: run {
            onError("相机未就绪")
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    if (bitmap == null) {
                        mainHandler.post { onError("图像解码失败") }
                        return
                    }

                    managerScope.launch {
                        try {
                            val params = if (preserveProParams) {
                                currentPresetParams
                            } else {
                                currentPresetParams
                            }
                            val processedBitmap = applyHasselbladColorEngine(
                                source = bitmap,
                                hasselbladParams = params
                            )
                            if (processedBitmap !== bitmap && !bitmap.isRecycled) {
                                bitmap.recycle()
                            }
                            saveBitmapAndNotify(processedBitmap, onPhotoSaved, onError)
                        } catch (e: Exception) {
                            Log.e(TAG, "拍照处理失败: ${e.message}", e)
                            if (!bitmap.isRecycled) bitmap.recycle()
                            mainHandler.post { onError(e.message ?: "拍照失败") }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exception.message}", exception)
                    mainHandler.post { onError(exception.message ?: "拍照失败") }
                }
            }
        )
    }

    /**
     * 人像模式拍照：常规拍照 -> 人像虚化 + 美颜 -> 哈苏色彩 -> 保存。
     */
    private fun takePortraitPhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onError("相机未就绪")
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    imageProxy.close()
                    if (bitmap == null) {
                        mainHandler.post { onError("图像解码失败") }
                        return
                    }

                    managerScope.launch {
                        try {
                            // 人像效果处理
                            val portraitResult = portraitModeManager.processFrame(bitmap)
                            val portraitBitmap = portraitResult.processedFrame ?: bitmap
                            if (portraitBitmap !== bitmap && !bitmap.isRecycled) {
                                bitmap.recycle()
                            }

                            // 哈苏色彩
                            val finalBitmap = applyHasselbladColorEngine(
                                source = portraitBitmap,
                                hasselbladParams = currentPresetParams
                            )
                            if (finalBitmap !== portraitBitmap && !portraitBitmap.isRecycled) {
                                portraitBitmap.recycle()
                            }

                            saveBitmapAndNotify(finalBitmap, onPhotoSaved, onError)
                        } catch (e: Exception) {
                            Log.e(TAG, "人像拍照处理失败: ${e.message}", e)
                            if (!bitmap.isRecycled) bitmap.recycle()
                            mainHandler.post { onError(e.message ?: "拍照失败") }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "人像拍照失败: ${exception.message}", exception)
                    mainHandler.post { onError(exception.message ?: "拍照失败") }
                }
            }
        )
    }

    /**
     * 夜景模式拍照：触发多帧合成降噪与长曝光增强。
     */
    private fun takeNightPhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        if (nightModeManager.state.value.isCapturing) {
            mainHandler.post { onError("夜景采集正在进行中") }
            return
        }

        camera?.let { nightModeManager.bindCamera(it) }

        managerScope.launch {
            try {
                val result = nightModeManager.captureAndProcess()
                if (result == null || result.isRecycled) {
                    mainHandler.post { onError("夜景合成失败") }
                    return@launch
                }

                // 叠加哈苏夜景参数
                val finalBitmap = applyHasselbladColorEngine(
                    source = result,
                    hasselbladParams = currentPresetParams
                )
                if (finalBitmap !== result && !result.isRecycled) {
                    result.recycle()
                }

                saveBitmapAndNotify(finalBitmap, onPhotoSaved, onError)
            } catch (e: Exception) {
                Log.e(TAG, "夜景拍照失败: ${e.message}", e)
                mainHandler.post { onError(e.message ?: "夜景拍照失败") }
            }
        }
    }

    /**
     * 光绘模式拍照：切换录制/停止，输出累积光轨帧。
     */
    private fun takeLightPaintingPhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val state = lightPaintingManager.state.value
        if (!state.isRecording) {
            camera?.let { lightPaintingManager.bindCamera(it) }
            lightPaintingManager.startRecording()
            mainHandler.post {
                Toast.makeText(context, "光绘录制已开始，再次点击快门结束", Toast.LENGTH_SHORT).show()
            }
            return
        }

        managerScope.launch {
            try {
                val result = lightPaintingManager.stopRecording()
                if (result == null || result.isRecycled) {
                    mainHandler.post { onError("光绘未采集到有效帧") }
                    return@launch
                }

                val finalBitmap = applyHasselbladColorEngine(
                    source = result,
                    hasselbladParams = currentPresetParams
                )
                if (finalBitmap !== result && !result.isRecycled) {
                    result.recycle()
                }

                saveBitmapAndNotify(finalBitmap, onPhotoSaved, onError)
            } catch (e: Exception) {
                Log.e(TAG, "光绘拍照失败: ${e.message}", e)
                mainHandler.post { onError(e.message ?: "光绘拍照失败") }
            }
        }
    }

    /**
     * 0 延迟快门：街拍 / 宠物模式下从预缓存缓冲区取出最佳帧。
     */
    private fun takeZeroLagPhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val bitmap = specializedModeManager.captureZeroLag()
        if (bitmap == null || bitmap.isRecycled) {
            mainHandler.post { onError("0 延迟快门未准备好，请稍后重试") }
            return
        }

        managerScope.launch {
            try {
                val finalBitmap = applyHasselbladColorEngine(
                    source = bitmap,
                    hasselbladParams = currentPresetParams
                )
                if (finalBitmap !== bitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
                saveBitmapAndNotify(finalBitmap, onPhotoSaved, onError)
            } catch (e: Exception) {
                Log.e(TAG, "0 延迟快门处理失败: ${e.message}", e)
                if (!bitmap.isRecycled) bitmap.recycle()
                mainHandler.post { onError(e.message ?: "拍照失败") }
            }
        }
    }

    /**
     * 将 Bitmap 保存为 JPEG 到系统相册（MediaStore），并回调 URI。
     * 修复：原实现保存到 cacheDir，照片不会出现在系统图库中；
     * 现改为写入 MediaStore，确保用户可在相册中查看拍摄结果。
     */
    private suspend fun saveBitmapAndNotify(
        bitmap: Bitmap,
        onPhotoSaved: (Uri) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val filename = "camerax_${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            val relativePath = Environment.DIRECTORY_PICTURES + "/OMaster/CameraX"

            val savedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(it, contentValues, null, null)
                    it
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OMaster/CameraX")
                dir.mkdirs()
                val file = File(dir, filename)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
                Uri.fromFile(file)
            }

            bitmap.recycle()

            if (savedUri != null) {
                Log.d(TAG, "照片已保存到相册: $savedUri")

                // OPPO 设备参数同步：拍照完成后尝试将哈苏参数同步到 OPPO 相机大师模式
                try {
                    val capability = oppoCameraManager.detectDeviceCapability()
                    if (capability.isOppoDevice) {
                        oppoCameraManager.applyHasselbladParams(currentPresetParams)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "OPPO 参数同步失败: ${e.message}")
                }

                mainHandler.post { onPhotoSaved(savedUri) }
            } else {
                mainHandler.post { onError("保存失败，无法创建媒体文件") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存照片失败: ${e.message}", e)
            if (!bitmap.isRecycled) bitmap.recycle()
            mainHandler.post { onError(e.message ?: "保存失败") }
        }
    }

    /**
     * 切换前后摄像头
     */
    fun switchCamera(previewView: PreviewView? = null) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _currentLensFacing.value = lensFacing
        val target = previewView ?: savedPreviewView
        if (target != null) {
            bindCameraUseCases(target)
        }
    }

    /**
     * 切换闪光灯模式（OFF → ON → AUTO → OFF）
     */
    fun toggleFlash(): Int {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _flashMode.value = flashMode
        return flashMode
    }

    /**
     * 将 ImageProxy 转换为 Bitmap（支持 CameraX 默认 YUV_420_888 输出）。
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return CameraFrameConverter.toBitmap(imageProxy)
    }

    /**
     * 应用预设参数到帧。
     *
     * 优先走 GPU 渲染管线（OpenGL ES 3.0 + 3D LUT），GPU 不可用或失败时
     * 自动降级到 CPU ColorMatrix 实现，保证取景器效果不中断。
     */
    private suspend fun applyPresetToFrame(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        // 1. 尝试 GPU 管线
        if (gpuRenderManager.isGPUAvailable()) {
            try {
                val renderParams = hasselbladParamsToRenderParameters(params)
                val result = gpuRenderManager.renderPreview(bitmap, renderParams)
                if (result != null && !result.isRecycled) {
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPU 实时渲染失败，降级 CPU", e)
            }
        }

        // 2. CPU 降级：ColorMatrix + 暗角
        return applyPresetToFrameCPU(bitmap, params)
    }

    /**
     * CPU 降级实时滤镜：使用 ColorMatrix 与 Canvas 绘制，保证最低功耗与兼容性。
     */
    private fun applyPresetToFrameCPU(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        val saturation = normalizeSigned(params.saturation, 30)
        val contrast = normalizeSigned(params.contrast, 30)
        val colorTemp = normalizeSigned(params.colorTemp, 30)
        val tone = normalizeSigned(params.tone, 30)
        val cyanMagenta = normalizeSigned(params.cyanMagenta, 30)
        val vignette = normalizeUnsigned(params.vignette, 30)

        val combinedMatrix = android.graphics.ColorMatrix()

        if (saturation <= -0.95f) {
            val bwMatrix = android.graphics.ColorMatrix(floatArrayOf(
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0.299f, 0.587f, 0.114f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.set(bwMatrix)
        } else if (saturation != 0f) {
            combinedMatrix.setSaturation(1f + saturation)
        }

        val hasPostMatrix = contrast != 0f || tone != 0f || colorTemp != 0f || cyanMagenta != 0f
        if (hasPostMatrix) {
            val contrastValue = 1f + contrast
            val postMatrix = android.graphics.ColorMatrix(floatArrayOf(
                contrastValue, 0f, 0f, 0f, tone * 25f + cyanMagenta * 25f,
                0f, contrastValue, 0f, 0f, tone * 10f - cyanMagenta * 20f,
                0f, 0f, contrastValue, 0f, -colorTemp * 15f + cyanMagenta * 15f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(postMatrix)
        }

        val hasColorMatrixOps = saturation != 0f || hasPostMatrix
        val hasVignette = vignette > 0.005f

        if (!hasColorMatrixOps && !hasVignette) {
            return bitmap
        }

        var current: Bitmap = bitmap
        if (hasColorMatrixOps) {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            val paint = android.graphics.Paint().apply {
                isAntiAlias = false
                colorFilter = android.graphics.ColorMatrixColorFilter(combinedMatrix)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            current = output
        }

        if (hasVignette) {
            val output = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            canvas.drawBitmap(current, 0f, 0f, null)
            val vignettePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                shader = android.graphics.RadialGradient(
                    current.width / 2f, current.height / 2f,
                    maxOf(current.width, current.height) * 0.7f,
                    intArrayOf(0x00000000, 0x00000000, (vignette * 180).toInt() shl 24),
                    floatArrayOf(0f, 0.6f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, current.width.toFloat(), current.height.toFloat(), vignettePaint)
            if (current !== bitmap) current.recycle()
            current = output
        }

        return current
    }

    private fun normalizeSigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(-1f, 1f)

    private fun normalizeUnsigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    /**
     * 将哈苏大师参数映射到 GPU 渲染管线使用的 [RenderParameters]。
     * 保留 HNCS 色彩科学的核心语义：饱和度克制、自然锐度、柔和影调。
     */
    private fun hasselbladParamsToRenderParameters(params: HasselbladParams): RenderParameters {
        return RenderParameters(
            saturation = params.saturation * 3.3f,
            contrast = params.contrast * 3.3f,
            brightness = params.tone * 1.5f,
            warmth = params.colorTemp * 3.3f,
            sharpness = params.sharpness.coerceAtLeast(0) * 3.3f,
            clarity = params.clarity * 3.3f,
            highlights = params.highlights * 3.3f,
            shadows = params.shadows * 3.3f,
            vibrance = params.saturation.coerceAtLeast(0) * 2f,
            fade = if (params.tone < 0) (-params.tone) * 1.5f else 0f,
            denoise = 0f,
            skinSmooth = 0f,
            exposure = 0f,
            texture = 0f,
            grain = 0f,
            dehaze = 0f,
            whites = 0f,
            blacks = 0f,
            lutTextureId = lutTextureId,
            lutSize = lutSize,
            lutStrength = lutStrength,
            lutEnabled = lutEnabled
        )
    }

    /**
     * 仅释放相机绑定（保留 executor，用于 onPause/onResume 切换）
     */
    private fun releaseCamera() {
        isReleased = true
        onFrameAnalyzed = null
        sceneScanJob?.cancel()
        clearActiveLUT3D()
        cameraProvider?.unbindAll()
        proModeManager.bindCamera(null)
        camera = null
        preview = null
        imageCapture = null
        imageAnalysis = null
        _isCameraReady.value = false
        Log.d(TAG, "CameraX 相机已解绑")
    }

    /**
     * 完全释放所有相机资源（用于 onDestroy）
     */
    fun release() {
        lifecycleOwner.lifecycle.removeObserver(this)
        releaseCamera()
        managerScope.cancel()
        sceneRecognitionManager.release()
        gpuRenderManager.release()
        arCompositionManager.release()
        portraitModeManager.release()
        nightModeManager.release()
        lightPaintingManager.release()
        proModeManager.release()
        specializedModeManager.release()
        lutPreviewRenderer?.release()
        lutPreviewRenderer = null
        lutInputSurfaceTexture?.release()
        lutInputSurfaceTexture = null
        lutInputSurface?.release()
        lutInputSurface = null
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
        // 注销陀螺仪监听
        unregisterGyroscope()
        Log.d(TAG, "CameraX 资源已完全释放")
    }

    // ==================== Phase 2.2：亮度统计与陀螺仪 ====================

    /**
     * 计算图像亮度分布统计。
     * 采用中心采样策略（取中心 60% 区域），避免边缘黑边干扰。
     * 上半部分亮度比例 = 上半部分平均亮度 / 整体平均亮度。
     */
    private fun computeBrightnessStats(bitmap: Bitmap): BrightnessStats {
        val width = bitmap.width
        val height = bitmap.height
        val startX = (width * 0.2f).toInt()
        val endX = (width * 0.8f).toInt()
        val startY = (height * 0.2f).toInt()
        val endY = (height * 0.8f).toInt()

        var totalLuma = 0L
        var upperLuma = 0L
        var sampleCount = 0
        val midY = (startY + endY) / 2

        // 采样步长 4px，平衡精度与性能
        val step = 4
        for (y in startY until endY step step) {
            for (x in startX until endX step step) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luma = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                totalLuma += luma
                if (y < midY) upperLuma += luma
                sampleCount++
            }
        }

        if (sampleCount == 0) return BrightnessStats()
        val overall = totalLuma.toFloat() / sampleCount
        val upperHalfSamples = ((midY - startY).toFloat() / (endY - startY) * sampleCount).toInt().coerceAtLeast(1)
        val upperAvg = upperLuma.toFloat() / upperHalfSamples
        val upperRatio = if (overall > 0f) (upperAvg / overall).coerceIn(0f, 1f) else 0f

        return BrightnessStats(
            upperBrightnessRatio = upperRatio,
            overallBrightness = overall.coerceIn(0f, 255f)
        )
    }

    // 陀螺仪传感器
    private var sensorManager: android.hardware.SensorManager? = null
    private var gyroscopeSensor: android.hardware.Sensor? = null
    private val gyroscopeListener = object : android.hardware.SensorEventListener {
        private val windowSize = 10
        private val angularVelocities = FloatArray(windowSize)
        private var index = 0

        override fun onSensorChanged(event: android.hardware.SensorEvent) {
            val vx = event.values[0]
            val vy = event.values[1]
            val vz = event.values[2]
            val magnitude = kotlin.math.sqrt(vx * vx + vy * vy + vz * vz)
            angularVelocities[index % windowSize] = magnitude
            index++

            // 计算最近 windowSize 个样本的标准差
            val count = kotlin.math.min(index, windowSize)
            if (count >= 5) {
                val mean = angularVelocities.take(count).average().toFloat()
                val variance = angularVelocities.take(count).map { (it - mean) * (it - mean) }.average().toFloat()
                val stdDev = kotlin.math.sqrt(variance)
                // 标准差 < 0.15 rad/s 认为稳定（手持轻微抖动阈值）
                _gyroscopeStable.value = stdDev < 0.15f
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    private fun registerGyroscope() {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? android.hardware.SensorManager
            gyroscopeSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_GYROSCOPE)
        }
        gyroscopeSensor?.let {
            sensorManager?.registerListener(gyroscopeListener, it, android.hardware.SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun unregisterGyroscope() {
        sensorManager?.unregisterListener(gyroscopeListener)
    }
}
