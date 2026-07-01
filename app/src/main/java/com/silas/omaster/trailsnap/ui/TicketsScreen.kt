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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.trailsnap.model.TicketType
import com.silas.omaster.trailsnap.model.TravelTicket
import com.silas.omaster.ui.theme.HasselbladOrange
import java.time.format.DateTimeFormatter

@Composable
fun TicketsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { TrailSnapRepository.getInstance(context) }
    val tickets by repository.tickets.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TrailSnapTopBar(title = "行程票据", onBack = onBack)
        if (tickets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "还没有票根记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TicketSummaryHeader(count = tickets.size)
            }
            items(tickets.sortedByDescending { it.departureTime }) { ticket ->
                TicketCard(ticket = ticket)
            }
        }
        }
    }
}

@Composable
private fun TicketSummaryHeader(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HasselbladOrange.copy(alpha = 0.12f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(HasselbladOrange.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ConfirmationNumber,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "已识别 $count 张票据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "火车票、机票、景区门票自动归档",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: TravelTicket) {
    val timeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ticketTypeColor(ticket.type).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ticketTypeIcon(ticket.type),
                        contentDescription = null,
                        tint = ticketTypeColor(ticket.type),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticketTypeLabel(ticket.type),
                        style = MaterialTheme.typography.labelMedium,
                        color = ticketTypeColor(ticket.type),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = ticket.ticketNo ?: "未识别票号",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                if (ticket.isRecognized) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(HasselbladOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "已识别",
                            style = MaterialTheme.typography.labelSmall,
                            color = HasselbladOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = ticket.departure,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = ticket.departureTime?.format(timeFormatter) ?: "未知时间",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(1.dp)
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                )

                if (ticket.arrival.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = ticket.arrival,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = ticket.arrivalTime?.format(timeFormatter) ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            if (ticket.seatInfo != null || ticket.price != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ticket.seatInfo?.let {
                        TicketInfoChip(
                            icon = Icons.Default.Schedule,
                            text = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ticket.price?.let {
                        TicketInfoChip(
                            icon = null,
                            text = it,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketInfoChip(
    icon: ImageVector?,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

private fun ticketTypeIcon(type: TicketType): ImageVector = when (type) {
    TicketType.TRAIN -> Icons.Default.Train
    TicketType.FLIGHT -> Icons.Default.AirplanemodeActive
    TicketType.SCENIC -> Icons.Default.Landscape
    TicketType.CONCERT -> Icons.Default.Movie
    TicketType.HOTEL -> Icons.Default.Hotel
    TicketType.MOVIE -> Icons.Default.Movie
    TicketType.OTHER -> Icons.Default.ConfirmationNumber
}

private fun ticketTypeColor(type: TicketType): Color = when (type) {
    TicketType.TRAIN -> HasselbladOrange
    TicketType.FLIGHT -> Color(0xFF4A90E2)
    TicketType.SCENIC -> Color(0xFF66BB6A)
    TicketType.CONCERT -> Color(0xFFAB47BC)
    TicketType.HOTEL -> Color(0xFF26A69A)
    TicketType.MOVIE -> Color(0xFFEF5350)
    TicketType.OTHER -> Color(0xFF78909C)
}

private fun ticketTypeLabel(type: TicketType): String = when (type) {
    TicketType.TRAIN -> "火车票"
    TicketType.FLIGHT -> "机票"
    TicketType.SCENIC -> "景区门票"
    TicketType.CONCERT -> "演出票"
    TicketType.HOTEL -> "酒店入住"
    TicketType.MOVIE -> "电影票"
    TicketType.OTHER -> "其他票据"
}
