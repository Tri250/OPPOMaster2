package com.silas.omaster.ui.features

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.silas.omaster.data.model.MasterLUT
import com.silas.omaster.lut.LUTExportParams
import com.silas.omaster.lut.LUTIntensityManager
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.vignette.VignetteController
import com.silas.omaster.vignette.VignetteController.DistortionType
import com.silas.omaster.vignette.VignetteController.VignetteShape
import kotlinx.coroutines.launch
import java.io.File

/**
 * LUT 精细调节 + 暗角与畸变控制页面
 *
 * P2 13. 3D LUT 精细调节
 * - LUT 强度滑块 (0% - 100%)
 * - LUT 混合叠加（多 LUT 权重）
 * - LUT 局部应用（结合蒙版）
 * - LUT 导出为 .cube 格式
 *
 * P2 14. 暗角与畸变控制
 * - 暗角强度 (0-100)
 * - 暗角形状选择（圆形/椭圆/方形）
 * - 暗角中心点拖拽定位
 * - 畸变校正（桶形/枕形）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LUTAndVignetteScreen(
    onBack: () -> Unit,
    sourceBitmap: Bitmap? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lutManager = remember { LUTIntensityManager.getInstance(context) }
    val vignetteController = remember { VignetteController.getInstance() }

    // LUT 参数
    var selectedLUTs by remember { mutableStateOf(listOf<MasterLUT>()) }
    var lutIntensity by remember { mutableFloatStateOf(1.0f) }
    var lutWeights by remember { mutableStateOf(mapOf<String, Float>()) }

    // 暗角参数
    var vignetteIntensity by remember { mutableIntStateOf(30) }
    var vignetteShape by remember { mutableStateOf(VignetteShape.CIRCLE) }
    var vignetteCenterX by remember { mutableFloatStateOf(0.5f) }
    var vignetteCenterY by remember { mutableFloatStateOf(0.5f) }
    var vignetteRadius by remember { mutableFloatStateOf(0.7f) }
    var vignetteFeather by remember { mutableFloatStateOf(0.5f) }

    // 畸变参数
    var distortionType by remember { mutableStateOf(DistortionType.NONE) }
    var distortionStrength by remember { mutableIntStateOf(0) }

    // 预览
    var previewBitmap by remember { mutableStateOf(sourceBitmap) }
    var showExportDialog by remember { mutableStateOf(false) }

    // 示例 LUT 列表
    val availableLUTs = remember {
        listOf(
            MasterLUT(id = "1", name = "哈苏经典", nameEn = "Hasselblad Classic", description = "哈苏经典色彩", category = com.silas.omaster.data.model.LUTCategory.HASSELBLAD, tags = listOf("哈苏"), suitableFor = listOf("人像"), format = com.silas.omaster.data.model.LUTFormat.CUBE, size = com.silas.omaster.data.model.LUTSize.SIZE_33, fileSize = 1024, coverImage = "", downloadUrl = "", author = "OMaster", createdAt = "2026-01-01"),
            MasterLUT(id = "2", name = "胶片模拟", nameEn = "Film Emulation", description = "胶片模拟色彩", category = com.silas.omaster.data.model.LUTCategory.FILM, tags = listOf("胶片"), suitableFor = listOf("风景"), format = com.silas.omaster.data.model.LUTFormat.CUBE, size = com.silas.omaster.data.model.LUTSize.SIZE_33, fileSize = 1024, coverImage = "", downloadUrl = "", author = "OMaster", createdAt = "2026-01-01"),
            MasterLUT(id = "3", name = "复古暖调", nameEn = "Vintage Warm", description = "复古暖色调", category = com.silas.omaster.data.model.LUTCategory.VINTAGE, tags = listOf("复古"), suitableFor = listOf("人像"), format = com.silas.omaster.data.model.LUTFormat.CUBE, size = com.silas.omaster.data.model.LUTSize.SIZE_33, fileSize = 1024, coverImage = "", downloadUrl = "", author = "OMaster", createdAt = "2026-01-01"),
            MasterLUT(id = "4", name = "冷色调", nameEn = "Cold Tone", description = "冷色调风格", category = com.silas.omaster.data.model.LUTCategory.CINEMATIC, tags = listOf("电影"), suitableFor = listOf("风景"), format = com.silas.omaster.data.model.LUTFormat.CUBE, size = com.silas.omaster.data.model.LUTSize.SIZE_33, fileSize = 1024, coverImage = "", downloadUrl = "", author = "OMaster", createdAt = "2026-01-01")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("LUT 与暗角", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Default.Save, "导出 LUT", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === LUT 精细调节 ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "LUT 精细调节",
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // LUT 选择
                        Text("选择 LUT", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableLUTs.forEach { lut ->
                                val isSelected = lut.id in selectedLUTs.map { it.id }
                                LUTChip(
                                    label = lut.name,
                                    selected = isSelected,
                                    onClick = {
                                        selectedLUTs = if (isSelected) {
                                            selectedLUTs.filter { it.id != lut.id }
                                        } else {
                                            selectedLUTs + lut
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LUT 强度
                        Text("LUT 强度: ${(lutIntensity * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = lutIntensity,
                            onValueChange = { lutIntensity = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )

                        // 多 LUT 权重（当选择多个 LUT 时）
                        if (selectedLUTs.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("LUT 混合权重", color = Color.White, fontSize = 13.sp)
                            selectedLUTs.forEach { lut ->
                                val weight = lutWeights[lut.id] ?: 1f
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        lut.name,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(80.dp)
                                    )
                                    Slider(
                                        value = weight,
                                        onValueChange = { lutWeights = lutWeights + (lut.id to it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = HasselbladOrange,
                                            activeTrackColor = HasselbladOrange
                                        )
                                    )
                                    Text(
                                        "${(weight * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // === 暗角控制 ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "暗角控制",
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 暗角强度
                        Text("暗角强度: $vignetteIntensity%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = vignetteIntensity.toFloat(),
                            onValueChange = { vignetteIntensity = it.toInt() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 暗角形状
                        Text("暗角形状", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VignetteShape.entries.forEach { shape ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (vignetteShape == shape) HasselbladOrange
                                            else Color(0xFF2A2A2A)
                                        )
                                        .clickable { vignetteShape = shape }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        shape.displayName,
                                        color = if (vignetteShape == shape) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 暗角中心点（可拖拽）
                        Text("暗角中心点（拖拽调整）", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        VignetteCenterPointSelector(
                            centerX = vignetteCenterX,
                            centerY = vignetteCenterY,
                            onCenterChange = { x, y ->
                                vignetteCenterX = x
                                vignetteCenterY = y
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 暗角半径
                        Text("暗角半径: ${(vignetteRadius * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = vignetteRadius,
                            onValueChange = { vignetteRadius = it },
                            valueRange = 0.3f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )

                        // 暗角羽化
                        Text("羽化程度: ${(vignetteFeather * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                        Slider(
                            value = vignetteFeather,
                            onValueChange = { vignetteFeather = it },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = HasselbladOrange,
                                activeTrackColor = HasselbladOrange
                            )
                        )
                    }
                }
            }

            // === 畸变校正 ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "畸变校正",
                            color = HasselbladOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 畸变类型
                        Text("畸变类型", color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DistortionType.entries.forEach { type ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (distortionType == type) HasselbladOrange
                                            else Color(0xFF2A2A2A)
                                        )
                                        .clickable { distortionType = type }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        type.displayName,
                                        color = if (distortionType == type) Color.White else Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (distortionType != DistortionType.NONE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("畸变强度: $distortionStrength", color = Color.White, fontSize = 13.sp)
                            Slider(
                                value = distortionStrength.toFloat(),
                                onValueChange = { distortionStrength = it.toInt() },
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

            // 预览区
            if (sourceBitmap != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("预览", color = HasselbladOrange, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                androidx.compose.foundation.Image(
                                    bitmap = previewBitmap?.asImageBitmap() ?: sourceBitmap.asImageBitmap(),
                                    contentDescription = "预览",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 导出对话框
    if (showExportDialog) {
        LUTExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { params ->
                scope.launch {
                    val file = File(context.getExternalFilesDir("LUT"), "${params.title}.cube")
                    val success = lutManager.exportToCube(params, file)
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            "LUT 已导出到: ${file.absolutePath}",
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
private fun LUTChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) HasselbladOrange else Color(0xFF2A2A2A))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

/**
 * 暗角中心点选择器（可拖拽）
 */
