# Release APK 构建指南与自检规范

## 前置要求

1. **Android SDK** - 安装 Android SDK 并设置 `ANDROID_HOME` 环境变量
2. **JDK 17** - 项目使用 Java 17
3. **Gradle 8.14.4** - 已配置在项目中
4. **Git** - 用于版本标签管理

## Release 自检规范标准

> 以下检查项必须在每次发布构建前逐项核对并勾选，确保发布包质量与可追溯性。

### 一、版本号与配置检查

- [ ] `app/build.gradle.kts` 中的 `versionCode` 已递增且符合规范（主版本 × 10000 + 次版本 × 100 + 修订版本）
- [ ] `app/build.gradle.kts` 中的 `versionName` 已更新为对外发布版本号（本次目标：**2.0.0**）
- [ ] `README.md` 中的版本信息已同步更新
- [ ] `CHANGELOG.md` 已记录本次发布的变更说明
- [ ] Git Tag 已按 `v{versionName}` 格式创建（如 `v2.0.0`）

### 二、构建工具链与依赖检查

- [ ] `gradle/libs.versions.toml` 中的依赖版本为最新兼容版本
- [ ] Android Gradle Plugin (AGP) 版本与 Gradle 版本兼容
- [ ] Kotlin 版本与 AGP、Compose Compiler 版本兼容
- [ ] `settings.gradle.kts` 中的国内镜像配置正确可用
- [ ] `gradle-wrapper.properties` 中的 Gradle 分发 URL 配置正确
- [ ] 本地 Maven 仓库（`local-maven-repo/`）中的离线依赖已更新或网络镜像可用

### 三、签名与安全检查

- [ ] 已配置正式 Release 签名（`keystore-release.properties` + `release.keystore`）
- [ ] 签名密钥文件未提交到版本控制
- [ ] 友盟 AppKey 等敏感信息通过 `local.properties` 或环境变量注入，未硬编码
- [ ] ProGuard / R8 混淆规则已覆盖所有需要保留的类（数据类、Compose UI、协程、TFLite 等）

### 四、资源与打包检查

- [ ] Web 前端资源（`index.html`、`src/`、`public/` 等）未打包进 APK
- [ ] `sourceSets["main"].assets` 和 `sourceSets["main"].res` 仅指向 Android 模块目录
- [ ] 不需要的多语言资源、测试资源、调试资源已排除
- [ ] 大型二进制资源（模型、字体、LUT 等）已启用 `noCompress` 优化
- [ ] ABI 拆分配置正确，生成通用 APK 与各架构 APK

### 五、构建与验证检查

- [ ] `./gradlew clean assembleRelease` 构建成功，无错误
- [ ] Lint 检查通过（`abortOnError` 在 Release 构建时开启）
- [ ] 单元测试与插桩测试全部通过
- [ ] APK 签名验证通过（`apksigner verify --print-certs`）
- [ ] APK 信息检查通过（`aapt dump badging`）
- [ ] APK 体积在预期范围内
- [ ] 在真机或模拟器上完成安装、启动、核心功能冒烟测试

### 六、发布后归档检查

- [ ] `app/build/outputs/mapping/release/mapping.txt` 已自动备份到 `app/mapping/mapping-{versionName}-{versionCode}.txt`
- [ ] 各架构 APK 与通用 APK 已归档
- [ ] Git Tag 已推送至远程仓库
- [ ] CI/CD 构建产物与本地构建产物一致

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

- **versionCode**: 20000
- **versionName**: 2.0.0

> 注意：版本号应与 CHANGELOG.md 和 Git Tag 保持同步

### 构建特性

- ✅ **代码混淆**: 启用 ProGuard
- ✅ **资源压缩**: 启用 ShrinkResources
- ✅ **ABI拆分**: 生成多个架构专用APK
- ✅ **签名**: 使用 Release 签名
- ✅ **Web 资源排除**: 仅打包 Android 模块资源

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

### Q: 国内依赖下载慢或失败

检查 `settings.gradle.kts` 中的阿里云镜像配置：
- `https://maven.aliyun.com/repository/google`
- `https://maven.aliyun.com/repository/public`
- `https://maven.aliyun.com/repository/central`

必要时在 `gradle-wrapper.properties` 中切换为国内 Gradle 分发镜像：
- `https://mirrors.cloud.tencent.com/gradle/gradle-8.14.4-bin.zip`（当前可用）
- `https://mirrors.aliyun.com/gradle/gradle-8.14.4-bin.zip`（如可用）

## 自动化构建（CI/CD）

项目已配置 GitHub Actions，可自动构建 Release APK：

1. 推送 tag 到仓库：`git tag v2.0.0 && git push --tags`
2. GitHub Actions 自动构建并发布

---

**注意**：`keystore-release.properties`、`release.keystore` 和 `local.properties` 不应提交到版本控制，已在 `.gitignore` 中排除。
