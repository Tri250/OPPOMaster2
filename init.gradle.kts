// Gradle 初始化脚本 - 沙箱环境代理 + 国内镜像全局配置
// 使用方法: ./gradlew -I init.gradle.kts <task>
//
// 沙箱网络环境要点：
//   1. 直连外网全部超时（被沙箱网络策略阻断）
//   2. 必须通过 127.0.0.1:18080 HTTP 代理访问外网
//   3. 代理可达: mirrors.cloud.tencent.com / repo1.maven.org / plugins.gradle.org / dl.google.com / maven.aliyun.com

allprojects {
    buildscript {
        repositories {
            // 阿里云镜像（走代理后实测 10-20ms 极速响应）
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/central") }

            // 腾讯云镜像（聚合镜像，全仓库一体化）
            maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

            // 官方仓库
            gradlePluginPortal()
            google()
            mavenCentral()
        }
    }

    repositories {
        // 阿里云镜像（沙箱走代理后最快）
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("com\\.android\\..*")
                includeGroupByRegex("androidx\\..*")
                includeGroupByRegex("com\\.google\\..*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            content {
                includeGroupByRegex(".*")
            }
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }

        // 腾讯云镜像（聚合）
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }

        // 官方仓库
        google()
        mavenCentral()

        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

// 打印配置信息
println("✅ Gradle 沙箱环境代理镜像配置已加载")
println("   - HTTP 代理: 127.0.0.1:18080 (通过 gradle.properties 配置)")
println("   - 阿里云镜像: 已启用 (走代理实测 10-20ms)")
println("   - 腾讯云镜像: 已启用 (走代理实测 200-400ms)")
println("   - 官方仓库 (google/mavenCentral/gradlePluginPortal): 已启用")
