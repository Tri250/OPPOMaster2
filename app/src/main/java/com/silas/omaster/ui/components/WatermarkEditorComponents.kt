package com.silas.omaster.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.OnSurfacePrimary
import com.silas.omaster.ui.theme.OnSurfaceSecondary
import com.silas.omaster.ui.theme.OnSurfaceTertiary
import com.silas.omaster.ui.theme.OnSurfaceDisabled
import com.silas.omaster.ui.theme.DividerColor
import com.silas.omaster.ui.theme.OnSurfaceInverse
import com.silas.omaster.ui.theme.OutlineVariant
import com.silas.omaster.util.perform
import com.silas.omaster.watermark.WatermarkEditorManager
import com.silas.omaster.watermark.WatermarkElement
import com.silas.omaster.watermark.WatermarkElementConfig
import com.silas.omaster.watermark.WatermarkPosition
import com.silas.omaster.watermark.WatermarkTemplate
import kotlin.math.roundToInt

/**
 * WM-001/WM-002: 水印模板选择器
 */
@Composable
fun WatermarkTemplateSelector(
    watermarkManager: WatermarkEditorManager,
    onTemplateSelected: (String) -> Unit,
    onEditTemplate: (WatermarkTemplate) -> Unit
) {
    val templates by remember { mutableStateOf(watermarkManager.templates) }
    val selectedTemplate by watermarkManager.selectedTemplate.collectAsState()
    val customTemplates by watermarkManager.customTemplates.collectAsState()
    val haptic = LocalHapticFeedback.current

    Column {
        // 品牌水印
        WatermarkTemplateSection(
            title = "品牌水印",
            templates = templates.filter { it.type == com.silas.omaster.watermark.WatermarkType.BRAND },
            selectedId = selectedTemplate?.id,
            onSelect = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.selectTemplate(it)
                onTemplateSelected(it)
            },
            onEdit = onEditTemplate
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 功能水印
        WatermarkTemplateSection(
            title = "功能水印",
            templates = templates.filter { it.type == com.silas.omaster.watermark.WatermarkType.FUNCTIONAL },
            selectedId = selectedTemplate?.id,
            onSelect = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.selectTemplate(it)
                onTemplateSelected(it)
            },
            onEdit = onEditTemplate
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 风格水印
        WatermarkTemplateSection(
            title = "风格水印",
            templates = templates.filter { it.type == com.silas.omaster.watermark.WatermarkType.STYLE },
            selectedId = selectedTemplate?.id,
            onSelect = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.selectTemplate(it)
                onTemplateSelected(it)
            },
            onEdit = onEditTemplate
        )

        // 自定义模板
        if (customTemplates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            WatermarkTemplateSection(
                title = "自定义模板",
                templates = customTemplates,
                selectedId = selectedTemplate?.id,
                onSelect = {
                    haptic.perform(HapticFeedbackType.LongPress)
                    watermarkManager.selectTemplate(it)
                    onTemplateSelected(it)
                },
                onEdit = onEditTemplate
            )
        }
    }
}

@Composable
private fun WatermarkTemplateSection(
    title: String,
    templates: List<WatermarkTemplate>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onEdit: (WatermarkTemplate) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfaceSecondary.copy(alpha = 0.8f / 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(templates) { template ->
                WatermarkTemplateItem(
                    template = template,
                    isSelected = template.id == selectedId,
                    onClick = { onSelect(template.id) },
                    onEdit = { onEdit(template) }
                )
            }
        }
    }
}

@Composable
private fun WatermarkTemplateItem(
    template: WatermarkTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) HasselbladOrange else androidx.compose.ui.graphics.Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HasselbladOrange.copy(alpha = 0.15f) else Color(0xFF2A2A2A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (template.type) {
                        com.silas.omaster.watermark.WatermarkType.BRAND -> Icons.Default.Image
                        com.silas.omaster.watermark.WatermarkType.FUNCTIONAL -> Icons.Default.Edit
                        com.silas.omaster.watermark.WatermarkType.STYLE -> Icons.Default.ColorLens
                        com.silas.omaster.watermark.WatermarkType.CUSTOM -> Icons.Default.Add
                    },
                    contentDescription = null,
                    tint = if (isSelected) HasselbladOrange else OnSurfacePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) HasselbladOrange else OnSurfacePrimary,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选中",
                    tint = HasselbladOrange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

/**
 * WM-002: 水印元素配置面板
 */
