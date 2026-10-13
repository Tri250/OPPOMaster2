package com.silas.omaster.data.imports

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetList
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * FD-01b 粘贴导入校验器（纯 JVM，ED-03 单测覆盖）。
 *
 * 支持三种输入形态（R-3 校验管道的文本入口）：
 * 1. 单个预设 JSON 对象：{"name":"...", "iso":"100", ...}
 * 2. 预设数组：[{...}, {...}]
 * 3. 完整订阅内容：{"name":"...", "author":"...", "presets":[...]}
 *
 * 校验规则：
 * - 内容非空、大小 ≤ [MAX_TEXT_LENGTH]；
 * - 预设数量 ≤ [MAX_PRESET_COUNT]；
 * - 至少包含一个 name 非空的预设；
 * - 未知字段一律忽略（ignoreUnknownKeys），损坏内容整体拒绝，不静默截断。
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

        val presets = when (root) {
            is JsonArray -> decodePresets { json.decodeFromJsonElement<List<MasterPreset>>(root) }
            is JsonObject ->
                if (root.containsKey("presets")) {
                    // 完整订阅内容：以 PresetList 结构解析，取其 presets
                    decodePresets { json.decodeFromJsonElement<PresetList>(root).presets }
                } else {
                    // 单个预设对象
                    decodePresets { listOf(json.decodeFromJsonElement<MasterPreset>(root)) }
                }
            else -> return ImportResult.Failure(ErrorCode.INVALID_JSON)
        } ?: return ImportResult.Failure(ErrorCode.INVALID_JSON)

        val valid = presets.filter { !it.name.isBlank() }
        if (valid.isEmpty()) return ImportResult.Failure(ErrorCode.NO_VALID_PRESET)
        if (valid.size > MAX_PRESET_COUNT) return ImportResult.Failure(ErrorCode.TOO_MANY_PRESETS)
        return ImportResult.Success(valid)
    }

    private inline fun decodePresets(decode: () -> List<MasterPreset>): List<MasterPreset>? =
        try {
            decode()
        } catch (e: Exception) {
            null
        }
}
