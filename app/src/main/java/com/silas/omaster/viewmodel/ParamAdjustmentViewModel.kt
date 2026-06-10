package com.silas.omaster.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.silas.omaster.data.db.EditSession
import com.silas.omaster.data.db.SessionStatus
import com.silas.omaster.data.repository.EditSessionRepository
import com.silas.omaster.param.AdjustableParam
import com.silas.omaster.param.ParamAdjustmentManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 参数调节 ViewModel
 * 管理参数状态，支持持久化和跨会话恢复
 */
class ParamAdjustmentViewModel(application: Application) : AndroidViewModel(application) {
    private val paramManager = ParamAdjustmentManager.getInstance(application)
    private val repository = EditSessionRepository.getInstance(application)
    private val gson = Gson()

    // 当前编辑会话
    private val _currentSession = MutableStateFlow<EditSession?>(null)
    val currentSession: StateFlow<EditSession?> = _currentSession.asStateFlow()

    // 参数值（从 ParamAdjustmentManager 同步）
    val paramValues: StateFlow<Map<String, Int>> = paramManager.paramValues

    // 是否有未保存的修改
    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    // 最近编辑会话列表
    val recentSessions: Flow<List<EditSession>> = repository.observeRecentSessions(20)

    // 未完成的编辑会话
    val inProgressSessions: Flow<List<EditSession>> = repository.observeInProgressSessions()

    // 初始化：尝试恢复上次未完成的会话
    init {
        viewModelScope.launch {
            restoreLastSession()
        }
    }

    /**
     * 开始新的编辑会话
     */
    fun startNewSession(
        imageUri: String,
        imageName: String,
        initialParams: Map<String, Int> = emptyMap()
    ) {
        viewModelScope.launch {
            // 先保存当前会话（如果有）
            saveCurrentSession()

            // 检查是否已有该图片的会话
            val existingSession = repository.getSessionByImageUri(imageUri)
            if (existingSession != null) {
                // 恢复已有会话
                restoreSession(existingSession)
            } else {
                // 创建新会话
                val session = repository.createSession(imageUri, imageName, initialParams)
                _currentSession.value = session

                // 应用初始参数
                initialParams.forEach { (paramName, value) ->
                    paramManager.adjustParam(paramName, value.toFloat())
                }
            }
            _hasUnsavedChanges.value = false
        }
    }

    /**
     * 恢复会话
     */
    fun restoreSession(session: EditSession) {
        viewModelScope.launch {
            _currentSession.value = session

            // 恢复参数值
            val params = session.getParamsMap()
            params.forEach { (paramName, value) ->
                paramManager.adjustParam(paramName, value.toFloat())
            }

            _hasUnsavedChanges.value = false
        }
    }

    /**
     * 恢复上次未完成的会话（App重启后）
     */
    private suspend fun restoreLastSession() {
        val lastSession = repository.getLastInProgressSession()
        if (lastSession != null) {
            restoreSession(lastSession)
        }
    }

    /**
     * 调节参数
     */
    fun adjustParam(paramName: String, value: Int) {
        paramManager.adjustParam(paramName, value.toFloat())
        _hasUnsavedChanges.value = true

        // 自动保存（防抖）
        autoSaveSession()
    }

    /**
     * 批量调节参数
     */
    fun adjustParams(params: Map<String, Int>) {
        params.forEach { (paramName, value) ->
            paramManager.adjustParam(paramName, value.toFloat())
        }
        _hasUnsavedChanges.value = true
        autoSaveSession()
    }

    /**
     * 重置参数
     */
    fun resetParam(paramName: String) {
        paramManager.resetParam(paramName)
        _hasUnsavedChanges.value = true
        autoSaveSession()
    }

    /**
     * 重置所有参数
     */
    fun resetAllParams() {
        paramManager.adjustableParams.forEach { param: AdjustableParam ->
            paramManager.adjustParam(param.name, 0f)
        }
        _hasUnsavedChanges.value = true
        autoSaveSession()
    }

