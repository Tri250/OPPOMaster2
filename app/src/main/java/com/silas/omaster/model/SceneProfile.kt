package com.silas.omaster.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * Layer 1: 大师数据 (Master Data)
 * 统一 SceneProfile 模型 - 场景画像
 * 用于端到端的大师工作流
 */
@Serializable
data class SceneProfile(
    val id: String,
    val sceneHierarchy: SceneHierarchy, // 三级场景体系
    val hasselbladParams: HasselbladParams, // 哈苏参数体系
    val filmRecipe: FilmRecipe?, // 胶片配方
    val exifData: ExifData?, // EXIF元数据
    val histogramData: HistogramData?, // 直方图数据
    val faceData: FaceData?, // 人脸检测数据
    val confidence: Float = 0f, // 置信度
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        sceneHierarchy = parcel.readParcelable(SceneHierarchy::class.java.classLoader)!!,
        hasselbladParams = parcel.readParcelable(HasselbladParams::class.java.classLoader)!!,
        filmRecipe = parcel.readParcelable(FilmRecipe::class.java.classLoader),
        exifData = parcel.readParcelable(ExifData::class.java.classLoader),
        histogramData = parcel.readParcelable(HistogramData::class.java.classLoader),
        faceData = parcel.readParcelable(FaceData::class.java.classLoader),
        confidence = parcel.readFloat(),
        timestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeParcelable(sceneHierarchy, flags)
        parcel.writeParcelable(hasselbladParams, flags)
        parcel.writeParcelable(filmRecipe, flags)
        parcel.writeParcelable(exifData, flags)
        parcel.writeParcelable(histogramData, flags)
        parcel.writeParcelable(faceData, flags)
        parcel.writeFloat(confidence)
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
 * 三级场景体系
 * 大类 -> 细分 -> 精细
 */
@Serializable
data class SceneHierarchy(
    val primary: PrimaryScene, // 大类：人像/风景/静物/街拍
    val secondary: SecondaryScene, // 细分：室内人像/户外人像/夜景人像
    val fine: FineScene // 精细：咖啡馆人像/逆光人像/黄金时刻人像
) : Parcelable {
    constructor(parcel: Parcel) : this(
        primary = PrimaryScene.valueOf(parcel.readString() ?: PrimaryScene.PORTRAIT.name),
        secondary = SecondaryScene.valueOf(parcel.readString() ?: SecondaryScene.OUTDOOR_PORTRAIT.name),
        fine = FineScene.valueOf(parcel.readString() ?: FineScene.GOLDEN_HOUR.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(primary.name)
        parcel.writeString(secondary.name)
        parcel.writeString(fine.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SceneHierarchy> {
        override fun createFromParcel(parcel: Parcel): SceneHierarchy {
            return SceneHierarchy(parcel)
        }

        override fun newArray(size: Int): Array<SceneHierarchy?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 一级场景：大类
 */
enum class PrimaryScene(val displayName: String, val icon: String) {
    PORTRAIT("人像", "👤"),
    LANDSCAPE("风景", "🏔️"),
    STILL_LIFE("静物", "🍃"),
    STREET("街拍", "🚶"),
    NIGHT("夜景", "🌃"),
    ARCHITECTURE("建筑", "🏢"),
    MACRO("微距", "🔍"),
    EVENT("活动", "🎉")
}

/**
 * 二级场景：细分
 */
enum class SecondaryScene(val displayName: String, val primary: PrimaryScene) {
    // 人像细分
    INDOOR_PORTRAIT("室内人像", PrimaryScene.PORTRAIT),
    OUTDOOR_PORTRAIT("户外人像", PrimaryScene.PORTRAIT),
    NIGHT_PORTRAIT("夜景人像", PrimaryScene.PORTRAIT),
    ENVIRONMENTAL_PORTRAIT("环境人像", PrimaryScene.PORTRAIT),
    
    // 风景细分
    NATURAL_LANDSCAPE("自然风光", PrimaryScene.LANDSCAPE),
    URBAN_LANDSCAPE("城市风光", PrimaryScene.LANDSCAPE),
    SEASCAPE("海景", PrimaryScene.LANDSCAPE),
    MOUNTAIN("山景", PrimaryScene.LANDSCAPE),
    
    // 静物细分
    FOOD("美食", PrimaryScene.STILL_LIFE),
    PRODUCT("产品", PrimaryScene.STILL_LIFE),
    FLORA("花卉植物", PrimaryScene.STILL_LIFE),
    
    // 街拍细分
    URBAN_STREET("城市街拍", PrimaryScene.STREET),
    DOCUMENTARY("纪实", PrimaryScene.STREET),
    CANDID("抓拍", PrimaryScene.STREET)
}

/**
 * 三级场景：精细
 */
enum class FineScene(val displayName: String, val secondary: SecondaryScene) {
    // 室内人像精细
    CAFE_PORTRAIT("咖啡馆人像", SecondaryScene.INDOOR_PORTRAIT),
    STUDIO_PORTRAIT("影棚人像", SecondaryScene.INDOOR_PORTRAIT),
    HOME_PORTRAIT("居家人像", SecondaryScene.INDOOR_PORTRAIT),
    
    // 户外人像精细
    GOLDEN_HOUR("黄金时刻", SecondaryScene.OUTDOOR_PORTRAIT),
    BACKLIGHT_PORTRAIT("逆光人像", SecondaryScene.OUTDOOR_PORTRAIT),
    OVERCAST_PORTRAIT("阴天人像", SecondaryScene.OUTDOOR_PORTRAIT),
    
    // 夜景人像精细
    NEON_PORTRAIT("霓虹人像", SecondaryScene.NIGHT_PORTRAIT),
    BOKEH_PORTRAIT("光斑人像", SecondaryScene.NIGHT_PORTRAIT),
    
    // 自然风光精细
    SUNRISE_SUNSET("日出日落", SecondaryScene.NATURAL_LANDSCAPE),
    BLUE_HOUR("蓝调时刻", SecondaryScene.NATURAL_LANDSCAPE),
    MISTY("雾气朦胧", SecondaryScene.NATURAL_LANDSCAPE),
    
    // 城市风光精细
    CITY_NIGHT("城市夜景", SecondaryScene.URBAN_LANDSCAPE),
    ROOFTOP("天台视角", SecondaryScene.URBAN_LANDSCAPE),
    STREET_VIEW("街景", SecondaryScene.URBAN_LANDSCAPE)
}

/**
 * 哈苏参数体系 (HNCS - Hasselblad Natural Color Solution)
 * 与一加/OPPO/Realme 大师模式参数对齐
 */
@Serializable
data class HasselbladParams(
    // 曝光三要素
    val iso: Int = 100,
    val shutterSpeed: String = "1/125",
    val aperture: Float = 2.8f,
    
    // 白平衡与色温
    val whiteBalance: WhiteBalanceMode = WhiteBalanceMode.AUTO,
    val colorTemperature: Int = 5500, // Kelvin
    val tint: Int = 0, // -150 to +150
    
    // HNCS 色彩参数
    val saturation: Int = 0, // -100 to +100
    val contrast: Int = 0, // -100 to +100
    val brightness: Int = 0, // -100 to +100
    val highlights: Int = 0, // -100 to +100
    val shadows: Int = 0, // -100 to +100
    val clarity: Int = 0, // 0 to 100 (结构/清晰度)
    
    // 哈苏特色参数
    val toneCurve: ToneCurve = ToneCurve.LINEAR,
    val colorProfile: HasselbladColorProfile = HasselbladColorProfile.HNCS,
    
    // 胶片模拟参数
    val filmGrain: Int = 0, // 0 to 100
    val vignette: VignetteStyle = VignetteStyle.NONE
) : Parcelable {
    constructor(parcel: Parcel) : this(
        iso = parcel.readInt(),
        shutterSpeed = parcel.readString() ?: "1/125",
        aperture = parcel.readFloat(),
        whiteBalance = WhiteBalanceMode.valueOf(parcel.readString() ?: WhiteBalanceMode.AUTO.name),
        colorTemperature = parcel.readInt(),
        tint = parcel.readInt(),
        saturation = parcel.readInt(),
        contrast = parcel.readInt(),
        brightness = parcel.readInt(),
        highlights = parcel.readInt(),
        shadows = parcel.readInt(),
        clarity = parcel.readInt(),
        toneCurve = ToneCurve.valueOf(parcel.readString() ?: ToneCurve.LINEAR.name),
        colorProfile = HasselbladColorProfile.valueOf(parcel.readString() ?: HasselbladColorProfile.HNCS.name),
        filmGrain = parcel.readInt(),
        vignette = VignetteStyle.valueOf(parcel.readString() ?: VignetteStyle.NONE.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(iso)
        parcel.writeString(shutterSpeed)
        parcel.writeFloat(aperture)
        parcel.writeString(whiteBalance.name)
        parcel.writeInt(colorTemperature)
        parcel.writeInt(tint)
        parcel.writeInt(saturation)
        parcel.writeInt(contrast)
        parcel.writeInt(brightness)
        parcel.writeInt(highlights)
        parcel.writeInt(shadows)
        parcel.writeInt(clarity)
        parcel.writeString(toneCurve.name)
        parcel.writeString(colorProfile.name)
        parcel.writeInt(filmGrain)
        parcel.writeString(vignette.name)
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
}

enum class WhiteBalanceMode(val displayName: String, val kelvin: Int?) {
    AUTO("自动", null),
    DAYLIGHT("日光", 5500),
    CLOUDY("阴天", 6500),
    SHADE("阴影", 7500),
    TUNGSTEN("钨丝灯", 3200),
    FLUORESCENT("荧光灯", 4000),
    CUSTOM("自定义", null)
}

enum class ToneCurve(val displayName: String) {
    LINEAR("线性"),
    SOFT("柔和"),
    HARD("硬朗"),
    HIGH_CONTRAST("高对比"),
    FILM_LIKE("胶片感")
}

enum class HasselbladColorProfile(val displayName: String, val description: String) {
    HNCS("HNCS", "哈苏自然色彩解决方案"),
    VIVID("鲜艳", "高饱和度色彩"),
    PORTRAIT("人像", "肤色优化"),
    LANDSCAPE("风景", "自然风景优化"),
    MONOCHROME("黑白", "经典黑白"),
    SEPIA(" sepia", "复古 sepia")
}

enum class VignetteStyle(val displayName: String) {
    NONE("无"),
    LIGHT("轻微"),
    MEDIUM("中等"),
    HEAVY("强烈"),
    FILM_LIKE("胶片感")
}

/**
 * 胶片配方 (Film Recipe)
 * 9款经典胶片风格映射
 */
@Serializable
data class FilmRecipe(
    val filmStock: FilmStock,
    val pushPull: Int = 0, // -2 to +2 stops
    val customAdjustments: Map<String, Float> = emptyMap()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        filmStock = FilmStock.valueOf(parcel.readString() ?: FilmStock.KODAK_PORTRA_400.name),
        pushPull = parcel.readInt(),
        customAdjustments = emptyMap() // Map需要特殊处理
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(filmStock.name)
        parcel.writeInt(pushPull)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<FilmRecipe> {
        override fun createFromParcel(parcel: Parcel): FilmRecipe {
            return FilmRecipe(parcel)
        }

        override fun newArray(size: Int): Array<FilmRecipe?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * 9款经典胶片风格
 */
enum class FilmStock(
    val displayName: String,
    val brand: String,
    val iso: Int,
    val characteristics: String
) {
    // Kodak 系列
    KODAK_PORTRA_400("Portra 400", "Kodak", 400, "柔和肤色，自然色彩"),
    KODAK_PORTRA_160("Portra 160", "Kodak", 160, "细腻颗粒，婚礼人像首选"),
    KODAK_EKTAR_100("Ektar 100", "Kodak", 100, "鲜艳饱和，风景专用"),
    KODAK_GOLD_200("Gold 200", "Kodak", 200, "暖调复古，日常记录"),
    KODAK_TRI_X_400("Tri-X 400", "Kodak", 400, "经典黑白，颗粒粗犷"),
    
    // Fujifilm 系列
    FUJI_PRO_400H("Pro 400H", "Fujifilm", 400, "柔和对比，日系清新"),
    FUJI_VELVIA_50("Velvia 50", "Fujifilm", 50, "极高饱和，反转片质感"),
    FUJI_ACROS_100("Acros 100", "Fujifilm", 100, "细腻黑白，丰富层次"),
    FUJI_C200("C200", "Fujifilm", 200, "平价入门，清新色调")
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
        if (meanLuminance != other.meanLuminance) return false
        if (shadowClipping != other.shadowClipping) return false
        if (highlightClipping != other.highlightClipping) return false

        return true
    }

    override fun hashCode(): Int {
        var result = luminance.contentHashCode()
        result = 31 * result + red.contentHashCode()
        result = 31 * result + green.contentHashCode()
        result = 31 * result + blue.contentHashCode()
        result = 31 * result + meanLuminance.hashCode()
        result = 31 * result + shadowClipping.hashCode()
        result = 31 * result + highlightClipping.hashCode()
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
    val bounds: RectData, // 人脸位置
    val confidence: Float,
    val hasSmile: Boolean,
    val leftEyeOpen: Boolean,
    val rightEyeOpen: Boolean,
    val yawAngle: Float, // 头部左右转动
    val rollAngle: Float, // 头部倾斜
    val pitchAngle: Float // 头部上下转动
) : Parcelable {
    constructor(parcel: Parcel) : this(
        bounds = parcel.readParcelable(RectData::class.java.classLoader)!!,
        confidence = parcel.readFloat(),
        hasSmile = parcel.readByte() != 0.toByte(),
        leftEyeOpen = parcel.readByte() != 0.toByte(),
        rightEyeOpen = parcel.readByte() != 0.toByte(),
        yawAngle = parcel.readFloat(),
        rollAngle = parcel.readFloat(),
        pitchAngle = parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(bounds, flags)
        parcel.writeFloat(confidence)
        parcel.writeByte(if (hasSmile) 1 else 0)
        parcel.writeByte(if (leftEyeOpen) 1 else 0)
        parcel.writeByte(if (rightEyeOpen) 1 else 0)
        parcel.writeFloat(yawAngle)
        parcel.writeFloat(rollAngle)
        parcel.writeFloat(pitchAngle)
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
