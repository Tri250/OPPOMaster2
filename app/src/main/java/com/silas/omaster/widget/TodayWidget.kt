package com.silas.omaster.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.provideGlance
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
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.DeviceDetector
import kotlinx.coroutines.runBlocking

/**
 * MU-28 / v1.6：OMaster 桌面小组件（AndroidX Glance API）
 *
 * Glance Widget 注意事项：
 * - 不能用 androidx.compose.material3.Text（要用 androidx.glance.material3.Text）
 * - 不能用 Row(spacing = ...)（spacing 是 Compose Row 的参数，Glance Row 没有）
 * - provideContent 是一次性渲染入口，里面不能 collect Flow / 不能 runBlocking
 * - 同步数据获取必须在 provideGlance 的外层 suspend 函数里完成
 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // 在这个 suspend 上下文里同步获取数据（Glance 框架允许）
        val deviceName = DeviceDetector.getCurrentDevice(context).displayNameZh

        // 同步读 StateFlow 初始值（Repository 底层是 StateFlow，runBlocking + first 安全）
        val presets: List<MasterPreset> = runCatching {
            runBlocking { PresetRepository.getInstance(context).getAllPresets().first() }
        }.getOrDefault(emptyList())
        val recommended = presets.take(4)

        provideContent {
            WidgetContent(
                deviceName = deviceName,
                presets = recommended,
                context = context
            )
        }
    }

    @Composable
    private fun WidgetContent(
        deviceName: String,
        presets: List<MasterPreset>,
        context: Context
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(0xFF141414.toInt()))
                .padding(12.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            // 标题
            Text(
                text = context.getString(R.string.widget_today_title, deviceName),
                fontSize = 12f,
                color = ColorProvider(0xFFAAAAAA.toInt()),
                modifier = GlanceModifier.padding(bottom = 6.dp)
            )

            if (presets.isNotEmpty()) {
                // 预设横排（Glance Row 没有 spacing 参数 → 用多个 Row + padding）
                presets.forEachIndexed { index, preset ->
                    Text(
                        text = "• ${preset.name}",
                        fontSize = 13f,
                        color = ColorProvider(0xFFFFFFFF.toInt()),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = if (index == presets.lastIndex) 0.dp else 2.dp)
                    )
                }
            } else {
                Text(
                    text = context.getString(R.string.widget_empty),
                    fontSize = 12f,
                    color = ColorProvider(0xFF666666.toInt())
                )
            }
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
