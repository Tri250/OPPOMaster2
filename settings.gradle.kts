pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（优先级最高，沙箱内已下载的依赖）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // ===== 官方仓库 =====
        gradlePluginPortal()
        google()
        mavenCentral()

        // ===== JetBrains Compose =====
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }

        // ===== 阿里云镜像（国内环境可用）=====
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }

        // ===== 腾讯云镜像 =====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（优先级最高）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // ===== 官方仓库 =====
        google()
        mavenCentral()

        // ===== JetBrains Compose =====
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }

        // ===== JitPack（第三方库）=====
        maven { url = uri("https://jitpack.io") }

        // ===== 阿里云镜像（国内环境可用）=====
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // ===== 腾讯云镜像 =====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    }
}

rootProject.name = "OMaster"
include(":app")
