package com.silas.omaster.ui.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.silas.omaster.data.config.ConfigCenter
import com.silas.omaster.data.local.FloatingWindowMode
import com.silas.omaster.model.MasterPreset
import com.silas.omaster.util.DeviceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 悬浮窗控制器
 * 管理悬浮窗状态和预设切换
 *
 * MU-27 / v1.6：新增 resolveModeForFloatingWindow() — 根据
 * ConfigCenter.followCameraFraming + 当前设备 ColorOS 版本
 * 在悬浮窗创建前自动判断最终模式：
 *
 *   优先级链（高 → 低）：
 *   ① Realme 预设 → STANDARD（PresetParamMapper.isRealmePreset）
 *   ② followCameraFraming=true 且 ColorOS ≥ 17 → FRAMING
 *   ③ 否则 → ConfigCenter.floatingWindowMode（用户偏好）
 */
class FloatingWindowController private constructor(private val context: Context) {

    private val configCenter by lazy { ConfigCenter.getInstance(context) }

    private val _currentPreset = MutableStateFlow<MasterPreset?>(null)
    val currentPreset: StateFlow<MasterPreset?> = _currentPreset.asStateFlow()

    private var presetList: List<MasterPreset> = emptyList()
    private var currentIndex: Int = 0

    /**
     * MU-27 / v1.6：自动避位模式缓存（避免每次 show 都重算）。
     * followCameraFraming 开关改变时通过 ConfigCenter Flow 感知（后续可扩展）。
     */
    private val shouldAutoFrame: Boolean by lazy {
        configCenter.isFollowCameraFramingEnabled &&
                DeviceDetector.isColorOsAtLeast(context, 17)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == FloatingWindowService.ACTION_SWITCH_PRESET) {
                val direction = intent.getStringExtra(FloatingWindowService.EXTRA_SWITCH_DIRECTION)
                handlePresetSwitch(direction)
            }
        }
    }

    /**
     * 注册广播接收器
     */
    fun register() {
        val filter = IntentFilter(FloatingWindowService.ACTION_SWITCH_PRESET)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(broadcastReceiver, filter)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 注销广播接收器
     */
    fun unregister() {
        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 显示悬浮窗
     *
     * MU-27 / v1.6：自动判断最终悬浮窗模式
     *   ① Realme 预设 → STANDARD（PresetParamMapper.isRealmePreset 在 Service 里再次判断，
     *     这里做一次前置预判让调用方感知）
     *   ② followCameraFraming=true && ColorOS ≥ 17 → FRAMING
     *   ③ 否则 → ConfigCenter.floatingWindowMode（用户偏好）
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

        currentIndex = index
        _currentPreset.value = preset

        // MU-27：通过 Intent extra 告诉 Service 该用什么模式（让 Service 跳过自身模式判断）
        FloatingWindowService.show(context, preset, currentIndex, presetList.map { it.id ?: "" })
    }

    /**
     * MU-27 / v1.6：根据预设内容 + ConfigCenter + DeviceDetector 解析出最终悬浮窗模式。
     * 优先级链：
     *   ① Realme 预设          → STANDARD
     *   ② followCameraFraming + ColorOS ≥ 17 → FRAMING
     *   ③ 否则                 → ConfigCenter.floatingWindowMode（用户偏好）
     */
    fun resolveModeForFloatingWindow(preset: MasterPreset): FloatingWindowMode {
        // ① Realme 品牌保护：Service 已有 isRealmePreset 判断，此处做一次前置对齐
        if (preset.author?.contains("@realme", ignoreCase = true) == true ||
            preset.author?.contains("realme", ignoreCase = true) == true) {
            return FloatingWindowMode.STANDARD
        }

        // ② 相机自动避位
        if (shouldAutoFrame) {
            return FloatingWindowMode.FRAMING
        }

        // ③ 用户偏好
        return configCenter.floatingWindowMode
    }

    /**
     * 设置预设列表（用于悬浮窗切换）
     */
    fun setPresetList(presets: List<MasterPreset>) {
        presetList = presets
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
        if (presetList.isEmpty()) return

        val newIndex = when (direction) {
            "prev" -> (currentIndex - 1 + presetList.size) % presetList.size
            "next" -> (currentIndex + 1) % presetList.size
            else -> return
        }

        currentIndex = newIndex
        val newPreset = presetList[newIndex]
        _currentPreset.value = newPreset

        // 使用 update 方法更新悬浮窗内容（避免闪动）
        FloatingWindowService.update(context, newPreset, currentIndex, presetList.map { it.id ?: "" })
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
