# OMaster 工作树记录

## 当前状态

**日期**: 2026-06-07
**分支**: `trae/solo-agent-geT7oB`
**最新提交**: `d42a6e7 feat: Web调试运行Android APP UI`
**工作树状态**: 干净 (无未提交更改)

---

## 项目结构

### Web UI 展示项目

```
src/
├── App.tsx                    # 主应用入口，路由配置
├── main.tsx                   # React 入口
├── index.css                  # 全局样式
│
├── components/
│   ├── PhoneMockup.tsx        # 手机模拟器框架
│   └── Empty.tsx              # 空状态组件
│
├── pages/
│   ├── HomeScreen.tsx         # 首页 (预设网格展示)
│   ├── FeaturedScreen.tsx     # 精选推荐 (品牌/场景筛选)
│   ├── FeaturesScreen.tsx     # 核心功能入口
│   ├── AboutScreen.tsx        # 关于页面
│   │
│   └── subpages/
│       ├── AISceneRecognitionPage.tsx  # AI场景识别
│       ├── AIFineTunePage.tsx          # AI微调
│       ├── SmartOptimizePage.tsx       # 智能优化
│       ├── WatermarkPage.tsx           # 水印编辑
│       ├── PresetManagerPage.tsx       # 预设管理
│       ├── ParamAdjustPage.tsx         # 参数调节
│       ├── ThemeSettingsPage.tsx       # 主题设置
│       ├── DarkModePage.tsx            # 深色模式
│       ├── NotificationPage.tsx        # 通知设置
│       └── PrivacyPage.tsx             # 隐私政策
│
├── store/
│   └── appStore.ts            # Zustand 状态管理
│
├── hooks/
│   └── useTheme.ts            # 主题 Hook
│
├── lib/
│   └── utils.ts               # 工具函数
│
└── assets/
    └── react.svg              # React Logo
```

### 配置文件

```
package.json           # 依赖配置
vite.config.ts         # Vite 构建配置
tailwind.config.js     # Tailwind CSS 配置
tsconfig.json          # TypeScript 配置
eslint.config.js       # ESLint 配置
postcss.config.js      # PostCSS 配置
```

---

## 功能模块

### 1. 首页 (HomeScreen)
- Tab 切换: 全部/收藏/我的
- 预设网格展示 (瀑布流布局)
- HNCS 标签显示
- 收藏按钮

### 2. 精选推荐 (FeaturedScreen)
- 品牌筛选: OPPO/realme/vivo/荣耀/小米
- 场景筛选: 人像/风景/夜景/美食/街拍/建筑
- 预设卡片展示
- 应用参数按钮

### 3. 核心功能 (FeaturesScreen)
- AI 智能功能区域 (4个功能)
- 专业工具区域 (2个功能)
- 品牌特色区域 (2个功能)
- 点击进入功能操作界面

### 4. 功能子页面

| 功能 | 页面 | 特性 |
|-----|------|------|
| AI场景识别 | AISceneRecognitionPage | 36+场景识别、扫描动画、场景选择 |
| AI微调 | AIFineTunePage | 实时预览、一键优化、参数滑块 |
| 智能优化 | SmartOptimizePage | HDR增强、降噪、锐化 |
| 水印编辑 | WatermarkPage | 4种模板、5个位置、自定义文字 |
| 预设管理 | PresetManagerPage | 搜索、网格/列表视图、收藏、批量操作 |
| 参数调节 | ParamAdjustPage | ISO/快门/光圈/白平衡、快捷档位 |
| 主题设置 | ThemeSettingsPage | 6种品牌主题 |
| 深色模式 | DarkModePage | 跟随系统/浅色/深色 |
| 通知设置 | NotificationPage | 通知开关 |
| 隐私政策 | PrivacyPage | 隐私政策内容 |

### 5. 关于页面 (AboutScreen)
- 应用信息卡片
- 设置列表入口
- 版本信息

---

## 状态管理 (appStore.ts)

```typescript
interface AppState {
  currentPage: PageType;           // 当前主页面
  currentSubPage: SubPageType;     // 当前子页面
  selectedTab: number;             // 首页 Tab
  selectedBrand: string | null;    // 精选品牌筛选
  selectedScene: string | null;    // 精选场景筛选
  features: Feature[];             // 功能列表
  aiParams: AIParams;              // AI微调参数
  cameraParams: CameraParams;      // 相机参数
  watermarkSettings: Watermark;    // 水印设置
  theme: ThemeType;                // 主题
  darkMode: DarkModeType;          // 深色模式
  notifications: Notification;     // 通知设置
}
```

---

## 技术栈

- **框架**: React 18 + TypeScript
- **构建**: Vite 6.4.3
- **样式**: Tailwind CSS
- **状态**: Zustand
- **图标**: Lucide React

---

## Git 历史

```
d42a6e7 feat: Web调试运行Android APP UI
57ebd09 feat: Web调试运行Android APP UI
241c7b8 feat: Web调试运行Android APP UI
b7693de feat: 修复首页功能入口导航链路、添加精选推荐应用功能
de10312 feat: 完成核心功能入口链接和Release APK发布准备
```

---

## 远程仓库

- **仓库**: https://github.com/Tri250/OPPOMaster2
- **分支**: main (已合并), trae/solo-agent-geT7oB (当前)