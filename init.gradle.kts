// Gradle 初始化脚本 - 彻底解决镜像问题
// 通过 settingsEvaluated 钩子覆盖 settings.gradle.kts 的仓库配置
// 使用方法: gradle -I init.gradle.kts <task>

beforeSettings {
    pluginManagement {
        repositories {
            // 阿里云镜像（首选，响应快）
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }

            // 华为云镜像（备用）
            maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

            // 官方仓库（最后备用）
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }

    dependencyResolutionManagement {
        // 使用 PREFER_PROJECT 允许 init 脚本添加仓库
        repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
        repositories {
            // 阿里云镜像（首选，响应快）
            maven { url = uri("https://maven.aliyun.com/repository/public") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }

            // 华为云镜像（备用）
            maven { url = uri("https://mirrors.huaweicloud.com/repository/maven/") }

            // 官方仓库（最后备用）
            google()
            mavenCentral()

            // 友盟仓库
            maven {
                url = uri("https://repo.umeng.com/maven-releases/")
                isAllowInsecureProtocol = true
            }

            // JitPack
            maven { url = uri("https://jitpack.io") }
        }
    }
}

println("✅ Gradle 国内镜像配置已加载（beforeSettings 钩子）")
