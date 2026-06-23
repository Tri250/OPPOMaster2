package com.silas.omaster.ui.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ui.theme.*
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * 参数精细调节页面 - 深度优化版
 *
 * 功能：
 * - ISO / 快门速度 / 光圈 离散步进调节（吸附标准值）
 * - 曝光表（EV 指示器）显示过曝/欠曝
 * - 对焦模式选择（自动/手动/连续）
 * - 测光模式选择（矩阵/中央重点/点测光）
 * - 直方图可视化（基于曝光参数的亮度分布）
 * - 8 种快速预设芯片（人像/风景/夜景/运动/街拍/美食/室内/逆光）
 * - 参数联动模式（改变一个参数自动调整相关参数）
 * - 参数关系提示
 * - 切换预设动画
 * - 使用 MaterialTheme 主题色替代硬编码颜色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParamAdjustScreen(
    onBack: () -> Unit,
    onApply: (CameraParams) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // ========== 相机参数状态 ==========
    var iso by remember { mutableIntStateOf(100) }
    var shutterSpeed by remember { mutableFloatStateOf(1f / 125f) }
    var aperture by remember { mutableFloatStateOf(2.8f) }
    var whiteBalance by remember { mutableIntStateOf(5500) }
    var focalLength by remember { mutableIntStateOf(23) }
    var exposureCompensation by remember { mutableFloatStateOf(0f) }

    // ========== 新增参数状态 ==========
    var focusMode by remember { mutableStateOf(FocusMode.AUTO) }
    var meteringMode by remember { mutableStateOf(MeteringMode.MATRIX) }
    var linkageEnabled by remember { mutableStateOf(false) }

    // ========== 预设模式 ==========
    var selectedPreset by remember { mutableStateOf<String?>(null) }

    // ========== ISO 标准值（1/3 档步进） ==========
    val isoSteps = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400, 12800, 25600)

    // ========== 快门速度标准值 ==========
    val shutterSteps = listOf(
        "1/8000" to 1f / 8000f,
        "1/4000" to 1f / 4000f,
        "1/2000" to 1f / 2000f,
        "1/1000" to 1f / 1000f,
        "1/500" to 1f / 500f,
        "1/250" to 1f / 250f,
        "1/125" to 1f / 125f,
        "1/60" to 1f / 60f,
        "1/30" to 1f / 30f,
        "1/15" to 1f / 15f,
        "1/8" to 1f / 8f,
        "1/4" to 1f / 4f,
        "1/2" to 1f / 2f,
        "1s" to 1f,
        "2s" to 2f,
        "4s" to 4f,
        "8s" to 8f,
        "15s" to 15f,
        "30s" to 30f
    )

    // ========== 光圈标准值（1/3 档步进） ==========
    val apertureSteps = listOf(1.4f, 1.8f, 2.0f, 2.8f, 4.0f, 5.6f, 8.0f, 11f, 16f, 22f)

    // ========== 白平衡选项 ==========
    val wbSteps = listOf(2800, 3200, 4000, 5000, 5500, 6000, 6500, 7000, 8000, 9000)

    // ========== 预设配置 ==========
    data class PresetConfig(
        val id: String,
        val label: String,
        val icon: @Composable () -> Unit,
        val iso: Int,
        val shutter: Float,
        val aperture: Float,
        val wb: Int,
        val focusMode: FocusMode,
        val meteringMode: MeteringMode
    )

    val presets = listOf(
        PresetConfig("portrait", "人像", { Icon(Icons.Outlined.Person, null, modifier = Modifier.size(14.dp)) }, 200, 1f / 125f, 2.8f, 5500, FocusMode.AUTO, MeteringMode.CENTER),
        PresetConfig("landscape", "风景", { Icon(Icons.Outlined.Landscape, null, modifier = Modifier.size(14.dp)) }, 100, 1f / 60f, 8.0f, 5600, FocusMode.AUTO, MeteringMode.MATRIX),
        PresetConfig("night", "夜景", { Icon(Icons.Outlined.Nightlight, null, modifier = Modifier.size(14.dp)) }, 3200, 4f, 2.8f, 4000, FocusMode.MANUAL, MeteringMode.SPOT),
        PresetConfig("sports", "运动", { Icon(Icons.Outlined.DirectionsRun, null, modifier = Modifier.size(14.dp)) }, 800, 1f / 500f, 4.0f, 5500, FocusMode.CONTINUOUS, MeteringMode.MATRIX),
        PresetConfig("street", "街拍", { Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(14.dp)) }, 400, 1f / 250f, 5.6f, 5500, FocusMode.CONTINUOUS, MeteringMode.CENTER),
        PresetConfig("food", "美食", { Icon(Icons.Outlined.Restaurant, null, modifier = Modifier.size(14.dp)) }, 200, 1f / 60f, 2.8f, 5000, FocusMode.AUTO, MeteringMode.SPOT),
        PresetConfig("indoor", "室内", { Icon(Icons.Outlined.Home, null, modifier = Modifier.size(14.dp)) }, 800, 1f / 60f, 4.0f, 4000, FocusMode.AUTO, MeteringMode.CENTER),
        PresetConfig("backlight", "逆光", { Icon(Icons.Outlined.WbSunny, null, modifier = Modifier.size(14.dp)) }, 200, 1f / 125f, 2.8f, 6500, FocusMode.AUTO, MeteringMode.SPOT)
    )

    // ========== 计算 EV 值 ==========
    fun calculateEV(): Float {
        // EV = log2(N² / t) - log2(ISO / 100)
        // N = aperture, t = shutter speed in seconds
        val apertureEv = log2(aperture * aperture)
        val shutterEv = -log2(shutterSpeed)
        val isoEv = -log2(iso.toFloat() / 100f)
        return apertureEv + shutterEv + isoEv + exposureCompensation
    }

    val currentEV = calculateEV()

    // ========== 参数关系提示 ==========
    fun getParamHint(): String? {
        return when {
            iso >= 6400 -> "高ISO会增加噪点，建议降低ISO或使用三脚架"
            iso >= 3200 -> "较高ISO可能产生可见噪点"
            aperture <= 2.0f -> "大光圈景深浅，注意对焦准确性"
            aperture >= 16f -> "小光圈可能产生衍射，影响锐度"
            shutterSpeed >= 1f -> "慢速快门容易手抖模糊，建议使用三脚架"
            shutterSpeed <= 1f / 500f -> "高速快门会减少进光量，注意曝光"
            focalLength >= 100 -> "长焦距需要更高快门速度防抖（1/${focalLength}s以上）"
            else -> null
        }
    }

    val paramHint = getParamHint()

    // ========== 参数联动逻辑 ==========
    fun applyLinkage(changedParam: String) {
        if (!linkageEnabled) return
        when (changedParam) {
            "iso" -> {
                // ISO 变化时，如果过曝则加快快门，欠曝则减慢快门
                val ev = calculateEV()
                if (ev > 12f) {
                    // 过曝，加快快门
                    val targetShutter = shutterSpeed / 2f
                    val snapped = snapToNearestShutter(targetShutter, shutterSteps)
                    if (snapped != shutterSpeed) shutterSpeed = snapped
                } else if (ev < 8f) {
                    // 欠曝，减慢快门
                    val targetShutter = shutterSpeed * 2f
                    val snapped = snapToNearestShutter(targetShutter, shutterSteps)
                    if (snapped != shutterSpeed) shutterSpeed = snapped
                }
            }
            "aperture" -> {
                // 光圈变化时，调整快门补偿
                val ev = calculateEV()
                if (ev > 12f) {
                    val targetShutter = shutterSpeed / 2f
                    shutterSpeed = snapToNearestShutter(targetShutter, shutterSteps)
                } else if (ev < 8f) {
                    val targetShutter = shutterSpeed * 2f
                    shutterSpeed = snapToNearestShutter(targetShutter, shutterSteps)
                }
            }
            "shutter" -> {
                // 快门变化时，调整ISO补偿
                val ev = calculateEV()
                if (ev > 12f && iso > 100) {
                    iso = snapToNearestISO(iso / 2, isoSteps)
                } else if (ev < 8f && iso < 12800) {
                    iso = snapToNearestISO(iso * 2, isoSteps)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // ========== TopAppBar ==========
        TopAppBar(
            title = { Text("参数精细调节", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            actions = {
                // 参数联动开关
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    linkageEnabled = !linkageEnabled
                }) {
                    Icon(
                        if (linkageEnabled) Icons.Default.Link else Icons.Default.LinkOff,
                        "参数联动",
                        tint = if (linkageEnabled) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                // 重置按钮
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    iso = 100
                    shutterSpeed = 1f / 125f
                    aperture = 2.8f
                    whiteBalance = 5500
                    focalLength = 23
                    exposureCompensation = 0f
                    focusMode = FocusMode.AUTO
                    meteringMode = MeteringMode.MATRIX
                    selectedPreset = null
                }) {
                    Icon(Icons.Default.Refresh, "重置", tint = MaterialTheme.colorScheme.onBackground)
                }
                // 应用按钮
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onApply(
                        CameraParams(
                            iso = iso,
                            shutterSpeed = shutterSpeed,
                            aperture = aperture,
                            whiteBalance = whiteBalance,
                            focalLength = focalLength,
                            exposureCompensation = exposureCompensation
                        )
                    )
                }) {
                    Icon(Icons.Default.Check, "应用", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        // ========== 曝光表 + 直方图区域 ==========
        ExposureHistogramSection(
            currentEV = currentEV,
            iso = iso,
            shutterSpeed = shutterSpeed,
            aperture = aperture
        )

        // ========== 快速预设芯片 ==========
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(presets) { preset ->
                val isSelected = selectedPreset == preset.id
                PresetChip(
                    label = preset.label,
                    icon = preset.icon,
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedPreset = preset.id
                        iso = preset.iso
                        shutterSpeed = preset.shutter
                        aperture = preset.aperture
                        whiteBalance = preset.wb
                        focusMode = preset.focusMode
                        meteringMode = preset.meteringMode
                    }
                )
            }
        }

        // ========== 参数调节列表 ==========
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ISO 离散调节
            item {
                DiscreteParamCard(
                    title = "ISO",
                    subtitle = "感光度，影响噪点和曝光",
                    valueLabel = iso.toString(),
                    steps = isoSteps.map { it.toString() to it.toFloat() },
                    currentValue = iso.toFloat(),
                    onValueChange = { newIso ->
                        iso = newIso.toInt()
                        applyLinkage("iso")
                    },
                    color = Color(0xFF9C27B0)
                )
            }

            // 快门速度离散调节
            item {
                DiscreteParamCard(
                    title = "快门",
                    subtitle = "曝光时间，影响运动模糊",
                    valueLabel = formatShutterSpeed(shutterSpeed),
                    steps = shutterSteps.map { it.first to it.second },
                    currentValue = shutterSpeed,
                    onValueChange = { newShutter ->
                        shutterSpeed = newShutter
                        applyLinkage("shutter")
                    },
                    color = Color(0xFF2196F3)
                )
            }

            // 光圈离散调节
            item {
                DiscreteParamCard(
                    title = "光圈",
                    subtitle = "景深控制，影响背景虚化",
                    valueLabel = "f/$aperture",
                    steps = apertureSteps.map { "f/$it" to it },
                    currentValue = aperture,
                    onValueChange = { newAperture ->
                        aperture = newAperture
                        applyLinkage("aperture")
                    },
                    color = HasselbladOrange
                )
            }

            // 白平衡离散调节
            item {
                DiscreteParamCard(
                    title = "白平衡",
                    subtitle = "色温调节，影响色彩冷暖",
                    valueLabel = "${whiteBalance}K",
                    steps = wbSteps.map { "${it}K" to it.toFloat() },
                    currentValue = whiteBalance.toFloat(),
                    onValueChange = { whiteBalance = it.toInt() },
                    color = Color(0xFFFFEB3B)
                )
            }

            // 焦距调节
            item {
                ContinuousParamCard(
                    title = "焦距",
                    subtitle = "镜头焦距，影响视角和压缩感",
                    value = focalLength.toFloat(),
                    valueRange = 8f..400f,
                    valueLabel = "${focalLength}mm",
                    onValueChange = { focalLength = it.toInt() },
                    color = Color(0xFFE91E63)
                )
            }

            // 曝光补偿
            item {
                ContinuousParamCard(
                    title = "曝光补偿",
                    subtitle = "手动调整曝光量",
                    value = exposureCompensation,
                    valueRange = -3f..3f,
                    valueLabel = "${if (exposureCompensation >= 0) "+" else ""}${String.format("%.1f", exposureCompensation)} EV",
                    onValueChange = { exposureCompensation = it },
                    color = Color(0xFF4CAF50)
                )
            }

            // 对焦模式
            item {
                FocusModeCard(
                    selectedMode = focusMode,
                    onModeChange = { focusMode = it }
                )
            }

            // 测光模式
            item {
                MeteringModeCard(
                    selectedMode = meteringMode,
                    onModeChange = { meteringMode = it }
                )
            }

            // 参数关系提示
            item {
                AnimatedVisibility(
                    visible = paramHint != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    if (paramHint != null) {
                        ParamHintCard(hint = paramHint)
                    }
                }
            }

            // 底部留白
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // ========== 参数摘要栏 ==========
        ParamSummaryBar(
            iso = iso,
            shutterSpeed = shutterSpeed,
            aperture = aperture,
            whiteBalance = whiteBalance,
            currentEV = currentEV,
            linkageEnabled = linkageEnabled
        )
    }
}

// ==================== 曝光表 + 直方图区域 ====================

@Composable
private fun ExposureHistogramSection(
    currentEV: Float,
    iso: Int,
    shutterSpeed: Float,
    aperture: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 曝光表
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "曝光",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val trackWidth = maxWidth
                    // EV 刻度线
                    val evRange = -5f..17f
                    val evNormalized = ((currentEV - evRange.start) / (evRange.endInclusive - evRange.start))
                        .coerceIn(0f, 1f)

                    // 中间参考线 (EV 12 = 正常日光曝光)
                    val refLineFraction = (12f - evRange.start) / (evRange.endInclusive - evRange.start)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = trackWidth * refLineFraction)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    )

                    // EV 指示器
                    val indicatorColor = when {
                        currentEV > 14f -> Color(0xFFFF5722) // 过曝
                        currentEV < 8f -> Color(0xFF2196F3)  // 欠曝
                        else -> Color(0xFF4CAF50)             // 正常
                    }
                    val animatedFraction by animateFloatAsState(
                        targetValue = evNormalized,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "evIndicator"
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = trackWidth * animatedFraction - 10.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EV ${String.format("%.1f", currentEV)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        currentEV > 14f -> Color(0xFFFF5722)
                        currentEV < 8f -> Color(0xFF2196F3)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 直方图
            HistogramPlaceholder(iso = iso, shutterSpeed = shutterSpeed, aperture = aperture)
        }
    }
}

