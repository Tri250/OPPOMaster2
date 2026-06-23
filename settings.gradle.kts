// settings.gradle.kts - 国内镜像加速构建配置
// 使用阿里云 + 腾讯云镜像，加速国内开发环境依赖下载
// 更新时间: 2026-06-22

pluginManagement {
    repositories {
        maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
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
