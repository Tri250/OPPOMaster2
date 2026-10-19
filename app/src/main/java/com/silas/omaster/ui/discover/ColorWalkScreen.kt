package com.silas.omaster.ui.discover

import android.content.Context
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.ui.components.CelebrationOverlay
import com.silas.omaster.ui.theme.BrandTheme
import com.silas.omaster.ui.animation.FLUID_CELEBRATE_MS
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import com.silas.omaster.R
import com.silas.omaster.data.local.CheckInManager
import com.silas.omaster.data.local.ColorWalkManager
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val PREFS_NAME = "color_walk_prefs"
private const val KEY_HAS_SEEN_GUIDE = "has_seen_guide"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorWalkScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val colorWalkManager = remember { ColorWalkManager(context) }
    val checkInManager = remember { CheckInManager(context) }

    val viewModel: ColorWalkViewModel = viewModel(
        factory = ColorWalkViewModelFactory(colorWalkManager, checkInManager)
    )
    val state by viewModel.state.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val showGuideFirst = remember {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        !prefs.getBoolean(KEY_HAS_SEEN_GUIDE, false)
    }
    var showGuide by remember { mutableStateOf(showGuideFirst) }

    // ═══════════════════════════════════════════════════════════
    // 【v1.6.14】色卡 → App 主题联动（ColorOS 17 个性化主动智能落地）
    // 把抽到的色卡主色映射到最接近的品牌主题并即时换肤，杀进程后仍保持
    // （ConfigCenter 写入 SharedPreferences 持久化）。
    // ═══════════════════════════════════════════════════════════
    val configCenter = remember { ConfigCenter.getInstance(context) }
    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(celebrating) {
        if (celebrating) {
            delay(FLUID_CELEBRATE_MS.toLong() + 300L)
            celebrating = false
        }
    }
    // 抽到新卡 → 触发一次流体庆祝
    LaunchedEffect(state.todayCard?.id) {
        if (state.todayCard != null) celebrating = true
    }

    /** 应用色卡配色到 App 主题 */
    val applyCardTheme: () -> Unit = {
        val card = state.todayCard
        if (card != null) {
            val theme = nearestBrandTheme(card.colors.firstOrNull()?.hex)
            runCatching {
                configCenter.currentTheme = theme
            }
            celebrating = true
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.colorwalk_theme_applied, theme.id),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(themedBackground())
        ) {
            OMasterTopAppBar(
                title = stringResource(R.string.colorwalk_title),
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showGuide = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "玩法说明", tint = themedTextPrimary())
                    }
                    if (state.history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showHistorySheet() }) {
                            Icon(Icons.Default.History, stringResource(R.string.colorwalk_history), tint = themedTextPrimary())
                        }
                    }
                }
            )

            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(AppDesign.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.todayCard != null) {
                    ColorWalkCard(
                        config = CardDisplayConfig(
                            card = state.todayCard!!,
                            drawCount = state.drawCount,
                            dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("MM.dd")),
                            isAnimating = state.isAnimating,
                            isChallengeCompleted = state.isChallengeCompleted,
                            canRedraw = colorWalkManager.canDrawMore(),
                            remainingDraws = colorWalkManager.getRemainingDraws(),
                            onCompleteChallenge = { viewModel.completeChallenge() },
                            onRedraw = {
                                val card = state.todayCard
                                if (card != null) viewModel.redrawCard(context.getString(card.themeResId))
                            },
                            onClear = { viewModel.clearTodayCard() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ColorWalkEmptyCard(
                        onDrawClick = { viewModel.drawCard("") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 【v1.6.14】把今日色卡的配色应用到 App 主题
                if (state.todayCard != null) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { applyCardTheme() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.colorwalk_apply_theme),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }

        // 【v1.6.14】流体庆祝层（抽卡 / 应用主题 / 完成挑战）
        CelebrationOverlay(
            visible = celebrating,
            glowColor = MaterialTheme.colorScheme.primary,
            confettiColors = state.todayCard?.colors?.map { it.toColor() } ?: emptyList(),
            modifier = Modifier.fillMaxSize()
        )
    }

    if (showGuide) {
        ColorWalkGuide(
            onDismiss = {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_HAS_SEEN_GUIDE, true).apply()
                showGuide = false
            }
        )
    }

    when (val current = uiState) {
        is ColorWalkUiState.HistorySheet -> {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissUiState() },
                sheetState = sheetState
            ) {
                HistorySheetContent(
                    history = state.history,
                    onClearHistory = { viewModel.showClearConfirm() },
                    onCardClick = { card, date -> viewModel.showHistoryPreview(card, date) }
                )
            }
        }

        is ColorWalkUiState.HistoryPreview -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUiState() },
                title = {
                    Text(
                        stringResource(R.string.colorwalk_history_preview_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        ColorWalkCard(
                            config = CardDisplayConfig(
                                card = current.card,
                                dateLabel = current.date,
                                showActions = false
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissUiState() }) {
                        Text(stringResource(R.string.colorwalk_close_preview))
                    }
                }
            )
        }

        is ColorWalkUiState.ClearConfirm -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUiState() },
                title = { Text(stringResource(R.string.colorwalk_clear_history)) },
                text = { Text(stringResource(R.string.colorwalk_clear_history_confirm)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearHistory()
                            viewModel.dismissUiState()
                        }
                    ) {
                        Text(stringResource(R.string.colorwalk_confirm_clear))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUiState() }) {
                        Text(stringResource(R.string.colorwalk_cancel))
                    }
                }
            )
        }

        else -> { /* Normal */ }
    }
}

/**
 * 【v1.6.14】把色卡主色映射到最接近的品牌主题。
 *
 * App 主题体系基于 [BrandTheme] 枚举（哈苏 / 蔡司 / 徕卡 … 共 9 种品牌色），
 * 因此"把色卡配色应用到主题"落地为：按 RGB 欧氏距离找出色卡主色最接近的品牌色。
 * 解析失败一律回退哈苏（品牌默认），保证任何异常都不影响主题可用性。
 */
private fun nearestBrandTheme(hex: String?): BrandTheme {
    if (hex.isNullOrBlank()) return BrandTheme.Hasselblad
    val target = runCatching {
        Color(android.graphics.Color.parseColor("#${hex.removePrefix("#")}"))
    }.getOrNull() ?: return BrandTheme.Hasselblad

    return BrandTheme.entries.minByOrNull { theme ->
        val c = theme.primaryColor
        val dr = c.red - target.red
        val dg = c.green - target.green
        val db = c.blue - target.blue
        dr * dr + dg * dg + db * db
    } ?: BrandTheme.Hasselblad
}
