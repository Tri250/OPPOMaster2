package com.silas.omaster.ui.features

import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.model.SceneCategory
import com.silas.omaster.model.SceneProfile
import com.silas.omaster.model.ScenePresets

/**
 * P1-10：哈苏之眼 UI 模式定义中心
 *
 * 将原先散落在 HasselbladScreen.kt 中的 SceneMode / ColorMode / 全部模式列表
 * 抽取为公共可复用的模式仓库，便于 ViewModel 与分析结果做细粒度映射。
 */

/**
 * 粗粒度场景模式（UI 一级选项）
 */
data class SceneMode(
    val id: String,
    val name: String,
    val description: String,
    val category: SceneCategory,
    val confidence: Float,
    val params: Map<String, Int>,
    val icon: String
)

/**
 * 色彩模式（独立可选）
 */
data class ColorMode(
    val id: String,
    val name: String,
    val description: String,
    val color: Long,
    val params: Map<String, Int>
)

/**
 * 全部粗粒度场景模式
 */
val allSceneModes: List<SceneMode> = listOf(
    SceneMode("scene-portrait", "人像大师", "肤色自然 · 背景虚化 · 柔和色调", SceneCategory.PORTRAIT, 0.92f,
        mapOf("tone" to -3, "saturation" to 10, "contrast" to -15, "colorTemp" to 5, "vignette" to 8), "👤"),
    SceneMode("scene-landscape", "风景增强", "天空湛蓝 · 植被浓郁 · 层次分明", SceneCategory.LANDSCAPE, 0.88f,
        mapOf("tone" to 5, "saturation" to 15, "contrast" to 12, "clarity" to 10, "highlights" to -5), "🏔️"),
    SceneMode("scene-night", "夜景星空", "降噪保留细节 · 暗部纯净 · 星芒锐利", SceneCategory.NIGHT, 0.85f,
        mapOf("tone" to -15, "contrast" to 25, "colorTemp" to -5, "sharpness" to 8, "clarity" to 5), "🌃"),
    SceneMode("scene-food", "美食胶片", "色彩鲜艳 · 质感润泽 · 食欲诱惑", SceneCategory.FOOD, 0.87f,
        mapOf("tone" to -5, "saturation" to 15, "colorTemp" to 10, "sharpness" to 5), "🍜"),
    SceneMode("scene-urban", "城市街拍", "对比鲜明 · 质感硬朗 · 电影感强", SceneCategory.URBAN, 0.83f,
        mapOf("tone" to -5, "saturation" to 5, "contrast" to 18, "cyanMagenta" to -8), "🏢"),
    SceneMode("scene-still", "静物小品", "色彩细腻 · 质感丰富 · 主体突出", SceneCategory.STILL_LIFE, 0.86f,
        mapOf("saturation" to 15, "tone" to 5, "sharpness" to 10, "clarity" to 8), "📦"),
    SceneMode("scene-macro", "微距世界", "细节锐利 · 色彩鲜艳 · 背景虚化", SceneCategory.MACRO, 0.84f,
        mapOf("sharpness" to 25, "contrast" to 15, "saturation" to 10, "clarity" to 10), "🔍"),
    SceneMode("scene-event", "活动纪实", "抓拍精彩 · 动态丰富 · 真实感人", SceneCategory.EVENT, 0.82f,
        mapOf("tone" to 5, "saturation" to 10, "colorTemp" to 10, "sharpness" to 5), "🎉"),
    SceneMode("scene-natural", "自然色彩", "HNCS 3.0 哈苏自然色彩科学（默认推荐）", SceneCategory.UNKNOWN, 0.0f,
        emptyMap(), "✨")
)

/**
 * 全部色彩模式
 */
