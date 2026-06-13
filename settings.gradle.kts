pluginManagement {
    repositories {
        // 腾讯云镜像（沙箱环境首选，响应 0.05s）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 阿里云镜像（google 仓库含 Gradle 插件）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 华为云镜像（备用）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }

        // 官方仓库（最后备用）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 腾讯云镜像（沙箱环境首选，响应 0.05s）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // 华为云镜像（备用）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }

        // 官方仓库（最后备用）
        google()
        mavenCentral()

        // 友盟仓库（必须使用官方源）
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }

        // JitPack（第三方库）
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")