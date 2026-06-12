pluginManagement {
    repositories {
        // 官方仓库优先（更可靠）
        google()
        mavenCentral()
        gradlePluginPortal()
        
        // 国内镜像备用
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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 官方仓库优先
        google()
        mavenCentral()
        
        // 国内镜像备用
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        
        // 友盟仓库
        maven {
            url = uri("https://repo.umeng.com/maven-releases/")
            isAllowInsecureProtocol = true
            name = "Umeng"
        }
        
        // JitPack
        maven { 
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
    }
}

rootProject.name = "OMaster"
include(":app")