pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 优先使用可访问的仓库
        mavenCentral()
        gradlePluginPortal()
        // 最后尝试 Google（网络可能不通）
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 优先使用可访问的仓库
        mavenCentral()
        // 最后尝试 Google（网络可能不通）
        google()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
