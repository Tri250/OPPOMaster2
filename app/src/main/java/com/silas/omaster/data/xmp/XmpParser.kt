package com.silas.omaster.data.xmp

import android.util.Log
import com.silas.omaster.model.PresetItem
import com.silas.omaster.model.PresetSection
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.io.InputStreamReader

/**
 * XMP 文件解析器
 *
 * 支持解析 Adobe XMP 标准格式的 .xmp 文件，提取 Camera Raw Settings (crs:*) 标签中的
 * 调色参数，包括曝光、色温、对比度、饱和度、色调曲线、HSL 调整等。
 *
 * 同时支持品牌特定字段：
 * - Hasselblad: HNCS 相关标签
 * - Fuji: Film Simulation (胶片模拟) / CameraProfile
 * - Sony: DRO (Dynamic Range Optimizer)
 * - Leica: Leica 色彩配置
 */
object XmpParser {

    private const val TAG = "XmpParser"

    // XMP 命名空间
    private const val NS_CRS = "http://ns.adobe.com/camera-raw-settings/1.0/"
    private const val NS_DC = "http://purl.org/dc/elements/1.1/"
    private const val NS_XMP = "http://ns.adobe.com/xap/1.0/"
    private const val NS_TIFF = "http://ns.adobe.com/tiff/1.0/"
    private const val NS_EXIF = "http://ns.adobe.com/exif/1.0/"
    private const val NS_AUX = "http://ns.adobe.com/exif/1.0/aux/"

    /**
     * XMP 解析结果
     */
    sealed class XmpParseResult {
        data class Success(
            val sections: List<PresetSection>,
            val brand: String? = null,
            val cameraModel: String? = null
        ) : XmpParseResult()

        data class Failure(
            val errorMessage: String
        ) : XmpParseResult()
    }

