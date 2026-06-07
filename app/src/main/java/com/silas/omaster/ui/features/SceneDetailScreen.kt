package com.silas.omaster.ui.features

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.scene.SceneDetailManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 场景细分页面
 * 同步 Web 设计：18 种细分场景识别
 * 智能识别并提供优化建议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneDetailScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember { SceneDetailManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val detectedScene by manager.detectedScene.collectAsState()
    var selectedCategory by remember { mutableStateOf<SceneDetailManager.SceneCategory?>(null) }
    var selectedScene by remember { mutableStateOf<SceneDetailManager.DetailedScene?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isAnalyzing = true
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    bitmap?.let { bmp ->
                        val result = manager.detectScene(bmp)
                        selectedScene = result.primaryScene
                    }
                } catch (e: Exception) {
                    // 错误处理
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    val categories = remember {
        listOf(
            null to stringResource(R.string.scene_all),
            SceneDetailManager.SceneCategory.LIGHT to "光线",
            SceneDetailManager.SceneCategory.WEATHER to "天气",
            SceneDetailManager.SceneCategory.TIME to "时间"
        )
    }

    val filteredScenes = if (selectedCategory == null) {
        SceneDetailManager.DetailedScene.entries
    } else {
        SceneDetailManager.DetailedScene.entries.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.scene_title),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.scene_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.ToggleOff)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 上传图片分析
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.perform(HapticFeedbackType.Confirm)
                            imagePicker.launch("image/*")
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = HasselbladOrange
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI 智能识别",
                                color = HasselbladOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isAnalyzing) "分析中..." else "上传图片自动识别场景",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 识别结果
            detectedScene?.let { result ->
                item {
                    DetectedSceneCard(result.primaryScene, result.confidence)
                }
                item {
                    Text(
                        text = stringResource(R.string.scene_recommendation),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    SceneOptimizeCard(result.primaryScene)
                }
            }

            // 场景分类筛选
            item {
                Text(
                    text = "全部场景",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { (category, label) ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                selectedCategory = category
                            },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HasselbladOrange.copy(alpha = 0.2f),
                                selectedLabelColor = HasselbladOrange
                            )
                        )
                    }
                }
            }

            // 场景列表
            items(filteredScenes) { scene ->
                SceneCard(
                    scene = scene,
                    isSelected = selectedScene == scene,
                    onClick = {
                        haptic.perform(HapticFeedbackType.ToggleOn)
                        selectedScene = scene
                    }
                )
            }

            // 选中场景的拍摄建议
            selectedScene?.let { scene ->
                item {
                    Text(
                        text = stringResource(R.string.scene_tips),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                item {
                    TipsCard(scene)
                }
            }
        }
    }
}

@Composable
private fun DetectedSceneCard(
    scene: SceneDetailManager.DetailedScene,
    confidence: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = scene.icon,
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "检测到: ${scene.displayName}",
                    color = HasselbladOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "置信度: ${(confidence * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = scene.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SceneOptimizeCard(scene: SceneDetailManager.DetailedScene) {
    val context = LocalContext.current
    val manager = remember { SceneDetailManager.getInstance(context) }
    val params = remember(scene) {
        manager.getOptimizeParams(scene)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "优化参数",
                color = HasselbladOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParamRow("曝光", String.format("%+.1f", params.exposure))
            ParamRow("对比度", "${params.contrast}")
            ParamRow("饱和度", "${params.saturation}")
            ParamRow("暖度", "${params.warmth}")
            ParamRow("高光", "${params.highlights}")
            ParamRow("阴影", "${params.shadows}")
            ParamRow("清晰度", "${params.clarity}")
            ParamRow("鲜艳度", "${params.vibrance}")
        }
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = HasselbladOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SceneCard(
    scene: SceneDetailManager.DetailedScene,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else Color(0xFF1A1A1A)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, HasselbladOrange) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = scene.icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scene.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = scene.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Text(
                    text = scene.category.displayName,
                    color = HasselbladOrange.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun TipsCard(scene: SceneDetailManager.DetailedScene) {
    val tips = remember(scene) {
        when (scene) {
            SceneDetailManager.DetailedScene.BACKLIGHT -> listOf(
                "使用 HDR 模式保留高光和阴影细节",
                "适当增加曝光补偿 +0.3~+0.7",
                "对焦在主体上，避免背景过亮"
            )
            SceneDetailManager.DetailedScene.GOLDEN_HOUR -> listOf(
                "日落前 30 分钟是最佳拍摄时间",
                "使用长焦镜头突出光晕效果",
                "适当使用反光板为人物补光"
            )
            SceneDetailManager.DetailedScene.NIGHT -> listOf(
                "使用三脚架保证稳定",
                "ISO 控制在 800-1600",
                "寻找光源（霓虹灯、月光）作为兴趣点"
            )
            SceneDetailManager.DetailedScene.SNOWY -> listOf(
                "增加曝光补偿 +1~+1.5 EV 避免画面偏灰",
                "使用偏暖白平衡营造温馨感",
                "对焦在雪花或主体上"
            )
            else -> listOf(
                "保持稳定，必要时使用三脚架",
                "选择合适的光线时段",
                "注意构图与背景的关系"
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "拍摄建议",
                    color = HasselbladOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            tips.forEach { tip ->
                Text(
                    text = "• $tip",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
