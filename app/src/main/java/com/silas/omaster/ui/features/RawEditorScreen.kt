package com.silas.omaster.ui.features

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.raw.DngDecoder
import com.silas.omaster.raw.RawEditor
import com.silas.omaster.raw.RawParameters
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * RAW 编辑器屏幕
 * 2026 标准：完整 DNG/RAW 编辑能力
 * - 加载 DNG/RAW 文件
 * - 工作色彩空间（ProPhoto Linear）中编辑
 * - 全参数：白平衡 2000K-50000K、曝光 ±5EV、HSL 8 色、镜头校正
 * - 导出为高质量 JPEG / 16-bit TIFF
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawEditorScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editor = remember { RawEditor.getInstance(context) }

    val currentRaw by editor.currentRaw.collectAsState()
    val parameters by editor.parameters.collectAsState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                editor.loadRaw(it)
            }
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("RAW 编辑器", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = {
                    pickerLauncher.launch(arrayOf("image/x-adobe-dng", "image/x-canon-cr2", "image/x-sony-arw", "image/x-nikon-nef", "image/*"))
                }) {
                    Icon(Icons.Default.FolderOpen, "打开", tint = Color.White)
                }
                if (currentRaw != null) {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Save, "导出", tint = HasselbladOrange)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        if (currentRaw == null) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = HasselbladOrange.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("还没有加载 RAW 文件", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "支持 DNG/CR2/NEF/ARW 格式",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            pickerLauncher.launch(arrayOf("image/x-adobe-dng", "image/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开 RAW 文件")
                    }
                }
            }
        } else {
            // 编辑器界面
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 元数据
                MetadataPanel(metadata = currentRaw!!.metadata)
                Spacer(modifier = Modifier.height(16.dp))

                // 参数调节
                WhiteBalanceSection(
                    params = parameters,
                    onUpdate = { editor.setParameters(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposureSection(
                    params = parameters,
                    onUpdate = { editor.setParameters(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                ToneSection(
                    params = parameters,
                    onUpdate = { editor.setParameters(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                LensCorrectionSection(
                    params = parameters,
                    onUpdate = { editor.setParameters(it) }
                )
            }
        }
    }

    if (showExportDialog && currentRaw != null) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                scope.launch {
                    val file = java.io.File(
                        context.getExternalFilesDir("Pictures") ?: context.filesDir,
                        "raw_export_${System.currentTimeMillis()}.${format.extension}"
                    )
                    val success = when (format) {
                        com.silas.omaster.raw.ExportFormat.JPEG -> editor.exportJpeg(file, 95)
                        com.silas.omaster.raw.ExportFormat.TIFF -> editor.exportTiff16bit(file)
                        else -> editor.exportJpeg(file, 95)
                    }
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            "已导出到: ${file.absolutePath}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    showExportDialog = false
                }
            }
        )
    }
}

@Composable
private fun MetadataPanel(metadata: DngDecoder.RawMetadata) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("元数据", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            MetadataRow("尺寸", "${metadata.width} × ${metadata.height}")
            MetadataRow("ISO", "${metadata.isoSpeed}")
            MetadataRow("快门", "1/${(1f / metadata.exposureTime).roundToInt()}s")
            MetadataRow("光圈", "f/${metadata.fNumber}")
            MetadataRow("焦距", "${metadata.focalLength}mm")
            MetadataRow("色温", "${metadata.colorTempKelvin}K")
            MetadataRow("位数", "${metadata.bitsPerSample} bit")
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun WhiteBalanceSection(
    params: RawParameters,
    onUpdate: (RawParameters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("白平衡", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("色温: ${params.colorTempKelvin}K", color = Color.White, fontSize = 12.sp)
            Slider(
                value = params.colorTempKelvin.toFloat(),
                onValueChange = { onUpdate(params.copy(colorTempKelvin = it.toInt())) },
                valueRange = 2000f..50000f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("色调: ${params.tint}", color = Color.White, fontSize = 12.sp)
            Slider(
                value = params.tint.toFloat(),
                onValueChange = { onUpdate(params.copy(tint = it.toInt())) },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )
        }
    }
}

@Composable
private fun ExposureSection(
    params: RawParameters,
    onUpdate: (RawParameters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("曝光", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("EV: ${params.exposureEV}", color = Color.White, fontSize = 12.sp)
            Slider(
                value = params.exposureEV,
                onValueChange = { onUpdate(params.copy(exposureEV = it)) },
                valueRange = -5f..5f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("对比度: ${params.contrast.toInt()}", color = Color.White, fontSize = 12.sp)
            Slider(
                value = params.contrast,
                onValueChange = { onUpdate(params.copy(contrast = it)) },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("饱和度: ${params.saturation.toInt()}", color = Color.White, fontSize = 12.sp)
            Slider(
                value = params.saturation,
                onValueChange = { onUpdate(params.copy(saturation = it)) },
                valueRange = -100f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange
                )
            )
        }
    }
}

@Composable
private fun ToneSection(
    params: RawParameters,
    onUpdate: (RawParameters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("色调", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            listOf(
                "高光" to params.highlights,
                "阴影" to params.shadows,
                "白色色阶" to params.whites,
                "黑色色阶" to params.blacks,
                "清晰度" to params.clarity,
                "去霾" to params.dehaze
            ).forEach { (label, value) ->
                Text("$label: ${value.toInt()}", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = value,
                    onValueChange = { newValue ->
                        val updated = when (label) {
                            "高光" -> params.copy(highlights = newValue)
                            "阴影" -> params.copy(shadows = newValue)
                            "白色色阶" -> params.copy(whites = newValue)
                            "黑色色阶" -> params.copy(blacks = newValue)
                            "清晰度" -> params.copy(clarity = newValue)
                            "去霾" -> params.copy(dehaze = newValue)
                            else -> params
                        }
                        onUpdate(updated)
                    },
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = HasselbladOrange,
                        activeTrackColor = HasselbladOrange
                    )
                )
            }
        }
    }
}

@Composable
private fun LensCorrectionSection(
    params: RawParameters,
    onUpdate: (RawParameters) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("镜头校正", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = params.lensCorrectionEnabled,
                    onCheckedChange = { onUpdate(params.copy(lensCorrectionEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = HasselbladOrange,
                        checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f)
                    )
                )
            }
            if (params.lensCorrectionEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "畸变校正" to params.distortionCorrection,
                    "色差去除" to params.chromaticAberrationRemoval,
                    "暗角校正" to params.vignettingCorrection,
                    "紫边去除" to params.purpleFringingRemoval
                ).forEach { (label, value) ->
                    Text("$label: ${value.toInt()}", color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = value,
                        onValueChange = { newValue ->
                            val updated = when (label) {
                                "畸变校正" -> params.copy(distortionCorrection = newValue)
                                "色差去除" -> params.copy(chromaticAberrationRemoval = newValue)
                                "暗角校正" -> params.copy(vignettingCorrection = newValue)
                                "紫边去除" -> params.copy(purpleFringingRemoval = newValue)
                                else -> params
                            }
                            onUpdate(updated)
                        },
                        valueRange = -100f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = HasselbladOrange,
                            activeTrackColor = HasselbladOrange
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (com.silas.omaster.raw.ExportFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出 RAW") },
        text = {
            Column {
                com.silas.omaster.raw.ExportFormat.entries.forEach { format ->
                    TextButton(
                        onClick = { onExport(format) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(format.displayName, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = Color(0xFF1F1F1F),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
