package com.silas.omaster.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.security.MessageDigest

/**
 * 应用安全完整性检查器
 *
 * 负责：
 * - 检测 Root 环境（Magisk、SuperSU 等）
 * - 检测模拟器运行环境
 * - 检测 APK 签名完整性（防重打包）
 * - 检测调试器附加
 * - 检测 Xposed/Frida 等 Hook 框架
 *
 * 注意：这些检查不能完全阻止逆向，但可显著提高攻击门槛。
 * 生产环境建议结合后端风控和 Google Play Integrity API 使用。
 */
object SecurityIntegrityChecker {

    private const val TAG = "SecurityIntegrity"

    /** 已知的 Root 管理应用包名 */
    private val ROOT_APPS = arrayOf(
        "com.noshufou.android.su",
        "com.noshufou.android.su.elite",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.yellowes.su",
        "com.topjohnwu.magisk",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        "com.alephzain.framaroot"
    )

    /** 已知的 Root 二进制文件路径 */
    private val ROOT_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/system/xbin/daemonsu",
        "/system/etc/init.d/99SuperSUDaemon",
        "/system/bin/.ext/.su",
        "/vendor/bin/su"
    )

    /** 已知的模拟器特征 */
    private val EMULATOR_FINGERPRINTS = arrayOf(
        "generic",
        "unknown",
        "emulator",
        "vbox",
        "genymotion",
        "andy",
        "android_x86"
    )

    /** 已知的 Hook 框架特征 */
    private val HOOK_FRAMEWORK_FILES = arrayOf(
        "/system/framework/XposedBridge.jar",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        "/data/local/tmp/re.frida.server/frida-agent",
        "/data/local/tmp/frida-server",
        "/system/framework/lsposed",
        "/data/adb/lspd"
    )

    /**
     * 安全检查结果
     */
    data class IntegrityResult(
        val isSafe: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggerAttached: Boolean,
        val isHookDetected: Boolean,
        val issues: List<String>
    )

    /**
     * 执行完整安全检查
     */
    fun performCheck(context: Context): IntegrityResult {
        val issues = mutableListOf<String>()

        val isRooted = checkRoot()
        if (isRooted) issues.add("Root 环境检测到")

        val isEmulator = checkEmulator()
        if (isEmulator) issues.add("模拟器环境检测到")

        val isDebuggerAttached = checkDebugger()
        if (isDebuggerAttached) issues.add("调试器已附加")

        val isHookDetected = checkHookFramework()
        if (isHookDetected) issues.add("Hook 框架检测到")

        val isSafe = !isRooted && !isEmulator && !isDebuggerAttached && !isHookDetected

        val result = IntegrityResult(
            isSafe = isSafe,
            isRooted = isRooted,
            isEmulator = isEmulator,
            isDebuggerAttached = isDebuggerAttached,
            isHookDetected = isHookDetected,
            issues = issues
        )

        if (!isSafe) {
            Log.w(TAG, "安全检查未通过: ${issues.joinToString(", ")}")
        } else {
            Log.d(TAG, "安全检查通过")
        }

        return result
    }

    /**
     * 检测 Root 环境
     */
    private fun checkRoot(): Boolean {
        // 1. 检查 Root 管理应用
        for (app in ROOT_APPS) {
            try {
                val pm = Runtime.getRuntime().exec(arrayOf("pm", "list", "packages", app))
                val reader = BufferedReader(InputStreamReader(pm.inputStream))
                val line = reader.readLine()
                reader.close()
                pm.destroy()
                if (line != null && line.contains(app)) {
                    Log.w(TAG, "检测到 Root 应用: $app")
                    return true
                }
            } catch (_: Exception) {
                // pm 命令可能不可用
            }
        }

        // 2. 检查 Root 二进制文件
        for (path in ROOT_PATHS) {
            val file = File(path)
            if (file.exists()) {
                Log.w(TAG, "检测到 Root 文件: $path")
                return true
            }
        }

        // 3. 检查 su 命令是否可用
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            process.destroy()
            if (result != null && result.isNotEmpty()) {
                Log.w(TAG, "su 命令可用: $result")
                return true
            }
        } catch (_: Exception) {
            // which 命令可能不可用
        }

        return false
    }

    /**
     * 检测模拟器环境
     */
    private fun checkEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        for (pattern in EMULATOR_FINGERPRINTS) {
            if (fingerprint.contains(pattern) || model.contains(pattern) ||
                manufacturer.contains(pattern) || hardware.contains(pattern)
            ) {
                Log.w(TAG, "检测到模拟器特征: $pattern")
                return true
            }
        }

        return false
    }

    /**
     * 检测调试器是否附加
     */
    private fun checkDebugger(): Boolean {
        return (android.os.Debug.isDebuggerConnected() ||
                android.os.Debug.waitingForDebugger())
    }

    /**
     * 检测 Hook 框架（Xposed / Frida / LSPosed）
     */
    private fun checkHookFramework(): Boolean {
        for (path in HOOK_FRAMEWORK_FILES) {
            if (File(path).exists()) {
                Log.w(TAG, "检测到 Hook 框架文件: $path")
                return true
            }
        }
        return false
    }

    /**
     * 验证 APK 签名（防止重打包）
     *
     * @param context 应用上下文
     * @param expectedSignature 预期的签名 SHA-256 十六进制字符串
     * @return true 表示签名匹配
     */
    fun verifySignature(context: Context, expectedSignature: String): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            } ?: return false

            val md = MessageDigest.getInstance("SHA-256")
            for (signature in signatures) {
                val hash = md.digest(signature.toByteArray())
                val hashHex = hash.joinToString("") { "%02x".format(it) }
                if (hashHex == expectedSignature) {
                    return true
                }
            }
            Log.w(TAG, "签名验证失败：签名不匹配")
            false
        } catch (e: Exception) {
            Log.e(TAG, "签名验证异常", e)
            false
        }
    }
}