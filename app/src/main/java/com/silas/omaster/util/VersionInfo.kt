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
     */
    fun parseVersionCode(versionName: String): Int {
        val parts = versionName.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    private fun parseSegments(versionName: String): List<Int> =
        versionName.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    /**
     * 逐段比较版本号：candidate 是否比 current 更新。
     * 修订位 ≥100 时（如 1.7.100 vs 1.8.0）加权和会进位碰撞，
     * 逐段比较是唯一可靠口径。
     */
    fun isNewerVersion(current: String, candidate: String): Boolean {
        val a = parseSegments(current)
        val b = parseSegments(candidate)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return y > x
        }
        return false
    }
}
