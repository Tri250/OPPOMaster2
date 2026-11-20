package com.silas.omaster.ui.discover

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.silas.omaster.data.local.CheckInManager
import com.silas.omaster.data.local.ColorCardHistory
import com.silas.omaster.data.local.ColorWalkManager
import com.silas.omaster.model.ColorCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ColorWalkUiState {
    data object Normal : ColorWalkUiState()
    data object Guide : ColorWalkUiState()
    data object HistorySheet : ColorWalkUiState()
    data class HistoryPreview(val card: ColorCard, val date: String) : ColorWalkUiState()
    data object ClearConfirm : ColorWalkUiState()
}

class ColorWalkViewModel(
    private val colorWalkManager: ColorWalkManager,
    private val checkInManager: CheckInManager
) : ViewModel() {

    private val _state = MutableStateFlow(ColorWalkState())
    val state: StateFlow<ColorWalkState> = _state.asStateFlow()

    private val _uiState = MutableStateFlow<ColorWalkUiState>(ColorWalkUiState.Normal)
    val uiState: StateFlow<ColorWalkUiState> = _uiState.asStateFlow()

    init {
        // 【v1.7.7 P1 修复】此前仅恢复 todayCard，history/drawCount 停留在初始空值，
        // 且 loadTodayCard() 全项目无调用方 → 有历史记录但当日未抽卡时，
        // 历史入口不显示、历史面板不可达。改为入口即完整恢复全部状态。
        loadTodayCard()
    }

    /**
     * 抽卡（首抽）
     *
     * 【v1.7.7 P2 修复】此前由 UI 传入 themeName，首抽传空串 → 打卡详情主题为空；
     * 重抽传的是"旧卡"主题 → 与 cardId 张冠李戴。改为传入资源解析器，
     * 由 ViewModel 用**新抽出的卡**的 themeResId 解析本地化主题名，保证数据一致。
     */
    fun drawCard(themeResolver: (Int) -> String) {
        if (!colorWalkManager.canDrawMore()) return
        val card = colorWalkManager.drawTodayCard() ?: return
        checkInManager.checkIn(cardId = card.id, cardTheme = themeResolver(card.themeResId))
        applyCardState(card)
    }

    /** 重抽（同样以新卡主题记账） */
    fun redrawCard(themeResolver: (Int) -> String) {
        if (!colorWalkManager.canDrawMore()) return
        val nextCard = colorWalkManager.drawTodayCard() ?: return
        checkInManager.checkIn(cardId = nextCard.id, cardTheme = themeResolver(nextCard.themeResId))
        applyCardState(nextCard)
    }

    fun loadTodayCard() {
        val card = colorWalkManager.getTodayCard()
        _state.value = _state.value.copy(
            todayCard = card,
            history = colorWalkManager.getHistory(),
            drawCount = colorWalkManager.getDrawCount(),
            isChallengeCompleted = checkInManager.getTodayRecord()?.hasCompletedChallenge == true
        )
    }

    private fun applyCardState(card: ColorCard) {
        _state.value = _state.value.copy(
            todayCard = card,
            history = colorWalkManager.getHistory(),
            drawCount = colorWalkManager.getDrawCount(),
            isChallengeCompleted = false,
            isAnimating = true
        )

        viewModelScope.launch {
            delay(500)
            _state.value = _state.value.copy(isAnimating = false)
        }
    }

    fun clearTodayCard() {
        colorWalkManager.clearTodayCard()
        _state.value = _state.value.copy(
            todayCard = null,
            isChallengeCompleted = false
        )
    }

    fun completeChallenge() {
        checkInManager.markChallengeCompleted()
        _state.value = _state.value.copy(isChallengeCompleted = true)
    }

    fun clearHistory() {
        colorWalkManager.clearHistory()
        _state.value = _state.value.copy(history = emptyList())
    }

    fun showGuide() {
        _uiState.value = ColorWalkUiState.Guide
    }

    fun showHistorySheet() {
        _uiState.value = ColorWalkUiState.HistorySheet
    }

    fun showClearConfirm() {
        _uiState.value = ColorWalkUiState.ClearConfirm
    }

    fun showHistoryPreview(card: ColorCard, date: String) {
        _uiState.value = ColorWalkUiState.HistoryPreview(card, date)
    }

    fun dismissUiState() {
        _uiState.value = ColorWalkUiState.Normal
    }

    override fun onCleared() {
        super.onCleared()
    }
}

data class ColorWalkState(
    val todayCard: ColorCard? = null,
    val history: List<ColorCardHistory> = emptyList(),
    val drawCount: Int = 0,
    val isChallengeCompleted: Boolean = false,
    val isAnimating: Boolean = false
)

class ColorWalkViewModelFactory(
    private val colorWalkManager: ColorWalkManager,
    private val checkInManager: CheckInManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ColorWalkViewModel(colorWalkManager, checkInManager) as T
    }
}
