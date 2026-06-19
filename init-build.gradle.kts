// 构建专用初始化脚本：仅使用可达的国内镜像 + Google/MavenCentral，避免 Cloudflare  hosts 导致连接挂起
gradle.settingsEvaluated {
    pluginManagement {
        repositories.clear()
        repositories {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            google()
            mavenCentral()
        }
    }
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories.clear()
        repositories {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }
            google()
            mavenCentral()
        }
    }
}
