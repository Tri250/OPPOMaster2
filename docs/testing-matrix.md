# v1.5.6 兼容矩阵与图像管线验收基准（P1-2 / P1-3 / P1-4）

> 交付范围：外部审计 P1-2（Android 与设备兼容矩阵）、P1-3（全分辨率图像处理内存治理）、
> P1-4（图像正确性与能力降级测试）。静态可验证项已在代码/单测中落实并锁定，
> 需真机的执行项逐条列在本表"执行方式"列，供设备矩阵实测时逐项打钩。

## 一、兼容矩阵（P1-2）

| 维度 | 覆盖范围 | 代码处理点（已实现） | 执行方式 / 预期 |
|---|---|---|---|
| Android API | 23（minSdk）/ 28 / 29 / 33 / 34 / 36（targetSdk） | 存储分支：`ScopedStorageMediaStore`（API 29+ MediaStore，低版本文件路径回退）；通知权限：POST_NOTIFICATIONS 仅 API 33+ 请求；前台服务：foregroundServiceType 按 Android 14 规范声明 | 真机/模拟器逐 API 安装启动 + 拍摄 + 保存相册；API 29+ 确认 `MediaStore.Images` 落库可见 |
| 品牌 | OPPO（Reno15/16/17）/ 一加 / realme / 至少一台非 ColorOS 设备 | `RenoDeviceProfile.sharpeningMultiplier`（Reno 档 HAL 锐化降权防双重锐化光晕，识别失败逐位 1.0）；非 Reno 默认 1.0 | Reno 系列出片无过锐光晕；非相关品牌功能完整无崩溃 |
| 性能等级 | 低内存（2-4GB）/ 中端（6-8GB）/ 旗舰（12GB+） | `chooseStyleMaxSide(w, h, maxMemory)` 按 45% 堆预算动态守卫；`PopImageProcessor.process` 守卫+分带流式 | 见第三节内存基准；低内存机连续 20 张无 OOM/ANR |
| ABI | arm64-v8a / armeabi-v7a / x86_64（模拟器） | Release 产物含五 ABI（arm64-v8a、armeabi-v7a、x86、x86_64、universal） | 三类设备安装启动、拍摄出片、滤镜预览帧率可接受 |
| 相机能力 | Camera2 FULL / LIMITED / LEGACY；无闪光灯；仅前摄 | `CameraCapabilityProbe` 运行时探测（单测覆盖 FULL/LIMITED/LEGACY 能力判定、最大感光面积主摄选择） | LEGACY 设备 POP 管线走降级路径不崩溃；能力缺失项 UI 不展示对应操作 |
| 权限状态 | 首次允许 / 拒绝 / 永久拒绝 / 通知关闭 / 悬浮窗关闭 | 相机权限三态处理（ON_RESUME 权限刷新 + 永久拒绝引导系统设置）；通知权限被拒不影响核心拍摄 | 每种权限状态下冷启动 + 拍摄 + 出片正常；通知关闭仅影响下载通知条 |
| 生命周期 | 冷启动 / 后台恢复 / 旋转 / 相机被占用 / 进程重建 | Compose 生命周期感知 + 相机绑定失败兜底提示；拍摄竞态保护（`isCapturing` 门控） | 各状态切换后 UI 状态不丢、相机重绑成功、无重复处理 |
| 存储状态 | 低存储 / 无可用空间 / 媒体文件被删除 / 分享 URI 失效 | MediaStore 写入失败回滚路径；识别管线对失效 URI 的空结果降级（`PhotoRecognitionHost` 失败态 UI） | 无空间时明确报错不崩溃；失效 URI 不引发未捕获异常 |

**验收映射**：P0/P1 用例 100% 通过（真机执行后勾选）；API 29+ MediaStore 正确落库；通知权限被拒核心功能不受影响；Android 14+ 前台服务启停符合系统规范（manifest `foregroundServiceType` 已声明）。

## 二、内存治理决策记录（P1-3）

### 结论：保留 `largeHeap=true`，依据如下

| 事实 | 数据 |
|---|---|
| 12MP 全尺寸后处理峰值 | 原图 48MB + 输出 48MB + 分带缓冲 ≈ **150MB+**（AUDIT_POP链路_v1.7.5.md 实测记录） |
| 默认堆（无 largeHeap） | 低内存设备 128-192MB——**不足以容纳峰值，必 OOM** |
| largeHeap 堆 | 主流设备 256-512MB，45% 预算 ≈ 115-230MB，可容纳峰值 |

