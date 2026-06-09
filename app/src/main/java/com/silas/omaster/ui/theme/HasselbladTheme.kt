package com.silas.omaster.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 哈苏品牌色彩系统
 * 
 * 基于 OPPO × 哈苏品牌体系，定义专属色彩系统
 * 与 Web 端 HasselbladColors 保持一致
 */
object HasselbladColors {
    // ========== 主品牌色 ==========

    /** 哈苏橙 - 主品牌色 */
    val Primary = Color(0xFFFF6B35)

    /** 哈苏橙（浅色变体） */
    val PrimaryLight = Color(0xFFFF8C42)

    /** 哈苏橙（深色变体） */
    val PrimaryDark = Color(0xFFE55A25)

    // ========== 金色点缀 ==========

    /** 金色 - 哈苏大师系列分割线、徽章边框 */
    val GoldAccent = Color(0xFFD4AF37)

    /** 金色（浅色变体） */
    val GoldLight = Color(0xFFE5C158)

    /** 金色（深色变体） */
    val GoldDark = Color(0xFFB8962E)

    // ========== 背景颜色 ==========

    /** PureBlack - 全局背景 */
    val Background = Color(0xFF0A0A0A)

    /** 卡片背景 */
    val Card = Color(0xFF1A1A1A)

    /** 表面背景 */
    val Surface = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)

    // ========== 边框颜色 ==========

    /** 边框 */
    val Border = Color(0x0DFFFFFF)

    /** 边框（浅色） */
    val BorderLight = Color(0x1AFFFFFF)

    // ========== 文字颜色 ==========

    /** 文字主色 */
    val TextPrimary = Color.White

    /** 文字次级 */
    val TextSecondary = Color(0x99FFFFFF) // rgba(255,255,255,0.6)

    /** 文字辅助 */
    val TextTertiary = Color(0x66FFFFFF) // rgba(255,255,255,0.4)

    /** 文字暗色 - 高调画面自动切换 */
    val TextDark = Color(0xFF1A1A1A)

    // ========== 角标颜色 ==========

    /** HNCS 橙色渐变起始色 */
    val BadgeHncsStart = Color(0xFFFF6B35)

    /** HNCS 橙色渐变结束色 */
    val BadgeHncsEnd = Color(0xFFFF8C42)

    /** NEW 绿色 */
    val BadgeNew = Color(0xFF4CAF50)

    /** PRO 金色 */
    val BadgePro = Color(0xFFFFD700)

    /** HOT 红色 */
    val BadgeHot = Color(0xFFFF5722)

    // ========== HNCS 认证色 ==========

    /** HNCS 绿 - 认证标识点缀 */
    val HncsGreen = Color(0xFF4CAF50)

    /** HNCS 绿（浅色变体） */
    val HncsGreenLight = Color(0xFF66BB6A)

    // ========== 半透明背景 ==========

    /** 半透明黑色背景 - 底部信息栏 */
    val BackgroundSemiTransparent = Color(0x66000000) // alpha=0.4

    // ========== 分割线颜色 ==========

    /** 分割线（金色） */
    val DividerGold = Color(0x4DD4AF37) // alpha=0.3

    /** 分割线（白色） */
    val DividerWhite = Color(0x4DFFFFFF) // alpha=0.3
}

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