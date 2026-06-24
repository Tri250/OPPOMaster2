// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云镜像，加速国内开发环境依赖下载
// 更新时间: 2026-06-24

pluginManagement {
    repositories {
        // 阿里云 Gradle 插件镜像（Kotlin/AGP 插件优先从此解析）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 阿里云 Google 镜像（AGP 等）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 本地 Maven 仓库（CI 离线构建用，作为兜底；当前缓存不完整，不优先使用）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 阿里云 Google 镜像（AndroidX、Compose、CameraX 等）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共仓库（Kotlin、Ktor、Coil、Gson 等）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云 Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 友盟 SDK 仓库（umeng-common、umeng-asms）
        maven { url = uri("https://developer.umeng.com/sdk/repo") }
        // JitPack（社区库）
        maven { url = uri("https://jitpack.io") }
        // 本地 Maven 仓库（CI 离线构建用，置后作为兜底）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}

rootProject.name = "OMaster"
include(":app")
