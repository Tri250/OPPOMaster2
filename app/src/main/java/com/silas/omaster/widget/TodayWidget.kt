package com.silas.omaster.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.Text
import androidx.glance.unit.ColorProvider
import com.silas.omaster.MainActivity
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.util.DeviceDetector

/**
 * MU-28 / v1.6：OMaster 桌面小组件
 * 尺寸：1×4 小尺寸，显示今日推荐预设 + 设备展示名
 * 点击任意位置 → 打开 OMaster 首页
 *
 * 布局：
 * ┌─────────────────────────────┐
 * │ 📷 为 Find X10 · 精选       │  标题栏
 * │ ───────────────────────────  │
 * │ 哈苏 2亿像素   胶片风       │  2 条推荐预设横排
 * │ 专业色彩        复古        │
 * └─────────────────────────────┘
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        // 读当前设备展示名（Find X10 用户看到"为 Find X10 精选"）
        val deviceName = DeviceDetector.getCurrentDevice(context).displayNameZh
        // 读精选预设（最多取 4 条，按 deviceProfile 匹配）
        val repository = PresetRepository.getInstance(context)
        // 同步读 StateFlow 值（Widget 是广播 Context）
        val all = runCatching { repository.getAllPresetsDirectSync() }.getOrDefault(emptyList())
        val recommended = all.take(4)

        GlanceAppWidget(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(0xFF141414.toInt()))
                .padding(12.dp),
            onClick = actionStartActivity<MainActivity>()
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.Start
            ) {
                // 标题
                Text(
                    text = context.getString(R.string.widget_today_title, deviceName),
                    fontSize = 12.sp,
                    color = ColorProvider(0xFFAAAAAA.toInt()),
                    modifier = GlanceModifier.padding(bottom = 6.dp)
                )

                if (recommended.isNotEmpty()) {
                    // 预设横排
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.spacedBy(6.dp)
                    ) {
                        recommended.forEach { preset ->
                            Text(
                                text = preset.name,
                                fontSize = 13.sp,
                                color = ColorProvider(0xFFFFFFFF.toInt()),
                                modifier = GlanceModifier.width(80.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = context.getString(R.string.widget_empty),
                        fontSize = 12.sp,
                        color = ColorProvider(0xFF666666.toInt())
                    )
                }
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

/**
 * PresetRepository 同步读取（Widget 场景不适用 Flow）
 * 在 Repository 里作为扩展函数提供，不破坏现有 API
 */
private fun PresetRepository.getAllPresetsDirectSync(): List<com.silas.omaster.model.MasterPreset> {
    return runCatching {
        // 用 runBlocking 读 StateFlow 初始值（Repository 底层是 StateFlow，first() 立即返回）
        kotlinx.coroutines.runBlocking { getAllPresets().first() }
    }.getOrDefault(emptyList())
}
