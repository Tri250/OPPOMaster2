package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.silas.omaster.model.FilmPreset
import com.silas.omaster.model.HasselbladParams
import com.silas.omaster.ui.theme.*
import com.silas.omaster.util.ShareExportUtils
import kotlinx.coroutines.launch

/**
 * Layer 3: 大师呈现层 - 「哈苏大师之眼」识别结果页
 *
 * 完整设计规范：
 * - 主色调：#FF6B35（哈苏橙）
 * - 背景：#0A0A0A（纯黑）
 * - Before/After滑杆对比
 * - 置信度可视化
 * - 胶片推荐卡片
 * - 哈苏大师参数展示
 * - 大师拍摄建议
 */

/**
 * 场景类型数据（用于识别结果展示）
 */
data class SceneTypeData(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val confidence: Int,
    val params: HasselbladParams
)

/**
 * 场景分析结果
 */
data class SceneAnalysisResult(
    val primaryScene: SceneTypeData,
    val confidence: Float,
    val alternativeScenes: List<SceneTypeData> = emptyList(),
    val recommendedFilms: List<FilmPreset> = emptyList(),
    val hasselbladParams: HasselbladParams,
    val masterTips: List<String> = emptyList()
)

/**
 * 场景识别结果页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISceneRecognitionScreen(
    imageUrl: String? = null,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 状态
    var isAnalyzing by remember { mutableStateOf(true) }
    var isOptimized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0.5f) }
    var selectedFilmId by remember { mutableStateOf<String?>(null) }

    // 分析结果
    var analysisResult by remember {
        mutableStateOf<SceneAnalysisResult?>(null)
    }

    // 模拟分析过程
    LaunchedEffect(imageUrl) {
        kotlinx.coroutines.delay(1500)
        // 模拟分析结果
        analysisResult = SceneAnalysisResult(
            primaryScene = SceneTypeData(
                id = "landscape-sunset",
                name = "日落风景",
                category = "landscape",
                description = "黄金时刻的壮丽日落",
                confidence = 92,
                params = HasselbladParams(
                    saturation = 15,
                    contrast = 12,
                    colorTemp = 8,
                    clarity = 5,
                    sharpness = 3
                )
            ),
            confidence = 0.92f,
            alternativeScenes = listOf(
                SceneTypeData("landscape-golden", "黄金时刻", "landscape", "温暖光线", 78, HasselbladParams()),
                SceneTypeData("landscape-mountain", "山景", "landscape", "远山轮廓", 65, HasselbladParams()),
                SceneTypeData("nature-clouds", "云景", "nature", "天空云层", 52, HasselbladParams())
            ),
            recommendedFilms = listOf(
                FilmPreset("portra-400", "Portra 400", FilmPreset.FilmSeries.CLASSIC, 95f, "温暖柔和"),
                FilmPreset("cc-classic", "CC 经典负片", FilmPreset.FilmSeries.CLASSIC, 88f, "复古质感"),
                FilmPreset("nh-rich", "NH 浓郁负片", FilmPreset.FilmSeries.CLASSIC, 82f, "浓郁色彩")
            ),
            hasselbladParams = HasselbladParams(
                saturation = 15,
                contrast = 12,
                colorTemp = 8,
                clarity = 5,
                sharpness = 3,
                tone = 5,
                vignette = 8
            ),
            masterTips = listOf(
                "黄金时刻（日出后/日落前1小时）是拍摄风景的最佳时机",
                "使用小光圈（f/8-f/16）可获得更大的景深",
                "尝试将太阳置于画面边缘，创造戏剧性光影效果",
                "HNCS自然色彩解决方案可还原真实的日落色彩"
            )
        )
        selectedFilmId = analysisResult?.recommendedFilms?.firstOrNull()?.id
        isAnalyzing = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 顶部导航栏
        TopAppBar(
            title = { Text("AI 出片", fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White.copy(alpha = 0.8f))
                }
            },
            actions = {
                IconButton(onClick = {
                    // 导出：将分析结果生成配方卡片图片并保存到相册
                    val result = analysisResult
                    if (result != null) {
                        scope.launch {
                            try {
                                val bitmap = buildRecipeCardBitmap(result, context)
                                ShareExportUtils.exportImageToGallery(context, bitmap, "hasselblad_recipe_${System.currentTimeMillis()}.jpg")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.Download, "导出", tint = Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = {
                    // 分享配方：将分析结果作为文本分享
                    val result = analysisResult
                    if (result != null) {
                        val shareText = buildRecipeShareText(result)
                        ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setSubject("哈苏大师配方 - ${result.primaryScene.name}")
                            .setText(shareText)
                            .startChooser()
                    }
                }) {
                    Icon(Icons.Default.Share, "分享配方", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack.copy(alpha = 0.9f)
            )
        )

        if (isAnalyzing) {
            // 分析中状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = HasselbladOrange,
                            strokeWidth = 3.dp,
                            trackColor = HasselbladOrange.copy(alpha = 0.2f)
                        )
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = HasselbladOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "哈苏大师正在分析场景...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "识别颜色 · 分析光线 · 匹配胶片",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                }
            }
        } else if (analysisResult != null) {
            // 分析完成，显示结果
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Before/After 对比滑杆
                BeforeAfterSlider(
                    sliderPosition = sliderPosition,
                    onPositionChange = { sliderPosition = it }
                )

                // 哈苏大师识别结果
                RecognitionResultCard(
                    result = analysisResult!!
                )

                // 推荐胶片
                FilmRecommendationCard(
                    films = analysisResult!!.recommendedFilms,
                    selectedId = selectedFilmId,
                    onSelect = { selectedFilmId = it }
                )

                // 哈苏大师参数
                HasselbladParamsCard(
                    params = analysisResult!!.hasselbladParams
                )

                // 大师建议
                MasterTipsCard(
                    tips = analysisResult!!.masterTips
                )

                // 底部间距
                Spacer(modifier = Modifier.height(80.dp))
            }

            // 底部操作栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(PureBlack.copy(alpha = 0.95f))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 重拍按钮
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White.copy(alpha = 0.6f))
                        Text("重拍", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }

                    // 一键哈苏优化按钮
                    Button(
                        onClick = {
                            haptic.perform(HapticFeedbackType.Confirm)
                            isOptimized = true
                            scope.launch {
                                kotlinx.coroutines.delay(3000)
                                isOptimized = false
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isOptimized) Color(0xFF4CAF50) else HasselbladOrange
                        )
                    ) {
                        if (isOptimized) {
                            Icon(Icons.Default.Check, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("已优化", color = Color.White, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键哈苏优化", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }

                    // 保存配方按钮
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (saveSuccess) Icons.Default.Check else Icons.Default.Save,
                            null,
                            tint = if (saveSuccess) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            if (saveSuccess) "已保存" else "保存配方",
                            color = if (saveSuccess) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            // 分析失败
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("分析失败，请重试", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

/**
 * Before/After 对比滑杆
 */
