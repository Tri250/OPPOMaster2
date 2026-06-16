# Consumer ProGuard Rules
# 这些规则会被依赖此库的应用自动应用

# 保留公开 API（仅保留必要的公开接口）
-keep public class com.silas.omaster.model.** {
    public *;
}

# 保留注解
-keepattributes *Annotation*

# 保留泛型签名
-keepattributes Signature

# 保留异常
-keepattributes Exceptions

# 保留内部类
-keepattributes InnerClasses

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
