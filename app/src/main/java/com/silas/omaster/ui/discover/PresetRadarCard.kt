package com.silas.omaster.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.config.SubscriptionConfig
import com.silas.omaster.model.Subscription
import com.silas.omaster.ui.animation.fluidPressScale
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary

/**
 * 【v1.6.14 发现 2.0】订阅更新雷达卡
 * 聚合已启用订阅源自上次查看后的新增预设数，点击展开来源明细。
 * 让 v1.6.13 预置的 vivo / HONOR 等社区源在发现页保持"活"的更新感知。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetRadarCard(
    config: ConfigCenter,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subscriptions by config.subscriptionsFlow.collectAsState()
    val unseenCounts by config.unseenCountsFlow.collectAsState()

    // 仅统计启用的源；总数为 0 时整卡隐藏（保持发现页零噪音）
    val activeSubs = subscriptions.filter { it.isEnabled }
    val radarEntries = activeSubs.mapNotNull { sub ->
        val unseen = unseenCounts[sub.url] ?: 0
        if (unseen > 0) sub to unseen else null
    }
    if (radarEntries.isEmpty()) return
    val totalUnseen = radarEntries.sumOf { it.second }

    var showDetailSheet by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fluidPressScale(interactionSource)
            .border(1.dp, themedBorderLight(), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(16.dp),
        onClick = { showDetailSheet = true },
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Radar, null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.radar_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.radar_card_summary, radarEntries.size, totalUnseen),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary(),
                    maxLines = 1
                )
            }

            // 未读徽标
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (totalUnseen > 99) "99+" else totalUnseen.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDetailSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            sheetState = sheetState
        ) {
            RadarDetailSheet(
                config = config,
                entries = radarEntries,
                onManage = {
                    showDetailSheet = false
                    onNavigateToSubscription()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** 雷达明细：每源新增数列表 + 全部已读 + 管理订阅入口 */
@Composable
private fun RadarDetailSheet(
    config: ConfigCenter,
    entries: List<Pair<Subscription, Int>>,
    onManage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.radar_detail_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.radar_detail_desc),
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary()
        )
        Spacer(modifier = Modifier.height(14.dp))

        entries.forEach { (sub, unseen) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sub.name.ifEmpty { stringResource(R.string.radar_unnamed_source) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = themedTextPrimary(),
                        maxLines = 1
                    )
                    Text(
                        text = stringResource(R.string.radar_source_total, sub.presetCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = themedTextSecondary()
                    )
                }
                Text(
                    text = stringResource(R.string.radar_new_count, unseen),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 标记全部已读
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                .clickable {
                    entries.forEach { config.markSubscriptionSeen(it.first.url) }
                }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CloudDownload, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.radar_mark_all_seen),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 管理订阅
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, themedBorderLight(), RoundedCornerShape(12.dp))
                .clickable(onClick = onManage)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.radar_manage_subscriptions),
                style = MaterialTheme.typography.bodyMedium,
                color = themedTextPrimary(),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.radar_go),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
