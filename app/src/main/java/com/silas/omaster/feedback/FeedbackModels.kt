package com.silas.omaster.feedback

import com.silas.omaster.model.HasselbladParams
import kotlinx.serialization.Serializable

/**
 * 用户反馈数据模型
 */
@Serializable
data class FeedbackEntry(
    val id: String,
    val rating: Int,
    val tags: List<String>,
    val comment: String,
    val screenshotPath: String?,
    val sceneId: String?,
    val recipeId: String?,
    val params: HasselbladParams?,
    val deviceInfo: DeviceInfo,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 设备信息
 */
@Serializable
data class DeviceInfo(
    val model: String,
    val osVersion: String,
    val appVersion: String
)

/**
 * 反馈标签预定义库
 */
object FeedbackTags {
    val ALL = listOf(
        "色彩不准确",
        "分析速度慢",
        "场景识别错误",
        "LUT效果不自然",
        "UI卡顿",
        "保存失败",
        "希望增加XX功能",
        "其他问题"
    )
}
