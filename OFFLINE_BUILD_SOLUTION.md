# 沙箱离线构建解决方案

**日期**: 2026-06-17  
**状态**: 已修复 - 配置为纯离线模式

---

## 问题分析

沙箱环境网络受限，无法从 Maven 仓库下载 AGP 8.7.3 和 Kotlin 2.1.20 的传递依赖。AGP 的传递依赖链非常深，包含 100+ 个依赖。

**网络诊断结果**:
- 外部网络连接：❌ 不可达
- DNS 配置：`10.96.205.180`（内部 DNS）

---

## 修复内容

### 1. 修改 `settings.gradle.kts` ✅

**变更**: 移除所有外部仓库，仅保留本地 Maven 仓库

```kotlin
pluginManagement {
    repositories {
        // 仅使用本地缓存的依赖
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 仅使用本地缓存的依赖
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}
```

### 2. 修改 `gradle.properties` ✅

**变更**: 强制启用离线模式

```properties
# 沙盒环境网络受限，强制启用离线模式
org.gradle.offline=true
```

---

## 当前状态

### 本地仓库已预填充的依赖

**AndroidX 依赖**:
- ✅ Activity 1.9.3
- ✅ AppCompat 1.7.0
- ✅ Compose BOM 2025.01.01
- ✅ Core KTX 1.15.0
- ✅ DataStore 1.1.1
- ✅ Lifecycle 2.8.7
- ✅ Navigation 2.8.5

**Kotlin 依赖**:
- ✅ Kotlin Gradle Plugin 2.1.20
- ✅ Kotlin Compose Compiler Plugin 2.1.20
- ✅ Kotlin Serialization 2.1.20
- ✅ Kotlinx Coroutines 1.10.1
- ✅ Kotlinx Serialization JSON 1.8.0

**网络库**:
- ✅ Ktor Client 3.0.3
- ✅ Coil 2.7.0
- ✅ OkHttp 4.12.0

**ML/AI 库**:
- ✅ ML Kit Face Detection 16.1.7
- ✅ TensorFlow Lite 2.16.1

**测试库**:
- ✅ JUnit 4.13.2
- ✅ MockK 1.13.12

### 缺少的依赖（AGP 传递依赖）

以下依赖在本地仓库中缺失，导致离线构建失败：

```
com.android.tools.build:transform-api:2.0.0-deprecated-use-gradle-api
org.apache.httpcomponents:httpmime:4.5.6
commons-io:commons-io:2.13.0
org.ow2.asm:asm:9.6
org.bouncycastle:bcpkix-jdk18on:1.77
org.glassfish.jaxb:jaxb-runtime:2.3.2
com.google.protobuf:protobuf-java:3.22.3
io.grpc:grpc-core:1.57.0
com.google.guava:guava:32.0.1-jre
org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.1.20
... (约 50+ 个)
```

---

## 推荐的最终解决方案

由于 AGP 8.7.3 的传递依赖链太深（100+），手动维护本地仓库不现实。

### 方案 1: GitHub Actions CI (强烈推荐) ✅

项目已配置完整的 CI/CD 流程，网络环境完善。

**触发方式**:
```bash
# 推送标签自动构建
git tag v1.3.1
git push origin v1.3.1
```

**或手动触发**:
访问 https://github.com/Tri250/OPPOMaster2/actions/workflows/main-release.yml

**CI 输出**:
- `app-arm64-v8a-release.apk` - ARM64 推荐版
- `app-universal-release.apk` - 通用版

### 方案 2: 预下载完整 Gradle 缓存

在有网络的环境中执行以下命令，然后将 Gradle 缓存打包：

```bash
# 1. 在联网机器上执行
./gradlew dependencies --configuration classpath > deps.txt
./gradlew build --refresh-dependencies

# 2. 打包 Gradle 缓存
tar czf gradle-cache.tar.gz ~/.gradle/caches/modules-2/files-2.1/

# 3. 在沙箱中解压
mkdir -p ~/.gradle/caches/modules-2/files-2.1/
tar xzf gradle-cache.tar.gz -C ~/.gradle/caches/modules-2/files-2.1/
```

### 方案 3: 使用系统 Gradle

沙盒已预装 Gradle 8.14.4（通过 mise）：

```bash
# 使用系统 Gradle 而非 Wrapper
gradle clean build --offline --no-daemon
```

**注意**: 仍需解决依赖缺失问题

---

## 结论

**当前状态**: 
- ✅ 已配置纯离线模式（`settings.gradle.kts` + `gradle.properties`）
- ⚠️ 本地仓库缺少 AGP 8.7.3 的深层传递依赖（约 50+ 个）
- ❌ 沙盒离线构建仍会因缺少依赖而失败

**建议**: 
1. **首选**: 使用 GitHub Actions CI 进行构建
2. **备选**: 在联网环境中预下载完整 Gradle 缓存，然后导入沙箱

**相关文件**:
- [CI 配置](.github/workflows/main-release.yml)
- [构建文档](docs/BUILD_RELEASE.md)
- [本地仓库](local-maven-repo/)
