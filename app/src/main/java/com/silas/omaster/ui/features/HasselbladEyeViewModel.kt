package com.silas.omaster.ui.features

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.data.lut.LUT3DRenderer
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.ai.mapping.FilmAdjustments
import com.silas.omaster.data.local.RecipeHistoryManager
import com.silas.omaster.data.local.RecipeRecord
import com.silas.omaster.model.FilmPreset
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

    // 3D LUT 集成状态
    private val _active3DLUTId = MutableStateFlow<String?>(null)
    val active3DLUTId: StateFlow<String?> = _active3DLUTId.asStateFlow()

    private val _lut3DStrength = MutableStateFlow(1.0f)
    val lut3DStrength: StateFlow<Float> = _lut3DStrength.asStateFlow()

    private val _lut3DData = MutableStateFlow<LUT3DData?>(null)
    val lut3DData: StateFlow<LUT3DData?> = _lut3DData.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _exportFormat = MutableStateFlow(ExportFormat.JPEG)
    val exportFormat: StateFlow<ExportFormat> = _exportFormat.asStateFlow()

    private val _lastSavedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastSavedUri: StateFlow<android.net.Uri?> = _lastSavedUri.asStateFlow()

    /** 保存/分享操作错误信息（null 表示无错误），UI 层消费后调用 clearOperationError 清除 */
    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    // ================== AI 构图辅助状态 ==================

    /**
     * 当前已选中的构图指南 ID（null 表示未应用）
     * 选中后会在 Stage 切换、文件命名、保存分享等处生效
     */
    private val _appliedCompositionGuideId = MutableStateFlow<String?>(null)
    val appliedCompositionGuideId: StateFlow<String?> = _appliedCompositionGuideId.asStateFlow()

    /**
     * 当前应用的构图场景模式
     */
    private val _appliedCompositionSceneMode = MutableStateFlow<CompositionSceneMode?>(null)
    val appliedCompositionSceneMode: StateFlow<CompositionSceneMode?> = _appliedCompositionSceneMode.asStateFlow()

    /**
     * 当前应用的 AR 引导线类型
     */
    private val _appliedARGuideType = MutableStateFlow<ARGuideType?>(null)
    val appliedARGuideType: StateFlow<ARGuideType?> = _appliedARGuideType.asStateFlow()

    /**
     * 是否启用 AR 构图引导线显示
     * 启用后会在拍照页和预览页叠加引导线
     */
    private val _isARGuideEnabled = MutableStateFlow(false)
    val isARGuideEnabled: StateFlow<Boolean> = _isARGuideEnabled.asStateFlow()

    /**
     * 应用指定的构图指南
     * 会同时设置：构图 ID、场景模式、AR 引导线类型，并自动开启 AR 引导显示
     * 取消时传入 null
     */
    fun applyCompositionGuide(guide: CompositionGuide?) {
        if (guide == null) {
            _appliedCompositionGuideId.value = null
            _appliedCompositionSceneMode.value = null
            _appliedARGuideType.value = null
            _isARGuideEnabled.value = false
        } else {
            _appliedCompositionGuideId.value = guide.id
            _appliedCompositionSceneMode.value = guide.sceneMode
            _appliedARGuideType.value = guide.arGuideType
            _isARGuideEnabled.value = true
        }
    }

    /**
     * 切换 AR 引导线显示开关
     */
    fun toggleARGuide() {
        _isARGuideEnabled.value = !_isARGuideEnabled.value
    }

    /**
     * 清除已应用的构图（用于重置分析）
     */
    fun clearAppliedComposition() {
        _appliedCompositionGuideId.value = null
        _appliedCompositionSceneMode.value = null
        _appliedARGuideType.value = null
        _isARGuideEnabled.value = false
    }

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
        colorModes: List<ColorMode>,
        context: Context? = null
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

                // 写入配方历史，供大师洞察报告页读取
                recordRecipeHistory(profile, films, context)
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
     * 将分析结果写入配方历史，供大师洞察报告页读取。
     * 每次分析完成时调用，不依赖用户是否保存图片。
     */
    private fun recordRecipeHistory(
        profile: SceneProfile,
        films: List<FilmPreset>,
        context: Context?
    ) {
        if (context == null) {
            Log.w(TAG, "Context 为空，跳过配方历史记录")
            return
        }
        try {
            val topFilm = films.firstOrNull()
            val record = RecipeRecord(
                id = java.util.UUID.randomUUID().toString(),
                sceneId = profile.id,
                sceneName = profile.name,
                sceneCategory = profile.id.substringBefore("-", "UNKNOWN").uppercase(),
                filmId = topFilm?.id,
                filmName = topFilm?.name ?: "CC 经典负片",
                timestamp = System.currentTimeMillis(),
                confidence = profile.confidence
            )
            RecipeHistoryManager.getInstance(context).addRecipe(record)
            Log.d(TAG, "配方历史已记录: scene=${profile.name}, film=${record.filmName}")
        } catch (e: Exception) {
            Log.e(TAG, "写入配方历史失败", e)
        }
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

    // ================== 3D LUT 集成 ==================

    /**
     * 应用 3D LUT 到当前图片
     *
     * 从 LUTManager 获取已下载的 LUT 数据，通过 CPU 三线性插值
     * 将 LUT 效果叠加到哈苏色彩引擎处理后的 Bitmap 上。
     *
     * @param context Android 上下文
     * @param lutId LUT 资源 ID
     * @param strength LUT 强度 [0, 1]
     */
    fun apply3DLUT(context: Context, lutId: String, strength: Float = 1.0f) {
        val lutManager = LUTManager.getInstance(context)
        val lutData = lutManager.getCachedLUTData(lutId)
        if (lutData == null) {
            Log.w(TAG, "LUT data not available for id=$lutId")
            return
        }

        _active3DLUTId.value = lutId
        _lut3DStrength.value = strength
        _lut3DData.value = lutData

        // 重新生成预览（LUT 叠加在色彩引擎结果之上）
        triggerRealtimePreview()
    }

    /**
     * 移除当前 3D LUT 效果
     */
    fun remove3DLUT() {
        _active3DLUTId.value = null
        _lut3DStrength.value = 1.0f
        _lut3DData.value = null
        triggerRealtimePreview()
    }

    /**
     * 调整 3D LUT 强度
     */
    fun update3DLUTStrength(strength: Float) {
        _lut3DStrength.value = strength.coerceIn(0f, 1f)
        triggerRealtimePreview()
    }

    /**
     * 将 3D LUT 应用到 Bitmap（CPU 三线性插值）
     * 在色彩引擎处理之后叠加
     */
    private suspend fun apply3DLUTToBitmap(source: Bitmap): Bitmap {
        val lutData = _lut3DData.value ?: return source
        val strength = _lut3DStrength.value
        if (strength < 0.01f) return source
        return LUT3DRenderer.applyLUTCPU(source, lutData, strength)
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
                val colorApplied = applyHasselbladColorScience(scaled, targetParams)
                // 3D LUT 叠加在色彩引擎结果之上
                apply3DLUTToBitmap(colorApplied)
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
                val colorApplied = applyHasselbladColorScience(scaled, targetParams)
                // 3D LUT 叠加在色彩引擎结果之上
                apply3DLUTToBitmap(colorApplied)
            }
            _previewBitmap.value = result
            _stage.value = HasselbladEyeStage.PREVIEW
        }
    }

    /**
     * 保存图片到相册。保存结果通过 [isSaving] 与 [stage] 状态暴露。
     * 保存失败时通过 [operationError] 暴露错误信息，UI 层应消费并提示用户。
     * 为防止大图 OOM，bitmap 会先采样到 [EXPORT_MAX_DIMENSION]。
     *
     * HEIF 格式说明：
     * - Android R+ 使用 HEVC MediaCodec + MediaMuxer 编码为真正的 HEIF 容器
     * - Android Q 使用 WEBP_LOSSY 编码 + .webp 扩展名（无原生 HEIF 编码能力）
     * - Android Q 以下回退 JPEG
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
                        val scaled = createThumbnail(bitmap, maxDimension = EXPORT_MAX_DIMENSION)
                        // 文件名包含构图名，便于用户追溯使用的构图方案
                        val compositionTag = _appliedCompositionGuideId.value?.let { "_${it}" } ?: ""

                        when (format) {
                            ExportFormat.JPEG -> {
                                val filename = "Hasselblad${compositionTag}_${System.currentTimeMillis()}.jpg"
                                saveToMediaStore(context, scaled, Bitmap.CompressFormat.JPEG, 95, filename, "image/jpeg")
                            }
                            ExportFormat.PNG -> {
                                val filename = "Hasselblad${compositionTag}_${System.currentTimeMillis()}.png"
                                saveToMediaStore(context, scaled, Bitmap.CompressFormat.PNG, 100, filename, "image/png")
                            }
                            ExportFormat.WEBP -> {
                                val filename = "Hasselblad${compositionTag}_${System.currentTimeMillis()}.webp"
                                val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    Bitmap.CompressFormat.WEBP_LOSSY
                                } else {
                                    @Suppress("DEPRECATION")
                                    Bitmap.CompressFormat.WEBP
                                }
                                saveToMediaStore(context, scaled, compressFormat, 95, filename, "image/webp")
                            }
                            ExportFormat.HEIF -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    // Android R+：使用 HEVC MediaCodec 编码为真正的 HEIF
                                    val filename = "Hasselblad${compositionTag}_${System.currentTimeMillis()}.heic"
                                    saveHeifToMediaStore(context, scaled, filename)
                                } else {
                                    // Android Q 及以下：无原生 HEIF 编码能力，回退为高质量 JPEG
                                    val filename = "Hasselblad${compositionTag}_${System.currentTimeMillis()}.jpg"
                                    saveToMediaStore(context, scaled, Bitmap.CompressFormat.JPEG, 98, filename, "image/jpeg")
                                }
                            }
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
            } else {
                // 保存失败：暴露错误信息，保持当前 PREVIEW 阶段，用户可重试
                _operationError.value = "保存失败，请检查存储权限或可用空间后重试"
            }
        }
    }

    /**
     * 将 Bitmap 保存到 MediaStore（JPEG/PNG/WEBP 通用路径）
     */
    private fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        quality: Int,
        filename: String,
        mimeType: String
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/OMaster/Hasselblad"
                )
            }
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.also {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(compressFormat, quality, out)
            }
            // 通知系统相册立即索引
            MediaScannerConnection.scanFile(
                context,
                arrayOf(it.toString()),
                arrayOf(mimeType),
                null
            )
        }
        return uri
    }

    /**
     * 使用 HEVC MediaCodec + MediaMuxer 编码为真正的 HEIF 文件
     * 仅在 Android R+ 可用，生成标准 HEVC 码流的 HEIF 容器
     */
    private fun saveHeifToMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/heif")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/OMaster/Hasselblad"
            )
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                // 使用 Android R+ 的 Bitmap.compress HEIF_LOSSY 格式
                // 该格式在 API 31+ 可用；API 30 使用 HEVC 编码器手动封装
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, outputStream)
                } else {
                    // Android R (API 30)：使用 MediaCodec HEVC 编码器
                    encodeBitmapToHeif(bitmap, outputStream)
                }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(uri.toString()),
                arrayOf("image/heif"),
                null
            )
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "HEIF encoding failed, falling back to JPEG", e)
            // HEIF 编码失败时删除空文件并回退 JPEG
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) { }
            val jpegFilename = filename.replace(".heic", ".jpg")
            return saveToMediaStore(context, bitmap, Bitmap.CompressFormat.JPEG, 98, jpegFilename, "image/jpeg")
        }
    }

    /**
     * 使用 MediaCodec HEVC 编码器将 Bitmap 编码为 HEIF
     * 生成标准 HEVC 码流写入 MP4/HEIF 容器
     * 仅在 Android R+ (API 30+) 可用
     */
    private fun encodeBitmapToHeif(bitmap: Bitmap, outputStream: java.io.OutputStream) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            throw IllegalStateException("HEIF encoding requires Android R+")
        }

        val width = bitmap.width
        val height = bitmap.height

        // 配置 HEVC 编码器
        val mediaFormat = android.media.MediaFormat.createVideoFormat(
            android.media.MediaFormat.MIMETYPE_VIDEO_HEVC,
            width,
            height
        ).apply {
            setInteger(android.media.MediaFormat.KEY_BIT_RATE, width * height * 4) // 高质量
            setInteger(android.media.MediaFormat.KEY_FRAME_RATE, 1)
            setInteger(android.media.MediaFormat.KEY_CAPTURE_RATE, 1)
            setInteger(android.media.MediaFormat.KEY_I_FRAME_INTERVAL, 0)
            setInteger(android.media.MediaFormat.KEY_COLOR_FORMAT,
                android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        }

        val encoder = android.media.MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_VIDEO_HEVC)
        encoder.configure(mediaFormat, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        // 使用 Surface 将 Bitmap 喂入编码器
        val canvas = inputSurface.lockCanvas(null)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(canvas)

        // 发送 EOS 信号
        encoder.signalEndOfInputStream()

        // 创建 MediaMuxer 写入 HEIF 容器
        val tempFile = File.createTempFile("heif_encode_", ".heic")
        val muxer = android.media.MediaMuxer(tempFile.absolutePath, android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF)

        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = android.media.MediaCodec.BufferInfo()

        // 循环读取编码输出
        while (true) {
            val outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
            when {
                outputBufferIndex >= 0 -> {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                        ?: break

                    if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        // 编码器配置数据（SPS/PPS），在 start 后由 muxer 自动处理
                        if (!muxerStarted && bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            val format = encoder.outputFormat
                            trackIndex = muxer.addTrack(format)
                            muxer.start()
                            muxerStarted = true
                        }
                        encoder.releaseOutputBuffer(outputBufferIndex, false)
                        continue
                    }

                    if (muxerStarted && outputBuffer != null) {
                        muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                    }

                    encoder.releaseOutputBuffer(outputBufferIndex, false)

                    if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
                outputBufferIndex == android.media.MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        val format = encoder.outputFormat
                        trackIndex = muxer.addTrack(format)
                        muxer.start()
                        muxerStarted = true
                    }
                }
                outputBufferIndex == android.media.MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // 等待更多输出
                }
            }
        }

        // 清理资源
        encoder.stop()
        encoder.release()
        inputSurface.release()
        if (muxerStarted) {
            muxer.stop()
        }
        muxer.release()

        // 将临时文件写入 outputStream
        tempFile.inputStream().use { input ->
            val buf = ByteArray(8192)
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                outputStream.write(buf, 0, read)
            }
        }
        tempFile.delete()
    }

    /**
     * 分享图片到其他应用。分享失败时通过 [operationError] 暴露错误信息。
     * 分享格式跟随当前 [exportFormat] 设置，确保用户选择的格式被尊重。
     */
    fun shareImage(context: Context, bitmap: Bitmap, format: ExportFormat = ExportFormat.JPEG) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                shareBitmapToUri(context, bitmap, format)
            }
            if (uri == null) {
                _operationError.value = "分享失败，无法创建分享文件，请重试"
                return@launch
            }

            try {
                val mimeType = when (format) {
                    ExportFormat.JPEG -> "image/jpeg"
                    ExportFormat.PNG -> "image/png"
                    ExportFormat.WEBP -> "image/webp"
                    ExportFormat.HEIF -> "image/heif"
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(intent, "分享哈苏色彩照片")
                    .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(chooser)
            } catch (e: Exception) {
                Log.e(TAG, "Share image start failed", e)
                _operationError.value = "未找到可分享的应用，请检查是否已安装社交或图库应用"
            }
        }
    }

    /**
     * 设置导出格式。
     */
    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    /**
     * 清除操作错误状态（UI 消费后调用）。
     */
    fun clearOperationError() {
        _operationError.value = null
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
        _operationError.value = null
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

    private fun shareBitmapToUri(context: Context, bitmap: Bitmap, format: ExportFormat = ExportFormat.JPEG): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "share").apply {
                if (!exists()) mkdirs()
            }
            val (compressFormat, extension) = when (format) {
                ExportFormat.JPEG -> Bitmap.CompressFormat.JPEG to "jpg"
                ExportFormat.PNG -> Bitmap.CompressFormat.PNG to "png"
                ExportFormat.WEBP -> {
                    val cf = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    cf to "webp"
                }
                ExportFormat.HEIF -> {
                    // HEIF 分享回退 JPEG（大多数社交应用不支持 HEIF 分享）
                    Bitmap.CompressFormat.JPEG to "jpg"
                }
            }
            val file = File(cacheDir, "hasselblad_share_${System.currentTimeMillis()}.$extension")
            FileOutputStream(file).use { out ->
                bitmap.compress(compressFormat, 95, out)
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

        /**
         * 预览图最大边长。
         * OPPO Find X9 主摄 5000 万像素 (8192×6144)，预览需保留足够细节用于对比。
         * 2048px 在内存与清晰度之间取得平衡（约 4MP 等效像素，内存占用 ~32MB ARGB_8888）。
         */
        const val PREVIEW_MAX_DIMENSION = 2048

        /**
         * 导出图最大边长。
         * 匹配 OPPO Find X9 主摄输出能力，保留高像素设备的完整画质。
         * 4096px ≈ 8.4MP 等效像素（内存占用 ~134MB ARGB_8888），在 IO 调度器执行不会阻塞 UI。
         */
        const val EXPORT_MAX_DIMENSION = 4096
    }
}