val allColorModes: List<ColorMode> = listOf(
    ColorMode("natural", "自然色彩", "HNCS 3.0 自然色彩，哈苏色彩克制哲学", 0xFFFF6B35,
        mapOf("saturation" to 0, "contrast" to 5, "warmth" to 0, "clarity" to 0, "highlights" to 0)),
    ColorMode("portrait", "人像肤色", "自然美化肤色，保留细节，柔和背景", 0xFFFF6B9D,
        mapOf("saturation" to 5, "contrast" to 8, "warmth" to 3, "tone" to 3, "clarity" to 0)),
    ColorMode("landscape", "风景色彩", "天空湛蓝，植被浓郁，层次分明", 0xFF4CAF50,
        mapOf("saturation" to 12, "contrast" to 10, "warmth" to 5, "clarity" to 10, "tone" to 5)),
    ColorMode("classic", "经典胶片", "复古胶片色彩质感，棕调浓郁", 0xFF9C27B0,
        mapOf("saturation" to 8, "contrast" to 15, "warmth" to 8, "clarity" to 0, "tone" to -3, "vignette" to 10)),
    ColorMode("bw", "哈苏黑白", "经典黑白摄影，高对比，暗部丰富", 0xFF212121,
        mapOf("saturation" to -30, "contrast" to 20, "clarity" to 15, "shadows" to -5, "highlights" to -10)),
    ColorMode("vivid", "鲜艳色彩", "鲜艳饱满的色彩表现，视觉冲击力强", 0xFFFF9800,
        mapOf("saturation" to 20, "contrast" to 10, "warmth" to 0, "clarity" to 5)),
    ColorMode("film-portra", "Portra 400", "柯达 Portra 400 胶片，温暖柔和人像胶片", 0xFFF4A460,
        mapOf("saturation" to 8, "contrast" to 8, "warmth" to 10, "tone" to 5)),
    ColorMode("film-cc", "CC 经典负片", "柯达 ColorPlus 200 风格，温暖怀旧色彩", 0xFFE4B060,
        mapOf("saturation" to 10, "contrast" to 12, "warmth" to 8, "tone" to 3, "vignette" to 6)),
    ColorMode("film-bw", "TX400 黑白", "Kodak Tri-X 400，高对比颗粒感黑白", 0xFF333333,
        mapOf("saturation" to -30, "contrast" to 25, "clarity" to 18, "tone" to -5)),
    // === 哈苏原厂胶片预设（对标 OPPO Find X9 哈苏大师） ===
    ColorMode("hasselblad-x1d", "哈苏 X1D", "哈苏 X1D 中画幅色彩，浓郁深邃，肤色通透", 0xFF8B4513,
        mapOf("saturation" to 8, "contrast" to 12, "warmth" to 5, "clarity" to 8, "tone" to 2, "highlights" to -5, "shadows" to 5)),
    ColorMode("hasselblad-hcd", "哈苏 HCD", "哈苏 HCD 镜头风格，锐利通透，色彩还原精准", 0xFF2E86C1,
        mapOf("saturation" to 5, "contrast" to 10, "warmth" to -3, "clarity" to 15, "tone" to 0, "sharpness" to 8)),
    ColorMode("hasselblad-portra160", "哈苏 Portra 160", "哈苏人像胶片，柔和肤色，低对比自然过渡", 0xFFD4A574,
        mapOf("saturation" to 6, "contrast" to 5, "warmth" to 8, "tone" to 5, "clarity" to 0, "softLight" to 1)),
    ColorMode("hasselblad-tmax", "哈苏 T-MAX", "哈苏黑白胶片，细腻灰阶，专业级黑白质感", 0xFF4A4A4A,
        mapOf("saturation" to -30, "contrast" to 18, "clarity" to 12, "tone" to -3, "shadows" to 8, "highlights" to -5))
)

/**
 * 细粒度场景 → 粗粒度场景模式 ID 映射
 *
 * 规则：按 SceneProfile.category 直接归类到对应粗粒度模式。
 */
fun mapSceneProfileToSceneModeId(profile: SceneProfile): String {
    return when (profile.category) {
        SceneCategory.PORTRAIT -> "scene-portrait"
        SceneCategory.LANDSCAPE -> "scene-landscape"
        SceneCategory.NIGHT -> "scene-night"
        SceneCategory.FOOD -> "scene-food"
        SceneCategory.URBAN -> "scene-urban"
        SceneCategory.STILL_LIFE -> "scene-still"
        SceneCategory.MACRO -> "scene-macro"
        SceneCategory.EVENT -> "scene-event"
        SceneCategory.UNKNOWN -> "scene-natural"
    }
}

/**
 * 根据粗粒度场景模式 ID 获取其下的细分子模式（2-4 个）。
 *
 * 数据来源：ScenePresets 中的 50+ 三级场景体系。
 */
fun getSubSceneProfiles(sceneModeId: String): List<SceneProfile> {
    val category = allSceneModes.find { it.id == sceneModeId }?.category ?: return emptyList()
    return ScenePresets.getScenesByCategory(category)
}

/**
 * 根据分析推荐的色彩模式名称，获取对应的色彩模式 ID。
 *
 * @param suggestedName 建议名称（如"哈苏自然色彩"）
 * @return 色彩模式 ID，找不到则返回 "natural"
 */
fun resolveColorModeId(suggestedName: String): String {
    return allColorModes.find { it.name == suggestedName }?.id ?: "natural"
}

/**
 * 根据 SceneCategory 推荐默认色彩模式 ID
 */
fun suggestColorModeIdByCategory(category: SceneCategory): String {
    return when (category) {
        SceneCategory.PORTRAIT -> "portrait"
        SceneCategory.LANDSCAPE -> "landscape"
        SceneCategory.NIGHT -> "classic"
        SceneCategory.FOOD -> "vivid"
        SceneCategory.URBAN -> "classic"
        SceneCategory.STILL_LIFE -> "natural"
        SceneCategory.MACRO -> "vivid"
        SceneCategory.EVENT -> "natural"
        SceneCategory.UNKNOWN -> "natural"
    }
}

/**
 * 将 HasselbladParams 转换为可调参数 Map（用于模式初始化）。
 */
fun paramsToMap(params: HasselbladParams): Map<String, Int> = mapOf(
    "tone" to params.tone,
    "saturation" to params.saturation,
    "contrast" to params.contrast,
    "colorTemp" to params.colorTemp,
    "sharpness" to params.sharpness,
    "vignette" to params.vignette,
    "cyanMagenta" to params.cyanMagenta,
    "highlights" to params.highlights,
    "shadows" to params.shadows,
    "clarity" to params.clarity
)
