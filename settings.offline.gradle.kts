// 离线构建专用配置 - 优先使用本地 Maven 仓库
// 使用方法: gradle -c settings.offline.gradle.kts assembleRelease --offline

pluginManagement {
    repositories {
        // 本地 Maven 仓库优先
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // 如果本地没有，尝试 Gradle 缓存
        maven { url = uri("${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1") }
        // 最后尝试外部仓库（仅当不启用离线模式时）
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 本地 Maven 仓库优先
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        // Gradle 缓存
        maven { url = uri("${System.getProperty("user.home")}/.gradle/caches/modules-2/files-2.1") }
        // 外部仓库
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
