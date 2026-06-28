// 沙箱离线构建完整配置
// 使用方法: gradle -I init-offline-full.gradle.kts assembleRelease
// 覆盖 pluginManagement + dependencyResolutionManagement + 离线模式

// 必须最先执行，在 settings.gradle.kts 解析之前
settingsEvaluated {
    // 覆盖 pluginManagement repositories
    pluginManagement {
        repositories {
            mavenLocal()
            maven { url = uri("${rootProject.projectDir}/local-maven-repo") }
        }
    }
}

// 所有项目添加本地仓库
allprojects {
    repositories {
        mavenLocal()
        maven { url = uri("/workspace/local-maven-repo") }
    }
}

// 设置全局离线模式
gradle.startParameter.isOffline = true