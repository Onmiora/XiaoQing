# Remove Log.d and Log.v in release builds (security: prevent leaking user data)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep Gson model classes used for serialization
-keep class com.onmi.qing.data.remote.** { *; }
-keep class com.onmi.qing.data.AnthropicMessage { *; }
-keep class com.onmi.qing.data.AnthropicRequest { *; }

# Keep Gson TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Preserve line number info for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
