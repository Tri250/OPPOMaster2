pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 优先使用本地缓存的依赖，避免网络请求
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 官方仓库优先，国内镜像作为fallback
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/kotlin/kotlin-gradle-plugins") }
        maven { url = uri("https://cache-redirector.jetbrains.com/gradlePluginPortal") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（沙箱离线构建模式）=====
        // 优先使用本地缓存的依赖，避免网络请求
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 官方仓库优先，国内镜像作为fallback
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    }
}

rootProject.name = "OMaster"
include(":app")
