package com.silas.omaster.ui.features

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.silas.omaster.ai.SceneRecognitionManager
import com.silas.omaster.ai.analyzer.AnalysisResult
import com.silas.omaster.ai.model.SceneCategory
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ui.hasselblad.*
import com.silas.omaster.ui.theme.HasselbladTheme
import kotlinx.coroutines.launch

/**
 * AI场景识别功能页面 - 哈苏大师版
 * 使用新的哈苏大师识别系统
 * 支持50+精细场景类型识别
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISceneRecognitionScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sceneManager = remember { SceneRecognitionManager.getInstance(context) }

    // 使用新的 SceneProfile 系统
    val supportedProfiles = sceneManager.supportedProfiles
    val profilesByCategory = sceneManager.profilesByCategory

    // 分析结果状态
    var analysisResult by remember { mutableStateOf<AnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<SceneProfile?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HasselbladTheme.PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = {
                Text(
                    "哈苏大师识别",
                    fontWeight = FontWeight.Bold,
                    color = HasselbladTheme.TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "返回",
                        tint = HasselbladTheme.TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HasselbladTheme.PureBlack,
                titleContentColor = HasselbladTheme.TextPrimary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 一键识别按钮
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = HasselbladTheme.HasselbladOrange.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "哈苏大师之眼",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HasselbladTheme.HasselbladOrange
                        )
                        Text(
                            text = "识别50+拍摄场景，自动推荐哈苏大师参数",
                            style = MaterialTheme.typography.bodySmall,
                            color = HasselbladTheme.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                scope.launch {
                                    isAnalyzing = true
                                    // 模拟分析（实际应从相机获取图片）
                                    // val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.sample)
                                    // analysisResult = sceneManager.analyzeSceneDetailed(bitmap)
                                    
                                    // 模拟分析结果
                                    val randomProfile = supportedProfiles.random()
                                    analysisResult = AnalysisResult(
                                        primaryScene = randomProfile.copy(confidence = 0.85f),
                                        confidence = 0.85f,
                                        alternativeScenes = supportedProfiles
                                            .filter { it.category == randomProfile.category && it.id != randomProfile.id }
                                            .take(2)
                                            .map { it.copy(confidence = 0.15f) },
                                        colorProfile = com.silas.omaster.ai.analyzer.ColorProfile(
                                            avgRed = 180, avgGreen = 120, avgBlue = 100,
                                            warmthRatio = 0.55f, coolRatio = 0.15f,
                                            greenDominance = 0.8f, blueDominance = 0.6f, redDominance = 1.2f,
                                            colorVariance = 0.3f,
                                            dominantTone = com.silas.omaster.ai.analyzer.DominantTone.WARM
                                        ),
                                        brightnessLevel = com.silas.omaster.ai.analyzer.BrightnessLevel.NORMAL,
                                        faceCount = 0,
                                        analysisTimeMs = 150
                                    )
                                    isAnalyzing = false
                                }
                            },
                            enabled = !isAnalyzing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HasselbladTheme.HasselbladOrange
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = HasselbladTheme.TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.CameraAlt, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("开始识别", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // 分析结果展示（使用新的哈苏大师呈现层）
            if (analysisResult != null) {
                item {
                    Text(
                        text = "哈苏大师分析结果",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladTheme.TextPrimary
                    )
                }

                // 识别结果卡片
                item {
                    HasselbladRecognitionCard(
                        profile = analysisResult!!.primaryScene,
                        confidence = analysisResult!!.confidence,
                        alternativeScenes = analysisResult!!.alternativeScenes
                    )
                }

                // 胶片推荐卡片
                item {
                    HasselbladFilmRecommendationCard(
                        films = analysisResult!!.primaryScene.recommendedFilm
                    )
                }

                // 参数显示
                item {
                    HasselbladParamsDisplaySimple(
                        params = analysisResult!!.primaryScene.hasselbladParams
                    )
                }

                // 大师建议
                item {
                    HasselbladMasterTipsCard(
                        tips = analysisResult!!.primaryScene.masterTips
                    )
                }

                // HNCS水印
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        HasselbladHNCSWatermark()
                    }
                }
            }

            // 场景分类列表
            profilesByCategory.forEach { (category, profiles) ->
                item {
                    Text(
                        text = "${category.icon} ${category.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladTheme.TextPrimary
                    )
                }

                items(profiles) { profile ->
                    HasselbladSceneProfileCard(
                        profile = profile,
                        isSelected = selectedProfile?.id == profile.id,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Select)
                            selectedProfile = profile
                            showDetailDialog = true
                        }
                    )
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 场景详情弹窗（使用新的哈苏大师设计）
    if (showDetailDialog && selectedProfile != null) {
        HasselbladSceneDetailDialog(
            profile = selectedProfile!!,
            onApply = {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                // 应用参数逻辑
                showDetailDialog = false
            },
            onDismiss = {
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                showDetailDialog = false
            }
        )
    }
}

/**
 * 哈苏场景卡片
 */
@Composable
private fun HasselbladSceneProfileCard(
    profile: SceneProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                HasselbladTheme.HasselbladOrange.copy(alpha = 0.15f)
            } else {
                HasselbladTheme.CardBackground
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 场景图标
            Text(
                text = profile.category.icon,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextPrimary
                )
                Text(
                    text = profile.subCategory,
                    style = MaterialTheme.typography.bodySmall,
                    color = HasselbladTheme.TextTertiary
                )
            }

            // 推荐胶片标签
            if (profile.recommendedFilm.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = HasselbladTheme.CardBackgroundHighlight
                ) {
                    Text(
                        text = profile.recommendedFilm.first().displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = HasselbladTheme.HasselbladOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = HasselbladTheme.TextTertiary
            )
        }
    }
}

/**
 * 哈苏场景详情弹窗
 */
@Composable
private fun HasselbladSceneDetailDialog(
    profile: SceneProfile,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp),
            colors = CardDefaults.cardColors(
                containerColor = HasselbladTheme.PureBlack
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // 标题
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.category.icon,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladTheme.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = profile.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HasselbladTheme.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 哈苏参数显示
                Text(
                    text = "哈苏大师参数",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = HasselbladTheme.HasselbladOrange
                )

                Spacer(modifier = Modifier.height(8.dp))

                HasselbladParamsDisplaySimple(params = profile.hasselbladParams)

                Spacer(modifier = Modifier.height(16.dp))

                // 推荐胶片
                if (profile.recommendedFilm.isNotEmpty()) {
                    Text(
                        text = "推荐胶片",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladTheme.HasselbladOrange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.recommendedFilm.take(3).forEach { film ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HasselbladTheme.CardBackgroundHighlight
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = film.displayName,
                                        color = HasselbladTheme.TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${film.matchPercent}%",
                                        color = HasselbladTheme.HasselbladOrange,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 大师建议
                if (profile.masterTips.isNotEmpty()) {
                    Text(
                        text = "大师建议",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HasselbladTheme.HasselbladOrange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    profile.masterTips.forEach { tip ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = HasselbladTheme.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HasselbladTheme.TextSecondary
                        )
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = onApply,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladTheme.HasselbladOrange
                        )
                    ) {
                        Text("应用哈苏参数", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}