// Top-level build file where you can add configuration options common to all sub-projects/modules.

/**
 * 版本约束声明
 * 所有版本号统一在 gradle/libs.versions.toml 中管理
 *
 * 关键版本约束：
 * - AGP (Android Gradle Plugin): 8.9.1
 * - Kotlin: 2.2.0
 * - Compose BOM: 2025.01.01
 * - Java: 17
 *
 * 版本兼容性：
 * - Kotlin 2.2.0 与 Gradle 8.14.4 / AGP 8.9.1 完全兼容
 * - Compose Compiler 由 Kotlin 插件自动管理（kotlin-compose plugin）
 * - 最低 SDK: 24, 目标 SDK: 36
 */
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Kotlin Compose Compiler 插件 - 自动处理 Compose 与 Kotlin 版本兼容性
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}
