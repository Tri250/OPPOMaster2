package com.silas.omaster.ui.features

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.RggbChannelVector
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.core.content.ContextCompat
import com.silas.omaster.model.HistogramData
import java.util.concurrent.Executor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 专业模式管理器
 *
 * 为 CameraXManager 提供专业模式所需的全部能力：
 * 1. 全手动参数控制（ISO、快门速度、对焦距离、白平衡色温、曝光补偿）
 * 2. 实时亮度 / R / G / B 直方图计算（256 级）
 * 3. 斑马纹（Zebra Stripes）过曝区域检测与掩码生成
 * 4. 对焦峰值（Focus Peaking）边缘检测与掩码生成
 * 5. 参数状态流 [params] 供 UI 实时订阅
 *
 * 注意：
 * - 所有图像计算均在 [Dispatchers.Default] 后台协程中执行。
 * - [processFrame] 返回的 [ZebraPeakingResult.zebraBitmap] 与 [ZebraPeakingResult.peakingBitmap]
 *   由调用方负责回收（recycle），避免 Bitmap 泄漏。
 * - Camera2Interop 相关 API 需要 [@OptIn(ExperimentalCamera2Interop::class)]。
 */
class ProModeManager(context: Context) {

    companion object {
        private const val TAG = "ProModeManager"

        /** 斑马纹亮度阈值：亮度超过 95% 视为过曝 */
        private const val ZEBRA_THRESHOLD_RATIO = 0.95

        /** 斑马纹条纹宽度（像素） */
        private const val ZEBRA_STRIPE_WIDTH = 16

        /** 对焦峰值边缘梯度阈值 */
        private const val PEAKING_EDGE_THRESHOLD = 40

        /** 裁剪判定比例：某极端 bin 像素数超过总像素 1% 视为裁剪 */
        private const val CLIPPING_RATIO = 0.01f
    }

    private val appContext: Context = context.applicationContext
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(appContext)

    /** 协程作用域：用于异步应用 Camera2 参数 */
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 当前绑定的 Camera 实例 */
    @Volatile
    private var camera: Camera? = null

    /** 当前专业模式参数状态 */
    private val _params = MutableStateFlow(ProModeParams())
    val params: StateFlow<ProModeParams> = _params.asStateFlow()

    /** 是否已释放 */
    @Volatile
    private var isReleased = false

    /**
     * 绑定 Camera 实例，并在绑定后立即应用当前保存的专业模式参数。
     *
     * @param camera CameraX 绑定完成后返回的 [Camera] 实例，传 null 表示解绑。
     */
    fun bindCamera(camera: Camera?) {
        if (isReleased) return
        this.camera = camera
        if (camera != null) {
            applyParamsToCamera()
        }
    }

    /**
     * 设置 ISO。
     *
     * @param iso ISO 值，null 表示恢复自动 ISO。
     */
    fun setIso(iso: Int?) {
        updateParams { copy(iso = iso) }
    }

    /**
     * 设置快门速度。
     *
     * @param shutterNs 快门打开时间，单位纳秒，null 表示恢复自动快门。
     */
    fun setShutterSpeedNs(shutterNs: Long?) {
        updateParams { copy(shutterSpeedNs = shutterNs) }
    }

    /**
     * 设置对焦距离。
     *
     * @param distance 归一化对焦距离 [0, 1]，0 表示远景/无穷远，1 表示最近对焦距离；null 表示自动对焦。
     */
    fun setFocusDistance(distance: Float?) {
        updateParams { copy(focusDistance = distance) }
    }

    /**
     * 设置白平衡色温。
     *
     * @param kelvin 色温值（单位 K），常见范围 2000K~10000K，null 表示自动白平衡。
     */
    fun setWhiteBalanceTemperature(kelvin: Int?) {
        updateParams { copy(whiteBalanceTemperature = kelvin) }
    }

