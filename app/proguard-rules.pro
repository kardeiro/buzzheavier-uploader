# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data models
-keep class com.buzzheavier.uploader.data.** { *; }

# Compose
-keep class androidx.compose.animation.core.KeyframesSpec { *; }
-keep class androidx.compose.animation.core.KeyframesSpec$KeyframeEntity { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
