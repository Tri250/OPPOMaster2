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
# ML Kit 规则（精简）
# ========================================
# Face / Text / Object / Segmentation 等模块通过反射创建实例，保留入口
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.mlkit.vision.face.FaceDetector { *; }
-keep class com.google.mlkit.vision.text.TextRecognizer { *; }
-keep class com.google.mlkit.vision.objects.ObjectDetector { *; }
-keep class com.google.mlkit.vision.segmentation.Segmenter { *; }
-keep class com.google.mlkit.common.sdkinternal.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.**

# MediaPipe Tasks（自定义算子）— 通过反射加载 native
-keep class com.google.mediapipe.tasks.** { *; }
-keepclassmembers class com.google.mediapipe.tasks.** { native <methods>; }
-dontwarn com.google.mediapipe.tasks.**

# ========================================
# CameraX / Camera2
# ========================================
-keep class androidx.camera.** { *; }
-keep interface androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { native <methods>; }
-dontwarn androidx.camera.**
-dontwarn com.google.auto.value.**
-keep class com.google.auto.value.** { *; }

# ========================================
# TensorFlow Lite 规则
# ========================================
# Interpreter 与 Support 库通过 JNI 加载，保留类名与 native 方法
-keep class org.tensorflow.lite.Interpreter { *; }
-keep class org.tensorflow.lite.Interpreter$Options { *; }
-keep class org.tensorflow.lite.gpu.GpuDelegate { *; }
-keep class org.tensorflow.lite.support.image.TensorImage { *; }
-keep class org.tensorflow.lite.support.image.ImageProcessor { *; }
-keep class org.tensorflow.lite.support.common.ops.** { *; }
-keepclassmembers class org.tensorflow.lite.** { native <methods>; }
-dontwarn org.tensorflow.lite.**

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
# 修复 v2.3.0 闪退：补全所有包含 @Serializable 类的包
-keep class com.silas.omaster.model.**$$serializer { *; }
-keep class com.silas.omaster.data.model.**$$serializer { *; }
-keep class com.silas.omaster.data.watermark.**$$serializer { *; }
-keep class com.silas.omaster.engine.**$$serializer { *; }
-keep class com.silas.omaster.watermark.**$$serializer { *; }
-keep class com.silas.omaster.cloud.**$$serializer { *; }
-keep class com.silas.omaster.billing.**$$serializer { *; }
-keep class com.silas.omaster.ai.mapping.**$$serializer { *; }
-keep class com.silas.omaster.ai.scene.**$$serializer { *; }
-keep class com.silas.omaster.trailsnap.model.**$$serializer { *; }
# 兜底：捕获未来新增包中的 @Serializable 类
-keep class com.silas.omaster.**$$serializer { *; }

# @Serializable 类的 Companion 对象（提供 serializer() 函数入口）
-keepclassmembers class com.silas.omaster.model.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.data.model.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.data.watermark.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.engine.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.watermark.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.cloud.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.billing.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.ai.mapping.** {
    *** Companion;
}
-keepclassmembers class com.silas.omaster.ai.scene.** {
    *** Companion;
}
# 兜底：所有项目包中的 Companion 对象
-keepclassmembers class com.silas.omaster.** {
    *** Companion;
}

