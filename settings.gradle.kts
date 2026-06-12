pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库优先 =====
        mavenLocal()

        // ===== 阿里云镜像（Maven Central 代理） =====
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
        }

        // ===== Google (Android 依赖) =====
        google()

        // ===== 阿里云 Google 镜像 =====
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }

        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库优先 =====
        mavenLocal()

        // ===== 阿里云镜像（Maven Central 代理） =====
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }

        // ===== Google (Android 依赖) =====
        google()

        // ===== 阿里云 Google 镜像 =====
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }

        // ===== 清华大学镜像 =====
        maven {
            url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/")
            name = "Tsinghua Maven"
        }

        // ===== 友盟仓库 =====
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
            name = "Umeng"
        }

        // ===== JitPack =====
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
    }
}

rootProject.name = "OMaster"
include(":app")
