# RapidRAW 深度源码级解析 & OMaster 智能优化模块整合建议报告（终极详细版）

> 分析日期：2026-06-27  
> 分析对象：[RapidRAW](https://gitee.com/gitsharp/RapidRAW)（开源 RAW 图像编辑器，Tauri + Rust + React + wgpu）  
> 目标项目：OMaster Android（Jetpack Compose + Kotlin + OpenGL ES + ML Kit）  
> 视角：资深专家级产品经理 + 技术架构深度洞察  
> 分析方法：RapidRAW 后端 12 个 Rust 源文件 + 前端 12 个 React/TSX 源文件 + OMaster 40+ Kotlin 源文件逐行代码级审阅

---

## 目录

- [一、RapidRAW 后端核心源码深度解析](#一rapidraw-后端核心源码深度解析)
- [二、RapidRAW 前端核心源码深度解析](#二rapidraw-前端核心源码深度解析)
- [三、OMaster 现有架构与代码现状深度梳理](#三omaster-现有架构与代码现状深度梳理)
- [四、逐功能点深度对比与整合建议](#四逐功能点深度对比与整合建议)
- [五、技术实现路线图](#五技术实现路线图)
- [六、竞品差异化定位与避坑指南](#六竞品差异化定位与避坑指南)

---

## 一、RapidRAW 后端核心源码深度解析

### 1.1 image_processing.rs（2248 行）—— 图像处理核心引擎

#### 1.1.1 核心数据结构

**`ImageMetadata`**（sidecar JSON 顶层结构）：
```rust
pub struct ImageMetadata {
    pub version: u32,        // = 1
    pub rating: u8,          // 0~5 星级
    pub adjustments: serde_json::Value,  // 松散 JSON，前端所有调整参数
    pub tags: Option<Vec<String>>,       // AI + 用户标签
}
```

**`GeometryParams`**（几何变换参数，Copy 结构）：
- 手动透视：`distortion, vertical, horizontal, rotate, aspect, scale, x_offset, y_offset`
- 镜头校正强度：`lens_distortion_amount, lens_vignette_amount, lens_tca_amount`（0~1），三个 `*_enabled: bool`
- 镜头模型参数：`lens_dist_k1/k2/k3`（多项式系数）、`lens_model: u32`（0=多项式，1=PTLens）、`tca_vr/tca_vb`（红/蓝径向缩放）、`vig_k1/k2/k3`（暗角系数）

**`AutoAdjustmentResults`**（自动调色输出）：
```rust
pub struct AutoAdjustmentResults {
    pub exposure: f64, pub contrast: f64, pub highlights: f64, pub shadows: f64,
    pub vibrancy: f64, pub vignette_amount: f64, pub temperature: f64, pub tint: f64,
    pub dehaze: f64, pub clarity: f64, pub centre: f64,
}
```

**`GlobalAdjustments`**（全局调整，约 80 个字段）：
- 基础：`exposure, brightness, contrast, highlights, shadows, whites, blacks`
- 颜色：`saturation, temperature, tint, vibrance`
- 细节：`sharpness, luma_noise_reduction, color_noise_reduction`
- 效果：`clarity, dehaze, structure, centre, vignette_amount/midpoint/roundness/feather, grain_amount/size/roughness`
- 创意：`glow_amount, halation_amount, flare_amount`
- 色差：`chromatic_aberration_red_cyan, chromatic_aberration_blue_yellow`
- 色彩分级：`color_grading_shadows/midtones/highlights`（各有 hue/saturation/luminance）+ `blending, balance`
- 色彩校准：`shadows_tint, red_hue/saturation, green_hue/saturation, blue_hue/saturation`
- HSL：`hsl: [HslColor; 8]`（红/橙/黄/绿/青/蓝/紫/洋红，各有 hue/saturation/luminance）
- 曲线：`luma_curve/red_curve/green_curve/blue_curve: [Point; 16]` + 各 `*_curve_count: u32`
- 标志位：`show_clipping, is_raw_image, has_lut, tonemapper_mode`（0=basic, 1=agx）、`lut_intensity`

**`MaskAdjustments`**（每张蒙版独立的局部调整，最多 9 张）：
字段与 GlobalAdjustments 子集一致（无暗角/颗粒/色差/LUT/AgX），但保留 clarity/dehaze/structure/glow/halation/flare + 色彩分级 + HSL + 曲线。

**`AllAdjustments`**（传给 GPU 的总 uniform）：
```rust
pub struct AllAdjustments {
    pub global: GlobalAdjustments,
    pub mask_adjustments: [MaskAdjustments; 9],  // 固定 9 槽
    pub mask_count: u32,
    pub tile_offset_x: u32, pub tile_offset_y: u32,
    pub mask_atlas_cols: u32,
}
```

**`AdjustmentScales`（常量 SCALES）**：前端 0~100 值到 GPU 物理量的缩放系数：
- `exposure: 0.8`（±100 → ±80 物理量）
- `contrast: 100.0, highlights: 150.0, dehaze: 750.0`
- `chromatic_aberration: 10000.0, color_grading_saturation: 500.0`

#### 1.1.2 核心算法

**`warp_image_geometry`（CPU 几何变换）**：
- 构造变换矩阵 = `T_center × M_offset × M_perspective × M_rotate × M_scale × T_uncenter`
- 对输出像素用逆矩阵反向映射 + **双线性插值**（unsafe get_unchecked 加速）
- 镜头畸变双模型：
  - 多项式：`rd = ru·(1 + k1·r² + k2·r⁴ + k3·r⁶)`
  - PTLens：`rd = ru·(a·r³ + b·r² + c·r + d)`，`d = 1−a−b−c`
  - 用 `lens_dist_amt` 在 `ru` 和 `rd` 间插值
- TCA：对 R/B 通道分别按 `vr/vb` 径向缩放采样坐标
- 暗角：`correction_gain = 1/(1 + vk1·r² + vk2·r⁴ + vk3·r⁶)`
- 全程 `rayon par_chunks_exact_mut` 并行按行处理

**`perform_auto_analysis`（自动调色算法，纯 CPU 统计法）**：
1. 下采样到 1024，转 RGB8
2. 统计 256 bin luma 直方图、平均饱和度、暗淡像素占比
3. **黑白点检测**：累计像素超 `total×0.001` 的首/末 bin 作为 black_point/white_point
4. `exposure = (128 − mid)·0.35`
5. `contrast = (250/range − 1)·50`（range>20 时）
6. shadows/highlights：按暗/亮像素占比（>5%）补偿，上限 ±80
7. **白平衡**：取最亮 1% 像素平均 RGB，`temperature = (B−R)·0.4`，`tint = (G − (R+B)/2)·0.5`（差值>3 才生效）
8. vibrancy：`mean_sat < 0.20` 时补到目标，暗淡像素>50% 额外+10
9. dehaze：`range<128 && sat<0.15` 时 `(1 − range/128)·40`
10. clarity：`range<180` 时 `(1 − range/180)·60`
11. **暗角检测**：比较中心 50% 区域与四角平均 luma，差值>0.05 给负 vignette + centre
12. 全部 clamp 到合理范围

**`remove_raw_artifacts_and_enhance`（RAW 去伪影 + 细节增强）**：
- RGB→YCbCr（BT.601 系数）
- **边缘感知双边滤波仅作用于 Cb/Cr**：3×3 邻域，权重 `1/(1 + (y_diff·14)² + spatial_penalty)`，滤波后色度幅值不超原始
- `apply_gentle_detail_enhance`：对 Y 做半径 2 可分离均值模糊，`detail = Y - blur`，边缘强度自适应增强（强边缘 ×0.3，平坦区 ×1.0）

### 1.2 gpu_processing.rs（948 行）—— GPU 渲染管线

#### 1.2.1 GPU 资源容器

**`GpuProcessor`**（按 max_width/height 预分配）：
- Blur：`h_blur_pipeline/v_blur_pipeline`（同一 `blur.wgsl` 的 horizontal/vertical 入口）
- Flare：3 张 512×512 Rgba16Float 中间纹理（threshold/ghosts/final）
- Main：`main_pipeline`（`shader.wgsl` 的 `main`，compute shader）
- 预分配复用纹理：`ping_pong_view`、`sharpness_blur_view`、`clarity_blur_view`、`structure_blur_view`、`output_texture_view`

#### 1.2.2 GPU 管线执行顺序（`GpuProcessor::run`）

```
1. Flare 预处理（若 flare_amount > 0）：
   a. flare_threshold_pipeline → 512×512 阈值图（dispatch 16×16 workgroups）
   b. flare_ghosts_pipeline → 鬼影图
   c. H-blur → V-blur（半径 12）高斯模糊 → flare_final

2. 分块主处理（TILE_SIZE=2048, TILE_OVERLAP=128）：
   对每块输入（带 overlap 区域）：
   a. run_blur(2.0, sharpness_blur_view)   ← 锐化用
   b. run_blur(8.0, clarity_blur_view)      ← clarity 用
   c. run_blur(40.0, structure_blur_view)   ← structure 用
   d. 写入 tile_adjustments（设 tile_offset_x/y）
   e. 绑定纹理：input(0)、output(1)、adjustments(2)、mask[0..9](3..11)、
      LUT 3D(12)+sampler(13)、三档模糊(14/15/16)、flare_final(17)+sampler(18)
   f. main_pipeline dispatch (w+7)/8 × (h+7)/8（每 workgroup 8×8 像素）
   g. read_texture_data 读回 Rgba8 → 按 crop 偏移拷贝
```

#### 1.2.3 输入纹理缓存

以 `transform_hash + width + height` 为 key，命中则复用 `GpuImageCache.texture_view`，未命中则 `to_rgba_f16`（半精度省显存）+ `create_texture_with_data`（Rgba16Float）。

### 1.3 mask_generation.rs（752 行）—— 蒙版系统

#### 1.3.1 蒙版类型与参数

| 蒙版类型 | 参数结构 | 关键字段 |
|---------|---------|---------|
| **Radial** | `RadialMaskParameters` | center_x/y, radius_x/y, rotation, feather |
| **Linear** | `LinearMaskParameters` | start_x/y, end_x/y, range（默认 50） |
| **Brush** | `BrushMaskParameters` | lines: Vec<BrushLine>（每条含 tool, brush_size, points, feather） |
| **AI Subject** | `AiSubjectMaskParameters` | start/end 框选坐标 + mask_data_base64 |
| **AI Sky** | `AiSkyMaskParameters` | mask_data_base64 |
| **AI Foreground** | `AiForegroundMaskParameters` | mask_data_base64 |
| **Quick Eraser** | 无额外参数 | 用于快速擦除 |

#### 1.3.2 蒙版合成算法

```
1. 遍历 sub_masks，每个调 generate_sub_mask_bitmap
2. 子蒙版先 invert（255−v），再按 opacity 缩放
3. 按 mode：Additive 用 max，Subtractive 用 saturating_sub
4. 整体 invert + opacity 缩放
```

**`apply_grow_and_feather`**：grow 用 `imageproc::morphology` 的 dilate/erode（LInf 范数，阈值 128），feather 用 `gaussian_blur_f32`；像素量 = `grow/100 × min(w,h) × 0.01`。

**`generate_ai_bitmap_from_full_mask`**：对输出像素做反向几何变换（uncrop → 去旋转 → 去 flip → 去旋转步数 → /scale）映射到全图蒙版采样。

### 1.4 culling.rs（304 行）—— 智能选片

#### 1.4.1 评分公式

```python
quality_score = sharpness × 0.40 + center_focus × 0.35 + exposure × 0.25
```

- **sharpness**：全图拉普拉斯方差（3×3 卷积 `(N+S+W+E − 4·center)`），对数归一化 `log10(metric+1)/3.5`
- **center_focus**：中心 50% 区域拉普拉斯方差，同上归一化
- **exposure**：`score = max(0, 1 − (dark_ratio + bright_ratio) × 5)`（dark: bin<5, bright: bin>250）

#### 1.4.2 相似图分组

- 感知哈希：`image_hasher` 的 `DoubleGradient` 算法，16×16
- BFS 连通分量：`hash.dist <= similarity_threshold` 则同组
- 组内按 quality_score 降序，最高为代表

### 1.5 denoising.rs（704 行）—— BM3D 降噪

#### 1.5.1 算法参数

```rust
BLOCK_SIZE = 8, BLOCK_AREA = 64, MAX_GROUP_SIZE = 16
STRIDE = 6, SEARCH_WINDOW = 19, FIXED_POINT_SCALE = 100_000
```

**`Bm3dParams::from_intensity(i)`**：
- `sigma = i × 80`
- `lambda = 2 + i × 2.5`
- `max_dist = 3000 + i × 20000`

#### 1.5.2 两步法流程

1. **Step 1（Hard Threshold）**：guide = noisy 自身
2. **Step 2（Wiener）**：guide = step1 结果

每步：
- block_matching（19×19 搜索窗，三通道渐进 SSD + early-exit，取 prev_power_of_two 个块）
- 3D 变换：每块 2D DCT → 沿第三维 1D Walsh-Hadamard
- Step1: hard_threshold（`threshold = lambda × sigma`），weight = 1/非零数
- Step2: wiener_filter（`coef = g²/(g²+σ²)`），weight = 1/Σcoef²
- 逆变换 + Kaiser 窗加权累加（AtomicAccumulator 定点数无锁并行）

### 1.6 ai_processing.rs（559 行）—— AI 推理

#### 1.6.1 模型矩阵

| 模型 | 输入尺寸 | 用途 | 加载策略 |
|------|---------|------|---------|
| SAM ViT-T encoder | 1024×1024 | 主体分割 embedding | 必装 |
| SAM ViT-T decoder | 256×256 | 交互式分割解码 | 必装 |
| U2Netp | 320×320 | 前景分割 | 必装 |
| SkySeg (U2Net) | 320×320 | 天空分割 | 必装 |
| CLIP ViT-B/32 | 224×224 | 智能标签 | 按需（enable_ai_tagging） |

所有模型托管在 HuggingFace `CyberTimon/RapidRAW-Models`，**SHA256 流式校验**，不匹配则删后重下。

#### 1.6.2 SAM 交互式分割流程

1. `generate_image_embeddings`：长边缩放到 1024，ImageNet 归一化，encoder 推理 → embeddings 缓存
2. `run_sam_decoder`：框选坐标映射到 1024 域，`point_coords [1,2,2]`（两点框），`point_labels [2,3]`，输出 mask > 0 → 255，blur(σ=3) 羽化

### 1.7 preset_converter.rs（341 行）—— XMP 预设转换

#### 1.7.1 映射表

| XMP 属性 | 内部字段 | 换算 |
|---------|---------|------|
| Exposure2012 | exposure | 直接 |
| Contrast2012 | contrast | 直接 |
| Highlights2012 | highlights | 直接 |
| Shadows2012 | shadows | ×1.5，上限 100 |
| Whites2012 | whites | 直接 |
| Blacks2012 | blacks | 直接 |
| Clarity2012 | clarity | 直接 |
| Dehaze | dehaze | 直接 |
| Texture | structure | 直接 |
| Sharpness | sharpness | /150×100，clamp 0~100 |
| Temperature | temperature | mired 转换：`(-mired_delta/150)×100`，clamp ±100 |
| Tint | tint | /150×100，clamp ±100 |
| HueAdjustment{Color} | hsl.hue | ×0.75 |
| ToneCurvePV2012 | luma_curve | 阴影区阻尼：x<64 且 y>x 时按 0.8+0.2·progress 衰减 |

### 1.8 其他后端文件摘要

- **tagging.rs**：CLIP zero-shot 分类（候选词批量编码 + softmax + 阈值 0.005 + TopK 10）+ HSV 颜色标签分桶 + 标签层次结构（子→父自动补全）+ 后台并发索引（默认 3 线程）
- **file_management.rs**：`.rrdata` sidecar（JSON）+ 虚拟副本 `?vc=` 语法 + 预设单文件 JSON + 文件夹嵌套 + 几何变换缓存（geometry_hash）+ LUT/蒙版缓存 + CopyPaste 版本迁移
- **image_loader.rs**：cancel_token generation 机制 + panic::catch_unwind 包裹 RAW 解码 + AI patch alpha 合成
- **lut_processing.rs**：`.cube`/`.3dl`/HALD PNG 三格式统一到 size³×3 float 数组
- **exif_processing.rs**：双路提取（exif crate + rawler）+ GPS 坐标格式化

---

## 二、RapidRAW 前端核心源码深度解析

### 2.1 App.tsx（4783 行）—— 全局状态与编排

#### 2.1.1 状态管理（100+ useState）

**基础状态**：
- `rootPath, appSettings, activeView('library'|'editor'), imageList, imageRatings, selectedImage, multiSelectedPaths`

**编辑器核心状态**：
- `adjustments` + `useHistoryState`（撤销/重做历史）
- `finalPreviewUrl / uncroppedAdjustedPreviewUrl / transformedOriginalUrl`（多级预览）
- `histogram / waveform`（直方图/波形数据）
- `activeRightPanel`（7 种面板：Adjustments/Metadata/Crop/Masks/Presets/Export/Ai）
- `activeMaskContainerId / activeMaskId`（蒙版编辑状态）
- `brushSettings`（笔刷大小/羽化/工具类型）
- `exportState / importState`

#### 2.1.2 Undo/Redo 三级防抖架构

```
滑块拖拽 → setLiveAdjustments（立即更新 UI）
         → throttledInteractiveUpdate（100ms 节流，交互中预览）
         → debouncedSetHistory（300ms 防抖入历史栈）
         → debouncedApplyAdjustments（50ms 防抖后端渲染）
         → debouncedSave（300ms 防抖保存 sidecar）
```

`useAsyncThrottle` 自定义钩子防止并发后端请求。

#### 2.1.3 右键菜单结构

- **Productivity 子菜单**：Auto Adjust / Virtual Copy / Denoise / Negative / Panorama / Collage / Cull
- **Rating 子菜单**：0-5 星
- **Color Label 子菜单**：颜色标签
- **Tagging 子菜单**：用户标签
- **Delete 子菜单**：虚拟副本/关联文件处理

### 2.2 Editor.tsx（601 行）—— 编辑器画布

#### 2.2.1 关键交互

- **targetZoom 动画**：`factor = Math.log(targetZoom/currentScale)`，200ms easeOut 缓动
- **旋转后最大内接矩形**：sin/cos 公式计算
- **双击缩放**：scale≥2 时重置，否则 zoomIn
- **maskOverlayUrl 异步生成**：调用 `invoke(GenerateMaskOverlay)`

### 2.3 ImageCanvas.tsx（1317 行）—— Konva 画布

#### 2.3.1 白平衡采样

```
采样 11×11 像素区域 → 计算线性化 R/G/B 平均值
deltaTemp = (B-R)/(R+B) × 125
deltaTint = (G-M)/(G+M) × 400  （M = (R+B)/2）
```

#### 2.3.2 图层淡入淡出系统

新图层 opacity 0 加入 → `requestAnimationFrame` 淡入到 1 → `onTransitionEnd` 切除旧图层（仅保留 finished 及之后）。

#### 2.3.3 蒙版绘制

- **Radial**：`Ellipse` + `Transformer`（可缩放/旋转）
- **Linear**：`Group` 含可拖拽范围线 + start/end `Circle`（`perpendicularDragBoundFunc` 限制垂直拖拽）
- **Brush**：`Group` of `Line`（沿路径插值盖章，步长 = `radius·(1−feather)/2`）
- **AiSubject**：`Rect`（框选区域）

窗口级 mousemove/mouseup 监听支持画布外绘制。

### 2.4 ControlsPanel.tsx —— 调整面板

- **Section 级复制/粘贴/重置**：右键上下文菜单，粘贴仅允许同 section
- **sectionVisibility 独立可见性**：不影响参数值，仅控制 UI 显示
- **Auto 按钮**（Aperture 图标）一键自动调整
- **CollapsibleSection**：basic/curves/color/details/effects 五段可折叠

### 2.5 MasksPanel.tsx（1221 行）—— 蒙版管理

- **DnD 三类型**：Creation（创建新容器/子蒙版）、Container（重排序）、SubMask（容器间移动/提取）
- **analyzingSubMaskId 200ms 延迟显示**：防加载闪烁
- **Apply Preset 递归子菜单**：从预设文件夹树生成嵌套菜单
- **Additive/Subtractive 模式切换**：Plus/Minus 图标
- **蒙版设置**：Invert、Opacity、grow/feather 参数、BrushTools（Size 1-200, Feather 0-100, Add/Erase）

### 2.6 AIPanel.tsx（1415 行）—— AI 编辑

- **ConnectionStatus**：绿色 Ready / 红色 Not Detected + tooltip 解释
- **useFastInpaint 三态逻辑**：QuickEraser 或未连接时强制开启并禁用
- **prompt 输入框**：Enter 触发生成，未开启 fast inpainting 时显示
- **按钮状态文字**：Generating... / Inpaint Selection / Generate with AI

### 2.7 PresetsPanel.tsx（987 行）—— 预设管理

- **预览生成顺序队列**：`previewQueue` ref + `isProcessingQueue` ref，避免并发
- **仅展开文件夹时生成预览**（懒加载）
- **blob URL revoke 清理**：卸载时 revoke 所有 + 清空队列
- **导入格式**：`.rrpreset`、`.xmp`、`.lrtemplate`
- **右键菜单**：Overwrite / Rename / Duplicate / Export / Delete

### 2.8 ExportPanel.tsx（682 行）—— 导出

- **9 锚点水印定位**：TopLeft/TopCenter/TopRight/CenterLeft/Center/CenterRight/BottomLeft/BottomCenter/BottomRight
- **resizeMode**：Long Edge / Short Edge / Width / Height
- **文件名模板变量**：`{sequence}`、`{original_filename}`、`{Date}`、`{Width}x{Height}`
- **debouncedEstimateSize**（500ms）：预估导出文件大小
- **批量导出**：自动追加 `_{sequence}`

### 2.9 MetadataPanel.tsx —— EXIF 展示

- **分区**：File Properties / Key Camera Settings / GPS Location / All EXIF Data
- **KEY_CAMERA_SETTINGS_MAP**：FNumber→Aperture, ExposureTime→Shutter Speed, PhotographicSensitivity→ISO, FocalLengthIn35mmFilm→Focal Length
- **OpenStreetMap iframe**：`bbox` + `marker`，`pointer-events-none` + 链接覆盖层

### 2.10 Waveform.tsx —— 波形示波器

- **5 模式**：RGB / Luma / Red / Green / Blue
- **对数强度缩放**：`scale = 255/log(1+maxVal)`
- **可拖拽浮动面板**：`handle=".handle"` + `bounds="parent"`
- **Canvas**：256×256

### 2.11 MainLibrary.tsx（1745 行）—— 图库

- **VariableSizeList + AutoSizer**：虚拟列表，万级图片不卡顿
- **标签式搜索**：`,` 或 Enter 创建标签，Backspace 删除，AND/OR 模式切换
- **Ctrl/Cmd 多选 + Shift 范围选择**（颜色标签筛选）
- **双图层淡入**避免缩略图闪烁
- **Contain 模式**：背景模糊层 `blur-md scale-110` + 前景 `object-contain`
- **活跃图片平滑滚动定位**（仅超出视口才滚动，SCROLL_OFFSET=120）

### 2.12 SettingsPanel.tsx（1262 行）—— 设置

- **三分类**：General / Processing / Shortcuts（layoutId 共享元素动画）
- **AiProviderSwitch**：CPU / AI Connector / Cloud（三按钮切换）
- **Adjustments Visibility**：Chromatic Aberration / Grain / Color Calibration 等开关
- **My Lenses**：厂商+型号级联 Dropdown（Lensfun 数据库）
- **快捷键**：Space/Enter 打开、Ctrl+Z/Y 撤销重做、0-5 评分、D/R/M/K/P/I/W/E 切换面板

---

## 三、OMaster 现有架构与代码现状深度梳理

### 3.1 四层架构

```
┌─────────────────────────────────────────────────────────────┐
│ UI / 呈现层                                                   │
│  SmartOptimizeScreen / AIFineTuneScreen / HasselbladScreen   │
│  ParamAdjustScreen / StyleLUTGeneratorScreen / CoreFeatures  │
│  AppNavigation (758 行, 25+ 路由)                             │
├─────────────────────────────────────────────────────────────┤
│ 推理与决策层                                                   │
│  MasterInferenceEngine / AIFineTuneManager / MasterInsight   │
│  HeuristicSceneAnalyzer / SceneToHasselbladMapping           │
├─────────────────────────────────────────────────────────────┤
│ 渲染与处理层                                                   │
│  GPURenderManager / ImageShaderRenderer / ShaderProgram      │
│  PixelFruitEngine / LUT3DParser/Renderer/Manager             │
│  HasselbladColorEngine / NightModeManager / PortraitModeMgr  │
│  BatchProcessingManager / HistogramAnalyzer                  │
├─────────────────────────────────────────────────────────────┤
│ 数据与状态层                                                   │
│  RenderParameters / SceneProfile / ScenePresets / MasterPreset│
│  FilmAdjustments / HistoryManager / RecipeHistoryManager     │
│  SettingsManager / PresetRepository / UndoRedoManager        │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 核心数据结构现状

#### RenderParameters（18 全局参数 + HSL 8 通道 + 4 通道曲线 + 3D LUT）
```
exposure, brightness, contrast, saturation, vibrance, warmth,
highlights, shadows, whites, blacks, clarity, dehaze, fade,
grain, denoise, skinSmooth, sharpness, texture
+ HSL 8×3 (H/S/L per red/orange/yellow/green/cyan/blue/purple/magenta)
+ Curve LUT 4×256 (rgb/red/green/blue)
+ 3D LUT (lutEnabled, lutTextureId, lutSize, lutStrength)
```

**vs RapidRAW GlobalAdjustments（80+ 字段）**：OMaster 缺少 tone curve 控制点、color grading (shadows/midtones/highlights 独立着色)、color calibration、vignette 参数化（midpoint/roundness/feather）、grain 参数化（size/roughness）、chromatic aberration、glow/halation/flare。

#### PixelFruitParams（14 参数）
```
brightness, exposure, contrast, saturation, highlights, shadows,
whitePoint, rgbTone, sharpness, denoise, faceWhitening, transitionSmooth
```

#### HasselbladParams（-30~30 范围）
```
tone, saturation, contrast, colorTemp, sharpness, vignette,
cyanMagenta, highlights, shadows, clarity
```

### 3.3 渲染管线对比

| 维度 | RapidRAW | OMaster |
|------|---------|---------|
| GPU 框架 | wgpu (WebGPU/Vulkan/Metal) | OpenGL ES 3.0 |
| Shader 语言 | WGSL (compute shader) | GLSL (fragment shader) |
| 管线设计 | 单 compute shader + 预计算多档模糊 + 分块渲染 | 多 pass fragment shader |
| 模糊预计算 | ✓ (sharpness σ2 / clarity σ8 / structure σ40) | ✗ |
| 分块渲染 | ✓ (TILE=2048, OVERLAP=128) | ✗ |
| 输入纹理缓存 | ✓ (transform_hash) | ✗ |
| CPU 降级 | ✗ (GPU 不可用则降级原图) | ✓ (PixelFruitEngine CPU 路径) |
| Uniform 增量上传 | N/A (compute 全量) | ✗ (每次全量) |

### 3.4 AI 能力对比

| 能力 | RapidRAW | OMaster |
|------|---------|---------|
| 主体分割 | SAM ViT-T (encoder+decoder) | ML Kit Selfie Segmentation（仅 PortraitMode） |
| 天空分割 | U2Net 专用天空模型 | ✗ |
| 前景分割 | U2Netp | ✗ |
| 智能标签 | CLIP ViT-B/32 zero-shot | ✗ |
| 生成式替换 | 外接 ComfyUI/SD | ✗ |
| 人脸检测 | ✗ | ML Kit FaceDetection ✓ |
| 场景识别 | ✗ (依赖用户手动) | 启发式规则分析 ✓ |
| 姿势引导 | ✗ | MediaPipe Pose Landmarker ✓ |
| 自动调色 | 统计法（黑白点+白平衡+暗角检测） | 云端 AI 建议 + 本地启发式 |
| 降噪 | BM3D 两步法 | 边缘感知均值滤波 |

### 3.5 工作流对比

| 维度 | RapidRAW | OMaster |
|------|---------|---------|
| 编辑模式 | 非破坏性（sidecar JSON） | 破坏性（内存操作，导出覆盖） |
| 撤销/重做 | useHistoryState 三级防抖 | HistoryManager + UndoRedoManager + VM 内部 ArrayDeque（三处重复） |
| 局部调整 | ✓ (9 蒙版槽，AI+画笔+径向+线性) | ✗ |
| 批量处理 | ✓ (多选+批量应用+批量导出) | BatchProcessingManager（存在但纯 CPU，效果与预览不一致） |
| 预设生态 | 文件夹嵌套+XMP 导入+预览生成+DnD 排序 | 7 内置 + PresetRepository 远程预设（无自定义/导入） |
| 图库管理 | 虚拟列表+智能筛选+Culling+标签搜索 | TrailSnap 影集（独立模块，未与编辑打通） |
| 导出 | 多格式+水印+文件名模板+批量 | 仅保存到相册 |
| EXIF 展示 | 完整面板+GPS 地图 | 仅用于分析，不展示 |

### 3.6 已发现的 OMaster 代码问题

1. **撤销栈三处重复实现**：`HistoryManager` + `UndoRedoManager` + `AIFineTuneViewModel` 内部 `ArrayDeque`
2. **CameraParams.shutterSpeed 默认 125f**（应为 1f/125f ≈ 0.008f）— `ParamAdjustScreen.kt:1192`
3. **renderPreviewAsync 竞态注释与实现不符** — if/else 两分支都调 `renderer.renderPreview`
4. **HSLValue 用 var 可变字段**，与 data class 不可变约定冲突
5. **BatchProcessingManager 纯 CPU**，与单张 GPU 预览效果不一致
6. **RecipeHistoryManager Base64 缩略图**，500 条可能 OOM
7. **ImageShaderRenderer 无 diff 增量**，每次 uniform 全量上传
8. **CoreFeaturesScreen slice 分组**依赖列表索引，添加功能易错位

---

## 四、逐功能点深度对比与整合建议

### 4.1 🔴 P0-1：非破坏性编辑 + 调整历史栈统一

#### RapidRAW 实现
- **Sidecar 机制**：每张图一个 `.rrdata` JSON 文件，存储 `ImageMetadata{version, rating, adjustments, tags}`
- **虚拟副本**：`path?vc=abcdef` 语法，同一原图可有多组调整
- **Undo/Redo 三级防抖**：live（立即 UI）→ throttled（100ms 交互预览）→ debounced history（300ms 入栈）
- **Section 级操作**：按 basic/color/details/effects/curves 分组，可独立复制/粘贴/重置

#### OMaster 现状
- 破坏性编辑：`PixelFruitEngine.applyAdjustments` 直接修改 Bitmap
- 三处撤销栈重复实现，SmartOptimizeScreen 无 Undo/Redo
- 无 Section 概念，参数平铺

#### 整合建议

**阶段一：统一撤销栈**
```kotlin
// 统一使用已有的 UndoRedoManager<T>，废弃 VM 内部 ArrayDeque
class SmartOptimizeViewModel(
    private val undoRedoManager: UndoRedoManager<RenderParameters>
) : ViewModel() {
    private val _currentParams = MutableStateFlow(RenderParameters())
    
    fun updateParam(paramName: String, value: Float) {
        val newParams = _currentParams.value.updateParam(paramName, value)
        // 立即更新 UI
        _currentParams.value = newParams
        // 防抖入栈（300ms）
        historyDebounceJob?.cancel()
        historyDebounceJob = viewModelScope.launch {
            delay(300)
            undoRedoManager.pushState(newParams)
        }
    }
}
```

**阶段二：非破坏性编辑 Sidecar**
```kotlin
data class ImageEditMetadata(
    val version: Int = 1,
    val rating: Int = 0,
    val adjustments: RenderParameters,  // 完整参数快照
    val tags: List<String> = emptyList(),
    val timestamp: Long
)

// 存储为 JSON sidecar：原图路径 + ".omaster_meta"
class EditMetadataRepository {
    suspend fun saveMetadata(imageUri: Uri, params: RenderParameters) {
        val meta = ImageEditMetadata(adjustments = params, timestamp = System.currentTimeMillis())
        val json = Gson().toJson(meta)
        // 写入 .omaster_meta 文件或 DataStore
    }
    
    suspend fun loadMetadata(imageUri: Uri): ImageEditMetadata?
}
```

**阶段三：Section 级操作**
```kotlin
enum class AdjustmentSection {
    BASIC,      // exposure/brightness/contrast/highlights/shadows/whites/blacks
    COLOR,      // saturation/vibrance/warmth + HSL
    DETAILS,    // sharpness/denoise/clarity/dehaze/texture
    EFFECTS,    // fade/grain/vignette
    CURVES      // 4 通道曲线
}

fun copySection(params: RenderParameters, section: AdjustmentSection): SectionSnapshot
fun pasteSection(params: RenderParameters, section: AdjustmentSection, snapshot: SectionSnapshot): RenderParameters
fun resetSection(params: RenderParameters, section: AdjustmentSection): RenderParameters
```

**用户价值**：解决"调错无法回退"的核心焦虑 + 实现编辑可恢复。

---

### 4.2 🔴 P0-2：AI 局部调整蒙版系统

#### RapidRAW 实现
- 9 个蒙版槽，每张蒙版独立全套调整参数
- AI 自动生成：SAM（主体）、U2Net（天空）、U2Netp（前景）
- 手动蒙版：Radial（椭圆+旋转+羽化）、Linear（线性渐变）、Brush（画笔+橡皮）
- 蒙版布尔运算：Additive(max) / Subtractive(saturating_sub)
- grow/feather 参数化后处理

#### OMaster 现状
- **PortraitModeManager 已有 ML Kit Selfie Segmentation**（仅用于人像模式背景虚化）
- 无任何编辑时蒙版功能
- 无局部调整能力

#### 整合建议

**阶段一：一键 AI 局部优化（复用现有 Selfie Segmentation）**

```kotlin
data class LocalAdjustment(
    val maskType: MaskType,        // SUBJECT / SKY / BACKGROUND
    val maskBitmap: Bitmap,        // R8 蒙版纹理
    val adjustments: RenderParameters,  // 局部调整参数
    val opacity: Float = 1f,
    val isEnabled: Boolean = true
)

class LocalAdjustmentManager {
    // 复用 PortraitModeManager 的 Selfie Segmentation
    suspend fun generateSubjectMask(bitmap: Bitmap): Bitmap {
        val segmenter = SelfieSegmenter.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SegmentationDetectorMode.SINGLE_IMAGE_MODE)
                .build()
        )
        val image = InputImage.fromBitmap(bitmap, 0)
        val mask = segmenter.process(image).await()
        // mask.buffer 转为 R8 Bitmap
        return maskToBitmap(mask, bitmap.width, bitmap.height)
    }
    
    // 天空分割：用简单颜色+位置启发式（蓝色+上半部分）
    suspend fun generateSkyMaskHeuristic(bitmap: Bitmap): Bitmap {
        // 上半部分蓝色像素 → 蒙版
        // 后续可替换为 TFLite SkySeg 模型
    }
    
    fun applyLocalAdjustment(
        sourceBitmap: Bitmap,
        mask: Bitmap,
        adjustments: RenderParameters,
        opacity: Float
    ): Bitmap {
        // GPU: 上传 mask 为 R8 纹理
        // Fragment shader: mix(globalResult, localResult, mask × opacity)
    }
}
```

**阶段二：手动蒙版（Radial + Brush）**

```kotlin
// Compose 画布上手绘蒙版
@Composable
fun MaskCanvas(
    bitmap: Bitmap,
    maskType: MaskType,
    onMaskGenerated: (Bitmap) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Radial: 椭圆路径 + 羽化
        // Brush: 路径插值盖章 + 橡皮
    }
}

// GPU Shader 增加 mask 支持
// image_adjust.frag 新增：
// uniform sampler2D u_MaskTexture;
// uniform float u_MaskOpacity;
// vec4 maskedColor = mix(globalColor, localColor, maskValue * u_MaskOpacity);
```

**阶段三：蒙版管理 UI**

```kotlin
@Composable
fun MasksPanel(
    masks: List<LocalAdjustment>,
    onAddMask: (MaskType) -> Unit,
    onToggleMask: (Int) -> Unit,
    onAdjustMask: (Int, RenderParameters) -> Unit
) {
    // 蒙版列表（可折叠）
    // 每个蒙版：图标 + 名称 + 可见性切换 + 删除
    // 选中蒙版后显示独立调整面板
    // Additive/Subtractive 模式切换
}
```

**用户价值**：从"全局滤镜"跃迁到"专业分区调色"，直接对标 Lightroom Mobile 核心付费功能。

---

### 4.3 🔴 P0-3：预设生态升级

#### RapidRAW 实现
- 文件夹嵌套管理 + DnD 排序
- XMP 导入（完整映射表，含 mired 白平衡换算、HSL hue×0.75、Shadows×1.5、曲线阴影阻尼）
- 预设预览图顺序队列生成（仅展开文件夹时懒加载）
- 导出为 `.rrpreset` 文件

#### OMaster 现状
- 7 个内置预设（PixelFruitModels.BuiltInPresets）
- PresetRepository 支持远程预设浏览/收藏/下载
- 无用户自定义预设、无导入导出

#### 整合建议

```kotlin
data class UserPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,           // 分类文件夹
    val params: RenderParameters,   // 完整参数快照
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)

class UserPresetManager {
    // 保存当前参数为预设
    suspend fun saveAsPreset(name: String, params: RenderParameters, category: String) {
        val preset = UserPreset(name = name, params = params, category = category)
        // 生成预览缩略图
        val thumbnail = generatePresetPreview(params)
        // 存储到 DataStore JSON
        presetRepository.saveUserPreset(preset.copy(thumbnailPath = saveThumbnail(thumbnail)))
    }
    
    // 导出为 JSON 文件
    suspend fun exportPreset(preset: UserPreset): Uri {
        val json = Gson().toJson(preset)
        // 通过 SAF 写入用户选择的位置
    }
    
    // 导入 JSON 预设
    suspend fun importPreset(uri: Uri): UserPreset {
        val json = readTextFromUri(uri)
        return Gson().fromJson(json, UserPreset::class.java)
    }
    
    // 导入 Lightroom XMP（可选高级功能）
    suspend fun importXmpPreset(uri: Uri): UserPreset {
        val xmp = readTextFromUri(uri)
        val params = XmpPresetConverter.convert(xmp)
        return UserPreset(name = extractXmpName(xmp), params = params)
    }
    
    // 预览图生成
    private suspend fun generatePresetPreview(params: RenderParameters): Bitmap {
        // 用默认样图应用参数，生成缩略图
    }
}

object XmpPresetConverter {
    fun convert(xmp: String): RenderParameters {
        val props = parseXmpAttributes(xmp)  // 正则 crs:(\w+)="([^"]*)"
        return RenderParameters(
            exposure = props["Exposure2012"]?.toFloatOrNull() ?: 0f,
            contrast = props["Contrast2012"]?.toFloatOrNull() ?: 0f,
            highlights = props["Highlights2012"]?.toFloatOrNull() ?: 0f,
            shadows = (props["Shadows2012"]?.toFloatOrNull() ?: 0f) * 1.5f,  // ×1.5 补偿
            // ... 完整映射表
        )
    }
}
```

**用户价值**：构建 UGC 内容生态 + 兼容 Lightroom 预设导入降低迁移门槛。

---

### 4.4 🔴 P0-4：图库集成与批量工作流

#### RapidRAW 实现
- 智能选片（Culling）：拉普拉斯方差模糊检测 + 中心对焦权重(0.35) + 曝光裁剪惩罚(0.25) + 感知哈希相似分组
- 批量应用调整：rayon 并行 merge 到多图 sidecar
- 批量导出：多格式 + 水印 + 文件名模板 + 进度展示

#### OMaster 现状
- BatchProcessingManager 存在但纯 CPU、效果与预览不一致
- TrailSnap 影集模块独立，未与编辑器打通
- 无智能选片

#### 整合建议

**智能选片算法移植（纯 CPU 统计法，移动端实时可跑）**：
```kotlin
object PhotoCullingEngine {
    data class PhotoQuality(
        val path: String,
        val qualityScore: Float,    // 0~1
        val sharpness: Float,       // 0~1
        val centerFocus: Float,     // 0~1
        val exposure: Float,        // 0~1
        val perceptualHash: Long
    )
    
    suspend fun analyzePhoto(bitmap: Bitmap): PhotoQuality {
        val thumb = downsample(bitmap, 512)
        val gray = toGrayscale(thumb)
        
        // 拉普拉斯方差 → 清晰度
        val laplacianVar = calculateLaplacianVariance(gray)
        val sharpness = normalizeLog(laplacianVar)  // log10(metric+1)/3.5
        
        // 中心 50% 区域拉普拉斯方差 → 中心对焦
        val centerLaplacianVar = calculateLaplacianVariance(centerCrop(gray, 0.5f))
        val centerFocus = normalizeLog(centerLaplacianVar)
        
        // 曝光评分
        val histogram = IntArray(256)
        gray.forEach { histogram[it.toInt()]++ }
        val darkRatio = histogram.take(5).sum().toFloat() / gray.size
        val brightRatio = histogram.takeLast(6).sum().toFloat() / gray.size
        val exposure = maxOf(0f, 1f - (darkRatio + brightRatio) * 5f)
        
        val qualityScore = sharpness * 0.40f + centerFocus * 0.35f + exposure * 0.25f
        
        // 感知哈希
        val hash = computePerceptualHash(thumb)  // 16×16 DoubleGradient
        
        return PhotoQuality(bitmap.path, qualityScore, sharpness, centerFocus, exposure, hash)
    }
    
    fun groupSimilarPhotos(photos: List<PhotoQuality>, threshold: Int = 5): List<List<PhotoQuality>> {
        // BFS 连通分量：hamming distance <= threshold 则同组
        // 组内按 qualityScore 降序，最高为代表
    }
}
```

**批量处理修复（接入 GPU 管线）**：
```kotlin
class BatchProcessingManager {
    suspend fun batchProcess(
        imageUris: List<Uri>,
        params: RenderParameters,
        onProgress: (Int, Int) -> Unit
    ) {
        // 确保使用 GPU 管线，与单张预览效果一致
        val renderManager = GPURenderManager.getInstance()
        renderManager.initialize()
        
        imageUris.forEachIndexed { index, uri ->
            val bitmap = loadBitmap(uri, maxDimension = 2048)
            val result = renderManager.renderSync(bitmap, params, RenderQuality.HIGH)
            saveToGallery(result.outputBitmap)
            onProgress(index + 1, imageUris.size)
        }
    }
}
```

**用户价值**：解决"连拍 100 张选 1 张"痛点 + 批量调色效率提升。

---

### 4.5 🟡 P1-5：专业色调曲线升级（4 通道 + GPU 实时渲染）

#### RapidRAW 实现
- 4 通道（RGB/红/绿/蓝），各 16 控制点
- 曲线预设（linear/S曲线/高对比/柔和/反相）
- GPU compute shader 实时渲染（曲线 LUT 256×4 传入 uniform）

#### OMaster 现状
- AIFineTuneScreen 已有 4 通道曲线，但生成 256 LUT 后 CPU 上传
- 曲线预设已有 5 种（linear/highContrast/soft/sCurve/invert）
- `syncRenderParamsToCurve` 仅做恒等重置，曲线与参数双向同步不完整

#### 整合建议

```kotlin
// 修复曲线双向同步
fun syncCurveFromLut(curveLut: FloatArray, channel: String): List<CurvePoint> {
    // 从 256 LUT 反推关键控制点
    // 使用 Douglas-Peucker 算法简化曲线到 ≤8 控制点
    val points = mutableListOf<CurvePoint>()
    for (i in 0..255) {
        points.add(CurvePoint(i / 255f, curveLut[i]))
    }
    return simplifyCurve(points, tolerance = 0.01f)  // Douglas-Peucker
}

// GPU 曲线 LUT 上传优化
class ImageShaderRenderer {
    private var lastCurveHash: Int = 0
    
    fun uploadCurveLutIfChanged(curveLuts: Map<String, FloatArray>) {
        val hash = curveLuts.values.hashCode()
        if (hash == lastCurveHash) return  // diff 增量上传
        lastCurveHash = hash
        
        // 上传为 1D 纹理（GL_R32F × 4）
        curveLuts.forEach { (channel, lut) ->
            val textureId = curveTextures[channel] ?: createCurveTexture()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_1D, textureId)
            GLES30.glTexSubImage2D(...)  // 仅更新数据
        }
    }
}
```

---

### 4.6 🟡 P1-6：自动调色算法移植

#### RapidRAW 实现（纯 CPU 统计法）
```
1. 下采样到 1024
2. 256 bin luma 直方图
3. 黑白点检测（累计 0.001 阈值）
4. exposure = (128 - mid) × 0.35
5. contrast = (250/range - 1) × 50
6. 白平衡 = 最亮 1% 像素 RGB 差值
7. shadows/highlights 按暗/亮占比补偿
8. dehaze = range<128 && sat<0.15 时补偿
9. clarity = range<180 时补偿
10. 暗角检测 = 中心 vs 四角 luma 差值
```

#### OMaster 现状
- 依赖云端 AI 建议或启发式场景分析
- 无本地自动调色算法

#### 整合建议

```kotlin
object AutoAdjustEngine {
    data class AutoResult(
        val exposure: Float, val contrast: Float,
        val highlights: Float, val shadows: Float,
        val vibrance: Float, val warmth: Float,
        val clarity: Float, val dehaze: Float
    )
    
    suspend fun autoAnalyze(bitmap: Bitmap): AutoResult {
        val thumb = downsample(bitmap, 1024)
        val histogram = HistogramAnalyzer.analyze(thumb)
        
        // 黑白点检测
        val total = thumb.width * thumb.height
        var blackPoint = 0
        var whitePoint = 255
        var cumulative = 0f
        for (i in 0..255) {
            cumulative += histogram.luminance[i]
            if (cumulative / total > 0.001f && blackPoint == 0) blackPoint = i
            if (cumulative / total > 0.999f) { whitePoint = i; break }
        }
        
        val range = whitePoint - blackPoint
        val mid = (blackPoint + whitePoint) / 2
        
        // 曝光
        val exposure = ((128 - mid) * 0.35f).coerceIn(-100f, 100f)
        
        // 对比度
        val contrast = if (range > 20) ((250f / range - 1) * 50f).coerceIn(-100f, 100f) else 0f
        
        // 白平衡（最亮 1% 像素）
        val brightPixels = getBrightestPixels(thumb, 0.01f)
        val avgR = brightPixels.map { it.r }.average()
        val avgG = brightPixels.map { it.g }.average()
        val avgB = brightPixels.map { it.b }.average()
        val temperature = if (abs(avgB - avgR) > 3) ((avgB - avgR) * 0.4f).coerceIn(-100f, 100f) else 0f
        val tint = if (abs(avgG - (avgR + avgB) / 2) > 3) ((avgG - (avgR + avgB) / 2) * 0.5f).coerceIn(-100f, 100f) else 0f
        
        // dehaze / clarity
        val meanSat = calculateMeanSaturation(thumb)
        val dehaze = if (range < 128 && meanSat < 0.15f) ((1 - range / 128f) * 40f) else 0f
        val clarity = if (range < 180) ((1 - range / 180f) * 60f) else 0f
        
        // 暗角检测
        val centerLuma = getRegionAverageLuma(thumb, 0.5f, center = true)
        val cornerLuma = getCornerAverageLuma(thumb)
        val vignette = if (centerLuma - cornerLuma > 0.05f) -(centerLuma - cornerLuma) * 100f else 0f
        
        return AutoResult(exposure, contrast, ...)
    }
}
```

**用户价值**：一键自动调色无需云端，离线可用，响应即时。

---

### 4.7 🟡 P1-7：导出工作流升级

#### RapidRAW 实现
- 格式：JPEG/PNG/8bit TIFF/16bit TIFF/EXR
- 质量调节、尺寸缩放（长边/短边/宽/高）、不放大选项
- 水印：图片水印 + 9 锚点定位 + 大小/间距/透明度
- 文件名模板变量
- 元数据保留/GPS 剥离
- 批量导出 + 进度展示

#### OMaster 现状
- 仅保存到相册，无格式/质量/尺寸选择
- 无水印
- 无批量导出

#### 整合建议

```kotlin
@Composable
fun ExportOptionsSheet(
    onExport: (ExportConfig) -> Unit
) {
    var format by remember { mutableStateOf(ImageFormat.JPEG) }
    var quality by remember { mutableStateOf(95) }
    var resizeEnabled by remember { mutableStateOf(false) }
    var resizeMode by remember { mutableStateOf(ResizeMode.LONG_EDGE) }
    var resizeValue by remember { mutableStateOf(2048) }
    var watermarkEnabled by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    
    Column {
        // 格式选择
        FormatSelector(format) { format = it }
        
        // 质量滑块（仅 JPEG）
        if (format == ImageFormat.JPEG) {
            Slider(value = quality.toFloat(), valueRange = 50f..100f, onValueChange = { quality = it.toInt() })
        }
        
        // 尺寸缩放
        Switch(checked = resizeEnabled, onCheckedChange = { resizeEnabled = it })
        if (resizeEnabled) {
            ResizeModeSelector(resizeMode) { resizeMode = it }
            TextField(value = resizeValue.toString(), ...)
        }
        
        // 文字水印
        Switch(checked = watermarkEnabled, onCheckedChange = { watermarkEnabled = it })
        if (watermarkEnabled) {
            TextField(value = watermarkText, ...)
            // 9 锚点选择网格
            AnchorSelector(watermarkPosition) { watermarkPosition = it }
        }
        
        // 导出按钮
        Button(onClick = { onExport(ExportConfig(format, quality, ...)) }) {
            Text("导出")
        }
    }
}

// 文字水印绘制
fun drawWatermark(canvas: Canvas, text: String, anchor: WatermarkPosition, textSize: Float) {
    val paint = Paint().apply {
        color = Color.WHITE.copy(alpha = 0.7f)
        textSize = textSize
        isAntiAlias = true
        setShadowLayer(2f, 1f, 1f, Color.BLACK.copy(alpha = 0.5f))
    }
    val metrics = paint.fontMetrics
    val textWidth = paint.measureText(text)
    val textHeight = metrics.descent - metrics.ascent
    
    val (x, y) = when (anchor) {
        WatermarkPosition.BOTTOM_RIGHT -> 
            canvas.width - textWidth - margin to canvas.height - margin
        // ... 9 锚点
    }
    canvas.drawText(text, x, y, paint)
}
```

**用户价值**：从"简单保存"升级为"专业输出"，摄影师刚需。

---

### 4.8 🟡 P1-8：EXIF 元数据面板

#### RapidRAW 实现
- 分区展示：File Properties / Key Camera Settings / GPS Location / All EXIF Data
- OpenStreetMap iframe 嵌入 GPS 位置
- 格式化模板：`1/xxx s`、`f/val`、`xxx mm`

#### OMaster 现状
- EXIF 仅用于 HeuristicSceneAnalyzer 分析
- 用户无法查看拍摄参数

#### 整合建议

```kotlin
@Composable
fun ExifInfoSheet(imageUri: Uri) {
    val exif = remember { ExifInterface(contentResolver.openInputStream(imageUri)!!) }
    
    Column {
        // 相机参数区
        ExifSection("关键拍摄参数") {
            ExifRow("光圈", "f/${exif.getAttribute(ExifInterface.TAG_F_NUMBER)}")
            ExifRow("快门", formatShutterSpeed(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)))
            ExifRow("ISO", exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY))
            ExifRow("焦距", "${exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)}mm")
            ExifRow("设备", exif.getAttribute(ExifInterface.TAG_MAKE) + " " + exif.getAttribute(ExifInterface.TAG_MODEL))
        }
        
        // GPS 地图
        val latLng = exif.latLong
        if (latLng != null) {
            ExifSection("拍摄位置") {
                // 高德/Google 静态地图
                AsyncImage(
                    model = "https://maps.googleapis.com/maps/api/staticmap?center=${latLng[0]},${latLng[1]}&zoom=15&size=400x200&markers=${latLng[0]},${latLng[1]}",
                    ...
                )
            }
        }
        
        // 大师点评（OMaster 独有差异化）
        ExifSection("大师点评") {
            val tips = generatePhotoTips(exif)
            tips.forEach { tip -> TipRow(tip) }
        }
    }
}

