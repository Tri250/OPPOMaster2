package com.silas.omaster.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
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
import com.silas.omaster.R
import com.silas.omaster.data.local.CheckInManager
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
 * 1) Bento 分栏布局：Color Walk / 相框出片 双列磁贴，层次更清晰
 * 2) 新增「每日灵感」卡片：按日期从预设库轮换精选，点击直达参数详情
 * 3) 快门日记卡片：已打卡状态徽章（绿勾）+ 连续记录天数
 * 4) 卡片统一 20dp 大圆角，对齐 ColorOS 17 视觉语言
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
    val checkInManager = remember { CheckInManager(context) }
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    var isScrollingUp by remember { mutableStateOf(true) }

    var showCheckInSheet by remember { mutableStateOf(false) }
    var sheetDetailDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentStreak = checkInManager.getCurrentStreak()
    val todayRecord = remember { checkInManager.getTodayRecord() }

    // ═══════════════════════════════════════════════════════════
    // v1.6.10 每日灵感：按日期从全部预设中稳定轮换
    // 同一天内结果固定，每天自动更换；与主页数据源一致（内置 + 订阅 + 自定义）
    // ═══════════════════════════════════════════════════════════
    val repository = remember { PresetRepository.getInstance(context) }
    val allPresets by repository.getAllPresets().collectAsState(initial = emptyList())
    val dailyPreset = remember(allPresets) {
        val pool = allPresets.filter { it.id != null }
        if (pool.isEmpty()) null
        else pool[LocalDate.now().dayOfYear % pool.size]
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
            // ① 每日灵感（宽卡，点击直达详情）
            DailyInspirationCard(
                preset = dailyPreset,
                onClick = { dailyPreset?.id?.let(onNavigateToPresetDetail) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ② Color Walk / 相框出片 双列磁贴
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
                    onClick = onNavigateToColorWalk,
                    modifier = Modifier.weight(1f)
                )
                FeatureTileCard(
                    icon = Icons.Default.CameraAlt,
                    title = stringResource(R.string.photoframe_card_title),
                    description = stringResource(R.string.photoframe_card_desc),
                    gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                    onClick = onNavigateToPhotoFrame,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            // ③ 快门日记（含打卡状态徽章）
            CheckInEntryCard(
                currentStreak = currentStreak,
                hasCheckedInToday = todayRecord != null,
                onClick = { showCheckInSheet = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.SectionSpacing))

            SurveyEntryCard(
                onClick = onNavigateToSurvey,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(AppDesign.ItemSpacing))

            ComingSoonCard(modifier = Modifier.fillMaxWidth())

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
// 封面（真实预设图）+ 名称 + 滤镜，按日期轮换；点击进入预设详情
// ═══════════════════════════════════════════════════════════

@Composable
private fun DailyInspirationCard(
    preset: MasterPreset?,
    onClick: () -> Unit,
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
                // 栏目标签
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary(),
                    maxLines = 1
                )
                preset.filter?.let { filter ->
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary(),
                        maxLines = 1
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

            Icon(
                Icons.Default.ChevronRight, null,
                tint = themedTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：双列功能磁贴（Bento 布局）
// 渐变图标块置顶 + 标题 + 描述，等高等宽
// ═══════════════════════════════════════════════════════════

@Composable
private fun FeatureTileCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: List<Color>,
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
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
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
// 已打卡 → 尾部绿色对勾徽章；未打卡 → 引导箭头
// ═══════════════════════════════════════════════════════════

@Composable
private fun CheckInEntryCard(
    currentStreak: Int,
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

            if (hasCheckedInToday) {
                // 今日已打卡徽章
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.checkin_done_today),
                    tint = Color(0xFF38EF7D),
                    modifier = Modifier.size(22.dp)
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

@Composable
private fun ComingSoonCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Star, null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    stringResource(R.string.discover_more_coming),
                    style = MaterialTheme.typography.bodyMedium,
                    color = themedTextSecondary(),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    stringResource(R.string.discover_stay_tuned),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f)
                )
            }
        }
    }
}
