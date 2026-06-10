// =============================================================================
// Gradle 全局镜像初始化脚本
// =============================================================================
// 用途：通过 -I 参数或 init.d 目录加载，自动将所有仓库请求重定向到国内镜像
// 使用方法：
//   1. 复制到 ~/.gradle/init.d/mirror.init.gradle.kts
//   2. 或使用命令行：./gradlew build -I gradle/init.d/mirror.init.gradle.kts
// 优先级：此文件优先级最高，会覆盖 settings.gradle.kts 中的仓库配置
// =============================================================================

import org.gradle.api.initialization.resolve.RepositoriesMode

val ALIYUN_GOOGLE = "https://maven.aliyun.com/repository/google"
val ALIYUN_CENTRAL = "https://maven.aliyun.com/repository/central"
val ALIYUN_PUBLIC = "https://maven.aliyun.com/repository/public"
val ALIYUN_GRADLE_PLUGIN = "https://maven.aliyun.com/repository/gradle-plugin"
val ALIYUN_JCENTER = "https://maven.aliyun.com/repository/jcenter"
val TENCENT_MAVEN = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/"
val HUAWEI_MAVEN = "https://repo.huaweicloud.com/repository/maven/"

val MIRROR_HOSTS = listOf(
    "maven.aliyun.com",
    "mirrors.cloud.tencent.com",
    "mirrors.tencent.com",
    "repo.huaweicloud.com"
)

fun isMirrorUrl(url: String): Boolean {
    return MIRROR_HOSTS.any { url.contains(it) }
}

fun isOfficialRepoUrl(url: String): Boolean {
    return url.contains("repo.maven.apache.org") ||
        url.contains("repo1.maven.org") ||
        url.contains("jcenter.bintray.com") ||
        url.contains("dl.google.com") ||
        url.contains("maven.google.com") ||
        url.contains("plugins.gradle.org")
}

allprojects {
    buildscript {
        repositories {
            // 清空原有仓库，重新添加镜像仓库
            clear()
            maven { setUrl(ALIYUN_GRADLE_PLUGIN) }
            maven { setUrl(ALIYUN_GOOGLE) }
            maven { setUrl(ALIYUN_CENTRAL) }
            maven { setUrl(ALIYUN_PUBLIC) }
            maven { setUrl(TENCENT_MAVEN) }
            maven { setUrl(HUAWEI_MAVEN) }
        }
    }

    repositories {
        // 清空原有仓库
        clear()
        // 添加国内镜像（按速度优先级排序）
        maven { setUrl(ALIYUN_GOOGLE) }
        maven { setUrl(ALIYUN_CENTRAL) }
        maven { setUrl(ALIYUN_PUBLIC) }
        maven { setUrl(ALIYUN_GRADLE_PLUGIN) }
        maven { setUrl(ALIYUN_JCENTER) }
        maven { setUrl(TENCENT_MAVEN) }
        maven { setUrl(HUAWEI_MAVEN) }
    }
}

settingsEvaluated {
    pluginManagement {
        repositories {
            clear()
            maven { setUrl(ALIYUN_GRADLE_PLUGIN) }
            maven { setUrl(ALIYUN_GOOGLE) }
            maven { setUrl(ALIYUN_CENTRAL) }
            maven { setUrl(ALIYUN_PUBLIC) }
            maven { setUrl(TENCENT_MAVEN) }
            maven { setUrl(HUAWEI_MAVEN) }
            // 保留官方仓库作为兜底
            gradlePluginPortal()
        }
    }

    @Suppress("UnstableApiUsage")
    dependencyResolutionManagement {
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
        repositories {
            clear()
            maven { setUrl(ALIYUN_GOOGLE) }
            maven { setUrl(ALIYUN_CENTRAL) }
            maven { setUrl(ALIYUN_PUBLIC) }
            maven { setUrl(ALIYUN_GRADLE_PLUGIN) }
            maven { setUrl(ALIYUN_JCENTER) }
            maven { setUrl(TENCENT_MAVEN) }
            maven { setUrl(HUAWEI_MAVEN) }
        }
    }
}
