// settings.ci.gradle.kts - CI 专用配置
// 使用本地 Maven 仓库 + 阿里云镜像，CI 环境无需代理
// 更新时间: 2026-06-22

pluginManagement {
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://developer.umeng.com/sdk/repo") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OMaster"
include(":app")
