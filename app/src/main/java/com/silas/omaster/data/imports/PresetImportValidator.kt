package com.silas.omaster.data.imports

import com.silas.omaster.model.MasterPreset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer

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
        data class Success(
            val presets: List<MasterPreset>,
            /** MU-05 校验 v2：预设索引 → 未识别字段名列表（Schema v2 前向兼容提示，UI 可选展示） */
            val unknownFields: Map<Int, List<String>> = emptyMap()
        ) : ImportResult()
        data class Failure(val code: ErrorCode) : ImportResult()
    }

    const val MAX_TEXT_LENGTH: Int = 200_000
    const val MAX_PRESET_COUNT: Int = 200

    /**
     * Schema 扩展字段白名单（v2 / v3）：作为元数据声明合法存在，不计入未知字段告警。
     * v2: deviceProfile — 机型档案对象
     * v3: deviceSupport — 机型兼容列表（List<String>）
     */
    private val knownExtensionFields = setOf("deviceProfile", "deviceSupport")

    private val json = Json { ignoreUnknownKeys = true }

    /** MasterPreset 已知字段集合（从 serializer descriptor 取，随模型自动更新） */
    private val masterPresetFieldNames: Set<String> by lazy {
        val d = serializer<MasterPreset>().descriptor
        (0 until d.elementsCount).map { d.getElementName(it) }.toSet()
    }

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

        val decoded = elements.mapNotNull { element ->
            runCatching {
                val obj = element as? JsonObject ?: return@runCatching null
                // coverPath 为模型必填字段：粘贴内容允许缺省，注入空串走占位图
                val normalized = if (obj.containsKey("coverPath")) {
                    obj
                } else {
                    JsonObject(obj + ("coverPath" to JsonPrimitive("")))
                }
                val preset = json.decodeFromJsonElement<MasterPreset>(normalized)
                if (preset.name.isBlank()) return@runCatching null
                preset to normalized
            }.recoverCatching {
                // MU-21 兜底：deviceProfile / deviceSupport 是 Schema v2/v3 扩展字段
                // 可能因类型不匹配（如 DeviceProfile 必填字段缺失）导致解码失败
                // 剥离扩展字段后重试，主预设解码不受影响
                val obj = element as? JsonObject ?: return@recoverCatching null
                val stripped = JsonObject(obj.filter { it.key !in knownExtensionFields } +
                    ("coverPath" to JsonPrimitive("")))
                val preset = json.decodeFromJsonElement<MasterPreset>(stripped)
                if (preset.name.isBlank()) return@recoverCatching null
                preset to obj // 用原始 obj 做 unknown 字段分析
            }.getOrNull()?.let { (preset, sourceObj) ->
                // MU-05 校验 v2：收集未知字段（解码照常忽略，仅告警）；
                // deviceProfile 需为对象声明，deviceSupport 需为数组（v3 新增）
                val unknown = sourceObj.keys - masterPresetFieldNames - knownExtensionFields +
                    listOfNotNull("deviceProfile".takeIf {
                        sourceObj.containsKey("deviceProfile") && sourceObj["deviceProfile"] !is JsonObject
                    }) +
                    listOfNotNull("deviceSupport".takeIf {
                        sourceObj.containsKey("deviceSupport") && sourceObj["deviceSupport"] !is JsonArray
                    })
                preset to unknown
            }
        }

        val valid = decoded.map { it.first }
        if (valid.isEmpty()) return ImportResult.Failure(ErrorCode.NO_VALID_PRESET)
        if (valid.size > MAX_PRESET_COUNT) return ImportResult.Failure(ErrorCode.TOO_MANY_PRESETS)
        val warnings = mutableMapOf<Int, List<String>>()
        decoded.forEachIndexed { index, (_, unknown) ->
            if (unknown.isNotEmpty()) warnings[index] = unknown.distinct()
        }
        return ImportResult.Success(valid, warnings)
    }
}
