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

# 保留行号信息用于崩溃日志
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留属性用于序列化
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# 保留所有枚举的 values 和 valueOf 方法
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Serializable 的特殊成员
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留 Compose 相关类
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# 保留 Gson 相关
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# 保留泛型签名（Gson 使用反射获取泛型类型）
-keepattributes Signature
# 保留所有使用 @SerializedName 注解的字段
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留 ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keep class * implements java.io.Serializable { *; }

# 保留友盟 SDK
-keep class com.umeng.** { *; }
-dontwarn com.umeng.**
-keep class com.umeng.analytics.** { *; }
-keep class com.umeng.commonsdk.** { *; }

# 保留 Coil 图片加载
-keep class coil.** { *; }
-dontwarn coil.**

# 保留导航序列化
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
# 保留 @Serializable 类的序列化器
-keepclassmembers class **$$serializer { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
    @kotlinx.serialization.SerialName <fields>;
}

# 保留数据模型
-keep class com.silas.omaster.model.** { *; }
-keep class com.silas.omaster.data.model.** { *; }

# 保留Manager类（包含StateFlow和SharedPreferences操作）
-keep class com.silas.omaster.ai.** { *; }
-keep class com.silas.omaster.cloud.** { *; }
-keep class com.silas.omaster.watermark.** { *; }
-keep class com.silas.omaster.param.** { *; }
-keep class com.silas.omaster.data.** { *; }
-keep class com.silas.omaster.scene.** { *; }
-keep class com.silas.omaster.lut.** { *; }
-keep class com.silas.omaster.vignette.** { *; }
-keep class com.silas.omaster.mask.** { *; }
-keep class com.silas.omaster.tflite.** { *; }
-keep class com.silas.omaster.network.** { *; }
-keep class com.silas.omaster.util.** { *; }
-keep class com.silas.omaster.viewmodel.** { *; }
-keep class com.silas.omaster.renderer.** { *; }
-keep class com.silas.omaster.raw.** { *; }
-keep class com.silas.omaster.image.** { *; }

# 保留UI组件
-keep class com.silas.omaster.ui.** { *; }

# 保留SettingsManager的所有方法
-keepclassmembers class com.silas.omaster.data.local.SettingsManager {
    public *;
    private *;
}

# 保留继承自 Application 的类
-keep public class * extends android.app.Application

# 保留继承自 Activity 的类
-keep public class * extends android.app.Activity
-keep public class * extends androidx.activity.ComponentActivity

# 保留继承自 Service 的类
-keep public class * extends android.app.Service

# 保留继承自 BroadcastReceiver 的类
-keep public class * extends android.content.BroadcastReceiver

# 保留继承自 ContentProvider 的类
-keep public class * extends android.content.ContentProvider

# 保留 R 文件的内部类
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 优化移除未使用的代码
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**

# 混淆优化
-optimizationpasses 5
-allowaccessmodification
-repackageclasses
-mergeinterfacesaggressively
-overloadaggressively

# 保留方法参数名（用于崩溃报告）
-keepattributes MethodParameters

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
# TFLite 自定义操作可能需要的 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

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

# ========================================
# Room 数据库相关规则（未来扩展）
# ========================================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ========================================
# 友盟 + OkHttp 依赖
# ========================================
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ========================================
# ExifInterface
# ========================================
-keep class androidx.exifinterface.** { *; }
-keepclassmembers class androidx.exifinterface.** { *; }

# ========================================
# 应用特定的初始化方法保留
# ========================================
# CrashHandler 静态初始化
-keepclassmembers class com.silas.omaster.util.CrashHandler {
    public static *;
    private static *;
}
# SecurityCrypto
-keepclassmembers class com.silas.omaster.util.SecurityCrypto {
    public static *;
    private static *;
}
# LogUtil
-keepclassmembers class com.silas.omaster.util.LogUtil {
    public static *;
    private static *;
}

# ========================================
# 避免删除注解
# ========================================
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations
-keepattributes AnnotationDefault
