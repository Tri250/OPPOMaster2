pluginManagement {
    repositories {
        // ========== 阿里云镜像（国内首选，速度最快）==========
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // ========== 腾讯云镜像（备用）==========
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/gradle-plugins/") }
        // ========== 华为云镜像（备用）==========
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        // ========== 官方仓库（兜底）==========
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ========== 阿里云镜像（国内首选，速度最快）==========
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        // ========== 腾讯云镜像（备用）==========
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // ========== 华为云镜像（备用）==========
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        // ========== 友盟仓库 ==========
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }
        // ========== 官方仓库（兜底）==========
        google()
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2") }
    }
}

rootProject.name = "OMaster"
include(":app")
