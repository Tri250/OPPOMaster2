package com.silas.omaster.trailsnap.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.AnnualReport
import com.silas.omaster.trailsnap.model.CityStat
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow

@Composable
fun AnnualReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val report by repository.annualReport.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TrailSnapTopBar(title = "年度回忆录", onBack = onBack)
        }

        item {
            AnnualHero(report = report)
        }

        item {
            StatGrid(report = report)
        }

        item {
            SectionTitle(title = "城市足迹")
            Spacer(modifier = Modifier.height(8.dp))
            CityRanking(cities = report?.cityStats ?: emptyList())
        }

        item {
            SectionTitle(title = "季节分布")
            Spacer(modifier = Modifier.height(8.dp))
            SeasonDistribution(stats = report?.seasonStats ?: emptyMap())
        }

        item {
            SectionTitle(title = "高光时刻")
            Spacer(modifier = Modifier.height(8.dp))
            HighlightGrid(count = report?.highlightPhotoIds?.size ?: 0)
        }
    }
}

@Composable
private fun AnnualHero(report: AnnualReport?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        HasselbladOrange.copy(alpha = 0.95f),
                        WarningYellow.copy(alpha = 0.85f)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = "${report?.year ?: 2025} 行影集",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "这一年，\n你拍下了 ${report?.totalPhotos ?: 0} 个瞬间",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                report?.farthestCity?.let {
                    HeroChip(text = "最远：$it", icon = Icons.Default.LocationOn)
                }
                report?.favoriteCity?.let {
                    HeroChip(text = "最爱：$it", icon = Icons.Default.Map)
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatGrid(report: AnnualReport?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = "${report?.totalCities ?: 0}",
                label = "点亮城市",
                icon = Icons.Default.LocationOn,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = "${report?.totalScenics ?: 0}",
                label = "探索景点",
                icon = Icons.Default.Terrain,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                value = "${report?.totalTrips ?: 0}",
                label = "出行次数",
                icon = Icons.Default.PhotoCamera,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
            )
            StatCard(
                value = "${report?.totalPhotos ?: 0}",
                label = "年度照片",
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
            )
        }
    }
}

@Composable
private fun CityRanking(cities: List<CityStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cities.take(5).forEachIndexed { index, city ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (index) {
                                0 -> HasselbladOrange
                                1 -> WarningYellow
                                2 -> Color(0xFF42A5F5)
                                else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (index < 3) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = city.city,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    city.province?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
                Text(
                    text = "${city.photoCount} 张",
                    style = MaterialTheme.typography.labelMedium,
                    color = HasselbladOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SeasonDistribution(stats: Map<String, Int>) {
    val total = stats.values.sum().coerceAtLeast(1)
    val seasons = listOf("春" to Color(0xFF66BB6A), "夏" to Color(0xFF42A5F5), "秋" to Color(0xFFFFA726), "冬" to Color(0xFF42A5F5))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        seasons.forEach { (name, color) ->
            val count = stats[name] ?: 0
            val ratio = count.toFloat() / total
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightGrid(count: Int) {
    val cells = (0 until count.coerceIn(3, 9)).toList()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = HasselbladOrange.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
