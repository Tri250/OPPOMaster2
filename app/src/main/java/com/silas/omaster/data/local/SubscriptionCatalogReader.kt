package com.silas.omaster.data.local

import android.content.Context
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.model.PresetList
import kotlinx.serialization.json.Json
import java.io.File

/**
 * FD-02① 订阅目录预览：读取本地已下载的订阅文件，解析源内预设名称列表。
 * 只读操作，失败（未下载 / 解析失败）静默返回空列表——详情页展示占位文案。
 */
object SubscriptionCatalogReader {

    private val json = Json { ignoreUnknownKeys = true }

    fun readPresetNames(context: Context, url: String, limit: Int = 20): List<String> {
        return runCatching {
            val config = ConfigCenter.getInstance(context)
            val file = File(context.filesDir, config.getSubscriptionFileName(url))
            if (!file.exists()) return emptyList()
            val list = json.decodeFromString<PresetList>(file.readText())
            list.presets.map { it.name }.filter { it.isNotBlank() }.take(limit)
        }.getOrDefault(emptyList())
    }
}
