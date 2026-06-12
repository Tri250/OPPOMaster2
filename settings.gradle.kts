pluginManagement {
    repositories {
        // ===== 本地Maven仓库优先（手动下载的插件） =====
        maven {
            url = uri("${rootProject.projectDir}/local-maven-repo")
            name = "Local Maven"
        }
        
        // ===== 阿里云镜像优先（网络稳定） =====
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
        
        // ===== Gradle Plugin Portal =====
        maven {
            url = uri("https://plugins.gradle.org/m2")
            name = "Gradle Plugin Portal"
        }
        
        // ===== Google仓库 =====
        google()
        
        // ===== 华为云镜像（备用） =====
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }

        // ===== 官方仓库（最后备用） =====
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 阿里云镜像优先（网络稳定） =====
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
                includeGroupByRegex("org\\.gradle\\..*")
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
