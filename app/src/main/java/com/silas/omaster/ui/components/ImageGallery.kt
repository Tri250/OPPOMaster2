package com.silas.omaster.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.minimumInteractiveComponentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.silas.omaster.ui.animation.AnimationSpecs
import com.silas.omaster.ui.theme.NearBlack
import com.silas.omaster.util.ImageCacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 图片画廊组件
 * 支持自动轮播和手动切换，手动切换时暂停自动播放
 *
 * @param images 图片路径列表
 * @param modifier 修饰符
 * @param autoPlayInterval 自动播放间隔（毫秒），默认 3000ms
 * @param showIndicators 是否显示指示器
 * @param showNavigationButtons 是否显示左右切换按钮
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGallery(
    images: List<String>,
    modifier: Modifier = Modifier,
    autoPlayInterval: Long = AnimationSpecs.AutoPlayIntervalMillis,
    showIndicators: Boolean = true,
    showNavigationButtons: Boolean = true
) {
    if (images.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { images.size })

    // 用户是否手动干预过（点击按钮或滑动页面后暂停自动播放）
    var isUserInteracted by remember { mutableStateOf(false) }

    // 监听用户手势滑动：只要处于滚动状态即视为手动干预
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .collect { scrolling ->
                if (scrolling) isUserInteracted = true
            }
    }

    // 自动播放协程：避免使用 mutableState 保存 Job 导致频繁重组，
    // 在独立 LaunchedEffect 中管理生命周期，用户干预或页面数为 1 时自动停止
    LaunchedEffect(pagerState, images.size, isUserInteracted) {
        if (isUserInteracted || images.size <= 1) return@LaunchedEffect

        while (true) {
            delay(autoPlayInterval)
            if (!pagerState.isScrollInProgress && !isUserInteracted) {
                val nextPage = (pagerState.currentPage + 1) % images.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(
                        durationMillis = AnimationSpecs.PageTransitionMillis.toInt(),
                        easing = AnimationSpecs.NormalTween.easing
                    )
                )
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NearBlack
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 图片轮播
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1  // 预加载相邻页面，避免卡顿
            ) { page ->
                // 使用 ImageCacheManager 获取加载路径（优先本地缓存）
                val imagePath = images[page]
                val imageUri = ImageCacheManager.getInstance(context).getImageLoadPath(context, imagePath)

                // 如果是网络图片且未缓存，后台下载
                LaunchedEffect(imagePath) {
                    if (imagePath.startsWith("http") &&
                        !ImageCacheManager.getInstance(context).isImageCached(context, imagePath)) {
                        ImageCacheManager.getInstance(context).downloadAndCacheImage(context, imagePath)
                    }
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "预设图片 ${page + 1}/${images.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 左切换按钮
            if (showNavigationButtons && images.size > 1) {
                GalleryNavigationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    onClick = {
                        isUserInteracted = true
                        scope.launch {
                            val prevPage = if (pagerState.currentPage > 0) {
                                pagerState.currentPage - 1
                            } else {
                                images.size - 1
                            }
                            pagerState.animateScrollToPage(
                                page = prevPage,
                                animationSpec = tween(
                                    durationMillis = AnimationSpecs.PageTransitionMillis.toInt(),
                                    easing = AnimationSpecs.NormalTween.easing
                                )
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                // 右切换按钮
                GalleryNavigationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    onClick = {
                        isUserInteracted = true
                        scope.launch {
                            val nextPage = (pagerState.currentPage + 1) % images.size
                            pagerState.animateScrollToPage(
                                page = nextPage,
                                animationSpec = tween(
                                    durationMillis = AnimationSpecs.PageTransitionMillis.toInt(),
                                    easing = AnimationSpecs.NormalTween.easing
                                )
                            )
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }

            // 指示器
            if (showIndicators && images.size > 1) {
                GalleryIndicators(
                    pageCount = images.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * 画廊导航按钮
 */
@Composable
private fun GalleryNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .size(40.dp)
            // P2-2：保证触控目标 ≥ 48dp，视觉保持 40dp 不变
            .minimumInteractiveComponentSize(),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (icon == Icons.AutoMirrored.Filled.KeyboardArrowLeft) "上一张" else "下一张",
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * 画廊指示器
 */
@Composable
private fun GalleryIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                )
            if (index < pageCount - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}
