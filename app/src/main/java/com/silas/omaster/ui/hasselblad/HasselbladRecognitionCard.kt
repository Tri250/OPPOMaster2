package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.model.SceneProfile
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 哈苏大师识别结果卡片
 * 显示场景识别结果、置信度、备选场景
 *
 * 设计规范：
 * - 哈苏橙渐变置信度条
 * - HNCS 自然色彩已优化提示
 * - 备选场景进度条
 */
@Composable
fun HasselbladRecognitionCard(
    profile: SceneProfile,
    confidence: Float,
    alternativeScenes: List<SceneProfile>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HasselbladTheme.CardCornerRadius.dp))
            .background(HasselbladTheme.CardBackground)
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "哈苏大师识别",
            color = HasselbladTheme.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 主场景识别结果
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 场景图标和名称
            Text(
                text = profile.category.icon,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "${profile.name} · 置信度 ${(confidence * 100).toInt()}%",
                    color = HasselbladTheme.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // HNCS 提示
                Text(
                    text = "HNCS 自然色彩已优化",
                    color = HasselbladTheme.HasselbladOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 置信度进度条
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(HasselbladTheme.ConfidenceBarCornerRadius.dp))
                .background(HasselbladTheme.ConfidenceBarBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(confidence)
                    .clip(RoundedCornerShape(HasselbladTheme.ConfidenceBarCornerRadius.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                HasselbladTheme.HasselbladOrangeLight,
                                HasselbladTheme.HasselbladOrange,
                                HasselbladTheme.HasselbladOrangeDark
                            )
                        )
                    )
            )
        }

        // 置信度百分比文字
        Text(
            text = "${(confidence * 100).toInt()}%",
            modifier = Modifier.align(Alignment.End),
            color = HasselbladTheme.HasselbladOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        // 备选场景（如果有）
        if (alternativeScenes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "备选场景：",
                color = HasselbladTheme.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            alternativeScenes.take(2).forEach { altScene ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = altScene.name,
                        color = HasselbladTheme.TextTertiary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${(altScene.confidence * 100).toInt()}%",
                        color = HasselbladTheme.TextTertiary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 备选场景进度条
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(HasselbladTheme.CardBackgroundHighlight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(altScene.confidence)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HasselbladTheme.TextTertiary)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 简化版识别结果卡片
 */
@Composable
fun HasselbladRecognitionCardSimple(
    sceneName: String,
    sceneIcon: String,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = HasselbladTheme.CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sceneIcon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sceneName,
                    color = HasselbladTheme.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 置信度条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(HasselbladTheme.ConfidenceBarBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(confidence)
                            .clip(RoundedCornerShape(3.dp))
                            .background(HasselbladTheme.HasselbladOrange)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${(confidence * 100).toInt()}%",
                color = HasselbladTheme.HasselbladOrange,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}