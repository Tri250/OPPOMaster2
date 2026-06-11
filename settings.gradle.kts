pluginManagement {
    repositories {
        // 阿里云镜像（包含 AGP 插件）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        // 官方仓库（兜底）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（包含 AndroidX 和其他依赖）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        // 官方仓库（兜底）
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")