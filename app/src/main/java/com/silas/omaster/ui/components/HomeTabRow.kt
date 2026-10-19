package com.silas.omaster.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.silas.omaster.ui.animation.FluidComponentDp
import com.silas.omaster.ui.theme.AppDesign
import com.silas.omaster.ui.theme.themedBackground
import com.silas.omaster.ui.theme.themedTextPrimary
import com.silas.omaster.ui.theme.themedTextSecondary
import com.silas.omaster.util.perform

/**
 * 主页 Tab 栏 —— 【v1.6.14】ColorOS 17 流体胶囊
 *
 * 变更（相对 v1.6.13）：
 * 1. 选中态由"整格填充背景"改为"底部流体胶囊指示器"，切换时以 L2 组件弹簧
 *    弹性滑动到目标 Tab（与底部 PillNavBar 胶囊语言统一，消除两者手感差异）；
 * 2. 计数由静态文本改为 NumberRollText 灵动数字滚动 —— 下拉刷新、收藏取消后
 *    数量变化可被"看见"（ColorOS 17 灵动数字风格）；
 * 3. 新增长按回调（ColorOS 17 长按交互范式），供主页弹出批量管理 / 快捷创建菜单。
 *
 * @param onTabLongPress 长按某个 Tab 的回调；不需要长按菜单时传 null（默认）
 */
@Composable
fun HomeTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    tabs: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    onTabLongPress: ((Int) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(themedBackground())
            .padding(horizontal = AppDesign.TabBarPadding, vertical = AppDesign.ItemSpacing)
    ) {
        // weight(1f) 等分后的单格宽度
        val itemWidth = maxWidth / tabs.size.coerceAtLeast(1)
        // 胶囊取单格 60% 宽，水平居中于选中项
        val capsuleWidth = itemWidth * 0.6f
        val capsuleOffset by animateDpAsState(
            targetValue = itemWidth * selectedTab + (itemWidth - capsuleWidth) / 2,
            animationSpec = FluidComponentDp,
            label = "homeTabCapsule"
        )

        // 流体胶囊指示器
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = capsuleOffset, y = (-2).dp)
                .width(capsuleWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDesign.ItemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, (title, count) ->
                val isSelected = selectedTab == index
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDesign.TabBarHeight)
                        .clip(AppDesign.PillShape)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptic.perform(HapticFeedbackType.ToggleOn)
                                onTabSelected(index)
                            },
                            onLongClick = {
                                if (onTabLongPress != null) {
                                    haptic.perform(HapticFeedbackType.LongPress)
                                    onTabLongPress(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) themedTextPrimary()
                            else themedTextSecondary().copy(alpha = AppDesign.TertiaryAlpha),
                            fontSize = AppDesign.TabTextSize,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (count > 0) {
                            Text(text = " ", fontSize = AppDesign.TabTextSize)
                            // 【v1.6.14】灵动数字滚动
                            NumberRollText(
                                value = count,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else themedTextSecondary().copy(alpha = AppDesign.TertiaryAlpha),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
