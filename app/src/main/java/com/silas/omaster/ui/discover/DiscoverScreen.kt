package com.silas.omaster.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.silas.omaster.R
import com.silas.omaster.data.local.CheckInManager
import com.silas.omaster.data.local.ColorWalkManager
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.fluidPressScale
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.components.PresetImage
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.util.DeviceDetector
import com.silas.omaster.util.PresetI18n
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    val checkInManager = remember { CheckInManager(context) }
    val colorWalkManager = remember { ColorWalkManager(context) }
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    var showCheckInSheet by remember { mutableStateOf(false) }
    var sheetDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentStreak = checkInManager.getCurrentStreak()
    val todayRecord = remember { checkInManager.getTodayRecord() }
    // 【v1.6.13】ColorWalk 今日状态（从 Color Walk 返回后自动刷新）
    val todayCard by colorWalkManager.todayCardFlow.collectAsState()
    // 【v1.6.13】当前设备能力（Find X10 → 哈苏专业影像 / 2亿像素）
    val deviceDp = remember { DeviceDetector.getCurrentDevice(context) }

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
            // 【v1.6.13】时段问候头部（ColorOS 17 个性化体验）
            GreetingHeader()

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // 【v1.6.13】今日灵感卡：每日轮换一张精选预设，点击查看参数
            DailyInspirationCard(onPresetClick = onNavigateToPresetDetail)

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            FeatureCard(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.colorwalk_card_title),
                description = stringResource(R.string.colorwalk_card_desc),
                statusText = if (todayCard != null) {
                    stringResource(R.string.colorwalk_today_done)
                } else {
                    stringResource(R.string.colorwalk_draw_hint)
                },
                gradient = listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
                onClick = onNavigateToColorWalk,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            FeatureCard(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.photoframe_card_title),
                description = stringResource(R.string.photoframe_card_desc),
                // 【v1.6.13】Find X10 专属：哈苏专业影像能力状态行（其他机型不显示）
                statusText = if (deviceDp.isProImaging) {
                    stringResource(R.string.photoframe_status_hasselblad)
                } else {
                    null
                },
                gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                onClick = onNavigateToPhotoFrame,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            CheckInEntryCard(
                currentStreak = currentStreak,
                hasCheckedInToday = todayRecord != null,
                onClick = { showCheckInSheet = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.SectionSpacing))

            ComingSoonCard(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            SurveyEntryCard(
                onClick = onNavigateToSurvey,
                modifier = Modifier.fillMaxWidth()
            )

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

@Composable
private fun SurveyEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 【v1.6.13】ColorOS 17 液态按压反馈
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fluidPressScale(interactionSource)
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        interactionSource = interactionSource
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

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    statusText: String? = null
) {
    // 【v1.6.13】ColorOS 17 液态按压反馈
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.fluidPressScale(interactionSource),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = themedTextPrimary(),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 2
                )
                // 【v1.6.13】今日状态提示（如 ColorWalk 今日进度）
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight, null,
                tint = themedTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CheckInEntryCard(
    currentStreak: Int,
    hasCheckedInToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 【v1.6.13】ColorOS 17 液态按压反馈
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.fluidPressScale(interactionSource),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        interactionSource = interactionSource
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
                    Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(24.dp))
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentStreak > 0) {
                        stringResource(R.string.discover_checkin_subtitle_active, currentStreak)
                    } else if (!hasCheckedInToday) {
                        stringResource(R.string.discover_checkin_not_today)
                    } else {
                        stringResource(R.string.discover_checkin_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
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

@Composable
private fun ComingSoonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Star, null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                stringResource(R.string.discover_more_coming),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextSecondary(),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.discover_stay_tuned),
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary().copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 【v1.6.13】时段问候头部（ColorOS 17 个性化体验）
 * 本地化日期 + 早上好 / 下午好 / 晚上好 + 今日主题问句
 * 日期格式由系统 Locale 决定（对齐 ColorOS 桌面智能信息服务）
 */
@Composable
private fun GreetingHeader(modifier: Modifier = Modifier) {
    val hour = remember { LocalTime.now().hour }
    val todayText = remember {
        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
    val greetingRes = when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = todayText,
            style = MaterialTheme.typography.labelMedium,
            color = themedTextSecondary().copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(greetingRes),
            style = MaterialTheme.typography.bodyMedium,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.discover_greeting_today),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
    }
}

/**
 * 【v1.6.13】今日灵感卡
 * 以日期为种子每日轮换一张精选预设，展示真实样片，点击进入参数详情。
 * 【v1.6.13】机型感知：优先从当前设备专属预设（deviceProfile / deviceSupport 匹配）中轮换，
 * 例如 Find X10 用户优先看到哈苏 / 2亿像素专属灵感；无专属时回退全部预设。
 */
@Composable
private fun DailyInspirationCard(
    onPresetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { PresetRepository.getInstance(context) }
    val allPresets by repository.getAllPresets().collectAsState(initial = emptyList())
    val deviceProfile = remember { DeviceDetector.getCurrentDevice(context) }

    // 以 epochDay 为种子确定性轮换，当天内保持稳定
    val dailyPreset = remember(allPresets) {
        if (allPresets.isEmpty()) {
            null
        } else {
            val seed = LocalDate.now().toEpochDay().toInt()
            val candidates = allPresets.filter { !it.id.isNullOrEmpty() }
            if (candidates.isEmpty()) {
                null
            } else {
                // 机型专属池：预设声明的 deviceKey 与当前设备匹配
                val deviceKey = deviceProfile.deviceKey
                val deviceSpecific = candidates.filter { p ->
                    p.deviceProfile?.deviceKey == deviceKey ||
                            p.deviceSupport?.contains(deviceKey) == true
                }
                val pool = deviceSpecific.ifEmpty { candidates }
                pool[(seed % pool.size).coerceAtLeast(0)]
            }
        }
    } ?: return

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .fluidPressScale(interactionSource),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = { dailyPreset.id?.let(onPresetClick) },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 真实样片（复用 PresetImage 统一加载管线）
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                PresetImage(
                    preset = dailyPreset,
                    modifier = Modifier.fillMaxSize(),
                    showDownloadIndicator = false
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.discover_daily_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = PresetI18n.getLocalizedPresetName(dailyPreset.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary(),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.discover_daily_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // CTA：查看参数
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.discover_daily_cta),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
