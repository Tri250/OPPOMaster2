package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌文案规范
 * 
 * 与 Web 端 HasselbladCopy 保持一致
 */
object HasselbladCopy {
    // ========== 按钮文案 ==========

    /** 应用预设按钮 */
    const val ApplyPreset = "一键应用哈苏配方"

    /** 收藏按钮 */
    const val SavePreset = "收藏配方"

    /** 分享按钮 */
    const val SharePreset = "分享配方"

    /** 导出按钮 */
    const val ExportPreset = "导出配方"

    // ========== 标签文案 ==========

    /** HNCS 标签 */
    const val HncsBadge = "HNCS 自然色彩认证"

    /** NEW 标签 */
    const val NewBadge = "NEW"

    /** PRO 标签 */
    const val ProBadge = "PRO"

    /** HOT 标签 */
    const val HotBadge = "HOT"

    // ========== 标题文案 ==========

    /** Hero 横幅 */
    const val HeroTitle = "今日哈苏大师推荐"

    /** 预设详情标题模板 */
    fun presetDetailTitle(name: String) = "哈苏大师配方 · $name"

    /** 参数标题 */
    const val ParamsTitle = "大师调色参数"

    /** 拍摄建议标题 */
    const val ShootingTipsTitle = "哈苏大师拍摄建议"

    /** 关联推荐标题 */
    const val RelatedTitle = "哈苏大师也爱用"

    // ========== 空状态文案 ==========

    /** 空状态 */
    const val EmptyState = "探索哈苏大师配方库"

    /** 无预设 */
    const val NoPresets = "暂无预设，开始探索哈苏大师配方"

    // ========== 场景文案 ==========

    /** 场景识别 */
    const val SceneRecognition = "哈苏之眼"

    /** 场景分析 */
    const val SceneAnalysis = "场景分析"

    // ========== 胶片文案 ==========

    /** 胶片推荐 */
    const val FilmRecommendation = "胶片推荐"

    /** 胶片匹配度 */
    const val FilmMatch = "胶片匹配度"

    // ========== 水印文案 ==========

    /** 水印编辑器标题 */
    const val WatermarkEditorTitle = "哈苏大师印记"

    /** 保存水印 */
    const val SaveWatermark = "另存为印记"

    /** 导出水印 */
    const val ExportWatermark = "铭刻并导出"

    // ========== 报告文案 ==========

    /** 拍摄报告 */
    const val ShootingReport = "拍摄分析报告"

    /** 大师建议 */
    const val MasterTips = "大师建议"
}