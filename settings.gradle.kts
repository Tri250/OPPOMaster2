pluginManagement {
    repositories {
        // ===== 腾讯云镜像（优先，国内稳定） =====
        maven { 
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
        maven { 
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/")
            name = "Tencent Gradle Plugins"
        }
        
        // ===== 阿里云镜像（备用） =====
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
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
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // ===== 腾讯云镜像（优先） =====
        maven { 
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            name = "Tencent Maven"
        }
        
        // ===== 阿里云镜像（备用） =====
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        
        // ===== 华为云镜像（备用） =====
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