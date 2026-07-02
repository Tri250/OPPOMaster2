package com.silas.omaster.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.model.PresetDescription
import com.silas.omaster.model.PresetSection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room 预设实体
 * 用于本地持久化预设数据，支持离线浏览和收藏
 */
@Entity(tableName = "presets")
@TypeConverters(PresetConverters::class)
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val author: String,
    val coverPath: String,
    val mode: String?,
    val descriptionJson: String?,
    val tagsJson: String?,
    val paramsJson: String?,
    val brand: String?,
    val isFavorite: Boolean = false,
    val isHncs: Boolean = false,
    val isNew: Boolean = false,
    val isCustom: Boolean = false,
    val isPinned: Boolean = false,
    val rating: Float? = null,
    val downloads: Int? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val styleTagsJson: String? = null,
    val sceneTagsJson: String? = null
) {
    fun toMasterPreset(): MasterPreset {
        return MasterPreset(
            id = id,
            name = name,
            author = author,
            coverPath = coverPath,
            mode = mode,
            description = descriptionJson?.let { Json.decodeFromString(it) },
            tags = tagsJson?.let { Json.decodeFromString(it) },
            params = paramsJson?.let { Json.decodeFromString(it) } ?: emptyMap(),
            brand = brand,
            isFavorite = isFavorite,
            isHncs = isHncs,
            isNew = isNew,
            isCustom = isCustom,
            isPinned = isPinned,
            rating = rating,
            downloads = downloads,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromMasterPreset(preset: MasterPreset): PresetEntity {
            return PresetEntity(
                id = preset.id ?: "${preset.name}_${System.currentTimeMillis()}",
                name = preset.name,
                author = preset.author,
                coverPath = preset.coverPath,
                mode = preset.mode,
                descriptionJson = preset.description?.let { Json.encodeToString(it) },
                tagsJson = preset.tags?.let { Json.encodeToString(it) },
                paramsJson = preset.params.let { Json.encodeToString(it) },
                brand = preset.brand,
                isFavorite = preset.isFavorite,
                isHncs = preset.isHncs,
                isNew = preset.isNew,
                isCustom = preset.isCustom,
                isPinned = preset.isPinned,
                rating = preset.rating,
                downloads = preset.downloads,
                createdAt = preset.createdAt ?: 0L,
                updatedAt = preset.updatedAt ?: 0L
            )
        }
    }
}
