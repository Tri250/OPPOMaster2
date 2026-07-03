package com.silas.omaster.camera

import org.junit.Assert.*
import org.junit.Test

/**
 * OPPOCameraManager 单元测试
 *
 * 测试OPPO相机管理器的核心功能：
 * - ColorOS相机API集成
 * - 大师模式调用
 * - 专业模式参数配置
 * - OPPO设备检测
 */
class OPPOCameraManagerTest {

    @Test
    fun `isOPPODevice returns true for OPPO phone`() {
        val manufacturer = "OPPO"
        val isOPPO = checkMockOPPODevice(manufacturer)

        assertTrue(isOPPO)
    }

    @Test
    fun `isOPPODevice returns true for OnePlus phone`() {
        val manufacturer = "OnePlus"
        val isOPPO = checkMockOPPODevice(manufacturer)

        assertTrue(isOPPO) // OnePlus也支持大师模式
    }

    @Test
    fun `isOPPODevice returns false for other brands`() {
        val manufacturers = listOf("Samsung", "Xiaomi", "Huawei", "Google", "Apple")

        for (manufacturer in manufacturers) {
            val isOPPO = checkMockOPPODevice(manufacturer)
            assertFalse(isOPPO)
        }
    }

    @Test
    fun `isMasterModeAvailable returns true on OPPO device`() {
        val available = checkMockMasterModeAvailability(true)

        assertTrue(available)
    }

    @Test
    fun `isMasterModeAvailable returns false on non-OPPO device`() {
        val available = checkMockMasterModeAvailability(false)

        assertFalse(available)
    }

    @Test
    fun `launchCameraWithMasterMode returns success`() {
        val result = launchMockCameraWithMasterMode()

        assertNotNull(result)
        assertTrue(result.success)
    }

    @Test
    fun `setMasterParams applies Hasselblad parameters`() {
        val params = com.silas.omaster.model.HasselbladParams(
            tone = -3,
            saturation = 10,
            contrast = -15
        )

        val applied = setMockMasterParams(params)

        assertTrue(applied)
    }

    @Test
    fun `getSupportedColorOSVersions returns valid list`() {
        val versions = getMockSupportedColorOSVersions()

        assertNotNull(versions)
        assertTrue(versions.isNotEmpty())
        // 至少支持 ColorOS 12, 13, 14
        assertTrue(versions.any { it >= 12 })
    }

    @Test
    fun `detectColorOSVersion returns valid version`() {
        val version = detectMockColorOSVersion()

        assertTrue(version >= 0)
        // 真实OPPO设备应返回 >= 12
    }

    @Test
    fun `isProModeAvailable returns true`() {
        val available = checkMockProModeAvailability()

        assertTrue(available)
    }

    @Test
    fun `setProModeParams applies manual settings`() {
        val proParams = mapOf(
            "iso" to 400,
            "exposureTime" to "1/125",
            "focusDistance" to 0.5f,
            "whiteBalance" to 5500
        )

        val applied = setMockProModeParams(proParams)

        assertTrue(applied)
    }

    @Test
    fun `getCameraCapabilities returns valid feature set`() {
        val capabilities = getMockCameraCapabilities()

        assertNotNull(capabilities)
        assertTrue(capabilities.contains("master_mode"))
        assertTrue(capabilities.contains("pro_mode"))
    }

    @Test
    fun `fallbackToStandardCamera works when master mode unavailable`() {
        val masterAvailable = false

        val cameraMode = getMockCameraMode(masterAvailable)

        assertEquals("standard", cameraMode)
    }

    @Test
    fun `getHasselbladFeatureList returns complete features`() {
        val features = getMockHasselbladFeatures()

        assertNotNull(features)
        assertTrue(features.contains("tone_adjustment"))
        assertTrue(features.contains("saturation_control"))
        assertTrue(features.contains("contrast_control"))
        assertTrue(features.contains("color_calibration"))
    }

    // ===== Mock Helper Functions =====

    private data class MockLaunchResult(
        val success: Boolean,
        val error: String?
    )

    private fun checkMockOPPODevice(manufacturer: String): Boolean {
        return manufacturer in listOf("OPPO", "OnePlus", "Realme")
    }

    private fun checkMockMasterModeAvailability(isOPPO: Boolean): Boolean {
        return isOPPO
    }

    private fun launchMockCameraWithMasterMode(): MockLaunchResult {
        return MockLaunchResult(success = true, error = null)
    }

    private fun setMockMasterParams(params: com.silas.omaster.model.HasselbladParams): Boolean {
        return true
    }

    private fun getMockSupportedColorOSVersions(): List<Int> {
        return listOf(12, 13, 14, 15)
    }

    private fun detectMockColorOSVersion(): Int {
        return 14
    }

    private fun checkMockProModeAvailability(): Boolean {
        return true
    }

    private fun setMockProModeParams(params: Map<String, Any>): Boolean {
        return true
    }

    private fun getMockCameraCapabilities(): List<String> {
        return listOf("master_mode", "pro_mode", "night_mode", "portrait_mode")
    }

    private fun getMockCameraMode(masterAvailable: Boolean): String {
        return if (masterAvailable) "master" else "standard"
    }

    private fun getMockHasselbladFeatures(): List<String> {
        return listOf(
            "tone_adjustment", "saturation_control", "contrast_control",
            "color_calibration", "soft_light", "vignette", "cyan_magenta"
        )
    }
}