pluginManagement {
    repositories {
        // ===== 国内镜像优先（阿里云 - 最稳定） =====
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

        // ===== 腾讯云镜像（备用） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
            name = "Tencent Gradle Plugins"
        }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
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
        // ===== 国内镜像优先（阿里云 - 最稳定） =====
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
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
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
            content {
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\..*")
                includeGroupByRegex("com\\.google\\.mlkit\\..*")
                includeGroupByRegex("com\\.google\\.android\\..*")
            }
        }

        // ===== 腾讯云镜像（备用） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("io\\.coil-kt")
                includeGroupByRegex("com\\.google\\..*")
            }
        }

        // ===== 华为云镜像（备用） =====
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }

        // ===== 清华大学镜像（备用） =====
        maven {
            url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/")
            name = "Tsinghua Maven"
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

        // ===== JitPack（Compose Navigation 等） =====
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }

        // ===== TensorFlow Lite 官方仓库 =====
        // TFLite 某些版本可能需要官方源
        maven {
            url = uri("https://google.maven.org/")
            name = "Google Maven Direct"
            content {
                includeGroupByRegex("org\\.tensorflow\\..*")
            }
        }
    }
}

rootProject.name = "OMaster"
include(":app")