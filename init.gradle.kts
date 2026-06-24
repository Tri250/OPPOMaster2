// Gradle 初始化脚本 - 国内镜像全局配置
// 使用方法: ./gradlew -I init.gradle.kts <task>
//
// 网络环境要点：
//   1. 默认配置走国内镜像（阿里云 / 腾讯云 / 华为），不依赖外网代理
//   2. 如需代理，请在用户级 ~/.gradle/gradle.properties 中配置 systemProp.http.proxyHost
//   3. 主仓库 gradle.properties 不应包含代理配置（避免影响其他开发者/CI）
// 更新时间: 2026-06-24

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

            // 华为云镜像（备选）
            maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }

            // 官方仓库（兜底）
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

        // 华为云镜像（备选）
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }

        // 官方仓库（兜底）
        google()
        mavenCentral()

        // JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

// 打印配置信息
println("✅ Gradle 国内镜像配置已加载（阿里云 + 腾讯云 + 华为云 + Google + Central）")
println("   - 如需代理，请在用户级 ~/.gradle/gradle.properties 配置 systemProp.http.proxyHost")
println("   - 主仓库 gradle.properties 已零代理硬编码（不影响其他开发者/CI）")
