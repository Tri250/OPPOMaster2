// 仅开启 Gradle 离线模式，不修改仓库配置
// 配合 settings.gradle.kts 中已配置的 local-maven-repo 优先使用本地依赖
gradle.startParameter.isOffline = true
