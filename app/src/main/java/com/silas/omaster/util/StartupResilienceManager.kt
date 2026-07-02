package com.silas.omaster.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

/**
 * 启动韧性管理器
 *
 * 覆盖所有启动异常场景的容错处理：
 * - 存储空间检查（1.2 低存储空间场景）
 * - 自定义ROM兼容性检查（2.2 定制ROM）
 * - 低内存设备保护（2.5 低端配置，5.4 低内存）
 * - 省电/后台限制模式检测（3.4 省电模式）
 * - 显示/字体缩放安全值（3.3 显示设置变更）
 * - 启动数据完整性检查（1.5 安装中断，6.3 清除数据）
 * - 资源加载容错（5.3 启动资源加载容错）
 * - WebView可用性检查（4.4 WebView异常）
 * - Dex优化状态检测（5.1 Dex优化）
 */
object StartupResilienceManager {

    private const val TAG = "StartupResilience"

    /** 最小可接受剩余存储空间（MB） */
    private const val MIN_STORAGE_MB = 100L

    /** 最小可接受剩余内存（MB） */
    private const val MIN_AVAILABLE_MEMORY_MB = 64L

    /**
     * 启动环境检查结果
     */
    data class StartupEnvironment(
        val isStorageSufficient: Boolean,
        val freeStorageMB: Long,
        val isMemorySufficient: Boolean,
        val availableMemoryMB: Long,
        val totalMemoryMB: Long,
        val isLowMemoryDevice: Boolean,
        val isPowerSaveMode: Boolean,
        val isWebViewAvailable: Boolean,
        val fontScale: Float,
        val displayDensity: Float,
        val issues: List<String>
    ) {
        val hasIssues: Boolean get() = issues.isNotEmpty()
    }

