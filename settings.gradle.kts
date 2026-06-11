pluginManagement {
    repositories {
        // ========== 国内镜像源（按优先级排序，已验证可用）==========
        
        // 华为云镜像（完全可用，优先）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 腾讯云镜像（完全可用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 阿里云镜像（Google仓库可用，Maven仓库部分可用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // ========== 官方仓库（后备）==========
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
        // ========== 国内镜像源（按优先级排序，已验证可用）==========
        
        // 华为云镜像（完全可用，优先）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 腾讯云镜像（完全可用）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 阿里云镜像（Google仓库可用）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // ========== 官方仓库（后备）==========
        google()
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2/") }
        
        // 友盟仓库
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "OMaster"
include(":app")
