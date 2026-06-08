# OMaster Release ProGuard Rules
# 行业最高水平配置 - 确保功能完整可用

# ===========================================
# 基础配置
# ===========================================

# 保留调试信息（便于排查问题）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ===========================================
# Kotlin & Compose 配置
# ===========================================

# Kotlin 标准库
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin 协程
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# Kotlin 序列化
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepclassmembers class **$$serializer { *; }
-keep class **$$serializer { *; }

# Compose 相关
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# ===========================================
# AndroidX 配置
# ===========================================

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# LiveData
-keep class androidx.lifecycle.LiveData { *; }
-keep class androidx.lifecycle.MutableLiveData { *; }

# Navigation
-keep class androidx.navigation.** { *; }
-keepclassmembers class androidx.navigation.** { *; }

# ===========================================
# 数据模型配置（关键 - 确保JSON序列化正常）
# ===========================================

# 保留所有数据类（用于JSON序列化）
-keep class com.silas.omaster.model.** { *; }
-keepclassmembers class com.silas.omaster.model.** { *; }

# 保留Serializable类
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <methods>;
    !private <methods>;
}

# 保留Parcelable类
-keep class * implements android.os.Parcelable { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ===========================================
# Manager类配置（关键 - 确保业务逻辑正常）
# ===========================================

# AI场景识别
-keep class com.silas.omaster.ai.** { *; }
-keepclassmembers class com.silas.omaster.ai.** { *; }

# 云同步
-keep class com.silas.omaster.cloud.** { *; }
-keepclassmembers class com.silas.omaster.cloud.** { *; }

# 水印编辑
-keep class com.silas.omaster.watermark.** { *; }
-keepclassmembers class com.silas.omaster.watermark.** { *; }

# 参数调节
-keep class com.silas.omaster.param.** { *; }
-keepclassmembers class com.silas.omaster.param.** { *; }

# 数据层
-keep class com.silas.omaster.data.** { *; }
-keepclassmembers class com.silas.omaster.data.** { *; }

# 网络层
-keep class com.silas.omaster.network.** { *; }
-keepclassmembers class com.silas.omaster.network.** { *; }

# 工具类
-keep class com.silas.omaster.util.** { *; }
-keepclassmembers class com.silas.omaster.util.** { *; }

# ===========================================
# UI组件配置
# ===========================================

-keep class com.silas.omaster.ui.** { *; }
-keepclassmembers class com.silas.omaster.ui.** { *; }

# Compose UI函数
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===========================================
# 第三方库配置
# ===========================================

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }

# Coil图片加载
-keep class coil.** { *; }
-dontwarn coil.**

# OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Retrofit（如果使用）
-keep class retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-dontwarn retrofit2.**

# Ktor（如果使用）
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# 友盟SDK
-keep class com.umeng.** { *; }
-keepclassmembers class com.umeng.** { *; }
-dontwarn com.umeng.**

# CameraX 相机库
-keep class androidx.camera.** { *; }
-keepclassmembers class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ===========================================
# 优化配置
# ===========================================

# 优化级别
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively

# 移除未使用的代码
-dontwarn java.lang.invoke.**
-dontwarn sun.misc.**

# 忽略Android不支持类
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
-dontwarn org.slf4j.impl.StaticLoggerBinder

# 保持Native方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持自定义View
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
}

# 保持枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# ===========================================
# 调试配置（Release可保留）
# ===========================================

# 保留BuildConfig（用于版本信息）
-keep class com.silas.omaster.BuildConfig { *; }

# 保留日志调用（便于排查问题）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}