    /**
     * 从 InputStream 解析 XMP 文件
     */
    fun parse(inputStream: InputStream): XmpParseResult {
        return try {
            val params = mutableMapOf<String, String>()
            val hslHue = mutableMapOf<String, String>()
            val hslSaturation = mutableMapOf<String, String>()
            val hslLuminance = mutableMapOf<String, String>()
            val toneCurvePoints = mutableListOf<String>()
            var cameraModel: String? = null
            var cameraProfile: String? = null
            var brand: String? = null

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(InputStreamReader(inputStream, "UTF-8"))

            var eventType = parser.eventType
            var inDescription = false
            var inSeq = false
            var currentSeqTag = ""
            var seqItemCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        val ns = parser.namespace

                        when {
                            // crs:Description 是 XMP 的主描述容器
                            tagName.equals("Description", ignoreCase = true) -> {
                                inDescription = true
                                // 提取 Description 标签上的属性（XMP 常把参数放在属性里）
                                for (i in 0 until parser.attributeCount) {
                                    val attrName = parser.getAttributeName(i)
                                    val attrValue = parser.getAttributeValue(i)
                                    val attrNs = parser.getAttributeNamespace(i) ?: ""

                                    when {
                                        // Camera Model
                                        attrName == "Model" && attrNs.contains("tiff") -> {
                                            cameraModel = attrValue
                                        }
                                        // crs: Exposure
                                        attrName == "Exposure2012" && attrNs.contains("camera-raw") -> {
                                            params["exposure"] = attrValue
                                        }
                                        attrName == "Exposure" && attrNs.contains("camera-raw") -> {
                                            if (!params.containsKey("exposure")) {
                                                params["exposure"] = attrValue
                                            }
                                        }
                                        // crs: Temperature
                                        attrName == "Temperature" && attrNs.contains("camera-raw") -> {
                                            params["temperature"] = attrValue
                                        }
                                        // crs: Contrast
                                        attrName == "Contrast2012" && attrNs.contains("camera-raw") -> {
                                            params["contrast"] = attrValue
                                        }
                                        attrName == "Contrast" && attrNs.contains("camera-raw") -> {
                                            if (!params.containsKey("contrast")) {
                                                params["contrast"] = attrValue
                                            }
                                        }
                                        // crs: Saturation
                                        attrName == "Saturation" && attrNs.contains("camera-raw") -> {
                                            params["saturation"] = attrValue
                                        }
                                        // crs: Sharpness
                                        attrName == "Sharpness" && attrNs.contains("camera-raw") -> {
                                            params["sharpness"] = attrValue
                                        }
                                        // crs: Hue
                                        attrName == "Hue" && attrNs.contains("camera-raw") -> {
                                            params["hue"] = attrValue
                                        }
                                        // crs: Highlights
                                        attrName == "Highlights2012" && attrNs.contains("camera-raw") -> {
                                            params["highlights"] = attrValue
                                        }
                                        attrName == "Highlights" && attrNs.contains("camera-raw") -> {
                                            if (!params.containsKey("highlights")) {
                                                params["highlights"] = attrValue
                                            }
                                        }
                                        // crs: Shadows
                                        attrName == "Shadows2012" && attrNs.contains("camera-raw") -> {
                                            params["shadows"] = attrValue
                                        }
                                        attrName == "Shadows" && attrNs.contains("camera-raw") -> {
                                            if (!params.containsKey("shadows")) {
                                                params["shadows"] = attrValue
                                            }
                                        }
                                        // crs: Whites
                                        attrName == "Whites2012" && attrNs.contains("camera-raw") -> {
                                            params["whites"] = attrValue
                                        }
                                        // crs: Blacks
                                        attrName == "Blacks2012" && attrNs.contains("camera-raw") -> {
                                            params["blacks"] = attrValue
                                        }
                                        // crs: Clarity
                                        attrName == "Clarity2012" && attrNs.contains("camera-raw") -> {
                                            params["clarity"] = attrValue
                                        }
                                        // crs: Vibrance
                                        attrName == "Vibrance" && attrNs.contains("camera-raw") -> {
                                            params["vibrance"] = attrValue
                                        }
                                        // crs: Tint
                                        attrName == "Tint" && attrNs.contains("camera-raw") -> {
                                            params["tint"] = attrValue
                                        }
                                        // crs: CameraProfile
                                        attrName == "CameraProfile" && attrNs.contains("camera-raw") -> {
                                            cameraProfile = attrValue
                                        }
                                        // HSL Hue adjustments
                                        attrName.startsWith("HueAdjustment") && attrNs.contains("camera-raw") -> {
                                            val color = attrName.removePrefix("HueAdjustment")
                                            hslHue[color.lowercase()] = attrValue
                                        }
                                        // HSL Saturation adjustments
                                        attrName.startsWith("SaturationAdjustment") && attrNs.contains("camera-raw") -> {
                                            val color = attrName.removePrefix("SaturationAdjustment")
                                            hslSaturation[color.lowercase()] = attrValue
                                        }
                                        // HSL Luminance adjustments
                                        attrName.startsWith("LuminanceAdjustment") && attrNs.contains("camera-raw") -> {
                                            val color = attrName.removePrefix("LuminanceAdjustment")
                                            hslLuminance[color.lowercase()] = attrValue
                                        }
                                        // Sony DRO
                                        attrName == "DRO" || attrName == "DynamicRangeOptimizer" -> {
                                            params["dro"] = attrValue
                                        }
                                        // Hasselblad HNCS
                                        attrName == "HNCS" || attrName == "HasselbladNaturalColourSolution" -> {
                                            params["hncs"] = attrValue
                                        }
                                        // Leica color profile
                                        attrName == "LeicaProfile" || attrName == "LeicaColorProfile" -> {
                                            params["leica_profile"] = attrValue
                                        }
                                        // Fuji film simulation
                                        attrName == "FilmSimulation" || attrName == "FilmMode" -> {
                                            params["film_simulation"] = attrValue
                                        }
                                        // Split Toning
                                        attrName == "SplitToningShadowHue" && attrNs.contains("camera-raw") -> {
                                            params["split_tone_shadow_hue"] = attrValue
                                        }
                                        attrName == "SplitToningHighlightHue" && attrNs.contains("camera-raw") -> {
                                            params["split_tone_highlight_hue"] = attrValue
                                        }
                                        attrName == "SplitToningShadowSaturation" && attrNs.contains("camera-raw") -> {
                                            params["split_tone_shadow_saturation"] = attrValue
                                        }
                                        attrName == "SplitToningHighlightSaturation" && attrNs.contains("camera-raw") -> {
                                            params["split_tone_highlight_saturation"] = attrValue
                                        }
                                        // Vignette
                                        attrName == "PostCropVignetteAmount" && attrNs.contains("camera-raw") -> {
                                            params["vignette"] = attrValue
                                        }
                                        // Grain
                                        attrName == "GrainAmount" && attrNs.contains("camera-raw") -> {
                                            params["grain"] = attrValue
                                        }
                                    }
                                }
                            }

                            // Tone Curve (crs:ToneCurvePV2012 / crs:ToneCurve)
                            tagName.equals("ToneCurvePV2012", ignoreCase = true) && ns.contains("camera-raw") -> {
                                currentSeqTag = "ToneCurvePV2012"
                                inSeq = true
                                seqItemCount = 0
                            }
                            tagName.equals("ToneCurve", ignoreCase = true) && ns.contains("camera-raw") -> {
                                if (!inSeq) {
                                    currentSeqTag = "ToneCurve"
                                    inSeq = true
                                    seqItemCount = 0
                                }
                            }

                            // <li> items inside a seq for tone curve
                            tagName == "li" && inSeq -> {
                                val text = parser.nextText()
                                if (text.isNotBlank()) {
                                    toneCurvePoints.add(text.trim())
                                    seqItemCount++
                                }
                            }

                            // Nested element tags inside Description (for non-attribute form)
                            ns.contains("camera-raw") && inDescription -> {
                                when (tagName) {
                                    "Exposure2012", "Exposure" -> {
                                        if (!params.containsKey("exposure")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["exposure"] = text.trim()
                                        }
                                    }
                                    "Temperature" -> {
                                        if (!params.containsKey("temperature")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["temperature"] = text.trim()
                                        }
                                    }
                                    "Contrast2012", "Contrast" -> {
                                        if (!params.containsKey("contrast")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["contrast"] = text.trim()
                                        }
                                    }
                                    "Saturation" -> {
                                        if (!params.containsKey("saturation")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["saturation"] = text.trim()
                                        }
                                    }
                                    "Sharpness" -> {
                                        if (!params.containsKey("sharpness")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["sharpness"] = text.trim()
                                        }
                                    }
                                    "Hue" -> {
                                        if (!params.containsKey("hue")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["hue"] = text.trim()
                                        }
                                    }
                                    "Highlights2012", "Highlights" -> {
                                        if (!params.containsKey("highlights")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["highlights"] = text.trim()
                                        }
                                    }
                                    "Shadows2012", "Shadows" -> {
                                        if (!params.containsKey("shadows")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["shadows"] = text.trim()
                                        }
                                    }
                                    "Whites2012" -> {
                                        if (!params.containsKey("whites")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["whites"] = text.trim()
                                        }
                                    }
                                    "Blacks2012" -> {
                                        if (!params.containsKey("blacks")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["blacks"] = text.trim()
                                        }
                                    }
                                    "Clarity2012", "Clarity" -> {
                                        if (!params.containsKey("clarity")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["clarity"] = text.trim()
                                        }
                                    }
                                    "Vibrance" -> {
                                        if (!params.containsKey("vibrance")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["vibrance"] = text.trim()
                                        }
                                    }
                                    "Tint" -> {
                                        if (!params.containsKey("tint")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["tint"] = text.trim()
                                        }
                                    }
                                    "CameraProfile" -> {
                                        if (cameraProfile == null) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) cameraProfile = text.trim()
                                        }
                                    }
                                    "PostCropVignetteAmount" -> {
                                        if (!params.containsKey("vignette")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["vignette"] = text.trim()
                                        }
                                    }
                                    "GrainAmount" -> {
                                        if (!params.containsKey("grain")) {
                                            val text = parser.nextText()
                                            if (text.isNotBlank()) params["grain"] = text.trim()
                                        }
                                    }
                                }
                                // HSL nested elements
                                if (tagName.startsWith("HueAdjustment")) {
                                    val color = tagName.removePrefix("HueAdjustment").lowercase()
                                    val text = parser.nextText()
                                    if (text.isNotBlank() && !hslHue.containsKey(color)) {
                                        hslHue[color] = text.trim()
                                    }
                                }
                                if (tagName.startsWith("SaturationAdjustment")) {
                                    val color = tagName.removePrefix("SaturationAdjustment").lowercase()
                                    val text = parser.nextText()
                                    if (text.isNotBlank() && !hslSaturation.containsKey(color)) {
                                        hslSaturation[color] = text.trim()
                                    }
                                }
                                if (tagName.startsWith("LuminanceAdjustment")) {
                                    val color = tagName.removePrefix("LuminanceAdjustment").lowercase()
                                    val text = parser.nextText()
                                    if (text.isNotBlank() && !hslLuminance.containsKey(color)) {
                                        hslLuminance[color] = text.trim()
                                    }
                                }
                            }

                            // Camera model from tiff:Model or aux:Lens
                            tagName == "Model" && ns.contains("tiff") -> {
                                if (cameraModel == null) {
                                    val text = parser.nextText()
                                    if (text.isNotBlank()) cameraModel = text.trim()
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val tagName = parser.name
                        if (tagName.equals("Description", ignoreCase = true)) {
                            inDescription = false
                        }
                        if (inSeq && (tagName == "Seq" || tagName == "Bag")) {
                            inSeq = false
                            currentSeqTag = ""
                        }
                    }
                }
                eventType = parser.next()
            }