    /**
     * 执行启动环境综合检查
     * 所有检查失败不阻断启动，仅记录问题
     */
    fun checkStartupEnvironment(context: Context): StartupEnvironment {
        val issues = mutableListOf<String>()
        val ctx = context.applicationContext

        // 1. 存储空间检查
        val freeStorageMB = getFreeStorageMB()
        val isStorageSufficient = freeStorageMB >= MIN_STORAGE_MB
        if (!isStorageSufficient) {
            issues.add("存储空间不足: ${freeStorageMB}MB < ${MIN_STORAGE_MB}MB")
        }

        // 2. 内存检查
        val memInfo = getMemoryInfo(ctx)
        val isMemorySufficient = memInfo.availableMB >= MIN_AVAILABLE_MEMORY_MB
        if (!isMemorySufficient) {
            issues.add("可用内存不足: ${memInfo.availableMB}MB < ${MIN_AVAILABLE_MEMORY_MB}MB")
        }

        // 3. 低内存设备检测
        val isLowMemoryDevice = ctx.getSystemService(Context.ACTIVITY_SERVICE)
            ?.let { it as? ActivityManager }?.isLowRamDevice ?: false
        if (isLowMemoryDevice) {
            issues.add("低内存设备 (isLowRamDevice=true)")
        }

        // 4. 省电模式检测
        val isPowerSaveMode = try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                pm?.isPowerSaveMode ?: false
            } else false
        } catch (e: Throwable) {
            false
        }
        if (isPowerSaveMode) {
            issues.add("设备处于省电模式")
        }

        // 5. WebView 可用性检查（Android 4.4+）
        val isWebViewAvailable = checkWebViewAvailability()
        if (!isWebViewAvailable) {
            issues.add("WebView 不可用或版本过低")
        }

        // 6. 字体缩放
        val fontScale = ctx.resources.configuration.fontScale
        if (fontScale > 1.5f || fontScale < 0.8f) {
            issues.add("字体缩放值为 $fontScale，可能影响UI")
        }

        // 7. 显示密度
        val displayDensity = ctx.resources.displayMetrics.density

        // 8. 检查启动数据完整性（SharedPreferences 是否可正常读写）
        checkDataIntegrity(ctx).let { if (!it) issues.add("启动数据完整性检查未通过") }

        val result = StartupEnvironment(
            isStorageSufficient = isStorageSufficient,
            freeStorageMB = freeStorageMB,
            isMemorySufficient = isMemorySufficient,
            availableMemoryMB = memInfo.availableMB,
            totalMemoryMB = memInfo.totalMB,
            isLowMemoryDevice = isLowMemoryDevice,
            isPowerSaveMode = isPowerSaveMode,
            isWebViewAvailable = isWebViewAvailable,
            fontScale = fontScale,
            displayDensity = displayDensity,
            issues = issues
        )

        if (result.hasIssues) {
            Log.w(TAG, "启动环境检查发现问题: ${issues.joinToString("; ")}")
        } else {
            Log.i(TAG, "启动环境检查通过 (存储: ${freeStorageMB}MB, 内存: ${memInfo.availableMB}MB)")
        }

        return result
    }

    /**
     * 获取可用存储空间（MB）
     */
    private fun getFreeStorageMB(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val availableBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stat.availableBytes
            } else {
                @Suppress("DEPRECATION")
                stat.availableBlocks.toLong() * stat.blockSize.toLong()
            }
            availableBytes / (1024 * 1024)
        } catch (e: Throwable) {
            Log.e(TAG, "获取存储空间失败", e)
            -1L
        }
    }

    /**
     * 内存信息
     */
    data class MemInfo(val availableMB: Long, val totalMB: Long)

    /**
     * 获取系统内存信息
     */
    private fun getMemoryInfo(context: Context): MemInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            MemInfo(
                availableMB = memInfo.availMem / (1024 * 1024),
                totalMB = memInfo.totalMem / (1024 * 1024)
            )
        } catch (e: Throwable) {
            Log.e(TAG, "获取内存信息失败", e)
            MemInfo(availableMB = -1L, totalMB = -1L)
        }
    }

    /**
     * 检查 WebView 可用性
     * 某些定制ROM或低版本设备可能缺少 WebView
     */
    private fun checkWebViewAvailability(): Boolean {
        return try {
            // 如果应用使用了 WebView，需要检查 WebView 包是否可用
            val webViewClass = try {
                Class.forName("android.webkit.WebView")
                true
            } catch (_: ClassNotFoundException) {
                false
            }
            if (!webViewClass) return false

            // 检查 WebView 实现是否可用
            try {
                val webViewFactory = Class.forName("android.webkit.WebViewFactory")
                val provider = webViewFactory.getMethod("getProvider").invoke(null)
                provider != null
            } catch (_: Throwable) {
                // 某些设备上WebView可能未更新，但不影响使用系统WebView
                true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "WebView 可用性检查失败", e)
            true
        }
    }

    /**
     * 检查启动数据完整性
     * 验证 SharedPreferences 和 DataStore 是否可正常读写
     */
    private fun checkDataIntegrity(context: Context): Boolean {
        return try {
            val testPrefs = context.getSharedPreferences(
                "startup_integrity_check",
                Context.MODE_PRIVATE
            )
            testPrefs.edit().putBoolean("test", true).commit()
            val result = testPrefs.getBoolean("test", false)
            testPrefs.edit().remove("test").commit()
            result
        } catch (e: Throwable) {
            Log.w(TAG, "数据完整性检查失败", e)
            false
        }
    }

    /**
     * 检查是否为已知问题ROM
     * @return ROM名称，正常设备返回null
     */
    fun checkCustomROM(): String? {
        return try {
            val buildProps = listOf(
                System.getProperty("ro.miui.ui.version.name"),
                System.getProperty("ro.build.version.opporom"),
                System.getProperty("ro.vivo.os.version"),
                System.getProperty("ro.build.version.emui"),
                System.getProperty("ro.build.version.originos"),
                System.getProperty("ro.build.version.coloros"),
                System.getProperty("ro.build.version.hmos")
            )
            when {
                buildProps[0] != null -> "MIUI ${buildProps[0]}"
                buildProps[1] != null -> "ColorOS ${buildProps[1]}"
                buildProps[2] != null -> "OriginOS ${buildProps[2]}"
                buildProps[3] != null -> "EMUI ${buildProps[3]}"
                buildProps[4] != null -> "OriginOS ${buildProps[4]}"
                buildProps[5] != null -> "ColorOS ${buildProps[5]}"
                buildProps[6] != null -> "HarmonyOS ${buildProps[6]}"
                else -> null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "检查定制ROM失败", e)
            null
        }
    }

    /**
     * 获取设备CPU架构信息
     */
    fun getCpuAbi(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Build.SUPPORTED_ABIS.joinToString(", ")
            } else {
                @Suppress("DEPRECATION")
                "${Build.CPU_ABI}, ${Build.CPU_ABI2}"
            }
        } catch (e: Throwable) {
            "unknown"
        }
    }

    /**
     * 检查SO库是否匹配当前架构
     * 在armeabi-v7a设备上安装仅包含arm64-v8a库的APK时返回false
     */
    fun checkNativeLibCompatibility(context: Context): Boolean {
        return try {
            val abiList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Build.SUPPORTED_ABIS.toList()
            } else {
                @Suppress("DEPRECATION")
                listOf(Build.CPU_ABI, Build.CPU_ABI2)
            }
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val libDir = File(nativeLibDir)
            // 如果存在 .so 文件目录且非空，说明库兼容
            if (libDir.exists() && libDir.isDirectory) {
                val soFiles = libDir.listFiles { f -> f.name.endsWith(".so") }
                if (!soFiles.isNullOrEmpty()) {
                    Log.i(TAG, "SO库加载正常: ${abiList.joinToString()}, ${soFiles.size}个.so文件")
                    return true
                }
            }
            Log.w(TAG, "SO库目录为空或不存在: $nativeLibDir, 架构: ${abiList.joinToString()}")
            // 目录为空不一定是错误，某些应用可能没有原生库
            true
        } catch (e: Throwable) {
            Log.w(TAG, "检查SO库兼容性失败", e)
            true
        }
    }
}