    /**
     * 设置曝光补偿索引。
     *
     * @param index 曝光补偿索引，具体有效范围由设备 [androidx.camera.core.CameraInfo.exposureState] 决定。
     */
    fun setExposureCompensation(index: Int) {
        updateParams { copy(exposureCompensation = index) }
    }

    /**
     * 处理一帧预览图像，计算直方图、斑马纹和对焦峰值。
     *
     * 方法在后台线程执行，不会阻塞调用线程。
     * 返回的 Bitmap 掩码由调用方负责回收。
     *
     * @param bitmap 输入图像，必须未被回收。
     * @return Pair<直方图数据, 斑马纹与对焦峰值结果>
     */
    suspend fun processFrame(bitmap: Bitmap): Pair<HistogramData, ZebraPeakingResult> =
        withContext(Dispatchers.Default) {
            require(!bitmap.isRecycled) { "输入 Bitmap 已回收" }
            analyzeFrameInternal(bitmap)
        }

    /**
     * 释放资源。
     *
     * 释放后，所有设置方法与处理方法均不再生效。
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        camera = null
        managerScope.cancel()
    }

    /**
     * 更新参数状态并尝试立即应用到相机。
     */
    private inline fun updateParams(transform: ProModeParams.() -> ProModeParams) {
        if (isReleased) return
        _params.value = _params.value.transform()
        applyParamsToCamera()
    }

    /**
     * 将当前参数通过 Camera2Interop 应用到已绑定的相机。
     *
     * 实现要点：
     * - ISO / 快门速度：当任一参数手动设置时，关闭自动曝光（AE），并写入 SENSOR_SENSITIVITY 与 SENSOR_EXPOSURE_TIME。
     * - 对焦距离：手动设置时关闭连续自动对焦（AF），写入 LENS_FOCUS_DISTANCE（归一化值会映射为实际 diopter）。
     * - 白平衡色温：手动设置时关闭自动白平衡（AWB），写入 COLOR_CORRECTION_GAINS。
     * - 曝光补偿：在自动曝光模式下通过 CameraX CameraControl 设置；手动曝光模式下仅保存状态。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun applyParamsToCamera() {
        val camera = this.camera ?: return
        val params = _params.value

        try {
            val characteristics = getCameraCharacteristics(camera)
            val isoRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val exposureTimeRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val minFocusDistance = characteristics?.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

            val camera2Control = Camera2CameraControl.from(camera.cameraControl)
            val builder = CaptureRequestOptions.Builder()

            // ----- 曝光控制 -----
            val manualExposure = params.iso != null || params.shutterSpeedNs != null
            if (manualExposure) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF
                )
                params.iso?.let { iso ->
                    val clampedIso = isoRange?.clamp(iso) ?: iso
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)
                }
                params.shutterSpeedNs?.let { shutterNs ->
                    val clampedShutter = exposureTimeRange?.clamp(shutterNs) ?: shutterNs
                    builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedShutter)
                }
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON
                )
            }

            // ----- 对焦控制 -----
            params.focusDistance?.let { distance ->
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
                )
                val actualDiopters = if (minFocusDistance > 0f) {
                    distance.coerceIn(0f, 1f) * minFocusDistance
                } else {
                    distance.coerceIn(0f, 1f)
                }
                builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, actualDiopters)
            } ?: run {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }

            // ----- 白平衡控制 -----
            params.whiteBalanceTemperature?.let { kelvin ->
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CameraMetadata.CONTROL_AWB_MODE_OFF
                )
                val gains = kelvinToRggbGains(kelvin)
                builder.setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            } ?: run {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CameraMetadata.CONTROL_AWB_MODE_AUTO
                )
            }

            // 提交 Camera2Interop 请求
            val future = camera2Control.setCaptureRequestOptions(builder.build())
            future.addListener({
                try {
                    future.get()
                } catch (e: Exception) {
                    Log.e(TAG, "Camera2Interop 参数提交失败", e)
                }
            }, mainExecutor)

            // ----- 曝光补偿（仅在自动曝光时生效）-----
            if (!manualExposure) {
                camera.cameraControl.setExposureCompensationIndex(params.exposureCompensation)
            }
        } catch (e: Exception) {
            Log.e(TAG, "应用专业模式参数失败", e)
        }
    }

    /**
     * 获取当前相机的 [CameraCharacteristics]，用于查询设备能力范围。
     */
    @OptIn(ExperimentalCamera2Interop::class)
    private fun getCameraCharacteristics(camera: Camera): CameraCharacteristics? {
        return try {
            Camera2CameraInfo.from(camera.cameraInfo).getCameraCharacteristics()
        } catch (e: Exception) {
            Log.w(TAG, "获取 CameraCharacteristics 失败", e)
            null
        }
    }