@Composable
fun WatermarkElementConfigPanel(
    watermarkManager: WatermarkEditorManager,
    onPositionChange: (WatermarkElement, WatermarkPosition) -> Unit,
    onColorChange: (WatermarkElement, Color) -> Unit,
    onAlphaChange: (WatermarkElement, Float) -> Unit,
    onToggleElement: (WatermarkElement) -> Unit,
    onSaveAsTemplate: () -> Unit
) {
    val elementConfig by watermarkManager.elementConfig.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        // 元素开关
        Text(
            text = "显示元素",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfacePrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ElementToggleRow(
            element = WatermarkElement.BRAND,
            name = "品牌名称",
            isEnabled = elementConfig.showBrand,
            onToggle = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.toggleElement(WatermarkElement.BRAND)
            }
        )

        ElementToggleRow(
            element = WatermarkElement.MODEL,
            name = "手机型号",
            isEnabled = elementConfig.showModel,
            onToggle = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.toggleElement(WatermarkElement.MODEL)
            }
        )

        ElementToggleRow(
            element = WatermarkElement.PARAMS,
            name = "参数栏",
            isEnabled = elementConfig.showParams,
            onToggle = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.toggleElement(WatermarkElement.PARAMS)
            }
        )

        ElementToggleRow(
            element = WatermarkElement.TIMESTAMP,
            name = "时间戳",
            isEnabled = elementConfig.showTimestamp,
            onToggle = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.toggleElement(WatermarkElement.TIMESTAMP)
            }
        )

        ElementToggleRow(
            element = WatermarkElement.VIGNETTE,
            name = "暗角效果",
            isEnabled = elementConfig.showVignette,
            onToggle = {
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.toggleElement(WatermarkElement.VIGNETTE)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 位置配置
        Text(
            text = "元素位置",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfacePrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 位置选择网格
        PositionGrid(
            selectedPosition = elementConfig.brandPosition,
            onPositionSelected = { pos ->
                haptic.perform(HapticFeedbackType.LongPress)
                watermarkManager.updateElementPosition(WatermarkElement.BRAND, pos)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 透明度配置
        Text(
            text = "透明度调整",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = OnSurfacePrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AlphaSlider(
            label = "品牌名称",
            value = elementConfig.brandAlpha,
            onValueChange = { watermarkManager.updateElementAlpha(WatermarkElement.BRAND, it) }
        )

        AlphaSlider(
            label = "手机型号",
            value = elementConfig.modelAlpha,
            onValueChange = { watermarkManager.updateElementAlpha(WatermarkElement.MODEL, it) }
        )

        AlphaSlider(
            label = "参数栏",
            value = elementConfig.paramsAlpha,
            onValueChange = { watermarkManager.updateElementAlpha(WatermarkElement.PARAMS, it) }
        )

        if (elementConfig.showVignette) {
            AlphaSlider(
                label = "暗角",
                value = elementConfig.vignetteAlpha,
                onValueChange = { watermarkManager.updateElementAlpha(WatermarkElement.VIGNETTE, it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 保存为模板按钮
        Button(
            onClick = {
                haptic.perform(HapticFeedbackType.LongPress)
                onSaveAsTemplate()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = HasselbladOrange
            )
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("另存为模板")
        }
    }
}

@Composable
private fun ElementToggleRow(
    element: WatermarkElement,
    name: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Switch(
            checked = isEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnSurfacePrimary,
                checkedTrackColor = HasselbladOrange,
                uncheckedThumbColor = OutlineVariant,
                uncheckedTrackColor = OutlineVariant.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PositionGrid(
    selectedPosition: WatermarkPosition,
    onPositionSelected: (WatermarkPosition) -> Unit
) {
    val positions = listOf(
        listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT),
        listOf(WatermarkPosition.CENTER_LEFT, WatermarkPosition.CENTER, WatermarkPosition.CENTER_RIGHT),
        listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM, WatermarkPosition.BOTTOM_RIGHT)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        positions.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { position ->
                    PositionButton(
                        position = position,
                        isSelected = position == selectedPosition,
                        onClick = { onPositionSelected(position) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionButton(
    position: WatermarkPosition,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isSelected) HasselbladOrange else Color(0xFF2A2A2A)
            )
            .border(
                1.dp,
                if (isSelected) HasselbladOrange else OutlineVariant,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
    )
}

@Composable
private fun AlphaSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceSecondary.copy(alpha = 0.8f / 0.7f),
            modifier = Modifier.width(60.dp)
        )

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.3f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = HasselbladOrange,
                activeTrackColor = HasselbladOrange,
                inactiveTrackColor = OutlineVariant.copy(alpha = 0.3f)
            )
        )

        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = OutlineVariant,
            modifier = Modifier.width(40.dp)
        )
    }
}

/**
 * WM-002: 另存为模板对话框
 */
@Composable
fun SaveAsTemplateDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var templateName by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "另存为模板",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "请输入模板名称",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary.copy(alpha = 0.8f / 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    placeholder = { Text("我的自定义模板") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "模板名不允许与系统模板同名",
                    style = MaterialTheme.typography.bodySmall,
                    color = OutlineVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (templateName.isNotBlank()) {
                        haptic.perform(HapticFeedbackType.LongPress)
                        onConfirm(templateName.trim())
                    }
                },
                enabled = templateName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = Color(0xFF1A1A1A),
        titleContentColor = OnSurfacePrimary,
        textContentColor = OnSurfacePrimary
    )
}

/**
 * WM-002: 拖拽编辑器（预留）
 * 未来版本可扩展为真正的拖拽编辑
 */
@Composable
fun DraggableWatermarkPreview(
    modifier: Modifier = Modifier
) {
    // 预留拖拽编辑功能
    // 当前版本通过PositionGrid控制位置
}
