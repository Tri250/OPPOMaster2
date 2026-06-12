import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

// 读取签名配置
val keystorePropertiesFile = file("keystore-release.properties")
    .takeIf { it.exists() }
    ?: file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.silas.omaster"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.silas.omaster"
        minSdk = 24
        targetSdk = 35
        versionCode = 10
        versionName = "1.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en", "zh", "zh-rCN", "zh-rTW")

        // ===== 编译优化配置 =====
        // 启用多dex加速编译
        multiDexEnabled = true
    }

    // ===== 签名配置优化 =====
    signingConfigs {
        getByName("debug") {
            // 使用默认debug签名
        }
        create("release") {
            val hasValidKeystore = keystoreProperties.containsKey("storePassword") &&
                keystoreProperties.getProperty("storePassword") != "YOUR_STORE_PASSWORD"

            if (hasValidKeystore) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            } else {
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    // ===== ABI拆分优化 =====
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ===== 构建类型深度优化 =====
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
            isMinifyEnabled = false
            isShrinkResources = false
            // Debug构建优化
            isCrunchPngs = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ===== Lint深度优化 =====
    lint {
        checkOnly.add("Interoperability")
        abortOnError = false
        checkReleaseBuilds = false
        // 性能优化
        checkDependencies = false
        ignoreTestSources = true
    }

    // ===== 测试配置优化 =====
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all { testTask ->
                testTask.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                    showStandardStreams = true
                }
                testTask.outputs.upToDateWhen { false }
                // 测试并行执行
                testTask.maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
            }
        }
        // 模拟器测试优化
        animationsDisabled = true
    }

    // ===== 打包配置优化 =====
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "META-INF/*.kotlin_module"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    // ===== Compose编译器优化 =====
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

// ===== 依赖配置优化 =====
dependencies {
    // 核心依赖
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material)

    // 导航组件
    implementation(libs.androidx.navigation.compose)

    // Kotlin Serialization
    implementation(libs.kotlinx.serialization.json)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coil
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)

    // 友盟
    implementation(libs.umeng.common)
    implementation(libs.umeng.asms)

    // ML Kit
    implementation(libs.mlkit.face.detection)

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.support)

    // Coroutines
    implementation(libs.kotlinx.coroutines.play.services)

    // ===== 测试依赖 =====
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
