package com.silas.omaster.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * 场景分析结果（兼容旧代码）
 */
data class SceneAnalysisResult(
    val primaryScene: SceneTypeData,
    val confidence: Float,
    val alternativeScenes: List<SceneTypeData>,
    val recommendedFilms: List<FilmPreset>,
    val hasselbladParams: HasselbladParams,
    val masterTips: List<String>
)

/**
 * 场景类型数据（兼容旧代码）
 */
data class SceneTypeData(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val confidence: Int,
    val params: HasselbladParams
)

/**
 * Layer 1: 大师数据 (Master Data)
 * 统一 SceneProfile 模型 - 场景画像
 * 对齐 Android 和 React 两端的数据结构
 */
@Serializable
data class SceneProfile(
    val id: String,                        // "portrait-backlit"
    val name: String,                      // "逆光人像"
    val category: SceneCategory,           // PORTRAIT
    val description: String,               // "侧逆光环境下的柔美人像..."
    val color: Long,                       // 主题色 0xFFFF6B35 (哈苏橙)
    val confidence: Float = 0f,            // 识别置信度
    // 🔑 哈苏大师参数（对齐 OPPO 大师模式参数体系）
    val hasselbladParams: HasselbladParams,
    // 🔑 推荐胶片风格
    val recommendedFilm: List<FilmPreset>,
    // 🔑 拍摄建议（哈苏大师风格）
    val masterTips: List<String>,
    // 关联的相机参数
    val cameraParams: CameraParams? = null,
    // 扩展数据
    val exifData: ExifData? = null,
    val histogramData: HistogramData? = null,
    val faceData: FaceData? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        category = try {
            SceneCategory.valueOf(parcel.readString() ?: SceneCategory.PORTRAIT.name)
        } catch (e: IllegalArgumentException) {
            SceneCategory.PORTRAIT
        },
        description = parcel.readString() ?: "",
        color = parcel.readLong(),
        confidence = parcel.readFloat(),
        hasselbladParams = parcel.readParcelable(HasselbladParams::class.java.classLoader)
            ?: HasselbladParams.DEFAULT,
        recommendedFilm = parcel.createTypedArrayList(FilmPreset.CREATOR) ?: emptyList(),
        masterTips = parcel.createStringArrayList() ?: emptyList(),
        cameraParams = parcel.readParcelable(CameraParams::class.java.classLoader),
        exifData = parcel.readParcelable(ExifData::class.java.classLoader),
        histogramData = parcel.readParcelable(HistogramData::class.java.classLoader),
        faceData = parcel.readParcelable(FaceData::class.java.classLoader),
        timestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(category.name)
        parcel.writeString(description)
        parcel.writeLong(color)
        parcel.writeFloat(confidence)
        parcel.writeParcelable(hasselbladParams, flags)
        parcel.writeTypedList(recommendedFilm)
        parcel.writeStringList(masterTips)
        parcel.writeParcelable(cameraParams, flags)
        parcel.writeParcelable(exifData, flags)
        parcel.writeParcelable(histogramData, flags)
        parcel.writeParcelable(faceData, flags)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SceneProfile> {
        override fun createFromParcel(parcel: Parcel): SceneProfile {
            return SceneProfile(parcel)
        }

        override fun newArray(size: Int): Array<SceneProfile?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 场景大类（一级分类）
 */
enum class SceneCategory(val displayName: String, val icon: String, val color: Long) {
    PORTRAIT("人像", "👤", 0xFFFF6B35),      // 哈苏橙
    LANDSCAPE("风景", "🏔️", 0xFF4CAF50),    // 自然绿
    NIGHT("夜景", "🌃", 0xFF2196F3),        // 夜空蓝
    FOOD("美食", "🍜", 0xFFFF9800),         // 美食橙
    URBAN("城市", "🏢", 0xFF9C27B0),        // 城市紫
    STILL_LIFE("静物", "🍃", 0xFF00BCD4),   // 静物青
    MACRO("微距", "🔍", 0xFFE91E63),        // 微距粉
    EVENT("活动", "🎉", 0xFFFF5722)         // 活动红
}

/**
 * 哈苏大师参数（对齐 OPPO 大师模式真实参数范围）
 * 所有参数范围：-30 ~ +30
 */
@Serializable
data class HasselbladParams(
    val tone: Int = 0,           // 影调 -30 ~ +30，负值=暗调/电影感，正值=明亮/通透
    val saturation: Int = 0,     // 饱和度 -30 ~ +30，HNCS理念：克制使用，±15为舒适区
    val contrast: Int = 0,       // 对比度 -30 ~ +30，哈苏风格偏柔和，不建议极端值
    val colorTemp: Int = 0,      // 色温 -30 ~ +30，负=冷调，正=暖调
    val sharpness: Int = 0,      // 锐度 -30 ~ +30，哈苏风格偏自然锐度，不建议过度锐化
    val vignette: Int = 0,       // 暗角 -30 ~ +30，正=暗角加深，增加胶片感
    val cyanMagenta: Int = 0,    // 青品调 -30 ~ +30，负=偏青/电影感，正=偏品/复古感
    val softLight: SoftLightMode = SoftLightMode.NONE,  // 柔光模式：无/柔/梦幻
    // 扩展参数（用于高级调节）
    val highlights: Int = 0,     // 高光 -30 ~ +30
    val shadows: Int = 0,        // 阴影 -30 ~ +30
    val clarity: Int = 0         // 清晰度 0 ~ +30
) : Parcelable {
    constructor(parcel: Parcel) : this(
        tone = parcel.readInt(),
        saturation = parcel.readInt(),
        contrast = parcel.readInt(),
        colorTemp = parcel.readInt(),
        sharpness = parcel.readInt(),
        vignette = parcel.readInt(),
        cyanMagenta = parcel.readInt(),
        softLight = try { SoftLightMode.valueOf(parcel.readString() ?: SoftLightMode.NONE.name) } catch (_: IllegalArgumentException) { SoftLightMode.NONE },
        highlights = parcel.readInt(),
        shadows = parcel.readInt(),
        clarity = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(tone)
        parcel.writeInt(saturation)
        parcel.writeInt(contrast)
        parcel.writeInt(colorTemp)
        parcel.writeInt(sharpness)
        parcel.writeInt(vignette)
        parcel.writeInt(cyanMagenta)
        parcel.writeString(softLight.name)
        parcel.writeInt(highlights)
        parcel.writeInt(shadows)
        parcel.writeInt(clarity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<HasselbladParams> {
        override fun createFromParcel(parcel: Parcel): HasselbladParams {
            return HasselbladParams(parcel)
        }

        override fun newArray(size: Int): Array<HasselbladParams?> {
            return arrayOfNulls(size)
        }
    }

    /**
     * 格式化参数显示
     */
    fun formatParamValue(value: Int): String {
        return if (value >= 0) "+$value" else "$value"
    }
}

/**
 * 柔光模式
 */
enum class SoftLightMode(val displayName: String, val description: String) {
    NONE("无", "标准效果"),
    SOFT("柔", "柔和光线效果"),
    DREAMY("梦幻", "梦幻柔光效果")
}

/**
 * 胶片预设（对齐 OPPO 9 款原生胶片）
 */
@Serializable
data class FilmPreset(
    val id: String,              // "portra", "cc", "nc", "nh", "rdp3", "800t", "tx400", "ccd_cool", "ccd_warm"
    val name: String,            // "Portra 400", "CC 经典负片"
    val series: FilmSeries,      // 原生经典 / 情绪与表达 / 结构与时间 / 数字记忆
    val matchScore: Float,       // 场景匹配度 0-1
    val description: String = "", // 胶片特性描述
    // 扩展字段（对齐 Web 端）
    val colorStyle: String = "自然",      // 色彩风格
    val grainLevel: String = "中等",      // 颗粒感
    val contrastLevel: String = "标准",   // 对比度
    val bestFor: String = "通用场景"      // 适用场景
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        name = parcel.readString() ?: "",
        series = try { FilmSeries.valueOf(parcel.readString() ?: FilmSeries.CLASSIC.name) } catch (_: IllegalArgumentException) { FilmSeries.CLASSIC },
        matchScore = parcel.readFloat(),
        description = parcel.readString() ?: "",
        colorStyle = parcel.readString() ?: "自然",
        grainLevel = parcel.readString() ?: "中等",
        contrastLevel = parcel.readString() ?: "标准",
        bestFor = parcel.readString() ?: "通用场景"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(series.name)
        parcel.writeFloat(matchScore)
        parcel.writeString(description)
        parcel.writeString(colorStyle)
        parcel.writeString(grainLevel)
        parcel.writeString(contrastLevel)
        parcel.writeString(bestFor)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FilmPreset> {
        override fun createFromParcel(parcel: Parcel): FilmPreset {
            return FilmPreset(parcel)
        }

        override fun newArray(size: Int): Array<FilmPreset?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 胶片系列分类
 */
enum class FilmSeries(val displayName: String, val films: List<String>) {
    CLASSIC("原生经典", listOf("cc", "nc", "nh")),        // CC经典负片, NC自然, NH浓郁
    EMOTION("情绪与表达", listOf("portra", "rdp3")),      // Portra 400, RDP3
    STRUCTURE("结构与时间", listOf("800t", "tx400")),     // 800T, TX400黑白
    DIGITAL("数字记忆", listOf("ccd_cool", "ccd_warm"));   // 冷CCD, 暖CCD

    companion object {
        fun fromString(value: String): FilmSeries = try {
            valueOf(value.uppercase())
        } catch (_: IllegalArgumentException) {
            CLASSIC
        }
    }
}

/**
 * 相机参数
 */
@Serializable
data class CameraParams(
    val iso: Int? = null,
    val shutterSpeed: String? = null,
    val aperture: Float? = null,
    val focalLength: Float? = null,
    val whiteBalance: String? = null,
    val focusMode: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        iso = parcel.readValue(Int::class.java.classLoader) as? Int,
        shutterSpeed = parcel.readString(),
        aperture = parcel.readValue(Float::class.java.classLoader) as? Float,
        focalLength = parcel.readValue(Float::class.java.classLoader) as? Float,
        whiteBalance = parcel.readString(),
        focusMode = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(iso)
        parcel.writeString(shutterSpeed)
        parcel.writeValue(aperture)
        parcel.writeValue(focalLength)
        parcel.writeString(whiteBalance)
        parcel.writeString(focusMode)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CameraParams> {
        override fun createFromParcel(parcel: Parcel): CameraParams {
            return CameraParams(parcel)
        }

        override fun newArray(size: Int): Array<CameraParams?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * EXIF 元数据
 */
@Serializable
data class ExifData(
    val cameraModel: String?,
    val lensModel: String?,
    val focalLength: Float?,
    val fNumber: Float?,
    val exposureTime: String?,
    val iso: Int?,
    val dateTime: String?,
    val gpsLatitude: Double?,
    val gpsLongitude: Double?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        cameraModel = parcel.readString(),
        lensModel = parcel.readString(),
        focalLength = parcel.readValue(Float::class.java.classLoader) as? Float,
        fNumber = parcel.readValue(Float::class.java.classLoader) as? Float,
        exposureTime = parcel.readString(),
        iso = parcel.readValue(Int::class.java.classLoader) as? Int,
        dateTime = parcel.readString(),
        gpsLatitude = parcel.readValue(Double::class.java.classLoader) as? Double,
        gpsLongitude = parcel.readValue(Double::class.java.classLoader) as? Double
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(cameraModel)
        parcel.writeString(lensModel)
        parcel.writeValue(focalLength)
        parcel.writeValue(fNumber)
        parcel.writeString(exposureTime)
        parcel.writeValue(iso)
        parcel.writeString(dateTime)
        parcel.writeValue(gpsLatitude)
        parcel.writeValue(gpsLongitude)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExifData> {
        override fun createFromParcel(parcel: Parcel): ExifData {
            return ExifData(parcel)
        }

        override fun newArray(size: Int): Array<ExifData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 直方图数据
 */
@Serializable
data class HistogramData(
    val luminance: IntArray, // 256 levels
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val meanLuminance: Float,
    val shadowClipping: Boolean,
    val highlightClipping: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        luminance = parcel.createIntArray() ?: IntArray(256),
        red = parcel.createIntArray() ?: IntArray(256),
        green = parcel.createIntArray() ?: IntArray(256),
        blue = parcel.createIntArray() ?: IntArray(256),
        meanLuminance = parcel.readFloat(),
        shadowClipping = parcel.readByte() != 0.toByte(),
        highlightClipping = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeIntArray(luminance)
        parcel.writeIntArray(red)
        parcel.writeIntArray(green)
        parcel.writeIntArray(blue)
        parcel.writeFloat(meanLuminance)
        parcel.writeByte(if (shadowClipping) 1 else 0)
        parcel.writeByte(if (highlightClipping) 1 else 0)
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HistogramData
        if (!luminance.contentEquals(other.luminance)) return false
        if (!red.contentEquals(other.red)) return false
        if (!green.contentEquals(other.green)) return false
        if (!blue.contentEquals(other.blue)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = luminance.contentHashCode()
        result = 31 * result + red.contentHashCode()
        result = 31 * result + green.contentHashCode()
        result = 31 * result + blue.contentHashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<HistogramData> {
        override fun createFromParcel(parcel: Parcel): HistogramData {
            return HistogramData(parcel)
        }

        override fun newArray(size: Int): Array<HistogramData?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 人脸检测数据
 */
@Serializable
data class FaceData(
    val faces: List<FaceInfo>,
    val hasFace: Boolean get() = faces.isNotEmpty()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        faces = parcel.createTypedArrayList(FaceInfo.CREATOR) ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(faces)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FaceData> {
        override fun createFromParcel(parcel: Parcel): FaceData {
            return FaceData(parcel)
        }

        override fun newArray(size: Int): Array<FaceData?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class FaceInfo(
    val bounds: RectData,
    val confidence: Float,
    val hasSmile: Boolean,
    val leftEyeOpen: Boolean,
    val rightEyeOpen: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        bounds = parcel.readParcelable(RectData::class.java.classLoader)
            ?: RectData(0f, 0f, 0f, 0f),
        confidence = parcel.readFloat(),
        hasSmile = parcel.readByte() != 0.toByte(),
        leftEyeOpen = parcel.readByte() != 0.toByte(),
        rightEyeOpen = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bounds, flags)
        parcel.writeFloat(confidence)
        parcel.writeByte(if (hasSmile) 1 else 0)
        parcel.writeByte(if (leftEyeOpen) 1 else 0)
        parcel.writeByte(if (rightEyeOpen) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FaceInfo> {
        override fun createFromParcel(parcel: Parcel): FaceInfo {
            return FaceInfo(parcel)
        }

        override fun newArray(size: Int): Array<FaceInfo?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
data class RectData(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) : Parcelable {
    constructor(parcel: Parcel) : this(
        left = parcel.readFloat(),
        top = parcel.readFloat(),
        right = parcel.readFloat(),
        bottom = parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(left)
        parcel.writeFloat(top)
        parcel.writeFloat(right)
        parcel.writeFloat(bottom)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<RectData> {
        override fun createFromParcel(parcel: Parcel): RectData {
            return RectData(parcel)
        }

        override fun newArray(size: Int): Array<RectData?> {
            return arrayOfNulls(size)
        }
    }
}