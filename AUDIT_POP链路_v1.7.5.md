# v1.7.5 Android 端「预设详情页选预设 → 直接 POP 拍摄」链路深度审计报告

- 审计对象：tag `v1.7.5`（commit `7117515`，仓库当前 HEAD 即发布基线，无增量提交）
- 审计方式：全链路代码通读 + 对真实资产数据 `app/src/main/assets/presets.json` 的等价逻辑仿真
- 环境说明：沙箱无 Android SDK，未执行 Gradle 编译/单测；结论基于代码事实与纯函数等价移植仿真（`PresetColorTransform` / `PresetCapturePlan` / `PresetPixelEffects` 均为纯 JVM 逻辑，可确定性复现）

---

## 一、总体结论

| 维度 | 结论 |
|---|---|
| 链路是否完整（代码接通） | ✅ 完整。入口 → 路由 → 取预设 → 权限 → 相机管线 → 拍摄 → 拍后套色 → 保存相册，全段真实存在且互相接通 |
| 是否真实有效 | ⚠️ 管线真实，但**核心效果（预设套色）对真实数据空转** |
| 是否达到预期效果 | ❌ 未达到。25/25 内置预设与全部应用内自建预设，套色矩阵恒为单位矩阵、暗角/锐度恒 0、参数卡恒空；UI 仍提示"已套用「X」风格"，构成虚假宣称 |

**一句话**：拍摄/保存的"管子"是真的，但"水"（预设调色参数）根本没接上——数据模型在 `sections`（展示字段）与 typed 字段（引擎消费字段）之间断裂，且无任何桥接代码、无任何测试拦截。

---

## 二、链路逐段核验（全部真实代码，附位置）