@Composable
private fun HistogramPlaceholder(
    iso: Int,
    shutterSpeed: Float,
    aperture: Float
) {
    // 基于曝光参数计算真实亮度分布直方图
    // 使用高斯分布模拟相机传感器在不同 EV 下的亮度响应特征
    val brightness = calculateBrightness(iso, shutterSpeed, aperture)
    val barHeights = remember(brightness) {
        (0..31).map { i ->
            // 将 bin 索引映射到 0-255 亮度范围
            val binCenter = i * 255f / 31f
            // 基于当前 EV 计算期望亮度中心
            val evCenter = brightness * 255f
            // 高斯分布：sigma 随 EV 变化（低 EV 更宽，高 EV 更窄）
            val sigma = 40f + (1f - brightness) * 30f
            val dist = binCenter - evCenter
            val gaussian = kotlin.math.exp(-(dist * dist) / (2f * sigma * sigma))
            // 添加基于参数的确定性微扰（非随机，保证重组一致性）
            val perturbation = kotlin.math.sin(i * 0.7f + brightness * 10f) * 0.05f
            (gaussian * 0.85f + perturbation).coerceIn(0.05f, 1f)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        barHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(height)
                    .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                    .background(
                        HasselbladOrange.copy(alpha = 0.6f)
                    )
            )
        }
    }
}