审计建议的六项防御**全部已在代码中实现并有单测锁定**（`ImagePipelineCorrectnessTest`）：

1. **采样解码**：相册路径 `inSampleSize` + `chooseStyleMaxSide` 双重守卫（PopCaptureScreen L1001-1035）
2. **分块流式处理**：`PopImageProcessor.process` 按 `BAND_ROWS` 分带逐条处理（processBitmapBand），无第三份全图中间缓冲
3. **缓冲回收**：守卫缩放的 `work` 位图处理完即 `recycle()`（PopImageProcessor L75）；无效果直通零拷贝（L316-328）
4. **并发限制**：套色处理在单一 `Dispatchers.Default` 协程串行执行，无并发大图叠加
5. **EXIF 矩阵转换**：`rotateToUpright` 用 Matrix（非逐像素搬移）
6. **低端自动降级**：`chooseStyleMaxSide` 按堆预算等比收缩，单测锁定 12MP@100MB → 长边 1507；旗舰全分辨率不受影响

### 内存基准（真机执行项）

- 连续处理 20 张目标最大分辨率图片：无 OOM / 无 ANR / 前后台切换与旋转后无大图驻留
- 记录指标：峰值 PSS（`adb shell dumpsys meminfo`）、单张处理耗时、输出文件大小
- 回归门：后续提交相对基线退化 ≤10%

## 三、图像正确性黄金样本（P1-4）

| 用例 | 状态 | 验证层 |
|---|---|---|
| 内存守卫数学（45% 预算 / 2048 与 1080 下限 / 等比收缩） | ✅ 已锁定 | 单测（纯 JVM 精确值断言：12MP@100MB→1507） |
| 0% 强度不改像素（XDR 强度 0 调整量全零；全效果关闭逐位一致） | ✅ 已锁定 | 单测 |
| 守卫降级（小堆大图长边收敛）与源位图不可变 | ✅ 已锁定 | 单测 |
| 极小图（<3px）直通 | ✅ 已锁定 | 单测 |
| EXIF 0°/90°/180°/270° 尺寸几何 | ✅ 已锁定 | 单测（Robolectric 布局库不支持矩阵旋转像素渲染，像素级角点验证归真机） |
| EXIF 四方向像素级角点正确 | ⏳ 真机项 | 黄金样本 + 逐方向出片比对 |
| JPEG / PNG / HEIF 格式、sRGB / Display P3 色彩空间 | ⏳ 真机项 | 黄金样本逐张解码→处理→导出比对 |
| 极小图 / 超大图 / 透明图 / 损坏文件 | ⏳ 真机项 | 损坏文件走 decode 失败降级路径不崩溃 |
| 滤镜 50% / 100% 强度曲线 | ⏳ 真机项 | 单测已锁 0% 直通，50/100 需像素比对 |
| XDR 支持/不支持设备路径 | ⏳ 真机项 | 不支持设备自动隐藏 XDR 入口（`XdrCapabilityProbe`） |
| 导出后相册可见性、EXIF 保留 | ⏳ 真机项 | `exiftool` 读取导出文件与 MediaStore 查询比对 |

**验收映射**：0% 滤镜像素不变 ✅（单测锁定）；不支持能力不展示对应操作（能力探测 + UI 门控，真机复核）；输出方向/尺寸/色彩/EXIF/相册索引（真机黄金样本执行后勾选）。

## 四、单测落点

- [ImagePipelineCorrectnessTest.kt](../app/src/test/java/com/silas/omaster/recognition/ImagePipelineCorrectnessTest.kt)：14 项正确性锁定测试（守卫数学 / XDR 语义 / 管线直通与降级 / EXIF 几何）
- [PresetPixelEffectsTest.kt](../app/src/test/java/com/silas/omaster/recognition/PresetPixelEffectsTest.kt)：锐化边界/强度/拷贝语义（既有）
- [CameraCapabilityProbeTest.kt](../app/src/test/java/com/silas/omaster/recognition/CameraCapabilityProbeTest.kt)：相机能力判定（既有）