### 1. 入口：预设详情页相机按钮
- [DetailScreen.kt](file:///workspace/app/src/main/java/com/silas/omaster/ui/detail/DetailScreen.kt#L125-L139)：顶栏相机 IconButton，条件 `preset != null && onPopCapture != null`，点击 `onPopCapture(preset.id)`（含确认震感）。
- [MainActivity.kt](file:///workspace/app/src/main/java/com/silas/omaster/MainActivity.kt#L685-L702)：`composable<Screen.Detail>` 接线 `navController.navigate(Screen.PopCapture(presetId))`。
- [Screen.kt](file:///workspace/app/src/main/java/com/silas/omaster/navigation/Screen.kt#L63-L65)：`PopCapture(val presetId)` 类型安全路由，单测 `ScreenRouteSerializationTest` 覆盖序列化往返。
- 可发现性备注：入口为顶栏裸图标（contentDescription「POP 即拍」），无文字标签，弱于底部主按钮。

### 2. 预设装载
- [PopCaptureHost.kt](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureHost.kt#L18-L46)：按 id 经 `PresetRepository.getPresetById` 加载（内置走 `JsonUtil`，自定义走 `CustomPresetManager`），加载中/预设缺失态完备，缺失时给返回按钮。
- ID 一致性无问题：`JsonUtil.processPresets` 对无 id 预设生成稳定 id，详情页与 POP 页同源同 id。

### 3. 拍摄管线（自研 CameraX + Camera2 Interop）
- [PopCaptureScreen.kt](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureScreen.kt#L87-L185)：
  - `CameraCapabilityProbe.probe()` 真实探测（正确选取后置主摄、AE_OFF、ISO/曝光时间范围、EV 步进），非硬编码；
  - `PresetCapturePlan.build()` 真实分流参数状态（WRITTEN / POST / UNSUPPORTED + 原因）；
  - `buildImageCapture(plan)` 经 Camera2Interop 真实写入 `CONTROL_AE_MODE_OFF` + `CONTROL_CAPTURE_INTENT_MANUAL` + ISO/快门/EV（[L459-L489](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureScreen.kt#L459-L489)）；
  - 运行时申请 CAMERA 权限（manifest 已声明，`uses-feature required=false` 兼容无相机设备）。
- 实时取景：[FilteredPreviewAnalyzer.kt](file:///workspace/app/src/main/java/com/silas/omaster/recognition/FilteredPreviewAnalyzer.kt) 逐帧套用**与拍后同源**的色彩矩阵 + 暗角，30fps 节流、双缓冲、守护线程、退出 shutdown——实现质量高。
- 快门→拍后套色→预览→保存：`captureWithPreset()` → `applyStyle()`（矩阵 + unsharp 锐化 + 径向暗角，[L645-L658](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureScreen.kt#L645-L658)）→ `ImageExporter.saveToGallery`（Q+ 走 MediaStore 真实落相册）。
- 降级路径真实可用：无权限时系统相机（FileProvider `pop_capture/` 已在 [file_paths.xml](file:///workspace/app/src/main/res/xml/file_paths.xml#L28-L31) 配置）/ 相册选图，拍后同样完整走套色管线。
- 本地化：30 条 `pop_*` 文案中/英/日三语齐全。

---

## 三、关键缺陷（按严重度）

### P0 —— 预设套色对真实数据完全空转（数据模型断裂）
**事实链**：
1. `app/src/main/assets/presets.json` 全部 25 个真实预设只含 `sections`（展示字符串，如 `filter="复古 75%"`、`saturation="+20"`、`vignette="开"`），**typed 字段（`filter/saturation/warmCool/cyanMagenta/tone/colorTemperature/colorHue/softLight/sharpness/vignette/iso/shutterSpeed/exposureCompensation`）全部缺失**。
2. 应用内自建预设同样只写 sections：[UniversalCreatePresetViewModel.savePreset](file:///workspace/app/src/main/java/com/silas/omaster/ui/create/UniversalCreatePresetViewModel.kt#L228-L235) 构造 `MasterPreset(sections = state.sections)`，无 typed 字段。
3. POP 引擎只消费 typed 字段：[PresetColorTransform.buildColorMatrix](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PresetColorTransform.kt#L100-L146)、[vignetteStrength/sharpnessAmount](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PresetColorTransform.kt#L86-L89)、[PresetCapturePlan.build](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PresetCapturePlan.kt#L80-L157)。
4. 全仓检索 `\.sections` 消费方：仅详情页展示（`MasterPreset.getDisplaySections`）、创建页编辑、悬浮窗 `PresetParamMapper`（**存在 sections 解析能力，但 POP 链路未复用**）。**不存在任何 sections → typed 的桥接**。

**仿真证据**（对真实 presets.json 逐条执行 `hasVisibleEffect` 等价逻辑）：

```
25/25 预设 hasVisibleEffect = False（德味预设、富士胶片、胶片感、高对比黑白、理光绿/蓝、手机徕卡、哈苏浓郁……全部空转）
```

**后果**：
- 取景器实时滤镜 = 原画面（单位矩阵）；成片套色 = 原图；参数卡显示「该预设无摄影参数，仅套用滤镜风格」——而滤镜其实也没套；
- 拍摄成功后 toast 仍无条件显示「已套用「X」风格」（[L209](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureScreen.kt#L207-L211)），`applyStyle` 异常时也返回原图并照样提示，构成**虚假宣称**；
- 唯一能生效的两条旁路：照片识别草稿（[PhotoRecognitionEngine.buildDraft](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PhotoRecognitionEngine.kt#L63-L97) 写全 typed 字段）、截图/OCR 导入（[ScreenCaptureParser](file:///workspace/app/src/main/java/com/silas/omaster/data/imports/ScreenCaptureParser.kt#L94-L107) 写 typed 字段）。本任务审计的主链路（详情页 → POP）不在其中。

### P1 —— Android 6–9（API 23–28）保存相册必败
[ImageExporter](file:///workspace/app/src/main/java/com/silas/omaster/util/ImageExporter.kt#L37-L47) Q 以下分支写 `Environment.getExternalStoragePublicDirectory`，但 manifest 未声明 `WRITE_EXTERNAL_STORAGE`（minSdk=23）→ SecurityException 被 catch → 返回 false → 用户看到「保存失败」。

### P1 —— 全分辨率 CPU 后处理的卡顿/OOM 风险
相机成片路径不降采样（仅相册/系统相机路径限 2048px，[L610-L635](file:///workspace/app/src/main/java/com/silas/omaster/recognition/PopCaptureScreen.kt#L610-L635)）。12MP 成片走 `applyStyle`：全尺寸 Bitmap 拷贝 + 锐化 `IntArray(w*h)`，瞬时峰值约 150MB+（无 `largeHeap`），中低端机必卡甚至 OOM。

### P2 —— 预览缓冲回收竞态
[FilteredPreviewAnalyzer.obtainBuffer](file:///workspace/app/src/main/java/com/silas/omaster/recognition/FilteredPreviewAnalyzer.kt#L80-L93) 在帧尺寸变化时 `recycle()` 旧缓冲，而该缓冲可能正被 Compose 绘制 → 「Canvas: trying to use a recycled bitmap」崩溃窗口（概率低但存在）。

### P2 —— 滤镜表外滤镜名静默中性
`filterAdjustment` 为固定中文关键词表，「经典负片」「醇黑」等未收录滤镜名 → NEUTRAL，无任何提示（即使 typed 字段接通也会静默无效）。

### 测试盲区
`PresetColorTransformTest` / `PresetCapturePlanTest` / `PresetListJsonTest` 全部使用**合成 typed 数据**，无一条用真实 `presets.json` 驱动 POP 引擎 → P0 类数据契约断裂没有任何测试拦截，单测全绿与线上空转并存。

---

## 四、修复建议（按优先级）

1. **P0 桥接数据**：在 POP 入口（`PopCaptureHost` 或 repository 层）把 `sections` 归一化为 typed 字段——直接复用 `PresetParamMapper` 已有的 sections 解析思路，抽出公共 `SectionParamsParser`（滤镜/饱和度/影调/冷暖/青品/柔光/锐度/暗角/ISO/快门/EV 全量映射）；或给 `presets.json` 补写 typed 字段（双写，sections 保留展示）。
2. **UI 如实化**：`hasVisibleEffect(preset) == false` 时，参数卡与 toast 不得宣称"已套用"，改显「该预设无机器可套用参数」。
3. **P1 兼容保存**：Q 以下分支补 `WRITE_EXTERNAL_STORAGE`（`maxSdkVersion="28"`）或改 MediaStore 兼容插入。
4. **P1 性能**：相机成片按 4096px 上限降采样后再套色；锐化改 GPU（RenderEffect/RenderScript）或限定规格。
5. **P2 稳健性**：预览缓冲尺寸变化时延迟一帧回收；滤镜表外名称在参数卡标注「未识别，按原图处理」。
6. **回归测试**：新增 JVM 单测——直接解析 assets `presets.json`（或抽取数据契约），断言每个内置预设 `hasVisibleEffect == true`，杜绝数据与引擎再次脱节。

---

## 五、审计结论

「预设详情页选预设 → 直接 POP 拍摄」的**导航与拍摄链路完整、代码真实**（v1.7.5 的自研拍摄管线、能力探测、参数分级、降级路径均属实）；但**该链路未实际达到预期效果**：其核心价值"按预设实时取景 + 成片套色"因 sections/typed 数据模型断裂，对全部内置预设与自建预设空转，且 UI 存在虚假成功提示。判定为 **P0 功能虚焊**，需按第四节第 1、2 项修复后方可宣称该特性生效。
