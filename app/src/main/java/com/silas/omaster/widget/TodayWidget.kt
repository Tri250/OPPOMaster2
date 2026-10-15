package com.silas.omaster.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import com.silas.omaster.MainActivity
import com.silas.omaster.R
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.util.DeviceDetector
import kotlinx.coroutines.runBlocking

/**
 * MU-28 / v1.6：OMaster 桌面小组件（AndroidX Glance API）
 *
 * Glance 1.1.x API 要点：
 * - GlanceAppWidget 只有一个可 override 的 suspend fun: provideContent(context, id)
 *   （正确入口是 provideGlance（override）
 // provideContent 是内部 builder DSL）
 * - 在 provideContent 内调用 Glance 版 provideContent { ... } 作为 Composable builder
 * - androidx.glance.material3.Text 仅在 Glance Material3 模块存在，
 *   为避免依赖版本不兼容，用 androidx.glance.text.Text（基础 Text）
 * - runBlocking 可在 provideContent (suspend) 外层调用 StateFlow.first()
 */
class TodayWidget : GlanceAppWidget() {

    // Glance 1.x 正确入口是 provideGlance（不是 provideContent）
    // provideContent 是 provideGlance 内部调用的 Composable builder
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // provideContent 是 suspend 函数，可以安全地获取数据
        val deviceName = DeviceDetector.getCurrentDevice(context).displayNameZh

        // 同步读 StateFlow 初始值：runBlocking + first() 在 suspend 上下文内是安全的
        val presets: List<com.silas.omaster.model.MasterPreset> = runCatching {
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
}

/**
 * Glance Composable —— 注意这不是 androidx.compose.runtime.Composable，
 * 而是 androidx.glance 的内容 builder 函数
 */
@androidx.compose.runtime.Composable
private fun WidgetContent(
    deviceName: String,
    presets: List<com.silas.omaster.model.MasterPreset>,
    context: Context
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(0xFF141414))
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.Start
    ) {
        // 标题
        Text(
            text = context.getString(R.string.widget_today_title, deviceName),
            modifier = GlanceModifier.padding(bottom = 6.dp)
        )

        if (presets.isNotEmpty()) {
            presets.forEachIndexed { index, preset ->
                Text(
                    text = "• ${preset.name}",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(bottom = if (index == presets.lastIndex) 0.dp else 2.dp)
                )
            }
        } else {
            Text(
                text = context.getString(R.string.widget_empty)
            )
        }
    }
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
