pluginManagement {
    repositories {
        // ===== 腾讯云镜像（沙箱环境首选，响应 0.05s） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
            name = "Tencent Gradle Plugins"
        }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }

        // ===== 阿里云镜像（备用） =====
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }

        // ===== 华为云镜像（备用） =====
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }

        // ===== 官方仓库（最后备用） =====
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 腾讯云镜像（沙箱环境首选，响应 0.05s） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
            content {
                // 包含所有常见依赖组
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("org\\.jetbrains\\.kotlinx\\..*")
                includeGroupByRegex("io\\.coil-kt")
                includeGroupByRegex("io\\.coil-kt\\..*")
                includeGroupByRegex("com\\.squareup\\..*")
                includeGroupByRegex("com\\.google\\..*")
                includeGroupByRegex("com\\.google\\.code\\..*")
                includeGroupByRegex("com\\.google\\.gson")
                includeGroupByRegex("io\\.ktor\\..*")
                includeGroupByRegex("io\\.mockk")
                includeGroupByRegex("org\\.tensorflow\\..*")
                includeGroupByRegex("junit")
            }
        }

        // ===== 阿里云镜像（备用） =====
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("io\\.coil-kt")
                includeGroupByRegex("com\\.google\\..*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
            content {
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\..*")
                includeGroupByRegex("com\\.google\\.mlkit\\..*")
            }
        }

        // ===== 华为云镜像（备用） =====
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }

        // ===== 官方仓库（最后备用） =====
        google {
            content {
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\..*")
            }
        }
        mavenCentral {
            content {
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("org\\.jetbrains\\.kotlinx\\..*")
                includeGroupByRegex("io\\.coil-kt")
                includeGroupByRegex("io\\.ktor\\..*")
                includeGroupByRegex("io\\.mockk")
                includeGroupByRegex("org\\.tensorflow\\..*")
                includeGroupByRegex("com\\.squareup\\..*")
                includeGroupByRegex("com\\.google\\.code\\.gson")
                includeGroupByRegex("junit")
            }
        }

        // ===== 友盟仓库（必须使用官方） =====
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
            name = "Umeng"
            content {
                includeGroupByRegex("com\\.umeng\\..*")
                includeGroupByRegex("com\\.uc\\..*")
            }
        }

        // ===== JitPack（第三方库） =====
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
}

rootProject.name = "OMaster"
include(":app")