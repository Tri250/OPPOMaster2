package com.silas.omaster.data.exports

import com.silas.omaster.data.imports.PresetImportValidator
import com.silas.omaster.model.MasterPreset
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

/**
 * MU-07 预设导出服务（纯 JVM，ED-03 单测覆盖）。
 *
 * 「导出即投稿」口径（自检 F7）：产出的 JSON 与导入管道（PresetImportValidator）
 * 完全互逆——导出内容可直接粘贴回应用或分发给他人导入，无需后端。
 *
 * 两种产物：
 * - [toJson]：干净预设 JSON（单包 / 数组 / 完整订阅包三种形态，null 字段不输出）
 * - [toShareText]：人类可读参数摘要（剪贴板友好，非 JSON，导入管道会拒绝并提示）
 */
object ExportPresetService {

    /** 导出专用序列化：不输出 null 字段，prettyPrint 便于阅读与人工微调 */
    private val json = Json {
        explicitNulls = false
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    enum class Format { SINGLE, ARRAY, PACKAGE }

    sealed class ExportResult {
        data class Success(val text: String, val format: Format) : ExportResult()
        data class Failure(val reason: String) : ExportResult()
    }

    fun toJson(presets: List<MasterPreset>, format: Format): ExportResult {
        if (presets.isEmpty()) return ExportResult.Failure("没有可导出的预设")
        if (presets.size > PresetImportValidator.MAX_PRESET_COUNT) {
            return ExportResult.Failure("预设数量超出导出上限")
        }
        val text = when (format) {
            Format.SINGLE -> json.encodeToString(
                serializer<List<MasterPreset>>(),
                presets
            ).let { stripArray(it) }
            Format.ARRAY -> json.encodeToString(serializer<List<MasterPreset>>(), presets)
            Format.PACKAGE -> json.encodeToString(
                serializer<JsonObject>(),
                buildJsonObject {
                    put("name", "OMaster 导出包")
                    put("author", "OMaster")
                    put("build", 1)
                    put("version", 1)
                    put("presets", json.encodeToJsonElement(serializer<List<MasterPreset>>(), presets))
                }
            )
        }
        return ExportResult.Success(text, format)
    }

    /** 单预设便捷入口 */
    fun toJson(preset: MasterPreset): ExportResult =
        toJson(listOf(preset), Format.SINGLE)

    /** 人类可读分享文本（参数摘要，最多 12 行） */
    fun toShareText(preset: MasterPreset): String = buildString {
        appendLine("【${preset.name}】· OMaster 预设")
        preset.iso?.let { appendLine("ISO $it") }
        preset.shutterSpeed?.let { appendLine("快门 $it") }
        preset.exposureCompensation?.let { appendLine("EV $it") }
        preset.colorTemperature?.let { appendLine("白平衡 ${it}K") }
        preset.filter?.let { appendLine("滤镜 $it") }
        preset.colorTone?.let { appendLine("色调风格 $it") }
        preset.tone?.let { appendLine("色调 $it") }
        preset.saturation?.let { appendLine("饱和度 $it") }
        preset.warmCool?.let { appendLine("冷暖 $it") }
        preset.sharpness?.let { appendLine("锐度 $it") }
        preset.tags?.takeIf { it.isNotEmpty() }?.let {
            appendLine(it.joinToString(" ") { tag -> "#$tag" })
        }
        appendLine("—— 由 OMaster 2.0 导出，粘贴回「创建」页即可导入")
    }.trimEnd()

    /** 把单元素数组 JSON 简化为单对象（SINGLE 形态） */
    private fun stripArray(arrayJson: String): String =
        runCatching {
            val el = json.parseToJsonElement(arrayJson)
            (el as? JsonArray)?.singleOrNull()?.let { (it as JsonObject).toString() } ?: arrayJson
        }.getOrDefault(arrayJson)
}
