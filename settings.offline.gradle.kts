// settings.offline.gradle.kts - 完全离线构建配置
// 使用方法: 复制为 settings.gradle.kts 后构建

pluginManagement {
    repositories {
        maven { url = uri("/workspace/local-maven-repo") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { url = uri("/workspace/local-maven-repo") }
    }
}

rootProject.name = "OMaster"
include(":app")