fun generatePhotoTips(exif: ExifInterface): List<String> {
    val tips = mutableListOf<String>()
    val iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 100)
    val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
    val focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
    
    if (iso > 3200) tips.add("ISO 偏高，建议在光线充足时降低以减少噪点")
    if (exposure > 1.0 / 30) tips.add("快门速度偏慢，建议使用三脚架或提高 ISO")
    if (focalLength > 85) tips.add("长焦拍摄，注意防抖和快门速度（≥ 1/焦距）")
    return tips
}
```

**用户价值**：满足摄影爱好者参数查看需求 + 增加"大师点评"教育属性（差异化）。

---

### 4.9 🟡 P1-9：智能标签与语义搜索

#### RapidRAW 实现
- CLIP ViT-B/32 zero-shot 分类
- 候选词批量编码 + softmax + 阈值 0.005 + TopK 10
- HSV 颜色标签分桶（red/orange/yellow/green/blue/purple + black/white/gray/brown）
- 标签层次结构（子→父自动补全）
- 后台并发索引（默认 3 线程）

#### OMaster 现状
- 无标签系统
- TrailSnap 影集有独立搜索但基于文件名

#### 整合建议

**轻量版：颜色标签 + 场景标签（无需 ML 模型）**
```kotlin
object AutoTagger {
    fun extractColorTags(bitmap: Bitmap): List<String> {
        val thumb = downsample(bitmap, 100)
        val hsvBuckets = IntArray(8) // red/orange/yellow/green/cyan/blue/purple/magenta
        thumb.forEach { pixel ->
            val hsv = rgbToHsv(pixel)
            when {
                hsv.saturation < 0.15f && hsv.value > 0.8f -> "white"
                hsv.saturation < 0.15f && hsv.value < 0.3f -> "black"
                hsv.saturation < 0.15f -> "gray"
                else -> hueToColorName(hsv.hue)  // H 分桶
            }
        }
        return topColors(hsvBuckets, 2)
    }
    