private fun calculateBrightness(iso: Int, shutterSpeed: Float, aperture: Float): Float {
    // 简化的亮度计算，映射到 0-1
    val ev = log2(aperture * aperture) - log2(shutterSpeed) - log2(iso.toFloat() / 100f)
    return ((ev - 4f) / 14f).coerceIn(0f, 1f)
}

// ==================== 离散参数卡片 ====================

@Composable
private fun DiscreteParamCard(
    title: String,
    subtitle: String,
    valueLabel: String,
    steps: List<Pair<String, Float>>,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    val haptic = LocalHapticFeedback.current

    // 找到当前值最近的步骤索引
    val currentIndex = remember(currentValue, steps) {
        steps.indices.minByOrNull { kotlin.math.abs(steps[it].second - currentValue) } ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 当前值标签
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 离散步进滑块
            Slider(
                value = currentIndex.toFloat(),
                onValueChange = { index ->
                    val snappedIndex = index.roundToInt().coerceIn(0, steps.lastIndex)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(steps[snappedIndex].second)
                },
                valueRange = 0f..steps.lastIndex.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 快速选项行 - 显示所有选项，可滚动
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(steps) { (label, stepValue) ->
                    val isSelected = kotlin.math.abs(stepValue - currentValue) < 0.001f
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "chipScale"
                    )
                    Box(
                        modifier = Modifier
                            .scale(animatedScale)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) color.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) color else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onValueChange(stepValue)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ==================== 连续参数卡片 ====================

@Composable
private fun ContinuousParamCard(
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = valueLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = value,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(it)
                },
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color
                )
            )
        }
    }
}

