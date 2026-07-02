package com.silas.omaster.data.local.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room 类型转换器
 * 用于将复杂类型转换为数据库可存储的字符串
 */
class PresetConverters {
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String? {
        return map?.let { Json.encodeToString(it) }
    }
}
