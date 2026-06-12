pluginManagement {
    repositories {
        // ===== 国内镜像优先（腾讯云 - 沙箱环境最快） =====
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
        // ===== 国内镜像优先（腾讯云 - 沙箱环境最快 0.05s） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
            content {
                // 优先处理常见依赖
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
                includeGroupByRegex("io\\.coil-kt")
                includeGroupByRegex("com\\.squareup\\..*")
                includeGroupByRegex("com\\.google\\..*")
                includeGroupByRegex("org\\.gradle\\..*")
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
                includeGroupByRegex("com\\.squareup\\..*")
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
        google()
        mavenCentral()

        // ===== 友盟仓库 =====
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
            name = "Umeng"
        }

        // ===== JitPack（Compose Navigation 等） =====
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
    }
}

rootProject.name = "OMaster"
include(":app")
