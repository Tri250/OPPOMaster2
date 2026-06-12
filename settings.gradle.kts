pluginManagement {
    repositories {
        // ===== 腾讯云镜像优先（诊断确认可达） =====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // ===== 官方仓库（诊断确认可达） =====
        google()
        mavenCentral()
        gradlePluginPortal()

        // ===== 阿里云镜像（备用） =====
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ===== 腾讯云镜像优先（诊断确认可达） =====
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // ===== 官方仓库（诊断确认可达） =====
        google()
        mavenCentral()

        // ===== 阿里云镜像（备用） =====
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }

        // ===== 友盟仓库 =====
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "OMaster"
include(":app")