// ==================== 对焦模式卡片 ====================

@Composable
private fun FocusModeCard(
    selectedMode: FocusMode,
    onModeChange: (FocusMode) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "对焦模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "focusWeight"
                    )
                    Surface(
                        modifier = Modifier.weight(animatedWeight),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) BorderStroke(1.dp, HasselbladOrange) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onModeChange(mode)
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    FocusMode.AUTO -> Icons.Outlined.CenterFocusStrong
                                    FocusMode.MANUAL -> Icons.Outlined.Tune
                                    FocusMode.CONTINUOUS -> Icons.Outlined.CenterFocusWeak
                                },
                                contentDescription = mode.label,
                                tint = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 测光模式卡片 ====================

@Composable
private fun MeteringModeCard(
    selectedMode: MeteringMode,
    onModeChange: (MeteringMode) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "测光模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeteringMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "meteringWeight"
                    )
                    Surface(
                        modifier = Modifier.weight(animatedWeight),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) BorderStroke(1.dp, HasselbladOrange) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onModeChange(mode)
                                }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    MeteringMode.MATRIX -> Icons.Outlined.GridOn
                                    MeteringMode.CENTER -> Icons.Outlined.FilterCenterFocus
                                    MeteringMode.SPOT -> Icons.Outlined.GpsFixed
                                },
                                contentDescription = mode.label,
                                tint = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== 参数提示卡片 ====================

