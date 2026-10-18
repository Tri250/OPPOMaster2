package com.silas.omaster.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.silas.omaster.R
import com.silas.omaster.model.DeviceProfile
import com.silas.omaster.ui.components.OMasterTopAppBar
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.CardBorderLight
import com.silas.omaster.ui.theme.themedCardBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.ui.theme.themedBorderLight
import com.silas.omaster.util.DeviceDetector
import com.silas.omaster.util.VersionInfo
import com.silas.omaster.util.perform
import com.silas.omaster.util.rememberScrollHaptics

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToSurvey: () -> Unit,
    onScrollStateChanged: (Boolean) -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {},
    onNavigateToOpenSourceLicense: () -> Unit = {},
    currentVersionName: String = VersionInfo.VERSION_NAME
) {
    val scrollState = rememberScrollState()
    var previousScrollValue by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    val isScrollingUp by remember {
        derivedStateOf {
            val currentScroll = scrollState.value
            val isUp = currentScroll <= previousScrollValue
            previousScrollValue = currentScroll
            isUp
        }
    }

    // 滚动到顶/底部震感
    rememberScrollHaptics(scrollState)

    LaunchedEffect(isScrollingUp) {
        onScrollStateChanged(isScrollingUp)
    }

    // v1.6.10 设备档案（机型感知，供头部标识 / 档案磁贴 / 功能介绍共用）
    val deviceProfile = remember { DeviceDetector.getCurrentDevice(context) }
    val isKnownDevice = deviceProfile.brandTag != "unknown" &&
            deviceProfile.brandTag != DeviceProfile.ALL
    val isZhLocale = context.resources.configuration.locales[0].language == "zh"
    val deviceName = if (isZhLocale) {
        deviceProfile.displayNameZh.ifBlank { deviceProfile.model }
    } else {
        deviceProfile.displayNameEn.ifBlank { deviceProfile.model }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OMasterTopAppBar(
            title = stringResource(R.string.nav_about),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        tint = themedTextPrimary()
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(AppDesign.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppTitleSection(currentVersionName, deviceProfile, deviceName, isKnownDevice)

            // v1.6.10 机型档案 Bento 磁贴（Find X10 等 ColorOS 17 已知机型展示）
            if (isKnownDevice) {
                Spacer(modifier = Modifier.height(16.dp))
                DeviceSpecsCard(deviceProfile, deviceName)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 功能入口列表
            ProfileMenuList(
                onNavigateToSubscription = onNavigateToSubscription,
                onNavigateToSurvey = onNavigateToSurvey
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureList(deviceName, isKnownDevice)

            Spacer(modifier = Modifier.height(16.dp))

            CreditsCard(context)

            Spacer(modifier = Modifier.height(16.dp))

            ProjectCard(context)

            Spacer(modifier = Modifier.height(16.dp))

            QQGroupCard(context)

            Spacer(modifier = Modifier.height(24.dp))

            FooterSection(context, onNavigateToPrivacyPolicy, onNavigateToOpenSourceLicense)

            // 底部额外留白，避免内容太靠下
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10 改善（Find X10 / ColorOS 17 对齐）：
// 1) 头部新增应用图标块（ColorOS 桌面图标 20dp 大圆角语言）
// 2) 设备档案标识加机型图标前缀
// 3) 新增机型档案 Bento 磁贴：影像系统 / 屏幕 / ColorOS 版本
// 4) 菜单项副标题 + 全卡片 O-Haptics 触感反馈
// 5) 功能介绍图标块化，机型适配项动态显示实际机型名
// 6) 卡片圆角统一 20dp
// ═══════════════════════════════════════════════════════════

@Composable
private fun AppTitleSection(
    currentVersionName: String,
    deviceProfile: DeviceProfile,
    deviceName: String,
    isKnownDevice: Boolean
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // v1.6.10 应用图标（ColorOS 桌面图标大圆角视觉）
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 应用名称（O 使用主题色）
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("O")
                }
                withStyle(style = SpanStyle(color = themedTextPrimary())) {
                    append("Master")
                }
            },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 副标题
        Text(
            text = stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.bodyLarge,
            color = themedTextSecondary().copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 版本号标签 + 更新日志入口
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "v$currentVersionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = stringResource(R.string.about_view_changelog),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    haptic.perform(HapticFeedbackType.Confirm)
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/iCurrer/OMaster/releases")
                    )
                    context.startActivity(intent)
                }
            )
        }

        // 设备档案标识（仅已知机型显示，如 Find X10 · 哈苏 2亿像素 · ColorOS 17）
        if (isKnownDevice) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = buildString {
                            append(deviceName)
                            if (deviceProfile.colorOsVersion > 0) {
                                append(" · ")
                                append(context.getString(R.string.about_device_coloros, deviceProfile.colorOsVersion))
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// v1.6.10：机型档案 Bento 磁贴（Find X10 / ColorOS 17 专属）
// 影像系统（哈苏/像素/光圈）+ 屏幕（高刷）+ ColorOS 版本
// ═══════════════════════════════════════════════════════════

@Composable
private fun DeviceSpecsCard(profile: DeviceProfile, deviceName: String) {
    val context = LocalContext.current

    // 影像系统数据行
    val imagingValue = buildList {
        if (profile.isProImaging) add(context.getString(R.string.about_specs_hasselblad))
        if (profile.totalPixelsMp > 0) add("${profile.totalPixelsMp}MP")
        if (profile.maxAperture.isNotBlank()) add(profile.maxAperture)
    }.joinToString(" · ").ifBlank { profile.brand }

    val displayValue = "${profile.maxRefreshRateHz}Hz"
    val systemValue = if (profile.colorOsVersion > 0) {
        context.getString(R.string.about_device_coloros, profile.colorOsVersion)
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = themedCardBackground()),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 标题行：机型图标 + 机型名
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.about_specs_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 磁贴行：影像系统 + 屏幕
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecTile(
                    icon = Icons.Default.CameraAlt,
                    label = stringResource(R.string.about_specs_imaging),
                    value = imagingValue,
                    gradient = listOf(Color(0xFFFF7E5F), Color(0xFFFEB47B)),
                    modifier = Modifier.weight(1f)
                )
                SpecTile(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.about_specs_display),
                    value = displayValue,
                    gradient = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                    modifier = Modifier.weight(1f)
                )
            }

            // 系统磁贴（ColorOS / OxygenOS / RealmeUI 版本）
            if (systemValue != null) {
                SpecTile(
                    icon = Icons.Default.PhoneAndroid,
                    label = stringResource(R.string.about_specs_system),
                    value = systemValue,
                    gradient = listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SpecTile(
    icon: ImageVector,
    label: String,
    value: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(themedTextPrimary().copy(alpha = 0.04f))
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = themedTextSecondary().copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = themedTextPrimary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FeatureList(deviceName: String, isKnownDevice: Boolean) {
    val features = listOf(
        FeatureItem(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.feature_custom_title),
            description = stringResource(R.string.feature_custom_desc)
        ),
        FeatureItem(
            icon = Icons.Default.Cloud,
            title = stringResource(R.string.feature_cloud_title),
            description = stringResource(R.string.feature_cloud_desc)
        ),
        FeatureItem(
            icon = Icons.Default.DashboardCustomize,
            title = stringResource(R.string.feature_update_title),
            description = stringResource(R.string.feature_update_desc)
        ),
        // v1.6.10：安全可靠 + 机型适配（已知机型时动态显示实际机型名）
        FeatureItem(
            icon = Icons.Default.Favorite,
            title = stringResource(R.string.feature_safe_title),
            description = stringResource(R.string.feature_safe_desc)
        ),
        FeatureItem(
            icon = Icons.Default.CameraAlt,
            title = stringResource(R.string.feature_device_title),
            description = if (isKnownDevice) {
                stringResource(R.string.about_feature_device_known, deviceName)
            } else {
                stringResource(R.string.feature_device_desc)
            }
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDesign.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDesign.IconButtonSize - 16.dp)
                )
                Text(
                    text = stringResource(R.string.feature_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
            }

            features.forEach { feature ->
                FeatureListItem(feature)
            }
        }
    }
}

// v1.6.10：功能项图标改 Bento 圆角块，视觉层次更清晰
@Composable
private fun FeatureListItem(feature: FeatureItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDesign.ItemSpacing / 2),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = themedTextPrimary()
            )
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary().copy(alpha = AppDesign.SecondaryAlpha),
                textAlign = TextAlign.Start
            )
        }
    }
}

