// 自定义初始化脚本：官方仓库直连，避免本地仓库中的损坏/缺失文件
gradle.settingsEvaluated {
    pluginManagement {
        repositories.clear()
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories.clear()
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://jitpack.io") }
        }
    }
}
