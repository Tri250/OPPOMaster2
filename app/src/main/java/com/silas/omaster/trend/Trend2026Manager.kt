package com.silas.omaster.trend

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 2026 趋势专题管理器
 * 小红书同款引流
 * 
 * 整合 2026 年流行摄影风格：
 * - 氧气感、莫兰迪、桂花黄、柯达金
 * - 赛博霓虹、经典黑白、日系清新、老钱风
 */
class Trend2026Manager private constructor(context: Context) {

    // 2026 趋势风格
    data class TrendStyle(
        val id: String,
        val name: String,
        val displayName: String,
        val description: String,
        val icon: String,
        val color: String,
        val params: TrendParams,
        val xiaohongshuTag: String,      // 小红书标签
        val xiaohongshuCount: String,     // 小红书笔记数
        val sampleImages: List<String> = emptyList(),
        val tips: List<String> = emptyList(),
        val suitableScenes: List<String> = emptyList()
    )

    // 趋势参数
    data class TrendParams(
        val saturation: Int = 0,
        val contrast: Int = 0,
        val brightness: Int = 0,
        val warmth: Int = 0,
        val sharpness: Int = 0,
        val clarity: Int = 0,
        val highlights: Int = 0,
        val shadows: Int = 0,
        val vibrance: Int = 0,
        val hslAdjustments: Map<String, HSLAdjust> = emptyMap()
    )

    data class HSLAdjust(
        val hue: Int = 0,
        val saturation: Int = 0,
        val lightness: Int = 0
    )

    // 2026 年 8 大流行风格
    val trendStyles = listOf(
        // 氧气感 - 2026 最火风格
        TrendStyle(
            id = "trend_oxygen",
            name = "oxygen",
            displayName = "氧气感",
            description = "通透清新，自带呼吸感的画面",
            icon = "💨",
            color = "#87CEEB",
            params = TrendParams(
                saturation = 8,
                contrast = -5,
                brightness = 15,
                warmth = -8,
                sharpness = 10,
                clarity = 12,
                highlights = -15,
                shadows = 10,
                vibrance = 15,
                hslAdjustments = mapOf(
                    "cyan" to HSLAdjust(saturation = 20, lightness = 10),
                    "blue" to HSLAdjust(saturation = 15, lightness = 5)
                )
            ),
            xiaohongshuTag = "氧气感照片",
            xiaohongshuCount = "520w+",
            tips = listOf(
                "适当降低对比度增加通透感",
                "冷色调偏青蓝更出片",
                "高光压暗保留天空细节"
            ),
            suitableScenes = listOf("人像", "风景", "日常", "旅行")
        ),

        // 莫兰迪 - 经典高级灰
        TrendStyle(
            id = "trend_morandi",
            name = "morandi",
            displayName = "莫兰迪",
            description = "低饱和高级灰，艺术感满满",
            icon = "🎨",
            color = "#C4B7A6",
            params = TrendParams(
                saturation = -25,
                contrast = 5,
                brightness = 5,
                warmth = 10,
                sharpness = 8,
                clarity = 10,
                highlights = -10,
                shadows = 5,
                vibrance = -10
            ),
            xiaohongshuTag = "莫兰迪色调",
            xiaohongshuCount = "380w+",
            tips = listOf(
                "降低饱和度是关键",
                "偏暖灰更有质感",
                "适合静物和室内"
            ),
            suitableScenes = listOf("静物", "室内", "人像", "建筑")
        ),

        // 桂花黄 - 秋日暖调
        TrendStyle(
            id = "trend_osmanthus",
            name = "osmanthus",
            displayName = "桂花黄",
            description = "温暖秋日色调，治愈系首选",
            icon = "🍂",
            color = "#DAA520",
            params = TrendParams(
                saturation = 15,
                contrast = 8,
                brightness = 5,
                warmth = 25,
                sharpness = 12,
                clarity = 15,
                highlights = -5,
                shadows = 15,
                vibrance = 10,
                hslAdjustments = mapOf(
                    "yellow" to HSLAdjust(saturation = 20, lightness = 5),
                    "orange" to HSLAdjust(saturation = 15, lightness = 10)
                )
            ),
            xiaohongshuTag = "桂花黄调色",
            xiaohongshuCount = "290w+",
            tips = listOf(
                "暖色调偏黄橙",
                "阴影提亮增加温柔感",
                "适合秋景和人像"
            ),
            suitableScenes = listOf("秋景", "人像", "街拍", "美食")
        ),

        // 柯达金 - 复古胶片
        TrendStyle(
            id = "trend_kodak_gold",
            name = "kodak_gold",
            displayName = "柯达金",
            description = "经典柯达金胶片质感",
            icon = "🎞️",
            color = "#CD853F",
            params = TrendParams(
                saturation = 12,
                contrast = 15,
                brightness = 0,
                warmth = 20,
                sharpness = 8,
                clarity = 10,
                highlights = -20,
                shadows = 10,
                vibrance = 5
            ),
            xiaohongshuTag = "柯达金胶片",
            xiaohongshuCount = "450w+",
            tips = listOf(
                "高光偏暖黄",
                "对比度稍高",
                "暗角增加胶片感"
            ),
            suitableScenes = listOf("街拍", "人像", "风景", "旅行")
        ),

        // 赛博霓虹 - 赛博朋克风
        TrendStyle(
            id = "trend_cyber_neon",
            name = "cyber_neon",
            displayName = "赛博霓虹",
            description = "赛博朋克霓虹灯效果",
            icon = "🌃",
            color = "#FF00FF",
            params = TrendParams(
                saturation = 35,
                contrast = 25,
                brightness = -10,
                warmth = -15,
                sharpness = 20,
                clarity = 25,
                highlights = 20,
                shadows = -20,
                vibrance = 30,
                hslAdjustments = mapOf(
                    "blue" to HSLAdjust(hue = 15, saturation = 30),
                    "magenta" to HSLAdjust(saturation = 40, lightness = 10)
                )
            ),
            xiaohongshuTag = "赛博朋克调色",
            xiaohongshuCount = "180w+",
            tips = listOf(
                "高对比度是关键",
                "偏青品色调",
                "适合夜景和室内"
            ),
            suitableScenes = listOf("夜景", "室内", "街拍", "人像")
        ),

        // 经典黑白 - 永不过时
        TrendStyle(
            id = "trend_classic_bw",
            name = "classic_bw",
            displayName = "经典黑白",
            description = "高质感黑白影调",
            icon = "⬛",
            color = "#2C2C2C",
            params = TrendParams(
                saturation = -100,
                contrast = 20,
                brightness = 5,
                warmth = 0,
                sharpness = 15,
                clarity = 20,
                highlights = -15,
                shadows = 10,
                vibrance = 0
            ),
            xiaohongshuTag = "黑白摄影",
            xiaohongshuCount = "620w+",
            tips = listOf(
                "注意光影层次",
                "对比度决定风格",
                "适合有纹理的场景"
            ),
            suitableScenes = listOf("街拍", "人像", "建筑", "纪实")
        ),

        // 日系清新 - 少女感
        TrendStyle(
            id = "trend_japanese_fresh",
            name = "japanese_fresh",
            displayName = "日系清新",
            description = "日系小清新少女感",
            icon = "🌸",
            color = "#FFB6C1",
            params = TrendParams(
                saturation = -5,
                contrast = -10,
                brightness = 20,
                warmth = 5,
                sharpness = 5,
                clarity = 8,
                highlights = -20,
                shadows = 15,
                vibrance = 10,
                hslAdjustments = mapOf(
                    "red" to HSLAdjust(saturation = -10, lightness = 15),
                    "magenta" to HSLAdjust(saturation = 10, lightness = 10)
                )
            ),
            xiaohongshuTag = "日系小清新",
            xiaohongshuCount = "780w+",
            tips = listOf(
                "低对比度增加柔和感",
                "高亮度通透画面",
                "偏粉色调更少女"
            ),
            suitableScenes = listOf("人像", "日常", "风景", "美食")
        ),

        // 老钱风 - 高级质感
        TrendStyle(
            id = "trend_old_money",
            name = "old_money",
            displayName = "老钱风",
            description = "低调奢华高级质感",
            icon = "💎",
            color = "#8B7355",
            params = TrendParams(
                saturation = -15,
                contrast = 10,
                brightness = 0,
                warmth = 15,
                sharpness = 12,
                clarity = 18,
                highlights = -10,
                shadows = 5,
                vibrance = -5
            ),
            xiaohongshuTag = "老钱风调色",
            xiaohongshuCount = "210w+",
            tips = listOf(
                "低饱和高级感",
                "偏暖棕色调",
                "清晰度增加质感"
            ),
            suitableScenes = listOf("人像", "室内", "静物", "街拍")
        )
    )

