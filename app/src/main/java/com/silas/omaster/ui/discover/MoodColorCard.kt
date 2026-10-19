package com.silas.omaster.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.silas.omaster.R
import com.silas.omaster.data.local.Mood
import com.silas.omaster.data.local.MoodManager
import com.silas.omaster.model.DeviceProfile
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.fluidPressScale
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.util.DeviceDetector
import com.silas.omaster.util.PresetI18n
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 【v1.6.14 发现 2.0】ColorMood 色彩心情卡
 * 记录每日心情（6 种情绪色系），联动心情月历与同情绪气质的灵感预设匹配。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodColorCard(
    moodManager: MoodManager,
    onPresetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods by moodManager.moodsFlow.collectAsState()
    val todayMood = moods[LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))]
        ?.let { Mood.byId(it) }

    var showMoodSheet by remember { mutableStateOf(false) }
    var showInspirationSheet by remember { mutableStateOf(false) }
    var showCalendarSheet by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fluidPressScale(interactionSource)
            .border(1.dp, themedBorderLight(), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = { showMoodSheet = true },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 心情色块（已选展示心情渐变，未选展示中性占位）
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (todayMood != null) {
                            Brush.linearGradient(todayMood.gradient.map { Color(it) })
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (todayMood != null) Icons.Default.Mood else Icons.Default.Psychology,
                    contentDescription = null,
                    tint = if (todayMood != null) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.mood_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (todayMood != null) {
                        stringResource(moodNameRes(todayMood))
                    } else {
                        stringResource(R.string.mood_card_desc)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
                )
            }

            // 已选心情时提供灵感直达
            if (todayMood != null) {
                MoodInspirationChip(onClick = { showInspirationSheet = true })
            } else {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = themedTextSecondary().copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showMoodSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMoodSheet = false },
            sheetState = sheetState
        ) {
            MoodPickerSheet(
                moodManager = moodManager,
                onOpenCalendar = {
                    showMoodSheet = false
                    showCalendarSheet = true
                },
                onDismiss = { showMoodSheet = false }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showInspirationSheet && todayMood != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showInspirationSheet = false },
            sheetState = sheetState
        ) {
            MoodInspirationSheet(
                mood = todayMood,
                onPresetClick = { presetId ->
                    showInspirationSheet = false
                    onPresetClick(presetId)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCalendarSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showCalendarSheet = false },
            sheetState = sheetState
        ) {
            MoodMonthCalendar(moodManager = moodManager)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoodInspirationChip(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .fluidPressScale(interactionSource, pressedScale = 0.94f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.AutoAwesome, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(R.string.mood_inspiration_cta),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** 心情选择面板：6 宫格情绪色系 + 心情月历入口 */
@Composable
private fun MoodPickerSheet(
    moodManager: MoodManager,
    onOpenCalendar: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.mood_picker_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.mood_picker_desc),
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Mood.entries.chunked(3).forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowMoods.forEach { mood ->
                    MoodCell(
                        mood = mood,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            moodManager.setTodayMood(mood)
                            onDismiss()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 心情月历入口
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                .clickable(onClick = onOpenCalendar)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AutoAwesome, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.mood_calendar_entry),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MoodCell(mood: Mood, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, themedBorderLight(), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(mood.gradient.map { Color(it) }))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(moodNameRes(mood)),
            style = MaterialTheme.typography.labelMedium,
            color = themedTextPrimary(),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/** 心情月历：复用打卡日历网格交互，格子底色为当日心情色 */
@Composable
private fun MoodMonthCalendar(moodManager: MoodManager) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val haptic = LocalHapticFeedback.current
    val moods by moodManager.moodsFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.mood_calendar_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 翻月头
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(Icons.Default.ChevronLeft, null, tint = themedTextPrimary())
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("yyyy.MM")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = themedTextPrimary()
            )
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(Icons.Default.ChevronRight, null, tint = themedTextPrimary())
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 周头
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                R.string.checkin_weekday_sun, R.string.checkin_weekday_mon, R.string.checkin_weekday_tue,
                R.string.checkin_weekday_wed, R.string.checkin_weekday_thu, R.string.checkin_weekday_fri,
                R.string.checkin_weekday_sat
            ).forEach { res ->
                Text(
                    text = stringResource(res),
                    style = MaterialTheme.typography.labelSmall,
                    color = themedTextSecondary(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 网格
        val firstDay = currentMonth.atDay(1)
        val daysInMonth = currentMonth.lengthOfMonth()
        val leadingBlank = firstDay.dayOfWeek.value % 7
        val totalSlots = ((leadingBlank + daysInMonth + 6) / 7) * 7

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (weekStart in 0 until totalSlots step 7) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (offset in 0 until 7) {
                        val dayNumber = weekStart + offset - leadingBlank + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val mood = moods[date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))]
                                ?.let { Mood.byId(it) }
                            val isToday = date == LocalDate.now()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            mood != null -> Color(mood.gradient.first()).copy(alpha = if (isToday) 0.9f else 0.55f)
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (mood != null) Color.White else themedTextSecondary(),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        } else {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** 心情灵感面板：匹配同情绪气质的预设灵感，横滑浏览 */
@Composable
private fun MoodInspirationSheet(
    mood: Mood,
    onPresetClick: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { com.silas.omaster.data.repository.PresetRepository.getInstance(context) }
    val allPresets by repository.getAllPresets().collectAsState(initial = emptyList())

    val matched = remember(mood, allPresets) {
        matchPresetsForMood(mood, allPresets, DeviceDetector.getCurrentDevice(context))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.mood_inspiration_title, stringResource(moodNameRes(mood))),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.mood_inspiration_desc),
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (matched.isEmpty()) {
            Text(
                text = stringResource(R.string.mood_inspiration_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextSecondary(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                matched.forEach { preset ->
                    Column(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, themedBorderLight(), RoundedCornerShape(14.dp))
                            .clickable { preset.id?.let(onPresetClick) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PresetImage(
                            preset = preset,
                            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                            showDownloadIndicator = false
                        )
                        Text(
                            text = PresetI18n.getLocalizedPresetName(preset.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = themedTextPrimary(),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * 心情 → 预设匹配：
 * 1. name / tags 命中心情影像关键词 +2；
 * 2. warmCool 色温倾向与心情一致（同号且差异合理）+1；
 * 3. 得分降序取前 N，不足由机型专属池随机补齐（以心情 id 为种子，同日稳定）。
 */
internal fun matchPresetsForMood(
    mood: Mood,
    allPresets: List<MasterPreset>,
    deviceProfile: DeviceProfile,
    count: Int = 6
): List<MasterPreset> {
    val candidates = allPresets.filter { !it.id.isNullOrEmpty() }
    if (candidates.isEmpty()) return emptyList()

    val scored = candidates.map { preset ->
        var score = 0
        val text = (preset.name + " " + (preset.tags?.joinToString(" ") ?: "")).lowercase()
        mood.keywords.forEach { kw ->
            if (text.contains(kw.lowercase())) score += 2
        }
        preset.warmCool?.let { wc ->
            val sameSign = (wc > 0 && mood.warmBias > 0) || (wc < 0 && mood.warmBias < 0)
            if (sameSign && kotlin.math.abs(wc - mood.warmBias) <= 120) score += 1
        }
        preset to score
    }

    val matched = scored.filter { it.second > 0 }
        .sortedByDescending { it.second }
        .take(count)
        .map { it.first }
    if (matched.size >= count) return matched

    val deviceKey = deviceProfile.deviceKey
    val pool = candidates.filter { p ->
        p.deviceProfile?.deviceKey == deviceKey || p.deviceSupport?.contains(deviceKey) == true
    }.ifEmpty { candidates }
    val usedIds = matched.mapNotNull { it.id }.toSet()
    val filler = pool.filter { it.id !in usedIds }
        .shuffled(java.util.Random(mood.hashCode() + LocalDate.now().toEpochDay()))
        .take(count - matched.size)
    return matched + filler
}

@Composable
private fun moodNameRes(mood: Mood): Int = when (mood) {
    Mood.JOYFUL -> R.string.mood_joyful
    Mood.CALM -> R.string.mood_calm
    Mood.MELANCHOLY -> R.string.mood_melancholy
    Mood.ROMANTIC -> R.string.mood_romantic
    Mood.ENERGETIC -> R.string.mood_energetic
    Mood.SERENE -> R.string.mood_serene
}
