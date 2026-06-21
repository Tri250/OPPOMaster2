// 沙箱离线构建优化配置
// 使用方法: gradle -I init-offline.gradle.kts <task>

allprojects {
    repositories {
        // 强制使用本地仓库（最高优先级）
        maven { url = uri("/workspace/local-maven-repo") }
    }
}

// 设置全局离线模式
gradle.startParameter.isOffline = true
