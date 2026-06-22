package com.silas.omaster.ui.features

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.ai.mapping.FilmAdjustments
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.ui.components.AnalysisStatus
import com.silas.omaster.ui.components.AnalysisStep
import com.silas.omaster.ui.components.ApertureState
import com.silas.omaster.ui.components.defaultAnalysisSteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 哈苏之眼 ViewModel
 * 管理哈苏之眼（HasselbladScreen）的完整工作流状态：
 * 工作流阶段、参数调节、AI 推荐、色彩模式、分析结果、预览生成、保存与分享。
 */
class HasselbladEyeViewModel : ViewModel() {

    /** 导出格式 - P2 HEIF支持 */
    enum class ExportFormat {
        JPEG, PNG, WEBP, HEIF
    }

    // ================== StateFlow 状态 ==================

    private val _stage = MutableStateFlow(HasselbladEyeStage.SETUP)
    val stage: StateFlow<HasselbladEyeStage> = _stage.asStateFlow()

    private val _params = MutableStateFlow(HasselbladParams())
    val params: StateFlow<HasselbladParams> = _params.asStateFlow()

    private val _recommendedParams = MutableStateFlow<HasselbladParams?>(null)
    val recommendedParams: StateFlow<HasselbladParams?> = _recommendedParams.asStateFlow()

    private val _isParamsLocked = MutableStateFlow(false)
    val isParamsLocked: StateFlow<Boolean> = _isParamsLocked.asStateFlow()

    // 场景模式与色彩模式独立选择（两者互不互斥）
    private val _selectedSceneModeId = MutableStateFlow("scene-natural")
    val selectedSceneModeId: StateFlow<String> = _selectedSceneModeId.asStateFlow()

    private val _selectedSceneParams = MutableStateFlow(emptyMap<String, Int>())
    val selectedSceneParams: StateFlow<Map<String, Int>> = _selectedSceneParams.asStateFlow()

    private val _selectedColorModeId = MutableStateFlow("natural")
    val selectedColorModeId: StateFlow<String> = _selectedColorModeId.asStateFlow()

    private val _selectedColorParams = MutableStateFlow(emptyMap<String, Int>())
    val selectedColorParams: StateFlow<Map<String, Int>> = _selectedColorParams.asStateFlow()

    private val _analysisResult = MutableStateFlow<AnalysisResult?>(null)
    val analysisResult: StateFlow<AnalysisResult?> = _analysisResult.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()

    private val _analysisMessage = MutableStateFlow("正在读取光影信息...")
    val analysisMessage: StateFlow<String> = _analysisMessage.asStateFlow()

    private val _analysisSteps = MutableStateFlow(defaultAnalysisSteps())
    val analysisSteps: StateFlow<List<AnalysisStep>> = _analysisSteps.asStateFlow()