    /**
     * 将色温（K）转换为 RGGB 色彩校正增益。
     *
     * 算法步骤：
     * 1. 使用黑体辐射近似公式计算当前色温下的 RGB 响应。
     * 2. 将响应归一化到 green = 1.0，得到各通道需要施加的校正增益。
     *
     * @param kelvin 色温值（K）
     * @return 可用于 [CaptureRequest.COLOR_CORRECTION_GAINS] 的 [RggbChannelVector]
     */
    private fun kelvinToRggbGains(kelvin: Int): RggbChannelVector {
        val (r, g, b) = kelvinToRgb(kelvin.toFloat())
        val rNorm = (r / 255f).coerceAtLeast(0.01f)
        val gNorm = (g / 255f).coerceAtLeast(0.01f)
        val bNorm = (b / 255f).coerceAtLeast(0.01f)

        // 以绿色为基准，计算 R/B 通道的相对增益，用于抵消环境光色偏
        val redGain = gNorm / rNorm
        val blueGain = gNorm / bNorm
        return RggbChannelVector(redGain, 1f, 1f, blueGain)
    }

    /**
     * 色温转 RGB（黑体近似公式）。
     *
     * 参考：http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
     */
    private fun kelvinToRgb(kelvin: Float): Triple<Float, Float, Float> {
        val t = kelvin / 100f
        val r: Float
        val g: Float
        val b: Float

        if (t <= 66f) {
            r = 255f
            g = (99.4708025861f * ln(t) - 161.1195681661f).toFloat()
            b = if (t <= 19f) {
                0f
            } else {
                (138.5177312231f * ln(t - 10f) - 305.0447927307f).toFloat()
            }
        } else {
            r = (329.698727446f * (t - 60f).pow(-0.1332047592f)).toFloat()
            g = (288.1221695283f * (t - 60f).pow(-0.0755148492f)).toFloat()
            b = 255f
        }

        return Triple(
            r.coerceIn(0f, 255f),
            g.coerceIn(0f, 255f),
            b.coerceIn(0f, 255f)
        )
    }

    /**
     * 单帧图像分析：直方图 + 斑马纹 + 对焦峰值。
     *
     * 所有计算基于输入 Bitmap 的原始像素，不修改输入图像。
     */
    private fun analyzeFrameInternal(bitmap: Bitmap): Pair<HistogramData, ZebraPeakingResult> {
        val width = bitmap.width
        val height = bitmap.height
        val total = width * height
        val pixels = IntArray(total)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // ----- 直方图统计 -----
        val lumHist = IntArray(256)
        val rHist = IntArray(256)
        val gHist = IntArray(256)
        val bHist = IntArray(256)
        val gray = IntArray(total)

        var sumLum = 0L
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            // 使用 Rec.601 亮度系数
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

            lumHist[lum]++
            rHist[r]++
            gHist[g]++
            bHist[b]++
            gray[i] = lum
            sumLum += lum
        }

        val meanLuminance = if (total > 0) sumLum.toFloat() / total else 0f
        val shadowClipping = lumHist[0] > total * CLIPPING_RATIO
        val highlightClipping = lumHist[255] > total * CLIPPING_RATIO

        val histogramData = HistogramData(
            luminance = lumHist,
            red = rHist,
            green = gHist,
            blue = bHist,
            meanLuminance = meanLuminance,
            shadowClipping = shadowClipping,
            highlightClipping = highlightClipping
        )

