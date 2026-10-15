package com.silas.omaster.network

import android.content.Context
import android.util.Log
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.model.PresetList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.silas.omaster.util.JsonUtil
import kotlinx.serialization.json.Json
import java.io.File

object PresetRemoteManager {

    // ═══════════════════════════════════════════════════════════
    // FD-11 / v1.6：订阅 v2 — 增量 diff 计算 + 变更预览
    // 客户端自主计算：本地已有预设 vs 新拉到的预设 → added/removed/modified
    // 不需要订阅仓库改 JSON Schema，向后兼容
    // ═══════════════════════════════════════════════════════════

    data class DeltaResult(
        val added: List<com.silas.omaster.model.MasterPreset>,
        val removedIds: List<String>,
        val modified: List<com.silas.omaster.model.MasterPreset>
    ) {
        val hasChanges get() = added.isNotEmpty() || removedIds.isNotEmpty() || modified.isNotEmpty()
        val totalDiff get() = added.size + removedIds.size + modified.size
    }

    data class FetchResult(
        val presetList: com.silas.omaster.model.PresetList,
        val changelog: DeltaResult
    )

    /** MU-18：订阅文件响应体大小上限 10MB */
    private const val MAX_RESPONSE_BYTES = 10 * 1024 * 1024

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // MU-18 网络加固：超时上限，避免订阅更新无限挂起
        install(io.ktor.client.plugins.HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 45_000
        }
    }

    suspend fun fetchPresets(url: String): PresetList? {
        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            val response: HttpResponse = client.get(url)
            // Some servers (GitHub raw) may return Content-Type: text/plain; charset=utf-8
            // which prevents Ktor's content-negotiation from selecting the JSON transformer.
            // Read as text and decode explicitly to avoid NoTransformationFoundException.
            val text: String = response.body()
            val presets = Json.decodeFromString(PresetList.serializer(), text)
            Log.d("PresetRemoteManager", "Fetched ${presets.presets.size} presets")
            presets
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to fetch presets", e)
            null
        }
    }

    suspend fun fetchAndSave(context: Context, url: String, forceUpdate: Boolean = false): Result<FetchResult> {
        Log.d("PresetRemoteManager", "Starting fetch from $url")
        return try {
            val response: HttpResponse = client.get(url)
            // MU-18 网络加固：响应体大小上限（10MB），防恶意超大文件写满存储
            val bytes: ByteArray = response.body()
            if (bytes.size > MAX_RESPONSE_BYTES) {
                return Result.failure(Exception("订阅文件过大（${bytes.size / 1024 / 1024}MB > 10MB 上限）"))
            }
            val text: String = bytes.toString(Charsets.UTF_8)
            
            // 验证 JSON 是否有效
            val presetList = try {
                Json.decodeFromString(PresetList.serializer(), text)
            } catch (e: Exception) {
                Log.e("PresetRemoteManager", "Invalid JSON received", e)
                return Result.failure(Exception("JSON 格式错误"))
            }

            // 验证必填字段
            val missingFields = mutableListOf<String>()
            if (presetList.name.isNullOrBlank()) missingFields.add("name (订阅名称)")
            if (presetList.author.isNullOrBlank()) missingFields.add("author (作者)")            
            if (missingFields.isNotEmpty()) {
                val errorMsg = "缺少必要字段: ${missingFields.joinToString(", ")}"
                return Result.failure(Exception(errorMsg))
            }

            val config = ConfigCenter.getInstance(context)
            val currentSub = config.subscriptionsFlow.value.find { it.url == url }

            // 检查版本号是否相同（判定规则见 SubscriptionUpdatePolicy，ED-03 有单测覆盖）
            if (SubscriptionUpdatePolicy.shouldSkipUpdate(
                    remoteBuild = presetList.build,
                    localBuild = currentSub?.build,
                    forceUpdate = forceUpdate
                )
            ) {
                return Result.failure(Exception("无需更新"))
            }

            // FD-02③ 回退保护：远端 build 低于本地时拒绝写入，保留本地「最后已知良好」数据
            if (SubscriptionUpdatePolicy.shouldRejectRegression(
                    remoteBuild = presetList.build,
                    localBuild = currentSub?.build,
                    forceUpdate = forceUpdate
                )
            ) {
                return Result.failure(
                    Exception("远端版本回退（build ${presetList.build} < ${currentSub!!.build}），已保留本地数据")
                )
            }


            // FD-11：计算本次更新的增量 diff（本地已有预设 vs 新拉到的）
            val changelog = computeDelta(context, url, presetList)

            withContext(Dispatchers.IO) {

                val fileName = config.getSubscriptionFileName(url)
                val file = File(context.filesDir, fileName)
                file.writeText(text)
                Log.d("PresetRemoteManager", "Saved remote presets to ${file.absolutePath}")

                // Update subscription info
                config.updateSubscriptionStatus(
                    url = url,
                    presetCount = presetList.presets.size,
                    lastUpdateTime = System.currentTimeMillis(),
                    name = presetList.name,
                    author = presetList.author,
                    build = presetList.build
                )

                // Invalidate JsonUtil cache so subsequent loads read the new remote file
                try {
                    JsonUtil.invalidateCache()
                } catch (e: Exception) {
                    Log.w("PresetRemoteManager", "Failed to invalidate JsonUtil cache", e)
                }
            }
            Result.success(FetchResult(presetList, changelog))
        } catch (e: Exception) {
            Log.e("PresetRemoteManager", "Failed to save presets", e)
            Result.failure(e)
        }
    }

    /**
     * FD-11 / v1.6：计算订阅增量 diff
     * 读取本地已有 JSON 文件（如果存在），比较新旧 MasterPreset 列表 → added/removed/modified
     * 用 MasterPreset.id 匹配；modified 检查 name/filter/author 核心字段变化
     */
    private fun computeDelta(context: Context, url: String, newList: com.silas.omaster.model.PresetList): DeltaResult {
        return try {
            val config = com.silas.omaster.data.config.ConfigCenter.getInstance(context)
            val fileName = config.getSubscriptionFileName(url)
            val file = java.io.File(context.filesDir, fileName)

            if (!file.exists()) {
                // 首次拉取 → 全部算 added
                return DeltaResult(added = newList.presets, removedIds = emptyList(), modified = emptyList())
            }

            val oldList: com.silas.omaster.model.PresetList = com.silas.omaster.util.JsonUtil.parsePresetList(file.readText())
            val oldById = oldList.presets.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }
            val newById = newList.presets.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }

            val oldIds = oldById.keys
            val newIds = newById.keys

            val added = newIds.subtract(oldIds).mapNotNull { newById[it] }
            val removed = oldIds.subtract(newIds).toList()
            val modified = oldIds.intersect(newIds).mapNotNull { id ->
                val old = oldById[id] ?: return@mapNotNull null
                val new = newById[id] ?: return@mapNotNull null
                if (old.name != new.name || old.filter != new.filter || old.author != new.author) new else null
            }

            DeltaResult(added = added, removedIds = removed, modified = modified)
        } catch (e: Exception) {
            // diff 计算失败静默降级为全部 added（保守策略：不会漏变更）
            com.silas.omaster.util.Logger.e("PresetRemoteManager", "delta 计算失败", e)
            DeltaResult(added = newList.presets, removedIds = emptyList(), modified = emptyList())
        }
    }

}
