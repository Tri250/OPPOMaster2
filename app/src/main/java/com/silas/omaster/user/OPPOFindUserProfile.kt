package com.silas.omaster.user

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * OPPO Find摄影用户画像
 * 针对OPPO Find系列用户的专业摄影需求优化
 */
class OPPOFindUserProfile private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    // 设备信息
    private val _deviceModel = MutableStateFlow(getDeviceModel())
    val deviceModel: StateFlow<String> = _deviceModel.asStateFlow()
    
    // 用户偏好
    private val _userPreferences = MutableStateFlow(loadUserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()
    
    // 使用统计
    private val _usageStats = MutableStateFlow(loadUsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()
    
    /**
     * 获取设备型号
     * 识别OPPO Find系列设备
     */
    private fun getDeviceModel(): String {
        val model = Build.MODEL
        return when {
            model.contains("Find X8", ignoreCase = true) -> "OPPO Find X8 Pro"
            model.contains("Find X7", ignoreCase = true) -> "OPPO Find X7 Ultra"
            model.contains("Find X6", ignoreCase = true) -> "OPPO Find X6 Pro"
            model.contains("Find N3", ignoreCase = true) -> "OPPO Find N3"
            model.contains("Find N2", ignoreCase = true) -> "OPPO Find N2"
            model.contains("OnePlus 12", ignoreCase = true) -> "OnePlus 12"
            model.contains("OnePlus 11", ignoreCase = true) -> "OnePlus 11"
            else -> model
        }
    }
    
    /**
     * 检查是否为OPPO Find系列
     */
    fun isOPPOFindDevice(): Boolean {
        return deviceModel.value.contains("Find", ignoreCase = true) ||
               deviceModel.value.contains("OnePlus", ignoreCase = true)
    }
    
    /**
     * 获取推荐预设（基于用户画像）
     */
    fun getRecommendedPresets(): List<String> {
        val preferences = _userPreferences.value
        val stats = _usageStats.value
        
        val recommendations = mutableListOf<String>()
        
        // 根据常用场景推荐
        val topScenes = stats.sceneUsage.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
        
        topScenes.forEach { scene ->
            when (scene) {
                "portrait" -> recommendations.addAll(listOf("人像柔美", "哈苏人像", "清新人文"))
                "landscape" -> recommendations.addAll(listOf("风景鲜明", "哈苏浓郁", "理光绿"))
                "night" -> recommendations.addAll(listOf("夜景氛围", "霓虹灯", "蓝调时刻"))
                "food" -> recommendations.addAll(listOf("美味流芳", "美食暖调", "美味梦境"))
                "street" -> recommendations.addAll(listOf("街拍快照", "人文", "手机徕卡"))
            }
        }
        
        // 根据色彩偏好推荐
        when (preferences.preferredColorStyle) {
            ColorStyle.FILM -> recommendations.addAll(listOf("富士胶片", "胶片感", "复古怀旧"))
            ColorStyle.VIVID -> recommendations.addAll(listOf("哈苏浓郁", "赛博朋克", "风景鲜明"))
            ColorStyle.NATURAL -> recommendations.addAll(listOf("哈苏自然", "清新人文", "理光蓝"))
        }
        
        return recommendations.distinct()
    }
    
    /**
     * 获取推荐参数（基于设备特性）
     */
    fun getRecommendedParams(): Map<String, String> {
        return when (_deviceModel.value) {
            "OPPO Find X8 Pro" -> mapOf(
                "iso" to "50-400",
                "aperture" to "f/1.6-f/4.0",
                "features" to "哈苏色彩, 潜望长焦, AI消除"
            )
            "OPPO Find X7 Ultra" -> mapOf(
                "iso" to "50-800",
                "aperture" to "f/1.8-f/4.0",
                "features" to "双潜望, 哈苏色彩"
            )
            "OnePlus 12" -> mapOf(
                "iso" to "50-400",
                "aperture" to "f/1.6-f/2.6",
                "features" to "哈苏影像, 潜望长焦"
            )
            else -> mapOf(
                "iso" to "自动",
                "aperture" to "自动",
                "features" to "标准模式"
            )
        }
    }
    
    /**
     * 更新用户偏好
     */
    fun updatePreferences(preferences: UserPreferences) {
        _userPreferences.value = preferences
        saveUserPreferences(preferences)
    }
    
    /**
     * 记录场景使用
     */
    fun recordSceneUsage(sceneType: String) {
        val stats = _usageStats.value
        val currentCount = stats.sceneUsage[sceneType] ?: 0
        val newStats = stats.copy(
            sceneUsage = stats.sceneUsage + (sceneType to currentCount + 1),
            totalEdits = stats.totalEdits + 1
        )
        _usageStats.value = newStats
        saveUsageStats(newStats)
    }
    
    /**
     * 记录预设应用
     */
    fun recordPresetApplied(presetName: String) {
        val stats = _usageStats.value
        val currentCount = stats.presetUsage[presetName] ?: 0
        val newStats = stats.copy(
            presetUsage = stats.presetUsage + (presetName to currentCount + 1)
        )
        _usageStats.value = newStats
        saveUsageStats(newStats)
    }
    
    /**
     * 获取个性化首页配置
     */
    fun getHomePageConfig(): HomePageConfig {
        val preferences = _userPreferences.value
        val stats = _usageStats.value
        
        return HomePageConfig(
            // 根据使用频率决定默认Tab
            defaultTab = when {
                stats.presetUsage.isEmpty() -> 0 // 新用户显示全部
                stats.presetUsage.values.sum() > 50 -> 1 // 老用户显示收藏
                else -> 0
            },
            // 推荐预设
            recommendedPresets = getRecommendedPresets().take(6),
            // 快捷功能（根据使用习惯排序）
            quickFeatures = getQuickFeaturesByUsage(),
            // 显示云同步提示
            showCloudSyncTip = !preferences.hasSyncedCloud
        )
    }
    
    /**
     * 根据使用习惯排序快捷功能
     */
    private fun getQuickFeaturesByUsage(): List<String> {
        val stats = _usageStats.value
        val featureUsage = stats.featureUsage
        
        val allFeatures = listOf(
            "ai_scene", "ai_finetune", "watermark", 
            "smart_optimize", "preset_manager", "param_adjust"
        )
        
        return allFeatures.sortedByDescending { 
            featureUsage[it] ?: 0 
        }
    }
    
    /**
     * 加载用户偏好
     */
    private fun loadUserPreferences(): UserPreferences {
        val json = prefs.getString(KEY_USER_PREFERENCES, null)
        return if (json != null) {
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                UserPreferences()
            }
        } else {
            UserPreferences()
        }
    }
    
    /**
     * 保存用户偏好
     */
    private fun saveUserPreferences(preferences: UserPreferences) {
        val json = Json.encodeToString(preferences)
        prefs.edit().putString(KEY_USER_PREFERENCES, json).apply()
    }
    
    /**
     * 加载使用统计
     */
    private fun loadUsageStats(): UsageStats {
        val json = prefs.getString(KEY_USAGE_STATS, null)
        return if (json != null) {
            try {
                Json.decodeFromString(json)
            } catch (e: Exception) {
                UsageStats()
            }
        } else {
            UsageStats()
        }
    }
    
    /**
     * 保存使用统计
     */
    private fun saveUsageStats(stats: UsageStats) {
        val json = Json.encodeToString(stats)
        prefs.edit().putString(KEY_USAGE_STATS, json).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "oppo_find_user_profile"
        private const val KEY_USER_PREFERENCES = "user_preferences"
        private const val KEY_USAGE_STATS = "usage_stats"
        
        @Volatile
        private var instance: OPPOFindUserProfile? = null
        
        fun getInstance(context: Context): OPPOFindUserProfile {
            return instance ?: synchronized(this) {
                instance ?: OPPOFindUserProfile(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

/**
 * 用户偏好数据类
 */
@Serializable
data class UserPreferences(
    val preferredColorStyle: ColorStyle = ColorStyle.NATURAL,
    val preferredMode: String = "auto", // auto, pro
    val autoApplyWatermark: Boolean = true,
    val watermarkStyle: String = "hasselblad_official",
    val hasSyncedCloud: Boolean = false,
    val enableAIRecommendation: Boolean = true,
    val preferredExportFormat: String = "jpg", // jpg, png
    val exportQuality: Int = 95
)

/**
 * 色彩风格偏好
 */
enum class ColorStyle {
    NATURAL,    // 自然
    VIVID,      // 鲜艳
    FILM,       // 胶片
    MONO        // 黑白
}

/**
 * 使用统计数据类
 */
@Serializable
data class UsageStats(
    val sceneUsage: Map<String, Int> = emptyMap(),
    val presetUsage: Map<String, Int> = emptyMap(),
    val featureUsage: Map<String, Int> = emptyMap(),
    val totalEdits: Int = 0,
    val firstUseTime: Long = System.currentTimeMillis(),
    val lastUseTime: Long = System.currentTimeMillis()
)

/**
 * 首页配置
 */
data class HomePageConfig(
    val defaultTab: Int,
    val recommendedPresets: List<String>,
    val quickFeatures: List<String>,
    val showCloudSyncTip: Boolean
)