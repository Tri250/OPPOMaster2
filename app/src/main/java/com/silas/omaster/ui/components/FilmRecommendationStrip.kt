package com.silas.omaster.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import com.silas.omaster.model.FilmPreset
import com.silas.omaster.model.FilmSeries
import com.silas.omaster.ui.theme.*

/**
 * 胶片推荐条组件
 * 
 * 功能：
 * - 展示场景匹配的胶片列表
 * - 胶片选择和切换
 * - 匹配度显示
 * - 胶片信息展示
 * 
 * 对齐 Web 端 FilmRecommendationStrip.tsx
 */
@Composable
fun FilmRecommendationStrip(
    films: List<FilmPreset>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Camera,
                    null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "胶片推荐",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Text(
                text = "${films.size} 款胶片",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 胶片列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(films) { film ->
                FilmCard(
                    film = film,
                    selected = film.id == selectedId,
                    onClick = {
                        haptic.perform(HapticFeedbackType.TextHandleMove)
                        onSelect(film.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun FilmCard(
    film: FilmPreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) HasselbladOrange.copy(alpha = 0.15f) else Color(0xFF2A2A2A),
        label = "bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.1f),
        label = "border"
    )

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .clickable { onClick() }
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 胶片图标/预览
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (film.series) {
                            FilmSeries.CLASSIC -> Color(0xFFD4AF37) // 金色
                            FilmSeries.EMOTION -> Color(0xFF4CAF50) // 绿色
                            FilmSeries.STRUCTURE -> Color(0xFF808080) // 灰色
                            FilmSeries.DIGITAL -> Color(0xFF9C27B0) // 紫色
                            else -> HasselbladOrange
                        }.copy(alpha = 0.3f)
                    )
            ) {
                // 胶片名称首字母
                Text(
                    text = film.name.firstOrNull()?.toString() ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = when (film.series) {
                        FilmSeries.CLASSIC -> Color(0xFFD4AF37)
                        FilmSeries.EMOTION -> Color(0xFF4CAF50)
                        FilmSeries.STRUCTURE -> Color(0xFF808080)
                        FilmSeries.DIGITAL -> Color(0xFF9C27B0)
                        else -> HasselbladOrange
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
                
                // 选中指示器
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 胶片名称
            Text(
                text = film.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) HasselbladOrange else Color.White,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // 匹配度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(film.matchScore * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.6f)
                )
                
                Spacer(modifier = Modifier.width(2.dp))
                
                Icon(
                    Icons.Default.Star,
                    null,
                    tint = if (selected) HasselbladOrange else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 胶片详情弹窗
 */
@Composable
fun FilmDetailDialog(
    film: FilmPreset,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF1A1A1A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HasselbladOrange.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = film.name.firstOrNull()?.toString() ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = film.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = film.series.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
        },
        text = {
            Column {
                // 匹配度
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "匹配度",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${(film.matchScore * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HasselbladOrange
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 胶片描述
                Text(
                    text = film.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 胶片特性
                Text(
                    text = "胶片特性",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 特性列表
                Column {
                    FilmCharacteristicRow("色彩风格", film.colorStyle)
                    FilmCharacteristicRow("颗粒感", film.grainLevel)
                    FilmCharacteristicRow("对比度", film.contrastLevel)
                    FilmCharacteristicRow("适用场景", film.bestFor)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                    onApply()
                },
                colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("应用此胶片")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FilmCharacteristicRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}