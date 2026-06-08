package com.silas.omaster.ui.features

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.ai.SceneRecognitionManager
import com.silas.omaster.ai.SceneType
import kotlinx.coroutines.*

/**
 * 相机实时场景识别页面
 * 用户打开相机 → 实时预览 → AI识别场景 → 推荐预设参数 → 帮助拍摄大片
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSceneRecognitionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sceneManager = remember { SceneRecognitionManager.getInstance(context) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 状态
    var cameraPreview by remember { mutableStateOf<Bitmap?>(null) }
    var isRecognizing by remember { mutableStateOf(false) }
    var recognizedScene by remember { mutableStateOf<SceneType?>(null) }
    var confidence by remember { mutableStateOf(0f) }
    var recommendedPreset by remember { mutableStateOf<ScenePreset?>(null) }
    var showPresetDetail by remember { mutableStateOf(false) }
    var showTips by remember { mutableStateOf(false) }
    var autoRecognize by remember { mutableStateOf(true) }
    var recognitionCount by remember { mutableStateOf(0) }

    // 自动识别
    LaunchedEffect(autoRecognize, cameraPreview) {
        if (autoRecognize && cameraPreview != null) {
            while (isActive) {
                isRecognizing = true
                try {
                    val result = sceneManager.recognizeScene(cameraPreview!!)
                    recognizedScene = result.sceneType
                    confidence = result.confidence
                    recognitionCount++

                    // 匹配预设
                    recommendedPreset = scenePresets.find {
                        it.sceneType == result.sceneType
                    } ?: scenePresets.first()

                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (e: Exception) {
                    // 识别失败
                }
                isRecognizing = false
                delay(2000) // 每2秒识别一次
            }
        }
    }

    // 主界面
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 相机预览区域
        if (cameraPreview != null) {
            Image(
                bitmap = cameraPreview!!.asImageBitmap(),
                contentDescription = "相机预览",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 空状态
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "点击下方按钮开始相机取景",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (autoRecognize) Color(0xFF4CAF50).copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (autoRecognize) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (autoRecognize) "AI实时识别" else "手动识别",
                            color = if (autoRecognize) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = { autoRecognize = !autoRecognize }) {
                    Icon(
                        Icons.Default.Refresh,
                        "切换",
                        tint = Color.White,
                        modifier = Modifier.then(
                            if (autoRecognize) Modifier.rotate(360f) else Modifier
                        )
                    )
                }
            }
        }

        // 识别动画
        if (isRecognizing && cameraPreview != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatableSpec(tween(2000))
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .rotate(rotation)
                        .border(2.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), CircleShape)
                )

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI识别中...", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // 识别成功指示器
        if (recognizedScene != null && !isRecognizing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${recognizedScene!!.displayName} · ${(confidence * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 相机参数显示
        if (recommendedPreset != null && cameraPreview != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 120.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ParameterRow(Icons.Default.Camera, recommendedPreset!!.params.aperture)
                    ParameterRow(Icons.Default.Timer, recommendedPreset!!.params.shutter)
                    ParameterRow(Icons.Default.Exposure, "ISO ${recommendedPreset!!.params.iso}")
                    ParameterRow(Icons.Default.Thermostat, recommendedPreset!!.params.wb)
                }
            }
        }

        // 底部推荐预设卡片
        if (recommendedPreset != null && cameraPreview != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(16.dp)
                        .padding(top = 48.dp)
                ) {
                    Row(
                        modifier = Modifier.clickable { showPresetDetail = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 样张预览
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color(0xFFFF6B35).copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(recommendedPreset!!.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    recommendedPreset!!.icon,
                                    null,
                                    tint = recommendedPreset!!.color,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = recommendedPreset!!.color.copy(alpha = 0.2f)
                                ) {
                                    Icon(
                                        recommendedPreset!!.icon,
                                        null,
                                        tint = recommendedPreset!!.color,
                                        modifier = Modifier.size(24.dp).padding(4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    recommendedPreset!!.name,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "AI推荐",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                recommendedPreset!!.desc,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "哈苏${recommendedPreset!!.hasselbladStyle}风格",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // 应用参数
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6B35)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("应用", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 查看拍摄技巧按钮
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .clickable { showTips = true },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("查看拍摄技巧", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 识别次数统计
        if (cameraPreview != null) {
            Text(
                "识别 #$recognitionCount",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )
        }
    }

    // 预设详情弹窗
    if (showPresetDetail && recommendedPreset != null) {
        PresetDetailSheet(
            preset = recommendedPreset!!,
            onDismiss = { showPresetDetail = false },
            onApply = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showPresetDetail = false
            }
        )
    }

    // 拍摄技巧弹窗
    if (showTips && recommendedPreset != null) {
        TipsSheet(
            preset = recommendedPreset!!,
            onDismiss = { showTips = false }
        )
    }
}

@Composable
private fun ParameterRow(icon: ImageVector, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun PresetDetailSheet(
    preset: ScenePreset,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.padding(24.dp)) {
            // 样张对比
            Text("大师样张效果对比", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.weight(1f).aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("当前取景", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).aspectRatio(4f / 3f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(2.dp, Color(0xFFFF6B35).copy(alpha = 0.5f)),
                    color = preset.color.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(preset.icon, null, tint = preset.color, modifier = Modifier.size(32.dp))
                            Text("${preset.name}样张", color = Color(0xFFFF6B35), fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 推荐参数
            Text("推荐拍摄参数", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                preset.params.toList().forEach { (key, value) ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.05f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                when (key) {
                                    "iso" -> "ISO"
                                    "shutter" -> "快门"
                                    "aperture" -> "光圈"
                                    "wb" -> "白平衡"
                                    else -> key
                                },
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(value, color = Color(0xFFFF6B35), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 应用按钮
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B35)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("应用${preset.name}参数", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun TipsSheet(
    preset: ScenePreset,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = preset.color.copy(alpha = 0.2f)
                ) {
                    Icon(
                        preset.icon,
                        null,
                        tint = preset.color,
                        modifier = Modifier.size(48.dp).padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(preset.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(preset.desc, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("专业拍摄技巧", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            preset.tips.forEachIndexed { index, tip ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        ) {
                            Text(
                                "${index + 1}",
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(tip, color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开始拍摄", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

// 场景预设数据
data class ScenePreset(
    val id: String,
    val name: String,
    val sceneType: SceneType,
    val icon: ImageVector,
    val color: Color,
    val hasselbladStyle: String,
    val desc: String,
    val tips: List<String>,
    val params: SceneParams
)

data class SceneParams(
    val iso: String = "100",
    val shutter: String = "1/125",
    val aperture: String = "f/2.8",
    val wb: String = "5500K"
)

val scenePresets = listOf(
    ScenePreset(
        id = "portrait",
        name = "人像大师",
        sceneType = SceneType.PORTRAIT,
        icon = Icons.Default.Person,
        color = Color(0xFFE91E63),
        hasselbladStyle = "portrait",
        desc = "柔美肤色，自然光影",
        tips = listOf("建议使用f/1.8大光圈", "对焦人物眼睛", "背景虚化增强层次"),
        params = SceneParams("200", "1/125", "f/1.8", "5500K")
    ),
    ScenePreset(
        id = "landscape",
        name = "风景大师",
        sceneType = SceneType.LANDSCAPE,
        icon = Icons.Default.Landscape,
        color = Color(0xFF4CAF50),
        hasselbladStyle = "natural",
        desc = "通透质感，色彩饱满",
        tips = listOf("建议使用f/8小光圈", "开启HDR增强", "注意构图三分法"),
        params = SceneParams("100", "1/60", "f/8", "5600K")
    ),
    ScenePreset(
        id = "night",
        name = "夜景大师",
        sceneType = SceneType.NIGHT,
        icon = Icons.Default.NightsStay,
        color = Color(0xFF3F51B5),
        hasselbladStyle = "cinematic",
        desc = "降噪增强，氛围感强",
        tips = listOf("建议ISO 1600-3200", "使用三脚架稳定", "注意光源曝光"),
        params = SceneParams("3200", "1/15", "f/1.6", "4000K")
    ),
    ScenePreset(
        id = "food",
        name = "美食大师",
        sceneType = SceneType.FOOD,
        icon = Icons.Default.Restaurant,
        color = Color(0xFFFF9800),
        hasselbladStyle = "natural",
        desc = "暖色调，食欲感强",
        tips = listOf("建议45度俯拍", "使用暖色光源", "注意食物纹理"),
        params = SceneParams("200", "1/60", "f/2.8", "5200K")
    ),
    ScenePreset(
        id = "sunset",
        name = "日落大师",
        sceneType = SceneType.SUNSET,
        icon = Icons.Default.WbSunny,
        color = Color(0xFFFF5722),
        hasselbladStyle = "cinematic",
        desc = "金色暖调，氛围浪漫",
        tips = listOf("拍摄时机黄金时刻", "注意剪影构图", "开启HDR保留细节"),
        params = SceneParams("100", "1/125", "f/5.6", "6000K")
    ),
    ScenePreset(
        id = "street",
        name = "街拍大师",
        sceneType = SceneType.STREET,
        icon = Icons.Default.DirectionsCar,
        color = Color(0xFFFF5722),
        hasselbladStyle = "cinematic",
        desc = "人文气息，故事感",
        tips = listOf("快速抓拍瞬间", "注意光影对比", "胶片质感增强"),
        params = SceneParams("400", "1/250", "f/5.6", "5500K")
    ),
    ScenePreset(
        id = "flower",
        name = "花卉大师",
        sceneType = SceneType.FLOWER,
        icon = Icons.Default.LocalFlorist,
        color = Color(0xFFE91E63),
        hasselbladStyle = "natural",
        desc = "色彩鲜艳，细节丰富",
        tips = listOf("微距拍摄细节", "注意背景简洁", "自然光最佳"),
        params = SceneParams("100", "1/200", "f/2.8", "5500K")
    ),
    ScenePreset(
        id = "architecture",
        name = "建筑大师",
        sceneType = SceneType.BUILDING,
        icon = Icons.Default.Apartment,
        color = Color(0xFF607D8B),
        hasselbladStyle = "natural",
        desc = "线条清晰，质感强",
        tips = listOf("注意对称构图", "控制曝光平衡", "强调几何美感"),
        params = SceneParams("100", "1/125", "f/8", "5600K")
    ),
    ScenePreset(
        id = "pet",
        name = "宠物大师",
        sceneType = SceneType.PET,
        icon = Icons.Default.Pets,
        color = Color(0xFF9C27B0),
        hasselbladStyle = "natural",
        desc = "毛发细节，眼神灵动",
        tips = listOf("捕捉眼神光", "低角度拍摄", "注意动态瞬间"),
        params = SceneParams("200", "1/250", "f/2.8", "5500K")
    ),
    ScenePreset(
        id = "beach",
        name = "海滩大师",
        sceneType = SceneType.BEACH,
        icon = Icons.Default.Water,
        color = Color(0xFF00BCD4),
        hasselbladStyle = "cinematic",
        desc = "蔚蓝水域，清新通透",
        tips = listOf("注意水面反光", "低角度拍摄", "HDR增强天空"),
        params = SceneParams("100", "1/200", "f/5.6", "5600K")
    ),
    ScenePreset(
        id = "default",
        name = "通用大师",
        sceneType = SceneType.UNKNOWN,
        icon = Icons.Default.CameraAlt,
        color = Color(0xFF9E9E9E),
        hasselbladStyle = "natural",
        desc = "智能优化，全面提升",
        tips = listOf("注意光线条件", "稳定拍摄姿势", "构图简洁大方"),
        params = SceneParams("100", "1/125", "f/4", "5500K")
    )
)