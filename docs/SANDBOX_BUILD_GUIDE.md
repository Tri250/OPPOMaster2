# 沙箱环境构建指南

## 问题说明

沙箱环境网络受限，无法下载 Gradle wrapper 和 Maven 依赖。

## 解决方案

### 方案一：在有网络的环境中预缓存（推荐）

在本地开发环境或有网络的环境中执行：

```bash
# 1. 运行预缓存脚本
./scripts/prepare-cache.sh

# 2. 或手动执行
./gradlew --version          # 下载 Gradle wrapper
./gradlew dependencies       # 下载项目依赖
./gradlew assembleDebug      # 验证构建
```

然后将整个项目（包括 `~/.gradle/caches`）复制到沙箱环境。

### 方案二：使用系统 Gradle + 离线模式

如果沙箱环境已安装 Gradle（如通过 mise）：

```bash
# 使用系统 Gradle 进行离线构建
gradle assembleDebug --offline --stacktrace
```

### 方案三：降低依赖版本

如果 AGP 8.10.0 等新版本插件不在缓存中，可以暂时降低版本：

```toml
# gradle/libs.versions.toml
agp = "8.6.0"      # 降低到已缓存的版本
kotlin = "2.0.21"  # 降低到已缓存的版本
```

## 当前环境状态

- 系统 Gradle: 8.14.4 (已通过 mise 安装)
- Gradle Wrapper: 需要网络下载
- Maven 缓存: 部分依赖已缓存，但 AGP 8.10.0 未缓存

## 快速命令

```bash
# 查看系统 Gradle 版本
gradle --version

# 尝试离线构建
gradle assembleDebug --offline

# 使用构建脚本
./scripts/build-sandbox.sh assembleDebug
```

## 网络优化配置

已在 `gradle.properties` 中配置：

- 连接超时: 10 分钟
- Socket 超时: 10 分钟
- 重试次数: 10 次
- 镜像源: 阿里云、腾讯云

已在 `settings.gradle.kts` 中配置：

- Maven 仓库镜像优先
- 国内镜像源作为首选