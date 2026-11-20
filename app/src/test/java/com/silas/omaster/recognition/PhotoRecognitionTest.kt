package com.silas.omaster.recognition

import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.PhotoParamParser
import org.junit.Assert.*
import org.junit.Test

/**
 * 照片识别核心逻辑单元测试（纯 JVM，无 Android 依赖）。
 * 对应 PRD 第八章 8.4 质量标准：字段正确、格式合规、置信度分层、可解释。
 */
class PhotoRecognitionTest {

    // ---------- PhotoParamParser ----------

    @Test
    fun formatShutter_fractionAndDecimal() {
        assertEquals("1/250", PhotoParamParser.formatShutter("1/250"))
        assertEquals("1/125", PhotoParamParser.formatShutter("2/250")) // 约分
        assertEquals("1/250", PhotoParamParser.formatShutter("0.004"))
        assertEquals("1.6s", PhotoParamParser.formatShutter("1.6"))
        assertNull(PhotoParamParser.formatShutter(null))
        assertNull(PhotoParamParser.formatShutter("abc"))
    }

    @Test
    fun formatExposureBias() {
        assertEquals("+0.7EV", PhotoParamParser.formatExposureBias("0.7"))
        assertEquals("-1.0EV", PhotoParamParser.formatExposureBias("-1"))
        assertEquals("0EV", PhotoParamParser.formatExposureBias("0"))
        assertNull(PhotoParamParser.formatExposureBias("xyz"))
    }

    @Test
    fun apexConversion() {
        // EV=8 → 1/256 ≈ 1/256s
        val shutter = PhotoParamParser.apexToShutterSeconds("8")
        assertNotNull(shutter)
        assertTrue(shutter!!.startsWith("1/"))
        // APEX 光圈 5 → f/5.7
        assertTrue(PhotoParamParser.apexToAperture("5")!!.startsWith("f/"))
    }

    @Test
    fun parseColorTempAndWhiteBalance() {
        assertEquals(5500, PhotoParamParser.parseColorTemp("5500K"))
        assertNull(PhotoParamParser.parseColorTemp("999")) // 超出范围
        assertEquals("日光", PhotoParamParser.normalizeWhiteBalance("Daylight"))
        assertEquals("自动", PhotoParamParser.normalizeWhiteBalance("auto"))
    }

    // ---------- StyleAnalyzer ----------

    @Test
    fun styleVector_bounds() {
        // 纯白样本 → 高亮度、零饱和
        val whites = List(1024) { 0xFFFFFFFF.toInt() }
        val v = StyleAnalyzer.computeVectorFromSamples(whites)
        assertTrue(v.brightness > 0.95f)
        assertTrue(v.saturation < 0.05f)
    }

    @Test
    fun styleVector_blackAndWhite() {
        val blacks = List(1024) { 0xFF000000.toInt() }
        val v = StyleAnalyzer.computeVectorFromSamples(blacks)
        assertTrue(v.brightness < 0.05f)
    }

    @Test
    fun styleVector_emptySamplesReturnsNeutral() {
        assertEquals(StyleVector.NEUTRAL, StyleAnalyzer.computeVectorFromSamples(emptyList()))
    }

    @Test
    fun matchCatalog_returnsTop3Sorted() {
        val vivid = StyleVector(0.55f, 0.58f, 0.80f, 0.52f, 0.70f, 0.85f)
        val matches = StyleAnalyzer.matchCatalog(vivid, topN = 3)
        assertEquals(3, matches.size)
        // 置信度降序
        assertTrue(matches[0].confidence >= matches[1].confidence)
        assertTrue(matches[1].confidence >= matches[2].confidence)
        // 鲜明风格应排在首位
        assertEquals("vivid", matches[0].preset.key)
    }

    // ---------- PhotoRecognitionEngine ----------

    @Test
    fun recognize_withExif_generatesDraftWithParams() {
        val analysis = StyleAnalyzer.Analysis(
            vector = StyleCatalog.byKey("clear")!!.target,
            sampleCount = 1024, compressed = false,
            matches = StyleAnalyzer.matchCatalog(StyleCatalog.byKey("clear")!!.target, 3)
        )
        val input = RecognitionInput(
            hasExif = true, aperture = "f/1.8", shutterSpeed = "1/250", iso = "100",
            whiteBalance = "日光", colorTemperature = 5500, exposureBias = "-0.3EV",
            focusMm = "23mm", sourceLabel = "Find X7 哈苏", styleAnalysis = analysis
        )
        val result = PhotoRecognitionEngine.recognize(input)
        assertEquals("100", result.draft.iso)
        assertEquals("1/250", result.draft.shutterSpeed)
        assertEquals(5500, result.draft.colorTemperature)
        assertTrue(result.draft.tags!!.contains("照片识别"))
        assertTrue(result.confidence > 0.7f) // 强匹配
        assertTrue(result.draft.name.contains("清透"))
    }