    fun extractSceneTags(sceneProfile: SceneProfile): List<String> {
        // 复用 HeuristicSceneAnalyzer 结果
        return listOf(sceneProfile.category.name.lowercase(), sceneProfile.name)
    }
}
```

**完整版：轻量 CLIP（TFLite）**
```kotlin
class ClipTagger(context: Context) {
    private val interpreter = Interpreter(loadModel("clip_tiny.tflite"))
    private val candidates = listOf("sunset", "beach", "portrait", "food", "night", "architecture", ...)
    
    suspend fun generateTags(bitmap: Bitmap): List<String> {
        val input = preprocessClipImage(bitmap, 224)  // resize + normalize
        val imageEmbedding = infer(interpreter, input)
        val scores = candidates.map { candidate ->
            val textEmbedding = textEmbeddings[candidate]!!
            cosineSimilarity(imageEmbedding, textEmbedding)
        }
        return scores.mapIndexed { i, score -> candidates[i] to score }
            .filter { it.second > 0.005f }
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }
}
```

**用户价值**：解决"照片太多找不到"痛点。

---

### 4.10 🟢 P2-10：降噪算法升级

#### RapidRAW 实现
- BM3D 两步法（Hard Threshold + Wiener）
- Block matching（19×19 搜索窗，三通道渐进 SSD + early-exit）
- 3D 变换（2D DCT + 1D Walsh-Hadamard）
- AtomicAccumulator 定点数无锁并行累加
- intensity→sigma/lambda/dist 参数映射

#### OMaster 现状
- `PixelFruitEngine.applyNoiseReduction`：简单边缘感知均值滤波
- NightModeManager：多帧合成 + 双边滤波（仅拍摄时）

#### 整合建议

```kotlin
// 轻量 AI 降噪（推荐方案）
class AIDenoiser(context: Context) {
    private val interpreter = Interpreter(loadModel("cbdnet_lite.tflite"))
    
