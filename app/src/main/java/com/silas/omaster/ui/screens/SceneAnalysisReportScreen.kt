package com.silas.omaster.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.silas.omaster.data.local.RecipeHistoryManager
import com.silas.omaster.data.local.RecipeRecord
import com.silas.omaster.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Layer 4: 大师洞察层 - 场景分析报告数据看板
 *
 * 「哈苏大师之眼数据看板」
 * - 场景分布统计（柱状图）
 * - 拍摄习惯洞察（黄金时刻利用率、夜景占比增长）
 * - 胶片风格使用排行
 * - 哈苏大师建议（个性化优化建议）
 *
 * 已修复：
 * - 使用真实数据统计替代硬编码模拟数据
 * - 实现导出、分享、查看功能
 * - 添加时间范围选择
 * - 添加概览卡片
 */

/**
 * 场景分布数据
 */
data class SceneDistribution(
    val category: String,
    val name: String,
    val count: Int,
    val percentage: Int,
    val color: Color
)

/**
 * 拍摄习惯洞察
 */
data class ShootingInsight(
    val icon: String,
    val title: String,
    val value: String,
    val trend: String? = null,
    val warning: Boolean = false
)

/**
 * 胶片使用统计
 */
data class FilmUsage(
    val name: String,
    val count: Int,
    val percentage: Int
)

/**
 * 大师建议
 */
data class MasterSuggestion(
    val icon: String,
    val title: String,
    val description: String,
    val recommendation: String
)

/**
 * 拍摄习惯概览
 */
data class ShootingHabits(
    val totalPhotos: Int,
    val totalRecipes: Int,
    val favoriteScene: String,
    val favoriteFilm: String,
    val avgConfidence: Float,
    val streakDays: Int,
    val lastShootDate: String
)

/**
 * 场景分析报告页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneAnalysisReportScreen(
    onBack: () -> Unit = {},
    onViewDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    val recipeHistoryManager = remember { RecipeHistoryManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // 时间范围选择
    var timeRange by remember { mutableStateOf("month") }
    var isLoading by remember { mutableStateOf(true) }

    // 真实统计数据
    var sceneStats by remember { mutableStateOf<List<SceneDistribution>>(emptyList()) }
    var filmUsage by remember { mutableStateOf<List<FilmUsage>>(emptyList()) }
    var habits by remember { mutableStateOf<ShootingHabits?>(null) }
    var masterTips by remember { mutableStateOf<List<String>>(emptyList()) }

    // 从本地存储加载真实数据
    LaunchedEffect(timeRange) {
        isLoading = true
        kotlinx.coroutines.delay(300)

        // 获取指定时间范围的配方记录
        val savedRecipes = recipeHistoryManager.getRecipesByTimeRange(timeRange)

        // 计算场景统计（使用标准库 Pair 替代自定义 MutablePair）
        val sceneCounts = mutableMapOf<String, Pair<String, Int>>()
        savedRecipes.forEach { recipe ->
            val existing = sceneCounts[recipe.sceneId]
            sceneCounts[recipe.sceneId] = if (existing != null) {
                existing.copy(second = existing.second + 1)
            } else {
                Pair(recipe.sceneName, 1)
            }
        }

        val totalScenes = sceneCounts.values.sumOf { it.second }
        val sortedScenes = sceneCounts.entries
            .sortedByDescending { it.value.second }
            .take(7)
            .mapIndexed { index, entry ->
                val percentage = if (totalScenes > 0) (entry.value.second * 100 / totalScenes) else 0
                SceneDistribution(
                    category = entry.key,
                    name = entry.value.first,
                    count = entry.value.second,
                    percentage = percentage,
                    color = getSceneColor(index)
                )
            }

        sceneStats = sortedScenes

        // 计算胶片使用统计
        val filmCounts = mutableMapOf<String, MutablePair<String, Int>>()
        savedRecipes.forEach { recipe ->
            val filmId = recipe.filmId ?: "unknown"
            val filmName = recipe.filmName ?: "未知胶片"
            val existing = filmCounts.getOrPut(filmId) { MutablePair(filmName, 0) }
            existing.second++
        }

        val totalFilms = filmCounts.values.sumOf { it.second }
        val sortedFilms = filmCounts.entries
            .sortedByDescending { it.value.second }
            .take(5)
            .map { entry ->
                val percentage = if (totalFilms > 0) (entry.value.second * 100 / totalFilms) else 0
                FilmUsage(
                    name = entry.value.first,
                    count = entry.value.second,
                    percentage = percentage
                )
            }

        filmUsage = sortedFilms

        // 计算拍摄习惯
        val totalPhotos = savedRecipes.size
        val uniqueScenes = savedRecipes.map { it.sceneId }.distinct().size
        val avgConf = if (savedRecipes.isNotEmpty()) {
            savedRecipes.map { it.confidence }.average()
        } else 0f

        // 计算连续拍摄天数
        val dates = savedRecipes.map { formatDate(it.timestamp) }.distinct()
        val streakDays = calculateStreakDays(dates)

        habits = ShootingHabits(
            totalPhotos = totalPhotos,
            totalRecipes = uniqueScenes,
            favoriteScene = sortedScenes.firstOrNull()?.name ?: "暂无数据",
            favoriteFilm = sortedFilms.firstOrNull()?.name ?: "暂无数据",
            avgConfidence = avgConf.toFloat(),
            streakDays = streakDays,
            lastShootDate = dates.firstOrNull() ?: "从未"
        )

        // 生成大师建议
        masterTips = generateMasterTips(sortedScenes, sortedFilms, totalPhotos)

        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "拍摄分析报告",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // 导出报告：将报告保存到外部文件并通过系统分享触发保存
                        val reportText = buildReportText(habits, sceneStats, filmUsage, masterTips)
                        scope.launch {
                            try {
                                val file = File(
                                    context.getExternalFilesDir(null),
                                    "scene_report_${System.currentTimeMillis()}.txt"
                                )
                                file.writeText(reportText)
                                ShareCompat.IntentBuilder(context)
                                    .setType("text/plain")
                                    .setSubject("哈苏大师之眼 - 拍摄分析报告")
                                    .setText(reportText)
                                    .setStream(androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    ))
                                    .startChooser()
                            } catch (e: Exception) {
                                // 失败时回退为分享文本
                                ShareCompat.IntentBuilder(context)
                                    .setType("text/plain")
                                    .setSubject("哈苏大师之眼 - 拍摄分析报告")
                                    .setText(reportText)
                                    .startChooser()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, "导出", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                    IconButton(onClick = {
                        // 分享报告文本
                        val reportText = buildReportText(habits, sceneStats, filmUsage, masterTips)
                        ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setSubject("哈苏大师之眼 - 拍摄分析报告")
                            .setText(reportText)
                            .startChooser()
                    }) {
                        Icon(Icons.Default.Share, "分享", tint = HasselbladOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = HasselbladOrange,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "正在分析你的拍摄数据...",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 时间范围选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRangeChip("本周", "week", timeRange == "week") { timeRange = "week" }
                        TimeRangeChip("本月", "month", timeRange == "month") { timeRange = "month" }
                        TimeRangeChip("本年", "year", timeRange == "year") { timeRange = "year" }
                        TimeRangeChip("全部", "all", timeRange == "all") { timeRange = "all" }
                    }

                    // 概览卡片
                    habits?.let { OverviewCards(it) }

                    // 拍摄偏好
                    habits?.let { ShootingPreferenceCard(it) }

                    // 场景分布
                    SceneDistributionCard(sceneStats)

                    // 胶片风格使用排行
                    FilmUsageCard(filmUsage)

                    // 大师建议
                    MasterTipsCard(masterTips)

                    // 底部间距
                    Spacer(modifier = Modifier.height(80.dp))
                }

                // 底部操作栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        .padding(vertical = 12.dp)
                ) {
                    Button(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("查看详细数据", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * 时间范围选择芯片
 */
