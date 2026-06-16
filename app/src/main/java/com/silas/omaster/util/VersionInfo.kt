package com.silas.omaster.util

import com.silas.omaster.BuildConfig

/**
 * 版本信息管理工具
 * 从 BuildConfig 自动读取版本号，避免多处修改
 */
object VersionInfo {

    /**
     * 对外显示版本号，例如 "1.1.0"
     * 对应 build.gradle.kts 中的 versionName
     */
    val VERSION_NAME: String = BuildConfig.VERSION_NAME

    /**
     * 内部版本号，用于更新检查
     * 从 versionName 计算：主版本*10000 + 次版本*100 + 修订版本
     * 例如：1.1.0 -> 10100, 1.0.3 -> 10003
     */
    val VERSION_CODE: Int = parseVersionCode(VERSION_NAME)

    /**
     * 计算版本号对应的数字值
     * 用于与 GitHub release 的版本比较
     * 
     * 修复 P2-10: 支持预发布版本号（如 "1.2.0-beta1"）
     * 预发布版本号会被解析为负数，确保正式版 > 预发布版
     */
    fun parseVersionCode(versionName: String): Int {
        // 移除前缀 'v' 如果有
        val cleanVersion = versionName.removePrefix("v")
        
        // 分离主版本和预发布标识
        val (mainVersion, prerelease) = when {
            cleanVersion.contains("-") -> {
                val parts = cleanVersion.split("-", limit = 2)
                parts[0] to parts.getOrNull(1)
            }
            cleanVersion.contains("+") -> {
                val parts = cleanVersion.split("+", limit = 2)
                parts[0] to parts.getOrNull(1)
            }
            else -> cleanVersion to null
        }
        
        val parts = mainVersion.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        
        // 修复：patch 部分可能包含预发布标识（如 "0-beta1"）
        val patchPart = parts.getOrNull(2) ?: "0"
        val patch = patchPart.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        
        // 基础版本号
        val baseCode = major * 10000 + minor * 100 + patch
        
        // 预发布版本处理：返回负数，确保正式版 > 预发布版
        return if (prerelease != null) {
            // 预发布版本解析为负数，按预发布类型排序
            // alpha < beta < rc < 其他
            val prereleaseCode = when {
                prerelease.startsWith("alpha", ignoreCase = true) -> -4
                prerelease.startsWith("beta", ignoreCase = true) -> -3
                prerelease.startsWith("rc", ignoreCase = true) -> -2
                else -> -1
            }
            // 提取预发布版本号（如 beta1 -> 1）
            val prereleaseNum = prerelease.filter { it.isDigit() }.toIntOrNull() ?: 0
            // 组合：基础版本号为负，加上预发布类型和版本号
            -(baseCode * 100 + prereleaseCode * 10 + prereleaseNum)
        } else {
            baseCode
        }
    }
}
