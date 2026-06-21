package com.silas.omaster.ui.features

import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val TAG = "HasselbladCameraPreview"

/**
 * 哈苏之眼 CameraX 实时预览控制器。
 * 持有 [ImageCapture] 用例，供外部触发拍照。
 */
class HasselbladCameraController {
    internal var imageCapture: ImageCapture? = null

    /**
     * 拍摄一张照片并保存到临时文件。
     *
     * @param context 上下文
     * @param executor 执行拍照的 Executor
     * @param onImageCaptured 拍照成功回调，返回 FileProvider Uri
     * @param onError 失败回调
     */
    fun takePicture(
        context: android.content.Context,
        executor: Executor,
        onImageCaptured: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError("相机未初始化完成")
            return
        }

        val photoFile = createCameraPhotoFile(context)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "拍照失败: ${exc.message}", exc)
                    onError("拍照失败: ${exc.message}")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    onImageCaptured(uri)
                }
            }
        )
    }

    private fun createCameraPhotoFile(context: android.content.Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "Hasselblad").apply {
            if (!exists()) mkdirs()
        }
        return File.createTempFile("IMG_${timeStamp}_", ".jpg", storageDir)
    }
}

/**
 * CameraX 实时相机预览组件。
 *
 * 绑定生命周期、初始化 Preview 与 ImageCapture，并通过 [controller] 暴露拍照能力。
 *
 * @param controller 外部持有的控制器，用于触发拍照
 * @param modifier 修饰符
 * @param onError 初始化或绑定失败回调
 */
@Composable
fun HasselbladCameraPreview(
    controller: HasselbladCameraController,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isInitializing by remember { mutableStateOf(true) }

    // 使用单线程 Executor 处理拍照，避免主线程压力
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = try {
                        cameraProviderFuture.get()
                    } catch (e: Exception) {
                        Log.e(TAG, "获取 CameraProvider 失败", e)
                        onError("获取相机提供者失败")
                        isInitializing = false
                        return@addListener
                    }

                    val preview = Preview.Builder()
                        .build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }

                    val imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    controller.imageCapture = imageCapture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                        isInitializing = false
                    } catch (e: Exception) {
                        Log.e(TAG, "绑定相机用例失败", e)
                        onError("相机绑定失败: ${e.message}")
                        isInitializing = false
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isInitializing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * 拍照按钮，用于触发 [controller] 拍摄。
 */
@Composable
fun HasselbladCameraShutterButton(
    controller: HasselbladCameraController,
    onCaptured: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    IconButton(
        onClick = {
            controller.takePicture(
                context = context,
                executor = executor,
                onImageCaptured = onCaptured,
                onError = onError
            )
        },
        enabled = enabled,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = "拍照",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}
