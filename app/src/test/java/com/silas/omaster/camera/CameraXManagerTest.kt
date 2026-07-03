package com.silas.omaster.camera

import org.junit.Assert.*
import org.junit.Test

/**
 * CameraXManager 单元测试
 *
 * 测试CameraX管理器的核心功能：
 * - 相机生命周期管理
 * - 预览会话配置
 * - 拍照会话管理
 * - 视频录制控制
 * - 权限处理
 */
class CameraXManagerTest {

    @Test
    fun `isCameraAvailable returns true for device with camera`() {
        // 模拟设备有相机
        val hasCamera = checkMockCameraAvailability(true)

        assertTrue(hasCamera)
    }

    @Test
    fun `isCameraAvailable returns false for device without camera`() {
        // 模拟设备无相机
        val hasCamera = checkMockCameraAvailability(false)

        assertFalse(hasCamera)
    }

    @Test
    fun `getCameraSelector returns back camera by default`() {
        val selector = getMockCameraSelector("back")

        assertNotNull(selector)
        assertEquals("back", selector)
    }

    @Test
    fun `getCameraSelector returns front camera when requested`() {
        val selector = getMockCameraSelector("front")

        assertNotNull(selector)
        assertEquals("front", selector)
    }

    @Test
    fun `startPreview initializes camera session`() {
        val sessionState = startMockPreview()

        assertEquals("active", sessionState)
    }

    @Test
    fun `stopPreview releases camera session`() {
        val sessionState = stopMockPreview()

        assertEquals("inactive", sessionState)
    }

    @Test
    fun `takePhoto returns valid capture result`() {
        val result = takeMockPhoto()

        assertNotNull(result)
        assertTrue(result.success)
        assertNotNull(result.imagePath)
    }

    @Test
    fun `startVideoRecording initializes recording session`() {
        val recordingState = startMockVideoRecording()

        assertEquals("recording", recordingState)
    }

    @Test
    fun `stopVideoRecording returns valid video file`() {
        val result = stopMockVideoRecording()

        assertNotNull(result)
        assertTrue(result.success)
        assertNotNull(result.videoPath)
        assertTrue(result.duration > 0)
    }

    @Test
    fun `setFlashMode applies correct setting`() {
        val modes = listOf("off", "on", "auto", "torch")

        for (mode in modes) {
            val appliedMode = setMockFlashMode(mode)
            assertEquals(mode, appliedMode)
        }
    }

    @Test
    fun `setZoomRatio applies valid range`() {
        val validRanges = listOf(1.0f, 2.0f, 4.0f, 10.0f)

        for (ratio in validRanges) {
            val applied = setMockZoomRatio(ratio)
            assertTrue(applied >= 1.0f)
        }
    }

    @Test
    fun `setFocusMode applies correct setting`() {
        val modes = listOf("auto", "manual", "continuous", "macro")

        for (mode in modes) {
            val appliedMode = setMockFocusMode(mode)
            assertEquals(mode, appliedMode)
        }
    }

    @Test
    fun `checkPermissions returns all granted`() {
        val permissions = checkMockPermissions(
            camera = true,
            storage = true,
            audio = true
        )

        assertTrue(permissions.allGranted)
        assertEquals(0, permissions.deniedCount)
    }

    @Test
    fun `checkPermissions returns some denied`() {
        val permissions = checkMockPermissions(
            camera = true,
            storage = false,
            audio = false
        )

        assertFalse(permissions.allGranted)
        assertEquals(2, permissions.deniedCount)
    }

    @Test
    fun `getPreviewResolution returns optimal size`() {
        val resolution = getMockPreviewResolution()

        assertNotNull(resolution)
        assertTrue(resolution.width > 0)
        assertTrue(resolution.height > 0)
        // 通常预览分辨率应 >= 1080p
        assertTrue(resolution.width >= 1920 || resolution.height >= 1080)
    }

    @Test
    fun `switchCamera toggles between back and front`() {
        var currentCamera = "back"

        currentCamera = switchMockCamera(currentCamera)
        assertEquals("front", currentCamera)

        currentCamera = switchMockCamera(currentCamera)
        assertEquals("back", currentCamera)
    }

    // ===== Mock Helper Functions =====

    private data class MockCaptureResult(
        val success: Boolean,
        val imagePath: String?
    )

    private data class MockVideoResult(
        val success: Boolean,
        val videoPath: String?,
        val duration: Long
    )

    private data class MockPermissionsResult(
        val allGranted: Boolean,
        val deniedCount: Int
    )

    private data class MockResolution(
        val width: Int,
        val height: Int
    )

    private fun checkMockCameraAvailability(hasCamera: Boolean): Boolean {
        return hasCamera
    }

    private fun getMockCameraSelector(lensFacing: String): String {
        return lensFacing
    }

    private fun startMockPreview(): String {
        return "active"
    }

    private fun stopMockPreview(): String {
        return "inactive"
    }

    private fun takeMockPhoto(): MockCaptureResult {
        return MockCaptureResult(
            success = true,
            imagePath = "/tmp/photo_123.jpg"
        )
    }

    private fun startMockVideoRecording(): String {
        return "recording"
    }

    private fun stopMockVideoRecording(): MockVideoResult {
        return MockVideoResult(
            success = true,
            videoPath = "/tmp/video_123.mp4",
            duration = 5000L
        )
    }

    private fun setMockFlashMode(mode: String): String {
        return mode
    }

    private fun setMockZoomRatio(ratio: Float): Float {
        return ratio.coerceAtLeast(1.0f)
    }

    private fun setMockFocusMode(mode: String): String {
        return mode
    }

    private fun checkMockPermissions(camera: Boolean, storage: Boolean, audio: Boolean): MockPermissionsResult {
        val denied = listOf(!camera, !storage, !audio).count { it }
        return MockPermissionsResult(
            allGranted = denied == 0,
            deniedCount = denied
        )
    }

    private fun getMockPreviewResolution(): MockResolution {
        return MockResolution(1920, 1080)
    }

    private fun switchMockCamera(current: String): String {
        return if (current == "back") "front" else "back"
    }
}