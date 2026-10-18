package com.silas.omaster.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import com.silas.omaster.R
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * MU-24 / v1.6：ShortcutManager 动态 shortcut 注册
 *
 * 配合 AndroidManifest.xml 的静态 shortcut（shortcuts.xml）：
 *   - 静态：所有设备都有（floating_window_show / open_home）
 *   - 动态：Find X10 + ColorOS 17 额外有（v1.6.12 扩展为三连快捷）：
 *       ai_snap_import（rank 1，哈苏 AI 闪记参数截取导入）
 *       create_preset（rank 2，一键进入预设创作）
 *       discover（rank 3，发现广场浏览灵感）
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
                    add(navigateShortcut(context, DYNAMIC_SHORTCUT_CREATE,
                        R.string.shortcut_create_preset_short, R.string.shortcut_create_preset_long,
                        ACTION_CREATE, rank = 2))
                    add(navigateShortcut(context, DYNAMIC_SHORTCUT_DISCOVER,
                        R.string.shortcut_discover_short, R.string.shortcut_discover_long,
                        ACTION_DISCOVER, rank = 3))
                }
            }
            if (shortcuts.isNotEmpty()) {
                sm.addDynamicShortcuts(shortcuts)
            } else {
                // 如果用户设备降级/退出 Find X10，清除动态 shortcut
                sm.removeDynamicShortcuts(listOf(DYNAMIC_SHORTCUT_AI_SNAP, DYNAMIC_SHORTCUT_CREATE, DYNAMIC_SHORTCUT_DISCOVER))
            }
        } catch (e: Exception) {
            // 部分 OEM ROM 可能禁用 addDynamicShortcuts — 静默降级
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun aiSnapShortcut(context: Context): ShortcutInfo {
        // 注意：Android 没有 ACTION_SEND_IMAGE 常量，使用 ACTION_SEND 配合 mime image/*
        // AndroidManifest.xml 中已声明 MAIN + SEND + image/* intent-filter 接收此请求
        val intent = Intent(Intent.ACTION_SEND).apply {
            setClassName(context, "com.silas.omaster.MainActivity")
            type = "image/*"
            putExtra("from_shortcut", DYNAMIC_SHORTCUT_AI_SNAP)
        }
        return ShortcutInfo.Builder(context, DYNAMIC_SHORTCUT_AI_SNAP)
            .setShortLabel(context.getString(R.string.shortcut_ai_snap_short))
            .setLongLabel(context.getString(R.string.shortcut_ai_snap_long))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .setRank(1)
            .build()
    }

    /**
     * 【v1.6.12】导航类快捷方式通用构造：经 MainActivity 主入口 + shortcut_action extra，
     * 冷启动直达目标页（创建/发现）。
     */
    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun navigateShortcut(
        context: Context,
        id: String,
        shortLabelRes: Int,
        longLabelRes: Int,
        action: String,
        rank: Int
    ): ShortcutInfo {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(context, "com.silas.omaster.MainActivity")
            putExtra(EXTRA_SHORTCUT_ACTION, action)
        }
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(context.getString(shortLabelRes))
            .setLongLabel(context.getString(longLabelRes))
            .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher_round))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }

    const val DYNAMIC_SHORTCUT_AI_SNAP = "ai_snap_import"
    const val DYNAMIC_SHORTCUT_CREATE = "create_preset"
    const val DYNAMIC_SHORTCUT_DISCOVER = "discover"

    const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
    const val ACTION_CREATE = "create"
    const val ACTION_DISCOVER = "discover"
}
