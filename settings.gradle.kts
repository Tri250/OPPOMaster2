pluginManagement {
    repositories {
        // 阿里云镜像（优先 - 国内速度快）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（优先 - 国内速度快）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // 官方仓库（后备）
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")