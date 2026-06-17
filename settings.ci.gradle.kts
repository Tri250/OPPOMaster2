// CI 构建专用配置 - 使用标准 Maven 仓库
// GitHub Actions 使用此配置进行正式版构建
// 避免使用本地 Maven 仓库，防止 AGP 版本冲突

pluginManagement {
    repositories {
        // Gradle Plugin Portal - 优先获取 Gradle 插件
        gradlePluginPortal()

        // Maven Central - 标准 Maven 仓库
        mavenCentral()

        // Google - Android Gradle Plugin 和 AndroidX
        google()

        // JitPack - 第三方库
        maven { url = uri("https://www.jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Google - AndroidX 和 Android 库
        google()

        // Maven Central - 标准 Maven 仓库
        mavenCentral()

        // JitPack - 第三方库
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
