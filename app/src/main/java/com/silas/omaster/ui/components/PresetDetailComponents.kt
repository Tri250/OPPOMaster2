package com.silas.omaster.ui.components

import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.HasselbladOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ui.theme.HasselbladOrange

/**
 * 预设统计数据卡片（对齐用户规范）
 * 显示下载量、评分、评价数
 */
@Composable
fun PresetStatsCard(
    downloads: Int,
    rating: Float,
    ratingCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 下载量
            StatItem(
                value = if (downloads >= 1000) "${(downloads / 1000)}k" else downloads.toString(),
                label = "下载"
            )
            
            // 评分
            StatItem(
                value = String.format("%.1f", rating),
                label = "评分"
            )
            
            // 评价数
            StatItem(
                value = ratingCount.toString(),
                label = "评价"
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp
        )
    }
}

/**
 * 拍摄建议详情卡片（对齐用户规范：渐变背景）
 * 显示环境建议、场景推荐、拍摄要点
 */
@Composable
fun ShootingTipsDetailCard(
    environment: String,
    scenes: String,
    points: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1A1A2E), Color(0xFF0F0F1A))
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // 标题
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = HasselbladOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "拍摄建议",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 结构化内容
                TipRow(label = "环境建议", content = environment)
                Spacer(modifier = Modifier.height(8.dp))
                TipRow(label = "场景推荐", content = scenes)
                Spacer(modifier = Modifier.height(8.dp))
                TipRow(label = "拍摄要点", content = points)
            }
        }
    }
}

@Composable
private fun TipRow(
    label: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 圆点指示器
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(HasselbladOrange, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                color = HasselbladOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * 用户评价卡片（对齐用户规范）
 */
data class UserComment(
    val id: String,
    val user: String,
    val content: String,
    val rating: Int
)

@Composable
fun UserCommentsCard(
    comments: List<UserComment>,
    onViewAll: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (comments.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💬",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "用户评价",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 评价列表（最多显示2条）
            comments.take(2).forEach { comment ->
                CommentItem(comment = comment)
                if (comment != comments.take(2).last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 查看全部按钮
            if (comments.size > 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "查看全部评价",
                    color = HasselbladOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: UserComment
) {
    Column {
        // 用户名和评分
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 星级
            repeat(5) { index ->
                Text(
                    text = "★",
                    color = if (index < comment.rating) Color(0xFFFFC107) else Color.White.copy(alpha = 0.2f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = comment.user,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 评价内容
        Text(
            text = comment.content,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

/**
 * 关联推荐卡片（对齐用户规范）
 */
data class RelatedPreset(
    val id: String,
    val name: String,
    val coverPath: String,
    val author: String? = null,
    val tags: List<String>? = null
)

@Composable
fun RelatedPresetsCard(
    presets: List<RelatedPreset>,
    onSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 标题（对齐用户规范）
        Text(
            text = "🎞️ 看了这个的人也看了",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 推荐列表（对齐用户规范：w-24 h-24）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                RelatedPresetItem(
                    preset = preset,
                    onClick = { onSelect(preset.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RelatedPresetItem(
    preset: RelatedPreset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 封面图（对齐用户规范：96dp = w-24）
            Card(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
            ) {
                // 这里可以加载图片，暂时用占位
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 名称
            Text(
                text = preset.name,
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1
            )
            
            // 作者
            preset.author?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * 一键应用动画反馈按钮（对齐用户规范）
 * 点击后显示绿色"已应用哈苏配方"，2秒后恢复
 */
@Composable
fun ApplyPresetButton(
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    var applied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val buttonColor by animateColorAsState(
        targetValue = if (applied) Color(0xFF4CAF50) else HasselbladOrange,
        animationSpec = tween(durationMillis = 300),
        label = "buttonColor"
    )
    
    Button(
        onClick = {
            applied = true
            onApply()
            // 2秒后恢复（使用rememberCoroutineScope避免内存泄漏）
            coroutineScope.launch {
                delay(2000)
                applied = false
            }
        },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = if (applied) Icons.Default.CheckCircle else Icons.Default.Sparkles,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (applied) "已应用哈苏配方" else "一键应用哈苏配方",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 收藏按钮组件（对齐用户规范）
 */
@Composable
fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isFavorite) Color.Red.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
    val textColor = if (isFavorite) Color.Red else Color.White.copy(alpha = 0.8f)
    val borderColor = if (isFavorite) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f)
    
    Button(
        onClick = onToggle,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isFavorite) "已收藏" else "收藏",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}