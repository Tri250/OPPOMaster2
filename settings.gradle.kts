pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 优先使用本地缓存的依赖，避免网络请求
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 当前环境：直连 Maven Central / Google / Gradle Plugin Portal
        // 避免使用超时不可达的国内镜像
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 优先使用本地缓存的依赖，避免网络请求
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 当前环境：直连 Maven Central / Google
        // 避免使用超时不可达的国内镜像
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