private data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private data class Contributor(
    val name: String,
    val url: String
)

@Composable
private fun CreditsCard(context: android.content.Context) {
    val haptic = LocalHapticFeedback.current
    val contributors = listOf(
        Contributor("@OPPO影像", "https://xhslink.com/m/8c2gJYGlCTR"),
        Contributor("@蘭州白鴿", "https://xhslink.com/m/4h5lx4Lg37n"),
        Contributor("@派瑞特凯", "https://xhslink.com/m/AkrgUI0kgg1"),
        Contributor("@ONESTEP™", "https://xhslink.com/m/4LZ8zRdNCSv"),
        Contributor("@盒子叔", "https://xhslink.com/m/4mje9mimNXJ"),
        Contributor("@Aurora", "https://xhslink.com/m/2Ebow4iyVOE"),
        Contributor("@屋顶橙子味", "https://v.douyin.com/YkVXPX9kZgY/"),
        Contributor("@尼克lin", "https://xhslink.com/m/AjhYASTkUwq"),
        Contributor("@波子Booz", "https://xhslink.com/m/9leyjjz68Et")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CardBorderLight,
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.credits_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = themedTextPrimary()
                )
            }

            // 开发者区域
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.developer),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f)
                )

                val developers = listOf(
                    "Silas" to "https://xhslink.com/m/2gh56F1blnO",
                    "Luminary" to "https://github.com/fengyec2"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    developers.forEach { (name, url) ->
                        DeveloperChip(name = name, url = url, context = context, haptic = haptic)
                    }
                }
            }

            // 素材提供区域
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.material_provider),
                    style = MaterialTheme.typography.bodySmall,
                    color = themedTextSecondary().copy(alpha = 0.6f)
                )

                // 贡献者标签云
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    contributors.forEach { contributor ->
                        ContributorChip(
                            name = contributor.name,
                            url = contributor.url,
                            context = context,
                            haptic = haptic
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContributorChip(
    name: String,
    url: String,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(themedTextPrimary().copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = themedTextPrimary().copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                haptic.perform(HapticFeedbackType.Confirm)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = themedTextPrimary().copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun DeveloperChip(
    name: String,
    url: String,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable {
                haptic.perform(HapticFeedbackType.Confirm)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProjectCard(context: android.content.Context) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            haptic.perform(HapticFeedbackType.Confirm)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Tri250/OPPOMaster2"))
            context.startActivity(intent)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.about_project_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themedTextPrimary()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GitHub - Tri250/OPPOMaster2",
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary().copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun QQGroupCard(context: android.content.Context) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = CardBorderLight,
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        ),
        shape = RoundedCornerShape(20.dp),
        onClick = {
            haptic.perform(HapticFeedbackType.Confirm)
            // 群号
            val groupId = "1083543279"

            // 尝试使用URL Scheme打开QQ群
            val schemes = listOf(
                // 方式1：使用腾讯官方API（推荐）
                "mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$groupId&card_type=group&source=qrcode",
                // 方式2：使用QQ群加群链接（兼容旧版）
                "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fc%2Fcgi-bin%2Fqm%2Fqr%3Fk%3D$groupId",
                // 方式3：使用简化版scheme
                "mqq://card/show_pslcard?src_type=internal&version=1&uin=$groupId&card_type=group"
            )

            var opened = false
            for (scheme in schemes) {
                if (!opened) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(scheme))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        // 检查是否有应用可以处理这个intent
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                            opened = true
                            break
                        }
                    } catch (e: Exception) {
                        // 尝试下一个方式
                    }
                }
            }

            // 如果所有scheme都失败，使用网页版链接（浏览器会提示打开QQ）
            if (!opened) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://qm.qq.com/cgi-bin/qm/qr?k=$groupId&jump_from=webapi"))
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                } catch (e: Exception) {
                    // 如果浏览器也打不开，提示用户
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.about_qq_failed),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QQ",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.about_qq_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = themedTextPrimary()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.about_qq_number, "1083543279"),
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary().copy(alpha = 0.6f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// v1.6.10：菜单项升级 — 副标题描述 + 触感反馈
@Composable
private fun ProfileMenuList(
    onNavigateToSubscription: () -> Unit,
    onNavigateToSurvey: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = themedBorderLight(),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = themedCardBackground()
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.sub_title),
                subtitle = stringResource(R.string.about_sub_desc),
                onClick = onNavigateToSubscription
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = themedTextSecondary().copy(alpha = 0.1f)
            )
            ProfileMenuItem(
                icon = Icons.Default.CloudUpload,
                title = stringResource(R.string.survey_title),
                subtitle = stringResource(R.string.survey_desc),
                onClick = onNavigateToSurvey
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(0.dp),
        onClick = {
            haptic.perform(HapticFeedbackType.Confirm)
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = themedTextPrimary()
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = themedTextSecondary().copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = themedTextSecondary().copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun FooterSection(
    context: android.content.Context,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToOpenSourceLicense: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "© 2026 OMaster",
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary().copy(alpha = 0.6f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.user_agreement),
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary().copy(alpha = 0.7f),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ocnquih40x3i.feishu.cn/docx/WHVldUhumozJAUx7ZFhcO9uznaf?from=from_copylink"))
                    context.startActivity(intent)
                }
            )

            androidx.compose.material3.Divider(
                modifier = Modifier
                    .height(12.dp)
                    .width(1.dp),
                color = themedTextSecondary().copy(alpha = 0.2f)
            )

            Text(
                text = stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary().copy(alpha = 0.7f),
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ocnquih40x3i.feishu.cn/docx/NSgednMU0oeq9RxnGmcc9vRenvd?from=from_copylink"))
                    context.startActivity(intent)
                }
            )

            androidx.compose.material3.Divider(
                modifier = Modifier
                    .height(12.dp)
                    .width(1.dp),
                color = themedTextSecondary().copy(alpha = 0.2f)
            )

            Text(
                text = stringResource(R.string.open_source_license),
                style = MaterialTheme.typography.bodySmall,
                color = themedTextSecondary().copy(alpha = 0.7f),
                modifier = Modifier.clickable {
                    onNavigateToOpenSourceLicense()
                }
            )
        }

        Text(
            text = "豫 ICP 备 2026011707 号 -1A",
            style = MaterialTheme.typography.bodySmall,
            color = themedTextSecondary().copy(alpha = 0.5f),
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://beian.miit.gov.cn"))
                context.startActivity(intent)
            }
        )
    }
}
