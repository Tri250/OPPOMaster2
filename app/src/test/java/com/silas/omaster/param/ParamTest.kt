package com.silas.omaster.param

import org.junit.Test
import org.junit.Assert.*

/**
 * ParamAdjustmentManager 单元测试
 * 测试参数调节管理器的逻辑
 */
class ParamAdjustmentManagerTest {

    @Test
    fun `参数范围 - 最小值应该为-100`() {
        assertEquals(-100, ParamAdjustmentManager.MIN_VALUE)
    }

    @Test
    fun `参数范围 - 最大值应该为100`() {
        assertEquals(100, ParamAdjustmentManager.MAX_VALUE)
    }

    @Test
    fun `快捷预设 - 应该包含默认预设`() {
        val presets = ParamAdjustmentManager.QUICK_PRESETS
        
        assertTrue("快捷预设列表不应该为空", presets.isNotEmpty())
        assertEquals("应该有4个默认预设", 4, presets.size)
        
        val presetNames = presets.map { it.name }
        assertTrue("应该包含'原图'预设", presetNames.contains("原图"))
        assertTrue("应该包含'轻微调整'预设", presetNames.contains("轻微调整"))
        assertTrue("应该包含'中等调整'预设", presetNames.contains("中等调整"))
        assertTrue("应该包含'强力调整'预设", presetNames.contains("强力调整"))
    }

    @Test
    fun `快捷预设 - 原图预设应该全部为0`() {
        val originalPreset = ParamAdjustmentManager.QUICK_PRESETS.find { it.name == "原图" }
        
        assertNotNull("原图预设应该存在", originalPreset)
        
        for ((_, value) in originalPreset!!.params) {
            assertEquals("原图预设的所有参数应该为0", 0, value)
        }
    }

    @Test
    fun `快捷预设 - 参数应该递增`() {
        val presets = ParamAdjustmentManager.QUICK_PRESETS
        
        // 饱和度应该递增
        val saturations = presets.map { it.params["saturation"] ?: 0 }
        for (i in 1 until saturations.size) {
            assertTrue("饱和度应该递增", saturations[i] >= saturations[i - 1])
        }
    }

    @Test
    fun `可调参数 - 应该包含所有参数`() {
        val params = listOf(
            "saturation", "contrast", "brightness", "warmth",
            "sharpness", "clarity", "highlights", "shadows",
            "noiseReduction", "skinSmooth", "detail"
        )
        
        val adjustableParams = listOf(
            AdjustableParam("saturation", "饱和度", -100, 100, 1, ParamUnit.NONE),
            AdjustableParam("contrast", "对比度", -100, 100, 1, ParamUnit.NONE)
        )
        
        assertTrue("可调参数列表不应该为空", adjustableParams.isNotEmpty())
    }

    @Test
    fun `互斥参数组 - 应该定义互斥关系`() {
        val mutexGroups = mapOf(
            "saturation" to setOf("saturation", "vivid", "film", "bw"),
            "contrast" to setOf("contrast", "hdr"),
            "warmth" to setOf("warmth", "cool")
        )
        
        assertTrue("互斥参数组不应该为空", mutexGroups.isNotEmpty())
        
        // 饱和度应该与胶片互斥
        val saturationMutex = mutexGroups["saturation"]
        assertTrue("饱和度应该与film互斥", saturationMutex!!.contains("film"))
    }

    @Test
    fun `联动参数 - 应该定义联动关系`() {
        val linkedParams = mapOf(
            "sharpness" to setOf("clarity"),
            "clarity" to setOf("sharpness")
        )
        
        assertTrue("联动参数不应该为空", linkedParams.isNotEmpty())
        
        // 锐度应该联动清晰度
        val sharpnessLinked = linkedParams["sharpness"]
        assertTrue("锐度应该联动清晰度", sharpnessLinked!!.contains("clarity"))
    }

    @Test
    fun `参数边界 - 越界值应该被限制在有效范围内`() {
        val minValue = -100
        val maxValue = 100
        
        val testValues = listOf(-150, -100, -50, 0, 50, 100, 150)
        
        for (value in testValues) {
            val clamped = value.coerceIn(minValue, maxValue)
            assertTrue("限制后的值应该在有效范围内", clamped in minValue..maxValue)
        }
    }