    suspend fun denoise(bitmap: Bitmap, intensity: Float): Bitmap {
        // CBDNet: 输入噪声图 → 预测噪声 → 残差降噪
        val input = bitmapToTensor(bitmap)
        val noise = infer(interpreter, input)
        val denoised = input - noise * intensity  // 按强度混合
        return tensorToBitmap(denoised)
    }
}

// 或移植 BM3D 精简版（NDK 实现）
// 鉴于 BM3D 计算量大，建议：
// 1. 仅对暗光场景（ISO > 3200）自动触发
// 2. 降采样到 1024 后处理，再上采样回原尺寸
// 3. 或作为 Pro 功能走云端 API
```

---

### 4.11 🟢 P2-11：镜头校正

#### RapidRAW 实现
- Lensfun 数据库（6000+ 镜头配置文件）
- 畸变（多项式 + PTLens 双模型）+ TCA（R/B 分通道径向缩放）+ 暗角
- `warp_image_geometry` CPU 并行 + `unwarp_image_geometry` 8 次牛顿迭代逆解

#### OMaster 现状
- 无镜头校正

#### 整合建议

```kotlin
// 手机镜头简化方案（不需要 Lensfun 数据库）
object LensCorrection {
    // 手机主摄/超广角/长焦的典型畸变系数（预设值）
    data class PhoneLensProfile(
        val k1: Float,   // 三次多项式系数
        val k2: Float,
        val k3: Float,
        val vignetteK1: Float,
        val vignetteK2: Float
    )
    
