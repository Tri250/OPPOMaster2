# API 密钥配置指南

本文档介绍如何在 OMaster 应用中配置和管理云端 API 密钥。

## 目录

- [概述](#概述)
- [获取 API 密钥](#获取-api-密钥)
- [配置 API 密钥](#配置-api-密钥)
- [API 端点配置](#api-端点配置)
- [安全注意事项](#安全注意事项)
- [常见问题](#常见问题)

---

## 概述

OMaster 使用云端 API 提供以下功能：

- **AI 场景识别**：智能识别拍摄场景并推荐最佳水印参数
- **AI 微调**：根据用户偏好自动调整水印效果
- **预设同步**：云端备份和同步用户预设
- **用户认证**：跨设备账户同步

## 获取 API 密钥

### 1. 注册开发者账户

1. 访问 [OMaster 开发者平台](https://developer.omaster.app)
2. 使用邮箱或手机号注册账户
3. 完成账户验证

### 2. 创建应用

1. 登录开发者控制台
2. 进入「应用管理」页面
3. 点击「创建新应用」
4. 填写应用信息：
   - 应用名称：OMaster
   - 应用类型：移动应用
   - 平台：Android

### 3. 获取密钥

创建应用后，系统会生成以下凭证：

| 凭证类型 | 说明 | 用途 |
|---------|------|------|
| API Key | 应用唯一标识 | 识别应用来源 |
| Secret Key | 应用密钥 | 签名验证（服务端使用） |

> ⚠️ **重要**：Secret Key 仅显示一次，请妥善保存。不要在客户端代码中硬编码 Secret Key。

## 配置 API 密钥

### 方法一：应用内配置（推荐）

1. 打开 OMaster 应用
2. 进入「设置」→「高级设置」→「API 配置」
3. 输入您的 API Key
4. 点击「验证」确认密钥有效
5. 保存配置

### 方法二：配置文件

编辑 `api_config.json` 文件：

```json
{
  "aiApiEndpoint": "https://api.omaster.app/ai",
  "presetApiEndpoint": "https://api.omaster.app/presets",
  "authApiEndpoint": "https://api.omaster.app/auth",
  "apiVersion": "v1"
}
```

### 方法三：代码配置

```kotlin
val settingsManager = SettingsManager.getInstance(context)

// 设置 API Key
settingsManager.cloudApiKey = "your-api-key-here"

// 验证密钥
if (settingsManager.validateApiKey()) {
    // 密钥有效，可以使用云端功能
}

// 自定义 API 端点（可选）
settingsManager.setCustomApiEndpoints(
    aiEndpoint = "https://custom-api.example.com/ai",
    presetEndpoint = "https://custom-api.example.com/presets"
)
```

## API 端点配置

### 默认端点

| 服务 | 端点地址 | 说明 |
|------|---------|------|
| AI 推理 | `https://api.omaster.app/ai` | 场景识别、智能调参 |
| 预设同步 | `https://api.omaster.app/presets` | 云端预设管理 |
| 用户认证 | `https://api.omaster.app/auth` | 登录、注册、同步 |

### 自定义端点

如果您部署了私有服务器，可以修改端点地址：

```kotlin
settingsManager.setCustomApiEndpoints(
    aiEndpoint = "https://your-server.com/ai",
    presetEndpoint = "https://your-server.com/presets",
    authEndpoint = "https://your-server.com/auth"
)
```

### 重置为默认端点

```kotlin
settingsManager.resetApiEndpoints()
```

## 安全注意事项

### ✅ 推荐做法

1. **使用环境变量**：在构建时注入 API Key
2. **密钥轮换**：定期更换 API Key
3. **权限控制**：为不同环境使用不同的密钥
4. **监控使用**：定期检查 API 调用量

### ❌ 禁止做法

1. **不要硬编码密钥**：避免在代码中直接写入密钥
2. **不要提交密钥**：不要将密钥提交到版本控制系统
3. **不要分享密钥**：每个应用/环境使用独立密钥
4. **不要明文存储**：使用 Android Keystore 加密存储

### 密钥存储安全

OMaster 使用以下方式保护您的 API 密钥：

```kotlin
// 密钥存储在 SharedPreferences 中
// 建议在生产环境使用 EncryptedSharedPreferences

// 验证密钥格式
if (settingsManager.validateApiKeyFormat(key)) {
    settingsManager.cloudApiKey = key
}
```

### 添加到 .gitignore

确保以下文件已添加到 `.gitignore`：

```gitignore
# API 配置文件
app/src/main/assets/api_config.json
app/src/main/assets/api_config.local.json

# 本地配置
local.properties
*.local
```

## 常见问题

### Q: API Key 验证失败怎么办？

**A:** 请检查：
1. 密钥格式是否正确（至少16字符）
2. 密钥是否已过期
3. 网络连接是否正常
4. API 端点配置是否正确

### Q: 如何切换测试/生产环境？

**A:** 使用不同的 API Key 和端点配置：

```kotlin
// 测试环境
settingsManager.setCustomApiEndpoints(
    aiEndpoint = "https://test-api.omaster.app/ai"
)
settingsManager.cloudApiKey = "test-api-key"

// 生产环境
settingsManager.resetApiEndpoints()
settingsManager.cloudApiKey = "prod-api-key"
```

### Q: API 调用次数有限制吗？

**A:** 是的，根据您的套餐等级：
- 免费版：1000 次/天
- 专业版：10000 次/天
- 企业版：无限制

### Q: 如何查看 API 使用情况？

**A:** 登录开发者控制台，查看「用量统计」页面。

---

## 技术支持

如有问题，请联系：

- 邮箱：support@omaster.app
- 文档：https://docs.omaster.app
- GitHub：https://github.com/omaster-app

---

*最后更新：2024年*