# 保留包含 serializer() 方法的类及其成员（确保序列化字段名不被混淆）
-keepclasseswithmembers class com.silas.omaster.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin Serialization 枚举需要保留（枚举值按名称序列化）
-keepclassmembers enum com.silas.omaster.model.SceneCategory { *; }
-keepclassmembers enum com.silas.omaster.model.SoftLightMode { *; }
-keepclassmembers enum com.silas.omaster.model.FilmSeries { *; }
-keepclassmembers enum com.silas.omaster.engine.RenderQuality { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkLayerType { *; }
-keepclassmembers enum com.silas.omaster.watermark.ContentSource { *; }
-keepclassmembers enum com.silas.omaster.watermark.WatermarkPosition { *; }

# ========================================
# Phase 1 新增模块 ProGuard 规则
# ========================================
# 摄影配方数据模型：Gson 反序列化需要保留无参构造函数和字段
-keepclassmembers class com.silas.omaster.ai.recipe.** {
    <init>(...);
    <fields>;
}
-keep class com.silas.omaster.ai.recipe.EquivalentEquipment { *; }
-keep class com.silas.omaster.ai.recipe.PhoneShootingGuide { *; }
-keep class com.silas.omaster.ai.recipe.HasselbladPresetJson { *; }
-keep class com.silas.omaster.ai.recipe.LUTRecommendation { *; }
-keep class com.silas.omaster.ai.recipe.PhotographyRecipe { *; }

# 用户反馈数据模型：kotlinx.serialization
-keep class com.silas.omaster.feedback.**$$serializer { *; }
-keepclassmembers class com.silas.omaster.feedback.** {
    *** Companion;
}
-keepclasseswithmembers class com.silas.omaster.feedback.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.silas.omaster.feedback.FeedbackEntry { *; }
-keep class com.silas.omaster.feedback.DeviceInfo { *; }

# ========================================
# Gson 序列化模型（本地持久化与跨端传输）
# ========================================
# 以下类使用 Gson 进行本地 JSON 持久化，字段名必须保留以确保反序列化正确
-keep class com.silas.omaster.model.HasselbladParams { *; }
-keep class com.silas.omaster.model.MasterPreset { *; }
-keep class com.silas.omaster.model.PresetList { *; }
-keep class com.silas.omaster.model.PresetDescription { *; }
-keep class com.silas.omaster.model.PresetSection { *; }
-keep class com.silas.omaster.model.PresetItem { *; }
-keep class com.silas.omaster.model.PresetComment { *; }
-keep class com.silas.omaster.data.local.RecipeRecord { *; }

# 反模式检测器：其 AlertLevel 枚举按名称使用
-keepclassmembers enum com.silas.omaster.ai.antipattern.AntiPatternDetector$AlertLevel { *; }

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
# R8 缺失类兜底（第三方库仅编译期/可选依赖引用）
# ========================================
-dontwarn javax.lang.model.SourceVersion
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options$GpuBackend
-dontwarn org.tensorflow.lite.gpu.GpuDelegateFactory$Options

# ========================================
# 精确 dontwarn（替代 -dontwarn **.**）
# ========================================
-dontwarn java.lang.invoke.**

# ========================================
# 行影集模块 ProGuard 规则
# ========================================
# TrailSnap 数据模型（kotlinx.serialization）
-keep class com.silas.omaster.trailsnap.model.**$$serializer { *; }
-keepclassmembers class com.silas.omaster.trailsnap.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.silas.omaster.trailsnap.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# TrailSnap 枚举
-keepclassmembers enum com.silas.omaster.trailsnap.model.** { *; }

# ========================================
# 行影集 ML Kit / MediaStore 相关
# ========================================
# ExifInterface 反射
-keep class androidx.exifinterface.media.ExifInterface { *; }
-dontwarn androidx.exifinterface.**

# ========================================
# 液态玻璃组件 / 动画系统
# ========================================
# Compose 动画相关
-keep class androidx.compose.animation.** { *; }
-dontwarn androidx.compose.animation.**

# ========================================
# 发布验证：关键类不被混淆
# ========================================
# 应用入口类
-keep class com.silas.omaster.OMasterApplication { *; }
-keep class com.silas.omaster.MainActivity { *; }
-keep class com.silas.omaster.InitializationProvider { *; }

# 崩溃处理器（确保堆栈可追溯）
-keep class com.silas.omaster.infrastructure.utils.CrashHandler { *; }
-keep class com.silas.omaster.infrastructure.utils.CrashMonitorManager { *; }

# Sentry 崩溃上报
-keep class io.sentry.** { *; }
-dontwarn io.sentry.**
# 保留 Sentry 需要的行号信息
-keepattributes LineNumberTable,SourceFile
-keep class com.silas.omaster.infrastructure.utils.CrashHandler$CrashListener { *; }

# 安全加密工具
-keep class com.silas.omaster.infrastructure.security.SecurityCrypto { *; }

# 内购管理（防止服务端验证回调被混淆）
-keep class com.silas.omaster.billing.BillingManager { *; }
-keep class com.silas.omaster.billing.SubscriptionState { *; }
-keep class com.silas.omaster.billing.Tier { *; }

# 视频滤镜引擎（v2.3.0）
-keep class com.silas.omaster.video.VideoFilterEngine { *; }
-keep class com.silas.omaster.video.VideoFilterEngine$ProcessProgress { *; }
-keep class com.silas.omaster.video.VideoFilterEngine$ProcessResult { *; }

# 悬浮窗控制器（修复 v2.3.0 闪退：反射访问 isRegistered 字段）
-keep class com.silas.omaster.ui.service.FloatingWindowController { *; }
-keep class com.silas.omaster.ui.service.FloatingWindowService { *; }

# 注意：R8 优化配置已在上方"P2-12 修复"段落统一管理
# 此处不再重复声明，避免 -optimizationpasses / -mergeinterfacesaggressively 冲突

# ========================================
# 资源压缩 keep 规则
# ========================================
# 以下 -keepresources / -keepassets 为 ProGuard 选项，R8 不支持，故移除。
# 若需保留资源，请在 build.gradle 的 res/raw / resValues 等配置中处理。

# ========================================
# AGP 生成的缺失类兜底规则
# ========================================
# 这些 javax.lang.model 类仅在注解处理器中使用，不会到达运行时。
-dontwarn javax.lang.model.element.AnnotationMirror
-dontwarn javax.lang.model.element.AnnotationValue
-dontwarn javax.lang.model.element.AnnotationValueVisitor
-dontwarn javax.lang.model.element.ElementVisitor
-dontwarn javax.lang.model.element.ExecutableElement
-dontwarn javax.lang.model.element.Name
-dontwarn javax.lang.model.element.PackageElement
-dontwarn javax.lang.model.element.TypeElement
-dontwarn javax.lang.model.element.TypeParameterElement
-dontwarn javax.lang.model.element.VariableElement
-dontwarn javax.lang.model.type.ArrayType
-dontwarn javax.lang.model.type.DeclaredType
-dontwarn javax.lang.model.type.ErrorType
-dontwarn javax.lang.model.type.ExecutableType
-dontwarn javax.lang.model.type.PrimitiveType
-dontwarn javax.lang.model.type.TypeKind
-dontwarn javax.lang.model.type.TypeVariable
-dontwarn javax.lang.model.type.WildcardType
-dontwarn javax.lang.model.util.AbstractElementVisitor8
-dontwarn javax.lang.model.util.ElementFilter
-dontwarn javax.lang.model.util.Elements
-dontwarn javax.lang.model.util.SimpleAnnotationValueVisitor8
-dontwarn javax.lang.model.util.SimpleElementVisitor8
-dontwarn javax.lang.model.util.Types
