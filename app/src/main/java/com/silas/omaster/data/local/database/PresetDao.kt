package com.silas.omaster.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 预设数据访问对象
 */
@Dao
interface PresetDao {

    @Query("SELECT * FROM presets ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoritePresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isCustom = 1 ORDER BY createdAt DESC")
    fun getCustomPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE brand = :brand ORDER BY isPinned DESC, createdAt DESC")
    fun getPresetsByBrand(brand: String): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isHncs = 1 ORDER BY createdAt DESC")
    fun getHncsPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE isNew = 1 ORDER BY createdAt DESC")
    fun getNewPresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE name LIKE '%' || :query || '%' OR tagsJson LIKE '%' || :query || '%'")
    suspend fun searchPresets(query: String): List<PresetEntity>

    @Query("SELECT * FROM presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<PresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Update
    suspend fun updatePreset(preset: PresetEntity)

    @Query("UPDATE presets SET isFavorite = :isFavorite WHERE id = :presetId")
    suspend fun updateFavoriteStatus(presetId: String, isFavorite: Boolean)

    @Query("DELETE FROM presets WHERE id = :presetId")
    suspend fun deletePreset(presetId: String)

    @Query("DELETE FROM presets WHERE isCustom = 1")
    suspend fun deleteAllCustomPresets()

    @Query("SELECT COUNT(*) FROM presets")
    suspend fun getPresetCount(): Int

    @Query("DELETE FROM presets")
    suspend fun deleteAllPresets()
}
