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

    // 实时分析帧回调
    private var onFrameAnalyzed: ((Bitmap) -> Unit)? = null

    // 状态
    private val _isCameraReady = MutableStateFlow(false)
    val isCameraReady: StateFlow<Boolean> = _isCameraReady.asStateFlow()

    private val _currentLensFacing = MutableStateFlow(lensFacing)
    val currentLensFacing: StateFlow<Int> = _currentLensFacing.asStateFlow()

    // 当前应用的预设参数
    private var currentParams: HasselbladParams = HasselbladParams()

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
     */
    private fun analyzeFrame(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                // 应用预设参数到实时帧
                val processedBitmap = applyPresetToFrame(bitmap, currentParams)
                onFrameAnalyzed?.invoke(processedBitmap)
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
    fun setOnFrameAnalyzed(callback: (Bitmap) -> Unit) {
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
     */
    private fun applyPresetToFrame(bitmap: Bitmap, params: HasselbladParams): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(result)
        val paint = android.graphics.Paint()

        var needsFilter = false

        // 饱和度调整
        val saturation = (params.saturation + 30) / 60f * 2f
        if (saturation != 1f) {
            val cm = android.graphics.ColorMatrix()
            cm.setSaturation(saturation)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(result, 0f, 0f, paint)
            needsFilter = true
        }

        // 对比度调整
        val contrast = (params.contrast + 30) / 60f * 2f
        if (contrast != 1f) {
            val cm = android.graphics.ColorMatrix()
            cm.setScale(contrast, contrast, contrast, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(if (needsFilter) result else bitmap, 0f, 0f, paint)
            needsFilter = true
        }

        // 色温调整
        if (params.colorTemp != 0) {
            val warmth = params.colorTemp / 30f * 0.3f
            val cm = android.graphics.ColorMatrix()
            cm.set(floatArrayOf(
                1f + warmth, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f - warmth, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(if (needsFilter) result else bitmap, 0f, 0f, paint)
            needsFilter = true
        }

        // 影调调整
        if (params.tone != 0) {
            val toneScale = 1f + params.tone / 30f * 0.3f
            val cm = android.graphics.ColorMatrix()
            cm.setScale(toneScale, toneScale, toneScale, 1f)
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            canvas.drawBitmap(if (needsFilter) result else bitmap, 0f, 0f, paint)
            needsFilter = true
        }

        // 暗角效果
        if (params.vignette != 0) {
            val vignetteStrength = params.vignette / 30f
            val vignettePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                shader = android.graphics.RadialGradient(
                    bitmap.width / 2f, bitmap.height / 2f,
                    maxOf(bitmap.width, bitmap.height) * 0.7f,
                    intArrayOf(0x00000000, 0x00000000, (vignetteStrength * 180).toInt() shl 24),
                    floatArrayOf(0f, 0.6f, 1f),
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), vignettePaint)
        }

        paint.colorFilter = null
        return result
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