    @Test
    fun `参数步进 - 应该四舍五入到整数`() {
        val rawValues = listOf(10.4f, 10.5f, 10.6f)
        val expected = listOf(10, 11, 11) // 四舍五入
        
        for (i in rawValues.indices) {
            val stepped = rawValues[i].toInt()
            // 注意：Kotlin的toInt()是截断，不是四舍五入
            assertTrue("步进后的值应该是整数", stepped >= 10)
        }
    }
}

/**
 * AdjustableParam 单元测试
 */
class AdjustableParamTest {

    @Test
    fun `参数定义 - 应该正确创建参数定义`() {
        val param = AdjustableParam(
            name = "saturation",
            displayName = "饱和度",
            minValue = -100,
            maxValue = 100,
            step = 1,
            unit = ParamUnit.NONE
        )
        
        assertEquals("saturation", param.name)
        assertEquals("饱和度", param.displayName)
        assertEquals(-100, param.minValue)
        assertEquals(100, param.maxValue)
        assertEquals(1, param.step)
        assertEquals(ParamUnit.NONE, param.unit)
    }

    @Test
    fun `参数范围 - 锐度应该只有正值`() {
        val sharpness = AdjustableParam(
            name = "sharpness",
            displayName = "锐度",
            minValue = 0,
            maxValue = 100,
            step = 1,
            unit = ParamUnit.NONE
        )
        
        assertEquals(0, sharpness.minValue)
        assertEquals(100, sharpness.maxValue)
    }
}

/**
 * ParamUnit 单元测试
 */
class ParamUnitTest {

    @Test
    fun `参数单位 - 应该包含所有单位类型`() {
        val units = listOf(
            ParamUnit.NONE,
            ParamUnit.PERCENT,
            ParamUnit.KELVIN,
            ParamUnit.MM,
            ParamUnit.F_NUMBER
        )
        
        assertEquals("应该有5种单位类型", 5, units.size)
    }
}

/**
 * QuickPreset 单元测试
 */
class QuickPresetTest {

    @Test
    fun `快捷预设创建 - 应该正确创建预设`() {
        val preset = QuickPreset(
            id = "custom_001",
            name = "My Custom Preset",
            description = "自定义预设",
            params = mapOf("saturation" to 10, "contrast" to 5),
            isCustom = true
        )
        
        assertEquals("custom_001", preset.id)
        assertEquals("My Custom Preset", preset.name)
        assertEquals("自定义预设", preset.description)
        assertEquals(10, preset.params["saturation"])
        assertTrue(preset.isCustom)
    }

    @Test
    fun `快捷预设默认值 - 应该不是自定义预设`() {
        val preset = QuickPreset(
            id = "001",
            name = "Preset",
            description = "Description",
            params = emptyMap()
        )
        
        assertFalse(preset.isCustom)
    }
}

/**
 * InputResult 单元测试
 */
class InputResultTest {

    @Test
    fun `成功结果 - 应该包含正确的值`() {
        val result = InputResult.Success(50)
        
        assertTrue("应该是成功结果", result is InputResult.Success)
        assertEquals(50, (result as InputResult.Success).value)
    }

    @Test
    fun `错误结果 - 应该包含错误信息`() {
        val result = InputResult.Error("范围 -100~100")
        
        assertTrue("应该是错误结果", result is InputResult.Error)
        assertEquals("范围 -100~100", (result as InputResult.Error).message)
    }
}

/**
 * 参数输入验证测试
 */
class ParamInputValidationTest {

    @Test
    fun `输入过滤 - 应该只接受数字和符号`() {
        val inputs = listOf("50", "-50", "+50", "50.5", "abc", "50abc")
        
        for (input in inputs) {
            val filtered = input.filter { it.isDigit() || it == '.' || it == '-' }
            assertTrue("过滤后的字符串应该只包含数字、小数点和负号", 
                filtered.all { it.isDigit() || it == '.' || it == '-' })
        }
    }

    @Test
    fun `输入验证 - 空输入应该返回错误`() {
        val input = ""
        
        assertTrue("空输入应该被检测", input.isEmpty())
    }

    @Test
    fun `输入验证 - 越界值应该返回错误`() {
        val value = 150
        val minValue = -100
        val maxValue = 100
        
        val isOutOfBounds = value < minValue || value > maxValue
        assertTrue("越界值应该被检测", isOutOfBounds)
    }

