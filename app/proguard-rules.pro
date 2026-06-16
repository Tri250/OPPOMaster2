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
# 项目特定规则（仅保留必要的数据模型字段）
# ========================================

# 数据模型类（JSON 序列化需要保留字段名）
-keepclassmembers class com.silas.omaster.model.** {
    <fields>;
}
-keepclassmembers class com.silas.omaster.data.model.** {
    <fields>;
}
-keepclassmembers class com.silas.omaster.renderer.** {
    <fields>;
}
-keepclassmembers class com.silas.omaster.watermark.** {
    <fields>;
}

# 数据模型类名（Gson / Kotlinx Serialization 反序列化需要）
-keep class com.silas.omaster.model.MasterPreset { *; }
-keep class com.silas.omaster.model.PresetList { *; }
-keep class com.silas.omaster.model.PresetItem { *; }
-keep class com.silas.omaster.model.PresetSection { *; }
-keep class com.silas.omaster.model.PresetComment { *; }
-keep class com.silas.omaster.model.PresetDescription { *; }
-keep class com.silas.omaster.model.SceneProfile { *; }
-keep class com.silas.omaster.model.HasselbladParams { *; }
-keep class com.silas.omaster.model.FilmPreset { *; }
-keep class com.silas.omaster.model.CameraParams { *; }
-keep class com.silas.omaster.model.ExifData { *; }
-keep class com.silas.omaster.model.HistogramData { *; }
-keep class com.silas.omaster.model.FaceData { *; }
-keep class com.silas.omaster.model.FaceInfo { *; }
-keep class com.silas.omaster.model.RectData { *; }
-keep class com.silas.omaster.model.Subscription { *; }
-keep class com.silas.omaster.model.SubscriptionList { *; }
-keep class com.silas.omaster.data.model.PresetSource { *; }
-keep class com.silas.omaster.data.model.PresetSourceConfig { *; }
-keep class com.silas.omaster.data.model.PresetSourceResponse { *; }
-keep class com.silas.omaster.renderer.RenderParameters { *; }
-keep class com.silas.omaster.renderer.ParamMetadata { *; }
-keep class com.silas.omaster.watermark.WatermarkLayerDef { *; }
-keep class com.silas.omaster.watermark.OffsetData { *; }
-keep class com.silas.omaster.watermark.WatermarkLayerStyle { *; }
-keep class com.silas.omaster.watermark.WatermarkTemplateDef { *; }
-keep class com.silas.omaster.watermark.WatermarkConfigDef { *; }

# Kotlin Serialization 枚举需要保留
-keepclassmembers enum com.silas.omaster.model.SceneCategory { *; }
-keepclassmembers enum com.silas.omaster.model.SoftLightMode { *; }
-keepclassmembers enum com.silas.omaster.model.FilmSeries { *; }
-keepclassmembers enum com.silas.omaster.renderer.RenderQuality { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkLayerType { *; }
-keepclassmembers enum com.silas.omaster.watermark.ContentSource { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkPosition { *; }

# Kotlinx Serialization 生成的 serializer 需要保留
-keep class com.silas.omaster.model.**$$serializer { *; }
-keep class com.silas.omaster.data.model.**$$serializer { *; }
-keep class com.silas.omaster.renderer.**$$serializer { *; }
-keep class com.silas.omaster.watermark.**$$serializer { *; }
-keepclassmembers class com.silas.omaster.model.** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class com.silas.omaster.data.model.** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class com.silas.omaster.renderer.** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}
-keepclassmembers class com.silas.omaster.watermark.** {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.Serializable <methods>;
}

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
