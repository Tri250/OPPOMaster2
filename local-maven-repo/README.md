# 本地 Maven 依赖仓库

> **目的**：在沙箱环境（无外网或网络受限场景）下保证 Gradle 构建所需依赖全部就位。
>
> **总大小**：约 200MB ｜ **总文件数**：240+ 个 aar/jar/pom

## 镜像策略

| 优先级 | 镜像 | URL |
|--------|------|-----|
| 1（主） | 阿里云 | `https://maven.aliyun.com/repository/{public,google,gradle-plugin}` |
| 2（备） | 腾讯云 | `https://mirrors.cloud.tencent.com/nexus/repository/maven-public` |
| 3（兜底） | 官方 | `https://dl.google.com/android/maven2` / `https://repo1.maven.org/maven2` |

脚本（[download-all-deps.sh](../../download-all-deps.sh)）自动按此顺序回退，单个文件失败时自动尝试下一个镜像。

## 已下载的核心依赖

| 类别 | 关键包 |
|------|--------|
| **Android Gradle Plugin** | `com.android.tools.build:gradle:8.7.3` |
| **Kotlin** | `kotlin-stdlib:2.1.20`, `kotlin-reflect:2.1.20`, `kotlin-compiler-embeddable:2.1.20` |
| **Compose BOM** | `androidx.compose:compose-bom:2025.01.01`（实际 ui/foundation/material3 等 1.7.7 / 1.3.1） |
| **AndroidX Core** | `core-ktx:1.15.0`, `activity-compose:1.9.3`, `lifecycle-*:2.8.7` |
| **Navigation** | `navigation-compose:2.8.5` |
| **DataStore** | `datastore-preferences:1.1.1` |
| **Networking** | `ktor-client-*:3.0.3`, `coil-compose:2.7.0`, `gson:2.11.0` |
| **Coroutines** | `kotlinx-coroutines-*:1.10.1`, `kotlinx-serialization-json:1.8.0` |
| **ML Kit** | `face-detection:16.1.7` |
| **TensorFlow Lite** | `tensorflow-lite:2.16.1`, `tensorflow-lite-gpu:2.16.1`, `tensorflow-lite-support:0.4.4` |
| **友盟** | `common:9.8.9`, `asms:1.8.7.2` |
| **Testing** | `junit:4.13.2`, `mockk:1.13.12`, `espresso-core:3.7.0` |

## KMP Split 注意事项

Compose 1.7+ 引入 Kotlin Multiplatform，artifact 拆分为：
- `<artifact>` (KMP common，仅 200-300 字节占位)
- `<artifact>-android` (实际 Android 实现，约 1-5MB)

**Gradle 会自动选择 `-android` 后缀**，本仓库两种都预下载以保证兼容性。

## 离线构建使用

`settings.gradle.kts` 已配置 `maven { url = uri(file("local-maven-repo").toURI()) }`，Gradle 会优先从本地仓库解析依赖，无网络时仍可编译。

## 重新下载/补充

```bash
bash download-all-deps.sh
```

脚本会**自动跳过已存在且大小正确的文件**，重复执行是安全的。
