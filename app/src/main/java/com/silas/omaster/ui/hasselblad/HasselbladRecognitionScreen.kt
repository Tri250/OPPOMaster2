package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.analyzer.AnalysisResult
import com.silas.omaster.ai.model.FilmPreset
import com.silas.omaster.ai.model.HasselbladParams
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 哈苏大师识别结果页面
 * 「哈苏大师之眼」完整设计
 *
 * 设计规范：
 * - 纯黑背景 #0A0A0A
 * - 哈苏橙强调色 #FF6B35
 * - 圆角 16px 卡片
 * - HNCS 水印
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladRecognitionScreen(
    originalImageUrl: String,
    processedImageUrl: String,
    analysisResult: AnalysisResult,
    onBack: () -> Unit,
    onShareRecipe: () -> Unit,
    onRetake: () -> Unit,
    onApplyOptimization: () -> Unit,
    onSaveRecipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilm by remember { mutableStateOf<FilmPreset?>(null) }
    var currentParams by remember { mutableStateOf(analysisResult.primaryScene.hasselbladParams) }

    Scaffold(
        modifier = modifier.background(HasselbladTheme.PureBlack),
        topBar = {
            HasselbladTopBar(
                onBack = onBack,
                onShareRecipe = onShareRecipe
            )
        },
        bottomBar = {
            HasselbladBottomBar(
                onRetake = onRetake,
                onApplyOptimization = onApplyOptimization,
                onSaveRecipe = onSaveRecipe
            )
        },
        containerColor = HasselbladTheme.PureBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Before/After 滑杆对比
            HasselbladCompareSlider(
                originalImageUrl = originalImageUrl,
                processedImageUrl = processedImageUrl,
                modifier = Modifier.fillMaxWidth()
            )

            // 哈苏大师识别结果卡片
            HasselbladRecognitionCard(
                profile = analysisResult.primaryScene,
                confidence = analysisResult.confidence,
                alternativeScenes = analysisResult.alternativeScenes
            )

            // 推荐胶片卡片
            HasselbladFilmRecommendationCard(
                films = analysisResult.primaryScene.recommendedFilm,
                selectedFilmId = selectedFilm?.id,
                onFilmSelected = { film ->
                    selectedFilm = film
                }
            )

            // 哈苏大师参数滑块
            HasselbladParamsSliderCard(
                params = currentParams,
                onParamsChange = { newParams ->
                    currentParams = newParams
                },
                isEditable = false // 默认只读，可切换为可编辑
            )

            // 大师建议卡片
            HasselbladMasterTipsCard(
                tips = analysisResult.primaryScene.masterTips
            )

            // HNCS 水印
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                HasselbladHNCSWatermark()
            }
        }
    }
}

/**
 * 顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasselbladTopBar(
    onBack: () -> Unit,
    onShareRecipe: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "AI 出片",
                color = HasselbladTheme.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = HasselbladTheme.TextPrimary
                )
            }
        },
        actions = {
            TextButton(onClick = onShareRecipe) {
                Text(
                    text = "分享配方",
                    color = HasselbladTheme.HasselbladOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = HasselbladTheme.PureBlack,
            titleContentColor = HasselbladTheme.TextPrimary
        )
    )
}

/**
 * 底部操作栏
 */
@Composable
fun HasselbladBottomBar(
    onRetake: () -> Unit,
    onApplyOptimization: () -> Unit,
    onSaveRecipe: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HasselbladTheme.PureBlack,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 重拍按钮
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HasselbladTheme.TextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = HasselbladTheme.DividerColor
                )
            ) {
                Text(
                    text = "重拍",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 一键哈苏优化按钮（主按钮）
            Button(
                onClick = onApplyOptimization,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladTheme.HasselbladOrange,
                    contentColor = HasselbladTheme.TextPrimary
                )
            ) {
                Text(
                    text = "一键哈苏优化",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 保存配方按钮
            OutlinedButton(
                onClick = onSaveRecipe,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = HasselbladTheme.HasselbladOrange
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = HasselbladTheme.HasselbladOrange
                )
            ) {
                Text(
                    text = "保存配方",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 简化版哈苏识别结果页面
 */
@Composable
fun HasselbladRecognitionScreenSimple(
    sceneName: String,
    sceneIcon: String,
    confidence: Float,
    params: HasselbladParams,
    tips: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HasselbladTheme.PureBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 识别结果
        HasselbladRecognitionCardSimple(
            sceneName = sceneName,
            sceneIcon = sceneIcon,
            confidence = confidence
        )

        // 参数显示
        HasselbladParamsDisplaySimple(params = params)

        // 建议显示
        HasselbladMasterTipsSimple(tips = tips)
    }
}