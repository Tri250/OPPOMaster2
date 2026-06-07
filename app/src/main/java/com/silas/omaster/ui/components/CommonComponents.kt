package com.silas.omaster.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.silas.omaster.R
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.theme.BorderDefault
import com.silas.omaster.ui.theme.BorderLight
import com.silas.omaster.ui.theme.ButtonTextStyle
import com.silas.omaster.ui.theme.CardTitleStyle
import com.silas.omaster.ui.theme.GradientOrangeEnd
import com.silas.omaster.ui.theme.GradientOrangeStart
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.ui.theme.ParameterLabelStyle
import com.silas.omaster.ui.theme.ParameterValueStyle
import com.silas.omaster.ui.theme.TagTextStyle
import com.silas.omaster.ui.theme.TextPrimary
import com.silas.omaster.ui.theme.TextSecondary
import com.silas.omaster.ui.theme.TextTertiary
import com.silas.omaster.ui.theme.Zinc700
import com.silas.omaster.ui.theme.Zinc800
import com.silas.omaster.ui.theme.Zinc900
import com.silas.omaster.util.DownloadResult
import com.silas.omaster.util.ImageCacheManager
import com.silas.omaster.util.ImageDownloadCallback
import java.io.File

// ============================================
// 圆角规范
// ============================================
private val CardCornerRadius = 12.dp
private val ButtonCornerRadius = 12.dp
private val BadgeCornerRadius = 6.dp

// ============================================
// 间距规范
// ============================================
private val ComponentSpacing = 16.dp
private val SmallSpacing = 8.dp
private val TinySpacing = 4.dp

/**
 * OMaster 通用顶部导航栏组件
 *
 * 设计规范:
 * - 背景: PureBlack
 * - 标题: 加粗白色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OMasterTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = HasselbladOrange
                    )
                }
            }
        },
        navigationIcon = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = TextPrimary
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Zinc900,
            titleContentColor = TextPrimary
        ),
        modifier = modifier
    )
}

/**
 * 功能特性卡片组件
 *
 * 设计规范:
 * - 图标: 渐变橙色背景 + 12dp 圆角
 * - 标题: 加粗白色
 * - 描述: Zinc400
 * - 标签: Zinc700 半透明背景
 */
@Composable
fun FeatureCard(
    icon: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    iconSize: TextUnit = 32.sp
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BorderLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器 - 渐变橙色背景
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GradientOrangeStart,
                                GradientOrangeEnd
                            )
                        ),
                        shape = RoundedCornerShape(ButtonCornerRadius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    fontSize = iconSize
                )
            }

            Spacer(modifier = Modifier.width(ComponentSpacing))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = CardTitleStyle,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(TinySpacing))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

/**
 * 参数显示项组件
 */
@Composable
fun ParameterItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = ParameterLabelStyle
        )
        Spacer(modifier = Modifier.height(TinySpacing))
        Text(
            text = value,
            style = ParameterValueStyle
        )
    }
}

/**
 * OMaster 通用卡片组件
 *
 * 设计规范:
 * - 圆角: 12dp
 * - 背景: Zinc800
 * - 边框: 1px Zinc700 半透明
 */
@Composable
fun OMasterCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BorderLight
        ),
        content = content
    )
}

/**
 * 垂直间距组件
 */
@Composable
fun VerticalSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))

/**
 * 水平间距组件
 */
@Composable
fun HorizontalSpacer(width: Dp) = Spacer(modifier = Modifier.width(width))

/**
 * 模式标签组件
 *
 * 设计规范:
 * - 支持显示多个标签
 * - 圆角: 6dp
 * - 背景: Zinc800
 * - 边框: 0.5px Zinc700 半透明
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModeBadge(
    tags: List<String>?,
    modifier: Modifier = Modifier
) {
    if (tags.isNullOrEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            TagItem(text = tag)
        }
    }
}

/**
 * 单个标签组件
 */