    /**
     * 应用预设
     */
    fun applyPreset(presetId: String, presetName: String, params: Map<String, Int>) {
        adjustParams(params)

        viewModelScope.launch {
            _currentSession.value?.let { session ->
                _currentSession.value = session.copy(
                    presetId = presetId,
                    presetName = presetName
                )
            }
        }
    }

    /**
     * 设置LUT
     */
    fun setLUT(lutId: String?, intensity: Int = 100) {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                _currentSession.value = session.copy(
                    lutId = lutId,
                    lutIntensity = intensity
                )
            }
            _hasUnsavedChanges.value = true
            autoSaveSession()
        }
    }

    /**
     * 设置暗角
     */
    fun setVignette(
        intensity: Int,
        shape: String = "circle",
        centerX: Float = 0.5f,
        centerY: Float = 0.5f
    ) {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                _currentSession.value = session.copy(
                    vignette = intensity,
                    vignetteShape = shape,
                    vignetteCenterX = centerX,
                    vignetteCenterY = centerY
                )
            }
            _hasUnsavedChanges.value = true
            autoSaveSession()
        }
    }

    /**
     * 设置畸变校正
     */
    fun setDistortion(value: Int) {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                _currentSession.value = session.copy(distortion = value)
            }
            _hasUnsavedChanges.value = true
            autoSaveSession()
        }
    }

    /**
     * 自动保存会话（防抖）
     */
    private var saveJob: kotlinx.coroutines.Job? = null
    private fun autoSaveSession() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // 1秒防抖
            saveCurrentSession()
        }
    }

    /**
     * 保存当前会话
     */
    fun saveCurrentSession() {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                val updatedSession = session.copy(
                    paramsJson = gson.toJson(paramManager.getAllParamValues()),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveSession(updatedSession)
                _currentSession.value = updatedSession
                _hasUnsavedChanges.value = false
            }
        }
    }

    /**
     * 完成当前会话
     */
    fun completeSession() {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                saveCurrentSession()
                repository.markSessionCompleted(session.id)
                _currentSession.value = null
            }
        }
    }

    /**
     * 导出当前会话
     */
    fun exportSession() {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                saveCurrentSession()
                repository.markSessionExported(session.id)
            }
        }
    }

    /**
     * 删除会话
     */
    fun deleteSession(session: EditSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_currentSession.value?.id == session.id) {
                _currentSession.value = null
            }
        }
    }

    /**
     * 清理旧会话
     */
    fun cleanupOldSessions() {
        viewModelScope.launch {
            repository.cleanupOldSessions()
        }
    }

    /**
     * 获取参数值
     */
    fun getParamValue(paramName: String): Int {
        return paramManager.getParamValue(paramName)
    }

    /**
     * 获取所有参数值
     */
    fun getAllParamValues(): Map<String, Int> {
        return paramManager.getAllParamValues()
    }

    /**
     * 检查参数是否被手动修改
     */
    fun isParamManuallyModified(paramName: String): Boolean {
        return paramManager.isParamManuallyModified(paramName)
    }

    /**
     * 标记参数为手动修改
     */
    fun markParamAsManuallyModified(paramName: String) {
        paramManager.markAsManuallyModified(paramName)
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel销毁时保存当前会话
        saveJob?.cancel()
        _currentSession.value?.let { session ->
            viewModelScope.launch {
                val updatedSession = session.copy(
                    paramsJson = gson.toJson(paramManager.getAllParamValues()),
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveSession(updatedSession)
            }
        }
    }
}

/**
 * 编辑会话列表 ViewModel
 */
class EditSessionListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EditSessionRepository.getInstance(application)

    // 所有会话
    val allSessions: Flow<List<EditSession>> = repository.observeAllSessions()

    // 未完成的会话
    val inProgressSessions: Flow<List<EditSession>> = repository.observeInProgressSessions()

    // 最近会话
    val recentSessions: Flow<List<EditSession>> = repository.observeRecentSessions(20)

    /**
     * 删除会话
     */
    fun deleteSession(session: EditSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    /**
     * 清空所有会话
     */
    fun clearAllSessions() {
        viewModelScope.launch {
            repository.cleanupOldSessions()
        }
    }

    /**
     * 获取会话数量
     */
    suspend fun getSessionCount(): Int {
        return repository.getSessionCount()
    }
}
