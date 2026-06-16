# Google Play 商店资料

---

## 📋 必需资产清单

| # | 资产项目 | 规格要求 | 当前状态 | 备注 |
|---|---------|---------|---------|------|
| 1 | 应用图标 | 512×512 PNG，32位（不含Alpha通道） | ⚠️ 需修改 | `ic_launcher-playstore.png` 已存在，512×512，但当前为 RGBA（含Alpha），需转换为 RGB（不含Alpha） |
| 2 | 宣传图（Feature Graphic） | 1024×500 PNG | ❌ 缺失 | 尚未创建 |
| 3 | 手机截图 | 最少2张，最多8张；16:9 或 9:16；每张≤8MB | ❌ 缺失 | 尚未制作，详见 [APP_SCREENSHOTS_GUIDE.md](APP_SCREENSHOTS_GUIDE.md) |
| 4 | 平板截图（可选） | 最少2张，最多8张；每张≤8MB | ❌ 缺失 | 可选，建议7寸和10寸各准备 |
| 5 | 简短描述 | ≤80字符 | ✅ 已完成 | 中英文均已填写 |
| 6 | 完整描述 | ≤4000字符 | ✅ 已完成 | 中英文均已填写 |
| 7 | 隐私政策 URL | 必须为公开可访问的 HTTPS URL | ❌ 未完成 | 当前为占位符 `[填写隐私政策URL]`，需替换为实际 HTTPS 地址 |
| 8 | 内容分级问卷 | IARC 问卷 | ✅ 已完成 | 已填写，最终分级 PEGI:3 / USK:All / ESRB:Everyone / IARC:3+ |
| 9 | 应用签名密钥 | PEPK 导出或 Google 托管签名 | ❌ 未完成 | 尚未配置应用签名 |

---

## ✅ 上架前检查清单（Pre-Launch Checklist）

- [ ] **应用图标**：将 `ic_launcher-playstore.png` 从 RGBA 转换为 RGB（去除Alpha通道），确保为 512×512 32位 PNG
- [ ] **宣传图**：创建 1024×500 PNG 格式的 Feature Graphic，展示应用核心功能与品牌特色
- [ ] **手机截图**：制作 2–8 张手机截图（推荐 9:16 比例，如 1080×1920），每张≤8MB
- [ ] **平板截图**：（可选）制作平板截图
- [ ] **简短描述**：确认中英文简短描述均≤80字符 ✅
- [ ] **完整描述**：确认中英文完整描述均≤4000字符 ✅
- [ ] **隐私政策**：部署隐私政策页面并填写公开可访问的 HTTPS URL
- [ ] **内容分级**：确认 IARC 问卷答案准确无误 ✅
- [ ] **应用签名密钥**：选择 PEPK 密钥导出或 Google 托管签名方式，完成签名配置
- [ ] **联系信息**：补充开发者网站、支持邮箱等占位符字段

---

## 应用基本信息

### 应用名称

| 语言 | 名称 |
|------|------|
| 中文 | OPPOMaster2 - 大师影像参数库 |
| 英文 | OPPOMaster2 - Master Photo Presets |

---

## 商店展示资料

### 简短描述（80字符以内）

**中文：**
```
AI智能调色，哈苏大师风格一键出片
```

**英文：**
```
AI-powered color grading, Hasselblad master style in one tap
```

---

### 完整描述（4000字符以内）

**中文：**

```
OPPOMaster2 - 大师影像参数库，为摄影爱好者打造的专业调色工具。

🎨 核心功能：

【AI场景识别】
- 智能识别拍摄场景，自动匹配最佳参数
- 支持人像、风景、夜景、美食、街拍等多种场景
- 一键应用大师级色彩配方

【AI微调】
- 基于AI的智能参数微调
- 根据照片特征自动优化色彩、对比度、饱和度
- 轻松获得专业级调色效果

【哈苏HNCS色彩科学】
- 还原哈苏自然色彩解决方案
- 真实呈现肤色、天空、植被等自然色彩
- 大师级色彩风格一键获得

【智能优化】
- 自动分析照片质量
- 智能调整曝光、对比度、清晰度
- 一键提升照片质感

【LUT色彩查找表】
- 支持导入自定义LUT文件
- 丰富的预设LUT库
- 一键分享和导出

【水印编辑】
- 自定义水印样式和位置
- 支持EXIF信息水印
- 多种水印模板可选

📸 预设库：
- 30+款大师级预设配方
- 涵盖人像、风景、夜景、美食等多个场景
- 持续更新，不断丰富

📱 支持品牌：
- OPPO 全系列
- realme 全系列
- vivo 全系列
- 荣耀 全系列
- 小米 全系列

💡 特色功能：
- 悬浮窗快捷切换预设
- 预设云同步更新
- 自定义预设创建与编辑
- 收藏夹管理
- 深色模式支持

🎯 适用人群：
- 手机摄影爱好者
- 追求专业调色效果的用户
- 喜欢尝试不同风格的创作者
- 希望快速出片的摄影新手

立即下载，开启你的大师级影像之旅！
```

