pluginManagement {
    repositories {
        // 国内镜像源配置（经测试验证可用）
        // 1. 腾讯云镜像（测试通过）
        maven {
            url = uri("https://mirrors.cloud.tencent.com/maven/")
            name = "Tencent"
        }
        // 2. 阿里云镜像（使用正确路径）
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
            name = "AliyunPublic"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google/")
            name = "AliyunGoogle"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin/")
            name = "AliyunGradlePlugin"
        }
        // 3. 华为云镜像（备用）
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "HuaweiCloud"
        }

        // 官方仓库（作为后备）
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
        // 国内镜像源配置（经测试验证可用，按优先级排序）
        // 1. 腾讯云镜像（测试通过，优先）
        maven {
            url = uri("https://mirrors.cloud.tencent.com/maven/")
            name = "Tencent"
            content {
                // 优先解析常见依赖组
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("io\\.ktor\\..*")
            }
        }
        // 2. 阿里云镜像（使用正确路径）
        maven {
            url = uri("https://maven.aliyun.com/repository/public/")
            name = "AliyunPublic"
            content {
                includeGroupByRegex(".*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google/")
            name = "AliyunGoogle"
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // 3. 华为云镜像（备用）
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "HuaweiCloud"
        }

        // 官方仓库（后备）
        google()
        mavenCentral()
        // 友盟仓库
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "OMaster"
include(":app")
