pluginManagement {
    repositories {
        // 阿里云镜像（首选，响应快）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 华为云镜像（备用）
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

        // 本地 Maven 仓库（最后备用）
        maven { url = uri(rootProject.projectDir.resolve("local-maven-repo")) }

        // 官方仓库（最后备用）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        // 阿里云镜像（首选，响应快）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

        // 华为云镜像（备用）
        maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

        // 本地 Maven 仓库（最后备用）
        maven { url = uri(rootProject.projectDir.resolve("local-maven-repo")) }

        // 官方仓库（最后备用）
        google()
        mavenCentral()

        // 友盟仓库
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }

        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