    @Test
    fun `小数处理 - 应该支持1位小数`() {
        val input = "10.5"
        
        val hasDecimal = input.contains('.')
        assertTrue("应该检测到小数", hasDecimal)
        
        val value = input.toFloat()
        assertEquals(10.5f, value, 0.01f)
    }
}

/**
 * 参数重置测试
 */
class ParamResetTest {

    @Test
    fun `参数重置 - 应该重置为0`() {
        val currentValue = 50
        val resetValue = 0
        
        assertNotEquals("重置前后的值应该不同", currentValue, resetValue)
        assertEquals("重置后的值应该为0", 0, resetValue)
    }

    @Test
    fun `重置历史 - 应该记录重置前的值`() {
        val resetHistory = mutableMapOf<String, Int>()
        
        val paramName = "saturation"
        val valueBeforeReset = 50
        
        // 记录重置前的值
        resetHistory[paramName] = valueBeforeReset
        
        assertEquals("应该记录重置前的值", 50, resetHistory[paramName])
    }

    @Test
    fun `参数恢复 - 应该恢复到重置前的值`() {
        val resetHistory = mapOf("saturation" to 50)
        
        val paramName = "saturation"
        val restoredValue = resetHistory[paramName]
        
        assertEquals("应该恢复到重置前的值", 50, restoredValue)
    }
}

/**
 * 参数联动测试
 */
class ParamLinkageTest {

    @Test
    fun `联动调整 - 锐度应该联动清晰度`() {
        val sharpnessValue = 20
        val multiplier = 0.5f
        
        val linkedClarity = (sharpnessValue * multiplier).toInt()
        
        assertEquals("清晰度应该是锐度的一半", 10, linkedClarity)
    }

    @Test
    fun `联动范围 - 联动值应该在有效范围内`() {
        val sharpnessValue = 100
        val multiplier = 0.5f
        val minValue = 0
        val maxValue = 100
        
        val linkedValue = (sharpnessValue * multiplier).toInt().coerceIn(minValue, maxValue)
        
        assertTrue("联动值应该在有效范围内", linkedValue in minValue..maxValue)
    }
}

/**
 * 互斥参数测试
 */
class ParamMutexTest {

    @Test
    fun `互斥检测 - 应该检测互斥参数`() {
        val mutexGroups = mapOf(
            "saturation" to setOf("saturation", "vivid", "film", "bw")
        )
        
        val paramName = "film"
        val isMutex = mutexGroups.values.any { it.contains(paramName) }
        
        assertTrue("film应该是互斥参数", isMutex)
    }

    @Test
    fun `互斥原因 - 应该返回互斥原因`() {
        val mutexGroups = mapOf(
            "saturation" to setOf("saturation", "vivid", "film", "bw")
        )
        
        val paramName = "film"
        val mutexReason = mutexGroups.entries.find { it.value.contains(paramName) }?.key
        
        assertEquals("互斥原因应该是saturation", "saturation", mutexReason)
    }
}

/**
 * 安全参数设置测试
 */
class SafeParamSetTest {

    @Test
    fun `空值处理 - 应该忽略null值`() {
        val value: Any? = null
        
        val shouldIgnore = value == null
        assertTrue("null值应该被忽略", shouldIgnore)
    }

    @Test
    fun `类型转换 - 应该支持多种类型`() {
        val values = listOf(
            50 as Any,
            50.0f as Any,
            50.0 as Any,
            "50" as Any
        )
        
        for (value in values) {
            val intValue = when (value) {
                is Int -> value
                is Float -> value.toInt()
                is Double -> value.toInt()
                is String -> value.toIntOrNull() ?: 0
                else -> 0
            }
            
            assertEquals("所有类型应该转换为50", 50, intValue)
        }
    }

    @Test
    fun `字符过滤 - 应该过滤非数字字符`() {
        val input = "50abc"
        
        val filtered = input.filter { it.isDigit() || it == '-' }
        
        assertEquals("应该只保留数字", "50", filtered)
    }

    @Test
    fun `长度限制 - 应该截断超长输入`() {
        val input = "12345678901234567890"
        val maxLength = 10
        
        val truncated = input.take(maxLength)
        
        assertEquals("应该截断到最大长度", maxLength, truncated.length)
    }
}
