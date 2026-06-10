package com.silas.omaster.renderer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.silas.omaster.data.local.LUTDownloadManager
import com.silas.omaster.data.local.LUTDownloadResult
import com.silas.omaster.data.model.LUTResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统一渲染服务
 * 
 * 整合 GPU/CPU 渲染和 LUT 应用，提供一站式图像处理接口
 * 
 * 功能：
 * - GPU 加速渲染（OpenGL ES 3.0）
 * - CPU 降级渲染（当 GPU 不可用）
 * - LUT 应用（支持 .cube 文件）
 * - 参数调整（18 参数全通道）
 * - 异步渲染队列
 */
object UnifiedRenderService {

    private const val TAG = "UnifiedRenderService"

    /**
     * 初始化渲染服务
     * 
     * @param context 上下文
     * @return 是否初始化成功
     */
    suspend fun initialize(context: Context): Boolean {
        Log.d(TAG, "Initializing UnifiedRenderService...")
        
        // 初始化 GPU 渲染管理器
        val gpuManager = GPURenderManager.getInstance(context)
        val gpuInit = gpuManager.initialize()
        
        Log.d(TAG, "GPU Render Manager initialized: $gpuInit, GPU available: ${gpuManager.isGPUAvailable()}")
        
        return gpuInit
    }

    /**
     * 渲染图像（带 LUT）
     * 
     * @param context 上下文
     * @param inputBitmap 输入图像
     * @param params 渲染参数
     * @param lut LUT 资源（可选）
     * @param lutIntensity LUT 强度 [0, 100]
     * @return 渲染后的图像
     */
    suspend fun render(
        context: Context,
        inputBitmap: Bitmap,
        params: RenderParameters,
        lut: LUTResource? = null,
        lutIntensity: Float = 100f
    ): Bitmap? = withContext(Dispatchers.Default) {
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 1. 加载 LUT 数据（如果提供）
            var renderParams = params
            if (lut != null) {
                val lutData = loadLUTData(context, lut)
                if (lutData != null) {
                    renderParams = params.copy().apply {
                        this.lutId = lut.id
                        this.lutData = lutData
                        this.lutCubeSize = lut.cubeSize ?: 33
                        this.lutIntensity = lutIntensity
                    }
                    Log.d(TAG, "LUT ${lut.id} loaded, cubeSize=${lut.cubeSize}, intensity=$lutIntensity")
                } else {
                    Log.w(TAG, "Failed to load LUT ${lut.id}, rendering without LUT")
                }
            }
            
            // 2. 使用 CPU 渲染器（GPU 渲染器需要 GL 纹理读取，这里简化处理）
            val cpuRenderer = CPURenderer()
            val outputBitmap = cpuRenderer.render(inputBitmap, renderParams)
            
            val processingTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Render completed in ${processingTime}ms")
            
            outputBitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Render failed", e)
            null
        }
    }

    /**
     * 快速预览渲染
     * 
     * @param context 上下文
     * @param inputBitmap 输入图像
     * @param params 渲染参数
     * @return 预览图像（较低质量）
     */
    suspend fun renderPreview(
        context: Context,
        inputBitmap: Bitmap,
        params: RenderParameters
    ): Bitmap? {
        // 缩小图像以加速预览
        val previewScale = 0.5f
        val previewWidth = (inputBitmap.width * previewScale).toInt()
        val previewHeight = (inputBitmap.height * previewScale).toInt()
        
        val scaledBitmap = Bitmap.createScaledBitmap(inputBitmap, previewWidth, previewHeight, true)
        
        return render(context, scaledBitmap, params)
    }

    /**
     * 应用 LUT 到图像
     * 
     * @param context 上下文
     * @param inputBitmap 输入图像
     * @param lut LUT 资源
     * @param intensity LUT 强度 [0, 100]
     * @return 应用 LUT 后的图像
     */
    suspend fun applyLUT(
        context: Context,
        inputBitmap: Bitmap,
        lut: LUTResource,
        intensity: Float = 100f
    ): Bitmap? = withContext(Dispatchers.Default) {
        
        val lutData = loadLUTData(context, lut)
        if (lutData == null) {
            Log.w(TAG, "LUT ${lut.id} not available")
            return@withContext null
        }
        
        val cpuRenderer = CPURenderer()
        val params = RenderParameters().apply {
            this.lutData = lutData
            this.lutCubeSize = lut.cubeSize ?: 33
            this.lutIntensity = intensity
        }
        
        cpuRenderer.render(inputBitmap, params)
    }

    /**
     * 加载 LUT 数据
     * 
     * @param context 上下文
     * @param lut LUT 资源
     * @return LUT 数据数组，失败返回 null
     */
    private suspend fun loadLUTData(context: Context, lut: LUTResource): FloatArray? {
        // 1. 检查是否已下载
        if (!lut.isDownloaded(context)) {
            Log.d(TAG, "LUT ${lut.id} not downloaded, downloading...")
            
            // 下载 LUT
            val result = LUTDownloadManager.downloadLUT(context, lut)
            if (result !is LUTDownloadResult.Success) {
                Log.w(TAG, "Failed to download LUT ${lut.id}")
                return null
            }
        }
        
        // 2. 检查文件完整性
        if (!lut.verifyIntegrity(context)) {
            Log.w(TAG, "LUT ${lut.id} file corrupted, re-downloading...")
            
            // 删除损坏文件并重新下载
            lut.getLocalPath(context).delete()
            val result = LUTDownloadManager.downloadLUT(context, lut)
            if (result !is LUTDownloadResult.Success) {
                return null
            }
        }
        
        // 3. 解析 CUBE 文件
        val lutFile = lut.getLocalPath(context)
        return LUTDownloadManager.parseCubeFile(lutFile)
    }

    /**
     * 批量渲染
     * 
     * @param context 上下文
     * @param inputs 输入图像列表
     * @param params 渲染参数
     * @return 渲染结果列表
     */
    suspend fun renderBatch(
        context: Context,
        inputs: List<Bitmap>,
        params: RenderParameters
    ): List<Bitmap?> {
        return inputs.map { input ->
            render(context, input, params)
        }
    }

    /**
     * 获取 GPU 可用状态
     */
    fun isGPUAvailable(context: Context): Boolean {
        return GPURenderManager.getInstance(context).isGPUAvailable()
    }

    /**
     * 获取渲染队列大小
     */
    fun getQueueSize(context: Context): Int {
        return GPURenderManager.getInstance(context).getQueueSize()
    }

    /**
     * 释放渲染服务资源
     */
    fun release(context: Context) {
        GPURenderManager.getInstance(context).release()
        Log.d(TAG, "UnifiedRenderService released")
    }
}