            // 推断品牌
            brand = detectBrand(cameraModel, cameraProfile, params)

            // 处理 Fuji film simulation
            if (brand == "fujifilm" && cameraProfile != null && !params.containsKey("film_simulation")) {
                params["film_simulation"] = mapFujiFilmSimulation(cameraProfile)
            }

            // 构建预设分组
            val sections = buildSections(params, hslHue, hslSaturation, hslLuminance, toneCurvePoints, brand)

            if (sections.isEmpty()) {
                Log.w(TAG, "No valid parameters found in XMP")
                XmpParseResult.Failure("No valid parameters found in XMP")
            } else {
                Log.d(TAG, "XMP parsed successfully: ${sections.sumOf { it.items.size }} params, brand=$brand")
                XmpParseResult.Success(sections, brand, cameraModel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse XMP", e)
            XmpParseResult.Failure(e.message ?: "Unknown parse error")
        }
    }

    /**
     * 从 XMP 字符串解析（用于测试或直接文本输入）
     */
    fun parse(xmpContent: String): XmpParseResult {
        return parse(xmpContent.byteInputStream(Charsets.UTF_8))
    }

    /**
     * 推断相机品牌
     */
    private fun detectBrand(cameraModel: String?, cameraProfile: String?, params: Map<String, String>): String? {
        val modelLower = cameraModel?.lowercase() ?: ""
        val profileLower = cameraProfile?.lowercase() ?: ""

        return when {
            // Hasselblad
            modelLower.contains("hasselblad") || modelLower.contains("h6d") ||
                    modelLower.contains("x1d") || modelLower.contains("x2d") ||
                    modelLower.contains("907x") || modelLower.contains("500c") ||
                    params.containsKey("hncs") -> "hasselblad"

            // Fuji
            modelLower.contains("fuji") || modelLower.contains("fujifilm") ||
                    modelLower.contains("gfx") || modelLower.contains("x-t") ||
                    modelLower.contains("x-pro") || modelLower.contains("x100") ||
                    modelLower.contains("x-e") || modelLower.contains("x-s") ||
                    profileLower.contains("fuji") || profileLower.contains("classic chrome") ||
                    profileLower.contains("acros") || profileLower.contains("velvia") ||
                    profileLower.contains("provia") || profileLower.contains("astia") ||
                    profileLower.contains("classic neg") || profileLower.contains("eterne") ||
                    params.containsKey("film_simulation") -> "fujifilm"

            // Sony
            modelLower.contains("sony") || modelLower.contains("alpha") ||
                    modelLower.contains("a7") || modelLower.contains("a9") ||
                    modelLower.contains("a1") || modelLower.contains("a6") ||
                    modelLower.contains("ilce") || modelLower.contains("dsc-rx") ||
                    params.containsKey("dro") -> "sony"

            // Leica
            modelLower.contains("leica") || modelLower.contains("m10") ||
                    modelLower.contains("m11") || modelLower.contains("q2") ||
                    modelLower.contains("q3") || modelLower.contains("sl2") ||
                    modelLower.contains("sl3") || modelLower.contains("cl") ||
                    params.containsKey("leica_profile") -> "leica"

            else -> null
        }
    }

