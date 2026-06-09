package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 大师建议卡片组件
 * 显示哈苏大师风格的拍摄建议
 *
 * 设计规范：
 * - 每条建议带图标
 * - 专业影像工具质感
 * - 哈苏品牌简洁克制
 */
@Composable
fun HasselbladMasterTipsCard(
    tips: List<String>,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "大师建议",
                color = HasselbladTheme.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 建议列表
        tips.forEach { tip ->
            HasselbladTipItem(tip = tip)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * 单条建议项
 */
@Composable
fun HasselbladTipItem(
    tip: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(HasselbladTheme.CardBackgroundHighlight)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 根据建议内容自动匹配图标
        val icon = getTipIcon(tip)

        Text(
            text = icon,
            fontSize = 16.sp,
            modifier = Modifier.width(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = tip,
            color = HasselbladTheme.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 根据建议内容匹配图标
 */
private fun getTipIcon(tip: String): String {
    return when {
        tip.contains("光线") || tip.contains("光") || tip.contains("曝光") -> "☀️"
        tip.contains("时间") || tip.contains("时刻") || tip.contains("黄金") -> "🌅"
        tip.contains("构图") || tip.contains("角度") || tip.contains("层次") -> "📐"
        tip.contains("色彩") || tip.contains("颜色") || tip.contains("色调") -> "🎨"
        tip.contains("质感") || tip.contains("纹理") || tip.contains("细节") -> "🖼️"
        tip.contains("人像") || tip.contains("人物") || tip.contains("表情") -> "👤"
        tip.contains("夜景") || tip.contains("灯光") || tip.contains("霓虹") -> "🌃"
        tip.contains("胶片") || tip.contains("复古") -> "🎞️"
        tip.contains("宽幅") || tip.contains("XPAN") -> "📷"
        tip.contains("安全") || tip.contains("注意") -> "⚠️"
        tip.contains("自然") || tip.contains("真实") -> "🌿"
        else -> "💡"
    }
}

/**
 * 简化版大师建议（单行显示）
 */
@Composable
fun HasselbladMasterTipsSimple(
    tips: List<String>,
    modifier: Modifier = Modifier
) {
    if (tips.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HasselbladTheme.CardBackground)
            .padding(12.dp)
    ) {
        Text(
            text = "💡 大师建议",
            color = HasselbladTheme.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 显示第一条建议
        Text(
            text = tips.firstOrNull() ?: "",
            color = HasselbladTheme.TextTertiary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        if (tips.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "查看更多建议...",
                color = HasselbladTheme.HasselbladOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * XPAN 宽幅提示卡片
 */
@Composable
fun HasselbladXPanTipCard(
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
                text = "📷",
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "试试 XPAN 宽幅模式",
                    color = HasselbladTheme.HasselbladOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "哈苏经典宽幅，让风景更具电影感",
                    color = HasselbladTheme.TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * HNCS 水印提示
 */
@Composable
fun HasselbladHNCSWatermark(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        color = HasselbladTheme.CardBackground.copy(alpha = 0.6f)
    ) {
        Text(
            text = HasselbladTheme.WatermarkText,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = HasselbladTheme.TextTertiary,
            fontSize = HasselbladTheme.WatermarkFontSize.sp,
            fontWeight = FontWeight.Medium
        )
    }
}