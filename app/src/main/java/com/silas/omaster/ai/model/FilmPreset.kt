package com.silas.omaster.ai.model

import kotlinx.serialization.Serializable

/**
 * 胶片系列分类
 * 对齐 OPPO 9 款原生胶片的四大系列
 */
enum class FilmSeries(val displayName: String, val description: String) {
    CLASSIC("原生经典", "经典胶片风格，色彩浓郁复古"),
    EMOTION("情绪与表达", "情感化表达，柔美梦幻"),
    STRUCTURE("结构与时间", "结构感强，时间沉淀感"),
    DIGITAL("数字记忆", "数码 CCD 风格，冷暖调")
}

/**
 * 胶片预设
 * 对齐 OPPO 9 款原生胶片风格
 */
@Serializable
data class FilmPreset(
    val id: String,              // "portra", "cc", "nc", "nh", "rdp3", "800t", "tx400", "ccd_cool", "ccd_warm"
    val name: String,            // "Portra 400", "CC 经典负片"
    val displayName: String,     // 显示名称（国际化）
    val series: FilmSeries,      // 所属系列
    val matchScore: Float,       // 场景匹配度 0-1
    val description: String,     // 胶片风格描述
    val colorCharacteristics: String, // 色彩特征
    val bestFor: List<String>    // 最佳适用场景
) {
    /**
     * 获取匹配度百分比
     */
    val matchPercent: Int get() = (matchScore * 100).toInt()

    /**
     * 是否高匹配度（> 70%）
     */
    val isHighMatch: Boolean get() = matchScore >= 0.7f

    companion object {
        /**
         * OPPO 9 款原生胶片预设
         */
        val ALL_PRESETS = listOf(
            // 原生经典系列
            FilmPreset(
                id = "cc",
                name = "CC",
                displayName = "CC 经典负片",
                series = FilmSeries.CLASSIC,
                matchScore = 0f,
                description = "经典负片风格，色彩浓郁复古",
                colorCharacteristics = "浓郁暖调，高对比度",
                bestFor = listOf("街拍", "人像", "风景", "建筑")
            ),
            FilmPreset(
                id = "nc",
                name = "NC",
                displayName = "富士 NC",
                series = FilmSeries.CLASSIC,
                matchScore = 0f,
                description = "富士经典负片，柔和自然",
                colorCharacteristics = "柔和自然，日系风格",
                bestFor = listOf("人像", "日常", "旅行", "日系")
            ),
            FilmPreset(
                id = "nh",
                name = "NH",
                displayName = "NH 浓郁",
                series = FilmSeries.CLASSIC,
                matchScore = 0f,
                description = "浓郁色彩，强烈对比",
                colorCharacteristics = "浓郁饱和，高对比",
                bestFor = listOf("风景", "建筑", "棚拍", "艺术")
            ),

            // 情绪与表达系列
            FilmPreset(
                id = "portra",
                name = "Portra",
                displayName = "Portra 400",
                series = FilmSeries.EMOTION,
                matchScore = 0f,
                description = "柯达 Portra 400，柔美人像胶片",
                colorCharacteristics = "柔美肤色，低对比度",
                bestFor = listOf("人像", "逆光", "婚礼", "柔美")
            ),
            FilmPreset(
                id = "rdp3",
                name = "RDP3",
                displayName = "RDP3",
                series = FilmSeries.EMOTION,
                matchScore = 0f,
                description = "富士 Velvia 风格，风景专用",
                colorCharacteristics = "高饱和，风景专用",
                bestFor = listOf("风景", "日落", "秋景", "自然")
            ),

            // 结构与时间系列
            FilmPreset(
                id = "800t",
                name = "800T",
                displayName = "800T",
                series = FilmSeries.STRUCTURE,
                matchScore = 0f,
                description = "夜景胶片，霓虹灯专用",
                colorCharacteristics = "夜景专用，霓虹感",
                bestFor = listOf("夜景", "霓虹", "城市", "星空")
            ),
            FilmPreset(
                id = "tx400",
                name = "TX400",
                displayName = "TX400",
                series = FilmSeries.STRUCTURE,
                matchScore = 0f,
                description = "黑白胶片，经典质感",
                colorCharacteristics = "黑白经典，高对比",
                bestFor = listOf("黑白", "街拍", "建筑", "纪实")
            ),

            // 数字记忆系列
            FilmPreset(
                id = "ccd_cool",
                name = "CCD-Cool",
                displayName = "冷调 CCD",
                series = FilmSeries.DIGITAL,
                matchScore = 0f,
                description = "数码 CCD 冷调风格",
                colorCharacteristics = "冷色调，清透感",
                bestFor = listOf("雪景", "天空", "海滩", "冷调")
            ),
            FilmPreset(
                id = "ccd_warm",
                name = "CCD-Warm",
                displayName = "暖调 CCD",
                series = FilmSeries.DIGITAL,
                matchScore = 0f,
                description = "数码 CCD 暖调风格",
                colorCharacteristics = "暖色调，温馨感",
                bestFor = listOf("美食", "咖啡馆", "烛光", "儿童")
            )
        )

        /**
         * 根据 ID 获取胶片预设
         */
        fun fromId(id: String): FilmPreset? {
            return ALL_PRESETS.find { it.id == id }
        }

        /**
         * 根据系列获取胶片列表
         */
        fun bySeries(series: FilmSeries): List<FilmPreset> {
            return ALL_PRESETS.filter { it.series == series }
        }
    }
}