    // 当前选中的趋势
    private val _selectedTrend = MutableStateFlow<TrendStyle?>(null)
    val selectedTrend: StateFlow<TrendStyle?> = _selectedTrend.asStateFlow()

    // 热门趋势排行（根据小红书数据）
    val hotTrends = trendStyles.sortedByDescending { 
        it.xiaohongshuCount.replace("w+", "").toFloatOrNull() ?: 0f 
    }

    /**
     * 选择趋势风格
     */
    fun selectTrend(trend: TrendStyle) {
        _selectedTrend.value = trend
    }

    /**
     * 根据场景推荐趋势
     */
    fun recommendByScene(scene: String): List<TrendStyle> {
        return trendStyles.filter { 
            it.suitableScenes.any { s -> s.contains(scene, ignoreCase = true) }
        }
    }

    /**
     * 搜索趋势
     */
    fun searchTrend(query: String): List<TrendStyle> {
        if (query.isBlank()) return trendStyles
        val lowerQuery = query.lowercase()
        return trendStyles.filter {
            it.displayName.contains(query) ||
            it.name.contains(lowerQuery) ||
            it.description.contains(query) ||
            it.xiaohongshuTag.contains(query)
        }
    }

    /**
     * 获取趋势参数
     */
    fun getTrendParams(trendId: String): TrendParams? {
        return trendStyles.find { it.id == trendId }?.params
    }

    companion object {
        @Volatile
        private var instance: Trend2026Manager? = null

        fun getInstance(context: Context): Trend2026Manager {
            return instance ?: synchronized(this) {
                instance ?: Trend2026Manager(context.applicationContext).also { instance = it }
            }
        }
    }
}
