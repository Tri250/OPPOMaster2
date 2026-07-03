// settings.gradle.kts - CI 专用配置
// 使用本地 Maven 仓库 + 阿里云镜像，CI 环境通过代理访问
// 更新时间: 2026-07-02

pluginManagement {
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 阿里云镜像（国内 CI 加速）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 官方仓库（兜底，确保镜像缺失时仍可解析）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 阿里云镜像（国内 CI 加速）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 友盟 SDK 仓库（限定 group 避免误命中 404）
        maven {
            url = uri("https://developer.umeng.com/sdk/repo")
            content {
                includeGroup("com.umeng.umsdk")
            }
        }
        maven { url = uri("https://jitpack.io") }
        // 官方仓库（兜底，确保镜像缺失时仍可解析）
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
