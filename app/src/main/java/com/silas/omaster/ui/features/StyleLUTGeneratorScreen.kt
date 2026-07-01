package com.silas.omaster.ui.features

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.silas.omaster.data.lut.StyleLUTGenerator
import com.silas.omaster.ui.theme.*

/**
 * 风格 LUT 生成器页面
 *
 * 完整操作链路（对标 cubelut.cn/style_lab.php）：
 * 1. 上传原图 — 拍照/相册选择，自动检测色彩空间
 * 2. 上传参考风格图 — 拍照/相册选择
 * 3. 设置参数 — 色彩空间/迁移强度
 * 4. 生成 LUT — 色彩迁移算法生成 33x33x33 3D LUT
 * 5. 预览效果 — Before/After 对比
 * 6. 评估指标 — 场景匹配/色彩相似/亮度相似/导出精度
 * 7. 导出 .cube — 保存到 Download 目录
 * 8. 应用 LUT — 跳转哈苏之眼/AI调色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleLUTGeneratorScreen(
    onBack: () -> Unit,
    onApplyLUT: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: StyleLUTGeneratorViewModel = viewModel()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val sourceBitmap by viewModel.sourceBitmap.collectAsState()
    val referenceBitmap by viewModel.referenceBitmap.collectAsState()
    val colorSpace by viewModel.colorSpace.collectAsState()
    val detectedColorSpace by viewModel.detectedColorSpace.collectAsState()
    val strength by viewModel.strength.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val generationResult by viewModel.generationResult.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val error by viewModel.error.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportSuccess by viewModel.exportSuccess.collectAsState()

    // 图片选择器
    val sourcePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.loadSourceImage(context, it) } }

    val referencePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { viewModel.loadReferenceImage(context, it) } }

    // 导出成功提示
    LaunchedEffect(exportSuccess) {
        if (exportSuccess) {
            Toast.makeText(context, ".cube 文件已保存到 Download/OMaster/LUTs", Toast.LENGTH_LONG).show()
        }
    }

    // 错误提示
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("风格 LUT 生成器", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ========== 说明 ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HasselbladOrange.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, null, tint = HasselbladOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "上传原图和参考风格图，AI 自动生成可导入剪辑软件的 .cube LUT 文件",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // ========== 上传素材 ==========
            Text("上传素材", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("原图色彩空间：${colorSpaceLabel(if (colorSpace == StyleLUTGenerator.ColorSpace.AUTO) detectedColorSpace else colorSpace)}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            // 原图选择
            ImageUploadCard(
                label = "点击选择原图",
                subtitle = "请选择 Rec.709 或 Log",
                bitmap = sourceBitmap,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sourcePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = { viewModel.reset() }
            )

            // 参考风格图选择
            ImageUploadCard(
                label = "点击选择参考风格",
                subtitle = "用于匹配整体色彩观感",
                bitmap = referenceBitmap,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    referencePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onClear = { viewModel.clearReferenceImage() }
            )

            // ========== 参数设置 ==========
            Text("参数设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // 色彩空间选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyleLUTGenerator.ColorSpace.entries.forEach { cs ->
                    FilterChip(
                        selected = colorSpace == cs,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setColorSpace(cs)
                        },
                        label = { Text(colorSpaceLabel(cs), fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 迁移强度
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("迁移强度", fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text("${(strength * 100).toInt()}%", fontSize = 13.sp, color = HasselbladOrange, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = strength,
                onValueChange = { viewModel.setStrength(it) },
                colors = SliderDefaults.colors(thumbColor = HasselbladOrange, activeTrackColor = HasselbladOrange)
            )

            // ========== 生成按钮 ==========
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.generate()
                },
                enabled = sourceBitmap != null && referenceBitmap != null && !isGenerating,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成中 ${(progress * 100).toInt()}%")
                } else {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("生成风格 LUT")
                }
            }

            // 生成进度条
            if (isGenerating) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = HasselbladOrange,
                    trackColor = HasselbladOrange.copy(alpha = 0.2f)
                )
            }

            // ========== 预览与导出 ==========
            if (generationResult != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("预览与导出", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "将克隆的 LUT 套用在原图上的真实预览，确认效果后再下载 .cube 文件",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                // Before/After 对比预览
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 原图
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            sourceBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "原图",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("原图", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        // LUT 效果
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            previewBitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "LUT效果",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                                color = HasselbladOrange.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("LUT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                // 评估指标
                val currentGenerationResult = generationResult ?: return@Column
                val metrics = currentGenerationResult.metrics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricItem("场景匹配", metrics.sceneMatch)
                        MetricItem("色彩相似", metrics.colorSimilarity)
                        MetricItem("亮度相似", metrics.brightnessSimilarity)
                        MetricItem("导出精度", metrics.exportPrecision)
                    }
                }

                // 重新生成
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.regenerate()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重新生成", fontSize = 12.sp)
                    }

                    // 导出 .cube
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.exportCubeFile(context)
                        },
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("导出 .cube", fontSize = 12.sp)
                    }
                }

                // 应用 LUT
                if (onApplyLUT != null) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.applyToHasselblad(context)
                            onApplyLUT()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("应用此 LUT 到调色", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 图片上传卡片
 */
@Composable
private fun ImageUploadCard(
    label: String,
    subtitle: String,
    bitmap: android.graphics.Bitmap?,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (bitmap == null) Modifier.border(1.dp, HasselbladOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (bitmap == null) HasselbladOrange.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize()
                )
                // 清除按钮
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                // 标签
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(label, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = HasselbladOrange, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(label, color = HasselbladOrange, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * 评估指标项
 */
@Composable
private fun MetricItem(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(2.dp))
        val color = when {
            value >= 0.8f -> SuccessGreen
            value >= 0.6f -> HasselbladOrange
            else -> ErrorRed
        }
        Text("${(value * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

private fun colorSpaceLabel(cs: StyleLUTGenerator.ColorSpace): String = when (cs) {
    StyleLUTGenerator.ColorSpace.AUTO -> "自动检测"
    StyleLUTGenerator.ColorSpace.REC709 -> "Rec.709"
    StyleLUTGenerator.ColorSpace.LOG -> "Log"
}
