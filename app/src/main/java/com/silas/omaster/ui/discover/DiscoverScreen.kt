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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.local.CheckInManager
import com.silas.omaster.data.local.ColorWalkManager
import com.silas.omaster.data.local.InspirationManager
import com.silas.omaster.data.local.MoodManager
import com.silas.omaster.data.local.SnapNoteManager
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
import kotlinx.coroutines.launch
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
    onNavigateToSubscription: () -> Unit = {},
    onScrollStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val checkInManager = remember { CheckInManager(context) }
    val colorWalkManager = remember { ColorWalkManager(context) }
    // 【v1.6.14 发现 2.0】色彩心情 / 快拍笔记 / 灵感收藏 / 订阅雷达
    val repository = remember { PresetRepository.getInstance(context) }
    val moodManager = remember { MoodManager(context) }
    val snapNoteManager = remember { SnapNoteManager(context) }
    val inspirationManager = remember { InspirationManager(context) }
    val configCenter = remember { ConfigCenter.getInstance(context) }
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    var showCheckInSheet by remember { mutableStateOf(false) }
    var sheetDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    // 【v1.6.13 性能修复】连续打卡次数读取记忆化，避免每次重组同步读 SharedPreferences；
    // 打卡面板关闭时 bump refreshKey 保证返回后数据最新
    var streakRefreshKey by remember { mutableStateOf(0) }
    val currentStreak = remember(streakRefreshKey) { checkInManager.getCurrentStreak() }
    val todayRecord = remember(streakRefreshKey) { checkInManager.getTodayRecord() }
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

            // 【v1.6.14 发现 2.0】今日灵感流：每日 5 张精选（机型感知），横滑 + 换一批 + 收藏
            InspirationFlowCard(
                repository = repository,
                inspirationManager = inspirationManager,
                onPresetClick = onNavigateToPresetDetail
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // 【v1.6.14 发现 2.0】订阅更新雷达：有新增预设时展示（无更新自动隐藏）
            PresetRadarCard(
                config = configCenter,
                onNavigateToSubscription = onNavigateToSubscription,
                modifier = Modifier.fillMaxWidth()
            )

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

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // 【v1.6.14 发现 2.0】ColorMood 色彩心情（原占位模块转正）
            MoodColorCard(
                moodManager = moodManager,
                onPresetClick = onNavigateToPresetDetail,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // 【v1.6.14 发现 2.0】AiSnapNote 快拍笔记（原占位模块转正）
            SnapNoteCard(
                snapNoteManager = snapNoteManager,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.SectionSpacing))

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
                streakRefreshKey++
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
 * 【v1.6.14 发现 2.0】今日灵感流
 * 每日从精选池（机型专属优先）确定性抽取 5 张灵感，横滑浏览；
 * 支持"换一批"重新洗牌、单卡收藏、收藏夹回顾，点击进入参数详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspirationFlowCard(
    repository: PresetRepository,
    inspirationManager: InspirationManager,
    onPresetClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPresets by repository.getAllPresets().collectAsState(initial = emptyList())
    val deviceProfile = remember { DeviceDetector.getCurrentDevice(context) }
    val favorites by inspirationManager.favoritesFlow.collectAsState()

    // 会话内"换一批"nonce：递增重洗（跨日自动回落到每日固定流）
    var shuffleNonce by remember { mutableIntStateOf(0) }
    var showFavoritesSheet by remember { mutableStateOf(false) }

    val dailyPresets = remember(allPresets, shuffleNonce) {
        if (allPresets.isEmpty()) {
            emptyList()
        } else {
            val candidates = allPresets.filter { !it.id.isNullOrEmpty() }
            if (candidates.isEmpty()) {
                emptyList()
            } else {
                val deviceKey = deviceProfile.deviceKey
                val deviceSpecific = candidates.filter { p ->
                    p.deviceProfile?.deviceKey == deviceKey ||
                            p.deviceSupport?.contains(deviceKey) == true
                }
                val pool = deviceSpecific.ifEmpty { candidates }
                val seedBase = LocalDate.now().toEpochDay().toInt() + shuffleNonce * 7919
                pool.shuffled(java.util.Random(seedBase.toLong())).take(FLOW_SIZE)
            }
        }
    }

    if (dailyPresets.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { dailyPresets.size })
    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        // 头部：标题 + 换一批 + 收藏夹
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.discover_daily_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${pagerState.currentPage + 1}/${dailyPresets.size}",
                style = MaterialTheme.typography.labelSmall,
                color = themedTextSecondary().copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            FlowActionButton(
                icon = Icons.Default.Refresh,
                text = stringResource(R.string.discover_daily_shuffle),
                onClick = {
                    shuffleNonce++
                    scope.launch { pagerState.animateScrollToPage(0) }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FlowActionButton(
                icon = Icons.Default.Star,
                text = stringResource(R.string.discover_daily_favorites),
                onClick = { showFavoritesSheet = true }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            pageSpacing = 10.dp
        ) { page ->
            val preset = dailyPresets[page]
            val presetId = preset.id ?: return@HorizontalPager
            val isFav = favorites.any { it.presetId == presetId }
            val favInteraction = remember { MutableInteractionSource() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fluidPressScale(remember { MutableInteractionSource() }),
                colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
                shape = RoundedCornerShape(16.dp),
                onClick = { onPresetClick(presetId) },
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        PresetImage(
                            preset = preset,
                            modifier = Modifier.fillMaxSize(),
                            showDownloadIndicator = false
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = PresetI18n.getLocalizedPresetName(preset.name),
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

                    // 收藏按钮（阻止点击穿透到卡片）
                    Icon(
                        imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (isFav) Color(0xFFFFB300) else themedTextSecondary().copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = favInteraction,
                                indication = null
                            ) {
                                inspirationManager.toggleFavorite(presetId)
                            }
                    )
                }
            }
        }

        // 页点指示
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(dailyPresets.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                themedTextSecondary().copy(alpha = 0.25f)
                            }
                        )
                )
            }
        }
    }

    if (showFavoritesSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFavoritesSheet = false },
            sheetState = sheetState
        ) {
            InspirationFavoritesSheet(
                inspirationManager = inspirationManager,
                allPresets = allPresets,
                onPresetClick = { presetId ->
                    showFavoritesSheet = false
                    onPresetClick(presetId)
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FlowActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .fluidPressScale(interactionSource, pressedScale = 0.94f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** 灵感收藏夹：横滑列表，点击直达详情 */
@Composable
private fun InspirationFavoritesSheet(
    inspirationManager: InspirationManager,
    allPresets: List<MasterPreset>,
    onPresetClick: (String) -> Unit
) {
    val favorites by inspirationManager.favoritesFlow.collectAsState()
    val favoritePresets = remember(favorites, allPresets) {
        favorites.mapNotNull { fav -> allPresets.firstOrNull { it.id == fav.presetId } }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.discover_favorites_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (favoritePresets.isEmpty()) {
            Text(
                text = stringResource(R.string.discover_favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextSecondary(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                favoritePresets.forEach { preset ->
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

private const val FLOW_SIZE = 5
