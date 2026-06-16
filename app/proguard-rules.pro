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
# Ktor 客户端相关规则（精简）
# ========================================
# Ktor 内部使用反射，但大部分可以混淆
-keep class io.ktor.client.** { *; }
-keepclassmembers class io.ktor.client.** { *; }
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
# 项目特定规则（仅保留必要的数据模型字段）
# ========================================

# 数据模型类（JSON 序列化需要保留字段名）
-keepclassmembers class com.silas.omaster.model.** {
    <fields>;
}

# 数据模型类名（Gson 反序列化需要）
-keep class com.silas.omaster.model.MasterPreset { *; }
-keep class com.silas.omaster.model.PresetList { *; }
-keep class com.silas.omaster.model.PresetItem { *; }
-keep class com.silas.omaster.model.PresetSection { *; }

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
-useuniqueclassmembernames

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ========================================
# 精确 dontwarn（替代 -dontwarn **.**）
# ========================================
-dontwarn java.lang.invoke.**
