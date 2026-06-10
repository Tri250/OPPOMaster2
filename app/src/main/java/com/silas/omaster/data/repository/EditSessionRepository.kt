package com.silas.omaster.data.repository

import android.content.Context
import com.google.gson.Gson
import com.silas.omaster.data.db.*
import com.silas.omaster.data.local.RecipeRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 编辑会话仓库
 * 管理编辑会话的持久化和恢复
 */
class EditSessionRepository private constructor(context: Context) {
    private val database = OMasterDatabase.getInstance(context)
    private val editSessionDao = database.editSessionDao()
    private val recipeHistoryDao = database.recipeHistoryDao()
    private val gson = Gson()

    companion object {
        @Volatile
        private var instance: EditSessionRepository? = null

        fun getInstance(context: Context): EditSessionRepository {
            return instance ?: synchronized(this) {
                instance ?: EditSessionRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    // ==================== 编辑会话操作 ====================

    /**
     * 创建新的编辑会话
     */
    suspend fun createSession(
        imageUri: String,
        imageName: String,
        params: Map<String, Int> = emptyMap()
    ): EditSession {
        val session = EditSession(
            imageUri = imageUri,
            imageName = imageName,
            paramsJson = gson.toJson(params),
            status = SessionStatus.IN_PROGRESS
        )
        val id = editSessionDao.insert(session)
        return session.copy(id = id)
    }

    /**
     * 保存/更新编辑会话
     */
    suspend fun saveSession(session: EditSession): Long {
        return editSessionDao.insert(session.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * 更新会话参数
     */
    suspend fun updateSessionParams(
        sessionId: Long,
        params: Map<String, Int>
    ) {
        editSessionDao.updateParams(
            id = sessionId,
            paramsJson = gson.toJson(params)
        )
    }

    /**
     * 获取指定图片的会话（用于恢复编辑）
     */
    suspend fun getSessionByImageUri(imageUri: String): EditSession? {
        return editSessionDao.getByImageUri(imageUri)
    }

    /**
     * 获取最近未完成的会话（App重启后恢复）
     */
    suspend fun getLastInProgressSession(): EditSession? {
        return editSessionDao.getLastInProgressSession()
    }

    /**
     * 获取所有未完成的会话
     */
    suspend fun getInProgressSessions(): List<EditSession> {
        return editSessionDao.getInProgressSessions()
    }

    /**
     * 获取最近编辑的会话
     */
    suspend fun getRecentSessions(limit: Int = 10): List<EditSession> {
        return editSessionDao.getRecentSessions(limit)
    }

    /**
     * 标记会话为已完成
     */
    suspend fun markSessionCompleted(sessionId: Long) {
        editSessionDao.updateStatus(sessionId, SessionStatus.COMPLETED)
    }

    /**
     * 标记会话为已导出
     */
    suspend fun markSessionExported(sessionId: Long) {
        editSessionDao.updateStatus(sessionId, SessionStatus.EXPORTED)
    }

    /**
     * 删除会话
     */
    suspend fun deleteSession(session: EditSession) {
        editSessionDao.delete(session)
    }

    /**
     * 清理旧的已完成会话（超过30天）
     */
    suspend fun cleanupOldSessions() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        editSessionDao.deleteOldCompletedSessions(thirtyDaysAgo)
    }

    /**
     * 获取会话数量
     */
    suspend fun getSessionCount(): Int {
        return editSessionDao.getCount()
    }

    // Flow 观察方法

    /**
     * 观察所有会话
     */
    fun observeAllSessions(): Flow<List<EditSession>> {
        return editSessionDao.observeAllSessions()
    }

    /**
     * 观察未完成会话
     */
    fun observeInProgressSessions(): Flow<List<EditSession>> {
        return editSessionDao.observeInProgressSessions()
    }

    /**
     * 观察最近会话
     */
    fun observeRecentSessions(limit: Int = 10): Flow<List<EditSession>> {
        return editSessionDao.observeRecentSessions(limit)
    }

    // ==================== 配方历史操作（与 RecipeHistoryManager 统一） ====================

    /**
     * 添加配方记录
     */
    suspend fun addRecipeRecord(record: RecipeRecord) {
        val entity = RecipeHistoryEntity(
            id = record.id,
            sceneId = record.sceneId,
            sceneName = record.sceneName,
            sceneCategory = record.sceneCategory,
            filmId = record.filmId,
            filmName = record.filmName,
            timestamp = record.timestamp,
            confidence = record.confidence,
            thumbnail = record.thumbnail
        )
        recipeHistoryDao.insert(entity)
    }

    /**
     * 获取所有配方记录
     */
    suspend fun getAllRecipes(): List<RecipeRecord> {
        return recipeHistoryDao.getAll().map { entity ->
            RecipeRecord(
                id = entity.id,
                sceneId = entity.sceneId,
                sceneName = entity.sceneName,
                sceneCategory = entity.sceneCategory,
                filmId = entity.filmId,
                filmName = entity.filmName,
                timestamp = entity.timestamp,
                confidence = entity.confidence,
                thumbnail = entity.thumbnail
            )
        }
    }

    /**
     * 根据时间范围获取配方记录
     */
    suspend fun getRecipesByTimeRange(timeRange: String): List<RecipeRecord> {
        val now = System.currentTimeMillis()
        val startTime = when (timeRange) {
            "week" -> now - 7L * 24 * 60 * 60 * 1000
            "month" -> now - 30L * 24 * 60 * 60 * 1000
            "year" -> now - 365L * 24 * 60 * 60 * 1000
            "all" -> 0L
            else -> 0L
        }
        return recipeHistoryDao.getByTimeRange(startTime).map { entity ->
            RecipeRecord(
                id = entity.id,
                sceneId = entity.sceneId,
                sceneName = entity.sceneName,
                sceneCategory = entity.sceneCategory,
                filmId = entity.filmId,
                filmName = entity.filmName,
                timestamp = entity.timestamp,
                confidence = entity.confidence,
                thumbnail = entity.thumbnail
            )
        }
    }

    /**
     * 删除配方记录
     */
    suspend fun deleteRecipe(recipeId: String) {
        val recipes = recipeHistoryDao.getAll()
        recipes.find { it.id == recipeId }?.let {
            recipeHistoryDao.delete(it)
        }
    }

    /**
     * 清空配方历史
     */
    suspend fun clearAllRecipes() {
        recipeHistoryDao.clearAll()
    }

    /**
     * 获取配方数量
     */
    suspend fun getRecipeCount(): Int {
        return recipeHistoryDao.getCount()
    }

    /**
     * 观察配方历史
     */
    fun observeRecipes(): Flow<List<RecipeRecord>> {
        return recipeHistoryDao.observeAll().map { entities ->
            entities.map { entity ->
                RecipeRecord(
                    id = entity.id,
                    sceneId = entity.sceneId,
                    sceneName = entity.sceneName,
                    sceneCategory = entity.sceneCategory,
                    filmId = entity.filmId,
                    filmName = entity.filmName,
                    timestamp = entity.timestamp,
                    confidence = entity.confidence,
                    thumbnail = entity.thumbnail
                )
            }
        }
    }
}
