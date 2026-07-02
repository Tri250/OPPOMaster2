package com.silas.omaster.data.xmp

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * XMP 解析结果
 */
data class XMPParseResult(
    val success: Boolean,
    val presetName: String? = null,
    val brand: String? = null,
    val params: Map<String, Float> = emptyMap(),
    val hsl: Map<String, HSLValues> = emptyMap(),
    val curve: List<CurvePoint> = emptyList(),
    val errorMessage: String? = null,
    val rawFields: Map<String, String> = emptyMap()
)

/**
 * HSL 数值
 */
data class HSLValues(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f
)

/**
 * 曲线控制点
 */
data class CurvePoint(
    val x: Float,
    val y: Float
)

/**
 * XMP 预设解析器
 * 支持哈苏、富士、索尼、徕卡等品牌 .xmp 文件解析
 */
object XMPParser {

    private const val TAG = "XMPParser"

    // 品牌识别关键字
    private val BRAND_KEYWORDS = mapOf(
        "hasselblad" to "hasselblad",
        "fuji" to "fujifilm",
        "fujifilm" to "fujifilm",
        "sony" to "sony",
        "leica" to "leica",
        "canon" to "canon",
        "nikon" to "nikon"
    )

    // 富士胶片模拟映射
    private val FUJIFILM_SIMULATIONS = mapOf(
        "PROVIA" to "Provia/标准",
        "Velvia" to "Velvia/鲜艳",
        "ASTIA" to "Astia/柔和",
        "CLASSIC CHROME" to "Classic Chrome",
        "PRO Neg. Hi" to "Pro Neg. Hi",
        "PRO Neg. Std" to "Pro Neg. Std",
        "CLASSIC Neg." to "Classic Neg./NC",
        "NOSTALGIC Neg." to "Nostalgic Neg./NN",
        "ETERNA" to "Eterna/影院",
        "ACROS" to "Acros/黑白",
        "MONOCHROME" to "单色"
    )

    // 索尼 DRO 映射
    private val SONY_DRO_LEVELS = mapOf(
        "0" to "关",
        "1" to "D-R Lv1",
        "2" to "D-R Lv2",
        "3" to "D-R Lv3",
        "4" to "D-R Lv4",
        "5" to "D-R Lv5",
        "6" to "D-R 自动"
    )

