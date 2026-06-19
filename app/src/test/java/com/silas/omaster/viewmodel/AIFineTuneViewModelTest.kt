package com.silas.omaster.viewmodel

import com.silas.omaster.ai.AIFineTuneManager
import com.silas.omaster.ai.AISuggestion
import com.silas.omaster.ai.ErrorState
import com.silas.omaster.renderer.RenderParameters
import com.silas.omaster.ui.features.AIFineTuneViewModel
import com.silas.omaster.ui.features.InferenceStage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AIFineTuneViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AIFineTuneViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var aiManager: AIFineTuneManager

    private lateinit var viewModel: AIFineTuneViewModel

    private val isProcessingFlow = MutableStateFlow(false)
    private val suggestedParamsFlow = MutableStateFlow<AISuggestion?>(null)
    private val errorStateFlow = MutableStateFlow<ErrorState?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        aiManager = mockk(relaxed = true)

        every { aiManager.isProcessing } returns isProcessingFlow
        every { aiManager.suggestedParams } returns suggestedParamsFlow
        every { aiManager.errorState } returns errorStateFlow

        viewModel = AIFineTuneViewModel(aiManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始化时处理状态为false`() = runTest {
        assertFalse(viewModel.isProcessing.value)
    }

    @Test
    fun `初始化时推理阶段为IDLE`() = runTest {
        assertEquals(InferenceStage.IDLE, viewModel.inferenceStage.value)
    }

    @Test
    fun `设置Tab更新状态`() = runTest {
        val tab = "hsl"
        viewModel.setTab(tab)

        assertEquals(tab, viewModel.activeTab.value)
    }

    @Test
    fun `选择风格更新状态`() = runTest {
        val styleId = "natural"
        viewModel.selectStyle(styleId)

        assertEquals(styleId, viewModel.selectedStyleId.value)
    }

    @Test
    fun `切换优化选项更新状态`() = runTest {
        val optimizationId = "hdrEnhance"

        viewModel.toggleOptimization(optimizationId)
        assertTrue(viewModel.selectedOptimizations.value.contains(optimizationId))

        viewModel.toggleOptimization(optimizationId)
        assertFalse(viewModel.selectedOptimizations.value.contains(optimizationId))
    }

    @Test
    fun `切换参数锁定更新状态`() = runTest {
        val paramId = "exposure"

        viewModel.toggleParamLock(paramId)
        assertTrue(viewModel.lockedParams.value.contains(paramId))

        viewModel.toggleParamLock(paramId)
        assertFalse(viewModel.lockedParams.value.contains(paramId))
    }

    @Test
    fun `切换对比预览更新状态`() = runTest {
        assertFalse(viewModel.showCompare.value)

        viewModel.toggleCompare()
        assertTrue(viewModel.showCompare.value)

        viewModel.toggleCompare()
        assertFalse(viewModel.showCompare.value)
    }

    @Test
    fun `更新参数值更新当前参数`() = runTest {
        viewModel.updateParam("exposure", 10f)

        assertEquals(10f, viewModel.currentParams.value.exposure)
    }

    @Test
    fun `更新亮度参数`() = runTest {
        viewModel.updateParam("brightness", 20f)

        assertEquals(20f, viewModel.currentParams.value.brightness)
    }

    @Test
    fun `更新对比度参数`() = runTest {
        viewModel.updateParam("contrast", 15f)

        assertEquals(15f, viewModel.currentParams.value.contrast)
    }

    @Test
    fun `更新饱和度参数`() = runTest {
        viewModel.updateParam("saturation", 25f)

        assertEquals(25f, viewModel.currentParams.value.saturation)
    }

    @Test
    fun `设置曲线通道更新状态`() = runTest {
        val channel = "R"
        viewModel.setCurveChannel(channel)

        assertEquals(channel, viewModel.curveChannel.value)
    }

    @Test
    fun `应用曲线预设更新曲线点`() = runTest {
        viewModel.applyCurvePreset("sCurve")

        val points = viewModel.curvePoints.value[viewModel.curveChannel.value]
        assertNotNull(points)
        assertTrue(points!!.size >= 4)
    }

    @Test
    fun `重置推理状态`() = runTest {
        viewModel.resetInference()

        assertEquals(InferenceStage.IDLE, viewModel.inferenceStage.value)
        assertEquals(0f, viewModel.inferenceProgress.value)
        assertEquals("", viewModel.inferenceMessage.value)
        assertFalse(viewModel.showSuccess.value)
        assertNull(viewModel.errorState.value)
    }

    @Test
    fun `清除成功提示`() = runTest {
        viewModel.clearSuccess()

        assertFalse(viewModel.showSuccess.value)
    }

    @Test
    fun `获取最终参数返回当前参数`() = runTest {
        val params = viewModel.getFinalParams()

        assertNotNull(params)
        assertEquals(viewModel.currentParams.value, params)
    }

    @Test
    fun `HSL默认值包含8个通道`() = runTest {
        val hslValues = viewModel.hslValues.value

        assertEquals(8, hslValues.size)
    }

    @Test
    fun `HSL默认值包含红橙黄绿青蓝紫洋红`() = runTest {
        val hslValues = viewModel.hslValues.value

        assertTrue(hslValues.any { it.id == "red" })
        assertTrue(hslValues.any { it.id == "orange" })
        assertTrue(hslValues.any { it.id == "yellow" })
        assertTrue(hslValues.any { it.id == "green" })
        assertTrue(hslValues.any { it.id == "cyan" })
        assertTrue(hslValues.any { it.id == "blue" })
        assertTrue(hslValues.any { it.id == "purple" })
        assertTrue(hslValues.any { it.id == "magenta" })
    }
}
