// 沙箱离线构建优化配置
// 使用方法: gradle -I init-offline.gradle.kts <task>

allprojects {
    buildscript {
        repositories {
            // 强制只使用本地仓库
            maven { url = uri("/workspace/local-maven-repo") }
        }
        // 禁用所有外部依赖解析
        configurations.all {
            resolutionStrategy {
                failOnVersionConflict()
                cacheChangingModulesFor(0, "seconds")
                cacheDynamicVersionsFor(0, "seconds")
            }
        }
    }
    
    repositories {
        // 强制只使用本地仓库
        maven { url = uri("/workspace/local-maven-repo") }
    }
    
    // 配置所有项目使用离线模式
    configurations.all {
        resolutionStrategy {
            // 禁用网络依赖
            cacheChangingModulesFor(0, "seconds")
            cacheDynamicVersionsFor(0, "seconds")
            
            // 强制使用本地版本
            force(
                "com.android.tools.build:gradle:8.7.3",
                "org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20"
            )
        }
    }
}

// 设置全局离线模式
gradle.startParameter.isOffline = true

// 禁用所有外部仓库
gradle.settingsEvaluated {
    pluginManagement {
        repositories.clear()
        repositories {
            maven { url = uri("/workspace/local-maven-repo") }
        }
    }
}
