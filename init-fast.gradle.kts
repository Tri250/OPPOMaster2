// 快速构建初始化：限制仓库为 mavenCentral 和 google，避免镜像超时
gradle.settingsEvaluated {
    pluginManagement {
        repositories.clear()
        repositories {
            mavenCentral()
            google()
            gradlePluginPortal()
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories.clear()
        repositories {
            mavenCentral()
            google()
        }
    }
}
