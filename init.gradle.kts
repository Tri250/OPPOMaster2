// Gradle 初始化脚本 - 国内镜像全局配置
// 使用方法: ./gradlew -I init.gradle.kts <task>

allprojects {
    buildscript {
        repositories {
            // 阿里云镜像
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            
            // 腾讯云镜像
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
            
            // 华为云镜像
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
            
            // 官方仓库
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
    
    repositories {
        // 阿里云镜像
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("org\\.jetbrains\\..*")
            }
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\..*")
            }
        }
        
        // 腾讯云镜像
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        
        // 华为云镜像
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        
        // 清华大学镜像
        maven { url = uri("https://mirrors.tuna.tsinghua.edu.cn/maven/") }
        
        // 官方仓库
        google()
        mavenCentral()
        
        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

// 打印配置信息
println("✅ Gradle 国内镜像配置已加载")
println("   - 阿里云镜像: 已启用")
println("   - 腾讯云镜像: 已启用")
println("   - 华为云镜像: 已启用")
println("   - 清华大学镜像: 已启用")
