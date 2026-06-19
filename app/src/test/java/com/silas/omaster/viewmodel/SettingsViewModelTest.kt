package com.silas.omaster.viewmodel

import android.app.Application
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.settings.SettingsViewModel
import com.silas.omaster.ui.theme.BrandTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
 * SettingsViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var settingsManager: SettingsManager
    private lateinit var presetRepository: PresetRepository
    private lateinit var application: Application

    private lateinit var viewModel: SettingsViewModel

    private val themeFlow = MutableStateFlow(BrandTheme.Hasselblad)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        settingsManager = mockk(relaxed = true)
        presetRepository = mockk(relaxed = true)
        application = mockk(relaxed = true)

        every { settingsManager.themeFlow } returns themeFlow
        every { settingsManager.currentTheme } returns BrandTheme.Hasselblad
        every { settingsManager.darkMode } returns DarkMode.SYSTEM
        every { settingsManager.isVibrationEnabled } returns true
        every { settingsManager.isAnalyticsEnabled } returns false
        every { settingsManager.isCloudSyncEnabled } returns false
        every { settingsManager.lastSyncTime } returns 0L
        every { settingsManager.defaultStartTab } returns 0
        every { settingsManager.updateChannel } returns UpdateChannel.GITEE
        every { settingsManager.floatingWindowOpacity } returns 56

        viewModel = SettingsViewModel(application, settingsManager, presetRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初始化时主题为Hasselblad`() = runTest {
        assertEquals(BrandTheme.Hasselblad, viewModel.currentTheme.value)
    }

    @Test
    fun `设置主题更新状态`() = runTest {
        val newTheme = BrandTheme.Fujifilm
        viewModel.setTheme(newTheme)

        verify { settingsManager.currentTheme = newTheme }
        assertEquals(newTheme, viewModel.currentTheme.value)
    }

    @Test
    fun `设置深色模式更新状态`() = runTest {
        val mode = DarkMode.DARK
        viewModel.setDarkMode(mode)

        verify { settingsManager.darkMode = mode }
        assertEquals(mode, viewModel.darkMode.value)
    }

    @Test
    fun `设置振动开关更新状态`() = runTest {
        viewModel.setVibrationEnabled(false)

        verify { settingsManager.isVibrationEnabled = false }
        assertFalse(viewModel.vibrationEnabled.value)
    }

    @Test
    fun `设置分析开关更新状态`() = runTest {
        viewModel.setAnalyticsEnabled(true)

        verify { settingsManager.isAnalyticsEnabled = true }
        assertTrue(viewModel.analyticsEnabled.value)
    }

    @Test
    fun `设置云同步开关更新状态`() = runTest {
        viewModel.setCloudSyncEnabled(true)

        verify { settingsManager.isCloudSyncEnabled = true }
        assertTrue(viewModel.cloudSyncEnabled.value)
    }

    @Test
    fun `设置默认启动Tab更新状态`() = runTest {
        val tab = 2
        viewModel.setDefaultStartTab(tab)

        verify { settingsManager.defaultStartTab = tab }
        assertEquals(tab, viewModel.defaultStartTab.value)
    }

    @Test
    fun `设置更新渠道更新状态`() = runTest {
        val channel = UpdateChannel.GITHUB
        viewModel.setUpdateChannel(channel)

        verify { settingsManager.updateChannel = channel }
        assertEquals(channel, viewModel.updateChannel.value)
    }

    @Test
    fun `设置浮窗透明度更新状态`() = runTest {
        val opacity = 50
        viewModel.setFloatingWindowOpacity(opacity)

        verify { settingsManager.floatingWindowOpacity = opacity }
        assertEquals(opacity, viewModel.floatingWindowOpacity.value)
    }

    @Test
    fun `格式化最后同步时间返回正确格式`() = runTest {
        every { settingsManager.lastSyncTime } returns System.currentTimeMillis()

        val formatted = viewModel.formatLastSyncTime()

        assertNotNull(formatted)
        assertNotEquals("从未同步", formatted)
    }

    @Test
    fun `从未同步时返回正确文本`() = runTest {
        every { settingsManager.lastSyncTime } returns 0L

        val formatted = viewModel.formatLastSyncTime()

        assertEquals("从未同步", formatted)
    }

    @Test
    fun `清除错误信息`() = runTest {
        viewModel.clearError()

        assertNull(viewModel.errorMessage.value)
    }
}
