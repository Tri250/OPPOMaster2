# OMaster 沙箱/离线环境构建指南

## 问题背景

沙箱或内网开发环境通常无法访问外网，导致 Gradle 无法下载 Android Gradle Plugin (AGP)、Kotlin 插件、AndroidX/CameraX/ML Kit 等依赖。本指南提供几种应对方案。

## 方案一：GitHub Actions CI 构建（推荐）

项目已配置 `.github/workflows/main-release.yml`，在有网络的 CI 环境中自动完成构建与发布。

### 操作步骤

1. 本地完成代码修改并推送到 `origin/main`。
2. 打 Tag 并推送：
   ```bash
   git tag v2.1.0
   git push origin v2.1.0
   ```
3. GitHub Actions 自动触发，构建多架构 Release APK 并发布到 GitHub Releases。

### 优点
- 不依赖本地沙箱网络。
- 自动完成签名（CI 使用 debug keystore 兜底，正式签名请在 CI secrets 中配置）。
- 生成构建报告与 Release 说明。

## 方案二：导出 Gradle 缓存到沙箱

如果你有一台可联网的开发机，可预先将完整 Gradle 缓存打包，再迁移到沙箱。

### 1. 在联网机器上预下载依赖

```bash
./gradlew assembleRelease --no-daemon
```

### 2. 打包 Gradle 缓存

```bash
tar czf gradle-cache.tar.gz ~/.gradle/caches ~/.gradle/wrapper
```

### 3. 迁移到沙箱并解压

```bash
# 在沙箱中执行
rm -rf ~/.gradle/caches ~/.gradle/wrapper
tar xzf gradle-cache.tar.gz -C ~/
```

### 4. 使用本地 Gradle 离线构建

```bash
export ANDROID_HOME=/root/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
gradle assembleRelease --offline --no-daemon
```

> 注意：沙箱使用系统已安装的 `gradle` 命令，避免 `./gradlew` 再次下载 wrapper。

## 方案三：维护本地 Maven 仓库

项目已预留 `local-maven-repo/` 目录，并配置了本地仓库优先解析。可将联网机器下载的依赖按 Maven 坐标复制到该目录，或运行 `scripts/fix-plugin-markers.sh` 修复插件 marker。

### 补充 plugin marker

如果本地仓库已有插件实现 jar，但缺少 Gradle 插件 portal 的 marker artifact，运行：

```bash
bash scripts/fix-plugin-markers.sh
```

该脚本会根据 `gradle/libs.versions.toml` 中的版本自动生成 marker POM。

## 方案四：Docker/虚拟机离线镜像

长期离线开发建议构建一个包含完整 Android SDK + Gradle + Gradle 缓存的 Docker 镜像或虚拟机模板，沙箱直接复用该镜像。

## 当前沙箱状态

- ✅ Android SDK 已安装：`/root/android-sdk`
- ✅ `local.properties` 已创建
- ✅ 国内镜像已配置（阿里云）
- ✅ Release 签名已生成（测试用，未提交到 git）
- ✅ 环境自检脚本：`scripts/check-build-env.sh`
- ❌ Gradle 缓存缺少 AGP 8.7.3 / Kotlin 2.0.21 等关键依赖

因此当前沙箱无法完成首次完整离线构建，建议优先使用 **方案一 GitHub Actions CI**。

## 相关脚本

| 脚本 | 作用 |
|------|------|
| `install-android-sdk.sh` | 一键安装 Android SDK |
| `scripts/check-build-env.sh` | 检查构建前置条件 |
| `scripts/fix-plugin-markers.sh` | 修复本地 Maven 仓库 plugin marker |
| `build-release.sh` | Release 构建入口脚本 |