@Composable
private fun TagItem(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(Zinc800)
            .border(
                width = 0.5.dp,
                color = BorderDefault.copy(alpha = 0.3f),
                shape = RoundedCornerShape(BadgeCornerRadius)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = TagTextStyle,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 预设图片组件
 *
 * 设计规范:
 * - 支持 assets、内部存储和网络图片（带本地缓存）
 * - 优化: 使用更短的 crossfade 动画时长，优先加载本地缓存，带下载状态
 */
@Composable
fun PresetImage(
    preset: MasterPreset,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showDownloadIndicator: Boolean = true
) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }

    // 使用 ImageCacheManager 获取加载路径（优先本地缓存）
    val imageUri = ImageCacheManager.getImageLoadPath(context, preset.coverPath)

    // 如果是网络图片且未缓存，后台下载
    LaunchedEffect(preset.coverPath) {
        if (preset.coverPath.startsWith("http") &&
            !ImageCacheManager.isImageCached(context, preset.coverPath)
        ) {

            downloadState = DownloadState.Downloading

            val result = ImageCacheManager.downloadAndCacheImage(
                context, preset.coverPath,
                callback = object : ImageDownloadCallback {
                    override fun onStart(url: String) {}
                    override fun onProgress(url: String, bytesDownloaded: Long, totalBytes: Long) {}
                    override fun onSuccess(url: String, file: File) {
                        downloadState = DownloadState.Success
                    }
                    override fun onError(url: String, error: Throwable, retryCount: Int) {
                        downloadState = DownloadState.Error(error.message ?: "下载失败")
                    }
                    override fun onRetry(url: String, attempt: Int) {}
                }
            )

            downloadState = when (result) {
                is DownloadResult.Success -> DownloadState.Success
                is DownloadResult.Error -> DownloadState.Error("下载失败")
            }
        }
    }

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUri)
                .crossfade(AnimationSpecs.FastTween.durationMillis)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = preset.name,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        // 显示加载状态
        if (showDownloadIndicator && downloadState is DownloadState.Downloading) {
            LoadingIndicator()
        }
    }
}

/**
 * 简单加载指示器 - Material 3 风格
 *
 * 设计规范:
 * - 使用品牌橙色
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = HasselbladOrange,
            strokeWidth = 3.dp
        )
    }
}

/**
 * 下载状态
 */
private sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

/**
 * 章节标题组件
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    color: Color = HasselbladOrange
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

/**
 * 带卡片的参数项组件
 *
 * 设计规范:
 * - 圆角: 12dp
 * - 背景: Zinc800
 * - 边框: 0.5px Zinc700 半透明
 */
@Composable
fun ParameterCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = BorderLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentSpacing),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = ParameterLabelStyle
            )
            Spacer(modifier = Modifier.height(TinySpacing))
            Text(
                text = value,
                style = ParameterValueStyle
            )
        }
    }
}

/**
 * 拍摄建议卡片组件
 */
@Composable
fun ShootingTipsCard(
    tips: String,
    modifier: Modifier = Modifier
) {
    DescriptionCard(
        title = stringResource(R.string.shooting_tips),
        content = tips,
        modifier = modifier
    )
}

/**
 * 通用描述卡片组件
 *
 * 设计规范:
 * - 圆角: 12dp
 * - 背景: Zinc800 半透明
 * - 图标: 品牌橙色
 */
@Composable
fun DescriptionCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Zinc800.copy(alpha = 0.8f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BorderLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentSpacing)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = HasselbladOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(SmallSpacing))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HasselbladOrange
                )
            }

            Spacer(modifier = Modifier.height(SmallSpacing))

            // 内容
            content.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    Text(
                        text = line.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.9f),
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(TinySpacing))
                }
            }
        }
    }
}

/**
 * 主按钮组件
 *
 * 设计规范:
 * - 渐变背景: 橙色渐变
 * - 圆角: 12dp
 * - 文字: 白色加粗
 * - 悬停效果: 缩放 1.05x
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(ButtonCornerRadius))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GradientOrangeStart,
                        GradientOrangeEnd
                    )
                ),
                shape = RoundedCornerShape(ButtonCornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.let { it() }
            Text(
                text = text,
                style = ButtonTextStyle,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 次要按钮组件
 *
 * 设计规范:
 * - 背景: Zinc800
 * - 边框: 1px Zinc700
 * - 圆角: 12dp
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "buttonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(ButtonCornerRadius))
            .background(Zinc800)
            .border(
                width = 1.dp,
                color = BorderDefault,
                shape = RoundedCornerShape(ButtonCornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = ButtonTextStyle,
            color = TextPrimary
        )
    }
}
