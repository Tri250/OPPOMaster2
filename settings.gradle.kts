// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云镜像，加速国内开发环境依赖下载
// 更新时间: 2026-06-22

pluginManagement {
    repositories {
        // 本地 Maven 仓库（CI 离线构建用，优先命中已缓存的 AGP/AndroidX 构件）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 阿里云镜像（国内加速优先）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 腾讯云镜像（聚合镜像，全仓库一体化）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 官方仓库（兜底保证可用性）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 阿里云 Google 镜像（AndroidX、Compose、CameraX 等，国内加速优先）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云公共仓库（Kotlin、Ktor、Coil、Gson 等）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 阿里云 Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 腾讯云镜像（聚合镜像，全仓库一体化）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 友盟 SDK 仓库（umeng-common、umeng-asms）
        maven {
            url = uri("https://developer.umeng.com/sdk/repo")
            content {
                // 仅允许 umeng 相关 group，避免其他依赖误命中该仓库返回的 404 HTML
                includeGroup("com.umeng.umsdk")
            }
        }
        // JitPack（社区库）
        maven { url = uri("https://jitpack.io") }
        // 本地 Maven 仓库（CI 离线构建用）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 官方仓库（兜底保证可用性）
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
