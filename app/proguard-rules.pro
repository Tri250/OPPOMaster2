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

# Kotlin 协程
-keepclassmembers class kotlinx.coroutines.** {
    public <methods>;
    public <fields>;
}
-keep class kotlin.coroutines.Continuation { <fields>; <init>(...); }
-keep class kotlin.coroutines.CoroutineContext { <fields>; <init>(...); }
-dontwarn kotlinx.coroutines.**

# Kotlin 序列化
-keep class kotlinx.serialization.** { <fields>; <init>(...); }
-keepclassmembers class kotlinx.serialization.** {
    public <methods>;
    public <fields>;
}
-keep class **$$serializer { <fields>; <init>(...); }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn kotlinx.serialization.**

# ========================================
# Jetpack Compose 相关规则
# ========================================
-keep class androidx.compose.** { <fields>; <init>(...); }
-keepclassmembers class androidx.compose.** {
    public <methods>;
    public <fields>;
}
-dontwarn androidx.compose.**

# Composable 函数
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class **$$Composable { <fields>; }
-keep class **$composer { <fields>; }
-keep class **$Composer { <fields>; }
-keepclassmembers class **$composer {
    public <methods>;
    public <fields>;
}
-keepclassmembers class **$Composer {
    public <methods>;
    public <fields>;
}

# ========================================
# AndroidX 基础库
# ========================================
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    public <methods>;
}
-keep class androidx.navigation.** { <fields>; <init>(...); }
-keepclassmembers class androidx.navigation.** {
    public <methods>;
    public <fields>;
}
-keep class **$NavArgs { <fields>; <init>(...); }
-keepclassmembers class **$NavArgs { <fields>; }
-dontwarn androidx.**

# ========================================
# Gson 序列化规则
# ========================================
-keep class com.google.gson.** { <fields>; <init>(...); }
-keep class * implements com.google.gson.TypeAdapter { <init>(...); }
-keep class * implements com.google.gson.JsonSerializer { <init>(...); }
-keep class * implements com.google.gson.JsonDeserializer { <init>(...); }
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
-keep class **$$Parcelable { <fields>; }
-keepclassmembers class **$$Parcelable { <fields>; }

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
-keep class org.tensorflow.lite.Interpreter { <fields>; <init>(...); }
-keepclassmembers class org.tensorflow.lite.Interpreter {
    public <methods>;
    public <fields>;
}
-keep class org.tensorflow.lite.support.common.FileUtil { <fields>; <init>(...); }
-keepclassmembers class org.tensorflow.lite.support.common.FileUtil {
    public <methods>;
}
-keep class org.tensorflow.lite.support.common.TensorOperator { <fields>; <init>(...); }
-keepclassmembers class org.tensorflow.lite.support.common.TensorOperator {
    public <methods>;
}
-keep class org.tensorflow.lite.gpu.** { <fields>; <init>(...); }
-dontwarn org.tensorflow.**

# ========================================
# ML Kit 人脸检测相关规则
# ========================================
-keep class com.google.mlkit.** { <fields>; <init>(...); }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.**

# ========================================
# Ktor 客户端相关规则
# ========================================
-keep class io.ktor.** { <fields>; <init>(...); }
-keepclassmembers class io.ktor.** {
    public <methods>;
    public <fields>;
}
-dontwarn io.ktor.**
-dontwarn java.lang.management.**

# ========================================
# Coil 图片加载
# ========================================
-keep class coil.** { <fields>; <init>(...); }
-dontwarn coil.**

# ========================================
# SLF4J 日志框架
# ========================================
-dontwarn org.slf4j.**
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
-keep class com.silas.omaster.data.local.SettingsManager { <fields>; <init>(...); }
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

# ===== 水印系统 =====
-keep class com.silas.omaster.watermark.WatermarkLayerSystem { <fields>; <init>(...); }
-keep class com.silas.omaster.watermark.ExifWatermarkProvider { <fields>; <init>(...); }

# ===== 云同步 =====
-keep class com.silas.omaster.cloud.CloudSyncManager { <fields>; <init>(...); }

# ===== GPU 渲染器 =====
-keep class com.silas.omaster.renderer.GPURenderManager { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.ShaderProgram { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.ImageShaderRenderer { <fields>; <init>(...); }
-keep class com.silas.omaster.renderer.RenderParameters { <fields>; }

# ===== 参数系统 =====
-keep class com.silas.omaster.param.ParamAdjustmentManager { <fields>; <init>(...); }

# ===== 工具类 =====
-keep class com.silas.omaster.util.JsonUtil { <methods>; <init>(...); }
-keep class com.silas.omaster.util.SecurityCrypto { <methods>; <init>(...); }

# ===== ViewModel =====
-keep class com.silas.omaster.ui.home.HomeViewModel { <fields>; <init>(...); }
-keepclassmembers class com.silas.omaster.ui.home.HomeViewModel {
    public <methods>;
}
-keep class com.silas.omaster.ui.detail.DetailViewModel { <fields>; <init>(...); }
-keepclassmembers class com.silas.omaster.ui.detail.DetailViewModel {
    public <methods>;
}
-keep class com.silas.omaster.ui.create.UniversalCreatePresetViewModel { <fields>; <init>(...); }
-keepclassmembers class com.silas.omaster.ui.create.UniversalCreatePresetViewModel {
    public <methods>;
}

# ===== Application 和 Activity =====
-keep class com.silas.omaster.OMasterApplication { <fields>; <init>(...); }
-keepclassmembers class com.silas.omaster.OMasterApplication {
    public <methods>;
}
-keep class com.silas.omaster.MainActivity { <fields>; <init>(...); }
-keepclassmembers class com.silas.omaster.MainActivity {
    public <methods>;
}

# ========================================
# 优化配置
# ========================================
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-shrink
-optimize

# ========================================
# 混淆配置
# ========================================
-useuniqueclassmembernames

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ========================================
# 精确 dontwarn
# ========================================
-dontwarn java.lang.invoke.**
