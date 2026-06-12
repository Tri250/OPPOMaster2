pluginManagement {
    repositories {
        // ===== 国内镜像优先 =====
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
        }
        
        // ===== 腾讯云镜像备用 =====
        maven { 
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
        
        // ===== 华为云镜像备用 =====
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
        // ===== 国内镜像优先 =====
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        
        // ===== 腾讯云镜像备用 =====
        maven { 
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
        
        // ===== 华为云镜像备用 =====
        maven { 
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            name = "Huawei Maven"
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
        
        // ===== JitPack =====
        maven { 
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
    }
}

rootProject.name = "OMaster"
include(":app")