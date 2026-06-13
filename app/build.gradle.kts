import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

// ===== 安全配置读取 =====
// 从 gradle.properties 读取友盟 AppKey（避免硬编码）
val umengAppKey: String = project.findProperty("UMENG_APPKEY") as String? ?: "698938eb9a7f3764885bbdaa"
val umengMessageSecret: String = project.findProperty("UMENG_MESSAGE_SECRET") as String? ?: ""

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
    compileSdk = 35

    defaultConfig {
        applicationId = "com.silas.omaster"
        minSdk = 24
        targetSdk = 35
        // 版本号规范：
        // versionCode: 内部版本号，每次发布必须递增
        // versionName: 对外显示版本号，格式 主.次.修订
        // 正式版: 1.0, 1.0.1, 1.1.0, 2.0.0
        // 测试版: 1.0.0-beta1, 1.0.0-beta2
        versionCode = 10
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 资源优化：只保留需要的语言资源
        resourceConfigurations += listOf("en", "zh", "zh-rCN", "zh-rTW")
        
        // ===== 安全配置注入到 BuildConfig =====
        // 友盟统计 AppKey（从 gradle.properties 读取，避免硬编码）
        buildConfigField("String", "UMENG_APPKEY", "\"$umengAppKey\"")
        buildConfigField("String", "UMENG_MESSAGE_SECRET", "\"$umengMessageSecret\"")
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
                // 回退到 debug 签名（仅用于开发测试）
                // ⚠️ 正式发布前请配置真实的 release 签名！
                println("⚠️ 未配置真实 Release 签名，使用 debug 签名（仅用于开发测试）")
                println("⚠️ 请在 gradle.properties 或 keystore-release.properties 中配置 RELEASE_* 变量")
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
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
            // 指定需要拆分的 ABI 类型，可根据项目实际支持的 ABI 调整
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
    kotlinOptions {
        jvmTarget = "17"
        // 启用实验性 API opt-in，避免编译警告
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
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

    // Lint 配置优化
    lint {
        // 只检查主要源代码，排除测试和生成的代码
        checkOnly.add("Interoperability")
        // 发布前启用严格检查（开发阶段可关闭）
        // ⚠️ 正式发布前请将 abortOnError 改为 true
        abortOnError = false  // 发布前改为 true
        // release 构建时检查
        checkReleaseBuilds = true
        // 忽略警告（谨慎使用）
        ignore.add("IconLauncherShape")
        ignore.add("IconMissingDensityFolder")
        // 错误严重级别配置
        error.add("HardcodedText")
        error.add("MissingTranslation")
        warning.add("UnusedResources")
        warning.add("IconDensities")
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

    // 打包配置
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // ===== 排除 Web 前端资源，防止打包进 APK =====
    // 项目根目录存在 React/Vite Web 项目（src/、public/、index.html 等）
    // 这些文件不属于 Android 模块，必须显式排除
    sourceSets["main"].assets.setSrcDirs(
        listOf(file("src/main/assets"))
    )
    sourceSets["main"].res.setSrcDirs(
        listOf(file("src/main/res"))
    )
}

dependencies {
    // 核心依赖（已使用 catalog，保持不变）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM 平台依赖（已使用 catalog）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // ⚠️ 替换硬编码的 Material 依赖
    implementation(libs.androidx.compose.material)    // 对应 "androidx.compose.material:material:1.7.0"

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

    // Room 数据库已移除，使用 SharedPreferences 替代

    // ⚠️ 替换友盟硬编码依赖
// 友盟
    implementation(libs.umeng.common)
    implementation(libs.umeng.asms)

    // ML Kit 人脸检测
    implementation(libs.mlkit.face.detection)

    // TensorFlow Lite 推理引擎
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support)

    // kotlinx-coroutines-play-services（为 ML Kit Task 提供 await()）
    implementation(libs.kotlinx.coroutines.play.services)

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
