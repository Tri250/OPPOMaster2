pluginManagement {
    repositories {
        // 本地 Maven 仓库（优先使用，包含所有预下载的依赖）
        maven { url = uri(rootProject.projectDir.resolve("local-maven-repo")) }

        // 阿里云镜像（首选）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }

        // 官方仓库（最后备用）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 本地 Maven 仓库（优先使用，包含所有预下载的依赖）
        maven { url = uri(rootProject.projectDir.resolve("local-maven-repo")) }

        // 阿里云镜像（首选）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

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