        // ----- 斑马纹 + 对焦峰值 -----
        val zebraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val peakingBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val zebraPixels = IntArray(total)
        val peakingPixels = IntArray(total)

        val zebraThreshold = (255 * ZEBRA_THRESHOLD_RATIO).toInt()
        var overExposedCount = 0
        var edgeCount = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val lum = gray[idx]

                // 斑马纹：过曝区域绘制对角斜条纹
                if (lum > zebraThreshold) {
                    overExposedCount++
                    val isBlackStripe = ((x + y) / ZEBRA_STRIPE_WIDTH) % 2 == 0
                    zebraPixels[idx] = if (isBlackStripe) 0xFF000000.toInt() else 0x00000000
                } else {
                    zebraPixels[idx] = 0x00000000
                }

                // 对焦峰值：Sobel 算子检测边缘高频区域
                if (x == 0 || x == width - 1 || y == 0 || y == height - 1) {
                    peakingPixels[idx] = 0x00000000
                    continue
                }

                val gx = (
                    -gray[(y - 1) * width + (x - 1)]
                        - 2 * gray[y * width + (x - 1)]
                        - gray[(y + 1) * width + (x - 1)]
                        + gray[(y - 1) * width + (x + 1)]
                        + 2 * gray[y * width + (x + 1)]
                        + gray[(y + 1) * width + (x + 1)]
                    )
                val gy = (
                    -gray[(y - 1) * width + (x - 1)]
                        - 2 * gray[(y - 1) * width + x]
                        - gray[(y - 1) * width + (x + 1)]
                        + gray[(y + 1) * width + (x - 1)]
                        + 2 * gray[(y + 1) * width + x]
                        + gray[(y + 1) * width + (x + 1)]
                    )
                val magnitude = sqrt((gx * gx + gy * gy).toFloat()).toInt()

                if (magnitude > PEAKING_EDGE_THRESHOLD) {
                    edgeCount++
                    // 绿色对焦峰值叠加色
                    peakingPixels[idx] = 0xFF00FF00.toInt()
                } else {
                    peakingPixels[idx] = 0x00000000
                }
            }
        }

        zebraBitmap.setPixels(zebraPixels, 0, width, 0, 0, width, height)
        peakingBitmap.setPixels(peakingPixels, 0, width, 0, 0, width, height)

        val overExposedRatio = if (total > 0) overExposedCount.toFloat() / total else 0f
        val focusedEdgeRatio = if (total > 0) edgeCount.toFloat() / total else 0f

        // 若检测结果为空，则直接回收掩码 Bitmap 避免浪费内存
        val finalZebraBitmap = if (overExposedCount > 0) zebraBitmap else {
            zebraBitmap.recycle()
            null
        }
        val finalPeakingBitmap = if (edgeCount > 0) peakingBitmap else {
            peakingBitmap.recycle()
            null
        }

        val zebraPeakingResult = ZebraPeakingResult(
            zebraBitmap = finalZebraBitmap,
            peakingBitmap = finalPeakingBitmap,
            overExposedRatio = overExposedRatio,
            focusedEdgeRatio = focusedEdgeRatio
        )

        return histogramData to zebraPeakingResult
    }
}

/**
 * 斑马纹与对焦峰值处理结果。
 *
 * @param zebraBitmap 斑马纹掩码 Bitmap，过曝区域为黑白斜条纹，其余透明；无过曝时为 null。
 * @param peakingBitmap 对焦峰值掩码 Bitmap，边缘高频区域为绿色，其余透明；无边缘时为 null。
 * @param overExposedRatio 过曝像素占全图比例（0-1）。
 * @param focusedEdgeRatio 检测到的强边缘像素占全图比例（0-1）。
 */
data class ZebraPeakingResult(
    val zebraBitmap: Bitmap?,
    val peakingBitmap: Bitmap?,
    val overExposedRatio: Float,
    val focusedEdgeRatio: Float
)
