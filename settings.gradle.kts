pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（优先级最高，沙箱内已下载的依赖）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // ===== 系统 Maven 仓库（Debian/Ubuntu 系统级依赖）=====
        maven { url = uri("file:///usr/share/maven-repo") }

        // ===== 阿里云镜像（沙箱走代理后实测 10-20ms 极速响应）=====
        // gradle-plugin 子路径 → 插件（含 Kotlin / AGP）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // public 子路径 → Maven Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // google 子路径 → Google Android Maven 镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // central 子路径 → 备用 Maven Central 镜像
        maven { url = uri("https://maven.aliyun.com/repository/central") }

        // ===== 腾讯云镜像（沙箱代理可达 200-400ms，全镜像合一）=====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // ===== JetBrains 官方仓库（Kotlin 插件 - 使用 cache-redirector)=====
        maven { url = uri("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/public/p/kotlin/kotlin-gradle-plugins") }
        maven { url = uri("https://cache-redirector.jetbrains.com/gradlePluginPortal") }

        // ===== 官方仓库（通过代理可达） =====
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（优先级最高）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // ===== 系统 Maven 仓库（Debian/Ubuntu 系统级依赖）=====
        maven { url = uri("file:///usr/share/maven-repo") }

        // ===== 阿里云镜像（实测 10-20ms，优先使用）=====
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // ===== 腾讯云镜像（聚合镜像，含 Google + Central + Plugin）=====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // ===== Google Android Maven 官方（沙箱代理可达）=====
        google()

        // ===== Maven Central 官方（沙箱代理可达）=====
        mavenCentral()

        // ===== JitPack（第三方库）=====
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