    @Test
    fun recognize_withoutExif_warnsAndMarksStyleOnly() {
        val analysis = StyleAnalyzer.Analysis(
            vector = StyleVector.NEUTRAL, sampleCount = 1024, compressed = true,
            matches = StyleAnalyzer.matchCatalog(StyleVector.NEUTRAL, 3)
        )
        val input = RecognitionInput(hasExif = false, styleAnalysis = analysis)
        val result = PhotoRecognitionEngine.recognize(input)
        assertEquals(RecognitionSource.PHOTO_STYLE, result.source)
        assertTrue(result.warnings.any { it.type == WarningType.NO_EXIF })
        assertTrue(result.warnings.any { it.type == WarningType.COMPRESSED })
    }

    @Test
    fun recognize_conflict_detectsWarmthMismatch() {
        // EXIF 高色温(偏暖)但风格冷调(clear) → 应产生冲突 warning
        val analysis = StyleAnalyzer.Analysis(
            vector = StyleCatalog.byKey("clear")!!.target, sampleCount = 1024, compressed = false,
            matches = StyleAnalyzer.matchCatalog(StyleCatalog.byKey("clear")!!.target, 3)
        )
        val input = RecognitionInput(
            hasExif = true, colorTemperature = 7500, styleAnalysis = analysis
        )
        val result = PhotoRecognitionEngine.recognize(input)
        val conflict = result.warnings.firstOrNull { it.type == WarningType.TEMP_CONFLICT }
        assertNotNull(conflict)
        assertEquals(7500, conflict!!.value)   // 附带色温 K，供 UI 本地化展示
    }

    @Test
    fun recognize_exposesApertureAndFocalLengthForDisplay() {
        // 手机大师模式无光圈参数项：光圈/焦距仅作为识别依据透传给 UI 展示，不落入预设草稿
        val analysis = StyleAnalyzer.Analysis(
            vector = StyleCatalog.byKey("clear")!!.target, sampleCount = 1024, compressed = false,
            matches = StyleAnalyzer.matchCatalog(StyleCatalog.byKey("clear")!!.target, 3)
        )
        val input = RecognitionInput(
            hasExif = true, aperture = "f/1.8", focusMm = "23mm",
            sourceLabel = "Find X7 哈苏", styleAnalysis = analysis
        )
        val result = PhotoRecognitionEngine.recognize(input)
        assertEquals("f/1.8", result.aperture)
        assertEquals("23mm", result.focalLength)
        assertEquals("Find X7 哈苏", result.sourceLabel)
    }

    @Test
    fun recognize_featuresAndConfidenceLevel() {
        val analysis = StyleAnalyzer.Analysis(
            vector = StyleCatalog.byKey("clear")!!.target, sampleCount = 1024, compressed = false,
            matches = StyleAnalyzer.matchCatalog(StyleCatalog.byKey("clear")!!.target, 3)
        )
        val input = RecognitionInput(hasExif = true, styleAnalysis = analysis)
        val result = PhotoRecognitionEngine.recognize(input)
        // 特征标签构成"可解释依据"
        assertTrue(result.features.isNotEmpty())
        // 置信度等级分层（0=强 / 1=中 / 2=弱）
        assertTrue(result.confidenceLevel in 0..2)
        assertTrue(result.isStrongMatch == (result.confidence >= 0.7f))
        // 引擎不再输出展示层文案（warnings 为结构化类型）
        assertTrue(result.warnings.all { it.value == null || it.type == WarningType.TEMP_CONFLICT })
    }

    @Test
    fun buildReasonTags_structured() {
        val tags = StyleAnalyzer.buildReasonTags(
            StyleVector(0.9f, 0.5f, 0.1f, 0.8f, 0.9f, 0.5f)
        )
        assertTrue(tags.contains(FeatureTag.BRIGHT))
        assertTrue(tags.contains(FeatureTag.LOW_SAT))
        assertTrue(tags.contains(FeatureTag.WARM))
        assertTrue(tags.contains(FeatureTag.WIDE_DR))
        assertFalse(tags.contains(FeatureTag.DARK))
    }

    // ---------- PresetColorTransform ----------

    @Test
    fun colorMatrix_lengthAndNeutral() {
        val neutral = MasterPreset(name = "n", coverPath = "", saturation = 0, warmCool = 0,
            cyanMagenta = 0, tone = 0)
        val m = PresetColorTransform.buildColorMatrix(neutral)
        assertEquals(20, m.size)
        assertFalse(PresetColorTransform.hasVisibleEffect(neutral))
    }

    @Test
    fun colorMatrix_visibleEffect() {
        val vivid = MasterPreset(name = "v", coverPath = "", saturation = 30, warmCool = 10)
        assertTrue(PresetColorTransform.hasVisibleEffect(vivid))
    }

    // ---------- CameraCapabilityProbe ----------

    @Test
    fun capabilityLevel_mapping() {
        val l1 = CameraCapabilityProbe.capabilityFromLevel(
            android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, 2, null)
        assertEquals(CameraCapabilityProbe.Level.L1, l1.level)
        assertTrue(l1.isManualExposure)

        val l3 = CameraCapabilityProbe.capabilityFromLevel(-1, 0, null)
        assertEquals(CameraCapabilityProbe.Level.L3, l3.level)
        assertFalse(l3.isManualExposure)
    }
}
