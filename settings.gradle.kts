pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库（优先级最高）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 腾讯云镜像（沙箱环境首选，响应 0.05s）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }

        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 官方仓库（最后备用）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库（优先级最高）=====
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }

        // 腾讯云镜像（沙箱环境首选，响应 0.05s）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

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