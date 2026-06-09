# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 优化配置 - 减小 APK 体积

# 保留 Compose 相关类
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# 保留 Gson 相关
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 保留 ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# 保留 Parcelable
-keep class * implements android.os.Parcelable { *; }

# 保留 Serializable
-keep class * implements java.io.Serializable { *; }

# 保留友盟 SDK
-keep class com.umeng.** { *; }
-dontwarn com.umeng.**

# 保留 Coil 图片加载
-keep class coil.** { *; }
-dontwarn coil.**

# 保留导航序列化
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# 保留数据模型
-keep class com.silas.omaster.model.** { *; }

# 保留Manager类（包含StateFlow和SharedPreferences操作）
-keep class com.silas.omaster.ai.** { *; }
-keep class com.silas.omaster.cloud.** { *; }
-keep class com.silas.omaster.watermark.** { *; }
-keep class com.silas.omaster.param.** { *; }
-keep class com.silas.omaster.data.** { *; }
-keep class com.silas.omaster.scene.** { *; }

# 保留UI组件
-keep class com.silas.omaster.ui.** { *; }

# 保留SettingsManager的所有方法
-keepclassmembers class com.silas.omaster.data.local.SettingsManager {
    public *;
    private *;
}

# 优化移除未使用的代码
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**

# 混淆优化
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# 忽略 Ktor 调试检测引用的 ManagementFactory 类（Android 不支持）
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# 忽略 SLF4J 静态绑定器缺失警告
-dontwarn org.slf4j.impl.StaticLoggerBinder

# ========================================
# TensorFlow Lite 相关规则
# ========================================
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.**
# 保留TFLite模型加载相关类
-keepclassmembers class org.tensorflow.lite.Interpreter { *; }
-keepclassmembers class org.tensorflow.lite.support.common.FileUtil { *; }
-keepclassmembers class org.tensorflow.lite.support.common.TensorOperator { *; }

# ========================================
# Jetpack Compose 相关规则
# ========================================
# 保留Composable函数
-keep class **$$Composable { *; }
-keep class **$Composable { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
# 保留Compose UI组件
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
# 保留Compose编译器生成的类
-keep class **$composer { *; }
-keep class **$Composer { *; }
-keepclassmembers class **$composer { *; }
-keepclassmembers class **$Composer { *; }

# ========================================
# Kotlin 协程和 Flow 相关规则
# ========================================
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.flow.** { *; }
# 保留协程上下文和调度器
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.CoroutineContext { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineScope { *; }
# 保留StateFlow和MutableStateFlow
-keep class kotlinx.coroutines.flow.StateFlow { *; }
-keep class kotlinx.coroutines.flow.MutableStateFlow { *; }
-keepclassmembers class kotlinx.coroutines.flow.StateFlow { *; }
-keepclassmembers class kotlinx.coroutines.flow.MutableStateFlow { *; }

# ========================================
# 数据模型类保留规则（补充）
# ========================================
# 保留所有数据类（data class）
-keep class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
# 保留所有Parcelable实现类的CREATOR字段
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
# 保留Parcelable注解
-keep class **$$Parcelable { *; }
-keepclassmembers class **$$Parcelable { *; }

# ========================================
# ML Kit 人脸检测相关规则
# ========================================
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.**

# ========================================
# Ktor 客户端相关规则
# ========================================
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**
# 保留Ktor序列化
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# ========================================
# Navigation 导航相关规则
# ========================================
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }
# 保留导航参数序列化
-keep class **$NavArgs { *; }
-keepclassmembers class **$NavArgs { *; }