    val PHONE_LENS_PROFILES = mapOf(
        "wide" to PhoneLensProfile(0.02f, -0.01f, 0.003f, -0.5f, 0.2f),        // 主摄
        "ultrawide" to PhoneLensProfile(0.08f, -0.04f, 0.01f, -1.2f, 0.5f),    // 超广角
        "telephoto" to PhoneLensProfile(0.005f, -0.002f, 0.001f, -0.2f, 0.1f)  // 长焦
    )
    
    // GPU Shader 实现
    // image_adjust.frag 新增：
    // uniform float u_LensK1, u_LensK2, u_LensK3;
    // uniform float u_VignetteK1, u_VignetteK2;
    // vec2 correctDistortion(vec2 uv) {
    //     float r = length(uv - 0.5);
    //     float rd = r * (1.0 + k1*r*r + k2*r*r*r*r + k3*r*r*r*r*r*r);
    //     return mix(uv, 0.5 + normalize(uv - 0.5) * rd, correctionAmount);
    // }
    // float vignette(vec2 uv) {
    //     float r = length(uv - 0.5);
    //     return 1.0 / (1.0 + vigK1*r*r + vigK2*r*r*r*r);
    // }
}
```

---

### 4.12 🟢 P2-12：UI/UX 交互深度借鉴

#### 4.12.1 三级防抖架构（App.tsx）

```
live → 立即更新 UI StateFlow
throttled → 100ms 节流交互预览（GPU 渲染）
debounced history → 300ms 防抖入历史栈
debounced save → 300ms 防抖保存 sidecar
```

**Android 实现**：
```kotlin
private val liveParams = MutableStateFlow(RenderParameters())
private val historyDebounce = Channel<RenderParameters>(Channel.CONFLATED)