@Composable
private fun VignetteCenterPointSelector(
    centerX: Float,
    centerY: Float,
    onCenterChange: (Float, Float) -> Unit
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    val newX = (centerX + drag.x / size.width).coerceIn(0f, 1f)
                    val newY = (centerY + drag.y / size.height).coerceIn(0f, 1f)
                    onCenterChange(newX, newY)
                }
            }
    ) {
        // 网格
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 绘制网格线
            for (i in 1..3) {
                drawLine(
                    Color.White.copy(alpha = 0.2f),
                    Offset(w * i / 4f, 0f),
                    Offset(w * i / 4f, h),
                    strokeWidth = 1f
                )
                drawLine(
                    Color.White.copy(alpha = 0.2f),
                    Offset(0f, h * i / 4f),
                    Offset(w, h * i / 4f),
                    strokeWidth = 1f
                )
            }

            // 绘制中心点
            drawCircle(
                HasselbladOrange,
                radius = 12f,
                center = Offset(centerX * w, centerY * h)
            )
            drawCircle(
                Color.White,
                radius = 6f,
                center = Offset(centerX * w, centerY * h)
            )
        }

        // 坐标标签
        Text(
            "X: ${(centerX * 100).toInt()}%  Y: ${(centerY * 100).toInt()}%",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        )
    }
}

@Composable
private fun LUTExportDialog(
    onDismiss: () -> Unit,
    onExport: (LUTExportParams) -> Unit
) {
    var title by remember { mutableStateOf("OMaster Custom LUT") }
    var contrast by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var brightness by remember { mutableFloatStateOf(0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出 LUT") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("LUT 名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("对比度: ${contrast.toInt()}", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = -100f..100f
                )
                Text("饱和度: ${saturation.toInt()}", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = -100f..100f
                )
                Text("亮度: ${brightness.toInt()}", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = -100f..100f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onExport(LUTExportParams(
                    title = title,
                    contrast = contrast,
                    saturation = saturation,
                    brightness = brightness
                ))
            }) {
                Text("导出", color = HasselbladOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = Color(0xFF1F1F1F),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
