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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.PresetI18n

/**
 * 识别结果确认页（PRD 10.1 / 8.3）：
 * 展示草稿 → 查看 EXIF + 风格 + Top-3 候选 → 可微调名称 / 切换候选风格 →
 * 确认（落库）或取消（丢弃）。遵循 Ask first：识别结果不静默写入。
 *
 * 展示层文案全部经字符串资源本地化；引擎仅输出结构化数据（见 RecognitionResult）。
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
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recog_result_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.recog_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(),
                // 【v1.5.6 P2 修复】edge-to-edge 下标题/返回键避让状态栏
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
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
                    contentDescription = stringResource(R.string.recog_ref_photo),
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
                label = { Text(stringResource(R.string.recog_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // EXIF 摄影参数卡
            ExifParamsCard(result, accent)

            // 风格向量可视化（优先展示照片真实分析向量，缺失时回退候选风格目标向量）
            StyleVectorCard(
                vector = result.styleVector
                    ?: result.candidates.firstOrNull()?.preset?.target
                    ?: StyleVector.NEUTRAL,
                accent = accent
            )

            // Top-3 候选（可切换，确认时按所选候选重建调色参数）
            CandidatesCard(result, selectedCandidate, accent) { selectedCandidate = it }

            // 可解释依据（本地化组装）+ 警示
            Text(
                text = buildLocalizedExplanation(result),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            result.warnings.forEach { w ->
                Text(
                    text = "⚠ " + PresetI18n.getLocalizedWarning(context, w),
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
                Text(stringResource(R.string.recog_confirm), fontWeight = FontWeight.Bold)
            }
            Text(
                text = stringResource(R.string.recog_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** 本地化组装"可解释依据"（引擎仅输出结构化特征与数据） */
@Composable
private fun buildLocalizedExplanation(result: RecognitionResult): String {
    val context = LocalContext.current
    val parts = mutableListOf<String>()
    if (result.features.isNotEmpty()) {
        val tags = result.features.joinToString(" + ") { PresetI18n.getLocalizedFeatureTag(context, it) }
        parts.add(stringResource(R.string.recog_explanation_features, tags))
    } else {
        parts.add(stringResource(R.string.recog_tag_none))
    }
    parts.add(
        if (result.source == RecognitionSource.PHOTO_EXIF && result.sourceLabel != null) {
            stringResource(R.string.recog_explanation_source_exif, result.sourceLabel)
        } else {
            stringResource(R.string.recog_explanation_source_style)
        }
    )
    parts.add(stringResource(R.string.recog_explanation_confidence, (result.confidence * 100).toInt()))
    return parts.joinToString("\n")
}

@Composable
private fun ConfidenceBanner(result: RecognitionResult, accent: Color) {
    val color = if (result.isStrongMatch) Color(0xFF4CAF50) else Color(0xFFFFB300)
    val levelText = stringResource(
        when (result.confidenceLevel) {
            0 -> R.string.recog_confidence_strong
            1 -> R.string.recog_confidence_medium
            else -> R.string.recog_confidence_low
        }
    )
    val sourceText = stringResource(
        if (result.source == RecognitionSource.PHOTO_EXIF) R.string.recog_source_exif
        else R.string.recog_source_style
    )
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
                Text(levelText, color = color, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.recog_source_line, sourceText, result.engineVersion),
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
        d.iso?.let { add(stringResource(R.string.recog_label_iso) to it) }
        d.shutterSpeed?.let { add(stringResource(R.string.recog_label_shutter) to it) }
        // 光圈：EXIF 识别依据（MasterPreset 无光圈字段，不落入预设参数）
        result.aperture?.let { add(stringResource(R.string.recog_label_aperture) to it) }
        result.focalLength?.let { add(stringResource(R.string.recog_label_focal) to it) }
        d.colorTemperature?.let { add(stringResource(R.string.recog_label_temp) to "${it}K") }
            ?: d.whiteBalance?.let { add(stringResource(R.string.recog_label_wb) to it) }
        d.exposureCompensation?.let { add(stringResource(R.string.recog_label_ev) to it) }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.recog_exif_card), fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(10.dp))
            if (params.isEmpty()) {
                Text(
                    stringResource(R.string.recog_no_exif_params),
                    style = MaterialTheme.typography.bodySmall
                )
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
        stringResource(R.string.recog_vec_brightness) to vector.brightness,
        stringResource(R.string.recog_vec_contrast) to vector.contrast,
        stringResource(R.string.recog_vec_saturation) to vector.saturation,
        stringResource(R.string.recog_vec_warmth) to vector.warmth,
        stringResource(R.string.recog_vec_dr) to vector.dynamicRange,
        stringResource(R.string.recog_vec_colorfulness) to vector.colorfulness
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.recog_style_card), fontWeight = FontWeight.Bold, color = accent)
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
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.recog_candidates_card), fontWeight = FontWeight.Bold, color = accent)
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
                            Text(
                                PresetI18n.getLocalizedStyleKey(context, m.preset.key),
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) accent else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${(m.confidence * 100).toInt()}%",
                                color = if (isSel) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        // 结构化依据标签（本地化）
                        if (m.reasonTags.isNotEmpty()) {
                            Text(
                                m.reasonTags.joinToString(" + ") { PresetI18n.getLocalizedFeatureTag(context, it) },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