init {
    // 立即更新 UI
    liveParams
        .debounce(50)  // 防抖 GPU 渲染
        .onEach { params -> renderPreview(params) }
        .launchIn(viewModelScope)
    
    // 防抖入历史栈
    liveParams
        .debounce(300)
        .onEach { params -> undoRedoManager.pushState(params) }
        .launchIn(viewModelScope)
    
    // 防抖保存
    liveParams
        .debounce(300)
        .onEach { params -> saveMetadata(params) }
        .launchIn(viewModelScope)
}
```

#### 4.12.2 图层淡入淡出（ImageCanvas / MainLibrary）

```kotlin
// Android 双图层实现
@Composable
fun FadeImageLayer(newBitmap: Bitmap, oldBitmap: Bitmap?) {
    var layerOpacity by remember { mutableStateOf(0f) }
    
    Box {
        // 旧图层
        oldBitmap?.let {
            Image(bitmap = it.asImageBitmap(), modifier = Modifier.fillMaxSize())
        }
        // 新图层淡入
        Image(
            bitmap = newBitmap.asImageBitmap(),
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = layerOpacity }
        )
    }
    
    LaunchedEffect(newBitmap) {
        layerOpacity = 0f
        animate(0f, 1f, animationSpec = tween(100)) { value, _ ->
            layerOpacity = value
        }
        // 动画完成后通知清除旧图层
    }
}
```

#### 4.12.3 长按 Section 级操作（ControlsPanel 右键菜单 → Android 长按）

```kotlin
@Composable
fun AdjustmentSection(
    title: String,
    params: RenderParameters,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onReset: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.longPressClickable { showMenu = true }
    ) {
        Text(title)
        // 参数滑块...
    }
    
    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
        DropdownMenuItem(text = { Text("复制此组") }, onClick = { onCopy(); showMenu = false })
        DropdownMenuItem(text = { Text("粘贴到此组") }, onClick = { onPaste(); showMenu = false })
        DropdownMenuItem(text = { Text("重置此组") }, onClick = { onReset(); showMenu = false })
    }
}
```

#### 4.12.4 波形示波器（Waveform → Android Canvas）

```kotlin
@Composable
fun WaveformView(
    waveform: FloatArray,  // 256×256
    mode: WaveformMode,    // RGB/LUMA/R/G/B
    modifier: Modifier
) {
    Canvas(modifier = modifier.size(256.dp, 256.dp).background(Color.Black)) {
        val maxVal = waveform.maxOrNull() ?: 1f
        val scale = 255f / ln(1f + maxVal)  // 对数缩放
        
        when (mode) {
            WaveformMode.LUMA -> {
                waveform.forEachIndexed { x, value ->
                    val y = (value * scale).toInt()
                    drawLine(
                        color = HasselbladOrange.copy(alpha = 0.7f),
                        start = Offset(x.toFloat(), size.height),
                        end = Offset(x.toFloat(), size.height - y),
                        strokeWidth = 1f
                    )
                }
            }
            // RGB 模式：三通道叠加
        }
    }
}
```

#### 4.12.5 白平衡采样器（ImageCanvas → Android 点击采样）

```kotlin
// 复用 RapidRAW 的 11×11 采样公式
fun sampleWhiteBalance(bitmap: Bitmap, x: Int, y: Int): Pair<Float, Float> {
    val pixels = sampleRegion(bitmap, x, y, 5)  // 11×11 区域
    val avgR = pixels.map { Color.red(it) / 255f }.average()
    val avgG = pixels.map { Color.green(it) / 255f }.average()
    val avgB = pixels.map { Color.blue(it) / 255f }.average()
    
    val deltaTemp = ((avgB - avgR) / (avgR + avgB) * 125).toFloat()
    val m = (avgR + avgB) / 2
    val deltaTint = ((avgG - m) / (avgG + m) * 400).toFloat()
    
    return deltaTemp to deltaTint
}

