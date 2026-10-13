package com.silas.omaster.data.imports

import com.silas.omaster.model.MasterPreset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * FD-01a/01b/01c 粘贴导入校验器（纯 JVM，ED-03 单测覆盖）。
 * R-3 校验管道的文本入口，三条导入路径共用。
 *
 * 支持三种输入形态：
 * 1. 单个预设 JSON 对象：{"name":"...", "iso":"100", ...}
 * 2. 预设数组：[{...}, {...}]
 * 3. 完整订阅内容：{"name":"...", "author":"...", "presets":[...]}
 *
 * 校验规则：
 * - 内容非空、大小 ≤ [MAX_TEXT_LENGTH]；预设数量 ≤ [MAX_PRESET_COUNT]；
 * - 逐预设独立解码：单个损坏预设只被丢弃，不拖垮整批；
 * - 缺失 coverPath 自动注入空串（粘贴的预设不强制带封面，UI 走占位图）；
 * - name 非空为唯一硬校验；未知字段一律忽略（ignoreUnknownKeys）。
 */
object PresetImportValidator {

    enum class ErrorCode { EMPTY, TOO_LARGE, INVALID_JSON, NO_VALID_PRESET, TOO_MANY_PRESETS }

    sealed class ImportResult {
        data class Success(val presets: List<MasterPreset>) : ImportResult()
        data class Failure(val code: ErrorCode) : ImportResult()
    }

    const val MAX_TEXT_LENGTH: Int = 200_000
    const val MAX_PRESET_COUNT: Int = 200

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String?): ImportResult {
        if (text.isNullOrBlank()) return ImportResult.Failure(ErrorCode.EMPTY)
        if (text.length > MAX_TEXT_LENGTH) return ImportResult.Failure(ErrorCode.TOO_LARGE)

        val root = try {
            json.parseToJsonElement(text)
        } catch (e: Exception) {
            return ImportResult.Failure(ErrorCode.INVALID_JSON)
        }

        val elements: List<kotlinx.serialization.json.JsonElement> = when (root) {
            is JsonArray -> root.toList()
            is JsonObject ->
                // 完整订阅内容：取其 presets 数组；否则视为单个预设对象
                (root["presets"] as? JsonArray)?.toList() ?: listOf(root)
            else -> return ImportResult.Failure(ErrorCode.INVALID_JSON)
        }

        val presets = elements.mapNotNull { element ->
            runCatching {
                val obj = element as? JsonObject ?: return@runCatching null
                // coverPath 为模型必填字段：粘贴内容允许缺省，注入空串走占位图
                val normalized = if (obj.containsKey("coverPath")) {
                    obj
                } else {
                    JsonObject(obj + ("coverPath" to JsonPrimitive("")))
                }
                json.decodeFromJsonElement<MasterPreset>(normalized)
            }.getOrNull()
        }

        val valid = presets.filter { !it.name.isBlank() }
        if (valid.isEmpty()) return ImportResult.Failure(ErrorCode.NO_VALID_PRESET)
        if (valid.size > MAX_PRESET_COUNT) return ImportResult.Failure(ErrorCode.TOO_MANY_PRESETS)
        return ImportResult.Success(valid)
    }
}
