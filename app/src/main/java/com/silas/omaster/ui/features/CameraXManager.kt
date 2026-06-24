package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.util.Size
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
import com.silas.omaster.model.HasselbladParams
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CameraX 实时取景管理器 - P2 深度优化
 *
 * 功能：
 * - 实时相机预览（Preview）
 * - 实时图像分析（ImageAnalysis）用于应用预设参数
 * - 高质量拍照（ImageCapture）
 * - 前后摄像头切换
 * - 闪光灯控制
 * - 生命周期感知：onPause 自动释放，onResume 自动恢复
 */
class CameraXManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {
    companion object {
        private const val TAG = "CameraXManager"
        /** 帧跳过间隔：每 N 帧处理一帧，减少 CPU 负载 */
        private const val FRAME_SKIP_INTERVAL = 3
    }

    // 相机执行器
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    // 保存的 PreviewView，用于生命周期恢复
    private var savedPreviewView: PreviewView? = null

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
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // 构建 ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetResolution(Size(1920, 1080))
            .setFlashMode(flashMode)
            .build()

        // 构建 ImageAnalysis（实时分析）
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
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
        } catch (e: Exception) {
            Log.e(TAG, "绑定相机失败: ${e.message}", e)
            _isCameraReady.value = false
        }
    }

    /**
     * 实时分析帧 - 应用预设参数
     *
     * 帧跳过策略：每 FRAME_SKIP_INTERVAL 帧仅处理一帧，降低 CPU 负载。
     * Bitmap 所有权转移：processedBitmap 通过 mainHandler 投递给回调方，
     * 回调方在使用完毕后必须调用 bitmap.recycle() 释放内存。
     * 若已释放或无回调，则在此处立即回收 Bitmap。
     */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            // 帧跳过：每 N 帧处理一帧，减少 CPU 负载
            frameSkipCounter++
            if (frameSkipCounter % FRAME_SKIP_INTERVAL != 0) {
                return
            }

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                // 应用预设参数到实时帧
                val processedBitmap = applyPresetToFrame(bitmap, currentPresetParams)
                // 切到主线程再回调，避免在后台线程修改 Compose state
                val callback = onFrameAnalyzed
                if (callback != null && !isReleased) {
                    // Bitmap 所有权转移给回调方，回调方负责 recycle
                    mainHandler.post {
                        if (isReleased) {
                            // 在 post 排队期间被释放了，回收 Bitmap 防止泄漏
                            if (processedBitmap !== bitmap && !processedBitmap.isRecycled) processedBitmap.recycle()
                            if (!bitmap.isRecycled) bitmap.recycle()
                        } else {
                            callback(processedBitmap)
                        }
                    }
                } else {
                    // 没有订阅者或已释放，立即回收避免内存泄漏
                    if (processedBitmap !== bitmap && !processedBitmap.isRecycled) processedBitmap.recycle()
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "帧分析失败: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
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
     * 点击对焦：将屏幕坐标转换为测光点并触发对焦+测光
     *
     * 依赖已绑定的 PreviewView 的 meteringPointFactory 进行坐标映射。
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
     * 使用 OnImageCapturedCallback 在内存中获取图像，应用哈苏色彩科学后再保存到文件。
     * 在相机未绑定或未就绪时通过 onError 回调通知调用方。
     */
    fun takePhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        if (!_isCameraReady.value) {
            onError("相机未就绪")
            return
        }

        val imageCapture = imageCapture ?: run {
            onError("相机未就绪")
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()
                        if (bitmap == null) {
                            mainHandler.post { onError("图像解码失败") }
                            return
                        }
                        // 应用哈苏色彩科学处理
                        val processedBitmap = applyHasselbladColorEngine(
                            source = bitmap,
                            hasselbladParams = currentPresetParams
                        )
                        // 回收原始 bitmap（若处理后为不同实例）
                        if (processedBitmap !== bitmap && !bitmap.isRecycled) {
                            bitmap.recycle()
                        }
                        // 保存到文件
                        val photoFile = java.io.File(
                            context.cacheDir,
                            "camerax_${System.currentTimeMillis()}.jpg"
                        )
                        photoFile.outputStream().use { out ->
                            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        processedBitmap.recycle()
                        val savedUri = Uri.fromFile(photoFile)
                        Log.d(TAG, "照片已保存（已应用哈苏色彩）: $savedUri")
                        mainHandler.post { onPhotoSaved(savedUri) }
                    } catch (e: Exception) {
                        Log.e(TAG, "拍照处理失败: ${e.message}", e)
                        mainHandler.post { onError(e.message ?: "拍照失败") }
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
     * 切换前后摄像头
     */
    fun switchCamera(previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _currentLensFacing.value = lensFacing
        bindCameraUseCases(previewView)
    }

    /**
     * 切换闪光灯模式（OFF → ON → AUTO → OFF）
     *
     * 返回切换后的闪光灯模式，同时通过 [flashModeState] 暴露状态。
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
     * 将 ImageProxy 转换为 Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val buffer: ByteBuffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        // 旋转修正
        val rotation = imageProxy.imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    /**
     * 应用预设参数到帧
     *
     * 优化策略：将所有 ColorMatrix 操作（饱和度、对比度、色温、影调、青品调）合并为
     * 单一 ColorMatrix，仅执行一次 Bitmap 绘制，从 5 次分配降低到 1 次。
     * 暗角效果因使用 RadialGradient 无法合并，仍需单独绘制。
     *
     * ColorMatrix 乘法满足结合律：(A × B × C × D) × pixel = A(B(C(D(pixel)))
     * 因此可以预计算合并矩阵 M = A × B × C × D，然后 M × pixel。
     *
     * 参数映射统一使用 normalizeSigned / normalizeUnsigned（与 HasselbladColorEngine 一致），
     * 确保实时预览与导出的色彩处理结果一致。
     */
    private fun applyPresetToFrame(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        // 使用 normalizeSigned 统一参数映射（与 HasselbladColorEngine.mergeParams 一致）
        val saturation = normalizeSigned(params.saturation, 30)
        val contrast = normalizeSigned(params.contrast, 30)
        val colorTemp = normalizeSigned(params.colorTemp, 30)
        val tone = normalizeSigned(params.tone, 30)
        val cyanMagenta = normalizeSigned(params.cyanMagenta, 30)
        val vignette = normalizeUnsigned(params.vignette, 30)

        // 1) 构建 ColorMatrix（与 HasselbladColorEngine.buildColorMatrix 一致）
        val combinedMatrix = android.graphics.ColorMatrix()

        // 饱和度：接近 -1 时执行基于亮度的黑白转换
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

        // 对比度 + 影调 + 色温 + 青品调（合并为单一后置矩阵）
        val hasPostMatrix = contrast != 0f || tone != 0f || colorTemp != 0f || cyanMagenta != 0f
        if (hasPostMatrix) {
            val contrastValue = 1f + contrast
            val postMatrix = android.graphics.ColorMatrix(floatArrayOf(
                // R: 对比度 + 影调偏移 + 青品调（正值偏品，负值偏青）
                contrastValue, 0f, 0f, 0f, tone * 25f + cyanMagenta * 25f,
                // G: 对比度 + 影调偏移 - 青品调
                0f, contrastValue, 0f, 0f, tone * 10f - cyanMagenta * 20f,
                // B: 对比度 + 色温（正值减蓝偏暖，负值加蓝偏冷）+ 青品调
                0f, 0f, contrastValue, 0f, -colorTemp * 15f + cyanMagenta * 15f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(postMatrix)
        }

        // 检查是否有任何 ColorMatrix 操作
        val hasColorMatrixOps = saturation != 0f || hasPostMatrix
        val hasVignette = vignette > 0.005f

        // 如果无任何操作，直接返回原图
        if (!hasColorMatrixOps && !hasVignette) {
            return bitmap
        }

        // 2) 应用合并后的 ColorMatrix（仅一次 Bitmap 分配）
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

        // 3) 暗角效果（无法合并到 ColorMatrix，需单独绘制）
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
            // 回收上一步 current（如果不是原始输入 bitmap）
            if (current !== bitmap) current.recycle()
            current = output
        }

        return current
    }

    /**
     * 有符号参数归一化到 [-1, 1]（与 HasselbladColorEngine.normalizeSigned 一致）
     */
    private fun normalizeSigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(-1f, 1f)

    /**
     * 无符号参数归一化到 [0, 1]（与 HasselbladColorEngine.normalizeUnsigned 一致）
     */
    private fun normalizeUnsigned(value: Int, max: Int): Float =
        if (max == 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)

    /**
     * 仅释放相机绑定（保留 executor，用于 onPause/onResume 切换）
     */
    private fun releaseCamera() {
        isReleased = true
        onFrameAnalyzed = null
        cameraProvider?.unbindAll()
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
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
        Log.d(TAG, "CameraX 资源已完全释放")
    }
}