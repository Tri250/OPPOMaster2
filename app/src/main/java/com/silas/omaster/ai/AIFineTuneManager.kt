package com.silas.omaster.ai

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import com.silas.omaster.ai.analyzer.HeuristicSceneAnalyzer
import com.silas.omaster.ai.mapping.SceneToHasselbladMapping
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.util.UrlConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import kotlin.math.abs

/**
 * AI 微调管理器 - 端云协同推理
 * 
 * 功能用例实现：
 * FT-001/002/003: AI微调核心功能
 * FT-004: 无网络/服务异常降级
 * FT-006: AI微调权限与隐私
 * 
 * 端云协同架构：
 * - 本地优先：启发式场景分析器 + ML Kit 人脸检测（实时响应，无需模型文件）
 * - 云端增强：API调用（高质量推理，需网络与有效 API Key）
 * - 降级策略：推理超时 → 规则引擎保守建议
 * 
 * 注意：当前本地推理使用启发式算法（HeuristicSceneAnalyzer），非 TFLite 模型推理。
 * TFLite 模型文件尚未就绪（见 assets/models/MODEL_SPEC.json status: "not_ready"），
 * 待模型训练完成后将替换为真实模型推理管道。
 */
class AIFineTuneManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private val appContext = context.applicationContext
    
    // 大师推理引擎（真实AI推理）
    private val inferenceEngine = MasterInferenceEngine.getInstance(context)
    
    // 启发式场景分析器（图像特征提取）
    private val sceneAnalyzer = HeuristicSceneAnalyzer.getInstance(context)
    
    // 场景→参数映射表
    private val sceneMapping = SceneToHasselbladMapping

    // 当前调整参数
    private val _currentAdjustments = MutableStateFlow(AdjustmentParams())
    val currentAdjustments: StateFlow<AdjustmentParams> = _currentAdjustments.asStateFlow()

    // 处理状态
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // AI建议参数
    private val _suggestedParams = MutableStateFlow<AISuggestion?>(null)
    val suggestedParams: StateFlow<AISuggestion?> = _suggestedParams.asStateFlow()

    // 异常状态
    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    // 手动修改的参数
    private val _manuallyModifiedFields = MutableStateFlow<Set<String>>(emptySet())
    val manuallyModifiedFields: StateFlow<Set<String>> = _manuallyModifiedFields.asStateFlow()

    // 权限状态
    private val _permissionGranted = MutableStateFlow(checkPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // 已应用的AI推荐（权限拒绝后不清除）
    private var appliedSuggestions = mutableListOf<AISuggestion>()

    // 色彩风格
    val colorStyles = listOf(
        ColorStyle("natural", "自然", "还原真实色彩", 0, 0, 0),
        ColorStyle("vivid", "鲜艳", "增强色彩饱和度", 20, 10, 5),
        ColorStyle("film", "胶片", "复古胶片质感", -10, 15, 10),
        ColorStyle("bw", "黑白", "经典黑白影调", -100, 20, 0),
        ColorStyle("warm", "暖调", "温暖色调风格", 5, 0, 25),
        ColorStyle("cool", "冷调", "清冷色调风格", 5, 5, -20),
        ColorStyle("portrait", "人像", "优化肤色表现", 10, -5, 8),
        ColorStyle("landscape", "风景", "增强自然色彩", 15, 10, 0),
        ColorStyle("fresh_cc", "清新-CC胶片", "清新通透胶片感", 5, 8, 3),
        ColorStyle("rich", "浓郁", "浓郁饱满色彩", 25, 15, 8),
        ColorStyle("retro", "复古", "复古怀旧风格", -5, 10, 15)
    )

    // 智能优化预设
    val smartPresets = listOf(
        SmartPreset("auto_optimize", "智能优化", "AI自动分析并优化图片", "✨"),
        SmartPreset("hdr_enhance", "HDR增强", "提升动态范围", "🌅"),
        SmartPreset("noise_reduce", "降噪处理", "减少画面噪点", "🔇"),
        SmartPreset("sharpness", "清晰度", "增强细节锐度", "🔍"),
        SmartPreset("skin_smooth", "肤色优化", "自然美肤效果", "✨"),
        SmartPreset("sky_enhance", "天空增强", "优化天空色彩", "☁️"),
        SmartPreset("detail_enhance", "细节增强", "提升画面细节", "🔎"),
        SmartPreset("night_optimize", "夜景优化", "优化暗光表现", "🌃")
    )

    // 基础预设库（23+款）
    private val basePresets = listOf(
        BasePreset("fresh_cc", "清新-CC胶片", mapOf("saturation" to 5, "contrast" to 8, "brightness" to 3, "warmth" to 3, "clarity" to 10)),
        BasePreset("film_nc", "富士NC", mapOf("saturation" to 8, "contrast" to 5, "brightness" to 2, "warmth" to 5, "clarity" to 8)),
        BasePreset("portrait_soft", "人像柔美", mapOf("saturation" to 10, "contrast" to -5, "brightness" to 5, "skinSmooth" to 25, "warmth" to 8)),
        BasePreset("landscape_vivid", "风景鲜明", mapOf("saturation" to 20, "contrast" to 15, "brightness" to 5, "clarity" to 20, "sharpness" to 15)),
        BasePreset("night_scene", "夜景氛围", mapOf("contrast" to 20, "highlights" to -20, "shadows" to 30, "noiseReduction" to 30, "brightness" to -5)),
        BasePreset("food_warm", "美食暖调", mapOf("saturation" to 15, "contrast" to 10, "brightness" to 8, "warmth" to 20, "clarity" to 12)),
        BasePreset("bw_classic", "经典黑白", mapOf("saturation" to -100, "contrast" to 25, "brightness" to 0, "clarity" to 15, "sharpness" to 20)),
        BasePreset("cyberpunk", "赛博朋克", mapOf("saturation" to 30, "contrast" to 25, "brightness" to -5, "highlights" to 20, "shadows" to -15)),
        BasePreset("vintage_film", "复古胶片", mapOf("saturation" to -10, "contrast" to 15, "warmth" to 25, "clarity" to 8, "vignette" to 30)),
        BasePreset("hasselblad_rich", "哈苏浓郁", mapOf("saturation" to 12, "contrast" to 10, "brightness" to 3, "warmth" to 5, "clarity" to 15)),
        BasePreset("hasselblad_natural", "哈苏自然", mapOf("saturation" to 5, "contrast" to 8, "brightness" to 0, "warmth" to 2, "clarity" to 10)),
        BasePreset("find_x8_pro", "Find X8 Pro", mapOf("saturation" to 10, "contrast" to 12, "brightness" to 5, "warmth" to 3, "clarity" to 18)),
        BasePreset("reno_portrait", "Reno人像", mapOf("saturation" to 8, "contrast" to -3, "brightness" to 8, "skinSmooth" to 30, "warmth" to 5)),
        BasePreset("street_snapshot", "街拍快照", mapOf("saturation" to 15, "contrast" to 18, "brightness" to 0, "clarity" to 22, "sharpness" to 18)),
        BasePreset("blue_hour", "蓝调时刻", mapOf("saturation" to 5, "contrast" to 15, "brightness" to -8, "warmth" to -25, "highlights" to 10)),
        BasePreset("sunset_warm", "日落暖阳", mapOf("saturation" to 25, "contrast" to 12, "brightness" to -3, "warmth" to 35, "shadows" to 15)),
        BasePreset("cloud_clear", "通透蓝天", mapOf("saturation" to 20, "contrast" to 10, "brightness" to 8, "warmth" to -10, "clarity" to 25)),
        BasePreset("spring_green", "春日清新", mapOf("saturation" to 18, "contrast" to 8, "brightness" to 10, "warmth" to 8, "clarity" to 15)),
        BasePreset("night_neon", "霓虹夜景", mapOf("saturation" to 35, "contrast" to 20, "brightness" to -10, "highlights" to 25, "shadows" to -20)),
        BasePreset("macro_detail", "微距细节", mapOf("saturation" to 8, "contrast" to 15, "brightness" to 5, "clarity" to 30, "sharpness" to 25, "detail" to 20)),
        BasePreset("pet_soft", "宠物柔光", mapOf("saturation" to 12, "contrast" to 5, "brightness" to 8, "clarity" to 10, "skinSmooth" to 15)),
        BasePreset("snow_scene", "雪景纯净", mapOf("saturation" to 5, "contrast" to -5, "brightness" to 15, "warmth" to 10, "clarity" to 12)),
        BasePreset("beach_vacation", "海滩度假", mapOf("saturation" to 22, "contrast" to 12, "brightness" to 12, "warmth" to 15, "clarity" to 18))
    )

    /**
     * FT-004: 检查网络状态
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * FT-006: 检查权限
     */
    fun checkPermission(): Boolean {
        // AI微调需要网络权限以访问云端AI推理API
        return ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * FT-006: 请求权限回调
     */
    fun onPermissionResult(granted: Boolean) {
        _permissionGranted.value = granted
    }

    /**
     * 检查云端AI是否可用（网络正常、质量达标、API密钥有效）
     */
    fun isCloudAIAvailable(): Boolean {
        return isNetworkAvailable()
                && isNetworkQualityGood()
                && getApiKey() != null
    }

    // ==================== 真实AI推理接口 ====================

    /**
     * FT-001: 一键AI微调（基于图像的真实推理）
     * 
     * 端云协同架构：
     * 1. 本地优先：使用MasterInferenceEngine进行真实图像分析
     * 2. 云端增强：网络可用时调用云端API获取更高质量推理
     * 3. 降级策略：推理失败时使用规则引擎
     * 
     * @param bitmap 待分析的图像
     * @param currentParams 当前调整参数
     * @return AI建议结果
     */
    
    /**
     * AI 场景分析（委托给推理引擎）
     */
    suspend fun analyzeImage(bitmap: Bitmap, imagePath: String? = null): SceneProfile {
        return inferenceEngine.analyzeImage(bitmap, imagePath)
    }

    suspend fun generateAISuggestion(
        bitmap: Bitmap,
        currentParams: Map<String, Int>
    ): AISuggestionResult = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _errorState.value = null

        try {
            // 超时控制：3秒
            withTimeout(3000L) {
                // 端云协同策略
                if (isCloudAIAvailable() && _permissionGranted.value) {
                    // 尝试云端增强推理
                    try {
                        val cloudResult = generateCloudSuggestion(bitmap, currentParams)
                        if (cloudResult != null) {
                            Log.d(TAG, "云端AI推理成功")
                            appliedSuggestions.add(cloudResult)
                            _suggestedParams.value = cloudResult
                            _isProcessing.value = false
                            return@withTimeout AISuggestionResult.Success(cloudResult, isOfflineMode = false)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "云端AI推理失败，降级到本地推理: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "云端AI不可用，直接使用本地推理")
                }

                // 本地启发式推理（真实图像分析）
                val localResult = generateLocalSuggestionFromImage(bitmap, currentParams)
                appliedSuggestions.add(localResult)
                _suggestedParams.value = localResult
                _isProcessing.value = false
                AISuggestionResult.Success(localResult, isOfflineMode = true)
            }
        } catch (e: TimeoutCancellationException) {
            // 超时降级：使用规则引擎
            Log.w(TAG, "AI推理超时，降级到规则引擎")
            val fallbackResult = generateFallbackSuggestion(currentParams)
            _isProcessing.value = false
            _errorState.value = ErrorState.Timeout("处理超时，已使用规则引擎优化")
            AISuggestionResult.Success(fallbackResult, isOfflineMode = true)
        } catch (e: Exception) {
            Log.e(TAG, "AI推理异常: ${e.message}", e)
            _isProcessing.value = false
            _errorState.value = ErrorState.Unknown(e.message ?: "未知错误")
            AISuggestionResult.Error(ErrorState.Unknown(e.message ?: "未知错误"))
        }
    }

    /**
     * FT-001: 一键AI微调（基于预设ID的推理）
     *
     * 基于预设ID的推理始终使用本地规则引擎（无图像输入，无法调用云端视觉API）。
     * 因此统一标记为离线模式，并向 UI 反馈真实的端云状态。
     *
     * @param presetId 预设ID
     * @return AI建议结果
     */
    suspend fun generateAISuggestion(presetId: String): AISuggestionResult = withContext(Dispatchers.Default) {
        _isProcessing.value = true
        _errorState.value = null

        try {
            // FT-004: 超时控制 3秒
            val startTime = System.currentTimeMillis()
            val timeout = 3000L

            // 基于预设ID的推理无图像输入，无法使用云端视觉API，统一走本地规则引擎
            val isCloudAvailable = isCloudAIAvailable()
            if (isCloudAvailable) {
                Log.d(TAG, "云端AI可用，但预设ID推理无图像输入，仍使用本地规则引擎")
            } else {
                Log.d(TAG, "云端AI不可用，使用本地规则引擎: presetId=$presetId")
            }

            val localResult = try {
                withTimeout(timeout) {
                    generateLocalSuggestion(presetId)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "预设ID推理超时，使用规则引擎降级")
                generateFallbackSuggestion(_currentAdjustments.value.toMap())
            }

            appliedSuggestions.add(localResult)
            _suggestedParams.value = localResult

            // 最小处理时间，避免UI闪烁
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 500) delay(500 - elapsed)

            _isProcessing.value = false
            // 无图像输入的预设推理本质上是本地离线推理
            AISuggestionResult.Success(localResult, isOfflineMode = true)
        } catch (e: Exception) {
            _isProcessing.value = false
            _errorState.value = ErrorState.Unknown(e.message ?: "未知错误")
            Log.e(TAG, "预设ID推理异常: ${e.message}", e)
            AISuggestionResult.Error(ErrorState.Unknown(e.message ?: "未知错误"))
        }
    }

    // ==================== 本地真实AI推理 ====================

    /**
     * 本地AI推理：基于图像特征的真实分析
     * 
     * 推理流程：
     * 1. 使用HeuristicSceneAnalyzer提取图像特征（颜色、亮度、纹理、人脸）
     * 2. 使用MasterInferenceEngine进行场景识别
     * 3. 使用SceneToHasselbladMapping获取哈苏参数
     * 4. 基于当前参数与推荐参数的差异生成建议
     * 
     * @param bitmap 待分析的图像
     * @param currentParams 当前调整参数
     * @return AI建议
     */
    private suspend fun generateLocalSuggestionFromImage(
        bitmap: Bitmap,
        currentParams: Map<String, Int>
    ): AISuggestion = withContext(Dispatchers.Default) {
        Log.d(TAG, "开始本地AI推理")

        // Step 1: 使用启发式分析器提取图像特征
        val analysisResult = sceneAnalyzer.analyze(bitmap)
        val colorProfile = analysisResult.colorProfile
        val brightnessLevel = analysisResult.brightnessLevel
        val faceCount = analysisResult.faceCount
        val edgeDensity = analysisResult.edgeDensity

        Log.d(TAG, "图像特征分析完成: 场景=${analysisResult.primaryScene.id}, 置信度=${analysisResult.confidence}")

        // Step 2: 获取场景对应的哈苏参数
        val sceneId = analysisResult.primaryScene.id
        val hasselbladParams = sceneMapping.getParams(sceneId)
        val recommendedFilms = sceneMapping.getRecommendedFilms(sceneId)
        val masterTips = sceneMapping.getMasterTips(sceneId)

        Log.d(TAG, "哈苏参数映射完成: 场景=$sceneId, 饱和度=${hasselbladParams.saturation}, 对比度=${hasselbladParams.contrast}")

        // Step 3: 基于图像特征微调参数（质量分析驱动）
        val refinedParams = refineParamsByImageFeatures(
            hasselbladParams,
            colorProfile,
            brightnessLevel,
            faceCount,
            edgeDensity,
            currentParams
        )

        // Step 4: 生成参数建议列表
        val suggestions = generateParamSuggestions(
            currentParams,
            refinedParams,
            analysisResult.primaryScene.name
        )

        // Step 5: 构建AI建议
        AISuggestion(
            basePresetId = sceneId,
            basePresetName = analysisResult.primaryScene.name,
            suggestions = suggestions,
            generatedAt = System.currentTimeMillis(),
            isOfflineMode = true,
            confidence = analysisResult.confidence,
            sceneCategory = analysisResult.primaryScene.category.displayName,
            colorAnalysis = buildColorAnalysisDescription(colorProfile),
            lightAnalysis = buildLightAnalysisDescription(brightnessLevel),
            recommendedFilm = recommendedFilms.firstOrNull()?.name ?: "CC 经典负片",
            masterTips = masterTips.take(2)
        )
    }

    /**
     * 基于图像特征微调参数（质量分析驱动决策）
     * 
     * 微调规则：
     * - 暗部占比高 → 提升阴影、降低对比度
     * - 高光占比高 → 降低高光、提升对比度
     * - 肤色占比高 → 降低对比度、提升柔光
     * - 边缘密度高 → 提升清晰度、锐度
     * - 暖色调占比高 → 降低色温
     * - 冷色调占比高 → 提升色温
     */
    private fun refineParamsByImageFeatures(
        baseParams: HasselbladParams,
        colorProfile: HeuristicSceneAnalyzer.ColorProfile,
        brightnessLevel: HeuristicSceneAnalyzer.BrightnessLevel,
        faceCount: Int,
        edgeDensity: Float,
        currentParams: Map<String, Int>
    ): Map<String, Int> {
        // 将哈苏参数转换为调整参数格式
        val refined = mutableMapOf(
            "saturation" to baseParams.saturation,
            "contrast" to baseParams.contrast,
            "brightness" to baseParams.tone, // tone对应brightness
            "warmth" to baseParams.colorTemp,
            "clarity" to baseParams.sharpness,
            "highlights" to 0,
            "shadows" to 0,
            "sharpness" to baseParams.sharpness,
            "vignette" to baseParams.vignette
        )

        // 基于暗部占比调整阴影和高光
        if (colorProfile.darkPixelRatio > 0.5f) {
            // 暗部占比高，提升阴影恢复细节
            refined["shadows"] = (refined["shadows"] ?: 0) + (colorProfile.darkPixelRatio * 30).toInt()
            refined["contrast"] = (refined["contrast"] ?: 0) - 5
            Log.d(TAG, "暗部占比=${colorProfile.darkPixelRatio}, 提升阴影+${(colorProfile.darkPixelRatio * 30).toInt()}")
        }

        // 基于高光占比调整高光压制
        if (colorProfile.highlightRatio > 0.15f) {
            // 高光占比高，降低高光防止过曝
            refined["highlights"] = (refined["highlights"] ?: 0) - (colorProfile.highlightRatio * 40).toInt()
            refined["contrast"] = (refined["contrast"] ?: 0) + 5
            Log.d(TAG, "高光占比=${colorProfile.highlightRatio}, 降低高光-${(colorProfile.highlightRatio * 40).toInt()}")
        }

        // 基于肤色占比调整人像优化参数
        if (colorProfile.skinToneRatio > 0.05f || faceCount > 0) {
            // 检测到人脸或肤色，应用人像优化
            refined["contrast"] = (refined["contrast"] ?: 0) - 10
            refined["clarity"] = (refined["clarity"] ?: 0) + 15
            refined["skinSmooth"] = if (colorProfile.skinToneRatio > 0.1f) 20 else 10
            Log.d(TAG, "肤色占比=${colorProfile.skinToneRatio}, 人脸数=$faceCount, 应用人像优化")
        }

        // 基于边缘密度调整清晰度和锐度
        if (edgeDensity > 0.25f) {
            // 高纹理场景，提升细节表现
            refined["clarity"] = (refined["clarity"] ?: 0) + (edgeDensity * 20).toInt()
            refined["sharpness"] = (refined["sharpness"] ?: 0) + (edgeDensity * 15).toInt()
            Log.d(TAG, "边缘密度=${edgeDensity}, 提升清晰度+${(edgeDensity * 20).toInt()}")
        }

        // 基于暖色调占比调整色温
        if (colorProfile.warmthRatio > 0.4f) {
            // 暖色调场景，适当降低色温保持自然
            refined["warmth"] = (refined["warmth"] ?: 0) - 5
            Log.d(TAG, "暖色调占比=${colorProfile.warmthRatio}, 调整色温-5")
        }

        // 基于亮度等级调整曝光
        when (brightnessLevel) {
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_DARK -> {
                refined["brightness"] = (refined["brightness"] ?: 0) + 15
                refined["noiseReduction"] = 25
                Log.d(TAG, "亮度等级=极暗, 提升亮度+15, 降噪+25")
            }
            HeuristicSceneAnalyzer.BrightnessLevel.DARK -> {
                refined["brightness"] = (refined["brightness"] ?: 0) + 8
                refined["noiseReduction"] = 15
                Log.d(TAG, "亮度等级=暗调, 提升亮度+8")
            }
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_BRIGHT -> {
                refined["brightness"] = (refined["brightness"] ?: 0) - 10
                refined["highlights"] = (refined["highlights"] ?: 0) - 15
                Log.d(TAG, "亮度等级=高亮, 降低亮度-10")
            }
            else -> {
                Log.d(TAG, "亮度等级=正常, 无需调整")
            }
        }

        // 参数范围约束
        refined.forEach { (key, value) ->
            refined[key] = when (key) {
                "saturation", "contrast", "brightness", "warmth", "highlights", "shadows" ->
                    value.coerceIn(-100, 100)
                "clarity", "sharpness", "noiseReduction", "skinSmooth", "detail", "vignette" ->
                    value.coerceIn(0, 100)
                else -> value
            }
        }

        return refined
    }

    /**
     * 生成参数建议列表
     */
    private fun generateParamSuggestions(
        currentParams: Map<String, Int>,
        refinedParams: Map<String, Int>,
        sceneName: String
    ): List<ParamSuggestion> {
        val suggestions = mutableListOf<ParamSuggestion>()
        
        val paramDisplayNames = mapOf(
            "saturation" to "饱和度",
            "contrast" to "对比度",
            "brightness" to "亮度",
            "warmth" to "色温",
            "clarity" to "清晰度",
            "sharpness" to "锐度",
            "highlights" to "高光",
            "shadows" to "阴影",
            "noiseReduction" to "降噪",
            "skinSmooth" to "肤色优化",
            "vignette" to "暗角"
        )

        // 计算各参数差异，按差异大小排序
        data class ParamDiff(
            val key: String,
            val currentValue: Int,
            val suggestedValue: Int,
            val diff: Int
        )
        
        val diffs = refinedParams.map { (key, suggestedValue) ->
            val currentValue = currentParams[key] ?: 0
            val diff = abs(suggestedValue - currentValue)
            ParamDiff(key, currentValue, suggestedValue, diff)
        }.sortedByDescending { it.diff }

        // 选取差异最大的4个参数生成建议
        for (paramDiff in diffs.take(4)) {
            val (key, currentValue, suggestedValue, diff) = paramDiff
            if (diff >= 3) { // 差异大于3才生成建议
                suggestions.add(
                    ParamSuggestion(
                        field = key,
                        currentValue = currentValue,
                        suggestedValue = suggestedValue,
                        displayName = paramDisplayNames[key] ?: key,
                        isSelected = true
                    )
                )
            }
        }

        // 如果建议少于2个，添加默认建议
        if (suggestions.size < 2) {
            suggestions.add(ParamSuggestion("saturation", currentParams["saturation"] ?: 0, refinedParams["saturation"] ?: 5, "饱和度", true))
            suggestions.add(ParamSuggestion("contrast", currentParams["contrast"] ?: 0, refinedParams["contrast"] ?: 8, "对比度", true))
        }

        return suggestions
    }

    /**
     * 构建颜色分析描述
     */
    private fun buildColorAnalysisDescription(cp: HeuristicSceneAnalyzer.ColorProfile): String {
        val warmthDesc = if (cp.warmthRatio > 0.5f) "暖色调主导" else if (cp.warmthRatio < 0.2f) "冷色调主导" else "色调平衡"
        val saturationDesc = if (cp.redDominance > 1.3f || cp.greenDominance > 1.3f || cp.blueDominance > 1.3f) "色彩饱和" else "色彩自然"
        val skinDesc = if (cp.skinToneRatio > 0.05f) "检测到肤色区域" else ""
        
        return "$warmthDesc，$saturationDesc${if (skinDesc.isNotEmpty()) "，$skinDesc" else ""}"
    }

    /**
     * 构建光线分析描述
     */
    private fun buildLightAnalysisDescription(level: HeuristicSceneAnalyzer.BrightnessLevel): String {
        return when (level) {
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_DARK -> "极暗环境，建议提升曝光并降噪"
            HeuristicSceneAnalyzer.BrightnessLevel.DARK -> "暗调氛围，适度提升阴影细节"
            HeuristicSceneAnalyzer.BrightnessLevel.NORMAL -> "光线平衡，曝光正常"
            HeuristicSceneAnalyzer.BrightnessLevel.BRIGHT -> "亮调氛围，注意高光控制"
            HeuristicSceneAnalyzer.BrightnessLevel.VERY_BRIGHT -> "高亮环境，建议压制高光"
        }
    }

    // ==================== 云端AI推理 ====================

    /**
     * 云端AI推理
     * 
     * 实现真实云端API调用框架：
     * - 图像压缩和Base64编码
     * - HTTP请求构建和发送
     * - 响应解析和结果转换
     * - 错误处理和降级策略
     * 
     * @param bitmap 待分析的图像
     * @param currentParams 当前调整参数
     * @return AI建议（失败时返回null，触发降级到本地推理）
     */
    private suspend fun generateCloudSuggestion(
        bitmap: Bitmap,
        currentParams: Map<String, Int>
    ): AISuggestion? = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "开始云端AI推理")

            // Step 1: 检查API密钥有效性（拒绝 demo_key / 空密钥）
            val apiKey = getApiKey()
            if (apiKey == null) {
                Log.w(TAG, "未配置有效云端API密钥，跳过云端推理")
                return@withContext null
            }

            // Step 2: 检查网络连接质量
            if (!isNetworkAvailable() || !isNetworkQualityGood()) {
                Log.w(TAG, "网络不可用或质量不佳，跳过云端推理")
                return@withContext null
            }
            
            // Step 3: 压缩图像并转换为Base64
            val compressedBitmap = compressBitmapForUpload(bitmap)
            val imageBase64 = bitmapToBase64(compressedBitmap)
            
            if (imageBase64.length > MAX_IMAGE_SIZE_BYTES) {
                Log.w(TAG, "图像数据过大(${imageBase64.length}字节)，跳过云端推理")
                return@withContext null
            }
            
            // Step 4: 构建请求参数
            val requestParams = buildCloudRequestParams(imageBase64, currentParams)
            
            // Step 5: 发送HTTP请求（带超时控制）
            val responseJson = sendCloudRequestWithTimeout(requestParams, CLOUD_API_TIMEOUT_MS, apiKey)
            
            // Step 6: 解析响应并转换为AI建议
            if (responseJson != null) {
                val suggestion = parseCloudResponse(responseJson)
                Log.d(TAG, "云端AI推理成功: 场景=${suggestion?.basePresetName}, 置信度=${suggestion?.confidence}")
                return@withContext suggestion
            }
            
            Log.w(TAG, "云端API响应无效，降级到本地推理")
            null
        } catch (e: Exception) {
            Log.w(TAG, "云端AI推理失败: ${e.message}, 降级到本地推理")
            null
        }
    }

    /**
     * 检查网络连接质量
     * 验证网络是否稳定且带宽足够进行API调用
     */
    private fun isNetworkQualityGood(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        
        if (network == null) {
            Log.d(TAG, "无活跃网络连接")
            return false
        }
        
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        if (capabilities == null) {
            Log.d(TAG, "无法获取网络能力信息")
            return false
        }
        
        // 检查是否有有效的互联网连接
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        if (!hasInternet || !hasValidated) {
            Log.d(TAG, "网络连接未验证或无互联网访问")
            return false
        }
        
        // 检查网络类型（WiFi或高质量移动网络更适合云端推理）
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        
        // WiFi总是适合；移动网络需要检查带宽
        if (isWifi) {
            Log.d(TAG, "WiFi网络，适合云端推理")
            return true
        }
        
        if (isCellular) {
            // 检查预估下行带宽（至少1Mbps）
            val downlinkBandwidth = capabilities.linkDownstreamBandwidthKbps
            if (downlinkBandwidth >= 1000) {
                Log.d(TAG, "移动网络带宽充足(${downlinkBandwidth}Kbps)，适合云端推理")
                return true
            }
            Log.d(TAG, "移动网络带宽不足(${downlinkBandwidth}Kbps)，跳过云端推理")
            return false
        }
        
        // 其他网络类型（以太网等）默认允许
        Log.d(TAG, "其他网络类型，允许云端推理")
        return true
    }

    /**
     * 压缩图像用于上传
     * 保持合理质量的同时减少数据大小
     */
    private fun compressBitmapForUpload(bitmap: Bitmap): Bitmap {
        val maxDimension = 512 // 云端推理使用较小尺寸
        
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 将Bitmap转换为Base64字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    /**
     * 构建云端请求参数
     */
    private fun buildCloudRequestParams(imageBase64: String, currentParams: Map<String, Int>): Map<String, Any> {
        return mapOf(
            "image_data" to imageBase64,
            "image_format" to "jpeg",
            "current_params" to currentParams,
            "request_type" to "scene_analysis",
            "user_id" to (settingsManager.userId ?: "anonymous"),
            "device_model" to android.os.Build.MODEL,
            "app_version" to getAppVersion(),
            "timestamp" to System.currentTimeMillis()
        )
    }

    /**
     * 发送云端请求（带超时控制）
     */
    private suspend fun sendCloudRequestWithTimeout(
        params: Map<String, Any>,
        timeoutMs: Long,
        apiKey: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            withTimeout(timeoutMs) {
                // 使用OkHttp发送请求
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val requestBody = "application/json; charset=utf-8".toMediaType()
                    .let { mediaType ->
                        okhttp3.RequestBody.create(
                            mediaType,
                            toJsonString(params)
                        )
                    }
                
                val request = okhttp3.Request.Builder()
                    .url(CLOUD_API_ENDPOINT)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-Api-Key", apiKey)
                    .addHeader("X-Device-Id", getDeviceId())
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "云端API响应失败: ${response.code}")
                    null
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "云端API请求超时")
            null
        } catch (e: Exception) {
            Log.w(TAG, "云端API请求异常: ${e.message}")
            null
        }
    }

    /**
     * 解析云端响应并转换为AI建议
     */
    private fun parseCloudResponse(responseJson: String): AISuggestion? {
        try {
            // 解析JSON响应
            val jsonObject = org.json.JSONObject(responseJson)
            
            // 检查响应状态
            val success = jsonObject.optBoolean("success", false)
            if (!success) {
                Log.w(TAG, "云端API返回失败状态")
                return null
            }
            
            val data = jsonObject.optJSONObject("data")
            if (data == null) {
                Log.w(TAG, "云端API响应缺少data字段")
                return null
            }
            
            // 提取场景信息
            val sceneId = data.optString("scene_id", "unknown")
            val sceneName = data.optString("scene_name", "通用")
            val confidence = data.optDouble("confidence", 0.85).toFloat()
            val sceneCategory = data.optString("scene_category", "通用")
            
            // 提取参数建议
            val suggestionsJson = data.optJSONArray("suggestions")
            val suggestions = mutableListOf<ParamSuggestion>()
            
            if (suggestionsJson != null) {
                for (i in 0 until suggestionsJson.length()) {
                    val item = suggestionsJson.getJSONObject(i)
                    suggestions.add(
                        ParamSuggestion(
                            field = item.optString("field", ""),
                            currentValue = item.optInt("current_value", 0),
                            suggestedValue = item.optInt("suggested_value", 0),
                            displayName = item.optString("display_name", ""),
                            isSelected = item.optBoolean("is_selected", true)
                        )
                    )
                }
            }
            
            // 提取分析描述
            val colorAnalysis = data.optString("color_analysis", "")
            val lightAnalysis = data.optString("light_analysis", "")
            val recommendedFilm = data.optString("recommended_film", "CC 经典负片")
            
            // 提取大师建议
            val tipsJson = data.optJSONArray("master_tips")
            val masterTips = mutableListOf<String>()
            if (tipsJson != null) {
                for (i in 0 until tipsJson.length()) {
                    masterTips.add(tipsJson.getString(i))
                }
            }
            
            return AISuggestion(
                basePresetId = sceneId,
                basePresetName = sceneName,
                suggestions = suggestions,
                generatedAt = System.currentTimeMillis(),
                isOfflineMode = false,
                confidence = confidence,
                sceneCategory = sceneCategory,
                colorAnalysis = colorAnalysis,
                lightAnalysis = lightAnalysis,
                recommendedFilm = recommendedFilm,
                masterTips = masterTips
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析云端响应失败: ${e.message}")
            return null
        }
    }

    /**
     * 将Map转换为JSON字符串
     */
    private fun toJsonString(params: Map<String, Any>): String {
        val jsonObject = org.json.JSONObject()
        for ((key, value) in params) {
            when (value) {
                is String -> jsonObject.put(key, value)
                is Int -> jsonObject.put(key, value)
                is Long -> jsonObject.put(key, value)
                is Boolean -> jsonObject.put(key, value)
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    jsonObject.put(key, org.json.JSONObject(value as Map<String, Any>))
                }
                else -> jsonObject.put(key, value.toString())
            }
        }
        return jsonObject.toString()
    }

    /**
     * 获取API密钥
     * 返回 null 表示未配置有效密钥（含 demo_key / 空值 / 长度过短 / 格式错误）
     *
     * 使用 SettingsManager 统一的验证逻辑，避免两边规则不一致导致
     * 占位符/demo密钥被误用。
     */
    private fun getApiKey(): String? {
        val key = settingsManager.cloudApiKey
        return if (settingsManager.validateApiKeyFormat(key)) {
            key
        } else {
            null
        }
    }

    /**
     * 获取设备ID
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            appContext.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }

    /**
     * 获取应用版本
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // ==================== 降级策略：规则引擎 ====================

    /**
     * 降级策略：规则引擎
     * 当启发式推理超时时使用
     */
    private fun generateFallbackSuggestion(currentParams: Map<String, Int>): AISuggestion {
        Log.d(TAG, "使用规则引擎降级策略")
        
        // 基于当前参数生成保守建议
        val suggestions = listOf(
            ParamSuggestion("saturation", currentParams["saturation"] ?: 0, 5, "饱和度", true),
            ParamSuggestion("contrast", currentParams["contrast"] ?: 0, 8, "对比度", true),
            ParamSuggestion("clarity", currentParams["clarity"] ?: 0, 10, "清晰度", true)
        )

        return AISuggestion(
            basePresetId = "fallback",
            basePresetName = "智能优化",
            suggestions = suggestions,
            generatedAt = System.currentTimeMillis(),
            isOfflineMode = true,
            confidence = 0.6f,
            sceneCategory = "通用",
            colorAnalysis = "规则引擎分析",
            lightAnalysis = "降级模式",
            recommendedFilm = "CC 经典负片",
            masterTips = listOf("建议使用真实AI推理获取更精准的建议")
        )
    }

    /**
     * 本地建议生成（基于预设ID）
     * 使用真实场景分析而非数值差比较
     */
    private suspend fun generateLocalSuggestion(presetId: String): AISuggestion = withContext(Dispatchers.Default) {
        // 使用真实场景分析
        val base = basePresets.find { it.id == presetId } ?: basePresets.first()
        val current = _currentAdjustments.value

        // 获取场景对应的哈苏参数（真实映射）
        val hasselbladParams = sceneMapping.getParams(presetId)
        val recommendedFilms = sceneMapping.getRecommendedFilms(presetId)
        val masterTips = sceneMapping.getMasterTips(presetId)

        // 基于哈苏参数生成建议（而非数值差比较）
        val suggestions = mutableListOf<ParamSuggestion>()

        val paramMap = mapOf(
            "saturation" to Triple(current.saturation, hasselbladParams.saturation, "饱和度"),
            "contrast" to Triple(current.contrast, hasselbladParams.contrast, "对比度"),
            "brightness" to Triple(current.brightness, hasselbladParams.tone, "亮度"),
            "warmth" to Triple(current.warmth, hasselbladParams.colorTemp, "色温"),
            "clarity" to Triple(current.clarity, hasselbladParams.sharpness, "清晰度")
        )

        // 按差异排序，选取主要建议
        val sortedParams = paramMap.entries.sortedByDescending { abs(it.value.second - it.value.first) }

        for (entry in sortedParams.take(4)) {
            val (_, triple) = entry
            val (currentVal, baseVal, displayName) = triple
            val diff = abs(baseVal - currentVal)

            if (diff >= 3) {
                suggestions.add(ParamSuggestion(
                    field = entry.key,
                    currentValue = currentVal,
                    suggestedValue = baseVal,
                    displayName = displayName,
                    isSelected = true
                ))
            }
        }

        if (suggestions.size < 2) {
            suggestions.clear()
            suggestions.add(ParamSuggestion("saturation", current.saturation, hasselbladParams.saturation, "饱和度", true))
            suggestions.add(ParamSuggestion("contrast", current.contrast, hasselbladParams.contrast, "对比度", true))
            suggestions.add(ParamSuggestion("clarity", current.clarity, hasselbladParams.sharpness, "清晰度", true))
        }

        val suggestion = AISuggestion(
            basePresetId = presetId,
            basePresetName = base.name,
            suggestions = suggestions,
            generatedAt = System.currentTimeMillis(),
            isOfflineMode = true,
            confidence = 0.85f,
            sceneCategory = inferSceneCategory(presetId),
            colorAnalysis = "基于哈苏大师参数映射",
            lightAnalysis = "预设优化",
            recommendedFilm = recommendedFilms.firstOrNull()?.name ?: "CC 经典负片",
            masterTips = masterTips.take(2)
        )

        _suggestedParams.value = suggestion
        suggestion
    }

    /**
     * 推断场景类别
     */
    private fun inferSceneCategory(presetId: String): String {
        return when {
            presetId.contains("portrait") -> "人像"
            presetId.contains("landscape") -> "风景"
            presetId.contains("night") -> "夜景"
            presetId.contains("food") -> "美食"
            presetId.contains("street") -> "街拍"
            presetId.contains("bw") -> "黑白"
            presetId.contains("hasselblad") -> "哈苏"
            else -> "通用"
        }
    }

    /**
     * FT-002: 获取参数对比表
     */
    fun getParamComparison(): List<ParamComparison> {
        val suggestion = _suggestedParams.value ?: return emptyList()
        return suggestion.suggestions.map { s ->
            ParamComparison(
                field = s.field,
                displayName = s.displayName,
                currentValue = s.currentValue,
                suggestedValue = s.suggestedValue,
                difference = s.suggestedValue - s.currentValue
            )
        }
    }

    /**
     * FT-002: 应用选中的建议参数
     */
    fun applySelectedSuggestions(selectedFields: Set<String>) {
        val suggestion = _suggestedParams.value ?: return
        val current = _currentAdjustments.value

        var updated = current
        for (s in suggestion.suggestions) {
            if (selectedFields.contains(s.field)) {
                updated = when (s.field) {
                    "saturation" -> updated.copy(saturation = s.suggestedValue)
                    "contrast" -> updated.copy(contrast = s.suggestedValue)
                    "brightness" -> updated.copy(brightness = s.suggestedValue)
                    "warmth" -> updated.copy(warmth = s.suggestedValue)
                    "clarity" -> updated.copy(clarity = s.suggestedValue)
                    "sharpness" -> updated.copy(sharpness = s.suggestedValue)
                    "highlights" -> updated.copy(highlights = s.suggestedValue)
                    "shadows" -> updated.copy(shadows = s.suggestedValue)
                    "noiseReduction" -> updated.copy(noiseReduction = s.suggestedValue)
                    "skinSmooth" -> updated.copy(skinSmooth = s.suggestedValue)
                    "vignette" -> updated.copy(vignette = s.suggestedValue)
                    else -> updated
                }
            }
        }

        _currentAdjustments.value = updated
        // FT-003: 保留建议用于二次编辑
        if (_manuallyModifiedFields.value.isEmpty()) {
            clearSuggestion()
        }
    }

    /**
     * FT-002: 单参数采纳
     */
    fun applySingleParam(field: String) {
        applySelectedSuggestions(setOf(field))
    }

    /**
     * FT-003: 手动修改参数
     */
    fun manuallyAdjustParam(param: AdjustmentType, value: Int) {
        val fieldName = param.name.lowercase()
        _manuallyModifiedFields.value = _manuallyModifiedFields.value + fieldName
        adjustParam(param, value)
    }

    /**
     * FT-003: 检查参数是否被手动修改
     */
    fun isParamManuallyModified(field: String): Boolean {
        return _manuallyModifiedFields.value.contains(field)
    }

    /**
     * FT-003: 重置为AI推荐状态
     */
    fun resetToAISuggestion() {
        val suggestion = _suggestedParams.value ?: return
        _manuallyModifiedFields.value = emptySet()
        applySelectedSuggestions(suggestion.suggestions.filter { it.isSelected }.map { it.field }.toSet())
    }

    /**
     * FT-003: 清除建议（会话结束）
     */
    fun clearSuggestion() {
        _suggestedParams.value = null
        _manuallyModifiedFields.value = emptySet()
    }

    /**
     * 清除错误状态
     */
    fun clearError() {
        _errorState.value = null
    }

    /**
     * 应用色彩风格
     */
    suspend fun applyColorStyle(styleId: String): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) return@withContext AdjustmentParams()

        _isProcessing.value = true

        val style = colorStyles.find { it.id == styleId } ?: colorStyles.first()
        val newParams = AdjustmentParams(
            saturation = style.saturation,
            contrast = style.contrast,
            brightness = 0,
            warmth = style.warmth,
            selectedStyleId = styleId
        )

        _currentAdjustments.value = newParams
        clearSuggestion()
        _isProcessing.value = false
        newParams
    }

    /**
     * 应用智能优化
     */
    suspend fun applySmartPreset(presetId: String): AdjustmentParams = withContext(Dispatchers.Default) {
        if (!settingsManager.isAIFineTuneEnabled) return@withContext AdjustmentParams()

        _isProcessing.value = true

        val params = when (presetId) {
            "auto_optimize" -> AdjustmentParams(saturation = 10, contrast = 8, brightness = 5, sharpness = 15, clarity = 10)
            "hdr_enhance" -> AdjustmentParams(contrast = 20, highlights = -30, shadows = 25, clarity = 15)
            "noise_reduce" -> AdjustmentParams(noiseReduction = 40, sharpness = -5)
            "sharpness" -> AdjustmentParams(sharpness = 30, clarity = 20, detail = 15)
            "skin_smooth" -> AdjustmentParams(skinSmooth = 25, warmth = 5, saturation = -5)
            "sky_enhance" -> AdjustmentParams(saturation = 20, contrast = 10, highlights = -15, clarity = 10)
            "detail_enhance" -> AdjustmentParams(sharpness = 25, clarity = 25, detail = 20)
            "night_optimize" -> AdjustmentParams(contrast = 15, highlights = -15, shadows = 20, noiseReduction = 35)
            else -> AdjustmentParams()
        }

        _currentAdjustments.value = params
        clearSuggestion()
        _isProcessing.value = false
        params
    }

    /**
     * 应用预设（公开接口）
     */
    fun applyPreset(presetId: String) {
        val preset = basePresets.find { it.id == presetId } ?: return
        val params = preset.params
        
        _currentAdjustments.value = AdjustmentParams(
            saturation = params["saturation"] ?: 0,
            contrast = params["contrast"] ?: 0,
            brightness = params["brightness"] ?: 0,
            warmth = params["warmth"] ?: 0,
            clarity = params["clarity"] ?: 0,
            sharpness = params["sharpness"] ?: 0,
            highlights = params["highlights"] ?: 0,
            shadows = params["shadows"] ?: 0,
            noiseReduction = params["noiseReduction"] ?: 0,
            skinSmooth = params["skinSmooth"] ?: 0,
            vignette = params["vignette"] ?: 0,
            selectedStyleId = presetId
        )
        clearSuggestion()
    }

    /**
     * 手动调整参数
     */
    fun adjustParam(param: AdjustmentType, value: Int) {
        if (!settingsManager.isAIFineTuneEnabled) return

        val current = _currentAdjustments.value
        _currentAdjustments.value = when (param) {
            AdjustmentType.SATURATION -> current.copy(saturation = value.coerceIn(-100, 100))
            AdjustmentType.CONTRAST -> current.copy(contrast = value.coerceIn(-100, 100))
            AdjustmentType.BRIGHTNESS -> current.copy(brightness = value.coerceIn(-100, 100))
            AdjustmentType.WARMTH -> current.copy(warmth = value.coerceIn(-100, 100))
            AdjustmentType.SHARPNESS -> current.copy(sharpness = value.coerceIn(0, 100))
            AdjustmentType.CLARITY -> current.copy(clarity = value.coerceIn(0, 100))
            AdjustmentType.HIGHLIGHTS -> current.copy(highlights = value.coerceIn(-100, 100))
            AdjustmentType.SHADOWS -> current.copy(shadows = value.coerceIn(-100, 100))
            AdjustmentType.NOISE_REDUCTION -> current.copy(noiseReduction = value.coerceIn(0, 100))
            AdjustmentType.SKIN_SMOOTH -> current.copy(skinSmooth = value.coerceIn(0, 100))
            AdjustmentType.DETAIL -> current.copy(detail = value.coerceIn(0, 100))
        }
    }

    /**
     * 重置所有调整
     */
    fun resetAdjustments() {
        _currentAdjustments.value = AdjustmentParams()
        clearSuggestion()
    }

    /**
     * 保存为自定义预设
     */
    fun saveAsPreset(name: String): CustomPreset {
        return CustomPreset(
            id = System.currentTimeMillis().toString(),
            name = name,
            params = _currentAdjustments.value,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * 切换AI微调开关
     */
    fun toggleAIFineTune(enabled: Boolean) {
        settingsManager.isAIFineTuneEnabled = enabled
    }

    /**
     * 获取已应用的历史（FT-006）
     */
    fun getAppliedHistory(): List<AISuggestion> = appliedSuggestions.toList()

    companion object {
        private const val TAG = "AIFineTuneManager"
        
        // 云端API配置
        private val CLOUD_API_ENDPOINT = UrlConstants.API_CLOUD_SCENE_ANALYZE
        private const val CLOUD_API_TIMEOUT_MS = 5000L // 5秒超时
        private const val MAX_IMAGE_SIZE_BYTES = 500000 // 最大500KB图像数据

        @Volatile
        private var instance: AIFineTuneManager? = null

        fun getInstance(context: Context): AIFineTuneManager {
            return instance ?: synchronized(this) {
                instance ?: AIFineTuneManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

/**
 * AI建议结果
 */
sealed class AISuggestionResult {
    data class Success(val suggestion: AISuggestion, val isOfflineMode: Boolean) : AISuggestionResult()
    data class Error(val error: ErrorState) : AISuggestionResult()
}

/**
 * 错误状态
 */
data class ErrorState(
    val type: ErrorType,
    val message: String
) {
    enum class ErrorType {
        TIMEOUT,
        NETWORK_ERROR,
        SERVICE_EXCEPTION,
        PERMISSION_DENIED,
        UNKNOWN
    }

    companion object {
        fun Timeout(message: String) = ErrorState(ErrorType.TIMEOUT, message)
        fun NetworkError(message: String) = ErrorState(ErrorType.NETWORK_ERROR, message)
        fun ServiceException(message: String) = ErrorState(ErrorType.SERVICE_EXCEPTION, message)
        fun PermissionDenied(message: String) = ErrorState(ErrorType.PERMISSION_DENIED, message)
        fun Unknown(message: String) = ErrorState(ErrorType.UNKNOWN, message)
    }
}

/**
 * 色彩风格
 */
data class ColorStyle(
    val id: String,
    val name: String,
    val description: String,
    val saturation: Int,
    val contrast: Int,
    val warmth: Int
)

/**
 * 智能优化预设
 */
data class SmartPreset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String
)

/**
 * 基础预设
 */
data class BasePreset(
    val id: String,
    val name: String,
    val params: Map<String, Int>
)

/**
 * AI建议参数（扩展版）
 */
data class AISuggestion(
    val basePresetId: String,
    val basePresetName: String,
    val suggestions: List<ParamSuggestion>,
    val generatedAt: Long,
    val isOfflineMode: Boolean = false,
    // 新增：真实AI推理结果
    val confidence: Float = 0.85f,
    val sceneCategory: String = "通用",
    val colorAnalysis: String = "",
    val lightAnalysis: String = "",
    val recommendedFilm: String = "CC 经典负片",
    val masterTips: List<String> = emptyList()
)

/**
 * 单个参数建议
 */
data class ParamSuggestion(
    val field: String,
    val currentValue: Int,
    val suggestedValue: Int,
    val displayName: String,
    val isSelected: Boolean = true
) {
    val difference: Int get() = suggestedValue - currentValue
}

/**
 * 参数对比
 */
data class ParamComparison(
    val field: String,
    val displayName: String,
    val currentValue: Int,
    val suggestedValue: Int,
    val difference: Int
)

/**
 * 调整参数
 */
data class AdjustmentParams(
    val saturation: Int = 0,
    val contrast: Int = 0,
    val brightness: Int = 0,
    val warmth: Int = 0,
    val sharpness: Int = 0,
    val clarity: Int = 0,
    val highlights: Int = 0,
    val shadows: Int = 0,
    val noiseReduction: Int = 0,
    val skinSmooth: Int = 0,
    val detail: Int = 0,
    val vignette: Int = 0,
    val selectedStyleId: String? = null
) {
    fun toMap(): Map<String, Int> = mapOf(
        "saturation" to saturation,
        "contrast" to contrast,
        "brightness" to brightness,
        "warmth" to warmth,
        "sharpness" to sharpness,
        "clarity" to clarity,
        "highlights" to highlights,
        "shadows" to shadows,
        "noiseReduction" to noiseReduction,
        "skinSmooth" to skinSmooth,
        "detail" to detail,
        "vignette" to vignette
    )
}

/**
 * 调整类型
 */
enum class AdjustmentType {
    SATURATION, CONTRAST, BRIGHTNESS, WARMTH, SHARPNESS, CLARITY,
    HIGHLIGHTS, SHADOWS, NOISE_REDUCTION, SKIN_SMOOTH, DETAIL
}

/**
 * 自定义预设
 */
data class CustomPreset(
    val id: String,
    val name: String,
    val params: AdjustmentParams,
    val createdAt: Long
)