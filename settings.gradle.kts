// settings.gradle.kts - 直接使用官方源
// 更新时间: 2026-06-28

pluginManagement {
    repositories {
        // 本地 Maven 仓库优先
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // Google Maven（AGP、AndroidX 等）
        google()
        // Gradle Plugin Portal
        gradlePluginPortal()
        // Maven Central
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 本地 Maven 仓库
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // Google Maven
        google()
        // Maven Central
        mavenCentral()
        // 友盟 SDK 仓库
        maven {
            url = uri("https://developer.umeng.com/sdk/repo")
            content {
                includeGroup("com.umeng.umsdk")
            }
        }
        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")