# 相机模块使用指南

OMaster 应用集成双路相机架构：CameraX + OPPO 大师模式相机。

---

## 目录

- [架构概述](#架构概述)
- [CameraX 管理器](#camerax-管理器)
- [OPPO 大师模式相机](#oppo-大师模式相机)
- [权限配置](#权限配置)
- [最佳实践](#最佳实践)

---

## 架构概述

### 双路相机设计

```
┌───────────────────────────────────┐
│      OMaster 相机架构             │
├───────────────────────────────────┤
│                                   │
│  ┌─────────────┐  ┌────────────┐ │
│  │  CameraX    │  │ OPPOCamera │ │
│  │  Manager    │  │ Manager    │ │
│  └─────────────┘  └────────────┘ │
│        ↓                ↓        │
│  ┌─────────────────────────────┐ │
│  │     OPPO 设备检测           │ │
│  │  - OPPO/OnePlus/Realme      │ │
│  │  - ColorOS 版本检测         │ │
│  └─────────────────────────────┘ │
│        ↓                ↓        │
│  ┌─────────────────────────────┐ │
│  │     相机模式选择             │ │
│  │  - Master Mode (OPPO)       │ │
│  │  - Standard Mode (CameraX)  │ │
│  └─────────────────────────────┘ │
│                                   │
└───────────────────────────────────┘
```

---

## CameraX 管理器

### 核心功能

- 标准相机预览和拍照
- 视频录制
- 闪光灯、变焦、对焦控制
- 前后相机切换
- 生命周期管理

### 使用示例

```kotlin
// 获取 CameraXManager 实例
val cameraManager = CameraXManager.getInstance(context)

// 启动预览
cameraManager.startPreview(previewView)

// 拍照
cameraManager.takePhoto().observe(this) { result ->
    if (result.success) {
        val imagePath = result.imagePath
        // 处理拍摄的照片
    }
}

// 视频录制
cameraManager.startVideoRecording()
// 延迟5秒后停止
cameraManager.stopVideoRecording()

// 切换相机
cameraManager.switchCamera()

// 设置闪光灯
cameraManager.setFlashMode(FlashMode.AUTO)

// 设置变焦
cameraManager.setZoomRatio(2.0f)
```

### 生命周期管理

```kotlin
override fun onResume() {
    super.onResume()
    cameraManager.startPreview()
}

override fun onPause() {
    super.onPause()
    cameraManager.stopPreview()
}

override fun onDestroy() {
    super.onDestroy()
    cameraManager.release()
}
```

---

## OPPO 大师模式相机

### 设备检测

```kotlin
val oppoManager = OPPOCameraManager.getInstance(context)

// 检测是否为 OPPO 设备
if (oppoManager.isOPPODevice()) {
    // 使用大师模式
    oppoManager.launchCameraWithMasterMode()
} else {
    // 使用 CameraX 标准模式
    cameraManager.startPreview()
}
```

### ColorOS 版本检测

```kotlin
// 检测 ColorOS 版本
val colorOSVersion = oppoManager.detectColorOSVersion()

// 大师模式需要 ColorOS 12+
if (colorOSVersion >= 12) {
    oppoManager.setMasterParams(hasselbladParams)
}
```

### 大师模式参数

```kotlin
val hasselbladParams = HasselbladParams(
    tone = -3,           // 色调调整 (-30~+30)
    saturation = 10,     // 饱和度 (-30~+30)
    contrast = -15,      // 对比度 (-30~+30)
    colorTemp = -5,      // 色温 (-30~+30)
    sharpness = -15,     // 锐度 (-30~+30)
    vignette = 20,       // 暗角 (-30~+30)
    cyanMagenta = -5,    // 青/洋红 (-30~+30)
    softLight = SoftLightMode.SOFT  // 柔光模式
)

// 应用大师参数
oppoManager.setMasterParams(hasselbladParams)
```

### 支持的设备

- OPPO Find X 系列
- OPPO Reno 系列
- OnePlus 9/10/11/12 系列
- Realme GT 系列

---

## 权限配置

### AndroidManifest.xml

```xml
<!-- 相机权限 -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- 录制音频权限（视频录制） -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- 存储权限 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
```

### 运行时权限请求

```kotlin
val permissions = listOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_MEDIA_IMAGES
)

// 使用 ActivityResultContracts 请求权限
val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { results ->
    if (results.all { it.value }) {
        // 权限已授予，启动相机
        cameraManager.startPreview()
    } else {
        // 显示权限说明
        showPermissionExplanation()
    }
}

permissionLauncher.launch(permissions)
```

---

## 最佳实践

### 1. 按需请求权限

```kotlin
// 仅在用户点击"拍照"按钮时请求相机权限
fun onTakePhotoClicked() {
    if (checkSelfPermission(CAMERA) == PERMISSION_GRANTED) {
        cameraManager.takePhoto()
    } else {
        requestCameraPermission()
    }
}
```

### 2. 处理权限拒绝

```kotlin
fun onPermissionDenied() {
    if (shouldShowRequestPermissionRationale(CAMERA)) {
        // 用户之前拒绝过，显示说明
        showDialog(
            "相机权限用于拍摄照片，请授予权限以使用此功能。",
            onPositive = { requestCameraPermission() }
        )
    } else {
        // 用户选择"不再询问"，引导到设置
        openAppSettings()
    }
}
```

### 3. 相机异常处理

```kotlin
cameraManager.setErrorListener { error ->
    when (error) {
        CameraError.CAMERA_BUSY -> showToast("相机正在使用中")
        CameraError.CAMERA_DISCONNECTED -> showToast("相机已断开")
        CameraError.CAMERA_DISABLED -> showToast("相机已被禁用")
        else -> showToast("相机错误: ${error.message}")
    }
}
```

### 4. 预览分辨率优化

```kotlin
// 根据设备性能选择预览分辨率
val previewResolution = when (DevicePerformanceHelper.getPerformanceLevel()) {
    PerformanceLevel.HIGH -> Resolution(4K_WIDTH, 4K_HEIGHT)
    PerformanceLevel.MEDIUM -> Resolution(1080P_WIDTH, 1080P_HEIGHT)
    PerformanceLevel.LOW -> Resolution(720P_WIDTH, 720P_HEIGHT)
}

cameraManager.setPreviewResolution(previewResolution)
```

---

## 测试覆盖

### 单元测试

- `CameraXManagerTest.kt`: CameraX 功能测试
- `OPPOCameraManagerTest.kt`: OPPO 大师模式测试

### 集成测试

- `CameraIntegrationTest.kt`: 相机启动和拍照流程测试
- `PermissionFlowTest.kt`: 权限请求流程测试

---

## 故障排查

### 问题 1: 相机无法启动

**症状**: 点击拍照按钮无响应

**原因**:
- 权限未授予
- 相机被其他应用占用
- 设备无相机硬件

**解决方案**:
```kotlin
fun checkCameraAvailability(): Boolean {
    if (!hasCameraPermission()) {
        requestCameraPermission()
        return false
    }

    if (!deviceHasCamera()) {
        showToast("设备无相机硬件")
        return false
    }

    return true
}
```

### 问题 2: OPPO 大师模式不可用

**症状**: 非OPPO设备上大师模式按钮禁用

**原因**: 设备不是 OPPO/OnePlus/Realme

**解决方案**: 自动降级到 CameraX 标准模式
```kotlin
if (!oppoManager.isOPPODevice()) {
    // 禁用大师模式按钮
    masterModeButton.enabled = false
    // 使用 CameraX 标准模式
    useCameraXStandardMode()
}
```

### 问题 3: 视频录制失败

**症状**: 视频录制开始后立即停止

**原因**: 存储权限或音频权限未授予

**解决方案**: 检查所有必需权限
```kotlin
fun checkVideoRecordingPermissions(): Boolean {
    return hasCameraPermission() &&
           hasAudioPermission() &&
           hasStoragePermission()
}
```

---

## API 参考

### CameraXManager

```kotlin
class CameraXManager {
    // 单例获取
    fun getInstance(context: Context): CameraXManager

    // 预览控制
    fun startPreview(previewView: PreviewView)
    fun stopPreview()

    // 拍照
    fun takePhoto(): LiveData<CaptureResult>

    // 视频录制
    fun startVideoRecording()
    fun stopVideoRecording(): LiveData<VideoResult>

    // 相机切换
    fun switchCamera()

    // 参数设置
    fun setFlashMode(mode: FlashMode)
    fun setZoomRatio(ratio: Float)
    fun setFocusMode(mode: FocusMode)
    fun setPreviewResolution(resolution: Resolution)

    // 错误监听
    fun setErrorListener(listener: (CameraError) -> Unit)

    // 生命周期
    fun release()
}
```

### OPPOCameraManager

```kotlin
class OPPOCameraManager {
    // 单例获取
    fun getInstance(context: Context): OPPOCameraManager

    // 设备检测
    fun isOPPODevice(): Boolean
    fun detectColorOSVersion(): Int

    // 功能检测
    fun isMasterModeAvailable(): Boolean
    fun isProModeAvailable(): Boolean

    // 大师模式
    fun launchCameraWithMasterMode(): LiveData<LaunchResult>
    fun setMasterParams(params: HasselbladParams)

    // 专业模式
    fun setProModeParams(params: Map<String, Any>)

    // 功能列表
    fun getCameraCapabilities(): List<String>
    fun getHasselbladFeatureList(): List<String>
}
```

---

**文档版本**: v2.3.6  
**更新日期**: 2026-07-03