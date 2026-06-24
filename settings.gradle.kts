// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云镜像，加速国内开发环境依赖下载
// 更新时间: 2026-06-24

pluginManagement {
    repositories {
        // 本地 Maven 仓库（CI 离线构建用，优先命中已缓存的 AGP/AndroidX 构件）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 阿里云 Gradle 插件镜像（Kotlin/AGP 插件优先从此解析）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 阿里云 Google 镜像（AGP 等）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 腾讯云聚合镜像（全仓库一体化）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 官方仓库（兜底）
        gradlePluginPortal()
        google()
        mavenCentral()
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
        // 腾讯云聚合镜像（全仓库一体化）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 友盟 SDK 仓库（umeng-common、umeng-asms）
        maven { url = uri("https://developer.umeng.com/sdk/repo") }
        // JitPack（社区库）
        maven { url = uri("https://jitpack.io") }
        // 官方仓库（兜底，确保镜像未同步的依赖可解析）
        google()
        mavenCentral()
        // 本地 Maven 仓库（CI 离线构建用，置后作为兜底）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}

rootProject.name = "OMaster"
include(":app")
