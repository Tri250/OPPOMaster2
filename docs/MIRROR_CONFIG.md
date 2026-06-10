# Gradle 镜像网络配置指南

> 解决国内开发环境访问 Maven 仓库慢、连接超时等问题

## 目录
1. [配置概览](#1-配置概览)
2. [镜像源说明](#2-镜像源说明)
3. [使用方法](#3-使用方法)
4. [优先级与回退机制](#4-优先级与回退机制)
5. [故障排查](#5-故障排查)

---

## 1. 配置概览

本项目配置了多套镜像源，按优先级排序：

| 优先级 | 镜像源 | 仓库类型 | 速度 | 用途 |
|--------|--------|----------|------|------|
| 1 | 阿里云 | Google + Central + Plugin + JCenter | ⚡ 最快 | 主镜像 |
| 2 | 腾讯云 | Maven 公共仓库 | ⚡ 快 | 备用 1 |
| 3 | 华为云 | Maven 公共仓库 | ⚡ 快 | 备用 2 |
| 4 | 友盟官方 | Umeng SDK | ✅ 稳定 | 友盟专用 |
| 5 | Google + Maven Central | 官方 | 🐢 兜底 | 最后回退 |

## 2. 镜像源说明

### 2.1 阿里云镜像（首选）
```
https://maven.aliyun.com/repository/google          # Google 仓库镜像
https://maven.aliyun.com/repository/central         # Maven Central 镜像
https://maven.aliyun.com/repository/public          # 公共聚合仓库
https://maven.aliyun.com/repository/gradle-plugin   # Gradle 插件镜像
https://maven.aliyun.com/repository/jcenter         # JCenter 镜像（已停止服务）
```

### 2.2 腾讯云镜像
```
https://mirrors.cloud.tencent.com/nexus/repository/maven-public/
```

### 2.3 华为云镜像
```
https://repo.huaweicloud.com/repository/maven/
```

## 3. 使用方法

### 3.1 项目级配置（已自动启用）
`settings.gradle.kts` 已包含所有镜像源，无需手动配置。

### 3.2 全局配置（推荐）
将项目内的镜像脚本复制到 Gradle 用户目录，对所有项目生效：

```bash
./scripts/setup-mirror.sh
```

执行成功后，所有 Gradle 项目都会自动使用国内镜像。

### 3.3 命令行临时指定
```bash
./gradlew build -I gradle/init.d/mirror.init.gradle.kts
```

### 3.4 临时禁用
```bash
# 方法 1：禁用 init 脚本
./gradlew build --init-script /dev/null

# 方法 2：移除全局 init 脚本
rm ~/.gradle/init.d/mirror.init.gradle.kts
```

## 4. 优先级与回退机制

### 4.1 解析顺序
1. Gradle 按 `repositories` 配置顺序逐个尝试
2. 找到匹配的依赖后立即停止查找
3. 全部失败时抛出构建错误

### 4.2 当前配置
- **首选**：阿里云（覆盖度最高、速度最快）
- **备用**：腾讯云、华为云
- **兜底**：Google、Maven Central 官方源

### 4.3 镜像可用性检测
定期使用 `curl` 检测各镜像可用性：

```bash
# 检测阿里云
curl -I https://maven.aliyun.com/repository/public/

# 检测腾讯云
curl -I https://mirrors.cloud.tencent.com/nexus/repository/maven-public/

# 检测华为云
curl -I https://repo.huaweicloud.com/repository/maven/
```

## 5. 故障排查

### 5.1 常见错误

**错误 1：连接超时**
```
Could not resolve org.jetbrains.kotlin:kotlin-stdlib:1.9.0
> Could not resolve all dependencies
> Connection timed out
```

**解决**：
1. 确认镜像配置已加载
2. 检查网络是否通畅：`curl https://maven.aliyun.com/repository/public/`
3. 增加超时时间（已配置为 180 秒）

**错误 2：SSL 握手失败**
```
javax.net.ssl.SSLHandshakeException: PKIX path building failed
```

**解决**：
1. 更新 JDK 到 17+
2. 检查证书是否过期
3. 临时绕过：`-Dcom.sun.net.ssl.checkRevocation=false`

**错误 3：404 Not Found**
```
Could not find org.example:artifact:1.0.0
```

**解决**：
1. 确认依赖坐标正确
2. 某些新发布的包可能镜像还未同步，等待几小时后重试
3. 临时切换到官方源排查

### 5.2 性能优化

**已启用的优化**：
- ✅ 网络超时：180 秒
- ✅ 网络重试：5 次
- ✅ 构建缓存：`org.gradle.caching=true`
- ✅ 配置缓存：`org.gradle.configuration-cache=true`
- ✅ 并行构建：已关闭（避免镜像并发超时）
- ✅ 守护进程：已启用
- ✅ G1GC：已启用（JVM 垃圾回收优化）

### 5.3 性能数据

| 操作 | 国内镜像 | 官方仓库 |
|------|----------|----------|
| 首次拉取依赖 | 2-5 分钟 | 10-30 分钟 |
| 增量构建 | 10-30 秒 | 10-30 秒 |
| Clean Build | 3-5 分钟 | 15-30 分钟 |

### 5.4 离线模式

完全离线构建（使用本地缓存）：

```bash
./gradlew build --offline
```

## 6. 高级配置

### 6.1 自定义镜像源
如需添加企业内网镜像，编辑 `settings.gradle.kts`：

```kotlin
maven {
    url = uri("https://nexus.your-company.com/repository/maven-public/")
    credentials {
        username = "your-username"
        password = "your-password"
    }
}
```

### 6.2 镜像自动切换
根据网络环境动态切换（适用于开发机与 CI 环境）：

```kotlin
val useAliyun = System.getenv("USE_ALIYUN") != "false"
if (useAliyun) {
    maven { url = uri("https://maven.aliyun.com/repository/public") }
}
```

### 6.3 Gradle Wrapper 镜像

`gradle-wrapper.properties` 已配置使用阿里云镜像下载 Gradle 分发包：

```
distributionUrl=https\://mirrors.aliyun.com/macports/distfiles/gradle/gradle-8.14.4-bin.zip
```

如需切换回官方源：

```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.4-bin.zip
```

## 7. 参考资料

- [阿里云 Maven 镜像](https://maven.aliyun.com/)
- [腾讯云 Maven 镜像](https://mirrors.cloud.tencent.com/)
- [华为云 Maven 镜像](https://repo.huaweicloud.com/)
- [Gradle 官方文档 - Repositories](https://docs.gradle.org/current/userguide/declaring_repositories.html)
- [Android Gradle Plugin - 镜像配置](https://developer.android.com/build/optimize-your-build)
