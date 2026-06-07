package com.silas.omaster.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * 颜色信息
 * 参考 iCurrer/OMaster ColorInfo
 */
@Serializable
data class ColorInfo(
    val hex: String,
    val name: String,
    val role: ColorRole
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        ColorRole.valueOf(parcel.readString() ?: "ACCENT")
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(hex)
        parcel.writeString(name)
        parcel.writeString(role.name)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ColorInfo> {
        override fun createFromParcel(parcel: Parcel): ColorInfo = ColorInfo(parcel)
        override fun newArray(size: Int): Array<ColorInfo?> = arrayOfNulls(size)
    }
}

/**
 * 颜色角色
 */
@Serializable
enum class ColorRole {
    PRIMARY,    // 主色
    ACCENT      // 强调色
}

/**
 * 色卡
 * 参考 iCurrer/OMaster ColorCard
 */
@Serializable
data class ColorCard(
    val id: String,
    val colors: List<ColorInfo>,
    val theme: String,
    val desc: String,
    val tips: String,
    val challenge: String,
    val tags: List<String>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.createTypedArrayList(ColorInfo.CREATOR) ?: emptyList(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeTypedList(colors)
        parcel.writeString(theme)
        parcel.writeString(desc)
        parcel.writeString(tips)
        parcel.writeString(challenge)
        parcel.writeStringList(tags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ColorCard> {
        override fun createFromParcel(parcel: Parcel): ColorCard = ColorCard(parcel)
        override fun newArray(size: Int): Array<ColorCard?> = arrayOfNulls(size)
    }
}
