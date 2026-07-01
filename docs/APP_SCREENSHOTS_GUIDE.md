# OMaster Play Store 截图制作指南

## 一、截图规格要求

| 属性 | 要求 |
|------|------|
| 格式 | PNG 或 JPEG |
| 数量 | 最少 2 张，最多 8 张 |
| 比例 | 推荐 9:16（如 1080×1920） |
| 大小 | 每张 ≤ 8MB |
| 内容 | 展示应用核心功能界面 |

## 二、推荐截图内容（8 张）

| 编号 | 场景 | 截图内容 |
|------|------|---------|
| 01 | 首页 | 预设列表 + 品牌筛选 + 底部导航 |
| 02 | 预设详情 | 预设参数展示 + 收藏/应用按钮 |
| 03 | AI 微调 | 图片选择 + 自动调参后效果展示 |
| 04 | 哈苏取景器 | 实时取景器 + 滤镜效果 + 反模式检测 |
| 05 | LUT 资源库 | LUT 列表 + 分类筛选 + 下载按钮 |
| 06 | 智能优化 | 优化参数 + 亮度/对比度/清晰度评分 |
| 07 | 云端同步 | 预设源管理 + 同步状态 |
| 08 | 悬浮窗 | 悬浮窗在其他应用上显示的快捷切换 |

## 三、截图方法

### 方法一：Android Studio Device Explorer（推荐）

1. 连接 Android 设备或启动模拟器
2. 安装 Release APK：`adb install app-release.apk`
3. 打开 Android Studio → Logcat → 点击相机图标截图
4. 或使用命令行：`adb exec-out screencap -p > screenshot_01.png`

### 方法二：自动化截图脚本

```bash
#!/bin/bash
# 在模拟器/真机上自动截图
SCREENSHOTS=(
    "01_home"          # 首页
    "02_preset_detail" # 预设详情
    "03_ai_finetune"   # AI微调
    "04_viewfinder"    # 取景器
    "05_lut"           # LUT
    "06_optimize"      # 智能优化
    "07_sync"          # 云端同步
    "08_floating"      # 悬浮窗
)

for screen in "${SCREENSHOTS[@]}"; do
    echo "截图: $screen"
    adb exec-out screencap -p > "phoneScreenshots/${screen}.png"
    sleep 1
done
```

### 方法三：使用 AVD 快照

1. 在模拟器中创建各个场景的快照
2. 从快照恢复后截图
3. 确保截图分辨率为 1080×1920

## 四、注意事项

- 截图应使用**中文界面**（Play Store 中国区）
- 避免包含个人敏感信息
- 避免使用系统状态栏中的通知内容
- 截图背景应干净整洁
- 建议使用浅色模式截图（更广泛兼容）
- 不要包含其他应用的图标或内容