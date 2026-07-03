import java.security.SecureRandom
import java.util.Base64
import java.util.Properties

// ===== AppKey 运行时混淆密钥 =====
// 构建时使用此密钥对 AppKey 做 XOR+Base64 混淆，运行时在 OMasterApplication 中解混淆
// 防止 APK 反编译后直接提取明文 AppKey
// 
// 安全说明：
// - 密钥从环境变量或 local.properties 读取，不在代码中硬编码
// - 如未配置，使用动态生成的随机密钥（每次构建不同）
// - 生产环境建议将 AppKey 迁移至后端代理或 NDK 层
//
// 配置方式（local.properties）：
// OBFUSCATION_KEY=YourRandomKeyHere16!

/**
 * 获取混淆密钥
 * 优先级：1. 环境变量 2. local.properties 3. 动态生成（随机）
 *
 * v2.3.6 安全加固：对来自环境变量/文件的密钥做白名单过滤，
 * 防止不可见字符或 Java 字符串元字符（", \, 换行）破坏生成的 BuildConfig 代码
 */
fun getObfuscationKey(localProps: Properties): String {
    val safePattern = Regex("^[A-Za-z0-9+/=]{16,}$")

    // 1. 从环境变量读取（CI/CD 环境）
    System.getenv("OBFUSCATION_KEY")?.let {
        if (safePattern.matches(it)) return it
        println("⚠️ OBFUSCATION_KEY 环境变量包含非法字符，已忽略，使用随机生成的密钥")
    }

    // 2. 从 local.properties 读取
    localProps.getProperty("OBFUSCATION_KEY")?.let {
        if (safePattern.matches(it)) return it
        println("⚠️ OBFUSCATION_KEY local.properties 包含非法字符，已忽略，使用随机生成的密钥")
    }

    // 3. 动态生成随机密钥（每次构建不同，增加逆向难度）
    // 注意：这会使得同一 AppKey 在不同构建中混淆结果不同，但运行时能正确解混淆
    val random = SecureRandom()
    val bytes = ByteArray(16)
    random.nextBytes(bytes)
    return Base64.getEncoder().encodeToString(bytes).take(16)
}

/**
 * XOR + Base64 混淆：将明文与固定密钥逐字节 XOR 后 Base64 编码
 * 运行时使用相同密钥 XOR 即可还原
 */
fun obfuscateXor(input: String, key: String): String {
    if (input.isEmpty()) return ""
    val keyBytes = key.toByteArray(Charsets.UTF_8)
    val inputBytes = input.toByteArray(Charsets.UTF_8)
    val result = ByteArray(inputBytes.size)
    for (i in inputBytes.indices) {
        result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
    }
    return Base64.getEncoder().encodeToString(result)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.sentry)
}

// ===== 安全配置读取 =====
// 友盟 AppKey 从 local.properties 读取（已 gitignore），不在版本控制中
// 如未配置，构建时输出警告但不阻塞（debug 构建可用空值）
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val umengAppKey: String = localProperties.getProperty("UMENG_APPKEY")
    ?: project.findProperty("UMENG_APPKEY") as String?
    ?: ""
val umengMessageSecret: String = localProperties.getProperty("UMENG_MESSAGE_SECRET")
    ?: project.findProperty("UMENG_MESSAGE_SECRET") as String?
    ?: ""

val sentryDsn: String = localProperties.getProperty("SENTRY_DSN")
    ?: project.findProperty("SENTRY_DSN") as String?
    ?: System.getenv("SENTRY_DSN")
    ?: ""

if (umengAppKey.isEmpty()) {
    println("⚠️ UMENG_APPKEY 未配置，友盟统计将不可用。请在 local.properties 中设置。")
}

// 读取签名配置
// 优先级：1. gradle.properties 中的 RELEASE_* 配置
//        2. keystore-release.properties 文件（不应提交到版本控制）
//        3. keystore.properties 模板文件
val keystoreProperties = Properties()

// 方式1：从 gradle.properties 读取
val releaseStoreFile = project.findProperty("RELEASE_STORE_FILE") as String?
val releaseStorePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
val releaseKeyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
val releaseKeyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?

// 方式2：从 keystore-release.properties 文件读取（优先级更高）
val keystorePropertiesFile = file("keystore-release.properties")
    .takeIf { it.exists() }
    ?: file("keystore.properties")

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

// 合并配置：文件配置优先于 gradle.properties
val finalStoreFile = keystoreProperties.getProperty("storeFile") ?: releaseStoreFile
val finalStorePassword = keystoreProperties.getProperty("storePassword") ?: releaseStorePassword
val finalKeyAlias = keystoreProperties.getProperty("keyAlias") ?: releaseKeyAlias
val finalKeyPassword = keystoreProperties.getProperty("keyPassword") ?: releaseKeyPassword

