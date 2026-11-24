import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // 添加 Kotlin Serialization 插件，用于类型安全的导航
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.silas.omaster"
    compileSdk {
        // ED-08：compileSdk 升至 37（Android 17 API 面，ColorOS 17 / Find X10 适配基线）；
        // targetSdk 维持 36，运行时行为变更延后到 M2/M3 评估
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.silas.omaster"
        minSdk = 23  // Android 6
        targetSdk = 36
        // 版本号规范：
        // versionCode: 内部版本号，每次发布必须递增
        // versionName: 对外显示版本号，格式 主.次.修订
        // 正式版: 1.0, 1.0.1, 1.1.0, 2.0.0
        // 测试版: 1.0.0-beta1, 1.0.0-beta2
        // versionCode 由 CI 在发布构建时通过 -PciVersionCode 注入（基于提交数单调递增，ED-02）；
        // 本地开发缺省 13，禁止手填递增
        versionCode = (project.findProperty("ciVersionCode") as String?)?.toInt() ?: 13
        versionName = "1.8.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 【v1.8.10 上架合规】渠道分离：
    // github flavor——GitHub Releases 分发，保留应用内自更新（REQUEST_INSTALL_PACKAGES）。
    // store flavor——应用商店（华为/小米/OV/应用宝）分发，商店条款禁止应用内自更新 APK：
    //   1. app/src/store/AndroidManifest.xml 移除 REQUEST_INSTALL_PACKAGES 权限；
    //   2. BuildConfig.STORE_BUILD 门控全部更新触发点（自动检查 + 设置页 + 关于页），
    //      商店版本提示用户前往商店更新。
    flavorDimensions += listOf("channel")
    productFlavors {
        create("github") {
            dimension = "channel"
            buildConfigField("boolean", "STORE_BUILD", "false")
        }
        create("store") {
            dimension = "channel"
            buildConfigField("boolean", "STORE_BUILD", "true")
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    // 【v1.8.10 上架审计】Robolectric 测试（ED-03）运行时需联网下载 android-all SDK jar，
    // 但测试 JVM 不继承 Gradle daemon 的代理配置，在代理环境下初始化直接 ConnectException。
    // 从环境变量透传代理到测试 JVM；CI/无代理环境（env 未设置）不受影响。
    testOptions {
        unitTests.all {
            val proxyUrl = System.getenv("HTTPS_PROXY") ?: System.getenv("https_proxy")
            if (!proxyUrl.isNullOrBlank()) {
                val cleaned = proxyUrl.removePrefix("http://").removePrefix("https://").trimEnd('/')
                val host = cleaned.substringBefore(":")
                val port = cleaned.substringAfter(":", "8080")
                if (host.isNotBlank()) {
                    it.systemProperty("https.proxyHost", host)
                    it.systemProperty("https.proxyPort", port)
                    it.systemProperty("http.proxyHost", host)
                    it.systemProperty("http.proxyPort", port)
                }
            }
        }
    }

    // ED-01 签名体系：CI 通过环境变量注入 keystore（见 .github/workflows/release.yml）。
    // 本地未设置环境变量时不创建签名配置，debug 构建不受影响；
    // 密钥文件落盘到 .signing/（已加入 .gitignore，禁止入库）。
    val keystoreBase64 = System.getenv("OMASTER_KEYSTORE_BASE64")
    val keystorePassword = System.getenv("OMASTER_KEYSTORE_PASSWORD")

    signingConfigs {
        if (!keystoreBase64.isNullOrBlank() && !keystorePassword.isNullOrBlank()) {
            create("release") {
                val ksFile = rootProject.file(".signing/omaster-release.keystore")
                if (!ksFile.exists()) {
                    ksFile.parentFile.mkdirs()
                    ksFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                }
                storeFile = ksFile
                storePassword = keystorePassword
                keyAlias = System.getenv("OMASTER_KEYSTORE_ALIAS") ?: "omaster"
                keyPassword = System.getenv("OMASTER_KEY_PASSWORD") ?: keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // ED-08：java.time（YearMonth/LocalDate 等）需 API 26+，minSdk 23 通过 desugaring 支持
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // core library desugaring（java.time 支持，配合 isCoreLibraryDesugaringEnabled）
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
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

    // MU-28 / v1.6：AndroidX Glance 桌面小组件（今日推荐预设）
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

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
    // Gson 已移除（ED-04/MU-25：JSON 双栈统一为 kotlinx.serialization）

    // Room 数据库已移除，使用 SharedPreferences 替代

    // ⚠️ 替换友盟硬编码依赖
    implementation(libs.umeng.common)
    implementation(libs.umeng.asms)
    implementation(libs.androidx.material3)

    // 图片边框 - 主色调提取
    implementation(libs.androidx.palette)

    // 图片边框 - EXIF信息读取
    implementation(libs.androidx.exifinterface)

    // CameraX：POP 即拍自研拍摄管线（手动参数写入拍摄请求，见 PresetCapturePlan）
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // 测试依赖（已使用 catalog）
    testImplementation(libs.junit)
    // ED-03：Robolectric 持久层测试（SharedPreferences 读写无需真机）
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
