package com.silas.omaster.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.data.local.CheckInManager
import com.silas.omaster.data.local.ColorWalkManager
import com.silas.omaster.data.local.FavoriteManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.ui.theme.themedBorderLight
import java.time.LocalDate

/**
 * 发现页 — v1.6.10 Find X10 / ColorOS 17 对齐改版：
 * 1) 每日灵感卡：「换一换」手动轮换 + 收藏状态星标 + 作者标识（ColorOS 17 AI 灵感体验）
 * 2) Bento 分栏磁贴：Color Walk / 相框出片，磁贴支持动态状态徽章
 *    （Color Walk 联动抽取状态：待抽取 / 剩余次数 / 已完成）
 * 3) 快门日记卡：连击火焰徽章 / 打卡绿勾徽章 + 累计·最长统计
 * 4) AI 灵感实验室预告卡：Find X10 特色功能前瞻（AI 滤镜 / 哈苏大师通道 / 流体云参数卡）
 * 5) 全卡片 O-Haptics 触感反馈，对齐 ColorOS 17 触感引擎；20dp 大圆角统一视觉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    onNavigateToColorWalk: () -> Unit = {},
    onNavigateToPhotoFrame: () -> Unit = {},
    onNavigateToSurvey: () -> Unit = {},
    onNavigateToPresetDetail: (String) -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val checkInManager = remember { CheckInManager(context) }
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    var showCheckInSheet by remember { mutableStateOf(false) }
    var sheetDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentStreak = checkInManager.getCurrentStreak()
    val longestStreak = checkInManager.getLongestStreak()
    val totalDays = checkInManager.getAllRecords().size
    val todayRecord = remember { checkInManager.getTodayRecord() }

    // ═══════════════════════════════════════════════════════════
    // v1.6.10 每日灵感：按日期从全部预设中稳定轮换
    // 同一天内结果固定，每天自动更换；「换一换」手动偏移切换
    // 与主页数据源一致（内置 + 订阅 + 自定义）
    // ═══════════════════════════════════════════════════════════
    val repository = remember { PresetRepository.getInstance(context) }
    val allPresets by repository.getAllPresets().collectAsState(initial = emptyList())
    var dailyOffset by remember { mutableIntStateOf(0) }
    val dailyPreset = remember(allPresets, dailyOffset) {
        val pool = allPresets.filter { it.id != null }
        if (pool.isEmpty()) null
        else pool[Math.floorMod(LocalDate.now().dayOfYear + dailyOffset, pool.size)]
    }

    // v1.6.10 每日灵感收藏状态（联动 FavoriteManager）
    val favoriteManager = remember { FavoriteManager(context) }
    val favorites by favoriteManager.favoritesFlow.collectAsState()
    val isDailyFavorite = dailyPreset?.id?.let { it in favorites } ?: false

    // v1.6.10 Color Walk 动态状态徽章（联动抽取状态）
    val colorWalkManager = remember { ColorWalkManager(context) }
    val colorWalkBadge = if (!colorWalkManager.hasDrawnToday()) {
        stringResource(R.string.discover_colorwalk_badge_todo)
    } else {
        val remaining = colorWalkManager.getRemainingDraws()
        if (remaining > 0) {
            stringResource(R.string.discover_colorwalk_badge_ready, remaining)
        } else {
            stringResource(R.string.discover_colorwalk_badge_done)
        }
    }

    LaunchedEffect(scrollState.value) {
        val currentValue = scrollState.value
        if (currentValue != previousScrollValue) {
            val up = currentValue < previousScrollValue
            if (isScrollingUp != up) {
                isScrollingUp = up
                onScrollStateChanged(up)
            }
            previousScrollValue = currentValue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themedBackground())
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.discover_title),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(AppDesign.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ① 每日灵感（宽卡，点击直达详情；支持换一换 / 收藏标识）
            DailyInspirationCard(
                preset = dailyPreset,
                isFavorite = isDailyFavorite,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dailyPreset?.id?.let(onNavigateToPresetDetail)
                },
                onShuffle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dailyOffset++
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ② Color Walk / 相框出片 双列磁贴（含动态状态徽章）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing)
            ) {
                FeatureTileCard(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.colorwalk_card_title),
                    description = stringResource(R.string.colorwalk_card_desc),
                    gradient = listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
                    badge = colorWalkBadge,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToColorWalk()
                    },
                    modifier = Modifier.weight(1f)
                )
                FeatureTileCard(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(R.string.photoframe_card_title),
                    description = stringResource(R.string.photoframe_card_desc),
                    gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                    badge = null,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPhotoFrame()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ③ 快门日记（连击火焰徽章 / 打卡绿勾徽章 + 累计统计）
            CheckInEntryCard(
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalDays = totalDays,
                hasCheckedInToday = todayRecord != null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showCheckInSheet = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.SectionSpacing))

            SurveyEntryCard(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToSurvey()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ④ AI 灵感实验室（Find X10 / ColorOS 17 特色功能预告）
            AiLabPreviewCard(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showCheckInSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = {
                showCheckInSheet = false
                sheetDetailDate = null
            },
            sheetState = sheetState
        ) {
            if (sheetDetailDate != null) {
                val record = checkInManager.getRecordForDate(sheetDetailDate!!)
                CheckInDetailContent(
                    selectedDate = sheetDetailDate!!,
                    record = record
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                CheckInCalendar(
                    onDateClick = { date -> sheetDetailDate = date },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：每日灵感卡
// 封面（真实预设图）+ 名称 + 滤镜·作者，按日期轮换；
// 右上「换一换」手动轮换（触感反馈）；已收藏显示金星标识
// ═══════════════════════════════════════════════════════════

@Composable
private fun DailyInspirationCard(
    preset: MasterPreset?,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (preset == null) return

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 预设封面
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                PresetImage(
                    preset = preset,
                    modifier = Modifier.fillMaxSize(),
                    showDownloadIndicator = false
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 栏目标签 + 换一换
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Star, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        stringResource(R.string.discover_daily_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // v1.6.10 换一换（AI 灵感，同日手动切换）
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onShuffle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            stringResource(R.string.discover_daily_refresh),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themedTextPrimary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = stringResource(R.string.discover_daily_favorited),
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                // 滤镜 · 作者
                val metaLine = listOfNotNull(
                    preset.filter,
                    preset.author.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (metaLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = metaLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    stringResource(R.string.discover_daily_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：双列功能磁贴（Bento 布局）
// 渐变图标块置顶 + 可选动态状态徽章 + 标题 + 描述，等高等宽
// ═══════════════════════════════════════════════════════════

@Composable
private fun FeatureTileCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: List<Color>,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                // v1.6.10 动态状态徽章（如 Color Walk 剩余抽取次数）
                if (badge != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = themedTextPrimary(),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary(),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SurveyEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudUpload, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    stringResource(R.string.survey_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.survey_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f)
                )
            }
            Icon(
                Icons.Default.ChevronRight, null,
                tint = themedTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：快门日记入口卡
// 已打卡 → 绿色对勾徽章；有连击 → 火焰天数徽章；未打卡 → 引导箭头
// 副标题追加累计天数 / 最长连击统计
// ═══════════════════════════════════════════════════════════

@Composable
private fun CheckInEntryCard(
    currentStreak: Int,
    longestStreak: Int,
    totalDays: Int,
    hasCheckedInToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.discover_checkin_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = themedTextPrimary(),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = when {
                        currentStreak > 0 -> stringResource(R.string.discover_checkin_subtitle_active, currentStreak)
                        !hasCheckedInToday -> stringResource(R.string.discover_checkin_not_today)
                        else -> stringResource(R.string.discover_checkin_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
                )
                // v1.6.10 累计统计（ColorOS 17 数据磁贴风格）
                if (totalDays > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.discover_checkin_stat_line, totalDays, longestStreak),
                        style = MaterialTheme.typography.labelSmall,
                        color = themedTextSecondary().copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (hasCheckedInToday) {
                // 今日已打卡徽章（绿勾 pill）
                StatusBadgePill(
                    icon = Icons.Default.CheckCircle,
                    text = stringResource(R.string.checkin_done_today),
                    contentColor = Color(0xFF11998E),
                    containerColor = Color(0xFF11998E).copy(alpha = 0.1f)
                )
            } else if (currentStreak > 0) {
                // 连击火焰徽章
                StatusBadgePill(
                    icon = Icons.Default.LocalFireDepartment,
                    text = stringResource(R.string.discover_checkin_streak_badge, currentStreak),
                    contentColor = Color(0xFFFF6B35),
                    containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = themedTextSecondary().copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：状态徽章 pill（图标 + 文本，圆角胶囊）
// ═══════════════════════════════════════════════════════════

@Composable
private fun StatusBadgePill(
    icon: ImageVector,
    text: String,
    contentColor: Color,
    containerColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = contentColor, modifier = Modifier.size(13.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1
        )
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：AI 灵感实验室预告卡（替代"敬请期待"）
// Find X10 / ColorOS 17 影像特色功能前瞻：
// AI 灵感滤镜 / 哈苏大师通道 / 流体云参数卡
// ═══════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiLabPreviewCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF7F5CFF), Color(0xFF4FACFE))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.discover_lab_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = themedTextPrimary()
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        stringResource(R.string.discover_lab_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary().copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // 特色功能 pill 标签
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LabFeaturePill(stringResource(R.string.discover_lab_feature_ai))
                LabFeaturePill(stringResource(R.string.discover_lab_feature_hasselblad))
                LabFeaturePill(stringResource(R.string.discover_lab_feature_fluid))
            }
        }
    }
}

@Composable
private fun LabFeaturePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
