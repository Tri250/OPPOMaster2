package com.silas.omaster.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.silas.omaster.data.local.DarkMode
import com.silas.omaster.data.local.SettingsManager
import com.silas.omaster.data.local.UpdateChannel
import com.silas.omaster.data.repository.PresetRepository
import com.silas.omaster.ui.settings.SettingsViewModel
import com.silas.omaster.ui.theme.BrandTheme
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
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * SettingsViewModel 单元测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var settingsManager: SettingsManager

    @Mock
    private lateinit var presetRepository: PresetRepository

    private lateinit var viewModel: SettingsViewModel

    private val themeFlow = MutableStateFlow(BrandTheme.Hasselblad)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        whenever(settingsManager.themeFlow).thenReturn(themeFlow)
        whenever(settingsManager.currentTheme).thenReturn(BrandTheme.Hasselblad)
        whenever(settingsManager.darkMode).thenReturn(DarkMode.SYSTEM)
        whenever(settingsManager.isVibrationEnabled).thenReturn(true)
        whenever(settingsManager.isAnalyticsEnabled).thenReturn(false)
        whenever(settingsManager.isCloudSyncEnabled).thenReturn(false)
        whenever(settingsManager.lastSyncTime).thenReturn(0L)
        whenever(settingsManager.defaultStartTab).thenReturn(0)
        whenever(settingsManager.updateChannel).thenReturn(UpdateChannel.STABLE)
        whenever(settingsManager.floatingWindowOpacity).thenReturn(0.9f)
        
        viewModel = SettingsViewModel(settingsManager, presetRepository)
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
        val newTheme = BrandTheme.Fuji
        viewModel.setTheme(newTheme)
        
        verify(settingsManager).currentTheme = newTheme
        assertEquals(newTheme, viewModel.currentTheme.value)
    }

    @Test
    fun `设置深色模式更新状态`() = runTest {
        val mode = DarkMode.DARK
        viewModel.setDarkMode(mode)
        
        verify(settingsManager).darkMode = mode
        assertEquals(mode, viewModel.darkMode.value)
    }

    @Test
    fun `设置振动开关更新状态`() = runTest {
        viewModel.setVibrationEnabled(false)
        
        verify(settingsManager).isVibrationEnabled = false
        assertFalse(viewModel.vibrationEnabled.value)
    }

    @Test
    fun `设置分析开关更新状态`() = runTest {
        viewModel.setAnalyticsEnabled(true)
        
        verify(settingsManager).isAnalyticsEnabled = true
        assertTrue(viewModel.analyticsEnabled.value)
    }

    @Test
    fun `设置云同步开关更新状态`() = runTest {
        viewModel.setCloudSyncEnabled(true)
        
        verify(settingsManager).isCloudSyncEnabled = true
        assertTrue(viewModel.cloudSyncEnabled.value)
    }

    @Test
    fun `设置默认启动Tab更新状态`() = runTest {
        val tab = 2
        viewModel.setDefaultStartTab(tab)
        
        verify(settingsManager).defaultStartTab = tab
        assertEquals(tab, viewModel.defaultStartTab.value)
    }

    @Test
    fun `设置更新渠道更新状态`() = runTest {
        val channel = UpdateChannel.BETA
        viewModel.setUpdateChannel(channel)
        
        verify(settingsManager).updateChannel = channel
        assertEquals(channel, viewModel.updateChannel.value)
    }

    @Test
    fun `设置浮窗透明度更新状态`() = runTest {
        val opacity = 0.5f
        viewModel.setFloatingWindowOpacity(opacity)
        
        verify(settingsManager).floatingWindowOpacity = opacity
        assertEquals(opacity, viewModel.floatingWindowOpacity.value)
    }

    @Test
    fun `格式化最后同步时间返回正确格式`() = runTest {
        whenever(settingsManager.lastSyncTime).thenReturn(System.currentTimeMillis())
        
        val formatted = viewModel.formatLastSyncTime()
        
        assertNotNull(formatted)
        assertNotEquals("从未同步", formatted)
    }

    @Test
    fun `从未同步时返回正确文本`() = runTest {
        whenever(settingsManager.lastSyncTime).thenReturn(0L)
        
        val formatted = viewModel.formatLastSyncTime()
        
        assertEquals("从未同步", formatted)
    }

    @Test
    fun `清除错误信息`() = runTest {
        viewModel.clearError()
        
        assertNull(viewModel.errorMessage.value)
    }
}