android {
    namespace = "com.silas.omaster"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.silas.omaster"
        minSdk = 24
        targetSdk = 36
        // 版本号规范：
        // versionCode: 内部版本号，每次发布必须递增
        // versionName: 对外显示版本号，格式 主.次.修订
        // 正式版: 1.0, 1.0.1, 1.1.0, 2.0.0
        // 测试版: 1.0.0-beta1, 1.0.0-beta2
        //
        // 版本号与 Git Tag 同步规则：
        // - Git Tag 格式: v{versionName}，如 v1.0.0
        // - CI 构建时会自动从 Tag 提取版本号
        //
        // 当前版本: v2.3.6
        // 版本号计算公式: 主版本*10000 + 次版本*100 + 修订版本
        // 2.3.6 → 2*10000 + 3*100 + 6 = 20306
        versionCode = 20306
        versionName = "2.3.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 资源优化：只保留需要的语言资源
        @Suppress("DEPRECATION")
        resourceConfigurations += listOf("en", "zh", "zh-rCN", "zh-rTW")
        
        // ===== 安全配置注入到 BuildConfig =====
        // 友盟统计 AppKey：构建时混淆后注入，运行时在 OMasterApplication 中解混淆
        // 防止 APK 反编译后直接提取明文 AppKey
        val obfuscationKey = getObfuscationKey(localProperties)
        buildConfigField("String", "UMENG_APPKEY", "\"${obfuscateXor(umengAppKey, obfuscationKey)}\"")
        buildConfigField("String", "UMENG_MESSAGE_SECRET", "\"${obfuscateXor(umengMessageSecret, obfuscationKey)}\"")
        buildConfigField("String", "OBFUSCATION_KEY", "\"$obfuscationKey\"")
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDsn\"")
    }

    // 签名配置
    signingConfigs {
        getByName("debug") {
            // 使用默认debug签名
        }
        // Release签名配置
        create("release") {
            // 检查是否有有效的签名配置（来自 gradle.properties 或 keystore 文件）
            val hasValidKeystore = finalStorePassword != null &&
                finalStorePassword != "YOUR_STORE_PASSWORD" &&
                finalStorePassword != "android"

            if (hasValidKeystore && finalStoreFile != null) {
                // 使用真实签名配置
                storeFile = file(finalStoreFile)
                storePassword = finalStorePassword!!
                keyAlias = finalKeyAlias ?: "omaster"
                keyPassword = finalKeyPassword!!
                println("✅ Release 签名配置已加载: $finalStoreFile")
            } else {
                // CI 环境：使用 debug 签名回退，确保 CI 可以构建 release 包进行测试
                // 生产发布时必须配置真实签名
                val isCI = System.getenv("CI") == "true"
                if (isCI) {
                    println("⚠️ CI 环境检测到，Release 构建使用 debug 签名回退")
                    storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                } else {
                    // 本地开发：未配置签名时给出明确错误
                    throw GradleException(
                        "❌ Release 签名未配置！请在 gradle.properties 或 keystore-release.properties 中设置 RELEASE_* 变量。\n" +
                        "开发调试请使用 debug 构建类型：./gradlew assembleDebug\n" +
                        "CI 环境会自动使用 debug 签名回退。"
                    )
                }
            }
        }
    }

    // 添加 splits 配置，按 ABI 拆分 APK
    splits {
        abi {
            // 启用 ABI 拆分
            isEnable = true
            // 重置当前支持的 ABI 列表（如果不调用 reset()，include 会追加到默认列表）
            reset()
            // 指定需要拆分的 ABI 类型
            // 包含 ARM 架构（实际设备）和 x86/x86_64（平板、Chrome OS、模拟器）
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            // 生成一个包含所有 ABI 的通用 APK（用于不支持拆分的场景）
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Kotlin 编译选项
    kotlin {
        jvmToolchain(17)
        compilerOptions {
            optIn.add("kotlin.RequiresOptIn")
            optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
            optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
            optIn.add("androidx.compose.animation.ExperimentalAnimationApi")
            optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 测试覆盖率配置
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            // Debug 构建关闭代码压缩和资源压缩，加速构建
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // 启用 R8 代码混淆（完整模式）
            isMinifyEnabled = true
            // 启用资源压缩
            isShrinkResources = true
            // 启用 R8 完整模式（更激进的优化，APK 更小、运行更快）
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // 发布前关闭调试信息
            isDebuggable = false
            isJniDebuggable = false
            isPseudoLocalesEnabled = false
            // 启用资源去重与混淆
            resValue("string", "build_type", "release")
        }
    }

    // ===== ProGuard Mapping 文件管理 =====
    // Release 构建后，mapping 文件位于：
    //   app/build/outputs/mapping/release/mapping.txt
    //
    // 发布流程：
    // 1. 每次发布构建后，将 mapping.txt 备份到 app/mapping/ 目录
    // 2. 命名格式：mapping-{versionName}-{versionCode}.txt（如 mapping-1.0.0-100.txt）
    // 3. 将 mapping 文件提交到版本控制或上传到 Crash 分析平台
    // 4. 线上 Crash 堆栈反混淆需要对应的 mapping 文件
    // 5. 如使用 Firebase Crashlytics 或 Sentry，可自动上传 mapping 文件
    //
    // 自动备份 mapping 文件（Release 构建后执行）
    afterEvaluate {
        // Release 签名校验：禁止使用默认 android debug 密钥进行正式发布
        tasks.matching { it.name == "assembleRelease" }.configureEach {
            doFirst {
                val signing = android.signingConfigs.findByName("release")
                if (signing != null) {
                    val storeFile = signing.storeFile
                    val storePassword = signing.storePassword
                    if (storeFile != null && storeFile.exists() && !storePassword.isNullOrEmpty()) {
                        val isDebug = storeFile.name.contains("debug", ignoreCase = true) ||
                            storePassword == "android"
                        val isCI = System.getenv("CI") == "true"
                        if (isDebug && !isCI) {
                            throw GradleException(
                                "❌ Release 构建检测到 debug 签名（storeFile=${storeFile.name}），禁止用于生产发布。\n" +
                                "请在 keystore-release.properties 配置真实签名后重试。"
                            )
                        }
                        if (isDebug) {
                            println("⚠️ CI 环境使用 debug 签名回退（仅用于构建验证，不可发布到生产）")
                        }
                    }
                }
            }
            doLast {
                val mappingFile = file("${layout.buildDirectory.get()}/outputs/mapping/release/mapping.txt")
                if (mappingFile.exists()) {
                    val backupDir = file("${project.projectDir}/mapping")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    val targetFile = file("${backupDir}/mapping-${android.defaultConfig.versionName}-${android.defaultConfig.versionCode}.txt")
                    mappingFile.copyTo(targetFile, overwrite = true)
                    println("✅ ProGuard mapping 文件已备份: ${targetFile.name}")
                }
            }
        }
    }

    // Android 资源优化配置
    androidResources {
        // 标记不需要 AAPT2 压缩的文件类型（直接拷贝到 APK）
        noCompress += listOf(
            "tflite",      // TFLite 模型文件，直接拷贝避免运行时解压
            "task",        // ML Kit task 文件
            "bin",         // 二进制资源
            "dat",         // 数据文件
            "ttf",         // 字体文件
            "otf"          // OpenType 字体
        )
    }

    // Lint 配置
    lint {
        // 检查主要源代码，排除测试和生成的代码
        // 不限制 checkOnly，确保性能、兼容性、安全等全量检查
        // Release 构建强制 abortOnError，Debug 构建可容忍
        abortOnError = (gradle.startParameter.taskNames.any { it.contains("Release") })
        // release 构建时检查
        checkReleaseBuilds = true
        // 全量检查模式（无 baseline 容忍）
        checkAllWarnings = true
        warningsAsErrors = false
        // 忽略警告（谨慎使用）
        disable.add("IconLauncherShape")
        disable.add("IconMissingDensityFolder")
        // 错误严重级别配置
        // HardcodedText: 项目历史代码中存在大量中文硬编码，短期内全部迁移到 strings.xml
        // 风险高、收益低；降级为 warning 避免阻塞 Release 构建，后续迭代逐步国际化。
        warning.add("HardcodedText")
        // MissingTranslation: 英文翻译文件已存在但可能不完全对齐，降级为 warning
        // 避免新增 strings 后 Release 构建立即失败。
        warning.add("MissingTranslation")
        warning.add("UnusedResources")
        warning.add("IconDensities")
        // 错误级检查：保持开启，确保安全/性能/兼容性缺陷零容忍
        error.add("UnsafeProtectedBroadcastReceiver")
        error.add("WorldReadableFiles")
        error.add("WorldWriteableFiles")
        error.add("ExportedReceiver")
        error.add("ExportedService")
        error.add("ExportedContentProvider")
        // 国际化检查
        error.add("ExtraTranslation")
    }

    // 测试选项配置
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { testTask ->
                testTask.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                }
                // 强制每次执行测试
                testTask.outputs.upToDateWhen { false }
            }
        }
    }

    // ===== Sentry 配置 =====
    // 自动上传 ProGuard mapping 文件，关联版本和提交信息
    // DSN 通过 BuildConfig 注入，支持 debug/release 不同 DSN
    sentry {
        // 自动上传 ProGuard/R8 mapping 文件（CI 环境无 Sentry auth token，禁用上传）
        includeProguardMapping = true
        autoUploadProguardMapping = false
        // 自动上传源码包（CI 环境无 Sentry auth token，禁用上传）
        includeSourceContext = false
        // 自动关联 Git 提交信息
        includeDependenciesReport = true
        // 组织/项目标识（slug），用于在 Sentry 后台区分不同环境
        org = "omaster"
        projectName = "omaster-android"
        // 调试/发布使用不同环境
        debug = false
    }

    // 打包配置
    packaging {
        // v2.3.6 修复：与 AndroidManifest.extractNativeLibs="true" 配合，
        // 允许系统将 .so 从 APK 提取到 /data/app/.../lib 后再加载。
        // 相比直接从 APK 映射，这种方式兼容性更好，能显著降低部分 OEM/低内存设备
        // 因无法直接映射压缩 so 而导致的 UnsatisfiedLinkError / Native 崩溃 / OOM 风险。
        // 注意：提取后会占用额外磁盘空间，largeHeap=true 已同步开启以缓解内存压力。
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "linuxMain/**"
            excludes += "nativeMain/**"
            excludes += "commonMain/**"
            excludes += "META-INF/kotlin-project-structure-metadata.json"
            excludes += "META-INF/versions/**"
        }
    }

    // ===== 排除 Web 前端资源，防止打包进 APK =====
    // 项目根目录存在 React/Vite Web 项目（src/、public/、index.html 等）
    // 这些文件不属于 Android 模块，必须显式排除
    sourceSets["main"].java.setSrcDirs(
        listOf(file("src/main/java"), file("src/main/kotlin"))
    )
    sourceSets["main"].assets.setSrcDirs(
        listOf(file("src/main/assets"))
    )
    sourceSets["main"].res.setSrcDirs(
        listOf(file("src/main/res"))
    )

    // ===== Android 16 16KB Page Size 适配 =====
    // Android 16 设备支持 16KB 内存页，需验证原生库对齐
    // 项目无自研 .so 文件，第三方库（TFLite、ML Kit、友盟）需验证
    // CI 构建中添加 zipalign 检查确保对齐
    packaging {
        jniLibs {
            // 使用 legacy packaging 确保 .so 被提取而非直接映射
            // 兼容性更好，降低 OEM/低内存设备崩溃风险
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // 核心依赖（已使用 catalog，保持不变）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM 平台依赖（已使用 catalog）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Compose Material（pull-to-refresh 等兼容场景）- 已迁移到 catalog
    implementation(libs.androidx.compose.material)    // 对应 androidx.compose.material:material:1.7.7

    // 导航组件（已使用 catalog）
    implementation(libs.androidx.navigation.compose)

    // Kotlin Serialization（已使用 catalog）
    implementation(libs.kotlinx.serialization.json)

    // ⚠️ 替换所有 Ktor 硬编码依赖
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coil（已使用 catalog）
    implementation(libs.coil.compose)

    // Gson（已使用 catalog）
    implementation(libs.gson)

    // DataStore - 异步配置存储，替代 SharedPreferences
    implementation(libs.androidx.datastore.preferences)

    // ⚠️ 替换友盟硬编码依赖
// 友盟
    implementation(libs.umeng.common)
    implementation(libs.umeng.asms)

    // ML Kit 人脸检测 / 文字识别 / 自拍分割 / 物体检测
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.segmentation.selfie)
    implementation(libs.mlkit.`object`.detection)

    // TensorFlow Lite - 场景识别/参数预测模型推理
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support)

    // MediaPipe Tasks Vision: 人脸关键点 / 姿势检测
    implementation(libs.mediapipe.tasks.vision)

    // CameraX 实时相机预览 - P2 深度优化
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.extensions)

    // ProfileInstaller - 启动性能优化（ART 配置文件）
    implementation(libs.androidx.profileinstaller)

    // SplashScreen - Android 12+ 启动画面过渡，消除白屏
    implementation(libs.androidx.core.splashscreen)

    // WorkManager - 后台定期同步
    implementation(libs.androidx.work.runtime.ktx)

    // Sentry - 崩溃上报与性能监控
    implementation(libs.sentry.android)

    // LeakCanary - 内存泄漏检测（仅 Debug 构建）
    debugImplementation(libs.leakcanary.android)

    // kotlinx-coroutines-play-services（为 ML Kit Task 提供 await()）
    implementation(libs.kotlinx.coroutines.play.services)

    // Google Play Billing（订阅与内购）
    implementation(libs.bundles.billing)

    // Firebase Cloud Messaging
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Google Play In-App Review
    implementation(libs.play.review.ktx)

    // Google Play In-App Update
    implementation(libs.play.app.update.ktx)

    // 测试依赖（已使用 catalog）
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}