@Composable
private fun ParamHintCard(hint: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = "提示",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF9800)
            )
        }
    }
}

// ==================== 预设芯片 ====================

@Composable
private fun PresetChip(
    label: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit
) {
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (selected) HasselbladOrange else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipBg"
    )
    val animatedElevation by animateDpAsState(
        targetValue = if (selected) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipElevation"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "chipScale"
    )

    Surface(
        modifier = Modifier.scale(animatedScale),
        shape = RoundedCornerShape(20.dp),
        color = animatedBackgroundColor,
        shadowElevation = animatedElevation
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                icon()
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ==================== 参数摘要栏 ====================

@Composable
private fun ParamSummaryBar(
    iso: Int,
    shutterSpeed: Float,
    aperture: Float,
    whiteBalance: Int,
    currentEV: Float,
    linkageEnabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // 参数行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ParamSummaryItem("ISO", iso.toString())
                ParamSummaryItem("快门", formatShutterSpeed(shutterSpeed))
                ParamSummaryItem("光圈", "f/$aperture")
                ParamSummaryItem("WB", "${whiteBalance}K")
            }

            // EV 和联动状态
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val evColor = when {
                    currentEV > 14f -> Color(0xFFFF5722)
                    currentEV < 8f -> Color(0xFF2196F3)
                    else -> Color(0xFF4CAF50)
                }
                val evLabel = when {
                    currentEV > 14f -> "过曝"
                    currentEV < 8f -> "欠曝"
                    else -> "正常"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(evColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EV ${String.format("%.1f", currentEV)} · $evLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = evColor
                    )
                }

                if (linkageEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = "联动",
                            modifier = Modifier.size(14.dp),
                            tint = HasselbladOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "参数联动",
                            style = MaterialTheme.typography.bodySmall,
                            color = HasselbladOrange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamSummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = HasselbladOrange
        )
    }
}

// ==================== 枚举定义 ====================

enum class FocusMode(val label: String) {
    AUTO("自动对焦"),
    MANUAL("手动对焦"),
    CONTINUOUS("连续对焦")
}

enum class MeteringMode(val label: String) {
    MATRIX("矩阵测光"),
    CENTER("中央重点"),
    SPOT("点测光")
}

// ==================== 工具函数 ====================

/**
 * 格式化快门速度
 */
private fun formatShutterSpeed(speed: Float): String {
    return when {
        speed >= 1f -> "${speed.toInt()}s"
        else -> {
            val denominator = (1f / speed).roundToInt().coerceAtLeast(1)
            "1/${denominator}s"
        }
    }
}

/**
 * 将快门速度吸附到最近的标称值
 */
private fun snapToNearestShutter(target: Float, steps: List<Pair<String, Float>>): Float {
    return steps.minByOrNull { kotlin.math.abs(it.second - target) }?.second ?: target
}

/**
 * 将ISO吸附到最近的标称值
 */
private fun snapToNearestISO(target: Int, steps: List<Int>): Int {
    return steps.minByOrNull { kotlin.math.abs(it - target) } ?: target
}

/**
 * 相机参数数据类
 */
data class CameraParams(
    val iso: Int = 100,
    val shutterSpeed: Float = 125f,
    val aperture: Float = 2.8f,
    val whiteBalance: Int = 5500,
    val focalLength: Int = 23,
    val exposureCompensation: Float = 0f
)
