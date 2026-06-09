package com.silas.omaster.tflite

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 模型加载器
 * 
 * 负责从assets或文件系统加载TFLite模型文件
 * 支持模型下载、缓存和版本管理
 * 
 * 功能：
 * - 从assets加载模型
 * - 从文件系统加载模型
 * - 模型缓存管理
 * - 模型版本检查
 * - 模型完整性验证
 */
class ModelLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelLoader"
        
        // 模型目录
        private const val MODELS_DIR = "models"
        
        // 模型版本文件
        private const val VERSION_FILE = "model_versions.json"
        
        // 模型文件信息
        val MODEL_INFO = mapOf(
            TFLiteEngine.MODEL_SCENE_CLASSIFIER to ModelFileInfo(
                name = TFLiteEngine.MODEL_SCENE_CLASSIFIER,
                expectedSize = 700 * 1024, // 约700KB
                version = "1.0.0",
                description = "场景分类模型 - MobileNetV3变体，支持36+场景类型"
            ),
            TFLiteEngine.MODEL_QUALITY_ANALYZER to ModelFileInfo(
                name = TFLiteEngine.MODEL_QUALITY_ANALYZER,
                expectedSize = 500 * 1024, // 约500KB
                version = "1.0.0",
                description = "图像质量分析模型 - NIMA变体，评估亮度、对比度、噪点、模糊度"
            ),
            TFLiteEngine.MODEL_PARAM_PREDICTOR to ModelFileInfo(
                name = TFLiteEngine.MODEL_PARAM_PREDICTOR,
                expectedSize = 200 * 1024, // 约200KB
                version = "1.0.0",
                description = "参数预测模型 - 全连接网络，输出18个调校参数"
            )
        )
    }
    
    /**
     * 模型文件信息
     */
    data class ModelFileInfo(
        val name: String,
        val expectedSize: Long,
        val version: String,
        val description: String
    )
    
    /**
     * 加载模型状态
     */
    data class ModelLoadStatus(
        val modelName: String,
        val isAvailable: Boolean,
        val location: ModelLocation,
        val size: Long,
        val version: String,
        val isValid: Boolean
    )
    
    /**
     * 模型存储位置
     */
    enum class ModelLocation {
        ASSETS,      // 存储在assets中
        FILE_SYSTEM, // 存储在文件系统中
        NOT_FOUND    // 未找到
    }
    
    /**
     * 加载模型ByteBuffer
     * 
     * @param modelName 模型名称
     * @return ByteBuffer或null
     */
    suspend fun loadModelBuffer(modelName: String): ByteBuffer? = withContext(Dispatchers.IO) {
        try {
            // 首先尝试从assets加载
            val assetBuffer = loadFromAssets(modelName)
            if (assetBuffer != null) {
                Log.i(TAG, "模型从assets加载成功: $modelName")
                return@withContext assetBuffer
            }
            
            // 尝试从文件系统加载
            val fileBuffer = loadFromFileSystem(modelName)
            if (fileBuffer != null) {
                Log.i(TAG, "模型从文件系统加载成功: $modelName")
                return@withContext fileBuffer
            }
            
            Log.w(TAG, "模型文件未找到: $modelName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "加载模型失败: $modelName", e)
            null
        }
    }
    
    /**
     * 从assets加载模型
     */
    private fun loadFromAssets(modelName: String): ByteBuffer? {
        return try {
            // 尝试多个可能的路径
            val possiblePaths = listOf(
                "$MODELS_DIR/$modelName",
                modelName,
                "tflite/$modelName"
            )
            
            for (path in possiblePaths) {
                try {
                    val inputStream = context.assets.open(path)
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    
                    val buffer = ByteBuffer.allocateDirect(bytes.size)
                    buffer.order(ByteOrder.nativeOrder())
                    buffer.put(bytes)
                    buffer.rewind()
                    
                    Log.d(TAG, "从assets路径加载成功: $path, 大小: ${bytes.size}字节")
                    return buffer
                } catch (e: Exception) {
                    // 继续尝试下一个路径
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "从assets加载模型失败: $modelName", e)
            null
        }
    }
    
    /**
     * 从文件系统加载模型
     */
    private fun loadFromFileSystem(modelName: String): ByteBuffer? {
        return try {
            val modelDir = File(context.filesDir, MODELS_DIR)
            val modelFile = File(modelDir, modelName)
            
            if (!modelFile.exists()) {
                return null
            }
            
            val bytes = modelFile.readBytes()
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            
            Log.d(TAG, "从文件系统加载成功: ${modelFile.absolutePath}, 大小: ${bytes.size}字节")
            buffer
        } catch (e: Exception) {
            Log.e(TAG, "从文件系统加载模型失败: $modelName", e)
            null
        }
    }
    
    /**
     * 检查模型是否可用
     */
    fun isModelAvailable(modelName: String): Boolean {
        return checkModelLocation(modelName) != ModelLocation.NOT_FOUND
    }
    
    /**
     * 检查模型存储位置
     */
    fun checkModelLocation(modelName: String): ModelLocation {
        // 检查assets
        val possiblePaths = listOf("$MODELS_DIR/$modelName", modelName, "tflite/$modelName")
        for (path in possiblePaths) {
            try {
                context.assets.open(path).close()
                return ModelLocation.ASSETS
            } catch (e: Exception) {
                // 继续
            }
        }
        
        // 检查文件系统
        val modelDir = File(context.filesDir, MODELS_DIR)
        val modelFile = File(modelDir, modelName)
        if (modelFile.exists()) {
            return ModelLocation.FILE_SYSTEM
        }
        
        return ModelLocation.NOT_FOUND
    }
    
    /**
     * 获取所有模型的加载状态
     */
    fun getAllModelStatus(): List<ModelLoadStatus> {
        return MODEL_INFO.keys.map { modelName ->
            getModelStatus(modelName)
        }
    }
    
    /**
     * 获取单个模型的加载状态
     */
    fun getModelStatus(modelName: String): ModelLoadStatus {
        val info = MODEL_INFO[modelName]
        val location = checkModelLocation(modelName)
        val isAvailable = location != ModelLocation.NOT_FOUND
        
        val size = when (location) {
            ModelLocation.ASSETS -> {
                try {
                    val inputStream = context.assets.open("$MODELS_DIR/$modelName")
                    val size = inputStream.available()
                    inputStream.close()
                    size.toLong()
                } catch (e: Exception) {
                    0L
                }
            }
            ModelLocation.FILE_SYSTEM -> {
                val modelDir = File(context.filesDir, MODELS_DIR)
                val modelFile = File(modelDir, modelName)
                modelFile.length()
            }
            ModelLocation.NOT_FOUND -> 0L
        }
        
        val isValid = isAvailable && verifyModelIntegrity(modelName, size)
        
        return ModelLoadStatus(
            modelName = modelName,
            isAvailable = isAvailable,
            location = location,
            size = size,
            version = info?.version ?: "unknown",
            isValid = isValid
        )
    }
    
    /**
     * 验证模型完整性
     * 
     * 检查模型大小是否符合预期
     */
    private fun verifyModelIntegrity(modelName: String, actualSize: Long): Boolean {
        val info = MODEL_INFO[modelName]
        if (info == null) return false
        
        // 允许10%的大小误差（量化模型大小可能有变化）
        val minSize = info.expectedSize * 0.9
        val maxSize = info.expectedSize * 1.1
        
        return actualSize >= minSize && actualSize <= maxSize
    }
    
    /**
     * 将assets中的模型复制到文件系统
     * 
     * 用于模型缓存和离线使用
     */
    suspend fun copyModelToFileSystem(modelName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelDir = File(context.filesDir, MODELS_DIR)
            if (!modelDir.exists()) {
                modelDir.mkdirs()
            }
            
            val modelFile = File(modelDir, modelName)
            
            // 如果文件已存在且完整，跳过
            if (modelFile.exists() && verifyModelIntegrity(modelName, modelFile.length())) {
                Log.d(TAG, "模型已存在且完整，跳过复制: $modelName")
                return@withContext true
            }
            
            // 从assets复制
            val possiblePaths = listOf("$MODELS_DIR/$modelName", modelName, "tflite/$modelName")
            var copied = false
            
            for (path in possiblePaths) {
                try {
                    val inputStream = context.assets.open(path)
                    val outputStream = FileOutputStream(modelFile)
                    
                    inputStream.copyTo(outputStream)
                    
                    inputStream.close()
                    outputStream.close()
                    
                    copied = true
                    Log.i(TAG, "模型复制成功: $modelName -> ${modelFile.absolutePath}")
                    break
                } catch (e: Exception) {
                    // 继续尝试下一个路径
                }
            }
            
            copied
        } catch (e: Exception) {
            Log.e(TAG, "复制模型到文件系统失败: $modelName", e)
            false
        }
    }
    
    /**
     * 删除文件系统中的模型缓存
     */
    fun deleteModelCache(modelName: String): Boolean {
        return try {
            val modelDir = File(context.filesDir, MODELS_DIR)
            val modelFile = File(modelDir, modelName)
            
            if (modelFile.exists()) {
                modelFile.delete()
                Log.i(TAG, "模型缓存已删除: $modelName")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "删除模型缓存失败: $modelName", e)
            false
        }
    }
    
    /**
     * 清除所有模型缓存
     */
    fun clearAllModelCache(): Boolean {
        return try {
            val modelDir = File(context.filesDir, MODELS_DIR)
            if (modelDir.exists()) {
                modelDir.deleteRecursively()
                Log.i(TAG, "所有模型缓存已清除")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "清除所有模型缓存失败", e)
            false
        }
    }
    
    /**
     * 获取模型缓存目录大小
     */
    fun getCacheSize(): Long {
        val modelDir = File(context.filesDir, MODELS_DIR)
        if (!modelDir.exists()) return 0L
        
        return modelDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * 获取模型描述
     */
    fun getModelDescription(modelName: String): String {
        return MODEL_INFO[modelName]?.description ?: "未知模型"
    }
    
    /**
     * 获取模型预期大小
     */
    fun getExpectedModelSize(modelName: String): Long {
        return MODEL_INFO[modelName]?.expectedSize ?: 0L
    }
}