**英文：**

```
OPPOMaster2 - Master Photo Presets, a professional color grading tool crafted for photography enthusiasts.

🎨 Core Features:

【AI Scene Recognition】
- Intelligently identify shooting scenes and automatically match optimal parameters
- Support for portrait, landscape, night, food, street photography, and more
- Apply master-level color recipes with one tap

【AI Fine-Tuning】
- AI-powered intelligent parameter adjustment
- Automatically optimize color, contrast, and saturation based on photo characteristics
- Easily achieve professional-grade color grading results

【Hasselblad HNCS Color Science】
- Reproduce Hasselblad Natural Color Solution
- True-to-life rendering of skin tones, skies, vegetation, and other natural colors
- Master-level color style at your fingertips

【Smart Optimization】
- Automatic photo quality analysis
- Intelligent adjustment of exposure, contrast, and clarity
- One-tap enhancement for photo quality

【LUT Color Lookup Tables】
- Support for importing custom LUT files
- Rich preset LUT library
- One-tap sharing and export

【Watermark Editor】
- Customizable watermark styles and positions
- Support for EXIF information watermarks
- Multiple watermark templates available

📸 Preset Library:
- 30+ master-level preset recipes
- Covering portrait, landscape, night, food, and more scenarios
- Continuously updated with new content

📱 Supported Brands:
- OPPO full series
- realme full series
- vivo full series
- Honor full series
- Xiaomi full series

💡 Special Features:
- Floating window for quick preset switching
- Cloud sync for preset updates
- Custom preset creation and editing
- Favorites management
- Dark mode support

🎯 Target Users:
- Mobile photography enthusiasts
- Users seeking professional color grading effects
- Creators who enjoy experimenting with different styles
- Photography beginners looking for quick results

Download now and start your master-level photography journey!
```

---

## 应用分类

| 项目 | 选择 |
|------|------|
| 应用类别 | 摄影 |
| 次级类别 | 无 |

---

## 目标受众

### 年龄分级
- 适合所有年龄段

### 目标用户群体
- 摄影爱好者
- 内容创作者
- 社交媒体用户
- 手机摄影用户

### 地区定位
- 全球发布
- 主要市场：中国大陆、东南亚、欧洲、北美

---

## 内容分级

| 内容类型 | 分级 |
|---------|------|
| 暴力内容 | 无 |
| 裸露内容 | 无 |
| 性内容 | 无 |
| 药物使用 | 无 |
| 用户生成内容 | 无 |
| 用户交互 | 无 |

**IARC 分级问卷答案：**
- 应用不包含任何暴力、裸露、性内容或药物使用
- 应用不包含用户生成内容
- 应用不包含用户交互功能
- 应用不包含数字购买或广告

**最终分级：**
- PEGI: 3
- USK: All ages
- ESRB: Everyone
- IARC: 3+

---

## 联系信息

| 项目 | 内容 |
|------|------|
| 开发者网站 | [填写开发者网站] |
| 支持邮箱 | [填写支持邮箱] |
| 隐私政策链接 | [填写隐私政策URL] |

---

## 图形资源要求

### 应用图标
- 尺寸：512 x 512 px
- 格式：32位 PNG（不含Alpha通道）
- 要求：简洁、辨识度高、与应用功能相关

### 宣传图
- 尺寸：1024 x 500 px
- 格式：PNG 或 JPEG
- 内容：展示应用核心功能和特色

### 应用截图
- 最少：2张
- 最多：8张
- 格式：PNG 或 JPEG
- 要求：展示应用核心功能界面

---

## 关键词建议

```
摄影, 调色, 预设, 滤镜, AI, 哈苏, 色彩, 大师, 手机摄影, 专业
```

```
photography, color grading, presets, filters, AI, Hasselblad, color, master, mobile photography, professional
```