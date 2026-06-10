package com.silas.omaster.data.db

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 编辑会话实体
 * 每张图对应一个 EditSession，包含完整参数快照
 */
@Entity(tableName = "edit_sessions")
data class EditSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 图片标识
    val imageUri: String,              // 图片URI或路径
    val imageName: String,             // 图片名称

    // 参数快照 (JSON格式存储)
    val paramsJson: String,            // 所有调节参数

    // 会话状态
    val status: SessionStatus = SessionStatus.IN_PROGRESS,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // 元数据
    val presetId: String? = null,      // 应用的预设ID
    val presetName: String? = null,    // 预设名称
    val thumbnail: String? = null,     // 缩略图路径

    // LUT 相关
    val lutId: String? = null,         // 当前LUT ID
    val lutIntensity: Int = 100,       // LUT强度 (0-100)

    // 暗角相关
    val vignette: Int = 0,             // 暗角强度 (0-100)
    val vignetteShape: String = "circle", // 暗角形状
    val vignetteCenterX: Float = 0.5f, // 暗角中心X
    val vignetteCenterY: Float = 0.5f, // 暗角中心Y

    // 畸变校正
    val distortion: Int = 0,           // 畸变校正 (-100 to 100)

    // 蒙版数据
    val maskData: String? = null       // 蒙版数据 (JSON)
) {
    /**
     * 获取参数Map
     */
    fun getParamsMap(): Map<String, Int> {
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            Gson().fromJson(paramsJson, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /**
     * 格式化日期
     */
    fun formatDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(updatedAt))
    }

    /**
     * 是否为最近编辑（24小时内）
     */
    fun isRecent(): Boolean {
        return System.currentTimeMillis() - updatedAt < 24 * 60 * 60 * 1000
    }
}

/**
 * 会话状态
 */
enum class SessionStatus {
    IN_PROGRESS,   // 编辑中
    COMPLETED,     // 已完成
    EXPORTED       // 已导出
}

/**
 * 编辑会话DAO
 */
@Dao
interface EditSessionDao {
    /**
     * 插入或更新会话
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: EditSession): Long

    /**
     * 更新会话
     */
    @Update
    suspend fun update(session: EditSession)

    /**
     * 删除会话
     */
    @Delete
    suspend fun delete(session: EditSession)

    /**
     * 根据ID获取会话
     */
    @Query("SELECT * FROM edit_sessions WHERE id = :id")
    suspend fun getById(id: Long): EditSession?

    /**
     * 根据图片URI获取会话
     */
    @Query("SELECT * FROM edit_sessions WHERE imageUri = :imageUri LIMIT 1")
    suspend fun getByImageUri(imageUri: String): EditSession?

    /**
     * 获取所有未完成的会话
     */
    @Query("SELECT * FROM edit_sessions WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC")
    suspend fun getInProgressSessions(): List<EditSession>

    /**
     * 获取最近编辑的会话
     */
    @Query("SELECT * FROM edit_sessions ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<EditSession>

    /**
     * 获取所有会话
     */
    @Query("SELECT * FROM edit_sessions ORDER BY updatedAt DESC")
    suspend fun getAllSessions(): List<EditSession>

    /**
     * 获取最近一个未完成的会话
     */
    @Query("SELECT * FROM edit_sessions WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastInProgressSession(): EditSession?

    /**
     * 更新会话状态
     */
    @Query("UPDATE edit_sessions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SessionStatus, updatedAt: Long = System.currentTimeMillis())

    /**
     * 更新参数快照
     */
    @Query("UPDATE edit_sessions SET paramsJson = :paramsJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateParams(id: Long, paramsJson: String, updatedAt: Long = System.currentTimeMillis())

    /**
     * 删除超过指定天数的已完成会话
     */
    @Query("DELETE FROM edit_sessions WHERE status = 'COMPLETED' AND updatedAt < :timestamp")
    suspend fun deleteOldCompletedSessions(timestamp: Long)

    /**
     * 获取会话数量
     */
    @Query("SELECT COUNT(*) FROM edit_sessions")
    suspend fun getCount(): Int

    /**
     * 清空所有会话
     */
    @Query("DELETE FROM edit_sessions")
    suspend fun clearAll()

    /**
     * Flow: 监听所有会话变化
     */
    @Query("SELECT * FROM edit_sessions ORDER BY updatedAt DESC")
    fun observeAllSessions(): kotlinx.coroutines.flow.Flow<List<EditSession>>

    /**
     * Flow: 监听未完成会话变化
     */
    @Query("SELECT * FROM edit_sessions WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC")
    fun observeInProgressSessions(): kotlinx.coroutines.flow.Flow<List<EditSession>>

    /**
     * Flow: 监听最近会话变化
     */
    @Query("SELECT * FROM edit_sessions ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 10): kotlinx.coroutines.flow.Flow<List<EditSession>>
}

/**
 * 配方历史实体（与 RecipeHistoryManager 统一）
 */
@Entity(tableName = "recipe_history")
data class RecipeHistoryEntity(
    @PrimaryKey
    val id: String,

    val sceneId: String,
    val sceneName: String,
    val sceneCategory: String,
    val filmId: String?,
    val filmName: String?,
    val timestamp: Long,
    val confidence: Float = 0.85f,
    val thumbnail: String? = null
)

/**
 * 配方历史DAO
 */
@Dao
interface RecipeHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeHistoryEntity)

    @Delete
    suspend fun delete(recipe: RecipeHistoryEntity)

    @Query("SELECT * FROM recipe_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecipeHistoryEntity>

    @Query("SELECT * FROM recipe_history WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    suspend fun getByTimeRange(startTime: Long): List<RecipeHistoryEntity>

    @Query("DELETE FROM recipe_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM recipe_history")
    suspend fun getCount(): Int

    @Query("SELECT * FROM recipe_history ORDER BY timestamp DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<RecipeHistoryEntity>>
}
