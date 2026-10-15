package com.silas.omaster.data.local

/**
 * 配置相关枚举集中定义。
 *
 * 历史说明：这些枚举原先寄居在已废弃的 SettingsManager 中，
 * 由 ConfigCenter 反向依赖旧类。现独立成文件，使旧类可安全移除。
 * 包名保持不变，避免调用方导入路径变更。
 */

/**
 * 更新渠道枚举
 */
enum class UpdateChannel {
    GITEE,    // 默认，国内访问快
    GITHUB    // GitHub，国际访问
}

/**
 * 悬浮窗模式枚举（MU-27 扩展）
 */
enum class FloatingWindowMode {
    STANDARD,   // 标准模式（卡片式，MU-11 重构前原版）
    COMPACT,    // 紧凑参数条（屏幕底部，宽度 = 屏宽 - 32dp）
    FRAMING     // ⭐ v1.6 新增：取景框跟随模式 — 自动避位，不遮挡相机取景区域
}

/**
 * 深色模式枚举
 */
enum class DarkMode {
    SYSTEM,     // 跟随系统
    LIGHT,      // 浅色模式
    DARK        // 深色模式
}

/**
 * 应用语言枚举
 */
enum class AppLanguage {
    SYSTEM,     // 跟随系统
    CHINESE,    // 简体中文
    ENGLISH,    // English
    JAPANESE    // 日本語
}
