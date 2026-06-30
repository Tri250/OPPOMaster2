package com.silas.omaster.ui.features

import android.app.Application
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.lut.LUT3DData
import com.silas.omaster.data.lut.LUT3DRenderer
import com.silas.omaster.data.lut.LUTManager
import com.silas.omaster.ai.MasterInferenceEngine
import com.silas.omaster.ai.mapping.FilmAdjustments
import com.silas.omaster.ai.recipe.RecipeMatchResult
import com.silas.omaster.ai.recipe.RecipeRepository
import com.silas.omaster.camera.CameraApplyResult
import com.silas.omaster.feedback.FeedbackManager
import com.silas.omaster.camera.OPPOCameraManager
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.model.SoftLightMode
import com.silas.omaster.renderer.GPURenderManager
import com.silas.omaster.renderer.HasselbladParamMapper
import com.silas.omaster.renderer.RenderParameters
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
class HasselbladEyeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeRepository = RecipeRepository(application.applicationContext)
    private val feedbackManager = FeedbackManager(application.applicationContext)
    private var gpuRenderManager: GPURenderManager? = null

    /** 导出格式 - P2 HEIF支持 */
    enum class ExportFormat {
        JPEG, PNG, WEBP, HEIF
    }

    /**
     * 应用到 OPPO 大师模式相机的状态。
     * UI 层通过 [oppoApplyState] 观察并在状态变更时展示 Toast/SnackBar。
     */
    sealed class OPOApplyState {
        object Idle : OPOApplyState()
        object Applying : OPOApplyState()
        data class Success(val method: String) : OPOApplyState()
        data class PartialSuccess(val method: String, val failedParams: List<String>) : OPOApplyState()
        data class Failed(val reason: String) : OPOApplyState()
    }

    // ================== StateFlow 状态 ==================

    private val _stage = MutableStateFlow(HasselbladEyeStage.SETUP)
    val stage: StateFlow<HasselbladEyeStage> = _stage.asStateFlow()

    private val _params = MutableStateFlow(HasselbladParams())
    val params: StateFlow<HasselbladParams> = _params.asStateFlow()

    private val _recommendedParams = MutableStateFlow<HasselbladParams?>(null)
    val recommendedParams: StateFlow<HasselbladParams?> = _recommendedParams.asStateFlow()

    // ===== 配方系统状态（Phase 1 新增）=====
    private val _recipeParams = MutableStateFlow<HasselbladParams?>(null)
    val recipeParams: StateFlow<HasselbladParams?> = _recipeParams.asStateFlow()

    private val _recipeMatchResult = MutableStateFlow<RecipeMatchResult?>(null)
    val recipeMatchResult: StateFlow<RecipeMatchResult?> = _recipeMatchResult.asStateFlow()

    private val _avoidTips = MutableStateFlow<List<String>>(emptyList())
    val avoidTips: StateFlow<List<String>> = _avoidTips.asStateFlow()

    // 用户拍摄意图（SETUP 阶段输入，分析时用于配方匹配）
    private val _intentQuery = MutableStateFlow<String>("")
    val intentQuery: StateFlow<String> = _intentQuery.asStateFlow()

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

    /** 当前预览图是基于哪个 exportFormat 生成的（用于检测格式切换后是否需要重生成）。 */
    private val _previewBuiltForFormat = MutableStateFlow(ExportFormat.JPEG)
    val previewBuiltForFormat: StateFlow<ExportFormat> = _previewBuiltForFormat.asStateFlow()

    /**
     * P3-6：最近拍摄持久化键名前缀
     * 用于生成带场景模式标签的保存文件名（P2-3 修复）
     */
    private val _recentShotsTag = MutableStateFlow<String?>(null)
    val recentShotsTag: StateFlow<String?> = _recentShotsTag.asStateFlow()

    private val _lastSavedUri = MutableStateFlow<android.net.Uri?>(null)
    val lastSavedUri: StateFlow<android.net.Uri?> = _lastSavedUri.asStateFlow()

    /** 保存/分享操作错误信息（null 表示无错误），UI 层消费后调用 clearOperationError 清除 */
    private val _operationError = MutableStateFlow<String?>(null)
    val operationError: StateFlow<String?> = _operationError.asStateFlow()

    /** 应用到 OPPO 大师模式相机的状态，UI 层消费后调用 resetOPOApplyState 重置 */
    private val _oppoApplyState = MutableStateFlow<OPOApplyState>(OPOApplyState.Idle)
    val oppoApplyState: StateFlow<OPOApplyState> = _oppoApplyState.asStateFlow()

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
        // 加载配方索引
        viewModelScope.launch {
            recipeRepository.load()
        }
        // 参数变化 250ms 后自动触发低分辨率实时预览
        viewModelScope.launch {
            _params.debounce(250).collect {
                triggerRealtimePreview()
            }
        }
    }

    /**
     * 在合适的阶段触发低分辨率实时预览。
     * 使用当前选中色彩模式参数（_currentModeParams）以确保与最终导出效果一致。
     */
    private fun triggerRealtimePreview() {
        val source = _originalBitmap.value ?: return
        if (_stage.value != HasselbladEyeStage.RESULTS && _stage.value != HasselbladEyeStage.PREVIEW) return
        // P3-8 修复：使用当前选中场景 + 色彩模式参数，确保格式切换后预览效果一致
        val currentParams = _selectedSceneParams.value + _selectedColorParams.value
        updatePreviewAsync(source, currentParams)
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

                // Phase 1：自动配方匹配（意图 + 场景）
                val intent = _intentQuery.value
                val matchedRecipes = when {
                    intent.isNotBlank() -> recipeRepository.matchByIntent(intent, limit = 1)
                    else -> recipeRepository.findBySceneId(profile.id).take(1)
                }
                matchedRecipes.firstOrNull()?.let { match ->
                    applyRecipe(match)
                } ?: run {
                    // 无配方匹配时，按场景加载默认反模式提示
                    _avoidTips.value = com.silas.omaster.ai.mapping.getAvoidTips(profile.id)
                }

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
     * P2-4 修复：暴露为 UI 可调，并自动触发预览重生成
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
     * 获取当前生效的哈苏参数。
     *
     * - 参数未锁定时：_params 已通过 rebuildParamsFromModes() 融合了场景/色彩模式参数，
     *   直接返回 _params 即可，避免重复叠加。
     * - 参数锁定时：_params 是用户手动调节的基础值，场景/色彩模式参数在此基础上叠加，
     *   保证锁定后切换模式仍能影响最终效果。
     *
     * 用于跳转到 CameraX 取景器时携带完整生效参数，确保预览/拍摄效果与当前调节一致。
     */
    fun getEffectiveParams(): HasselbladParams {
        return if (_isParamsLocked.value) {
            applyParamsMapToBase(_params.value, _selectedSceneParams.value + _selectedColorParams.value)
        } else {
            _params.value
        }
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
     * Phase 1 优化：优先走 GPU 管线（GPURenderManager），GPU 失败降级到 CPU（HasselbladColorEngine）。
     */
    fun updatePreviewAsync(source: Bitmap, modeParams: Map<String, Int>) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val thumbnail = withContext(Dispatchers.Default) {
                val scaled = createThumbnail(source, maxDimension = 512)
                val targetParams = mergeParams(modeParams)
                renderWithGPUFallback(scaled, targetParams)
            }
            _thumbnailPreview.value = thumbnail
        }
    }

    /**
     * 生成预览效果图，并进入 PREVIEW 阶段。
     * Phase 1 优化：优先走 GPU 管线，GPU 失败降级到 CPU。
     * 为防止大图 OOM，source 会先采样到 [PREVIEW_MAX_DIMENSION]。
     */
    fun generateFullPreview(source: Bitmap, modeParams: Map<String, Int>) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val targetParams = mergeParams(modeParams)
                val scaled = createThumbnail(source, maxDimension = PREVIEW_MAX_DIMENSION)
                renderWithGPUFallback(scaled, targetParams)
            }
            _previewBitmap.value = result
            // P3-8：标记当前预览是为哪个 exportFormat 生成的
            _previewBuiltForFormat.value = _exportFormat.value
            _stage.value = HasselbladEyeStage.PREVIEW
        }
    }

    /**
     * Phase 1 核心：GPU 渲染主路径 + CPU 降级路径。
     * 将 HasselbladParams 映射到 RenderParameters，通过 GPURenderManager 执行 GPU 着色器渲染。
     * 失败时自动降级到 HasselbladColorEngine CPU 路径。
     *
     * P2-5 修复：函数签名改为 Bitmap?，避免 GPURenderManager.render 返回 null 时传递空引用
     * 导致 applyVignetteIfNeeded 违反非空约束。
     */
    private suspend fun renderWithGPUFallback(source: Bitmap, params: HasselbladParams): Bitmap? {
        val manager = gpuRenderManager ?: GPURenderManager.acquire(getApplication()).also {
            gpuRenderManager = it
        }
        return try {
            val renderParams = HasselbladParamMapper.map(
                hasselbladParams = params,
                colorModeParams = _selectedColorParams.value + _selectedSceneParams.value,
                active3DLUTId = _active3DLUTId.value,
                lut3DStrength = _lut3DStrength.value
            )
            val gpuResult = manager.render(source, renderParams)
            if (gpuResult == null) {
                Log.w(TAG, "GPU 渲染返回 null，降级到 CPU 路径")
                val colorApplied = applyHasselbladColorScience(source, params)
                apply3DLUTToBitmap(colorApplied)
            } else {
                // 暗角在 GPU 着色器外单独处理（着色器未含 vignette）
                applyVignetteIfNeeded(gpuResult, params.vignette)
            }
        } catch (e: Exception) {
            Log.w(TAG, "GPU 渲染失败，降级到 CPU 路径", e)
            val colorApplied = applyHasselbladColorScience(source, params)
            apply3DLUTToBitmap(colorApplied)
        }
    }

    /**
     * 暗角后处理（GPU 着色器暂不支持径向暗角，单独处理）。
     * P2-5 修复：接收 Bitmap? 并在 null 时直接返回 null，避免空指针。
     */
    private fun applyVignetteIfNeeded(bitmap: Bitmap?, vignette: Int): Bitmap? {
        if (bitmap == null || vignette <= 0) return bitmap
        // 复用 HasselbladColorEngine 的暗角实现（轻量操作）
        return applyHasselbladColorEngineVignette(bitmap, vignette)
    }

    // ===== 配方系统方法（Phase 1 新增）=====

    /**
     * 应用指定配方到当前参数。
     * 配方参数与 AI 推荐参数融合：配方权重 0.6 + AI 权重 0.4
     */
    fun applyRecipe(recipeMatch: RecipeMatchResult) {
        val recipe = recipeMatch.recipe
        _recipeMatchResult.value = recipeMatch
        _avoidTips.value = recipe.avoidTips
        _recipeParams.value = recipe.toHasselbladParams()

        // 配方参数与 AI 推荐参数融合
        val aiParams = _recommendedParams.value
        if (aiParams != null) {
            val fused = fuseParams(aiParams, recipe.toHasselbladParams(), recipeWeight = 0.6f)
            if (!_isParamsLocked.value) {
                _params.value = fused
            }
        } else {
            if (!_isParamsLocked.value) {
                _params.value = recipe.toHasselbladParams()
            }
        }

        // 自动应用配方推荐的 LUT（加载数据到内存）
        recipe.lutRecommendation?.let { lut ->
            apply3DLUT(getApplication(), lut.id, lut.strength.toFloat())
        }

        // 自动应用配方推荐的 AR 构图线
        try {
            val guideType = ARGuideType.valueOf(recipe.arGuideLine)
            _appliedARGuideType.value = guideType
            _isARGuideEnabled.value = true
        } catch (_: IllegalArgumentException) {
            // 忽略不支持的构图线
        }

        Log.i(TAG, "Applied recipe: ${recipe.id} ${recipe.name}, matchScore=${recipeMatch.matchScore}")
    }

    /**
     * 清除当前配方，恢复到 AI 推荐参数。
     */
    fun clearRecipe() {
        _recipeMatchResult.value = null
        _recipeParams.value = null
        _avoidTips.value = emptyList()
        _recommendedParams.value?.let {
            if (!_isParamsLocked.value) _params.value = it
        }
    }

    // ---------------- Phase 3.1：用户反馈闭环 ----------------

    /**
     * 提交用户反馈。
     */
    fun submitFeedback(
        rating: Int,
        tags: List<String>,
        comment: String,
        screenshot: android.graphics.Bitmap? = null
    ) {
        feedbackManager.submitFeedback(
            rating = rating,
            tags = tags,
            comment = comment,
            screenshot = screenshot,
            sceneId = _analysisResult.value?.sceneProfile?.id,
            recipeId = _recipeMatchResult.value?.recipe?.id,
            params = _params.value
        )
    }

    val feedbackPendingCount = feedbackManager.pendingCount
    val feedbackUploadStatus = feedbackManager.uploadStatus

    /**
     * 双参数融合：基于权重混合两个 HasselbladParams。
     */
    private fun fuseParams(
        aiParams: HasselbladParams,
        recipeParams: HasselbladParams,
        recipeWeight: Float = 0.6f
    ): HasselbladParams {
        val w = recipeWeight.coerceIn(0f, 1f)
        val aw = 1f - w
        fun blend(ai: Int, rp: Int): Int = ((ai * aw + rp * w).toInt()).coerceIn(-30, 30)
        fun blendClarity(ai: Int, rp: Int): Int = ((ai * aw + rp * w).toInt()).coerceIn(0, 30)

        return HasselbladParams(
            tone = blend(aiParams.tone, recipeParams.tone),
            saturation = blend(aiParams.saturation, recipeParams.saturation),
            contrast = blend(aiParams.contrast, recipeParams.contrast),
            colorTemp = blend(aiParams.colorTemp, recipeParams.colorTemp),
            sharpness = blend(aiParams.sharpness, recipeParams.sharpness),
            vignette = blend(aiParams.vignette, recipeParams.vignette),
            cyanMagenta = blend(aiParams.cyanMagenta, recipeParams.cyanMagenta),
            softLight = if (w >= 0.5f) recipeParams.softLight else aiParams.softLight,
            highlights = blend(aiParams.highlights, recipeParams.highlights),
            shadows = blend(aiParams.shadows, recipeParams.shadows),
            clarity = blendClarity(aiParams.clarity, recipeParams.clarity)
        )
    }

    /**
     * 设置用户拍摄意图查询词（SETUP 阶段使用）。
     */
    fun setIntentQuery(query: String) {
        _intentQuery.value = query.trim()
    }

    /**
     * 根据意图关键词搜索配方（SETUP 阶段实时搜索）。
     */
    fun searchRecipesByIntent(query: String): List<RecipeMatchResult> {
        return recipeRepository.matchByIntent(query, limit = 5)
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
                        try {
                            // P2-3 修复：使用带场景模式 + 构图 + 时间戳的命名规范

                            when (format) {
                                ExportFormat.JPEG -> {
                                    val filename = buildExportFilename(format)
                                    saveToMediaStore(context, scaled, Bitmap.CompressFormat.JPEG, 95, filename, "image/jpeg")
                                }
                                ExportFormat.PNG -> {
                                    val filename = buildExportFilename(format)
                                    saveToMediaStore(context, scaled, Bitmap.CompressFormat.PNG, 100, filename, "image/png")
                                }
                                ExportFormat.WEBP -> {
                                    val filename = buildExportFilename(format)
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
                                        val filename = buildExportFilename(format)
                                        saveHeifToMediaStore(context, scaled, filename)
                                    } else {
                                        // Android Q 及以下：无原生 HEIF 编码能力，回退为高质量 JPEG
                                        val filename = buildExportFilename(ExportFormat.JPEG)
                                        saveToMediaStore(context, scaled, Bitmap.CompressFormat.JPEG, 98, filename, "image/jpeg")
                                    }
                                }
                            }
                        } finally {
                            if (scaled !== bitmap && !scaled.isRecycled) scaled.recycle()
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
                // 统一使用 MediaCodec HEVC 编码器生成真正的 HEIF 容器。
                // 注意：Bitmap.CompressFormat.WEBP_LOSSY 并非 HEIF 编码，会导致 .heic 文件
                // 实际内容为 WEBP；而 Bitmap.compress 的 HEIF 格式直到 API 34 才支持，
                // API 30-33 无原生 Bitmap HEIF 编码能力，因此统一走 MediaCodec 路径。
                encodeBitmapToHeif(bitmap, outputStream)
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
            } catch (e: Exception) {
                Log.w(TAG, "删除失败 HEIF 占位文件失败", e)
            }
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
            Log.e(TAG, "HEIF encoding requires Android R+")
            return
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
     * P2-3 修复：生成带场景模式 + 构图 + 时间戳的保存文件名
     * 格式：Hasselblad_{sceneMode}_{compositionGuide}_{timestamp}.{ext}
     * 例如：Hasselblad_scene-portrait_thirds_1734000000.jpg
     */
    private fun buildExportFilename(format: ExportFormat): String {
        val ext = when (format) {
            ExportFormat.JPEG -> "jpg"
            ExportFormat.PNG -> "png"
            ExportFormat.WEBP -> "webp"
            ExportFormat.HEIF -> "heic"
        }
        val sceneTag = _selectedSceneModeId.value.takeIf { it.isNotEmpty() } ?: "natural"
        val compositionTag = _appliedCompositionGuideId.value?.takeIf { it.isNotEmpty() }
        val tags = listOfNotNull("Hasselblad", sceneTag, compositionTag).joinToString("_")
        return "${tags}_${System.currentTimeMillis()}.${ext}"
    }

    /**
     * 设置导出格式。
     * P3-8 修复：格式切换后自动触发预览重生成，确保用户看到的预览与最终导出格式一致。
     */
    fun setExportFormat(format: ExportFormat) {
        if (_exportFormat.value == format) return
        _exportFormat.value = format
        // 若当前已在 RESULTS/PREVIEW 阶段，立即重生成预览
        triggerRealtimePreview()
    }

    /**
     * 清除操作错误状态（UI 消费后调用）。
     */
    fun clearOperationError() {
        _operationError.value = null
    }

    /**
     * 将当前调好的哈苏参数 + 当前构图方案应用到 OPPO 大师模式相机。
     *
     * P2-2 修复：在原 applyHasselbladParams 基础上，额外：
     * 1. 若有活跃构图 (_appliedCompositionGuideId)，把构图 ID 写入 OPPO
     * 2. 把当前场景模式 ID 写入 OPPO 便于 OPPO 大师模式相机适配
     * 3. 把 LUT 强度（若活跃）写入 OPPO
     *
     * 应用优先级：ContentProvider → System Settings → Camera Intent → Clipboard
     * 应用结果通过 [oppoApplyState] 暴露，UI 层消费后应调用 [resetOPOApplyState] 重置回 Idle。
     */
    fun applyToOPPOMaster(context: Context) {
        viewModelScope.launch {
            _oppoApplyState.value = OPOApplyState.Applying
            try {
                val params = _params.value
                val cameraManager = OPPOCameraManager.getInstance(context)
                val compositionId = _appliedCompositionGuideId.value
                val sceneModeId = _selectedSceneModeId.value
                val lutId = _active3DLUTId.value
                val lutStrength = _lut3DStrength.value
                val result = cameraManager.applyHasselbladParams(
                    params = params,
                    compositionId = compositionId,
                    sceneModeId = sceneModeId,
                    lutId = lutId,
                    lutStrength = lutStrength
                )
                when (result) {
                    is CameraApplyResult.Success ->
                        _oppoApplyState.value = OPOApplyState.Success(result.method.name)
                    is CameraApplyResult.PartialSuccess ->
                        _oppoApplyState.value = OPOApplyState.PartialSuccess(result.method.name, result.failedParams)
                    is CameraApplyResult.Failed ->
                        _oppoApplyState.value = OPOApplyState.Failed(result.reason)
                }
            } catch (e: Exception) {
                Log.e(TAG, "applyToOPPOMaster failed", e)
                _oppoApplyState.value = OPOApplyState.Failed(e.message ?: "未知错误")
            }
        }
    }

    /**
     * 重置 OPPO 应用状态为 Idle（UI 消费完结果后调用）。
     */
    fun resetOPOApplyState() {
        _oppoApplyState.value = OPOApplyState.Idle
    }

    /**
     * P2-5 修复：将当前哈苏参数应用到 OPPO 大师模式后，自动拉起悬浮窗，
     * 便于用户在拍照时实时查看当前应用参数。
     *
     * 流程：先调用 applyToOPPOMaster，然后通过 FloatingWindowController 显示当前预设。
     */
    fun applyToOPPOAndShowFloating(context: Context) {
        applyToOPPOMaster(context)
        val colorMode = allColorModes.find { it.id == _selectedColorModeId.value }
        val sceneMode = allSceneModes.find { it.id == _selectedSceneModeId.value }
        if (colorMode != null) {
            viewModelScope.launch {
                // 构造临时 MasterPreset 用于悬浮窗展示
                val livePreset = com.silas.omaster.model.MasterPreset(
                    name = "${colorMode.name}${if (sceneMode != null) " · ${sceneMode.name}" else ""}",
                    coverPath = "",
                    author = "@哈苏之眼实时",
                    mode = "hasselblad",
                    isCustom = true,
                    description = com.silas.omaster.model.PresetDescription(
                        title = colorMode.name,
                        content = colorMode.description
                    ),
                    tags = listOf("实时", "哈苏之眼"),
                    params = colorMode.params.mapValues { it.value.toString() }
                )
                val controller = com.silas.omaster.ui.service.FloatingWindowController.getInstance(context)
                controller.showFloatingWindow(livePreset, listOf(livePreset))
            }
        }
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
        _previewBuiltForFormat.value = ExportFormat.JPEG
        _lastSavedUri.value = null
        _operationError.value = null
        _oppoApplyState.value = OPOApplyState.Idle
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
        // 释放反馈管理器后台上传工作器
        feedbackManager.release()
        // 释放 GPU 渲染器引用计数
        gpuRenderManager?.release()
        gpuRenderManager = null
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
        // 返回的名称必须与 allColorModes 中 ColorMode.name 完全一致，
        // 否则 resolveColorModeId 会找不到匹配而回退到 "natural"。
        val modeMapping = mapOf(
            "PORTRAIT" to "人像肤色",
            "LANDSCAPE" to "风景色彩",
            "NIGHT" to "经典胶片",
            "FOOD" to "鲜艳色彩",
            "URBAN" to "经典胶片",
            "STILL_LIFE" to "自然色彩",
            "MACRO" to "鲜艳色彩",
            "EVENT" to "自然色彩",
            "UNKNOWN" to "自然色彩"
        )
        val displayName = modeMapping[profile.category.name] ?: "自然色彩"
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
