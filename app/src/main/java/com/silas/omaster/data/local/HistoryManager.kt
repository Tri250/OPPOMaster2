package com.silas.omaster.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 用户操作历史记录
 * 记录最近使用的预设、编辑记录等
 */
@Serializable
data class UsageRecord(
    val presetId: String,
    val presetName: String,
    val action: String, // "applied", "edited", "viewed"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 历史记录管理器
 * 最多保留 50 条记录，自动按时间排序
 */
class HistoryManager private constructor(context: Context) {

    private val MAX_RECORDS = 50
    private val prefs = context.applicationContext.getSharedPreferences("history_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _records = MutableStateFlow<List<UsageRecord>>(loadRecords())
    val records: StateFlow<List<UsageRecord>> = _records.asStateFlow()

    private fun loadRecords(): List<UsageRecord> {
        val jsonStr = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<UsageRecord>>(jsonStr)
                .sortedByDescending { it.timestamp }
                .take(MAX_RECORDS)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 记录一次操作
     */
    fun record(presetId: String, presetName: String, action: String) {
        val record = UsageRecord(
            presetId = presetId,
            presetName = presetName,
            action = action
        )
        val updated = (listOf(record) + _records.value)
            .distinctBy { "${it.presetId}_${it.action}" }
            .sortedByDescending { it.timestamp }
            .take(MAX_RECORDS)
        _records.value = updated
        saveRecords(updated)
    }

    /**
     * 获取最近使用的 N 条记录
     */
    fun getRecentRecords(count: Int = 10): List<UsageRecord> {
        return _records.value.take(count)
    }

    /**
     * 获取最近应用的预设 ID 列表
     */
    fun getRecentAppliedPresetIds(): List<String> {
        return _records.value
            .filter { it.action == "applied" }
            .distinctBy { it.presetId }
            .map { it.presetId }
    }

    /**
     * 清除所有记录
     */
    fun clearHistory() {
        _records.value = emptyList()
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    private fun saveRecords(records: List<UsageRecord>) {
        try {
            val jsonStr = json.encodeToString(records)
            prefs.edit().putString(KEY_RECORDS, jsonStr).apply()
        } catch (e: Exception) {
            // 静默失败
        }
    }

    companion object {
        private const val KEY_RECORDS = "usage_records"

        @Volatile
        private var instance: HistoryManager? = null

        fun getInstance(context: Context): HistoryManager {
            return instance ?: synchronized(this) {
                instance ?: HistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
}