@Composable
private fun BeforeAfterSlider(
    sliderPosition: Float,
    onPositionChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            // After 图片（处理后）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HasselbladOrange.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("处理后", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            // Before 图片（原图）
            Box(
                modifier = Modifier
                    .fillMaxWidth(sliderPosition)
                    .fillMaxHeight()
                    .background(Color(0xFF333333))
            ) {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Image,
                        null,
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text("原图", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                }
            }

            // 滑杆线
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.White)
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((sliderPosition * (size.width - 2.dp.toPx())).toInt(), 0) }
            )

            // 滑杆手柄
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((sliderPosition * (size.width - 32.dp.toPx())).toInt(), 0) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newPosition = sliderPosition + (dragAmount.x / size.width)
                            onPositionChange(newPosition.coerceIn(0f, 1f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = Color.Black, modifier = Modifier.size(12.dp))
                }
            }
        }

        // 标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Before", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
            Text("After", color = HasselbladOrange, fontSize = 11.sp)
        }
    }
}

/**
 * 识别结果卡片
 */
@Composable
private fun RecognitionResultCard(
    result: SceneAnalysisResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("哈苏大师识别", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 主场景
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(getSceneEmoji(result.primaryScene.id), fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    result.primaryScene.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "· 置信度 ${Math.round(result.confidence * 100)}%",
                    color = HasselbladOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 置信度条
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(result.confidence)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(HasselbladOrange, Color(0xFFFF8A50))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("HNCS 自然色彩已优化", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

            // 备选场景
            if (result.alternativeScenes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Text("备选场景：", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                result.alternativeScenes.take(3).forEach { scene ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            scene.name,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((scene.confidence / 100f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${scene.confidence}%",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * 胶片推荐卡片
 */
@Composable
private fun FilmRecommendationCard(
    films: List<FilmPreset>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Movie, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("推荐胶片", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                films.forEach { film ->
                    FilmChip(
                        film = film,
                        isSelected = selectedId == film.id,
                        onClick = { onSelect(film.id) }
                    )
                }
            }
        }
    }
}

/**
 * 胶片选择芯片
 */
@Composable
private fun FilmChip(
    film: FilmPreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
        ),
        border = BorderStroke(1.dp, if (isSelected) HasselbladOrange else Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, null, tint = HasselbladOrange, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                film.name,
                color = if (isSelected) HasselbladOrange else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${film.matchScore.toInt()}% 匹配",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 哈苏参数卡片
 */
@Composable
private fun HasselbladParamsCard(
    params: HasselbladParams
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("哈苏大师参数", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val paramList = listOf(
                "影调" to params.tone,
                "饱和度" to params.saturation,
                "对比度" to params.contrast,
                "色温" to params.colorTemp,
                "清晰度" to params.clarity,
                "锐度" to params.sharpness,
                "暗角" to params.vignette,
                "青品调" to params.cyanMagenta
            )

            paramList.forEach { (name, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(name, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    Text(
                        params.formatParamValue(value),
                        color = HasselbladOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 大师建议卡片
 */
@Composable
private fun MasterTipsCard(
    tips: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null, tint = HasselbladOrange, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("大师建议", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            tips.forEach { tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = HasselbladOrange, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tip,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 根据场景ID获取对应的emoji
 */
private fun getSceneEmoji(sceneId: String): String {
    return when {
        sceneId.contains("portrait") -> "👤"
        sceneId.contains("landscape") -> "🏔️"
        sceneId.contains("night") -> "🌃"
        sceneId.contains("food") -> "🍜"
        sceneId.contains("urban") -> "🏢"
        sceneId.contains("still") -> "🍃"
        sceneId.contains("macro") -> "🔍"
        sceneId.contains("event") -> "🎉"
        sceneId.contains("sunset") -> "🌅"
        sceneId.contains("golden") -> "☀️"
        else -> "📷"
    }
}

/**
 * 构建配方卡片图片（用于导出到相册）
 */
private fun buildRecipeCardBitmap(
    result: SceneAnalysisResult,
    context: android.content.Context
): Bitmap {
    val width = 1080
    val height = 1440
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 背景
    canvas.drawColor(AndroidColor.parseColor("#0A0A0A"))

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 64f
        isFakeBoldText = true
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#80FFFFFF")
        textSize = 36f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#FF6B35")
        textSize = 44f
        isFakeBoldText = true
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#FF6B35")
        textSize = 52f
        isFakeBoldText = true
    }

    var y = 140f
    canvas.drawText("哈苏大师配方", 80f, y, titlePaint)
    y += 100f
    canvas.drawText("场景：${result.primaryScene.name}", 80f, y, valuePaint)
    y += 80f
    canvas.drawText("置信度：${(result.confidence * 100).toInt()}%", 80f, y, labelPaint)
    y += 80f

    if (result.recommendedFilms.isNotEmpty()) {
        canvas.drawText("推荐胶片：", 80f, y, labelPaint)
        y += 60f
        result.recommendedFilms.take(3).forEach { film ->
            canvas.drawText("• ${film.name} (${film.matchScore.toInt()}%)", 100f, y, valuePaint)
            y += 60f
        }
        y += 40f
    }

    canvas.drawText("哈苏参数：", 80f, y, accentPaint)
    y += 70f
    val params = result.hasselbladParams
    listOf(
        "影调" to params.tone,
        "饱和度" to params.saturation,
        "对比度" to params.contrast,
        "色温" to params.colorTemp,
        "清晰度" to params.clarity,
        "锐度" to params.sharpness
    ).forEach { (name, value) ->
        canvas.drawText(name, 100f, y, labelPaint)
        canvas.drawText(value.toString(), 320f, y, valuePaint)
        y += 56f
    }

    y += 40f
    if (result.masterTips.isNotEmpty()) {
        canvas.drawText("大师建议：", 80f, y, accentPaint)
        y += 70f
        result.masterTips.take(3).forEach { tip ->
            val text = if (tip.length > 30) tip.substring(0, 30) + "..." else tip
            canvas.drawText("• $text", 100f, y, labelPaint)
            y += 56f
        }
    }

    canvas.drawText("用哈苏之眼，记录每一刻的光影。", 80f, (height - 80f), labelPaint)

    return bitmap
}

/**
 * 构建配方分享文本
 */
private fun buildRecipeShareText(result: SceneAnalysisResult): String {
    val builder = StringBuilder()
    builder.appendLine("哈苏大师配方 - ${result.primaryScene.name}")
    builder.appendLine()
    builder.appendLine("置信度：${(result.confidence * 100).toInt()}%")
    builder.appendLine()
    if (result.recommendedFilms.isNotEmpty()) {
        builder.appendLine("推荐胶片：")
        result.recommendedFilms.take(3).forEach { film ->
            builder.appendLine("• ${film.name} (${film.matchScore.toInt()}% 匹配)")
        }
        builder.appendLine()
    }
    val params = result.hasselbladParams
    builder.appendLine("哈苏参数：")
    listOf(
        "影调" to params.tone,
        "饱和度" to params.saturation,
        "对比度" to params.contrast,
        "色温" to params.colorTemp,
        "清晰度" to params.clarity,
        "锐度" to params.sharpness
    ).forEach { (name, value) ->
        builder.appendLine("• $name: $value")
    }
    builder.appendLine()
    if (result.masterTips.isNotEmpty()) {
        builder.appendLine("大师建议：")
        result.masterTips.take(3).forEach { tip ->
            builder.appendLine("• $tip")
        }
    }
    builder.appendLine()
    builder.appendLine("—— 用哈苏之眼，记录每一刻的光影 ——")
    return builder.toString()
}