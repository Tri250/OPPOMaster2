package com.silas.omaster.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * CameraX 实时相机预览组件
 * 2026年行业最高标准实现
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    facingMode: CameraFacing = CameraFacing.BACK,
    onFrameCaptured: (Bitmap) -> Unit,
    onCameraError: (String) -> Unit = {},
    captureInterval: Long = 2000L // 帧捕获间隔（毫秒）
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // 相机状态
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCameraActive by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            onCameraError("相机权限被拒绝")
        }
    }

    // 检查权限
    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // 启动相机
    LaunchedEffect(hasPermission, facingMode) {
        if (!hasPermission) return@LaunchedEffect

        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider

            // 预览用例
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(1920, 1080))
                .setTargetFrameRate(FrameRateRange(30, 30))
                .build()

            // 图像分析用例 - 用于帧捕获
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1920, 1080))
                .setTargetFrameRate(FrameRateRange(15, 15))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // 选择相机
            val cameraSelector = CameraSelector.Builder()
                .requireFacing(
                    when (facingMode) {
                        CameraFacing.BACK -> CameraSelector.LENS_FACING_BACK
                        CameraFacing.FRONT -> CameraSelector.LENS_FACING_FRONT
                    }
                )
                .build()

            // 解绑所有用例
            provider.unbindAll()

            // 绑定用例
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            isCameraActive = true

            // 设置预览SurfaceProvider
            previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }

            // 设置图像分析器
            imageAnalyzer.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                FrameAnalyzer(onFrameCaptured, captureInterval)
            )

        } catch (e: Exception) {
            Log.e("CameraPreview", "相机启动失败: ${e.message}")
            onCameraError("相机启动失败: ${e.message}")
        }
    }

    // 更新预览SurfaceProvider
    LaunchedEffect(previewView, cameraProvider) {
        if (previewView != null && cameraProvider != null) {
            val previewUseCase = cameraProvider?.unbindAll()
            // 重新绑定预览
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(1920, 1080))
                .build()
            preview.setSurfaceProvider(previewView!!.surfaceProvider)
        }
    }

    // 清理相机
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            isCameraActive = false
        }
    }

    // PreviewView
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPETITIVE
                previewView = this
            }
        },
        update = { view ->
            previewView = view
            // 设置预览SurfaceProvider
            cameraProvider?.let { provider ->
                val preview = Preview.Builder()
                    .setTargetResolution(android.util.Size(1920, 1080))
                    .build()
                preview.setSurfaceProvider(view.surfaceProvider)
            }
        }
    )
}

/**
 * 帧分析器 - 从视频流截取帧进行分析
 */
private class FrameAnalyzer(
    private val onFrameCaptured: (Bitmap) -> Unit,
    private val captureInterval: Long
) : ImageAnalysis.Analyzer {

    private var lastCaptureTime = 0L
    private val executor = Dispatchers.Default

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // 控制捕获频率
        if (currentTime - lastCaptureTime < captureInterval) {
            image.close()
            return
        }
        
        lastCaptureTime = currentTime

        // 在后台线程处理帧
        CoroutineScope(executor).launch {
            try {
                val bitmap = imageProxyToBitmap(image)
                bitmap?.let { onFrameCaptured(it) }
            } catch (e: Exception) {
                Log.e("FrameAnalyzer", "帧转换失败: ${e.message}")
            } finally {
                image.close()
            }
        }
    }

    /**
     * 将ImageProxy转换为Bitmap
     * 支持YUV_420_888和JPEG格式
     */
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return when (image.format) {
            ImageFormat.YUV_420_888 -> yuvToBitmap(image)
            ImageFormat.JPEG -> jpegToBitmap(image)
            else -> null
        }
    }

    /**
     * YUV_420_888转Bitmap
     */
    private fun yuvToBitmap(image: ImageProxy): Bitmap? {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        // U和V是交错的
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * JPEG转Bitmap
     */
    private fun jpegToBitmap(image: ImageProxy): Bitmap? {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

/**
 * 相机朝向
 */
enum class CameraFacing {
    BACK,  // 后置相机
    FRONT  // 前置相机
}

/**
 * 拍照功能
 */
suspend fun capturePhoto(
    camera: Camera?,
    context: android.content.Context,
    onCaptureSuccess: (Bitmap) -> Unit,
    onCaptureError: (String) -> Unit
) {
    if (camera == null) {
        onCaptureError("相机未初始化")
        return
    }

    try {
        // 使用ImageCapture用例进行拍照
        val imageCapture = ImageCapture.Builder()
            .setTargetResolution(android.util.Size(1920, 1080))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bitmap?.let { onCaptureSuccess(it) }
                        image.close()
                    } catch (e: Exception) {
                        onCaptureError("图片处理失败: ${e.message}")
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onCaptureError("拍照失败: ${exception.message}")
                }
            }
        )
    } catch (e: Exception) {
        onCaptureError("拍照失败: ${e.message}")
    }
}