// UI：点击图片任意位置采样白平衡
@Composable
fun WhiteBalancePicker(bitmap: Bitmap, onPicked: (Float, Float) -> Unit) {
    Box(modifier = Modifier.clickable { offset ->
        val (temp, tint) = sampleWhiteBalance(bitmap, offset.x.toInt(), offset.y.toInt())
        onPicked(temp, tint)
    })
}
```

---

## 五、技术实现路线图

### 5.1 短期（1-2 个月）：P0 功能落地

| 周次 | 任务 | 产出 |
|------|------|------|
| W1-2 | 统一撤销栈：废弃 VM 内部 ArrayDeque，统一用 UndoRedoManager | SmartOptimizeScreen + AIFineTuneScreen 一致 Undo/Redo |
| W3-4 | 非破坏性编辑 Sidecar：ImageEditMetadata JSON 存储 | 编辑参数可恢复 |
| W3-4 | Section 级操作：basic/color/details/effects/curves 分组复制/粘贴/重置 | 长按 Section 弹出菜单 |
| W5-6 | AI 主体蒙版：复用 ML Kit Selfie Segmentation | 「主体提亮」「背景虚化」一键功能 |
| W5-6 | 天空蒙版启发式版：蓝色+上半部分检测 | 「天空增强」一键功能 |
| W7-8 | 用户自定义预设：保存/命名/分类/预览图生成 | 预设管理页升级 |
| W7-8 | 预设导入导出：JSON 格式 + SAF 分享 | 社区预设流通 |
| W9-10 | 智能选片：拉普拉斯方差 + 感知哈希相似分组 | 影集「推荐最优」标签 |
| W11-12 | 批量处理修复：接入 GPU 管线 + 批量导出进度 | 批量效果与单张一致 |

### 5.2 中期（3-6 个月）：P1 功能构建

| 月份 | 任务 | 产出 |
|------|------|------|
| M2 | 曲线双向同步修复 + GPU 1D 纹理上传优化 | 曲线编辑流畅 |
| M2-3 | 自动调色算法移植（统计法） | 一键 Auto 按钮 |
| M3 | 导出工作流：格式/质量/尺寸/文字水印/9 锚点 | 导出 BottomSheet |
| M3-4 | EXIF 元数据面板 + GPS 地图 + 大师点评 | 信息按钮 |
| M4 | 智能标签：颜色标签 + 场景标签（无 ML） | 自动打标 |
| M4-5 | 手动蒙版：Radial + Brush（Canvas 绘制） | 局部调整画笔 |
| M5-6 | 轻量 CLIP 标签（TFLite，~50MB） | 语义搜索 |
| M5-6 | 波形示波器（Canvas 5 模式） | 专业模式工具 |

### 5.3 长期（6-12 个月）：P2 技术壁垒

| 月份 | 任务 | 产出 |
|------|------|------|
| M6-8 | AI 降噪（TFLite CBDNet 或云端 API） | 夜景降噪升级 |
| M8-10 | 镜头校正（手机镜头预设 + GPU Shader） | 广角畸变修复 |
| M10-12 | 全景拼接（特征匹配 + 单应性矩阵） | 全景模式 |
| M10-12 | 生成式 AI 局部重绘（云端 SD API） | AI 擦除/替换 |

---

## 六、竞品差异化定位与避坑指南

### 6.1 产品定位

> **「AI 驱动的移动端专业影像工作流」**

### 6.2 核心差异化卖点

1. **AI 一键分区优化**：智能识别主体/天空/背景后分区调整（对标 Lightroom Select Subject，但更简单）
2. **哈苏色彩科学 + 胶片模拟**：HasselbladColorEngine 独特资产，继续深化
3. **从拍摄到出片一站式**：影集管理 → 智能选片 → 批量调色 → 专业导出
4. **端侧 AI 优先**：减少云端依赖，隐私保护，离线可用
5. **大师点评教育属性**：EXIF 参数 + 拍摄建议（RapidRAW 无此功能）

### 6.3 需要规避的坑

| 坑 | 原因 | 对策 |
|----|------|------|
| 照搬桌面三栏布局 | 手机屏空间有限 | 保留底部 Tab + 大卡片设计，平板/折叠屏再考虑双栏 |
| 追求 RAW 处理 | 手机主流 JPEG/HEIC，RAW 用户极少 | 不投入 RAW 解码，聚焦 JPEG/HEIC 优化 |
| 一次性堆砌功能 | 破坏「一键智能优化」核心心智 | 专业功能作为可展开的二级选项 |
| BM3D 全分辨率跑 | 计算量巨大，移动端会 ANR | 降采样到 1024 或走云端 |
| SAM 全模型移植 | ViT-T 也需 ~100MB + 大算力 | 用 ML Kit Selfie Segmentation 替代 |
| 完整 Lensfun 数据库 | 6000+ 镜头配置文件过大 | 仅预设手机镜头典型参数 |
| 照搬 WGSL compute shader | Android OpenGL ES 不支持 compute | 用 Fragment Shader 实现，或 Vulkan Compute |

### 6.4 技术选型对照

| 功能 | RapidRAW 方案 | OMaster 推荐方案 | 原因 |
|------|-------------|----------------|------|
| AI 主体分割 | SAM ViT-T (ONNX) | ML Kit Selfie Segmentation | 免费、无需模型管理、已集成 |
| 天空分割 | U2Net SkySeg | 颜色+位置启发式 → 后续 TFLite | 先快速上线，后续升级 |
| 智能标签 | CLIP ViT-B/32 | 颜色标签+场景标签 → 后续 MobileCLIP | 先无 ML 版本，后续升级 |
| 降噪 | BM3D (Rust) | TFLite CBDNet 或云端 | BM3D 移动端太慢 |
| 曲线 GPU | WGSL compute | GLSL fragment + 1D texture | OpenGL ES 兼容 |
| 预设导入 | XMP 解析 | XMP 解析（复用映射表） | Lightroom 用户迁移 |
| 批量处理 | rayon 并行 | Coroutines + WorkManager | Android 后台处理 |
| 虚拟列表 | react-window | RecyclerView + GridLayoutManager | Android 原生方案 |
| DnD 排序 | @dnd-kit | ItemTouchHelper | Android 原生方案 |

---

## 七、RapidRAW 可直接复用的算法/数据清单

以下是从 RapidRAW 源码中提取的、可直接移植到 OMaster Android 的纯算法和数据（无框架依赖）：

| 算法/数据 | 源文件 | 移植难度 | 价值 |
|-----------|-------|---------|------|
| **SCALES 常量表**（前端 0~100 → 物理量） | image_processing.rs | ⭐ 极低 | 统一滑块语义 |
| **自动调色 11 步算法** | image_processing.rs | ⭐⭐ 低 | 一键 Auto 功能 |
| **拉普拉斯方差模糊检测** | culling.rs | ⭐ 极低 | 智能选片 |
| **感知哈希相似分组** | culling.rs | ⭐⭐ 低 | 连拍去重 |
| **质量评分公式**（0.40/0.35/0.25 权重） | culling.rs | ⭐ 极低 | 智能选片 |
| **HSV 颜色标签分桶** | tagging.rs | ⭐ 极低 | 自动标签 |
| **CLIP zero-shot 流程** | tagging.rs | ⭐⭐⭐ 中 | 语义搜索 |
| **XMP→内部参数映射表** | preset_converter.rs | ⭐⭐ 低 | LR 预设导入 |
| **mired 白平衡换算** | preset_converter.rs | ⭐ 极低 | 色温转换 |
| **Radial 蒙版生成算法** | mask_generation.rs | ⭐⭐ 低 | 局部调整 |
| **Linear 蒙版生成算法** | mask_generation.rs | ⭐⭐ 低 | 局部调整 |
| **Brush 蒙版插值盖章** | mask_generation.rs | ⭐⭐ 低 | 局部调整 |
| **蒙版布尔运算**（max/saturating_sub） | mask_generation.rs | ⭐ 极低 | 蒙版合成 |
| **grow/feather 参数化** | mask_generation.rs | ⭐⭐ 低 | 蒙版后处理 |
| **白平衡 11×11 采样公式** | ImageCanvas.tsx | ⭐ 极低 | 白平衡采样器 |
| **波形图对数缩放** | Waveform.tsx | ⭐ 极低 | 波形示波器 |
| **旋转后最大内接矩形** | Editor.tsx | ⭐ 极低 | 裁切计算 |
| **9 锚点水印定位** | ExportPanel.tsx | ⭐ 极低 | 水印功能 |
| **文件名模板变量** | ExportPanel.tsx | ⭐ 极低 | 批量导出 |
| **双图层淡入淡出** | MainLibrary.tsx | ⭐⭐ 低 | 缩略图无闪烁 |
| **EXIF 格式化模板** | MetadataPanel.tsx | ⭐ 极低 | EXIF 面板 |
| **LUT 三格式解析**（cube/3dl/HALD） | lut_processing.rs | ⭐⭐ 低 | LUT 兼容性 |
| **BM3D 参数映射**（intensity→sigma/lambda/dist） | denoising.rs | ⭐ 极低 | 降噪参数 |
| **镜头畸变多项式/PTLens 双模型** | image_processing.rs | ⭐⭐⭐ 中 | 镜头校正 |
| **TCA 分通道径向缩放** | image_processing.rs | ⭐⭐ 低 | 镜头校正 |
| **暗角校正公式** | image_processing.rs | ⭐ 极低 | 镜头校正 |

---

## 八、总结

RapidRAW 的核心价值在于展示了**「现代专业图像编辑器」的完整功能蓝图**，其代码质量高、架构清晰、算法实现完整。对 OMaster 的整合建议可归纳为三个层次：

### 第一层：立即可做（复用现有代码 + 低难度移植）
- 统一撤销栈（已有 UndoRedoManager）
- 自动调色算法（纯统计法，无需 ML）
- 智能选片（拉普拉斯方差 + 感知哈希）
- 颜色标签（HSV 分桶）
- EXIF 面板（已有 ExifInterface）
- 导出水印（Canvas 绘制）

### 第二层：中等投入（需要新组件但架构清晰）
- AI 主体蒙版（复用 ML Kit Selfie Segmentation）
- 手动蒙版（Canvas 绘制 + GPU mask texture）
- 用户预设系统（JSON 存储 + 预览生成）
- XMP 预设导入（映射表移植）
- 批量处理修复（接入 GPU 管线）
- 曲线双向同步修复

### 第三层：长期投入（需要新模型/新管线）
- 轻量 CLIP 语义标签（TFLite）
- AI 降噪（TFLite 或云端）
- 镜头校正（GPU Shader + 预设参数）
- 全景拼接（特征匹配）
- 生成式 AI 局部重绘（云端 SD）

建议以 **「AI 局部优化 + 预设生态 + 批量工作流」** 作为下一阶段三大核心战役，配合 **「非破坏性编辑 + 统一撤销栈」** 的基础设施升级，逐步构建 OMaster 在专业移动影像领域的竞争壁垒。

---

*报告完。共分析 RapidRAW 源文件 24 个（后端 12 + 前端 12），OMaster 源文件 40+ 个，涵盖数据结构、算法、UI 交互、架构设计的逐行代码级对比。*
