// Top-level build file where you can add configuration options common to all sub-projects/modules.
// 镜像仓库已在 settings.gradle.kts 中配置，这里只需声明插件即可。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

// =============================================================================
// 子项目通用配置
// =============================================================================
allprojects {
    // 配置子项目的仓库（镜像源已在 settings.gradle.kts 中设置）
    // 如需额外配置可在此处添加

    tasks.withType<Test> {
        useJUnit()
    }
}