    /**
     * 解析 XMP 字符串
     * @param xmpContent XMP 文件内容
     * @return 解析结果
     */
    fun parse(xmpContent: String): XMPParseResult {
        return try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmpContent))

            val params = mutableMapOf<String, Float>()
            val hsl = mutableMapOf<String, HSLValues>()
            val curvePoints = mutableListOf<CurvePoint>()
            val rawFields = mutableMapOf<String, String>()
            var presetName: String? = null
            var brand: String? = null
            var fujiFilmSim: String? = null
            var sonyDRO: String? = null

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name
                    val prefix = parser.prefix ?: ""
                    val fullName = if (prefix.isNotEmpty()) "$prefix:$name" else name

                    // 提取所有 crs: 和 rdf: 属性到原始字段
                    for (i in 0 until parser.attributeCount) {
                        val attrName = parser.getAttributeName(i)
                        val attrValue = parser.getAttributeValue(i)
                        rawFields[attrName] = attrValue

                        when (attrName.lowercase()) {
                            // 基础参数
                            "exposure2012" -> params["exposure"] = attrValue.toFloatOrNull() ?: 0f
                            "temperature" -> params["temperature"] = attrValue.toFloatOrNull() ?: 0f
                            "tint" -> params["tint"] = attrValue.toFloatOrNull() ?: 0f
                            "contrast2012" -> params["contrast"] = attrValue.toFloatOrNull() ?: 0f
                            "highlights2012" -> params["highlights"] = attrValue.toFloatOrNull() ?: 0f
                            "shadows2012" -> params["shadows"] = attrValue.toFloatOrNull() ?: 0f
                            "whites2012" -> params["whites"] = attrValue.toFloatOrNull() ?: 0f
                            "blacks2012" -> params["blacks"] = attrValue.toFloatOrNull() ?: 0f
                            "clarity2012" -> params["clarity"] = attrValue.toFloatOrNull() ?: 0f
                            "vibrance" -> params["vibrance"] = attrValue.toFloatOrNull() ?: 0f
                            "saturation" -> params["saturation"] = attrValue.toFloatOrNull() ?: 0f
                            "sharpenradius" -> params["sharpenRadius"] = attrValue.toFloatOrNull() ?: 0f
                            "sharpenamount" -> params["sharpenAmount"] = attrValue.toFloatOrNull() ?: 0f
                            "sharpenedgeMasking" -> params["sharpenEdgeMasking"] = attrValue.toFloatOrNull() ?: 0f
                            "luminanceSmoothing" -> params["luminanceNR"] = attrValue.toFloatOrNull() ?: 0f
                            "colornoisereduction" -> params["colorNR"] = attrValue.toFloatOrNull() ?: 0f
                            // 富士胶片模拟
                            "cameraprofile" -> {
                                fujiFilmSim = attrValue
                                brand = detectBrand(attrValue)
                            }
                            // 索尼 DRO
                            "drodynamicrange" -> {
                                sonyDRO = attrValue
                                brand = brand ?: "sony"
                            }
                        }
                    }

                    // 尝试识别品牌
                    if (brand == null) {
                        brand = detectBrandFromRawFields(rawFields)
                    }
                }

                // 读取文本内容（用于解析曲线等序列数据）
                if (eventType == XmlPullParser.TEXT) {
                    val text = parser.text?.trim() ?: ""
                    if (text.isNotEmpty() && text.contains(",")) {
                        // 可能是曲线数据
                        val values = text.split(",").mapNotNull { it.trim().toFloatOrNull() }
                        if (values.size >= 4 && values.size % 2 == 0) {
                            for (i in values.indices step 2) {
                                curvePoints.add(CurvePoint(values[i], values[i + 1]))
                            }
                        }
                    }
                }

                eventType = parser.next()
            }

            // 解析 HSL 调整
            parseHSLFromRawFields(rawFields, hsl)

            // 如果没有识别到品牌，尝试从原始字段推断
            if (brand == null) {
                brand = inferBrand(rawFields)
            }

            // 构建预设名称
            presetName = buildPresetName(brand, fujiFilmSim, rawFields)

            XMPParseResult(
                success = params.isNotEmpty() || hsl.isNotEmpty(),
                presetName = presetName,
                brand = brand,
                params = params,
                hsl = hsl,
                curve = curvePoints,
                rawFields = rawFields
            )
        } catch (e: Exception) {
            Log.e(TAG, "XMP解析失败", e)
            XMPParseResult(
                success = false,
                errorMessage = "解析失败: ${e.message ?: "未知错误"}"
            )
        }
    }

    /**
     * 从原始字段解析 HSL
     */
    private fun parseHSLFromRawFields(
        rawFields: Map<String, String>,
        hsl: MutableMap<String, HSLValues>
    ) {
        val colors = listOf("Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple", "Magenta")
        colors.forEach { color ->
            val hue = rawFields["HueAdjustment$color"]?.toFloatOrNull() ?: 0f
            val sat = rawFields["SaturationAdjustment$color"]?.toFloatOrNull() ?: 0f
            val lum = rawFields["LuminanceAdjustment$color"]?.toFloatOrNull() ?: 0f
            if (hue != 0f || sat != 0f || lum != 0f) {
                hsl[color.lowercase()] = HSLValues(hue, sat, lum)
            }
        }
    }

    /**
     * 检测品牌
     */
    private fun detectBrand(value: String): String? {
        val lower = value.lowercase()
        BRAND_KEYWORDS.forEach { (keyword, brandName) ->
            if (lower.contains(keyword)) return brandName
        }
        return null
    }

    /**
     * 从原始字段检测品牌
     */
    private fun detectBrandFromRawFields(rawFields: Map<String, String>): String? {
        rawFields.values.forEach { value ->
            detectBrand(value)?.let { return it }
        }
        return null
    }

    /**
     * 推断品牌
     */
    private fun inferBrand(rawFields: Map<String, String>): String? {
        // 如果有 DRO 相关字段，很可能是索尼
        if (rawFields.keys.any { it.contains("DRO", ignoreCase = true) }) return "sony"
        // 如果有 FilmSimulation 相关字段，很可能是富士
        if (rawFields.keys.any { it.contains("Film", ignoreCase = true) } ||
            rawFields.values.any { it.contains("Film", ignoreCase = true) }
        ) return "fujifilm"
        return null
    }

    /**
     * 构建预设名称
     */
    private fun buildPresetName(
        brand: String?,
        fujiFilmSim: String?,
        rawFields: Map<String, String>
    ): String? {
        // 富士胶片模拟名称
        if (!fujiFilmSim.isNullOrEmpty()) {
            FUJIFILM_SIMULATIONS.entries.find { fujiFilmSim.contains(it.key, ignoreCase = true) }?.let {
                return "富士 ${it.value}"
            }
            return "富士 $fujiFilmSim"
        }

        // 从原始字段查找名称
        rawFields["PresetName"]?.let { return it }
        rawFields["presetName"]?.let { return it }
        rawFields["LookName"]?.let { return it }

        // 品牌默认名称
        return when (brand) {
            "hasselblad" -> "哈苏预设"
            "fujifilm" -> "富士预设"
            "sony" -> "索尼预设"
            "leica" -> "徕卡预设"
            else -> "导入预设"
        }
    }

    /**
     * 获取参数明细描述
     */
    fun formatParamsDetail(result: XMPParseResult): String {
        val sb = StringBuilder()
        sb.appendLine("品牌: ${result.brand ?: "未知"}")
        sb.appendLine("参数明细:")
        result.params.forEach { (key, value) ->
            sb.appendLine("  $key: ${String.format("%.2f", value)}")
        }
        if (result.hsl.isNotEmpty()) {
            sb.appendLine("HSL调整:")
            result.hsl.forEach { (color, values) ->
                sb.appendLine("  $color: H=${values.hue}, S=${values.saturation}, L=${values.luminance}")
            }
        }
        if (result.curve.isNotEmpty()) {
            sb.appendLine("曲线点: ${result.curve.size}个")
        }
        return sb.toString()
    }
}
