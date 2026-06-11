pluginManagement {
    repositories {
        // 本地 Maven 仓库（优先）
        mavenLocal()
        
        // Google Maven（显式 URL）
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        
        // 官方仓库
        google()
        mavenCentral()
        gradlePluginPortal()
        
        // 国内镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 本地 Maven 仓库（优先）
        mavenLocal()
        
        // Google Maven（显式 URL）
        maven { url = uri("https://dl.google.com/dl/android/maven2") }
        
        // 官方仓库
        google()
        mavenCentral()
        
        // 国内镜像（备用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "OMaster"
include(":app")