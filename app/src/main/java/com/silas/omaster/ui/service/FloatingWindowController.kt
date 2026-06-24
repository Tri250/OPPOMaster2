package com.silas.omaster.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.silas.omaster.model.MasterPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 悬浮窗控制器
 * 管理悬浮窗状态和预设切换
 */
class FloatingWindowController private constructor(private val context: Context) {

    private val _currentPreset = MutableStateFlow<MasterPreset?>(null)
    val currentPreset: StateFlow<MasterPreset?> = _currentPreset.asStateFlow()

    @Volatile
    private var presetList: List<MasterPreset> = emptyList()
    @Volatile
    private var currentIndex: Int = 0

    // 状态标志：避免重复注册或注销
    @Volatile
    private var isRegistered = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (intent.action == FloatingWindowService.ACTION_SWITCH_PRESET) {
                    val direction = intent.getStringExtra(FloatingWindowService.EXTRA_SWITCH_DIRECTION)
                    handlePresetSwitch(direction)
                }
            } catch (e: Exception) {
                // 广播回调中抛出的异常会被系统忽略，但记录日志便于排查
                android.util.Log.e("FloatingWindowController", "广播处理异常", e)
            }
        }
    }

    /**
     * 注册广播接收器
     */
    fun register() {
        if (isRegistered) {
            android.util.Log.w("FloatingWindowController", "广播接收器已注册,跳过")
            return
        }
        val filter = IntentFilter(FloatingWindowService.ACTION_SWITCH_PRESET)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }
            isRegistered = true
            android.util.Log.i("FloatingWindowController", "广播接收器已注册")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowController", "注册广播接收器失败", e)
        }
    }

    /**
     * 注销广播接收器
     */
    fun unregister() {
        if (!isRegistered) {
            // 未注册时静默返回,避免 IllegalArgumentException
            return
        }
        try {
            context.unregisterReceiver(broadcastReceiver)
            isRegistered = false
            android.util.Log.i("FloatingWindowController", "广播接收器已注销")
        } catch (e: IllegalArgumentException) {
            // 已被外部注销,重置状态
            isRegistered = false
            android.util.Log.w("FloatingWindowController", "注销时未找到接收器,已重置状态")
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowController", "注销广播接收器失败", e)
        }
    }

    /**
     * 显示悬浮窗
     */
    fun showFloatingWindow(preset: MasterPreset, presets: List<MasterPreset> = emptyList()) {
        // 如果传入了新的列表，则更新保存的列表
        if (presets.isNotEmpty()) {
            presetList = presets
        }
        
        // 尝试根据 ID 查找索引
        var index = presetList.indexOfFirst { it.id == preset.id }
        
        // 如果找不到 ID，尝试根据名称查找（兜底方案）
        if (index == -1) {
            index = presetList.indexOfFirst { it.name == preset.name }
        }
        
        // 仍然找不到，则默认为 0 并记录日志
        if (index == -1) {
            android.util.Log.w("FloatingWindowController", "Preset not found in list, defaulting to index 0. ID: ${preset.id}, Name: ${preset.name}")
            index = 0
        }

        synchronized(this) {
            currentIndex = index
        }
        _currentPreset.value = preset

        FloatingWindowService.show(context, preset, currentIndex, presetList.map { it.id ?: "" })
    }

    /**
     * 设置预设列表（用于悬浮窗切换）
     * 当列表变更时，同步更新 currentIndex 和 _currentPreset
     */
    fun setPresetList(presets: List<MasterPreset>) {
        val oldPreset = _currentPreset.value
        presetList = presets

        if (presets.isEmpty()) {
            // 列表为空时重置状态
            synchronized(this) { currentIndex = 0 }
            _currentPreset.value = null
            return
        }

        // 尝试在新列表中保持当前预设的选中状态
        if (oldPreset != null) {
            var newIndex = presets.indexOfFirst { it.id == oldPreset.id }
            // 如果找不到 ID，尝试根据名称查找（兜底方案）
            if (newIndex == -1) {
                newIndex = presets.indexOfFirst { it.name == oldPreset.name }
            }
            if (newIndex != -1) {
                // 当前预设仍在列表中，保持选中
                synchronized(this) { currentIndex = newIndex }
                _currentPreset.value = presets[newIndex]
            } else {
                // 当前预设不在新列表中，默认选中第一个并更新 StateFlow
                synchronized(this) { currentIndex = 0 }
                _currentPreset.value = presets[0]
            }
        } else if (currentIndex in presets.indices) {
            // 之前没有选中预设，但索引有效，更新 StateFlow
            _currentPreset.value = presets[currentIndex]
        } else {
            // 索引无效，重置为第一个
            synchronized(this) { currentIndex = 0 }
            _currentPreset.value = presets[0]
        }
    }

    /**
     * 隐藏悬浮窗
     */
    fun hideFloatingWindow() {
        FloatingWindowService.hide(context)
    }

    /**
     * 处理预设切换
     */
    private fun handlePresetSwitch(direction: String?) {
        val list = presetList
        if (list.isEmpty()) {
            android.util.Log.w("FloatingWindowController", "预设列表为空,无法切换")
            return
        }

        synchronized(this) {
            // 防御性：currentIndex 越界时重置
            if (currentIndex !in list.indices) {
                android.util.Log.w("FloatingWindowController", "currentIndex 越界($currentIndex),重置为0")
                currentIndex = 0
            }

            val newIndex = when (direction) {
                "prev" -> (currentIndex - 1 + list.size) % list.size
                "next" -> (currentIndex + 1) % list.size
                else -> {
                    android.util.Log.w("FloatingWindowController", "未知切换方向: $direction")
                    return
                }
            }

            // 防御性：newIndex 必须有效
            if (newIndex !in list.indices) {
                android.util.Log.e("FloatingWindowController", "计算后的索引无效: $newIndex")
                return
            }

            currentIndex = newIndex
        }

        val newPreset = list[currentIndex]
        _currentPreset.value = newPreset

        // 使用 update 方法更新悬浮窗内容（避免闪动）
        try {
            FloatingWindowService.update(context, newPreset, currentIndex, presetList.map { it.id ?: "" })
        } catch (e: Exception) {
            android.util.Log.e("FloatingWindowController", "更新悬浮窗失败", e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FloatingWindowController? = null

        fun getInstance(context: Context): FloatingWindowController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FloatingWindowController(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
