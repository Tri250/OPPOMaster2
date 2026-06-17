// 优化网络配置 - 使用可访问的镜像源
// 腾讯云镜像可访问 Google 仓库内容，Maven Central 可访问普通 Java 依赖

pluginManagement {
    repositories {
        // 腾讯云镜像 - 可访问 Google 仓库内容（AGP、AndroidX 等）
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }

        // Maven Central - 普通 Java 依赖
        mavenCentral()

        // Gradle Plugin Portal - Gradle 插件
        gradlePluginPortal()

        // JitPack - 第三方库
        maven { url = uri("https://www.jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 腾讯云镜像 - 可访问 Google 仓库内容（AGP、AndroidX 等）
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        }

        // Maven Central - 普通 Java 依赖
        mavenCentral()

        // JitPack - 第三方库
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
