package com.silas.omaster.ui.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.silas.omaster.mask.AISubject
import com.silas.omaster.mask.AdjustmentMask
import com.silas.omaster.mask.BlendMode
import com.silas.omaster.mask.MaskType
import com.silas.omaster.mask.MaskViewModel
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack

/**
 * 蒙版管理页面
 * 提供 2026 标准的蒙版编辑能力
 * - 渐变蒙版（线性 / 径向）
 * - 画笔蒙版
 * - AI 智能蒙版（天空/人物/主体）
 * - 亮度/颜色蒙版
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaskEditorScreen(
    onBack: () -> Unit
) {
    val viewModel: MaskViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MaskViewModel.create(LocalContext.current)
            }
        }
    )

    val masks by viewModel.masks.collectAsState()
    val selectedMaskId by viewModel.selectedMaskId.collectAsState()
    val selectedMask = remember(masks, selectedMaskId) {
        masks.find { it.id == selectedMaskId }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showAISubjects by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // TopAppBar
        TopAppBar(
            title = { Text("局部调整 · 蒙版", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "添加蒙版", tint = HasselbladOrange)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        // 蒙版列表
        if (masks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = HasselbladOrange.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有蒙版",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "点击右上角 + 添加蒙版",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HasselbladOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加第一个蒙版")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(masks) { mask ->
                    MaskListItem(
                        mask = mask,
                        isSelected = mask.id == selectedMaskId,
                        onSelect = { viewModel.selectMask(mask.id) },
                        onToggle = { viewModel.toggleEnabled(mask.id) },
                        onDelete = { viewModel.removeMask(mask.id) }
                    )
                }
            }
        }

        // 选中蒙版的编辑面板
        selectedMask?.let { mask ->
            Spacer(modifier = Modifier.height(16.dp))
            MaskEditPanel(
                mask = mask,
                onUpdate = { viewModel.updateMask(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }

    if (showAddDialog) {
        AddMaskDialog(
            onDismiss = { showAddDialog = false },
            onAddType = { type ->
                val newMask = AdjustmentMask(
                    name = when (type) {
                        MaskType.LINEAR_GRADIENT -> "线性渐变"
                        MaskType.RADIAL -> "径向渐变"
                        MaskType.BRUSH -> "画笔蒙版"
                        MaskType.AI -> "AI 智能"
                        MaskType.LUMINANCE -> "亮度范围"
                        MaskType.COLOR -> "颜色范围"
                    },
                    type = type
                )
                viewModel.addCustomMask(type, newMask.name)
                showAddDialog = false
            },
            onAddAI = {
                showAddDialog = false
                showAISubjects = true
            },
            onAddPreset = { preset ->
                viewModel.addPreset(preset)
                showAddDialog = false
            }
        )
    }

    if (showAISubjects) {
        AISubjectPicker(
            onDismiss = { showAISubjects = false },
            onSelect = { subject ->
                viewModel.generateAIMask(subject)
                showAISubjects = false
            }
        )
    }
}

@Composable
private fun MaskListItem(
    mask: AdjustmentMask,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (mask.type) {
        MaskType.LINEAR_GRADIENT -> Icons.Default.SwapVert
        MaskType.RADIAL -> Icons.Default.RadioButtonChecked
        MaskType.BRUSH -> Icons.Default.Brush
        MaskType.AI -> Icons.Default.AutoAwesome
        MaskType.LUMINANCE -> Icons.Default.Brightness6
        MaskType.COLOR -> Icons.Default.Palette
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) HasselbladOrange else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1F1F1F) else Color(0xFF2A2A2A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (mask.enabled) HasselbladOrange.copy(alpha = 0.2f)
                        else Color.White.copy(alpha = 0.05f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (mask.enabled) HasselbladOrange else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mask.name,
                    color = if (mask.enabled) Color.White else Color.White.copy(alpha = 0.4f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    "${mask.type.displayName} · ${mask.localParams.nonZeroCount()}个参数",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Switch(
                checked = mask.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HasselbladOrange,
                    checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "删除",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MaskEditPanel(
    mask: AdjustmentMask,
    onUpdate: (AdjustmentMask) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "编辑: ${mask.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 名称
            OutlinedTextField(
                value = mask.name,
                onValueChange = { onUpdate(mask.copy(name = it)) },
                label = { Text("蒙版名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = HasselbladOrange,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedLabelColor = HasselbladOrange,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 不透明度
            Text(
                "不透明度: ${(mask.opacity * 100).toInt()}%",
                color = Color.White,
                fontSize = 13.sp
            )
            Slider(
                value = mask.opacity,
                onValueChange = { onUpdate(mask.copy(opacity = it)) },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = HasselbladOrange,
                    activeTrackColor = HasselbladOrange,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            // 混合模式
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "混合模式",
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlendMode.entries.forEach { mode ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (mask.blendMode == mode) HasselbladOrange
                                else Color(0xFF2A2A2A)
                            )
                            .clickable { onUpdate(mask.copy(blendMode = mode)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mode.displayName,
                            color = if (mask.blendMode == mode) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 渐变类型特定设置
            if (mask.type == MaskType.LINEAR_GRADIENT || mask.type == MaskType.RADIAL) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "羽化: ${(mask.gradientParams.feathering * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Slider(
                    value = mask.gradientParams.feathering,
                    onValueChange = {
                        onUpdate(mask.copy(gradientParams = mask.gradientParams.copy(feathering = it)))
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = HasselbladOrange,
                        activeTrackColor = HasselbladOrange,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "反转蒙版",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = mask.gradientParams.invert,
                        onCheckedChange = {
                            onUpdate(mask.copy(gradientParams = mask.gradientParams.copy(invert = it)))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = HasselbladOrange,
                            checkedTrackColor = HasselbladOrange.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // 画笔特定设置
            if (mask.type == MaskType.BRUSH) {
                Spacer(modifier = Modifier.height(16.dp))
                BrushSettings(
                    params = mask.brushParams,
                    onUpdate = { onUpdate(mask.copy(brushParams = it)) }
                )
            }
        }
    }
}

@Composable
private fun BrushSettings(
    params: com.silas.omaster.mask.BrushMaskParams,
    onUpdate: (com.silas.omaster.mask.BrushMaskParams) -> Unit
) {
    Column {
        Text("画笔大小: ${(params.size * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
        Slider(
            value = params.size,
            onValueChange = { onUpdate(params.copy(size = it)) },
            valueRange = 0.01f..0.5f,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("硬度: ${(params.hardness * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
        Slider(
            value = params.hardness,
            onValueChange = { onUpdate(params.copy(hardness = it)) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("流量: ${(params.flow * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
        Slider(
            value = params.flow,
            onValueChange = { onUpdate(params.copy(flow = it)) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange
            )
        )
    }
}

@Composable
private fun AddMaskDialog(
    onDismiss: () -> Unit,
    onAddType: (MaskType) -> Unit,
    onAddAI: () -> Unit,
    onAddPreset: (AdjustmentMask) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加蒙版") },
        text = {
            Column {
                Text("快速预设", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                AdjustmentMask.DEFAULT_PRESETS.forEach { preset ->
                    TextButton(
                        onClick = { onAddPreset(preset) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✨ ${preset.name}", color = HasselbladOrange)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("自定义蒙版", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                MaskType.entries.forEach { type ->
                    val label = when (type) {
                        MaskType.LINEAR_GRADIENT -> "📐 线性渐变"
                        MaskType.RADIAL -> "⭕ 径向渐变"
                        MaskType.BRUSH -> "🖌️ 画笔蒙版"
                        MaskType.AI -> "🤖 AI 智能"
                        MaskType.LUMINANCE -> "💡 亮度范围"
                        MaskType.COLOR -> "🎨 颜色范围"
                    }
                    TextButton(
                        onClick = { onAddType(type) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, color = Color.White)
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

@Composable
private fun AISubjectPicker(
    onDismiss: () -> Unit,
    onSelect: (AISubject) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择 AI 识别目标") },
        text = {
            Column {
                AISubject.entries.forEach { subject ->
                    TextButton(
                        onClick = { onSelect(subject) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(subject.displayName, color = Color.White)
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
