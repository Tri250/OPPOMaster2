package com.silas.omaster.recognition

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.model.MasterPreset

/**
 * 照片识别确认页（PRD 16.2 原型 / FR-01~05）。
 *
 * 流程：展示识别结果 → 用户查看 EXIF + 风格 + Top-3 候选 → 可微调名称/参数 →
 * 确认（落库）或取消（丢弃）。遵循 Ask first：识别结果不静默写入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoRecognitionScreen(
    bitmap: Bitmap?,
    result: RecognitionResult,
    onBack: () -> Unit,
    onConfirm: (MasterPreset) -> Unit
) {
    var presetName by remember(result) { mutableStateOf(result.draft.name) }
    var selectedCandidate by remember(result) { mutableIntStateOf(0) }
    val accent = Color(0xFFEC7223)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("照片识别结果", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 参考图缩略图
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "参考照片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray),
                    contentScale = ContentScale.Crop
                )
            }

            // 置信度提示条
            ConfidenceBanner(result, accent)

            // 预设名称编辑
            OutlinedTextField(
                value = presetName,
                onValueChange = { presetName = it },
                label = { Text("预设名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // EXIF 摄影参数卡
            ExifParamsCard(result, accent)

            // 风格向量可视化
            StyleVectorCard(
                vector = result.candidates.firstOrNull()?.preset?.target
                    ?: com.silas.omaster.recognition.StyleVector.NEUTRAL,
                accent = accent
            )

            // Top-3 候选
            CandidatesCard(result, selectedCandidate, accent) { selectedCandidate = it }

            // 可解释依据 + 警示
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            result.warnings.forEach { w ->
                Text(
                    text = "⚠ $w",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFB300)
                )
            }

            // 确认按钮
            Button(
                onClick = {
                    val chosen = result.candidates.getOrNull(selectedCandidate)?.preset
                    val draft = result.draft.copy(
                        name = presetName.ifBlank { result.draft.name },
                        filter = chosen?.filter ?: result.draft.filter,
                        tone = chosen?.tone ?: result.draft.tone,
                        saturation = chosen?.saturation ?: result.draft.saturation,
                        warmCool = chosen?.warmCool ?: result.draft.warmCool,
                        cyanMagenta = chosen?.cyanMagenta ?: result.draft.cyanMagenta,
                        sharpness = chosen?.sharpness ?: result.draft.sharpness,
                        softLight = chosen?.softLight ?: result.draft.softLight,
                        vignette = chosen?.vignette ?: result.draft.vignette
                    )
                    onConfirm(draft)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("确认并保存到自定义预设库", fontWeight = FontWeight.Bold)
            }
            Text(
                text = "该预设为风格模拟，非哈苏原厂算法。所有处理均在本地完成，不上传照片。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ConfidenceBanner(result: RecognitionResult, accent: Color) {
    val color = if (result.isStrongMatch) Color(0xFF4CAF50) else Color(0xFFFFB300)
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${(result.confidence * 100).toInt()}%",
                color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(result.confidenceLabel, color = color, fontWeight = FontWeight.Bold)
                Text(
                    "来源：${if (result.source == RecognitionSource.PHOTO_EXIF) "照片 EXIF + 风格" else "像素风格"} · 引擎 v${result.engineVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ExifParamsCard(result: RecognitionResult, accent: Color) {
    val d = result.draft
    val params = buildList {
        d.iso?.let { add("ISO" to it) }
        d.shutterSpeed?.let { add("快门" to it) }
        d.apertureValue()?.let { add("光圈" to it) }
        d.colorTemperature?.let { add("色温" to "${it}K") } ?: d.whiteBalance?.let { add("白平衡" to it) }
        d.exposureCompensation?.let { add("曝光补偿" to it) }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("摄影参数", fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(10.dp))
            if (params.isEmpty()) {
                Text("照片无 EXIF 摄影参数，已生成纯风格预设，参数项可稍后手动补充。",
                    style = MaterialTheme.typography.bodySmall)
            } else {
                params.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { (k, v) ->
                            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                                Text(k, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                Text(v, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StyleVectorCard(vector: StyleVector, accent: Color) {
    val bars = listOf(
        "亮度" to vector.brightness, "对比" to vector.contrast, "饱和" to vector.saturation,
        "冷暖" to vector.warmth, "动态" to vector.dynamicRange, "色彩" to vector.colorfulness
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("画面风格特征", fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(10.dp))
            bars.forEach { (label, value) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp))
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(value * 100).toInt()}", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun CandidatesCard(
    result: RecognitionResult,
    selected: Int,
    accent: Color,
    onSelect: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("匹配风格（Top 3）", fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(10.dp))
            result.candidates.forEachIndexed { idx, m ->
                val isSel = idx == selected
                Card(
                    onClick = { onSelect(idx) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSel) accent.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.preset.displayName, fontWeight = FontWeight.Bold,
                                color = if (isSel) accent else MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.weight(1f))
                            Text("${(m.confidence * 100).toInt()}%", color = if (isSel) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Text(m.reason, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

/** 从 draft 的 whiteBalance 提取光圈展示（MasterPreset 无独立 aperture 字段，用 tag 中无；此处留空占位） */
private fun MasterPreset.apertureValue(): String? = null
