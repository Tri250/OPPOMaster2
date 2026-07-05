// settings.gradle.kts - CI 专用配置
// 使用本地 Maven 仓库 + 阿里云镜像，CI 环境通过代理访问
// 更新时间: 2026-07-02

pluginManagement {
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 沙箱/当前环境阿里云镜像连接不稳定，优先使用 mavenCentral 与官方仓库
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 沙箱/当前环境阿里云镜像连接不稳定，优先使用 mavenCentral
        mavenCentral()
        // 友盟 SDK 仓库（限定 group 避免误命中 404）
        maven {
            url = uri("https://developer.umeng.com/sdk/repo")
            content {
                includeGroup("com.umeng.umsdk")
            }
        }
        maven { url = uri("https://jitpack.io") }
        google()
    }
}

rootProject.name = "OMaster"
include(":app")
