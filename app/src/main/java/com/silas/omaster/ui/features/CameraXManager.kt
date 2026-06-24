package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
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

    // 实时分析帧回调（始终在主线程触发，方便 Compose state 直接更新）
    private var onFrameAnalyzed: ((Bitmap) -> Unit)? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // 状态
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _currentLensFacing = MutableStateFlow(lensFacing)
    val currentLensFacing: StateFlow<Int> = _currentLensFacing.asStateFlow()

    // 当前应用的预设参数
    @Volatile
    private var currentParams: HasselbladParams = HasselbladParams()

    // 帧处理节流：跳过过快的帧，避免UI线程积压
    @Volatile
    private var lastFrameTime = 0L
    private val frameIntervalMs = 80L // 约12fps，平衡流畅度与性能

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
     */
    fun startCamera(previewView: PreviewView) {
        savedPreviewView = previewView
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
        val previewResolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
            .build()
        preview = Preview.Builder()
            .setResolutionSelector(previewResolutionSelector)
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // 构建 ImageCapture
        val captureResolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(Size(1920, 1080), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
            .build()
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(captureResolutionSelector)
            .setFlashMode(flashMode)
            .build()

        // 构建 ImageAnalysis（实时分析）
        val analysisResolutionSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
            .build()
        imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(analysisResolutionSelector)
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
     * 帧节流：跳过处理间隔过短的帧，避免UI线程积压导致卡顿
     */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            // 帧节流：距离上次处理不足 frameIntervalMs 则跳过
            val now = System.currentTimeMillis()
            if (now - lastFrameTime < frameIntervalMs) {
                return
            }
            lastFrameTime = now

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                // 应用预设参数到实时帧
                val processedBitmap = applyPresetToFrame(bitmap, currentParams)
                // 切到主线程再回调，避免在后台线程修改 Compose state
                val callback = onFrameAnalyzed
                if (callback != null) {
                    mainHandler.post { callback(processedBitmap) }
                } else {
                    // 没有订阅者时回收，避免内存泄漏
                    if (processedBitmap !== bitmap) processedBitmap.recycle()
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
     */
    fun setOnFrameAnalyzed(callback: ((Bitmap) -> Unit)?) {
        onFrameAnalyzed = callback
    }

    /**
     * 更新当前预设参数
     */
    fun updatePresetParams(params: HasselbladParams) {
        currentParams = params
    }

    /**
     * 拍照
     */
    fun takePhoto(onPhotoSaved: (Uri) -> Unit, onError: (String) -> Unit) {
        val imageCapture = imageCapture ?: run {
            onError("相机未就绪")
            return
        }

        val photoFile = java.io.File(
            context.cacheDir,
            "camerax_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "照片已保存: $savedUri")
                    onPhotoSaved(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exception.message}", exception)
                    onError(exception.message ?: "拍照失败")
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
     * 切换闪光灯
     */
    fun toggleFlash(): Int {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
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
     * 优化策略：将所有 ColorMatrix 操作（饱和度、对比度、色温、影调）合并为
     * 单一 ColorMatrix，仅执行一次 Bitmap 绘制，从 5 次分配降低到 1 次。
     * 暗角效果因使用 RadialGradient 无法合并，仍需单独绘制。
     *
     * ColorMatrix 乘法满足结合律：(A × B × C × D) × pixel = A(B(C(D(pixel)))
     * 因此可以预计算合并矩阵 M = A × B × C × D，然后 M × pixel。
     */
    private fun applyPresetToFrame(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        // 1) 合并所有 ColorMatrix 操作为单一矩阵
        val combinedMatrix = android.graphics.ColorMatrix()

        // 饱和度调整
        val saturation = (params.saturation + 30) / 60f * 2f
        if (saturation != 1f) {
            val satMatrix = android.graphics.ColorMatrix()
            satMatrix.setSaturation(saturation)
            combinedMatrix.postConcat(satMatrix)
        }

        // 对比度调整
        val contrast = (params.contrast + 30) / 60f * 2f
        if (contrast != 1f) {
            val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(contrastMatrix)
        }

        // 色温调整
        if (params.colorTemp != 0) {
            val warmth = params.colorTemp / 30f * 0.3f
            val tempMatrix = android.graphics.ColorMatrix(floatArrayOf(
                1f + warmth, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - warmth, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(tempMatrix)
        }

        // 影调（亮度）调整
        if (params.tone != 0) {
            val toneScale = 1f + params.tone / 30f * 0.3f
            val toneMatrix = android.graphics.ColorMatrix(floatArrayOf(
                toneScale, 0f, 0f, 0f, 0f,
                0f, toneScale, 0f, 0f, 0f,
                0f, 0f, toneScale, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            combinedMatrix.postConcat(toneMatrix)
        }

        // 检查是否有任何 ColorMatrix 操作
        val hasColorMatrixOps = saturation != 1f || contrast != 1f || params.colorTemp != 0 || params.tone != 0
        val hasVignette = params.vignette != 0

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
            val vignetteStrength = params.vignette / 30f
            val output = Bitmap.createBitmap(current.width, current.height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            canvas.drawBitmap(current, 0f, 0f, null)
            val vignettePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                shader = android.graphics.RadialGradient(
                    current.width / 2f, current.height / 2f,
                    maxOf(current.width, current.height) * 0.7f,
                    intArrayOf(0x00000000, 0x00000000, (vignetteStrength * 180).toInt() shl 24),
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
     * 仅释放相机绑定（保留 executor，用于 onPause/onResume 切换）
     */
    private fun releaseCamera() {
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