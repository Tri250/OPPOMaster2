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

- 当前版本：**v2.0.0**
- 最低系统：Android 7.0 (API 24)
- 目标系统：Android 16 (API 36)

## 快速开始

```bash
# 开发调试
./gradlew assembleDebug

# 发布构建（需配置 release 签名）
./gradlew assembleRelease
```

## 发布说明

### v1.9.0

- 全面升级 Android SDK 目标平台至 API 36
- 深度全链路审查修复，确保 Android 核心功能模块 100% 完整实现
- 哈苏之眼 AI 场景识别、AI 微调、智能优化完整集成
- 哈苏色彩科学引擎（HNCS 3.0）真实渲染管线
- 水印编辑器、预设管理、参数调节全链路交互完善
- 云端同步、订阅管理、胶片推荐等功能模块完整实现

## 技术栈

- Kotlin + Jetpack Compose
- Hilt 依赖注入
- Room 本地数据库
- TensorFlow Lite（实验性功能）
- Ktor 网络客户端

## 许可证

Copyright © 2026 Silas. All rights reserved.
