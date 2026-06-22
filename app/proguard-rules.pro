# ========================================
# 基础配置
# ========================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ========================================
# Kotlin 基础规则
# ========================================
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Kotlin 协程 — 收敛到字段级别
-keepclassmembers class kotlinx.coroutines.** {
    public <methods>;
    public <fields>;
}
-keep class kotlin.coroutines.Continuation { *; }
-keep class kotlin.coroutines.CoroutineContext { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin 序列化
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn kotlinx.serialization.**

# ========================================
# Jetpack Compose 相关规则（精简）
# ========================================
# 仅保留 Composable 函数，不保留整个 compose 包
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <fields>;
}
-dontwarn androidx.compose.**

# ========================================
# AndroidX 基础库
# ========================================
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    public <methods>;
}
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }
-keep class **$NavArgs { *; }
-keepclassmembers class **$NavArgs { *; }
-dontwarn androidx.**

# ========================================
# Gson 序列化规则（精简）
# ========================================
# 仅保留必要的 Gson 类型适配器接口
-keep class * implements com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers class * implements com.google.gson.TypeAdapterFactory {
    <init>(...);
}
-dontwarn com.google.gson.**

# ========================================
# Parcelable/Serializable 规则
# ========================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keep class **$$Parcelable { *; }
-keepclassmembers class **$$Parcelable { *; }

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========================================
# TensorFlow Lite 相关规则（精简）
# ========================================
-keep class org.tensorflow.lite.Interpreter { *; }
-dontwarn org.tensorflow.**

# ========================================
# ML Kit 人脸检测相关规则（精简）
# ========================================
-keep class com.google.mlkit.vision.face.FaceDetector { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.**

# ========================================
# Ktor 客户端相关规则（精简优化版）
# ========================================
# 只保留必要的序列化/反射入口，大幅减小APK体积
# 保留核心引擎和插件接口
-keep class io.ktor.client.HttpClient { *; }
-keep class io.ktor.client.engine.** { *; }
-keep class io.ktor.client.plugins.** { *; }

# 保留序列化相关类（用于JSON解析）
-keepclassmembers class io.ktor.client.call.HttpClientCall {
    public <methods>;
}
-keepclassmembers class io.ktor.client.statement.HttpResponse {
    public <methods>;
}

# 保留内容协商插件（用于JSON序列化）
-keep class io.ktor.serialization.kotlinx.json.** { *; }

# 抑制警告
-dontwarn io.ktor.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ========================================
# Coil 图片加载（精简）
# ========================================
# Coil 使用注解处理器，大部分可以混淆
-keep class coil.request.ImageRequest$Builder { *; }
-keep class coil.ImageLoader { *; }
-dontwarn coil.**

# ========================================
# 友盟 SDK（精简）
# ========================================
-keep class com.umeng.analytics.** { *; }
-keep class com.umeng.commonsdk.** { *; }
-dontwarn com.umeng.**
-dontwarn com.uc.**

# ========================================
# SLF4J 日志框架
# ========================================
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn sun.misc.**

# ========================================
# 项目特定规则 — kotlinx.serialization 数据模型
# ========================================
# [优化] 替换原先的 -keep class ... { *; } 和 -keepclassmembers ... { <fields>; } 规则
# 原规则过于宽泛，保留了所有类和所有字段，阻碍 R8 优化并增大 APK 体积。
# 现改为 kotlinx.serialization 官方推荐规则，仅保留序列化器必需的成员，
# 让 R8 可正常混淆和移除未使用的非序列化字段。

-dontnote kotlinx.serialization.AnnotationsKt

# kotlinx.serialization 核心 JSON 序列化器
-keepclassmembers class kotlinx.serialization.json.** { *; }

# @Serializable 类的 $$serializer（编译器插件生成，反序列化入口）
-keep class com.silas.omaster.model.**$$serializer { *; }
-keep class com.silas.omaster.data.model.**$$serializer { *; }
-keep class com.silas.omaster.renderer.**$$serializer { *; }
-keep class com.silas.omaster.watermark.**$$serializer { *; }

# @Serializable 类的 Companion 对象（提供 serializer() 函数入口）
-keepclassmembers class com.silas.omaster.model.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.data.model.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.renderer.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.watermark.** {
    *** Companion;
}

# 保留包含 serializer() 方法的类及其成员（确保序列化字段名不被混淆）
-keepclasseswithmembers class com.silas.omaster.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.silas.omaster.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.silas.omaster.renderer.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.silas.omaster.watermark.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin Serialization 枚举需要保留（枚举值按名称序列化）
-keepclassmembers enum com.silas.omaster.model.SceneCategory { *; }
-keepclassmembers enum com.silas.omaster.model.SoftLightMode { *; }
-keepclassmembers enum com.silas.omaster.model.FilmSeries { *; }
-keepclassmembers enum com.silas.omaster.renderer.RenderQuality { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkLayerType { *; }
-keepclassmembers enum com.silas.omaster.watermark.ContentSource { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkPosition { *; }

# ========================================
# 优化配置（修复 P2-12: 降低激进程度，避免 NPE 风险）
# ========================================
# 优化轮次降低，减少内联和重排导致的运行时问题
-optimizationpasses 3
# 允许修改访问修饰符（但保持谨慎）
-allowaccessmodification
# 禁用激进接口合并（可能导致 NPE）
# -mergeinterfacesaggressively

# ========================================
# 混淆配置
# ========================================
# -useuniqueclassmembernames (R8 不支持此选项，已移除)

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ========================================
# Release 构建日志移除
# ========================================
# 移除调试日志，减少 APK 体积并防止信息泄露
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# ========================================
# 精确 dontwarn（替代 -dontwarn **.**）
# ========================================
-dontwarn java.lang.invoke.**
