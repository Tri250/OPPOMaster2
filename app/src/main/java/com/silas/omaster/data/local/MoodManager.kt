package com.silas.omaster.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 【v1.6.14 发现 2.0】ColorMood 色彩心情管理器
 * 每日记录用户心情，心情映射专属色系渐变与影像语义关键词，
 * 用于在预设库中匹配同情绪气质的灵感预设。
 */
class MoodManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // 宽容解码，兼容历史数据演进
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _moodsFlow = MutableStateFlow(loadMoods())
    val moodsFlow: StateFlow<Map<String, String>> = _moodsFlow.asStateFlow()

    /** 今日心情（未选择返回 null） */
    fun getTodayMood(): Mood? {
        val today = LocalDate.now().format(DATE_FORMATTER)
        return prefs.getString(KEY_MOODS, null)
            ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrDefault(emptyMap()) }
            ?.get(today)
            ?.let { Mood.byId(it) }
    }

    /** 指定日期心情（月历视图用） */
    fun getMoodForDate(date: LocalDate): Mood? {
        return _moodsFlow.value[date.format(DATE_FORMATTER)]?.let { Mood.byId(it) }
    }

    /** 记录今日心情（同日重复选择为覆盖） */
    fun setTodayMood(mood: Mood) {
        val today = LocalDate.now().format(DATE_FORMATTER)
        val moods = _moodsFlow.value.toMutableMap()
        moods[today] = mood.id
        val raw = json.encodeToString(moods)
        prefs.edit().putString(KEY_MOODS, raw).apply()
        _moodsFlow.value = moods
    }

    /** 本月已记录心情数（月统计用） */
    fun getMonthlyMoodCount(year: Int, month: Int): Int {
        val prefix = "$year-${month.toString().padStart(2, '0')}"
        return _moodsFlow.value.count { it.key.startsWith(prefix) }
    }

    private fun loadMoods(): Map<String, String> {
        val raw = prefs.getString(KEY_MOODS, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())
    }

    companion object {
        private const val PREFS_NAME = "color_mood_prefs"
        private const val KEY_MOODS = "moods"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}

/**
 * 心情枚举：专属渐变 + 色温倾向 + 影像关键词（用于预设灵感匹配）
 * @param warmBias 暖冷倾向，正值偏暖、负值偏冷，用于与预设 warmCool 字段匹配
 * @param keywords 影像语义关键词，用于在预设 name / tags 中匹配
 */
@Serializable
enum class Mood(
    val id: String,
    val gradient: List<Long>,
    val warmBias: Int,
    val keywords: List<String>
) {
    JOYFUL("joyful", listOf(0xFFFF9A56, 0xFFFFAD7F), 60, listOf("人像", "portr", "日落", "sunset", "花", "flower")),
    CALM("calm", listOf(0xFF4FACFE, 0xFF00F2FE), -60, listOf("海", "sea", "天空", "sky", "蓝调", "blue")),
    MELANCHOLY("melancholy", listOf(0xFF7F7FD5, 0xFF91EAE4), -40, listOf("夜景", "night", "雨", "rain", "街拍", "street")),
    ROMANTIC("romantic", listOf(0xFFFF758C, 0xFFC79081), 40, listOf("人像", "portr", "花", "flower", "晚霞", "glow")),
    ENERGETIC("energetic", listOf(0xFFF7971E, 0xFFFFD200), 80, listOf("街拍", "street", "运动", "sport", "阳光", "sunny")),
    SERENE("serene", listOf(0xFF11998E, 0xFF38EF7D), 0, listOf("风光", "landscap", "森林", "forest", "绿", "green"));

    companion object {
        fun byId(id: String): Mood? = entries.firstOrNull { it.id == id }
    }
}