    /**
     * 将 Fuji CameraProfile 映射为中文名称
     */
    private fun mapFujiFilmSimulation(profile: String): String {
        return when {
            profile.contains("Classic Chrome", ignoreCase = true) -> "经典正片"
            profile.contains("Classic Neg", ignoreCase = true) -> "经典负片"
            profile.contains("ACROS", ignoreCase = true) -> "ACROS"
            profile.contains("Velvia", ignoreCase = true) -> "Velvia"
            profile.contains("Provia", ignoreCase = true) -> "Provia"
            profile.contains("Astia", ignoreCase = true) -> "Astia"
            profile.contains("Eterna", ignoreCase = true) -> "Eterna"
            profile.contains("Pro Neg", ignoreCase = true) -> "Pro Neg"
            profile.contains("B&W", ignoreCase = true) -> "黑白"
            profile.contains("Sepia", ignoreCase = true) -> "怀旧"
            else -> profile
        }
    }

    /**
     * 构建预设分组
     */
    private fun buildSections(
        params: Map<String, String>,
        hslHue: Map<String, String>,
        hslSaturation: Map<String, String>,
        hslLuminance: Map<String, String>,
        toneCurvePoints: List<String>,
        brand: String?
    ): List<PresetSection> {
        val sections = mutableListOf<PresetSection>()

        // 1. 基础调色参数
        val basicItems = mutableListOf<PresetItem>()
        params["exposure"]?.let { basicItems.add(PresetItem("曝光补偿", it, 1)) }
        params["temperature"]?.let { basicItems.add(PresetItem("色温", "${it}K", 1)) }
        params["contrast"]?.let { basicItems.add(PresetItem("对比度", it, 1)) }
        params["highlights"]?.let { basicItems.add(PresetItem("高光", it, 1)) }
        params["shadows"]?.let { basicItems.add(PresetItem("阴影", it, 1)) }
        params["whites"]?.let { basicItems.add(PresetItem("白色", it, 1)) }
        params["blacks"]?.let { basicItems.add(PresetItem("黑色", it, 1)) }
        if (basicItems.isNotEmpty()) {
            sections.add(PresetSection("基础调色", basicItems))
        }

        // 2. 色彩参数
        val colorItems = mutableListOf<PresetItem>()
        params["saturation"]?.let { colorItems.add(PresetItem("饱和度", it, 1)) }
        params["vibrance"]?.let { colorItems.add(PresetItem("自然饱和度", it, 1)) }
        params["hue"]?.let { colorItems.add(PresetItem("色调", it, 1)) }
        params["tint"]?.let { colorItems.add(PresetItem("色偏", it, 1)) }
        params["clarity"]?.let { colorItems.add(PresetItem("清晰度", it, 1)) }
        params["sharpness"]?.let { colorItems.add(PresetItem("锐度", it, 1)) }
        params["vignette"]?.let { colorItems.add(PresetItem("暗角", it, 1)) }
        params["grain"]?.let { colorItems.add(PresetItem("颗粒感", it, 1)) }
        if (colorItems.isNotEmpty()) {
            sections.add(PresetSection("色彩参数", colorItems))
        }

        // 3. 色调曲线
        if (toneCurvePoints.isNotEmpty()) {
            val curveItems = mutableListOf<PresetItem>()
            val grouped = toneCurvePoints.chunked(2)
            // 将曲线点分组展示 (Input -> Output)
            val curveValue = grouped.mapIndexed { idx, pair ->
                val input = pair.getOrNull(0) ?: "?"
                val output = pair.getOrNull(1) ?: "?"
                "$input→$output"
            }.joinToString("  ")
            curveItems.add(PresetItem("色调曲线", curveValue, 2))
            sections.add(PresetSection("色调曲线", curveItems))
        }

        // 4. HSL 调整
        val hslItems = mutableListOf<PresetItem>()
        val colors = setOf(
            "red", "orange", "yellow", "green", "aqua",
            "blue", "purple", "magenta"
        )
        val colorNameMap = mapOf(
            "red" to "红", "orange" to "橙", "yellow" to "黄",
            "green" to "绿", "aqua" to "浅绿", "blue" to "蓝",
            "purple" to "紫", "magenta" to "品红"
        )
        for (color in colors) {
            val cn = colorNameMap[color] ?: color
            val hueVal = hslHue[color]
            val satVal = hslSaturation[color]
            val lumVal = hslLuminance[color]
            if (hueVal != null || satVal != null || lumVal != null) {
                val parts = mutableListOf<String>()
                hueVal?.let { parts.add("H$it") }
                satVal?.let { parts.add("S$it") }
                lumVal?.let { parts.add("L$it") }
                hslItems.add(PresetItem(cn, parts.joinToString(" "), 1))
            }
        }
        if (hslItems.isNotEmpty()) {
            sections.add(PresetSection("HSL 调整", hslItems))
        }

        // 5. 分离色调
        val splitItems = mutableListOf<PresetItem>()
        params["split_tone_highlight_hue"]?.let { splitItems.add(PresetItem("高光色相", it, 1)) }
        params["split_tone_highlight_saturation"]?.let { splitItems.add(PresetItem("高光饱和度", it, 1)) }
        params["split_tone_shadow_hue"]?.let { splitItems.add(PresetItem("阴影色相", it, 1)) }
        params["split_tone_shadow_saturation"]?.let { splitItems.add(PresetItem("阴影饱和度", it, 1)) }
        if (splitItems.isNotEmpty()) {
            sections.add(PresetSection("分离色调", splitItems))
        }

        // 6. 品牌特定参数
        val brandItems = mutableListOf<PresetItem>()
        when (brand) {
            "hasselblad" -> {
                params["hncs"]?.let { brandItems.add(PresetItem("HNCS", it, 2)) }
            }
            "fujifilm" -> {
                params["film_simulation"]?.let { brandItems.add(PresetItem("胶片模拟", it, 2)) }
            }
            "sony" -> {
                params["dro"]?.let { brandItems.add(PresetItem("DRO", it, 1)) }
            }
            "leica" -> {
                params["leica_profile"]?.let { brandItems.add(PresetItem("徕卡色彩配置", it, 2)) }
            }
        }
        if (brandItems.isNotEmpty()) {
            val sectionTitle = when (brand) {
                "hasselblad" -> "哈苏特色"
                "fujifilm" -> "富士特色"
                "sony" -> "索尼特色"
                "leica" -> "徕卡特色"
                else -> "品牌特色"
            }
            sections.add(PresetSection(sectionTitle, brandItems))
        }

        return sections
    }
}
