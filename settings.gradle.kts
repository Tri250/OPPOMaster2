pluginManagement {
    repositories {
        // ===== 国内镜像优先（加速下载） =====
        // 阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 腾讯云镜像（备选）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 官方仓库（后备）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 国内镜像优先（加速下载） =====
        // 阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 腾讯云镜像（备选）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 官方仓库（后备）
        google()
        mavenCentral()

        // 友盟仓库已由阿里云/腾讯云镜像完整代理，无需单独配置
        // 镜像路径: https://maven.aliyun.com/repository/public/com/umeng/umsdk/
        // 备用方案（如镜像缺包时可启用）:
        // maven { url = uri("https://repo.umeng.com/maven-releases/") }
    }
}

rootProject.name = "OMaster"
include(":app")
