package com.silas.omaster.data.watermark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 水印数据管理器
 * 负责水印配置的持久化、读取和应用
 */
class WatermarkRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val settingsManager = SettingsManager.getInstance(context)
    private val json = Json { ignoreUnknownKeys = true }

    private val _watermarks = MutableStateFlow<List<WatermarkConfig>>(emptyList())
    val watermarks: StateFlow<List<WatermarkConfig>> = _watermarks.asStateFlow()

    private val _selectedWatermarkId = MutableStateFlow<String?>(null)
    val selectedWatermarkId: StateFlow<String?> = _selectedWatermarkId.asStateFlow()

    init {
        loadWatermarks()
    }

    /**
     * 加载已保存的水印配置
     */
    private fun loadWatermarks() {
        try {
            val jsonStr = settingsManager.watermarksJson
            if (jsonStr.isNotBlank()) {
                val list = json.decodeFromString<List<WatermarkConfig>>(jsonStr)
                _watermarks.value = list
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "加载水印配置失败", e)
        }
    }

    /**
     * 保存水印配置列表
     */
    private fun saveWatermarks() {
        try {
            val jsonStr = json.encodeToString(_watermarks.value)
            settingsManager.watermarksJson = jsonStr
        } catch (e: Exception) {
            android.util.Log.e(TAG, "保存水印配置失败", e)
        }
    }

    /**
     * 创建品牌水印
     */
    fun createBrandWatermark(
        name: String,
        text: String,
        fontColor: String = "#FFFFFF",
        fontSize: Float = 48f,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT
    ): WatermarkConfig {
        val config = WatermarkConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            type = WatermarkType.BRAND,
            text = text,
            fontColor = fontColor,
            fontSize = fontSize,
            position = position
        )
        addWatermark(config)
        return config
    }

    /**
     * 创建大师印记水印
     */
    fun createMasterMarkWatermark(
        name: String,
        signatureUri: Uri,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        alpha: Float = 0.8f
    ): WatermarkConfig {
        // 保存签名图到内部存储
        val signaturePath = saveSignatureBitmap(signatureUri)
        val config = WatermarkConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            type = WatermarkType.MASTER_MARK,
            signatureBitmapPath = signaturePath,
            signatureAlpha = alpha,
            position = position
        )
        addWatermark(config)
        return config
    }

    /**
     * 创建 XPAN 宽幅水印
     */
    fun createXpanWatermark(
        name: String,
        topRatio: Float = 0.15f,
        bottomRatio: Float = 0.15f,
        topText: String = "",
        bottomText: String = ""
    ): WatermarkConfig {
        val config = WatermarkConfig(
            id = UUID.randomUUID().toString(),
            name = name,
            type = WatermarkType.XPAN,
            xpanTopRatio = topRatio.coerceIn(0.05f, 0.4f),
            xpanBottomRatio = bottomRatio.coerceIn(0.05f, 0.4f),
            xpanTextTop = topText,
            xpanTextBottom = bottomText
        )
        addWatermark(config)
        return config
    }

    /**
     * 添加水印配置
     */
    fun addWatermark(config: WatermarkConfig) {
        val current = _watermarks.value.toMutableList()
        current.add(config)
        _watermarks.value = current
        saveWatermarks()
    }

    /**
     * 更新水印配置
     */
    fun updateWatermark(config: WatermarkConfig) {
        val current = _watermarks.value.toMutableList()
        val index = current.indexOfFirst { it.id == config.id }
        if (index >= 0) {
            current[index] = config
            _watermarks.value = current
            saveWatermarks()
        }
    }

    /**
     * 删除水印配置
     */
    fun deleteWatermark(watermarkId: String) {
        val current = _watermarks.value.toMutableList()
        current.removeAll { it.id == watermarkId }
        _watermarks.value = current
        saveWatermarks()
    }

    /**
     * 选择当前使用的水印
     */
    fun selectWatermark(watermarkId: String?) {
        _selectedWatermarkId.value = watermarkId
        settingsManager.selectedWatermarkId = watermarkId ?: ""
    }

    /**
     * 应用选中的水印到图片
     */
    suspend fun applySelectedWatermark(source: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        val watermarkId = _selectedWatermarkId.value ?: return@withContext null
        val config = _watermarks.value.find { it.id == watermarkId } ?: return@withContext null

        val signatureBitmap = config.signatureBitmapPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        }

        WatermarkEngine.applyWatermark(source, config, signatureBitmap)
    }

    /**
     * 保存签名图到内部存储
     */
    private fun saveSignatureBitmap(uri: Uri): String {
        val dir = File(appContext.filesDir, "signatures").apply { mkdirs() }
        val file = File(dir, "signature_${System.currentTimeMillis()}.png")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    companion object {
        private const val TAG = "WatermarkRepository"

        @Volatile
        private var instance: WatermarkRepository? = null

        fun getInstance(context: Context): WatermarkRepository {
            return instance ?: synchronized(this) {
                instance ?: WatermarkRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * SettingsManager 扩展属性（用于水印配置持久化）
 */
private var SettingsManager.watermarksJson: String
    get() = getDataSync(java.lang.String::class.java, "watermarks_json", "")
    set(value) = setDataSync("watermarks_json", value)

private var SettingsManager.selectedWatermarkId: String
    get() = getDataSync(java.lang.String::class.java, "selected_watermark_id", "")
    set(value) = setDataSync("selected_watermark_id", value)
