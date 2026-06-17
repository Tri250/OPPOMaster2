# OMaster

OMaster 是一款面向摄影爱好者与专业人士的 Android 预设管理工具，支持 Lightroom 参数解析、水印编辑、设备型号定制、批量导出等核心功能。

## 功能特性

- **预设管理**：按品牌、风格、场景分类浏览与收藏预设
- **参数解析**：支持哈苏、富士、索尼、徕卡等主流品牌 Lightroom 参数
- **水印编辑器**：自定义品牌水印、大师印记、XPAN 宽幅水印
- **设备型号定制**：自定义拍摄设备型号，打造个性化影调签名
- **批量导出**：支持多种格式与分辨率导出
- **云端同步**：支持第三方云服务同步预设（可选）

## 版本信息

- 当前版本：**v1.3.1**
- 最低系统：Android 7.0 (API 24)
- 目标系统：Android 15 (API 35)

## 快速开始

```bash
# 开发调试
./gradlew assembleDebug

# 发布构建（需配置 release 签名）
./gradlew assembleRelease
```

## 发布说明

### v1.3.1

- 修复 CI 构建配置，统一版本号为 v1.3.1
- 替换大量硬编码主题为 MaterialTheme 主题色
- 完善无障碍访问支持（contentDescription）
- 优化 ProGuard / R8 混淆规则
- AI 场景识别、AI 微调、智能优化、哈苏色彩科学等实验性功能默认关闭，待模型文件就绪后开放

## 技术栈

- Kotlin + Jetpack Compose
- Hilt 依赖注入
- Room 本地数据库
- TensorFlow Lite（实验性功能）
- Ktor 网络客户端

## 许可证

Copyright © 2026 Silas. All rights reserved.
