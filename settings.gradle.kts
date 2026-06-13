pluginManagement {
    repositories {
        // 本地 Maven 仓库（离线构建优先）
        maven { url = uri(File(rootDir, "local-maven-repo").toURI()) }

        // Gradle 官方插件仓库（优先）
        gradlePluginPortal()

        // Google 和 Maven Central
        google()
        mavenCentral()

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }

        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // 本地 Maven 仓库（离线构建优先）
        maven { url = uri(File(rootDir, "local-maven-repo").toURI()) }

        // 官方仓库（优先）
        google()
        mavenCentral()

        // 腾讯云镜像（备用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 阿里云镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

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