package com.silas.omaster.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 上下文帮助系统
 *
 * 提供三种帮助组件：
 * - HelpBadge: 小型 ? 图标，点击触发工具提示
 * - HelpTooltip: 可锚定到目标元素的工具提示弹窗
 * - HelpGuide: 分步引导覆盖层，适合首次用户体验
 *
 * 已关闭的帮助提示会保存到 SharedPreferences，不再重复显示。
 * 支持 TalkBack 无障碍访问。
 */
object ContextualHelpPrefs {
    private const val PREFS_NAME = "contextual_help_prefs"
    private const val KEY_DISMISSED = "dismissed_help_items"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDismissed(context: Context, helpId: String): Boolean {
        val dismissed = getPrefs(context).getStringSet(KEY_DISMISSED, emptySet()) ?: emptySet()
        return dismissed.contains(helpId)
    }

    fun markDismissed(context: Context, helpId: String) {
        val prefs = getPrefs(context)
        val dismissed = (prefs.getStringSet(KEY_DISMISSED, emptySet()) ?: emptySet()).toMutableSet()
        dismissed.add(helpId)
        prefs.edit().putStringSet(KEY_DISMISSED, dismissed).apply()
    }

    fun reset(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}

/**
 * 帮助徽章 - 小型 ? 图标
 *
 * @param helpId 唯一标识，用于"不再显示"持久化
 * @param contentDescription 无障碍描述
 * @param onClick 点击回调
 */
@Composable
fun HelpBadge(
    helpId: String,
    contentDescription: String = "帮助",
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showBadge by remember { mutableStateOf(!ContextualHelpPrefs.isDismissed(context, helpId)) }

    if (!showBadge) return

    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .clickable { onClick() }
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * 帮助工具提示 - 锚定到目标元素的弹窗
 *
 * @param helpId 唯一标识
 * @param targetPosition 目标元素在屏幕上的位置（通过 onGloballyPositioned 获取）
 * @param title 提示标题
 * @param message 提示内容
 * @param onDismiss 关闭回调
 * @param show 是否显示
 */
@Composable
fun HelpTooltip(
    helpId: String,
    title: String,
    message: String,
    show: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!show) return

    AnimatedVisibility(
        visible = show,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = modifier
                    .width(280.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            ContextualHelpPrefs.markDismissed(context, helpId)
                            onDismiss()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("不再显示")
                    }
                }
            }
        }
    }
}

/**
 * 帮助引导 - 分步引导覆盖层
 *
 * 首次使用时的分步引导，展示关键功能入口。
 *
 * @param helpId 唯一标识
 * @param steps 引导步骤列表
 * @param onComplete 引导完成回调
 * @param show 是否显示引导
 */
@Composable
fun HelpGuide(
    helpId: String,
    steps: List<HelpGuideStep>,
    show: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (!show || ContextualHelpPrefs.isDismissed(context, helpId)) {
        LaunchedEffect(Unit) { onComplete() }
        return
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(currentStep) ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = false) { /* 阻止点击穿透 */ }
            .semantics {
                contentDescription = "引导步骤 ${currentStep + 1}/${steps.size}: ${step.title}"
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(300.dp)
                .padding(16.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 步骤指示器
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == currentStep) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStep) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 图标
                if (step.icon != null) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 标题
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 描述
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            ContextualHelpPrefs.markDismissed(context, helpId)
                            onComplete()
                        }
                    ) {
                        Text("跳过")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentStep + 1}/${steps.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentStep < steps.size - 1) {
                                    currentStep++
                                } else {
                                    ContextualHelpPrefs.markDismissed(context, helpId)
                                    onComplete()
                                }
                            }
                        ) {
                            Text(
                                if (currentStep < steps.size - 1) "下一步" else "完成"
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 引导步骤数据类
 */
data class HelpGuideStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null
)