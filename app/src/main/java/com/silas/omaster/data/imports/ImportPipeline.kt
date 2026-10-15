package com.silas.omaster.data.imports

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.data.imports.toPreset

/**
 * FD-09 统一导入管道 v2 — 所有外部预设内容的唯一入口。
 *
 * 架构红线：外部内容 → ImportPipeline → CustomPresetManager.importFromPipeline()，
 * 严禁 UI / Service / Network 层绕过此管道直接写库。
 *
 * 管道入口（由 enum 标识 source）：
 * - TEXT_PASTE          用户在 ClipboardImportDialog 粘贴 JSON 文本（FD-01b）
 * - FILE_JSON           SAF 选择 .json 文件（FD-01a）
 * - SYSTEM_SHARE_TEXT   其他 App 分享的 text/plain 到 OMaster（FD-01c）
 * - OCR_TEXT            Phase 2：ColorOS AI 闪记 OCR 出来的相机参数文本（FD-06）
 * - CAPTURE_TEXT        Phase 2：用户手动从相机参数页复制后粘贴的文本（MU-06 对照录入）
 *
 * 管道内部：
 *   文本 JSON → PresetImportValidator.parse()           → MasterPreset（纯 JSON 预设）
 *   相机参数文本 → ScreenCaptureParser.parse()           → MasterPreset（对照录入）
 *                  ↓
 *                PresetImportValidator 自动修复 coverPath（空值注入）+ 结构校验
 *
 * 本类只做"解析 + 结构化输出"，不写库；写库统一在 CustomPresetManager。
 */
object ImportPipeline {

    /** 导入来源 — 决定走哪条解析策略、UI 提示何种错误 */
    enum class Source {
        TEXT_PASTE,
        FILE_JSON,
        SYSTEM_SHARE_TEXT,
        OCR_TEXT,      // Phase 2（FD-06）
        CAPTURE_TEXT   // Phase 2（MU-06）
    }

    /** 解析方法 — 用于日志 / Result 区分纯 JSON 与对照录入 */
    enum class ParseMethod { JSON, CAPTURE }

    /** 管道错误码（与 PresetImportValidator.ErrorCode 并列，补充管道专属错误） */
    enum class ErrorCode {
        EMPTY, TOO_LARGE, INVALID_JSON, NO_VALID_PRESET, TOO_MANY_PRESETS,
        INVALID_CAPTURE_TEXT  // ScreenCaptureParser 无法识别到任何参数（对照录入）
    }

    sealed class Result {
        data class Success(
            val presets: List<MasterPreset>,
            /** Schema v3 前向兼容告警：预设索引 → 未识别字段列表 */
            val unknownFields: Map<Int, List<String>> = emptyMap(),
            val source: Source,
            val parseMethod: ParseMethod
        ) : Result()

        data class Failure(
            val code: ErrorCode,
            val source: Source,
            val messageHint: String? = null  // 可选的人类可读提示，UI 层展示
        ) : Result()
    }

    // 解析顺序：先尝试 JSON → 失败再尝试相机参数文本（OCR / CAPTURE）
    fun parse(text: String?, source: Source): Result {
        if (text.isNullOrBlank()) {
            return Result.Failure(ErrorCode.EMPTY, source, "文本为空")
        }

        // 文本过长优先拒绝（与 PresetImportValidator 同阈值）
        if (text.length > PresetImportValidator.MAX_TEXT_LENGTH) {
            return Result.Failure(ErrorCode.TOO_LARGE, source, "文本过长，最多 ${PresetImportValidator.MAX_TEXT_LENGTH} 字符")
        }

        // 1. 优先尝试 JSON 解析（所有来源都可能带 JSON）
        runCatching {
            val jsonResult = PresetImportValidator.parse(text)
            if (jsonResult is PresetImportValidator.ImportResult.Success) {
                return Result.Success(
                    presets = jsonResult.presets,
                    unknownFields = jsonResult.unknownFields,
                    source = source,
                    parseMethod = ParseMethod.JSON
                )
            }
        }
        // JSON 解析失败不立即返回，继续尝试对照录入（OCR / CAPTURE / PASTE 都可能是参数文本）
        val isJsonError = runCatching {
            val jsonResult = PresetImportValidator.parse(text)
            jsonResult is PresetImportValidator.ImportResult.Failure
        }.getOrDefault(false)

        // 2. 对照录入（OCR / CAPTURE 专属入口直接走这里；其他入口 JSON 失败时降级尝试）
        val shouldTryCapture = source == Source.OCR_TEXT || source == Source.CAPTURE_TEXT ||
                (source == Source.TEXT_PASTE && isJsonError)

        if (shouldTryCapture) {
            val capture = ScreenCaptureParser.parse(text)
            if (capture != null && capture.fields.isNotEmpty()) {
                // ScreenCaptureParser 产出的 ParsedCapture → toPreset → MasterPreset
                val preset = capture.toPreset()
                return Result.Success(
                    presets = listOf(preset),
                    unknownFields = emptyMap(),
                    source = source,
                    parseMethod = ParseMethod.CAPTURE
                )
            }
            // 如果 source 明确要求走 CAPTURE 路径但没识别出任何参数，返回专属错误
            if (source == Source.OCR_TEXT || source == Source.CAPTURE_TEXT) {
                return Result.Failure(ErrorCode.INVALID_CAPTURE_TEXT, source, "未能从文本中识别到相机参数")
            }
        }

        // 3. 兜底：返回原始 JSON 解析错误
        val jsonResult = PresetImportValidator.parse(text)
        return when (jsonResult) {
            is PresetImportValidator.ImportResult.Success ->
                Result.Success(jsonResult.presets, jsonResult.unknownFields, source, ParseMethod.JSON)
            is PresetImportValidator.ImportResult.Failure ->
                Result.Failure(
                    when (jsonResult.code) {
                        PresetImportValidator.ErrorCode.EMPTY -> ErrorCode.EMPTY
                        PresetImportValidator.ErrorCode.TOO_LARGE -> ErrorCode.TOO_LARGE
                        PresetImportValidator.ErrorCode.INVALID_JSON -> ErrorCode.INVALID_JSON
                        PresetImportValidator.ErrorCode.NO_VALID_PRESET -> ErrorCode.NO_VALID_PRESET
                        PresetImportValidator.ErrorCode.TOO_MANY_PRESETS -> ErrorCode.TOO_MANY_PRESETS
                    },
                    source
                )
        }
    }

    /** 便捷方法：SAF 文件内容（string）直接传 */
    fun parseFromFile(text: String?): Result = parse(text, Source.FILE_JSON)

    /** 便捷方法：系统分享 text/plain */
    fun parseFromShare(text: String?): Result = parse(text, Source.SYSTEM_SHARE_TEXT)

    /** 便捷方法：UI 手动粘贴 */
    fun parseFromPaste(text: String?): Result = parse(text, Source.TEXT_PASTE)
}
