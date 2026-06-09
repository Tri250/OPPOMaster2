pluginManagement {
    repositories {
        // 官方仓库（优先使用）- 使用显式URL确保正确解析
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        google()
        gradlePluginPortal()
        mavenCentral()

        // 阿里云镜像（后备）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 官方仓库（优先）- 使用显式URL确保正确解析
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        google()
        mavenCentral()

        // 阿里云镜像（后备）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

rootProject.name = "OMaster"
include(":app")