    private val _apertureState = MutableStateFlow(ApertureState.CLOSED)
    val apertureState: StateFlow<ApertureState> = _apertureState.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _thumbnailPreview = MutableStateFlow<Bitmap?>(null)
    val thumbnailPreview: StateFlow<Bitmap?> = _thumbnailPreview.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.JPEG)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _lastSavedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastSavedUri: StateFlow<android.net.Uri?> = _lastSavedUri.asStateFlow()

    // ================== 协程任务 ==================

    private var analysisJob: Job? = null
    private var previewJob: Job? = null

    init {
        // 参数变化 250ms 后自动触发低分辨率实时预览
        viewModelScope.launch {
            _params.debounce(250).collect {
                triggerRealtimePreview()
            }
        }
    }

    /**
     * 在合适的阶段触发低分辨率实时预览。
     */
    private fun triggerRealtimePreview() {
        val source = _originalBitmap.value ?: return
        if (_stage.value != HasselbladEyeStage.RESULTS && _stage.value != HasselbladEyeStage.PREVIEW) return
        updatePreviewAsync(source, emptyMap())
    }

    // ================== 核心方法 ==================

    /**
     * 启动哈苏之眼 AI 分析流程。
     *
     * @param bitmap 待分析的原始图片
     * @param inferenceEngine 分析引擎
     * @param colorModes 可用的色彩模式列表，用于推荐显示名称
     */
    fun startAnalysis(
        bitmap: Bitmap,
        inferenceEngine: MasterInferenceEngine,
        colorModes: List<ColorMode>
    ) {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _originalBitmap.value = bitmap
            _analysisResult.value = null
            _analysisError.value = null
            _stage.value = HasselbladEyeStage.ANALYZING

            val steps = defaultAnalysisSteps().toMutableList()

            try {
                updateAnalysisProgress(5f, "正在读取光影信息...", steps)
                delay(200)

                updateAnalysisProgress(15f, "准备分析引擎...", steps)
                delay(300)

                // Step 1: 色彩分析
                steps[0] = steps[0].copy(status = AnalysisStatus.PROCESSING)
                updateAnalysisProgress(20f, "色彩分析中...", steps)

                val analysisDetail = withContext(Dispatchers.Default) {
                    inferenceEngine.analyzeImageWithDetails(bitmap, imagePath = null)
                }
                val profile = analysisDetail.profile

                steps[0] = steps[0].copy(status = AnalysisStatus.COMPLETED)
                val meanLuma = profile.histogramData?.meanLuminance?.toInt() ?: 0
                updateAnalysisProgress(
                    40f,
                    "色彩分析完成 · 平均亮度 $meanLuma",
                    steps
                )

                // Step 2: 光影结构分析
                steps[1] = steps[1].copy(status = AnalysisStatus.PROCESSING)
                updateAnalysisProgress(55f, "光影结构分析中...", steps)

                val shadowClip = profile.histogramData?.shadowClipping == true
                val highlightClip = profile.histogramData?.highlightClipping == true
                delay(100)

                steps[1] = steps[1].copy(status = AnalysisStatus.COMPLETED)
                val lightMsg = if (shadowClip || highlightClip) {
                    "光影分析完成 · 检测到${if (shadowClip) "阴影" else ""}" +
                        "${if (shadowClip && highlightClip) "/" else ""}" +
                        "${if (highlightClip) "高光" else ""}裁剪"
                } else {
                    "光影分析完成 · 动态范围正常"
                }
                updateAnalysisProgress(70f, lightMsg, steps)

                // Step 3: 场景匹配
                steps[2] = steps[2].copy(status = AnalysisStatus.PROCESSING)
                updateAnalysisProgress(80f, "场景匹配中...", steps)

                val faceCount = profile.faceData?.faces?.size ?: 0
                delay(100)

                steps[2] = steps[2].copy(status = AnalysisStatus.COMPLETED)
                updateAnalysisProgress(
                    90f,
                    "场景匹配完成 · ${profile.name} (${(profile.confidence * 100).toInt()}%)",
                    steps
                )

                // Step 4: 胶片推荐
                steps[3] = steps[3].copy(status = AnalysisStatus.PROCESSING)
                updateAnalysisProgress(93f, "胶片推荐中...", steps)

                val films = if (profile.recommendedFilm.isNotEmpty()) {
                    profile.recommendedFilm
                } else {
                    inferenceEngine.getRecommendedFilms(profile.id)
                }

                steps[3] = steps[3].copy(status = AnalysisStatus.COMPLETED)
                val topFilm = films.firstOrNull()
                updateAnalysisProgress(
                    96f,
                    "胶片推荐完成 · ${topFilm?.name ?: "CC 经典负片"}",
                    steps
                )

                // Step 5: 参数优化
                steps[4] = steps[4].copy(status = AnalysisStatus.PROCESSING)
                updateAnalysisProgress(98f, "哈苏参数优化中...", steps)

                val masterTips = if (profile.masterTips.isNotEmpty()) {
                    profile.masterTips
                } else {
                    inferenceEngine.getMasterTips(profile.id)
                }

                val suggestedColorMode = suggestColorMode(profile, colorModes)
                val paramAdjustments = mapParamAdjustments(profile.hasselbladParams)

                steps[4] = steps[4].copy(status = AnalysisStatus.COMPLETED)
                updateAnalysisProgress(
                    100f,
                    "哈苏之眼已睁开 · 人脸数 $faceCount",
                    steps,
                    aperture = ApertureState.OPEN
                )

                delay(300)

                _recommendedParams.value = profile.hasselbladParams

                // P1-10：将细粒度识别结果映射到粗粒度 UI 模式，并自动选中推荐色彩模式
                val sceneModeId = mapSceneProfileToSceneModeId(profile)
                val colorModeId = resolveColorModeId(suggestedColorMode)
                applyAnalyzedModes(sceneModeId, colorModeId)

                if (!_isParamsLocked.value) {
                    _params.value = profile.hasselbladParams
                }
                _analysisResult.value = AnalysisResult(
                    sceneProfile = profile,
                    alternativeScenes = analysisDetail.alternatives,
                    recommendedFilms = films,
                    masterTips = masterTips,
                    suggestedColorMode = suggestedColorMode,
                    paramAdjustments = paramAdjustments
                )
                _stage.value = HasselbladEyeStage.RESULTS
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Analysis cancelled")
                _stage.value = HasselbladEyeStage.SETUP
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
                _analysisError.value = "分析失败: ${e.message ?: "未知错误"}"
                _stage.value = HasselbladEyeStage.RESULTS
            }
        }
    }

    /**
     * 取消当前正在进行的分析任务。
     */
    fun cancelAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
    }

    /**
     * 更新单个可调参数。
     *
     * 支持的 key 包括：tone、saturation、contrast、colorTemp、warmth、
     * sharpness、vignette、cyanMagenta、softLight、highlights、shadows、clarity。
     */
    fun updateParam(key: String, value: Int) {
        val current = _params.value
        _params.value = when (key) {
            "tone" -> current.copy(tone = value)
            "saturation" -> current.copy(saturation = value)
            "contrast" -> current.copy(contrast = value)
            "colorTemp", "warmth" -> current.copy(colorTemp = value)
            "sharpness" -> current.copy(sharpness = value)
            "vignette" -> current.copy(vignette = value)
            "cyanMagenta" -> current.copy(cyanMagenta = value)
            "softLight" -> current.copy(
                softLight = SoftLightMode.entries.getOrNull(value) ?: SoftLightMode.NONE
            )
            "highlights" -> current.copy(highlights = value)
            "shadows" -> current.copy(shadows = value)
            "clarity" -> current.copy(clarity = value)
            else -> current
        }
    }

    /**
     * 重置为 AI 推荐参数。参数被锁定时不会执行覆盖。
     */
    fun resetToRecommended() {
        if (_isParamsLocked.value) return
        _recommendedParams.value?.let { _params.value = it }
    }

    /**
     * 切换选中的场景模式。参数被锁定时仅更新模式 ID，不会应用模式参数。
     */
    fun updateSelectedSceneMode(modeId: String, modeParams: Map<String, Int>) {
        _selectedSceneModeId.value = modeId
        _selectedSceneParams.value = modeParams
        if (_isParamsLocked.value) return
        rebuildParamsFromModes()
    }

    /**
     * 切换选中的色彩模式。参数被锁定时仅更新模式 ID，不会应用模式参数。
     */
    fun updateSelectedColorMode(modeId: String, modeParams: Map<String, Int>) {
        _selectedColorModeId.value = modeId
        _selectedColorParams.value = modeParams
        if (_isParamsLocked.value) return
        rebuildParamsFromModes()
    }

    /**
     * 应用推荐胶片的参数到当前参数。参数被锁定时不会执行覆盖。
     */
    fun applyFilmPreset(filmId: String) {
        if (_isParamsLocked.value) return
        applyParamsMap(FilmAdjustments.get(filmId))
    }

    /**
     * 根据当前场景模式和色彩模式重新构建基础参数。
     */
    private fun rebuildParamsFromModes() {
        var base = HasselbladParams()
        base = applyParamsMapToBase(base, _selectedSceneParams.value)
        base = applyParamsMapToBase(base, _selectedColorParams.value)
        _params.value = base
    }

    /**
     * 应用分析推荐的场景与色彩模式。参数被锁定时仅更新模式 ID，不覆盖当前参数。
     */
    fun applyAnalysisRecommendations(
        sceneModeId: String,
        sceneParams: Map<String, Int>,
        colorModeId: String,
        colorParams: Map<String, Int>
    ) {
        _selectedSceneModeId.value = sceneModeId
        _selectedSceneParams.value = sceneParams
        _selectedColorModeId.value = colorModeId
        _selectedColorParams.value = colorParams
        if (_isParamsLocked.value) return
        rebuildParamsFromModes()
    }

    /**
     * 设置参数锁定状态。
     */
    fun setParamsLocked(locked: Boolean) {
        _isParamsLocked.value = locked
    }

    /**
     * P1-10：应用分析结果对应的 UI 模式（只切换模式 ID，不覆盖当前已调参数）。
     * 参数被锁定时仍然会更新模式选中状态，保证界面与分析结果一致。
     */
    fun applyAnalyzedModes(sceneModeId: String, colorModeId: String) {
        val sceneMode = allSceneModes.find { it.id == sceneModeId }
        val colorMode = allColorModes.find { it.id == colorModeId }
        _selectedSceneModeId.value = sceneModeId
        _selectedSceneParams.value = sceneMode?.params ?: emptyMap()
        _selectedColorModeId.value = colorModeId
        _selectedColorParams.value = colorMode?.params ?: emptyMap()
    }

    /**
     * P1-10：用户从备选/子模式中选择一个细粒度场景时，
     * 同时更新粗粒度模式选中态、色彩模式，并应用该细粒度场景的哈苏参数。
     */
    fun applyFineGrainedScene(sceneProfile: SceneProfile) {
        val sceneModeId = mapSceneProfileToSceneModeId(sceneProfile)
        val colorModeId = suggestColorModeIdByCategory(sceneProfile.category)
        applyAnalyzedModes(sceneModeId, colorModeId)
        if (!_isParamsLocked.value) {
            _params.value = sceneProfile.hasselbladParams
        }
    }

    /**
     * 设置工作流阶段（用于 DONE 页返回 PREVIEW 等场景）。
     */
    fun setStage(newStage: HasselbladEyeStage) {
        _stage.value = newStage
    }

    /**
     * 在 Default 调度器生成低分辨率缩略图预览。
     */
    fun updatePreviewAsync(source: Bitmap, modeParams: Map<String, Int>) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val thumbnail = withContext(Dispatchers.Default) {
                val scaled = createThumbnail(source, maxDimension = 512)
                val targetParams = mergeParams(modeParams)
                applyHasselbladColorScience(scaled, targetParams)
            }
            _thumbnailPreview.value = thumbnail
        }
    }

    /**
     * 生成预览效果图，并进入 PREVIEW 阶段。
     * 为防止大图 OOM，source 会先采样到 [PREVIEW_MAX_DIMENSION]。
     */
    fun generateFullPreview(source: Bitmap, modeParams: Map<String, Int>) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val targetParams = mergeParams(modeParams)
                val scaled = createThumbnail(source, maxDimension = PREVIEW_MAX_DIMENSION)
                applyHasselbladColorScience(scaled, targetParams)
            }
            _previewBitmap.value = result
            _stage.value = HasselbladEyeStage.PREVIEW
        }
    }

    /**
     * 保存图片到相册。保存结果通过 [isSaving] 与 [stage] 状态暴露。
     * 为防止大图 OOM，bitmap 会先采样到 [EXPORT_MAX_DIMENSION]。
     */
    fun saveImage(
        context: Context,
        bitmap: Bitmap,
        format: ExportFormat
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val savedUri = try {
                withContext(Dispatchers.IO) {
                    try {
                        val (compressFormat, extension) = when (format) {
                            ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG to "jpg"
                            ExportFormat.PNG -> Bitmap.CompressFormat.PNG to "png"
                            ExportFormat.WEBP -> Bitmap.CompressFormat.WEBP to "webp"
                            ExportFormat.HEIF -> Bitmap.CompressFormat.JPEG to "heic"
                        }
                        val mimeType = when (format) {
                            ExportFormat.JPEG -> "image/jpeg"
                            ExportFormat.PNG -> "image/png"
                            ExportFormat.WEBP -> "image/webp"
                            ExportFormat.HEIF -> "image/heif"
                        }
                        val scaled = createThumbnail(bitmap, maxDimension = EXPORT_MAX_DIMENSION)
                        val filename = "Hasselblad_${System.currentTimeMillis()}.$extension"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                            put(
                                MediaStore.Images.Media.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES + "/OMaster/Hasselblad"
                            )
                        }
                        val uri = context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        uri?.also {
                            context.contentResolver.openOutputStream(it)?.use { out ->
                                scaled.compress(compressFormat, 95, out)
                            }
                            // 通知系统相册立即索引
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(it.toString()),
                                arrayOf(mimeType),
                                null
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Save image failed", e)
                        null
                    }
                }
            } finally {
                _isSaving.value = false
            }
            if (savedUri != null) {
                _lastSavedUri.value = savedUri
                _stage.value = HasselbladEyeStage.DONE
            }
        }
    }

    /**
     * 分享图片到其他应用。
     */
    fun shareImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                shareBitmapToUri(context, bitmap)
            }
            uri ?: return@launch

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "分享哈苏色彩照片")
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
        }
    }

    /**
     * 设置导出格式。
     */
    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    /**
     * 重置所有状态，并释放持有的 Bitmap 防止内存泄漏。
     */
    fun clear() {
        analysisJob?.cancel()
        previewJob?.cancel()
        analysisJob = null
        previewJob = null

        _stage.value = HasselbladEyeStage.SETUP
        _params.value = HasselbladParams()
        _recommendedParams.value = null
        _isParamsLocked.value = false
        _selectedSceneModeId.value = "scene-natural"
        _selectedSceneParams.value = emptyMap()
        _selectedColorModeId.value = "natural"
        _selectedColorParams.value = emptyMap()
        _analysisResult.value = null
        _analysisError.value = null
        _analysisProgress.value = 0f
        _analysisMessage.value = "正在读取光影信息..."
        _analysisSteps.value = defaultAnalysisSteps()
        _apertureState.value = ApertureState.CLOSED

        // 释放 Bitmap 防止 OOM
        _originalBitmap.value?.recycle()
        _originalBitmap.value = null
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        _thumbnailPreview.value?.recycle()
        _thumbnailPreview.value = null

        _isSaving.value = false
        _exportFormat.value = ExportFormat.JPEG
        _lastSavedUri.value = null
    }

    // ================== 生命周期 ==================

    override fun onCleared() {
        super.onCleared()
        analysisJob?.cancel()
        previewJob?.cancel()
        // ViewModel 销毁时释放 Bitmap
        _originalBitmap.value?.recycle()
        _originalBitmap.value = null
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
        _thumbnailPreview.value?.recycle()
        _thumbnailPreview.value = null
    }

    // ================== 私有辅助方法 ==================

    private fun updateAnalysisProgress(
        progress: Float,
        message: String,
        steps: List<AnalysisStep>,
        aperture: ApertureState? = null
    ) {
        _analysisProgress.value = progress
        _analysisMessage.value = message
        _analysisSteps.value = steps
        _apertureState.value = aperture ?: when {
            progress < 15f -> ApertureState.ROTATING
            progress < 100f -> ApertureState.OPENING
            else -> ApertureState.OPEN
        }
    }

    private fun applyParamsMap(modeParams: Map<String, Int>) {
        modeParams.forEach { (key, value) ->
            updateParam(key, value)
        }
    }

    private fun mergeParams(modeParams: Map<String, Int>): HasselbladParams {
        return applyParamsMapToBase(_params.value, modeParams)
    }

    private fun applyParamsMapToBase(
        base: HasselbladParams,
        modeParams: Map<String, Int>
    ): HasselbladParams {
        var result = base
        modeParams.forEach { (key, value) ->
            result = when (key) {
                "tone" -> result.copy(tone = value)
                "saturation" -> result.copy(saturation = value)
                "contrast" -> result.copy(contrast = value)
                "colorTemp", "warmth" -> result.copy(colorTemp = value)
                "sharpness" -> result.copy(sharpness = value)
                "vignette" -> result.copy(vignette = value)
                "cyanMagenta" -> result.copy(cyanMagenta = value)
                "softLight" -> result.copy(
                    softLight = SoftLightMode.entries.getOrNull(value) ?: SoftLightMode.NONE
                )
                "highlights" -> result.copy(highlights = value)
                "shadows" -> result.copy(shadows = value)
                "clarity" -> result.copy(clarity = value)
                else -> result
            }
        }
        return result
    }

    private fun createThumbnail(source: Bitmap, maxDimension: Int): Bitmap {
        if (source.width <= maxDimension && source.height <= maxDimension) return source
        val scale = maxDimension.toFloat() / maxOf(source.width, source.height)
        val newWidth = (source.width * scale).toInt()
        val newHeight = (source.height * scale).toInt()
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    /**
     * 应用哈苏色彩科学到图片。
     */
    private fun applyHasselbladColorScience(
        source: Bitmap,
        hasselbladParams: HasselbladParams
    ): Bitmap {
        return applyHasselbladColorEngine(source, hasselbladParams)
    }

    private fun shareBitmapToUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "share").apply {
                if (!exists()) mkdirs()
            }
            val file = File(cacheDir, "hasselblad_share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Share image failed", e)
            null
        }
    }

    private fun suggestColorMode(profile: SceneProfile, colorModes: List<ColorMode>): String {
        val modeMapping = mapOf(
            "PORTRAIT" to "人像肤色优化",
            "LANDSCAPE" to "风景色彩增强",
            "NIGHT" to "哈苏经典胶片",
            "FOOD" to "鲜艳色彩",
            "URBAN" to "哈苏经典胶片",
            "STILL_LIFE" to "哈苏自然色彩",
            "MACRO" to "鲜艳色彩",
            "EVENT" to "哈苏自然色彩",
            "UNKNOWN" to "哈苏自然色彩"
        )
        val displayName = modeMapping[profile.category.name] ?: "哈苏自然色彩"
        return colorModes.find { it.name == displayName }?.name ?: displayName
    }

    private fun mapParamAdjustments(params: HasselbladParams): Map<String, Int> {
        return mapOf(
            "saturation" to (params.saturation * 3.3f).toInt().coerceIn(-100, 100),
            "contrast" to (params.contrast * 3.3f).toInt().coerceIn(-100, 100),
            "warmth" to (params.colorTemp * 3.3f).toInt().coerceIn(-100, 100),
            "vibrance" to (params.saturation * 2f).toInt().coerceIn(-100, 100),
            "clarity" to (params.clarity * 3.3f).toInt().coerceIn(-100, 100)
        )
    }

    companion object {
        private const val TAG = "HasselbladEyeViewModel"

        /** 预览图最大边长，防止大图 OOM */
        const val PREVIEW_MAX_DIMENSION = 1080

        /** 导出图最大边长，兼顾画质与内存 */
        const val EXPORT_MAX_DIMENSION = 2560
    }
}
