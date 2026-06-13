# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ========================================
# 基础配置
# ========================================
# 保留调试信息（便于排查崩溃）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解和签名（Gson/序列化需要）
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ========================================
# Kotlin 基础规则
# ========================================
-keep class kotlin.Metadata { *; }
# Kotlin 标准库：只保留必要元数据，不过度保留
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Kotlin 协程
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
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
# Compose 编译器会自动处理大部分保留，只需保留关键类
-keep class androidx.compose.runtime.Composable { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**

# Composable 函数
-keep class **$$Composable { *; }
-keep class **$Composable { *; }
-keep class **$composer { *; }
-keep class **$Composer { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembers class **$composer { *; }
-keepclassmembers class **$Composer { *; }
-dontwarn **$$Composable
-dontwarn **$Composable

# ========================================
# AndroidX 基础库
# ========================================
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
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
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keep class **$$Parcelable { *; }
-keepclassmembers class **$$Parcelable { *; }

-keep class * implements java.io.Serializable { *; }
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
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-keepclassmembers class org.tensorflow.lite.Interpreter { *; }
-keepclassmembers class org.tensorflow.lite.support.common.FileUtil { *; }
-keepclassmembers class org.tensorflow.lite.support.common.TensorOperator { *; }
-dontwarn org.tensorflow.**

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
-keep class kotlinx.serialization.json.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }
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

# ===== 数据模型类（必须保留，用于序列化） =====
# 保留所有 model 包下的类（JSON 序列化需要）
-keep class com.silas.omaster.model.** { *; }
-keepclassmembers class com.silas.omaster.model.** { *; }

# ===== 数据仓库和本地存储（SharedPreferences 序列化） =====
# 只保留序列化相关的类，其他类可以优化
-keep class com.silas.omaster.data.repository.Preset { *; }
-keep class com.silas.omaster.data.repository.PresetCategory { *; }
-keep class com.silas.omaster.data.local.SettingsManager { *; }
-keepclassmembers class com.silas.omaster.data.local.SettingsManager { *; }
-keep class com.silas.omaster.data.local.CustomPresetManager { *; }
-keepclassmembers class com.silas.omaster.data.local.CustomPresetManager { *; }
-keep class com.silas.omaster.data.local.RecipeHistoryManager { *; }
-keepclassmembers class com.silas.omaster.data.local.RecipeHistoryManager { *; }

# ===== AI 和 TFLite 相关（模型推理需要） =====
# 只保留与 TFLite 交互的核心类
-keep class com.silas.omaster.ai.AIEngine { *; }
-keepclassmembers class com.silas.omaster.ai.AIEngine { *; }
-keep class com.silas.omaster.ai.MasterInsightEngine { *; }
-keepclassmembers class com.silas.omaster.ai.MasterInsightEngine { *; }
-keep class com.silas.omaster.ai.SceneClassifier { *; }
-keepclassmembers class com.silas.omaster.ai.SceneClassifier { *; }
-keep class com.silas.omaster.ai.QualityAnalyzer { *; }
-keepclassmembers class com.silas.omaster.ai.QualityAnalyzer { *; }
-keep class com.silas.omaster.ai.ParamPredictor { *; }
-keepclassmembers class com.silas.omaster.ai.ParamPredictor { *; }
-keep class com.silas.omaster.tflite.TFLiteEngine { *; }
-keepclassmembers class com.silas.omaster.tflite.TFLiteEngine { *; }

# ===== 水印系统（JSON 配置序列化） =====
# 只保留配置类，其他类可以优化
-keep class com.silas.omaster.watermark.WatermarkConfig { *; }
-keep class com.silas.omaster.watermark.WatermarkLayer { *; }
-keep class com.silas.omaster.watermark.WatermarkTemplate { *; }
-keep class com.silas.omaster.watermark.HasselbladMasterTemplates { *; }
-keepclassmembers class com.silas.omaster.watermark.HasselbladMasterTemplates { *; }
-keep class com.silas.omaster.watermark.ExifWatermarkProvider { *; }
-keepclassmembers class com.silas.omaster.watermark.ExifWatermarkProvider { *; }

# ===== 云同步（网络请求序列化） =====
-keep class com.silas.omaster.cloud.CloudSyncManager { *; }
-keepclassmembers class com.silas.omaster.cloud.CloudSyncManager { *; }
-keep class com.silas.omaster.cloud.CloudPreset { *; }
-keep class com.silas.omaster.cloud.SyncState { *; }

# ===== GPU 渲染器（OpenGL ES 需要） =====
-keep class com.silas.omaster.renderer.GPURenderManager { *; }
-keepclassmembers class com.silas.omaster.renderer.GPURenderManager { *; }
-keep class com.silas.omaster.renderer.RenderConfig { *; }

# ===== 参数系统（JSON 序列化） =====
-keep class com.silas.omaster.param.RenderParams { *; }
-keep class com.silas.omaster.param.AdjustChannel { *; }

# ===== 场景识别（枚举和配置） =====
-keep class com.silas.omaster.scene.SceneType { *; }
-keep class com.silas.omaster.scene.SceneConfig { *; }

# ===== 工具类（反射使用） =====
-keep class com.silas.omaster.util.JsonUtil { *; }
-keepclassmembers class com.silas.omaster.util.JsonUtil { *; }
-keep class com.silas.omaster.util.SecurityCrypto { *; }
-keepclassmembers class com.silas.omaster.util.SecurityCrypto { *; }

# ===== UI 组件（允许优化，只保留必要的） =====
# Compose UI 组件会被 Compose 规则自动保留
# 这里只保留 ViewModel 和导航相关的类
-keep class com.silas.omaster.ui.home.HomeViewModel { *; }
-keepclassmembers class com.silas.omaster.ui.home.HomeViewModel { *; }
-keep class com.silas.omaster.ui.detail.DetailViewModel { *; }
-keepclassmembers class com.silas.omaster.ui.detail.DetailViewModel { *; }

# ===== Application 和 Activity（AndroidManifest 引用） =====
-keep class com.silas.omaster.OMasterApplication { *; }
-keep class com.silas.omaster.MainActivity { *; }

# ========================================
# 优化配置
# ========================================
# 优化次数（5次通常足够）
-optimizationpasses 5

# 允许访问修饰符优化
-allowaccessmodification

# 积极合并接口
-mergeinterfacesaggressively

# 移除未使用的代码
-shrink
-optimize

# 不警告缺失的类
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**

# ========================================
# 混淆配置
# ========================================
# 使用应用优化
-useuniqueclassmembernames

# 保留外部依赖的类名（避免反射问题）
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ========================================
# 调试配置（发布时可关闭）
# ========================================
# 打印详细信息（调试用，发布时注释掉）
# -printseeds
# -printusage
# -printmapping

# 不忽略所有警告，发布时保持严格