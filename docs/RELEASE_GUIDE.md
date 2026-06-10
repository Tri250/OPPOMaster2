# OMaster Android Release 发布指南

> 行业最严标准的 Release APK 发布流程

## 目录
1. [前置准备](#1-前置准备)
2. [签名配置](#2-签名配置)
3. [构建配置](#3-构建配置)
4. [代码混淆与收缩](#4-代码混淆与收缩)
5. [安全检查](#5-安全检查)
6. [发布前自检](#6-发布前自检)
7. [构建与发布](#7-构建与发布)

---

## 1. 前置准备

### 1.1 环境要求
- JDK 17+
- Android SDK 35
- Gradle 8.14.4+
- 有效的 Release 签名密钥库

### 1.2 检查清单
- [ ] 已安装 JDK 17（`java -version`）
- [ ] 已配置 Android SDK（`ANDROID_HOME`）
- [ ] 已有 Release 签名密钥库（`release.keystore`）
- [ ] 已设置签名配置（`keystore-release.properties`）

---

## 2. 签名配置

### 2.1 生成 Release 密钥库
```bash
# 使用项目提供的脚本
./scripts/generate-keystore.sh

# 或手动生成
keytool -genkeypair \
  -alias omaster \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore app/release.keystore \
  -storepass <STORE_PASSWORD> \
  -keypass <KEY_PASSWORD> \
  -dname "CN=OMaster, OU=Development, O=Silas, L=Beijing, ST=Beijing, C=CN"
```

### 2.2 配置 keystore-release.properties
```bash
# 复制模板
cp app/keystore-release.properties.template app/keystore-release.properties

# 编辑配置（填入真实密码）
vim app/keystore-release.properties
```

内容示例：
```properties
storeFile=release.keystore
storePassword=<YOUR_STORE_PASSWORD>
keyAlias=omaster
keyPassword=<YOUR_KEY_PASSWORD>
```

### 2.3 ⚠️ 安全提醒
- `keystore-release.properties` 已在 `.gitignore` 中排除
- `release.keystore` 已在 `.gitignore` 中排除
- **绝不能**将这两个文件提交到版本控制
- 丢失密钥库将**无法**更新应用，请妥善保管

---

## 3. 构建配置

### 3.1 Release 构建特性

| 配置项 | 值 | 说明 |
|--------|----|----|
| `isMinifyEnabled` | `true` | R8/ProGuard 混淆 |
| `isShrinkResources` | `true` | 资源压缩 |
| `signingConfig` | `release` | Release 签名 |
| `ABI splits` | 4 + universal | 按架构拆分 APK |
| `optimizationpasses` | 5 | 优化次数 |

### 3.2 BuildConfig 字段

| 字段 | Debug | Release | 说明 |
|------|-------|---------|------|
| `UMENG_APPKEY` | ✅ | ✅ | 友盟 AppKey |
| `UMENG_CHANNEL` | `default` | `official` | 友盟渠道 |
| `API_BASE_URL` | `https://test-api.omaster.app/` | `https://api.omaster.app/` | API 基础 URL |
| `ENABLE_LOG` | `true` | `false` | 调试日志开关 |
| `ENABLE_NET_LOG` | `true` | `false` | 网络日志开关 |
| `ENABLE_CRASH_REPORT` | `true` | `true` | 崩溃上报 |
| `ENABLE_UMENG` | `true` | `true` | 友盟统计 |
| `BUILD_TIME` | 自动 | 自动 | 构建时间 |
| `GIT_SHA` | 自动 | 自动 | Git 提交 SHA |

### 3.3 ABI 拆分
生成的 APK：
- `app-armeabi-v7a-release.apk` (32-bit ARM)
- `app-arm64-v8a-release.apk` (64-bit ARM)
- `app-x86-release.apk` (32-bit x86)
- `app-x86_64-release.apk` (64-bit x86)
- `app-universal-release.apk` (通用包，包含所有架构)

---

## 4. 代码混淆与收缩

### 4.1 R8/ProGuard 规则
- 位置：`app/proguard-rules.pro`
- 默认规则：`proguard-android-optimize.txt` + 自定义规则
- 优化次数：5
- 包含规则：
  - Compose、Material 3、Navigation
  - Gson、Kotlinx Serialization
  - ViewModel、Parcelable、Serializable
  - 友盟、Coil、ML Kit
  - TensorFlow Lite、Ktor

### 4.2 资源压缩
- 启用 `isShrinkResources = true`
- 排除规则：
  - `/META-INF/{AL2.0,LGPL2.1}`
  - `/META-INF/DEPENDENCIES`
  - `/META-INF/LICENSE*`
  - `/META-INF/NOTICE*`
  - `/META-INF/*.kotlin_module`

### 4.3 注意事项
- 资源 ID 在 Release 模式下会被重新映射
- 反射访问的类/方法必须保留
- 第三方库的 ProGuard 规则已配置

---

## 5. 安全检查

### 5.1 网络安全配置
- 文件：`app/src/main/res/xml/network_security_config.xml`
- ✅ 默认禁止明文流量
- ✅ 仅信任系统 CA 证书
- ✅ 生产域名白名单（`silas-omaster.com`, `api.omaster.app`）
- ✅ Debug 模式允许本地网络

### 5.2 备份规则
- 文件：`app/src/main/res/xml/backup_rules.xml`
- ✅ 排除订阅凭证
- ✅ 排除 API Key 存储
- ✅ 排除缓存、下载、模型

### 5.3 数据提取规则（Android 12+）
- 文件：`app/src/main/res/xml/data_extraction_rules.xml`
- ✅ 云备份排除敏感数据
- ✅ 设备传输包含偏好但排除凭证

### 5.4 AndroidManifest 安全
- ✅ `usesCleartextTraffic="false"`
- ✅ `networkSecurityConfig` 已配置
- ✅ `hardwareAccelerated="true"`（性能优化）
- ✅ `largeHeap="true"`（图片处理需要）
- ✅ `allowBackup="true"`（配合 backup_rules）
- ✅ `requestLegacyExternalStorage="false"`

### 5.5 敏感数据加密
- ✅ 订阅数据：`SecurityCrypto.encrypt()` 加密存储
- ✅ API Key：`SecurityCrypto.saveSecure()` 加密存储
- ✅ 加密算法：AES/GCM/NoPadding（256位密钥）
- ✅ 密钥存储：Android Keystore（硬件级别隔离）

### 5.6 权限管理
- 最小权限原则：仅请求必要的权限
- 运行时权限：危险权限按需申请
- 旧版权限：`maxSdkVersion` 限制

---

## 6. 发布前自检

### 6.1 运行自检脚本
```bash
./scripts/release-check.sh
```

### 6.2 自检项目（23+ 项）
1. 签名配置检查
2. build.gradle.kts 配置
3. ProGuard 规则
4. AndroidManifest 权限
5. 网络安全配置
6. 备份规则
7. 源代码安全（无 printStackTrace、无 println、无硬编码密钥）
8. 版本控制安全（.gitignore）

### 6.3 自检输出
```
通过: 23  警告: 1  失败: 0
✓ 所有关键检查通过！可以开始构建 Release APK。
```

---

## 7. 构建与发布

### 7.1 构建命令
```bash
# 清理
./gradlew clean

# 构建 Release APK
./gradlew :app:assembleRelease

# 构建 AAB（推荐用于 Google Play）
./gradlew :app:bundleRelease

# 输出位置
ls -la app/build/outputs/apk/release/
ls -la app/build/outputs/bundle/release/
```

### 7.2 验证 APK
```bash
# 检查 APK 签名
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-arm64-v8a-release.apk

# 检查 APK 内容
$ANDROID_HOME/build-tools/35.0.0/aapt dump badging app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### 7.3 性能指标
- APK 体积（预估）：
  - arm64-v8a: ~15-20MB
  - armeabi-v7a: ~12-15MB
  - 通用包: ~25-30MB
- 启动时间：< 1.5s（冷启动）
- 内存占用：< 80MB（基础运行）

### 7.4 灰度发布
1. 内部测试（Internal Testing）：5% 用户
2. 封闭测试（Closed Beta）：20% 用户
3. 开放测试（Open Beta）：50% 用户
4. 正式发布（Production）：100% 用户

### 7.5 监控指标
- 崩溃率：< 0.1%
- ANR 率：< 0.05%
- 启动成功率：> 99.5%
- 关键路径转化率：> 80%

---

## 附录：常见问题

### Q1: 构建失败提示 "Keystore was tampered with"
**A**: 密钥库密码错误。请检查 `keystore-release.properties` 配置。

### Q2: 启动后崩溃
**A**: 可能是 ProGuard 规则问题。检查日志并添加缺失的 `-keep` 规则。

### Q3: 网络请求失败
**A**: 确认域名在 `network_security_config.xml` 白名单中，且使用 HTTPS。

### Q4: 资源丢失
**A**: 检查 `isShrinkResources` 是否误删了资源。可在 `keep.xml` 中标记保留。

---

## 变更日志

- 2026-06-10: 创建 Release 发布指南
- 2026-06-10: 完成 Release APK 行业最严标准自检