@Composable
private fun TimeRangeChip(
    label: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        )
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 概览卡片
 */
@Composable
private fun OverviewCards(habits: ShootingHabits) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewCard(
            icon = Icons.Default.Camera,
            label = "总照片",
            value = habits.totalPhotos.toString(),
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            icon = Icons.Default.Image,
            label = "配方数",
            value = habits.totalRecipes.toString(),
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OverviewCard(
            icon = Icons.Default.TrendingUp,
            label = "连续拍摄",
            value = "${habits.streakDays}天",
            modifier = Modifier.weight(1f)
        )
        OverviewCard(
            icon = Icons.Default.Visibility,
            label = "平均置信度",
            value = "${Math.round(habits.avgConfidence * 100)}%",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 单个概览卡片
 */
@Composable
private fun OverviewCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = HasselbladOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * 拍摄偏好卡片
 */
@Composable
private fun ShootingPreferenceCard(habits: ShootingHabits) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("拍摄偏好", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("最爱场景", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                Text(habits.favoriteScene, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("最爱胶片", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                Text(habits.favoriteFilm, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("最后拍摄", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                Text(habits.lastShootDate, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
            }
        }
    }
}

/**
 * 场景分布卡片
 */
@Composable
private fun SceneDistributionCard(stats: List<SceneDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Camera, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("场景分布", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (stats.isNotEmpty()) {
                stats.forEach { scene ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            scene.name,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier.width(48.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(scene.percentage.toFloat() / 100f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(scene.color)
                            )

                            Text(
                                "${scene.count} (${scene.percentage}%)",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text("暂无场景数据", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 14.sp)
            }
        }
    }
}

/**
 * 胶片使用卡片
 */
@Composable
private fun FilmUsageCard(films: List<FilmUsage>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("胶片使用排行", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (films.isNotEmpty()) {
                films.forEachIndexed { index, film ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp)
                        )

                        Text(
                            film.name,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.width(96.dp)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(film.percentage.toFloat() / 100f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(HasselbladOrange, Color(0xFFFF8A50))
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("${film.count}次", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text("暂无胶片使用数据", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 14.sp)
            }
        }
    }
}

/**
 * 大师建议卡片
 */
@Composable
private fun MasterTipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("大师建议", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (tips.isNotEmpty()) {
                tips.forEach { tip ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = HasselbladOrange, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            tip,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text("开始拍摄你的第一张照片，哈苏大师将为你提供个性化建议。", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 14.sp)
            }
        }
    })
}

/**
 * 格式化日期
 */
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 构建报告文本（用于导出/分享）
 */
private fun buildReportText(
    habits: ShootingHabits?,
    scenes: List<SceneDistribution>,
    films: List<FilmUsage>,
    tips: List<String>
): String {
    val builder = StringBuilder()
    builder.appendLine("===== 哈苏大师之眼 · 拍摄分析报告 =====")
    builder.appendLine()
    habits?.let {
        builder.appendLine("总照片数: ${it.totalPhotos}")
        builder.appendLine("配方数: ${it.totalRecipes}")
        builder.appendLine("最爱场景: ${it.favoriteScene}")
        builder.appendLine("最爱胶片: ${it.favoriteFilm}")
        builder.appendLine("平均置信度: ${(it.avgConfidence * 100).toInt()}%")
        builder.appendLine("连续拍摄: ${it.streakDays}天")
        builder.appendLine("最后拍摄: ${it.lastShootDate}")
    }
    builder.appendLine()
    builder.appendLine("--- 场景分布 ---")
    if (scenes.isEmpty()) {
        builder.appendLine("（暂无数据）")
    } else {
        scenes.forEach { builder.appendLine("${it.name}: ${it.count} (${it.percentage}%)") }
    }
    builder.appendLine()
    builder.appendLine("--- 胶片使用排行 ---")
    if (films.isEmpty()) {
        builder.appendLine("（暂无数据）")
    } else {
        films.forEach { builder.appendLine("${it.name}: ${it.count} (${it.percentage}%)") }
    }
    builder.appendLine()
    builder.appendLine("--- 大师建议 ---")
    if (tips.isEmpty()) {
        builder.appendLine("（暂无建议）")
    } else {
        tips.forEach { builder.appendLine("• $it") }
    }
    builder.appendLine()
    builder.appendLine("—— 用哈苏之眼，记录每一刻的光影 ——")
    return builder.toString()
}

/**
 * 计算连续拍摄天数
 */
private fun calculateStreakDays(dates: List<String>): Int {
    if (dates.isEmpty()) return 0

    val today = formatDate(System.currentTimeMillis())
    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val todayDate = sdf.parse(today) ?: return 0

    var streak = 0
    val sortedDates = dates.mapNotNull { sdf.parse(it) }.sortedByDescending { it }

    for (i in sortedDates.indices) {
        val expectedDate = Date(todayDate.time - i * 24 * 60 * 60 * 1000L)
        if (sortedDates.getOrNull(i)?.time == expectedDate.time) {
            streak++
        } else {
            break
        }
    }

    return streak
}

/**
 * 获取场景颜色
 */
private fun getSceneColor(index: Int): Color {
    return when (index) {
        0 -> HasselbladOrange
        1 -> Color(0xFF4CAF50)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFF2196F3)
        4 -> Color(0xFF9C27B0)
        5 -> Color(0xFFE91E63)
        else -> Color(0xFF607D8B)
    }
}

/**
 * 生成大师建议
 */
private fun generateMasterTips(
    scenes: List<SceneDistribution>,
    films: List<FilmUsage>,
    total: Int
): List<String> {
    val tips = mutableListOf<String>()

    if (total == 0) {
        tips.add("开始拍摄你的第一张照片，哈苏大师将为你提供个性化建议。")
    } else {
        if (scenes.isNotEmpty() && scenes[0].percentage > 40) {
            tips.add("你在${scenes[0].name}上投入了大量精力，建议尝试其他场景类型以拓展创作视野。")
        }

        if (films.isNotEmpty()) {
            tips.add("${films[0].name}是你的最爱，它的色彩特性非常适合你的拍摄风格。")
        }

        if (total < 10) {
            tips.add("你的拍摄量还有提升空间，建议每周至少拍摄3-5张照片来培养摄影眼。")
        } else if (total > 50) {
            tips.add("你已经积累了相当丰富的拍摄经验，可以考虑尝试更高级的创作技巧。")
        }

        tips.add("黄金时刻（日出后/日落前1小时）是拍摄风景和人像的最佳时机。")
        tips.add("使用哈苏的HNCS自然色彩解决方案，可以获得更真实的色彩还原。")
    }

    return tips
}