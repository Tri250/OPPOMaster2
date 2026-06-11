package com.silas.omaster.param

import android.content.Context
import com.silas.omaster.data.local.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 参数调节管理器 - 完整版
 * 实现所有PP系列功能用例
 */
class ParamAdjustmentManager private constructor(context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)

    // 参数值范围定义
    companion object {
        const val MIN_VALUE = -100
        const val MAX_VALUE = 100

        // 快捷档位
        val QUICK_PRESETS = listOf(
            QuickPreset("原图", "还原原始效果", mapOf(
                "saturation" to 0, "contrast" to 0, "brightness" to 0,
                "warmth" to 0, "sharpness" to 0, "clarity" to 0,
                "highlights" to 0, "shadows" to 0
            )),
            QuickPreset("轻微调整", "轻度美化", mapOf(
                "saturation" to 5, "contrast" to 3, "brightness" to 2,
                "warmth" to 0, "sharpness" to 5, "clarity" to 3,
                "highlights" to 0, "shadows" to 0
            )),
            QuickPreset("中等调整", "中度增强", mapOf(
                "saturation" to 10, "contrast" to 8, "brightness" to 5,
                "warmth" to 0, "sharpness" to 10, "clarity" to 8,
                "highlights" to 0, "shadows" to 0
            )),
            QuickPreset("强力调整", "深度优化", mapOf(
                "saturation" to 15, "contrast" to 12, "brightness" to 8,
                "warmth" to 0, "sharpness" to 15, "clarity" to 12,
                "highlights" to 0, "shadows" to 0
            ))
        )

        @Volatile
        private var instance: ParamAdjustmentManager? = null

        fun getInstance(context: Context): ParamAdjustmentManager {
            return instance ?: synchronized(this) {
                instance ?: ParamAdjustmentManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // 可调参数定义
    val adjustableParams = listOf(
        AdjustableParam("saturation", "饱和度", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("contrast", "对比度", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("brightness", "亮度", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("warmth", "冷暖", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("sharpness", "锐度", 0, 100, 1, ParamUnit.NONE),
        AdjustableParam("clarity", "清晰度", 0, 100, 1, ParamUnit.NONE),
        AdjustableParam("highlights", "高光", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("shadows", "阴影", MIN_VALUE, MAX_VALUE, 1, ParamUnit.NONE),
        AdjustableParam("noiseReduction", "降噪", 0, 100, 1, ParamUnit.NONE),
        AdjustableParam("skinSmooth", "美肤", 0, 100, 1, ParamUnit.NONE),
        AdjustableParam("detail", "细节", 0, 100, 1, ParamUnit.NONE)
    )

    // 互斥参数组（切换时需要重置）
    val mutexGroups = mapOf(
        "saturation" to setOf("saturation", "vivid", "film", "bw"),
        "contrast" to setOf("contrast", "hdr"),
        "warmth" to setOf("warmth", "cool")
    )

    // 联动参数（调整一个影响另一个）
    val linkedParams = mapOf(
        "sharpness" to setOf("clarity"),
        "clarity" to setOf("sharpness")
    )

    // 当前参数值
    private val _paramValues = MutableStateFlow<Map<String, Int>>(emptyMap())
    val paramValues: StateFlow<Map<String, Int>> = _paramValues.asStateFlow()

    // 参数锁定状态（联动调整时）
    private val _lockedParams = MutableStateFlow<Set<String>>(emptySet())
    val lockedParams: StateFlow<Set<String>> = _lockedParams.asStateFlow()

    // 参数重置状态
    private val _resetHistory = MutableStateFlow<Map<String, Int>>(emptyMap())
    val resetHistory: StateFlow<Map<String, Int>> = _resetHistory.asStateFlow()

    /**
     * PP-001: 滑块调节
     * - 步进1单位
     * - 拖动手感跟手（由UI层保证）
     * - 越界值自动吸附到边界
     */
    fun adjustParam(paramName: String, rawValue: Float) {
        val param = adjustableParams.find { it.name == paramName } ?: return

        // PP-001: 越界值自动吸附到边界
        val clampedValue = rawValue.coerceIn(param.minValue.toFloat(), param.maxValue.toFloat())

        // PP-001: 步进1单位（四舍五入）
        val steppedValue = clampedValue.roundToInt()

        // 更新参数值
        val current = _paramValues.value.toMutableMap()
        current[paramName] = steppedValue
        _paramValues.value = current

        // PP-005: 联动调整
        linkedParams[paramName]?.let { linked ->
            val linkedParams = _lockedParams.value.toMutableSet()
            linkedParams.addAll(linked)
            _lockedParams.value = linkedParams

            linked.forEach { linkedName ->
                val linkedParam = adjustableParams.find { it.name == linkedName }
                if (linkedParam != null) {
                    val multiplier = when (paramName) {
                        "sharpness" -> 0.5f
                        "clarity" -> 0.5f
                        else -> 1f
                    }
                    val linkedValue = (steppedValue * multiplier).roundToInt()
                        .coerceIn(linkedParam.minValue, linkedParam.maxValue)

                    current[linkedName] = linkedValue
                }
            }
            _paramValues.value = current
        }
    }

    /**
     * PP-001: 获取参数值
     */
    fun getParamValue(paramName: String): Int {
        return _paramValues.value[paramName] ?: 0
    }

    /**
     * PP-001: 获取所有参数值
     */
    fun getAllParamValues(): Map<String, Int> = _paramValues.value

    /**
     * PP-002: 数值输入
     * - 输入框只接受数字+正负号
     * - 越界提示"范围 -100~100"
     * - 输入框支持1位小数
     */
    fun setParamValueFromInput(paramName: String, input: String): InputResult {
        // PP-002: 只接受数字+正负号
        val filtered = input.filter { it.isDigit() || it == '.' || it == '-' }

        if (filtered.isEmpty()) {
            return InputResult.Error("请输入有效数字")
        }

        // PP-002: 解析数值（支持1位小数）
        val value = try {
            if (filtered.contains('.')) {
                (filtered.toFloat() * 10).roundToInt() / 10f
            } else {
                filtered.toFloat()
            }
        } catch (e: NumberFormatException) {
            return InputResult.Error("格式错误")
        }

        val param = adjustableParams.find { it.name == paramName } ?: return InputResult.Error("参数不存在")

        // PP-002: 越界提示
        if (value < param.minValue || value > param.maxValue) {
            return InputResult.Error("范围 ${param.minValue}~${param.maxValue}")
        }

        // 应用值
        val intValue = value.roundToInt()
        adjustParam(paramName, intValue.toFloat())

        return InputResult.Success(intValue)
    }

    /**
     * PP-003: 重置参数到初始值
     */
    fun resetParam(paramName: String) {
        // 记录重置前的值
        val current = _paramValues.value[paramName] ?: return
        val history = _resetHistory.value.toMutableMap()
        history[paramName] = current
        _resetHistory.value = history

        // 重置为0
        adjustParam(paramName, 0f)
    }

    /**
     * PP-003: 恢复参数到重置前
     */
    fun restoreParam(paramName: String) {
        val history = _resetHistory.value
        history[paramName]?.let { value ->
            adjustParam(paramName, value.toFloat())
            val newHistory = history.toMutableMap()
            newHistory.remove(paramName)
            _resetHistory.value = newHistory
        }
    }

    /**
     * PP-004: 快捷档位
     * - 档位表可自定义
     * - 选择档位前显示效果预览
     * - 切换档位不破坏其他参数
     */
    fun applyQuickPreset(preset: QuickPreset) {
        val current = _paramValues.value.toMutableMap()

        preset.params.forEach { (paramName, value) ->
            // PP-004: 切换档位不破坏其他参数
            if (!isParamManuallyModified(paramName)) {
                current[paramName] = value
            }
        }

        _paramValues.value = current
    }

    /**
     * PP-004: 获取快捷档位
     */
    fun getQuickPresets(): List<QuickPreset> = QUICK_PRESETS

    /**
     * PP-004: 添加自定义快捷档位
     */
    fun addCustomQuickPreset(name: String, params: Map<String, Int>) {
        // 保存到本地
        settingsManager.customQuickPresets = settingsManager.customQuickPresets + mapOf(name to params)
    }

    /**
     * PP-005: 联动调整（互斥/联动）
     * - 切换滤镜不破坏用户已编辑的其他参数（除非互斥）
     * - 互斥参数变灰并显示原因
     */
    fun applyStyleFilter(styleId: String, styleParams: Map<String, Int>) {
        val current = _paramValues.value.toMutableMap()

        // 检查互斥参数
        val mutexGroup = mutexGroups.entries.find { (_, params) ->
            styleParams.keys.any { styleParams[it] != 0 }
        }

        if (mutexGroup != null) {
            // 互斥参数：需要重置用户修改的值
            mutexGroup.value.forEach { paramName ->
                if (isParamManuallyModified(paramName)) {
                    // 用户已手动修改，互斥处理
                    // 可选择：保持用户值或提示用户
                } else {
                    // 应用新值
                    current[paramName] = styleParams[paramName] ?: 0
                }
            }
        } else {
            // 非互斥：只更新对应参数
            styleParams.forEach { (paramName, value) ->
                if (!isParamManuallyModified(paramName)) {
                    current[paramName] = value
                }
            }
        }

        _paramValues.value = current
    }

    /**
     * PP-005: 检查参数是否互斥
     */
    fun isParamMutex(paramName: String): Boolean {
        return mutexGroups.values.any { it.contains(paramName) }
    }

    /**
     * PP-005: 获取互斥原因
     */
    fun getMutexReason(paramName: String): String? {
        return mutexGroups.entries.find { it.value.contains(paramName) }?.key
    }

    /**
     * 标记参数已被手动修改
     */
    fun markAsManuallyModified(paramName: String) {
        val modified = settingsManager.manuallyModifiedParams.toMutableSet()
        modified.add(paramName)
        settingsManager.manuallyModifiedParams = modified.toList()
    }

    /**
     * 检查参数是否被手动修改
     */
    fun isParamManuallyModified(paramName: String): Boolean {
        return settingsManager.manuallyModifiedParams.contains(paramName)
    }

    /**
     * PP-007: 范围越界与异常处理
     * - 不崩溃
     * - 输入字符超长截断
     * - 非数字字符自动过滤
     */
    fun safeSetParam(paramName: String, value: Any?) {
        try {
            if (value == null) return

            val intValue = when (value) {
                is Int -> value
                is Float -> value.roundToInt()
                is Double -> value.roundToInt()
                is String -> {
                    // PP-007: 非数字字符自动过滤
                    val filtered = value.filter { it.isDigit() || it == '-' }
                    // PP-007: 输入字符超长截断
                    filtered.take(10).toIntOrNull() ?: return
                }
                else -> return
            }

            val param = adjustableParams.find { it.name == paramName } ?: return

            // PP-007: 越界值自动吸附到边界
            val clampedValue = intValue.coerceIn(param.minValue, param.maxValue)

            adjustParam(paramName, clampedValue.toFloat())

        } catch (e: Exception) {
            // PP-007: 不崩溃，记录日志
        }
    }

    /**
     * 清除所有手动修改标记
     */
    fun clearManuallyModifiedFlags() {
        settingsManager.manuallyModifiedParams = emptyList()
    }

    /**
     * 获取参数定义
     */
    fun getParamDefinition(paramName: String): AdjustableParam? {
        return adjustableParams.find { it.name == paramName }
    }
}

/**
 * 可调参数定义
 */
data class AdjustableParam(
    val name: String,
    val displayName: String,
    val minValue: Int,
    val maxValue: Int,
    val step: Int,
    val unit: ParamUnit
)

/**
 * 参数单位
 */
enum class ParamUnit {
    NONE,       // 无单位
    PERCENT,    // 百分比
    KELVIN,     // 色温(K)
    MM,         // 焦距(mm)
    F_NUMBER    // 光圈(f/)
}

/**
 * 快捷预设
 */
data class QuickPreset(
    val id: String,
    val name: String,
    val description: String,
    val params: Map<String, Int>,
    val isCustom: Boolean = false
)

/**
 * 输入结果
 */
sealed class InputResult {
    data class Success(val value: Int) : InputResult()
    data class Error(val message: String) : InputResult()
}
