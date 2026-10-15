package com.silas.omaster.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * MU-24 / v1.6：ShortcutManager 动态 shortcut 注册
 *
 * 配合 AndroidManifest.xml 的静态 shortcut（shortcuts.xml）：
 *   - 静态：所有设备都有（floating_window_show / open_home）
 *   - 动态：Find X10 + ColorOS 17 额外有（ai_snap_import）
 *
 * 调用方：OMasterApplication.onCreate() — 设备首次启动时注册
 */
object ShortcutRegistrar {

    fun refresh(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        try {
            val sm = context.getSystemService(Context.SHORTCUT_SERVICE) as ShortcutManager
            val shortcuts = buildList {
                if (DeviceDetector.isFindX10Series(context) && DeviceDetector.isColorOsAtLeast(context, 17)) {
                    add(aiSnapShortcut(context))
                }
            }
            if (shortcuts.isNotEmpty()) {
                sm.addDynamicShortcuts(shortcuts)
            } else {
                // 如果用户设备降级/退出 Find X10，清除动态 shortcut
                sm.removeDynamicShortcuts(listOf(DYNAMIC_SHORTCUT_AI_SNAP))
            }
        } catch (e: Exception) {
            // 部分 OEM ROM 可能禁用 addDynamicShortcuts — 静默降级
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun aiSnapShortcut(context: Context): ShortcutInfo {
        val intent = Intent(Intent.ACTION_SEND_IMAGE).apply {
            setClassName(context, "com.silas.omaster.MainActivity")
            putExtra("from_shortcut", DYNAMIC_SHORTCUT_AI_SNAP)
        }
        return ShortcutInfo.Builder(context, DYNAMIC_SHORTCUT_AI_SNAP)
            .setShortLabel(context.getString(R.string.shortcut_ai_snap_short))
            .setLongLabel(context.getString(R.string.shortcut_ai_snap_long))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .setRank(1)
            .setEnabled(true)
            .build()
    }

    const val DYNAMIC_SHORTCUT_AI_SNAP = "ai_snap_import"
}
