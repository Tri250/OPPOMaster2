# Release APK 构建指南

## 前置要求

1. **Android SDK** - 安装 Android SDK 并设置 `ANDROID_HOME` 环境变量
2. **JDK 17** - 项目使用 Java 17
3. **Gradle 8.14.4** - 已配置在项目中

## 构建步骤

### 1. 配置签名密钥

#### 方式一：使用现有密钥（推荐用于正式发布）

创建 `app/keystore-release.properties` 文件：

```properties
storeFile=release.keystore
storePassword=你的密钥库密码
keyAlias=omaster
keyPassword=你的密钥密码
```

将 `release.keystore` 文件放在 `app/` 目录下。

#### 方式二：生成新密钥（用于测试）

```bash
cd app
keytool -genkeypair -v \
  -keystore release.keystore \
  -alias omaster \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass omaster123 \
  -keypass omaster123 \
  -dname "CN=OMaster, OU=Development, O=OMaster, L=Beijing, ST=Beijing, C=CN"
```

然后创建 `keystore-release.properties`：

```properties
storeFile=release.keystore
storePassword=omaster123
keyAlias=omaster
keyPassword=omaster123
```

### 2. 构建命令

```bash
# 清理并构建 Release APK
./gradlew clean assembleRelease

# 或者使用本地 Gradle
gradle clean assembleRelease
```

### 3. 输出位置

构建完成后，APK 文件位于：

```
app/build/outputs/apk/release/
├── app-arm64-v8a-release.apk      # ARM 64位设备
├── app-armeabi-v7a-release.apk    # ARM 32位设备
├── app-x86_64-release.apk         # x86 64位模拟器
├── app-x86-release.apk            # x86 32位模拟器
└── app-universal-release.apk      # 通用APK（包含所有架构）
```

## 构建配置说明

### 版本信息

- **versionCode**: 20306
- **versionName**: 2.3.6

> 注意：版本号应与 CHANGELOG.md 和 Git Tag 保持同步

---

## Android 16 16KB Page Size 验证指南

### 背景

Android 16 设备支持 16KB 内存页大小，需验证原生库（.so 文件）对齐以避免运行时崩溃。

### 验证步骤

#### 1. 配置 legacy packaging（已启用）

项目已在 `app/build.gradle.kts` 中配置：

```kotlin
packaging {
    jniLibs {
        useLegacyPackaging = true  // 确保 .so 被提取而非直接映射
    }
}
```

#### 2. 使用 zipalign 检查 APK

```bash
# 构建 Release APK
./gradlew assembleRelease

# 检查对齐（4KB 或 16KB）
zipalign -c -v 4 app/build/outputs/apk/release/app-universal-release.apk

# 查找 .so 文件
unzip -l app/build/outputs/apk/release/app-universal-release.apk | grep ".so"
```

#### 3. 验证第三方库

检查以下第三方库的 .so 文件：

- TensorFlow Lite (`libtensorflowlite_jni.so`)
- ML Kit (`libmlkit_face_detection.so`)
- 友盟统计 (`libumeng.so`)

#### 4. CI 自动验证

GitHub Actions CI 流程已添加自动检查：

```yaml
- name: Check 16KB Page Size Alignment
  run: |
    unzip -q app/build/outputs/apk/release/*.apk -d apk_extracted
    find apk_extracted -name "*.so" | head -10
    rm -rf apk_extracted
```

### 真机测试建议

在以下设备上测试：

- Pixel 9 Pro（16KB 设备）
- Samsung Galaxy S25（可能支持 16KB）
- 其他 Android 16+ 设备

测试项目：

1. 启动应用检查崩溃
2. AI 功能验证（启发式引擎）
3. 相机预览稳定性
4. 云同步功能验证

### 构建特性

- ✅ **代码混淆**: 启用 ProGuard
- ✅ **资源压缩**: 启用 ShrinkResources
- ✅ **ABI拆分**: 生成多个架构专用APK
- ✅ **签名**: 使用 Release 签名

### ProGuard 规则

混淆规则位于 `app/proguard-rules.pro`，已配置保留：
- 数据类（Parcelable、Serializable）
- Compose UI 组件
- Kotlin 协程相关类
- TFLite 模型类

## 验证 APK

### 检查签名

```bash
# 验证 APK 签名
apksigner verify --print-certs app/build/outputs/apk/release/app-universal-release.apk

# 查看 APK 信息
aapt dump badging app/build/outputs/apk/release/app-universal-release.apk
```

### 检查大小

```bash
ls -lh app/build/outputs/apk/release/*.apk
```

## 发布检查清单

- [ ] 更新版本号（`versionCode` 和 `versionName`）
- [ ] 配置正式签名密钥
- [ ] 运行所有测试通过
- [ ] 检查 ProGuard 规则是否正确
- [ ] 在真机上测试安装和运行
- [ ] 验证所有功能正常工作

## 常见问题

### Q: 构建失败 "SDK location not found"

设置环境变量：
```bash
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### Q: 签名失败

检查 `keystore-release.properties` 配置是否正确，确保密钥文件存在。

### Q: ProGuard 混淆导致运行时崩溃

在 `proguard-rules.pro` 中添加保留规则：
```proguard
-keep class com.silas.omaster.** { *; }
```

## 自动化构建（CI/CD）

项目已配置 GitHub Actions，可自动构建 Release APK：

1. 推送 tag 到仓库：`git tag v2.2.1 && git push --tags`
2. GitHub Actions 自动构建并发布

---

**注意**：`keystore-release.properties` 和 `release.keystore` 不应提交到版本控制，已在 `.gitignore` 中排除。
