pluginManagement {
    repositories {
        // ===== 本地 Maven 仓库优先 =====
        mavenLocal()

        // ===== Google 仓库优先（AGP 唯一来源） =====
        google()

        // ===== 国内镜像（用于其他插件） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
            name = "Tencent Gradle Plugins"
        }
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
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
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }

        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 本地 Maven 仓库优先 =====
        mavenLocal()

        // ===== Google 仓库优先 =====
        google()

        // ===== 国内镜像（备用） =====
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven {
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
        }
        maven {
            url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/")
            name = "Tsinghua Maven"
        }

        mavenCentral()

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
