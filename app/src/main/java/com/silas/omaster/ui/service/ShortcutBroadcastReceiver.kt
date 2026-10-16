package com.silas.omaster.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MU-24 / v1.6：ColorOS 17 侧边键快捷键广播接收器
 *
 * 当用户在 ColorOS 17 设置里把"悬浮窗"shortcut 映射到侧边键时，
 * 按一下侧边键 → 系统发送 ACTION_SHOW_FLOATING_WINDOW → 此 Receiver →
 * 取第一个收藏预设 → 第一个可用预设 → 调 Controller.showFloatingWindow()
 *
 * ED-05 合规：不采集任何数据、不申请额外权限
 */
class ShortcutBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SHOW_FLOATING_WINDOW) {
            val pendingResult = goAsync()
            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    showFirstPresetFloatingWindow(context)
                } catch (e: Exception) {
                    android.util.Log.e("ShortcutBroadcast", "showFloatingWindow 失败", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun showFirstPresetFloatingWindow(context: Context) {
        val controller = FloatingWindowController.getInstance(context)
        val repository = PresetRepository.getInstance(context)

        // 已是 suspend 上下文（外层的 withContext(Dispatchers.IO)），直接 first() 取初始值即可；
        // Repository 底层是 StateFlow，first() 立即返回，无需 runBlocking
        val preset: MasterPreset? = withContext(Dispatchers.IO) {
            val favs = repository.getFavoritePresets().first()
            favs.firstOrNull() ?: repository.getAllPresets().first().firstOrNull()
        }

        if (preset != null) {
            withContext(Dispatchers.Main) {
                controller.showFloatingWindow(preset)
            }
        }
    }

    companion object {
        const val ACTION_SHOW_FLOATING_WINDOW = "com.silas.omaster.action.ACTION_SHOW_FLOATING_WINDOW"
    }
}
