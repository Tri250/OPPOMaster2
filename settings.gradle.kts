pluginManagement {
    repositories {
        // ===== 国内镜像优先（加速下载） =====
        // 阿里云镜像 - 不使用 content 过滤以确保插件能正确解析
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 腾讯云镜像（备选）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 官方仓库（后备）
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 国内镜像优先（加速下载） =====
        // 阿里云镜像 - 使用内容过滤减少网络请求
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                // Android/Google 相关
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                // Kotlin 相关
                includeGroupByRegex("org\\.jetbrains.*")
                includeGroupByRegex("org\\.kotlinx.*")
                // 第三方库
                includeGroupByRegex("io\\.ktor.*")
                includeGroupByRegex("io\\.coil.*")
                includeGroupByRegex("com\\.squareup.*")
                includeGroupByRegex("io\\.github.*")
                // 友盟
                includeGroupByRegex("com\\.umeng.*")
                // TensorFlow
                includeGroupByRegex("org\\.tensorflow.*")
                // ML Kit
                includeGroupByRegex("com\\.google\\.mlkit.*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // 腾讯云镜像（备选）
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("org\\.jetbrains.*")
                includeGroupByRegex("org\\.kotlinx.*")
            }
        }

        // 官方仓库（后备）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()

        // 友盟仓库已由阿里云/腾讯云镜像完整代理，无需单独配置
        // 镜像路径: https://maven.aliyun.com/repository/public/com/umeng/umsdk/
        // 备用方案（如镜像缺包时可启用）:
        // maven { url = uri("https://repo.umeng.com/maven-releases/") }
    }
}

rootProject.name = "OMaster"
include(":app")
