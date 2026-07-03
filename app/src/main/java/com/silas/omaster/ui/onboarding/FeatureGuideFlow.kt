package com.silas.omaster.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silas.omaster.R
import com.silas.omaster.ui.theme.HasselbladOrange
import com.silas.omaster.infrastructure.utils.perform
import kotlinx.coroutines.launch

/**
 * 功能引导页数据
 */
data class FeatureGuidePage(
    val icon: ImageVector,
    val titleResId: Int,
    val descriptionResId: Int,
    val features: List<Int> = emptyList() // 功能点列表的资源ID
)

/**
 * 功能介绍引导流程
 * 
 * WelcomeFlow完成后显示，展示OMaster核心功能介绍
 * 共5页，使用Pager动画切换
 * 
 * 页面内容：
 * 1. OMaster是什么（相机调参助手）
 * 2. 核心功能（哈苏大师模式、AI场景识别）
 * 3. 预设管理（导入、导出、订阅）
 * 4. TrailSnap影像管理
 * 5. 个性化设置（主题、品牌色）
 */
@Composable
fun FeatureGuideFlow(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    
    val pages = listOf(
        // 页面1: OMaster是什么
        FeatureGuidePage(
            icon = Icons.Default.CameraAlt,
            titleResId = R.string.feature_guide_page1_title,
            descriptionResId = R.string.feature_guide_page1_desc,
            features = listOf(
                R.string.feature_guide_page1_feature1,
                R.string.feature_guide_page1_feature2,
                R.string.feature_guide_page1_feature3
            )
        ),
        // 页面2: 核心功能
        FeatureGuidePage(
            icon = Icons.Default.AutoFixHigh,
            titleResId = R.string.feature_guide_page2_title,
            descriptionResId = R.string.feature_guide_page2_desc,
            features = listOf(
                R.string.feature_guide_page2_feature1,
                R.string.feature_guide_page2_feature2,
                R.string.feature_guide_page2_feature3
            )
        ),
        // 页面3: 预设管理
        FeatureGuidePage(
            icon = Icons.Default.CloudUpload,
            titleResId = R.string.feature_guide_page3_title,
            descriptionResId = R.string.feature_guide_page3_desc,
            features = listOf(
                R.string.feature_guide_page3_feature1,
                R.string.feature_guide_page3_feature2,
                R.string.feature_guide_page3_feature3
            )
        ),
        // 页面4: TrailSnap影像管理
        FeatureGuidePage(
            icon = Icons.Default.Collections,
            titleResId = R.string.feature_guide_page4_title,
            descriptionResId = R.string.feature_guide_page4_desc,
            features = listOf(
                R.string.feature_guide_page4_feature1,
                R.string.feature_guide_page4_feature2,
                R.string.feature_guide_page4_feature3
            )
        ),
        // 页面5: 个性化设置
        FeatureGuidePage(
            icon = Icons.Default.Palette,
            titleResId = R.string.feature_guide_page5_title,
            descriptionResId = R.string.feature_guide_page5_desc,
            features = listOf(
                R.string.feature_guide_page5_feature1,
                R.string.feature_guide_page5_feature2,
                R.string.feature_guide_page5_feature3
            )
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 跳过按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.feature_guide_skip),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // 页面内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            FeatureGuidePageContent(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // 底部指示器 + 按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 页面指示器
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 24.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_width"
                    )
                    val color by animateColorAsState(
                        targetValue = if (isSelected) HasselbladOrange else Color.Gray.copy(alpha = 0.3f),
                        animationSpec = tween(300),
                        label = "indicator_color"
                    )
                    Box(
                        modifier = Modifier
                            .size(width = width, height = 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 开始/下一步按钮
            Button(
                onClick = {
                    haptic.perform(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HasselbladOrange
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) 
                        stringResource(R.string.feature_guide_next) 
                    else 
                        stringResource(R.string.feature_guide_start),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * 功能引导页内容
 */
@Composable
private fun FeatureGuidePageContent(
    page: FeatureGuidePage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HasselbladOrange.copy(alpha = 0.2f),
                            HasselbladOrange.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = HasselbladOrange
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 标题
        Text(
            text = stringResource(page.titleResId),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述
        Text(
            text = stringResource(page.descriptionResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        // 功能点列表
        if (page.features.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                page.features.forEach { featureResId ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 功能点指示器
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(HasselbladOrange.copy(alpha = 0.6f))
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = stringResource(featureResId),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}