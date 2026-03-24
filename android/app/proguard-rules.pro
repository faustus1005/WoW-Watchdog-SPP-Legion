# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.wowwatchdog.app.data.model.** { *; }
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes EnclosingMethod
