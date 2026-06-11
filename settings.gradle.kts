pluginManagement {
    repositories {
        // ========== 国内镜像源（按优先级排序）==========
        
        // 阿里云镜像（推荐，稳定性好）
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        // 华为云镜像
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 腾讯云镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 清华大学镜像
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }

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
        // ========== 国内镜像源（按优先级排序）==========
        
        // 阿里云镜像（推荐，稳定性好）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        
        // 华为云镜像
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 腾讯云镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 清华大学镜像
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }

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
