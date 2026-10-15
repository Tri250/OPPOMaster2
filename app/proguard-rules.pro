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
# 【v1.6.0 启动闪退修复】移除 -mergeinterfacesaggressively：
# 该开关会激进合并接口，破坏 Kotlin SAM / AndroidX 接口的虚方法分派，
# 在 release 包中偶发 AbstractMethodError / ClassCastException；R8 默认关闭，保持默认。

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

# ═══════════════════════════════════════════════════════════
# 【v1.6.0 启动闪退修复】Kotlin Serialization 运行时解析兜底
# ═══════════════════════════════════════════════════════════
# 背景：类型安全导航（NavHost / composable<T> / toRoute<T>）在运行时通过反射调用
# kotlinx.serialization.serializer<T>()。该调用发生在 navigation 库内部（已编译好的字节码），
# 无法被 kotlinx.serialization 编译插件 intrinsic 内联为直接引用，因此 R8 会把 App 内
# @Serializable 类生成的 $serializer / Companion.serializer() 视为"从未被调用"而剔除，
# 导致 release 包在 NavHost 建图时抛 SerializationException → 启动闪退。
# 说明：下面的 -if 条件规则只作用于"已存活"的 @Serializable 类，不会造成体积膨胀。
-keepattributes *Annotation*, InnerClasses, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.AnnotationsKt

# ① Companion 实例（serializer() 的宿主）
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# ② Companion.serializer()（类）
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# ③ @Serializable object 的 INSTANCE + 静态 serializer()（Screen.* 路由均为 data object）
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ④ App 自有包兜底：Screen 路由（com.silas.omaster.Screen.*）与 data.local.CheckInManager /
#    ColorWalkManager 均不在 model 包内，上面的 -keep class com.silas.omaster.model.** 覆盖不到，
#    这里再做一次显式保留（Google 类型安全导航官方建议规则）。
-keep,includedescriptorclasses class com.silas.omaster.**$$serializer { *; }
-keepclassmembers class com.silas.omaster.** {
    *** Companion;
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

# ═══════════════════════════════════════════════════════════
# 【v1.6.0 启动闪退修复】v1.6 新增静态组件保留
# 这些组件由系统 / Glance 按 AndroidManifest 中的类名反射实例化，
# 显式保留其无参构造器与入口方法，避免 release 包出现
# ClassNotFoundException / InstantiationException。
# ═══════════════════════════════════════════════════════════

# MU-28：Glance 桌面小组件（TodayWidget + TodayWidgetReceiver）
-keep class com.silas.omaster.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# 下载完成广播接收器（android.intent.action.DOWNLOAD_COMPLETE）
-keep class com.silas.omaster.util.DownloadCompleteReceiver {
    <init>(...);
}

# MU-24：ColorOS 17 侧边键悬浮窗快捷方式广播接收器
-keep class com.silas.omaster.ui.service.ShortcutBroadcastReceiver {
    <init>(...);
}

