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
# Jetpack Compose 相关规则
# ========================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Composable 函数
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class **$$Composable { *; }
-keep class **$composer { *; }
-keep class **$Composer { *; }
-keepclassmembers class **$composer { *; }
-keepclassmembers class **$Composer { *; }

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
# Gson 序列化规则
# ========================================
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
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
# TensorFlow Lite 相关规则
# ========================================
-keep class org.tensorflow.lite.Interpreter { *; }
-keepclassmembers class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.support.common.FileUtil { *; }
-keepclassmembers class org.tensorflow.lite.support.common.FileUtil { *; }
-keep class org.tensorflow.lite.support.common.TensorOperator { *; }
-keepclassmembers class org.tensorflow.lite.support.common.TensorOperator { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.**

# ========================================
# ML Kit 人脸检测相关规则
# ========================================
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.**

# ========================================
# Ktor 客户端相关规则
# ========================================
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# ========================================
# Coil 图片加载
# ========================================
-keep class coil.** { *; }
-dontwarn coil.**

# ========================================
# 友盟 SDK
# ========================================
-keep class com.umeng.** { *; }
-keep class com.uc.** { *; }
-dontwarn com.umeng.**
-dontwarn com.uc.**

# ========================================
# SLF4J 日志框架
# ========================================
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn sun.misc.**

# ========================================
# 项目特定规则（精细化）
# ========================================

# ===== 数据模型类（JSON 序列化需要保留字段） =====
-keep class com.silas.omaster.model.** {
    <fields>;
    <init>(...);
}

# ===== 数据仓库和本地存储 =====
-keep class com.silas.omaster.data.repository.Preset { <fields>; }
-keep class com.silas.omaster.data.repository.PresetCategory { <fields>; }
-keep class com.silas.omaster.data.local.SettingsManager { *; }
-keep class com.silas.omaster.data.local.CustomPresetManager { <fields>; <init>(...); }
-keep class com.silas.omaster.data.local.RecipeHistoryManager { <fields>; <init>(...); }

# ===== AI 和 TFLite 相关 =====
-keep class com.silas.omaster.ai.MasterInsightEngine { <fields>; <init>(...); }
-keep class com.silas.omaster.ai.MasterInferenceEngine { <fields>; <init>(...); }
-keep class com.silas.omaster.ai.SceneRecognitionManager { <fields>; <init>(...); }
-keep class com.silas.omaster.ai.AIFineTuneManager { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.TFLiteEngine { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.ImageQualityAnalyzer { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.SceneClassifier { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.ParamPredictor { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.ModelDownloadManager { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.ModelLoader { <fields>; <init>(...); }
-keep class com.silas.omaster.tflite.InferenceResult { <fields>; }
-keep class com.silas.omaster.tflite.models.** { <fields>; }

# ===== 水印系统 =====
-keep class com.silas.omaster.watermark.WatermarkConfig { <fields>; }
-keep class com.silas.omaster.watermark.WatermarkLayer { <fields>; }
-keep class com.silas.omaster.watermark.WatermarkTemplate { <fields>; }
-keep class com.silas.omaster.watermark.HasselbladMasterTemplates { <fields>; <init>(...); }
-keep class com.silas.omaster.watermark.ExifWatermarkProvider { <fields>; <init>(...); }

# ===== 云同步 =====
-keep class com.silas.omaster.cloud.CloudSyncManager { <fields>; <init>(...); }
-keep class com.silas.omaster.cloud.CloudPreset { <fields>; }
-keep class com.silas.omaster.cloud.SyncState { <fields>; }

# ===== GPU 渲染器 =====
-keep class com.silas.omaster.renderer.GPURenderManager { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.ShaderProgram { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.ImageShaderRenderer { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.RenderParameters { <fields>; }

# ===== 参数系统 =====
-keep class com.silas.omaster.param.RenderParams { <fields>; }
-keep class com.silas.omaster.param.AdjustChannel { <fields>; }
-keep class com.silas.omaster.param.ParamAdjustmentManager { <fields>; <init>(...); }

# ===== 工具类 =====
-keep class com.silas.omaster.util.JsonUtil { <methods>; }
-keep class com.silas.omaster.util.SecurityCrypto { <methods>; <init>(...); }

# ===== ViewModel =====
-keep class com.silas.omaster.ui.home.HomeViewModel { *; }
-keep class com.silas.omaster.ui.detail.DetailViewModel { *; }
-keep class com.silas.omaster.ui.create.UniversalCreatePresetViewModel { *; }

# ===== Application 和 Activity =====
-keep class com.silas.omaster.OMasterApplication { *; }
-keep class com.silas.omaster.MainActivity { *; }

# ========================================
# 优化配置
# ========================================
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

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
