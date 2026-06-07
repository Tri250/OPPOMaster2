# OMaster vs OPPOMaster 功能对比报告

**对比项目**: iCurrer/OMaster vs Tri250/OPPOMaster  
**对比时间**: 2026-06-07

---

## 一、OMaster 核心功能模块

| 模块 | OMaster | 我们 | 状态 |
|------|---------|------|------|
| 预设详情页 (DetailScreen) | ✅ 完整实现 | ⚠️ 简化版 | 可借鉴 |
| 悬浮窗 (FloatingWindow) | ✅ 完整 Service | ❌ 未实现 | 待开发 |
| 色卡图书馆 (ColorCardLibrary) | ✅ 12 个色卡 | ❌ 未实现 | 待开发 |
| 配置中心 (ConfigCenter) | ✅ 统一管理 | ⚠️ 分散 | 可借鉴 |
| 关于页 (AboutScreen) | ✅ 29KB 完整 | ❌ 未实现 | 待开发 |
| 开源许可证 | ✅ 完整页 | ❌ 未实现 | 待开发 |
| 隐私政策 | ✅ 完整页 | ❌ 未实现 | 待开发 |
| 订阅管理 | ✅ 多源 | ✅ 已实现 | 持平 |
| 自定义预设 | ✅ 创建页 | ✅ 已实现 | 持平 |
| 悬浮窗引导 | ✅ 引导页 | ❌ 未实现 | 待开发 |

---

## 二、主页设计逻辑分析（OMaster）

### OMaster HomeScreen 核心设计

**布局结构**（自上而下）：
1. 紧凑标题栏（应用名 + 状态栏适配）
2. **ScrollableTabRow**（带动画指示器的 Tab，可滑动）
3. **HorizontalPager**（每个 Tab 对应一个滑动页面）
4. **LazyVerticalStaggeredGrid**（瀑布流）
5. 加载更多提示

**核心特性**：
- ✅ Tab 与 Pager 双向同步（双向 LaunchedEffect）
- ✅ Tab 标题 + 计数徽章
- ✅ 自定义指示器动画
- ✅ 切换页面时 key 重建触发动画
- ✅ 悬浮窗控制器联动
- ✅ PullRefresh 下拉刷新

**3 个 Tab 分类**：
- 全部 (allPresets)
- 收藏 (favorites) 
- 我的 (customPresets)

---

## 三、我们主页现状分析

### 当前 Web HomeScreen

| 特性 | 状态 |
|------|------|
| 紧凑标题栏 | ✅ |
| 4 个 Tab（全部/收藏/哈苏/上新） | ✅ |
| HorizontalPager 联动 | ❌ |
| 品牌筛选 | ✅ |
| 搜索栏 | ✅ |
| 瀑布流 | ✅ |
| 下拉刷新 | ✅ |
| 同步状态指示器 | ✅ |

### 当前 Android HomeScreen

| 特性 | 状态 |
|------|------|
| 紧凑标题栏 | ✅ |
| ScrollableTabRow | ✅ |
| HorizontalPager | ✅ |
| Tab 与 Pager 双向同步 | ✅ |
| 计数徽章 | ✅ |
| 悬浮窗联动 | ✅ |
| PullRefresh | ✅ |
| LazyVerticalStaggeredGrid | ✅ |

---

## 四、可学习/集成功能

### P0 必集成（高价值）

1. **悬浮窗 FloatingWindow** - OMaster 核心差异化
   - 在相机应用上方显示参数
   - 可收起为悬浮球
   - 拍照时随时切换预设
   - 我们当前完全缺失

2. **色卡图书馆 ColorCardLibrary** - 12 个色彩主题
   - 城市暖调、海边清新、森林绿、工业冷调
   - 夕阳余晖、霓虹之夜、复古胶片
   - 极简黑白、春日樱花、咖啡时光、深邃海洋、秋日落叶
   - 每个色卡含：色板 + 主题 + 描述 + 拍摄技巧 + 挑战 + 场景标签

3. **预设详情页 DetailScreen** - 完整实现
   - 顶部图片轮播
   - 参数表格
   - 收藏/编辑/分享
   - 跳转应用

### P1 建议集成（中价值）

4. **配置中心 ConfigCenter** - 统一配置管理
   - 我们当前配置分散在各 Manager
   - 可统一主题/订阅/语言/悬浮窗设置

5. **悬浮窗引导 FloatingWindowGuide** - 首次使用引导
   - 系统权限申请
   - ColorOS / OxygenOS 特殊处理
   - 引导弹窗

6. **关于页/隐私政策/开源许可证** - 标准化
   - 29KB 完整关于页
   - 项目致谢
   - 开源协议展示

### P2 可选集成

7. **品牌主题 BrandTheme** - 多品牌色彩
   - OMaster 支持多品牌主题切换
   - 我们当前固定 OPPO 主题

8. **统计功能** - 同意后开启
   - 预设使用统计
   - 场景使用统计

---

## 五、集成计划

### 第一批：主页重构（本次）
- ✅ 主页采用 OMaster 风格：紧凑标题 + Tab + Pager + 瀑布流
- ✅ 添加计数徽章
- ✅ 添加双向同步
- ✅ 保持我们原有的哈苏/上新/品牌筛选

### 第二批：色卡图书馆
- 集成 ColorCardLibrary 12 个色卡
- 颜色挑战模式（每日推荐色卡）
- 色彩主题教程

### 第三批：悬浮窗
- 实现 FloatingWindowService
- 悬浮球模式
- 拍照时参数参考

### 第四批：详情页增强
- 完整 DetailScreen
- 图片轮播
- 参数可视化

### 第五批：配置中心
- 统一 ConfigCenter
- 主题切换
- 语言切换

### 第六批：标准化页面
- 关于页
- 隐私政策
- 开源许可证
