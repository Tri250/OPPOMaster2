package com.silas.omaster.trailsnap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow

@Composable
fun XingYingJiHomeScreen(
    onNavigateToTimeline: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToLocations: () -> Unit,
    onNavigateToPeople: () -> Unit,
    onNavigateToTickets: () -> Unit,
    onNavigateToToolbox: () -> Unit,
    onNavigateToAnnualReport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val stats by repository.dashboardStats.collectAsState()
    val annual by repository.annualReport.collectAsState()

    LaunchedEffect(Unit) {
        repository.refresh()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TrailSnapTopBar(
                title = "行影集",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { /* refresh */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }

        item {
            AnnualReportBanner(
                year = annual?.year ?: 2025,
                onClick = onNavigateToAnnualReport
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatsGrid(stats = stats)
        }

        item {
            SectionTitle(title = "回忆入口")
        }

        item {
            QuickEntryCard(
                title = "时间轴",
                subtitle = "按时间回顾所有照片与视频",
                icon = Icons.Default.AccessTime,
                onClick = onNavigateToTimeline,
                badge = stats?.totalPhotos?.let { "${it + (stats?.totalVideos ?: 0)}" }
            )
        }

        item {
            QuickEntryCard(
                title = "相册",
                subtitle = "普通相册、智能相册与条件相册",
                icon = Icons.Default.PhotoAlbum,
                onClick = onNavigateToAlbums,
                badge = stats?.totalAlbums?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = "足迹地图",
                subtitle = "点亮你去过的每一座城市",
                icon = Icons.Default.Map,
                onClick = onNavigateToLocations,
                badge = stats?.locationCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = "人物",
                subtitle = "人脸识别聚类，找到每个人的身影",
                icon = Icons.Default.Group,
                onClick = onNavigateToPeople,
                badge = stats?.peopleCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = "行程票据",
                subtitle = "火车票、机票、景区门票自动识别",
                icon = Icons.Default.Train,
                onClick = onNavigateToTickets,
                badge = stats?.ticketCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = "工具箱",
                subtitle = "清理、整理、重命名与回收站",
                icon = Icons.Default.Construction,
                onClick = onNavigateToToolbox
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "把旅行从“拍过”变成“可回味、可分享、可沉淀”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun AnnualReportBanner(
    year: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        HasselbladOrange.copy(alpha = 0.9f),
                        WarningYellow.copy(alpha = 0.85f)
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$year 年度回忆录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "一帧一画，定格步履与温柔",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "查看年度报告",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsGrid(
    stats: com.silas.omaster.trailsnap.model.DashboardStats?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats?.totalPhotos?.toString() ?: "0",
                label = "照片",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats?.totalVideos?.toString() ?: "0",
                label = "视频",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                value = stats?.locationCount?.toString() ?: "0",
                label = "城市",
                icon = Icons.Default.Map,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
            )
            StatCard(
                value = stats?.peopleCount?.toString() ?: "0",
                label = "人物",
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
            )
        }
    }
}
