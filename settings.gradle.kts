// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云镜像，加速国内开发环境依赖下载
// 更新时间: 2026-06-22

pluginManagement {
    repositories {
        // 本地 Maven 仓库（CI 离线构建用，优先命中已缓存的 AGP/AndroidX 构件）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // Maven Central（Kotlin/Gradle 插件）
        maven { url = uri("https://repo1.maven.org/maven2") }
        // Google Maven（AGP 插件 marker 与插件本体）
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        // 阿里云 Google 镜像（国内加速备选）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云 Gradle 插件镜像（备选）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 阿里云公共仓库（备选）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // Google Maven（AndroidX、Compose、CameraX 等）
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        // Maven Central（Kotlin、Ktor、Coil、Gson 等）
        maven { url = uri("https://repo1.maven.org/maven2") }
        // 友盟 SDK 仓库（umeng-common、umeng-asms）
        maven { url = uri("https://developer.umeng.com/sdk/repo") }
        // 阿里云 Google 镜像（国内加速备选）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共仓库（备选）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云 Central 镜像（备选）
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // JitPack（社区库）
        maven { url = uri("https://jitpack.io") }
        // 本地 Maven 仓库（CI 离线构建用，置后作为兜底）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
    }
}

rootProject.name = "OMaster"
include(":app")
