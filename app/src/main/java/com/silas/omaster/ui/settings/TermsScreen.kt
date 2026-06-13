package com.silas.omaster.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.PureBlack
import com.silas.omaster.util.perform

/**
 * 用户协议页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 标题栏
        TopAppBar(
            title = { Text("用户协议", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = {
                    haptic.perform(HapticFeedbackType.TextHandleMove)
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = PureBlack,
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 最后更新时间
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "最后更新：2026年6月1日",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // 欢迎卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0).copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "欢迎使用我们的服务",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "使用我们的应用即表示您同意本用户协议和隐私政策",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 重要条款
            item {
                Text(
                    text = "重要条款",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyTermItem(
                        icon = Icons.Default.Person,
                        title = "用户责任",
                        description = "您需要对自己的账户安全和使用行为负责"
                    )
                    KeyTermItem(
                        icon = Icons.Default.Copyright,
                        title = "知识产权",
                        description = "应用内所有内容均受知识产权法保护"
                    )
                    KeyTermItem(
                        icon = Icons.Default.Public,
                        title = "服务范围",
                        description = "我们致力于提供稳定、高质量的摄影工具服务"
                    )
                    KeyTermItem(
                        icon = Icons.Default.Security,
                        title = "隐私保护",
                        description = "我们严格保护您的个人信息和隐私安全"
                    )
                }
            }

            // 完整协议
            item {
                Text(
                    text = "完整协议",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TermSection(
                            number = "1",
                            title = "服务条款",
                            content = "本应用提供专业的摄影后期处理工具，包括但不限于 哈苏之眼、色彩调整、滤镜效果、水印添加等功能。我们将持续更新和优化服务，为用户提供更好的使用体验。"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TermSection(
                            number = "2",
                            title = "用户账户",
                            content = "用户需要注册账户才能使用部分功能。请妥善保管账户信息，对账户下的所有行为负责。如发现账户异常，请立即联系我们。"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TermSection(
                            number = "3",
                            title = "用户内容",
                            content = "用户上传和处理的图片内容归用户所有。我们不会在未经许可的情况下使用或分享您的图片内容。"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TermSection(
                            number = "4",
                            title = "禁止行为",
                            content = "禁止利用本应用从事任何违法活动，禁止传播恶意代码，禁止攻击或干扰服务正常运行。"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TermSection(
                            number = "5",
                            title = "免责声明",
                            content = "我们尽力保证服务稳定运行，但不对因不可抗力或技术原因导致的服务中断承担责任。"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TermSection(
                            number = "6",
                            title = "协议修改",
                            content = "我们保留随时修改本协议的权利，修改后的协议将在应用内公布，继续使用即表示您同意修改后的协议。"
                        )
                    }
                }
            }

            // 底部间距
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun KeyTermItem(
    icon: androidx.compose.material.icons.Icon,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = HasselbladOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TermSection(
    number: String,
    title: String,
    content: String
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HasselbladOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, start = 32.dp)
        )
    }
}
