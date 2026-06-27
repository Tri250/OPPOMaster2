// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云 + 华为云镜像，并保留 mavenCentral 兜底，加速国内开发环境依赖下载
// 更新时间: 2026-06-27

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
        // 华为云镜像（备选）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
        // 中央仓库官方源（兜底）
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
        // 华为云镜像（备选，覆盖 Google/AndroidX/Kotlin 等）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven") }
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
        // 本地 Maven 仓库（CI 离线构建用，置后作为兜底）
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 中央仓库官方源（最终兜底）
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
