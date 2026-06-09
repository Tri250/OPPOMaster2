package com.silas.omaster.ui.hasselblad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.ai.model.FilmPreset
import com.silas.omaster.ai.model.FilmSeries
import com.silas.omaster.ui.theme.HasselbladTheme

/**
 * 胶片推荐卡片组件
 * 横向滚动，仿胶片齿孔边框
 *
 * 设计规范：
 * - 横向滚动布局
 * - 胶片齿孔边框致敬胶片文化
 * - 匹配度显示
 */
@Composable
fun HasselbladFilmRecommendationCard(
    films: List<FilmPreset>,
    selectedFilmId: String? = null,
    onFilmSelected: (FilmPreset) -> Unit = {},
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
                text = "🎞️",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "推荐胶片",
                color = HasselbladTheme.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 胶片列表（横向滚动）
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(films) { film ->
                HasselbladFilmCard(
                    film = film,
                    isSelected = film.id == selectedFilmId,
                    onClick = { onFilmSelected(film) }
                )
            }
        }
    }
}

/**
 * 单个胶片卡片
 * 仿胶片齿孔边框设计
 */
@Composable
fun HasselbladFilmCard(
    film: FilmPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seriesColor = getFilmSeriesColor(film.series)

    Card(
        modifier = modifier
            .width(HasselbladTheme.FilmCardWidth.dp)
            .height(HasselbladTheme.FilmCardHeight.dp)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = HasselbladTheme.HasselbladOrange,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = HasselbladTheme.CardBackgroundHighlight
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .drawBehind {
                    // 绘制胶片齿孔边框
                    val strokeWidth = 2.dp.toPx()
                    val holeSize = 4.dp.toPx()
                    val spacing = 12.dp.toPx()

                    // 顶部齿孔
                    var x = spacing
                    while (x < size.width - spacing) {
                        drawCircle(
                            color = seriesColor.copy(alpha = 0.3f),
                            radius = holeSize / 2,
                            center = Offset(x, 4.dp.toPx())
                        )
                        x += spacing
                    }

                    // 底部齿孔
                    x = spacing
                    while (x < size.width - spacing) {
                        drawCircle(
                            color = seriesColor.copy(alpha = 0.3f),
                            radius = holeSize / 2,
                            center = Offset(x, size.height - 4.dp.toPx())
                        )
                        x += spacing
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 胶片名称
            Text(
                text = film.displayName,
                color = if (isSelected) HasselbladTheme.HasselbladOrange else HasselbladTheme.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 胶片系列
            Text(
                text = film.series.displayName,
                color = seriesColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 匹配度
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HasselbladTheme.CardBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(film.matchScore)
                        .clip(RoundedCornerShape(2.dp))
                        .background(seriesColor)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${film.matchPercent}%匹配",
                color = HasselbladTheme.TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 获取胶片系列对应的颜色
 */
@Composable
private fun getFilmSeriesColor(series: FilmSeries): Color {
    return when (series) {
        FilmSeries.CLASSIC -> HasselbladTheme.FilmClassic
        FilmSeries.EMOTION -> HasselbladTheme.FilmEmotion
        FilmSeries.STRUCTURE -> HasselbladTheme.FilmStructure
        FilmSeries.DIGITAL -> HasselbladTheme.FilmDigital
    }
}

/**
 * 简化版胶片推荐（单行显示）
 */
@Composable
fun HasselbladFilmRecommendationSimple(
    films: List<FilmPreset>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(HasselbladTheme.CardBackground)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎞️ 推荐:",
            color = HasselbladTheme.TextSecondary,
            fontSize = 12.sp
        )

        films.take(3).forEach { film ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = HasselbladTheme.CardBackgroundHighlight
            ) {
                Text(
                    text = film.displayName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = getFilmSeriesColor(film.series),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}