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

# 【关键】保留资源 ID 不被混淆，确保 getIdentifier() 正常工作
# Realme 预设使用 @string/xxx 格式通过反射获取资源
-keep class com.silas.omaster.R$string { *; }
-keepclassmembers class com.silas.omaster.R$string { *; }

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

# ═══════════════════════════════════════════════════════════
# MU-29 / v1.6：APK 体积 & 性能加固
# ═══════════════════════════════════════════════════════════

# Glance widget（MU-28）保持反射类不被混淆
-keep class androidx.glance.** { *; }
-keep class androidx.glance.appwidget.** { *; }

# Kotlin Serialization：已加 plugin，显式兜底 keep（防止 AGP 版本差异）
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Coil：图片加载性能 —— 允许 R8 去掉 DEBUG 级日志
-assumenosideeffects class coil.Coil {
    public static void setDefaultImageLoader(...);
}

# 悬浮窗工厂（MU-11 / MU-27）：反射构建 TextView 时 keep 构造器
# 避免 ProGuard 把被反射调用的构造器移除
-keep class com.silas.omaster.ui.service.FloatingWindowWidgetFactory {
    public android.widget.TextView createIconButton(...);
    public android.widget.LinearLayout createBackground(...);
}
-keep class com.silas.omaster.ui.service.FloatingWindowCompactFactory { *; }

# 预设 JSON schema 版本兼容（ED-03 历史版本迁移）
# 所有 @Serializable 的 MasterPreset / PresetSection / PresetItem 保留类名
# 否则运行时反射取字段名会失败
-keep class com.silas.omaster.model.** { *; }

# DeviceDetector / CameraFramingAvoidance 反射 Build.HARDWARE
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
