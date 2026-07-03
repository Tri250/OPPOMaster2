# OMaster Android 第三次深度链路自检报告

**审计日期**: 2026-06-23  
**审计焦点**: 端到端完整链路 — 排除任何"模拟/简化/空实现"  
**审计范围**: 6 条核心业务链路

---

## 一、执行摘要

本次"链路完整性"深度审计，针对"功能真实、实用、不是模拟、不是空实现、不是简化版、完整版链路"的要求，验证了 6 条核心业务端到端链路：

**本次发现并修复 1 项严重 bug**（会在 Android 14+ 真机启动悬浮窗时崩溃）。

**6 条核心业务链路均验证为真实端到端闭环，无模拟/简化。**

---

## 二、6 条核心链路完整审计

### 链路 1: AI 微调（云端增强 → 本地推理 → 应用到参数）

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| 用户触发 | [AIFineTuneScreen.kt:575-624](file:///workspace/app/src/main/java/com/silas/omaster/ui/features/AIFineTuneScreen.kt#L575-L624) `onAutoTune` | ✅ |
| 端云协同策略 | [AIFineTuneManager.kt:199-300](file:///workspace/app/src/main/java/com/silas/omaster/ai/AIFineTuneManager.kt#L199-L300) `generateAISuggestion` | ✅ |
| 云端推理 | `generateCloudSuggestion` Ktor HTTPS POST 到 `API_CLOUD_SCENE_ANALYZE` | ✅ |
| 本地推理 | `generateLocalSuggestionFromImage` 委托给 `MasterInferenceEngine.analyzeImage` | ✅ |
| 启发式分析 | `HeuristicSceneAnalyzer` 真实提取颜色直方图/亮度/边缘/ML Kit 人脸 | ✅ |
| 结果应用 | UI 端 `renderParams = updateRenderParam(renderParams, ...)` 实际更新参数 | ✅ |
| 用户应用 | "应用"按钮调 `onApply(renderParams)` 传出真实参数 | ✅ |

**结论**: ✅ **完整版链路，无模拟/简化**。AI 建议真实影响最终输出。

---

### 链路 2: 云端预设同步（拉取 → 解析 → 缓存 → 展示）

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| URL 配置 | [UrlConstants.kt:50-55](file:///workspace/app/src/main/java/com/silas/omaster/util/UrlConstants.kt#L50-L55) 4 个品牌 HTTPS URL | ✅ |
| HTTPS 拉取 | [CloudSyncManager.kt:163-168](file:///workspace/app/src/main/java/com/silas/omaster/cloud/CloudSyncManager.kt#L163-L168) Ktor `httpClient.get` | ✅ |
| JSON 解析 | `syncBrandPresets` 解析 `version/build/presets` 字段 | ✅ |
| 品牌同步 | 4 个品牌逐个同步，单个失败不影响其他 | ✅ |
| 缓存 | `saveToCache` Gson → SharedPreferences | ✅ |
| 状态 | `_syncState`/`_lastSyncTime` StateFlow 实时更新 | ✅ |
| 24h 节流 | `shouldSync()` 自动节流 | ✅ |

**结论**: ✅ **完整版链路**。CDN 拉取 → 解析 → 缓存 → 状态广播全闭环。

---

### 链路 3: 预设创建 → 保存 → 加载 → 应用

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| 用户输入 | [UniversalCreatePresetScreen.kt:73-83](file:///workspace/app/src/main/java/com/silas/omaster/ui/create/UniversalCreatePresetScreen.kt#L73-L83) | ✅ |
| 图片保存 | `saveImageToInternalStorage` 复制到应用私有目录 | ✅ |
| 数据组装 | `sectionsToParams` 将 UI sections 转为参数 Map | ✅ |
| 持久化 | [UniversalCreatePresetViewModel.kt:224-229](file:///workspace/app/src/main/java/com/silas/omaster/ui/create/UniversalCreatePresetViewModel.kt#L224-L229) `repository.createCustomPreset` | ✅ |
| 加载 | [PresetRepository.kt:160](file:///workspace/app/src/main/java/com/silas/omaster/data/repository/PresetRepository.kt#L160) `loadPresets` | ✅ |
| 应用 | `applyPresetToFrame` 真实应用参数到 Bitmap | ✅ |

**结论**: ✅ **完整版链路**。

---

### 链路 4: 摄像头采集 → 实时分析 → 预览

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| CameraX 初始化 | [CameraXManager.kt:80-95](file:///workspace/app/src/main/java/com/silas/omaster/ui/features/CameraXManager.kt#L80-L95) `startCamera` | ✅ |
| Preview/ImageCapture/ImageAnalysis 绑定 | 同上 `bindCameraUseCases` | ✅ |
| 实时帧回调 | `analyzeFrame` 收到 `ImageProxy` | ✅ |
| ImageProxy → Bitmap | `imageProxyToBitmap` 真实转换 | ✅ |
| 预设应用 | `applyPresetToFrame` ColorMatrix 真实滤镜 | ✅ |
| 回调到 UI | `onFrameAnalyzed` 切主线程回调 | ✅ |
| 资源回收 | 无订阅者时回收 Bitmap | ✅ |

**结论**: ✅ **完整版链路**。实时滤镜真实工作。

---

### 链路 5: 图片导入 → 加载 → 编辑 → 导出

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| 用户选择 | `PickVisualMedia` 启动器 | ✅ |
| Bitmap 解码 | `BitmapFactory.decodeStream` 真实解码 | ✅ |
| AI 微调 / 参数调节 | `applyColorMatrixToBitmap` 应用参数 | ✅ |
| 保存到相册 | [AIFineTuneScreen.kt:1515-1559](file:///workspace/app/src/main/java/com/silas/omaster/ui/features/AIFineTuneScreen.kt#L1515-L1559) `saveImageToGallery` | ✅ |
| Android 10+ | MediaStore.EXTERNAL_CONTENT_URI | ✅ |
| Android 9- | 应用私有目录 + MediaScannerConnection | ✅ |
| JPEG 95 质量 | `filteredBitmap.compress(JPEG, 95, out)` | ✅ |

**结论**: ✅ **完整版链路**。

---

### 链路 6: 悬浮窗（启动 → 渲染 → 交互 → 关闭）

| 阶段 | 实现位置 | 状态 |
|------|----------|------|
| 权限检查 | [FloatingWindowService.kt:138-142](file:///workspace/app/src/main/java/com/silas/omaster/ui/service/FloatingWindowService.kt#L138-L142) `canDrawOverlays` | ✅ |
| 启动前台服务 | `ContextCompat.startForegroundService` | ✅ |
| Service onCreate | [FloatingWindowService.kt:193-210](file:///workspace/app/src/main/java/com/silas/omaster/ui/service/FloatingWindowService.kt#L193-L210) | ✅（本轮修复）|
| 通知渠道 | `NotificationChannel` O+ 创建 | ✅ |
| WindowManager 添加视图 | `windowManager.addView` 真实添加悬浮窗 | ✅ |
| 数据传递 | Parcelable ArrayList sections | ✅ |
| 用户交互 | 点击切换预设/拖动悬浮球 | ✅ |
| 关闭 | `stopService` | ✅ |

**结论**: ✅ **完整版链路**（本轮修复 1 项 Android 14+ 崩溃 bug）。

---

## 三、本轮修复内容

### 🔴 高危：Android 14+ 悬浮窗启动崩溃

**问题**:
```kotlin
// 旧代码 - Android 14+ 必崩
startForeground(NOTIFICATION_ID, buildNotification())
```

**触发条件**:
- targetSdk=34+（项目为 36）
- foregroundServiceType="specialUse"
- 未传递 FOREGROUND_SERVICE_TYPE_SPECIAL_USE 参数

**异常**:
```
ForegroundServiceTypeException: 
  Starting FGS of type specialUse is not allowed. 
  Use ServiceCompat.startForeground() with the type.
```

**修复**:
```kotlin
// 新代码 - 兼容 Android 14+
ServiceCompat.startForeground(
    this, NOTIFICATION_ID, buildNotification(),
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    } else 0
)
```

### 🟡 中危：Google Play 审核合规

**问题**: Android 14+ 要求 `specialUse` 类型 FGS 必须在 manifest 声明 `<property>` 元素说明用途。

**修复**: 在 [AndroidManifest.xml:54-66](file:///workspace/app/src/main/AndroidManifest.xml#L54-L66) 添加：
```xml
<property
    android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
    android:value="camera_preset_overlay" />
```

---

## 四、第二次修复回顾（仍生效）

| 修复 | 文件 | 状态 |
|------|------|------|
| 云存储验证改为真实 HTTP 请求 | [CloudSyncScreen.kt](file:///workspace/app/src/main/java/com/silas/omaster/ui/features/CloudSyncScreen.kt) | ✅ |
| 直方图改为确定性 EV 计算 | [ParamAdjustScreen.kt](file:///workspace/app/src/main/java/com/silas/omaster/ui/features/ParamAdjustScreen.kt) | ✅ |
| TFLite 注释全部修正 | AI 模块多个文件 | ✅ |
| 预测性返回动画启用 | AndroidManifest + WelcomeFlow | ✅ |
| 4 处图片选择器迁移 | 4 个 Screen | ✅ |
| WebDAV 添加 Depth: 0 头 | CloudSyncScreen | ✅ |
| 失败时保留用户输入 API Key | CloudSyncScreen | ✅ |
| HttpURLConnection 资源 finally 释放 | CloudSyncScreen | ✅ |
| RenderRequest ID 改用 AtomicLong | GPURenderManager | ✅ |

---

## 五、模拟/空实现/简化版最终扫描

### 搜索结果（无违规）

| 关键词 | 命中 | 是否违规 |
|--------|------|----------|
| `GetContent()` | 0 | ✅ 完全迁移到 PickVisualMedia |
| `placeholder` | 20+ | ✅ 全为 Compose TextField/Image 标准参数 + 资源文件名 |
| `模拟` (模拟大光圈 / 模拟 HDR 压缩 / 模拟曝光补偿) | 3 | ✅ 算法描述性文字，不是功能模拟 |
| `not.implemented` | 0 | ✅ |
| `Random` 真实像素噪声 | 1 | ✅ `HasselbladColorEngine.applyGrain` 真实胶片颗粒 |
| `fake/mock/stub` | 0 | ✅ |
| `TFLite` 误导性 | 0 | ✅ 仅保留"未来计划" |
| `// 实际不工作 / 占位` | 0 | ✅ |

### 真实链路证据点

- AI 微调：`HeuristicSceneAnalyzer.kt` 真实采样 3 个区域颜色直方图 + 真实 ML Kit FaceDetector
- GPU 渲染：`GPURenderManager.applyPresetToFrame` 真实 ColorMatrix 链式合成
- 相机：`CameraXManager.analyzeFrame` 真实处理 ImageProxy
- 云同步：`CloudSyncManager.sync` 真实 Ktor HTTPS 请求
- 悬浮窗：`FloatingWindowService.onCreate` 真实 WindowManager 添加 View
- 导出：`saveImageToGallery` 真实 MediaStore 写入

---

## 六、最终结论

**OMaster Android v1.9.0 满足 2026 正式版自检要求：**

- ✅ **真实可用**：6 条核心链路全部端到端真实实现，无任何模拟
- ✅ **实用完整**：每条链路从用户输入 → 业务处理 → 数据持久化 → 结果输出完整闭环
- ✅ **非空实现**：无 TODO/FIXME/stub/空函数体；所有"模拟"关键词均为合理技术术语
- ✅ **非简化版**：未启用任何简化模式、占位实现、欺骗性声明
- ✅ **Android 16 兼容**：compileSdk/targetSdk=36、预测性返回、16 KB Page Size 验证、Photo Picker、specialUse FGS 全部适配
- ✅ **可发布**：建议正式发布前在 Android 14+ / Android 16 真机做最终回归测试

**自检通过。**
