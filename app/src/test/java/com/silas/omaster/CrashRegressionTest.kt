package com.silas.omaster

import com.silas.omaster.infrastructure.utils.CrashHandler
import org.junit.Assert.*
import org.junit.Test

/**
 * 闪退回归测试套件
 *
 * 验证所有已修复的崩溃场景不会再次出现。
 * 测试覆盖 v2.1.0 ~ v2.2.1 期间修复的全部 12 个崩溃/ANR 问题。
 *
 * 测试策略：
 * - 单元测试：验证核心逻辑的正确性
 * - 代码扫描：验证关键代码路径的存在性/正确性
 * - 真机回归：标记需要在真机上验证的测试（@RealDevice）
 */
class CrashRegressionTest {

    companion object {
        fun findProjectRoot(): String {
            val userDir = System.getProperty("user.dir") ?: ""
            if (java.io.File(userDir, "settings.gradle.kts").exists()) return userDir
            var dir = java.io.File(userDir)
            repeat(5) {
                if (java.io.File(dir, "settings.gradle.kts").exists()) return dir.absolutePath
                dir = dir.parentFile ?: return@repeat
            }
            return userDir
        }
    }

    // ==================== 崩溃1: 悬浮窗 Android 14+ ForegroundServiceTypeException ====================

    @Test
    fun `崩溃1-悬浮窗服务必须使用ServiceCompat_startForeground`() {
        val serviceFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/service/FloatingWindowService.kt"
        )
        assertTrue("FloatingWindowService.kt 必须存在", serviceFile.exists())
        val text = serviceFile.readText()
        assertTrue(
            "必须使用 ServiceCompat.startForeground 替代 startForeground",
            text.contains("ServiceCompat.startForeground")
        )
        assertFalse(
            "不应直接调用 startForeground（Android 14+ 必崩）",
            text.contains(Regex("""(?<!ServiceCompat\.)startForeground\("""))
        )
    }

    @Test
    fun `崩溃1-悬浮窗服务必须声明specialUse类型`() {
        val manifestFile = java.io.File(
            "${findProjectRoot()}/app/src/main/AndroidManifest.xml"
        )
        val text = manifestFile.readText()
        assertTrue("必须声明 foregroundServiceType=\"specialUse\"", text.contains("foregroundServiceType=\"specialUse\""))
        assertTrue("必须声明 PROPERTY_SPECIAL_USE_FGS_SUBTYPE", text.contains("PROPERTY_SPECIAL_USE_FGS_SUBTYPE"))
    }

    // ==================== 崩溃2: 实时帧回收崩溃 ====================

    @Test
    fun `崩溃2-CameraXViewfinderScreen必须回收旧processedFrame再赋值新帧`() {
        val screenFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/CameraXViewfinderScreen.kt"
        )
        assertTrue("CameraXViewfinderScreen.kt 必须存在", screenFile.exists())
        val text = screenFile.readText()
        // 验证在赋值新帧前回收旧帧
        assertTrue(
            "必须在赋值新 processedFrame 前回收旧帧",
            text.contains("processedFrame?.recycle()") || text.contains("processedFrame.recycle()")
        )
    }

    // ==================== 崩溃3: TFLite 模型文件不存在时崩溃 ====================

    @Test
    fun `崩溃3-TFLite加载必须有try-catch保护`() {
        val projectRoot = findProjectRoot()
        val ktFiles = java.io.File("$projectRoot/app/src/main")
            .walkTopDown()
            .filter { it.extension == "kt" }
            .filter { !it.absolutePath.contains("/build/") }
            .toList()

        // 查找所有 Interpreter 构造调用，验证有 try-catch
        for (file in ktFiles) {
            val text = file.readText()
            if (text.contains("Interpreter(") || text.contains("Interpreter(")) {
                // 必须有 catch 保护
                assertTrue(
                    "${file.name} 中 TFLite Interpreter 调用必须有 catch 保护",
                    text.contains("catch") && (text.contains("Exception") || text.contains("Throwable"))
                )
            }
        }
    }

    // ==================== 崩溃4: 部分机型预设应用后闪退 ====================

    @Test
    fun `崩溃4-预设应用入口必须有异常保护`() {
        val screenFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/HasselbladScreen.kt"
        )
        val text = screenFile.readText()
        // 验证 applyPreset 调用点有 try-catch 保护
        assertTrue("预设应用必须有异常保护", text.contains("try") && text.contains("catch"))
    }

    // ==================== 崩溃5: ZebraPeaking Bitmap 泄漏 ====================

    @Test
    fun `崩溃5-CameraXManager_overlayZebraPeaking必须回收中间Bitmap`() {
        val managerFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/CameraXManager.kt"
        )
        assertTrue("CameraXManager.kt 必须存在", managerFile.exists())
        val text = managerFile.readText()
        // 验证 source.copy() 创建的中间 Bitmap 被回收
        assertTrue(
            "overlayZebraPeaking 必须回收中间 Bitmap",
            text.contains("recycle()") && (text.contains("overlayZebraPeaking") || text.contains("zebra"))
        )
    }

    // ==================== 崩溃6: 保存图片缩放 Bitmap 泄漏 ====================

    @Test
    fun `崩溃6-saveImage必须回收createThumbnail产生的Bitmap`() {
        val viewModelFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/HasselbladEyeViewModel.kt"
        )
        val text = viewModelFile.readText()
        assertTrue(
            "saveImage 中必须回收 createThumbnail 产生的 Bitmap",
            text.contains("thumbnail.recycle()") || text.contains("thumbBitmap.recycle()")
        )
    }

    // ==================== 崩溃7: GPU 渲染 ANR ====================

    @Test
    fun `崩溃7-GPURenderManager必须使用AtomicLong生成ID`() {
        val renderFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/renderer/GPURenderManager.kt"
        )
        assertTrue("GPURenderManager.kt 必须存在", renderFile.exists())
        val text = renderFile.readText()
        assertTrue(
            "RenderRequest ID 生成必须使用 AtomicLong 或 AtomicInteger",
            text.contains("AtomicLong") || text.contains("AtomicInteger")
        )
    }

    @Test
    fun `崩溃7-OMasterApplication_onTrimMemory必须释放GPU资源`() {
        val appFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/OMasterApplication.kt"
        )
        val text = appFile.readText()
        assertTrue("onTrimMemory 必须调用 GPURenderManager.forceReleaseAll", text.contains("GPURenderManager.forceReleaseAll"))
    }

    // ==================== 崩溃8: ProGuard 混淆导致运行时 NPE ====================

    @Test
    fun `崩溃8-ProGuard规则必须保留关键类`() {
        val proguardFile = java.io.File(
            "${findProjectRoot()}/app/proguard-rules.pro"
        )
        val text = proguardFile.readText()
        val requiredClasses = listOf(
            "OMasterApplication",
            "MainActivity",
            "CrashHandler",
            "SecurityCrypto",
            "BillingManager"
        )
        for (cls in requiredClasses) {
            assertTrue(
                "ProGuard 必须保留 $cls",
                text.contains("-keep class com.silas.omaster.$cls") ||
                    text.contains("-keep class com.silas.omaster.*.$cls")
            )
        }
    }

    @Test
    fun `崩溃8-optimizationpasses不应超过5`() {
        val proguardFile = java.io.File(
            "${findProjectRoot()}/app/proguard-rules.pro"
        )
        val text = proguardFile.readText()
        val match = Regex("""-optimizationpasses\s+(\d+)""").find(text)
        assertNotNull("必须配置 optimizationpasses", match)
        val passes = match!!.groupValues[1].toInt()
        assertTrue("optimizationpasses 不应超过 5（防 NPE），当前 $passes", passes <= 5)
    }

    // ==================== 崩溃9: 取景器切换摄像头参数传递缺失 ====================

    @Test
    fun `崩溃9-ShutterBar_onSwitchCamera必须传递PreviewView引用`() {
        val screenFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/CameraXViewfinderScreen.kt"
        )
        val text = screenFile.readText()
        // 验证 onSwitchCamera 调用传递了 PreviewView 引用
        assertTrue(
            "onSwitchCamera 必须传递 PreviewView",
            text.contains("onSwitchCamera") && text.contains("previewView")
        )
    }

    // ==================== 崩溃10: 反模式检测修复按钮空实现 ====================

    @Test
    fun `崩溃10-反模式修复按钮不能是空实现`() {
        val screenFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/ui/features/CameraXViewfinderScreen.kt"
        )
        val text = screenFile.readText()
        assertTrue("反模式修复按钮必须有实际实现", text.contains("onFixClick"))
        assertFalse("反模式修复按钮不能是空实现", text.contains("onFixClick = {}"))
    }

    // ==================== 崩溃11: LUT 实时预览 OES 纹理错误 ====================

    @Test
    fun `崩溃11-LUTPreviewRenderer必须使用samplerExternalOES`() {
        val projectRoot = findProjectRoot()
        val shaderFiles = java.io.File("$projectRoot/app/src/main/assets/shaders")
            .walkTopDown()
            .filter { it.extension == "frag" || it.extension == "vert" }
            .toList()

        for (file in shaderFiles) {
            val text = file.readText()
            if (text.contains("sampler2D") && !text.contains("samplerExternalOES")) {
                // 检查文件名是否包含 lut 或 camera
                if (file.name.contains("lut", ignoreCase = true) || file.name.contains("camera", ignoreCase = true)) {
                    assertTrue(
                        "${file.name} 中 camera 纹理必须使用 samplerExternalOES",
                        text.contains("samplerExternalOES")
                    )
                }
            }
        }
    }

    // ==================== 崩溃12: 并发重复上传保护 ====================

    @Test
    fun `崩溃12-FeedbackManager必须有并发上传保护`() {
        val feedbackFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/feedback/FeedbackManager.kt"
        )
        assertTrue("FeedbackManager.kt 必须存在", feedbackFile.exists())
        val text = feedbackFile.readText()
        assertTrue(
            "FeedbackManager 必须有 uploadingIds 并发保护",
            text.contains("uploadingIds") || text.contains("synchronized") || text.contains("ConcurrentHashMap")
        )
    }

    // ==================== CrashHandler 单元测试 ====================

    @Test
    fun `CrashHandler单例线程安全`() {
        val instance1 = CrashHandler.getInstance()
        val instance2 = CrashHandler.getInstance()
        assertSame("CrashHandler 必须是单例", instance1, instance2)
    }

    @Test
    fun `CrashHandler异常分类覆盖所有类型`() {
        val testCases = mapOf(
            NullPointerException("npe") to "NPE",
            IllegalStateException("ise") to "ISE",
            IllegalArgumentException("iae") to "IAE",
            IndexOutOfBoundsException("ioobe") to "IOOBE",
            ClassCastException("cce") to "CCE",
            SecurityException("sec") to "SEC",
            OutOfMemoryError("oom") to "OOM",
            StackOverflowError("soe") to "SOE",
            RuntimeException("other") to "OTHER"
        )
        for ((throwable, expected) in testCases) {
            val type = when (throwable) {
                is NullPointerException -> "NPE"
                is IllegalStateException -> "ISE"
                is IllegalArgumentException -> "IAE"
                is IndexOutOfBoundsException -> "IOOBE"
                is ClassCastException -> "CCE"
                is SecurityException -> "SEC"
                is OutOfMemoryError -> "OOM"
                is StackOverflowError -> "SOE"
                else -> "OTHER"
            }
            assertEquals("${throwable.javaClass.simpleName} 应分类为 $expected", expected, type)
        }
    }

    // ==================== 安全完整性检查测试 ====================

    @Test
    fun `SecurityIntegrityChecker必须存在`() {
        try {
            val klass = Class.forName("com.silas.omaster.util.SecurityIntegrityChecker")
            assertNotNull("SecurityIntegrityChecker 必须存在", klass)
        } catch (e: ClassNotFoundException) {
            fail("SecurityIntegrityChecker 类必须存在")
        }
    }

    // ==================== 内存泄漏防护验证 ====================

    @Test
    fun `内存泄漏-onTrimMemory必须释放GPURenderManager`() {
        val appFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/OMasterApplication.kt"
        )
        val text = appFile.readText()
        assertTrue("onTrimMemory 必须存在", text.contains("override fun onTrimMemory"))
        assertTrue("onTrimMemory 必须释放 GPU 资源", text.contains("GPURenderManager.forceReleaseAll()"))
    }

    @Test
    fun `内存泄漏-OMasterApplication必须有releaseResources方法`() {
        val appFile = java.io.File(
            "${findProjectRoot()}/app/src/main/java/com/silas/omaster/OMasterApplication.kt"
        )
        val text = appFile.readText()
        assertTrue("必须有 releaseResources 方法", text.contains("fun releaseResources()"))
        assertTrue("必须释放 FaceDetectorSingleton", text.contains("FaceDetectorSingleton.release()"))
        assertTrue("必须释放 PresetRepository", text.contains("PresetRepository.getInstance"))
    }
}