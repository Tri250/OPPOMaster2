package com.silas.omaster.trailsnap.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.silas.omaster.R
import com.silas.omaster.trailsnap.data.TrailSnapRepository
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.WarningYellow
import kotlinx.coroutines.launch

@Composable
fun XingYingJiHomeScreen(
    onNavigateToTimeline: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToFavorites: () -> Unit,
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
    val isLoading by repository.isLoading.collectAsState()
    val loadError by repository.error.collectAsState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // 加载错误时以 Snackbar/Toast 形式反馈给用户
    LaunchedEffect(loadError) {
        loadError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    val requiredPermissions = remember {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    var hasMediaPermission by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasMediaPermission = requiredPermissions.any { results[it] == true }
        showRationale = !hasMediaPermission && (context as? android.app.Activity)?.let { activity ->
            requiredPermissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        } == true
    }

    LaunchedEffect(hasMediaPermission) {
        if (hasMediaPermission) {
            repository.refresh()
        }
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
                title = stringResource(R.string.xingyingji_title),
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            if (!isRefreshing && hasMediaPermission) {
                                isRefreshing = true
                                scope.launch {
                                    repository.refresh()
                                    isRefreshing = false
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh),
                            tint = if (isRefreshing) HasselbladOrange else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            )
        }

        if (!hasMediaPermission) {
            item {
                PermissionCard(
                    onRequest = { permissionLauncher.launch(requiredPermissions) },
                    onOpenSettings = {
                        val intent = Settings.ACTION_APPLICATION_DETAILS_SETTINGS.let {
                            android.content.Intent(it, android.net.Uri.parse("package:${context.packageName}"))
                        }
                        context.startActivity(intent)
                    },
                    showRationale = showRationale
                )
            }
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
            SectionTitle(title = stringResource(R.string.memories_entry))
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.timeline),
                subtitle = stringResource(R.string.timeline_desc),
                icon = Icons.Default.AccessTime,
                onClick = onNavigateToTimeline,
                badge = stats?.totalPhotos?.let { "${it + (stats?.totalVideos ?: 0)}" }
            )
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.albums),
                subtitle = stringResource(R.string.albums_desc),
                icon = Icons.Default.PhotoAlbum,
                onClick = onNavigateToAlbums,
                badge = stats?.totalAlbums?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = "收藏夹",
                subtitle = "你收藏的心动瞬间",
                icon = Icons.Default.Favorite,
                onClick = onNavigateToFavorites,
                badge = stats?.favoriteCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.locations_map),
                subtitle = stringResource(R.string.locations_map_desc),
                icon = Icons.Default.Map,
                onClick = onNavigateToLocations,
                badge = stats?.locationCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.people),
                subtitle = stringResource(R.string.people_desc),
                icon = Icons.Default.Group,
                onClick = onNavigateToPeople,
                badge = stats?.peopleCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.tickets),
                subtitle = stringResource(R.string.tickets_desc),
                icon = Icons.Default.Train,
                onClick = onNavigateToTickets,
                badge = stats?.ticketCount?.toString()
            )
        }

        item {
            QuickEntryCard(
                title = stringResource(R.string.toolbox),
                subtitle = stringResource(R.string.toolbox_desc),
                icon = Icons.Default.Construction,
                onClick = onNavigateToToolbox
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.travel_quote),
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
                            text = stringResource(R.string.annual_report_title, year),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.annual_report_subtitle),
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
                    contentDescription = stringResource(R.string.view_annual_report),
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
                label = stringResource(R.string.stat_photos),
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = stats?.totalVideos?.toString() ?: "0",
                label = stringResource(R.string.stat_videos),
                icon = Icons.Default.Videocam,
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
                label = stringResource(R.string.stat_cities),
                icon = Icons.Default.Map,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
            )
            StatCard(
                value = stats?.peopleCount?.toString() ?: "0",
                label = stringResource(R.string.stat_people),
                icon = Icons.Default.Group,
                modifier = Modifier.weight(1f),
                gradient = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
            )
        }
    }
}

@Composable
private fun PermissionCard(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    showRationale: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.permission_storage_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permission_storage_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (showRationale) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.go_to_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text(stringResource(R.string.request_permission_again))
                }
            } else {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HasselbladOrange)
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}
