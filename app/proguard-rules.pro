# ==============================================================================
# Srutam ProGuard / R8 Rules for Production Release Obfuscation
# ==============================================================================

# Preserve line numbers and source attributes for useful crash stack traces in Play Console
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# 1. Native JNI & Sherpa-ONNX Voice Recognition Engine
# ------------------------------------------------------------------------------
# Preserve all JNI method signatures across classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve the Sherpa-ONNX package completely (wrappers, configs, native bindings)
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

# ------------------------------------------------------------------------------
# 2. Gson Serialization / Deserialization & Data Models
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Preserve all Srutam domain and persistence data models
-keep class space.iamjustkrishna.srutam.data.model.** { *; }
-keep class space.iamjustkrishna.srutam.data.entity.** { *; }
-keep class space.iamjustkrishna.srutam.data.local.** { *; }
-keep class space.iamjustkrishna.srutam.utils.ActionItemParseResult { *; }

# ------------------------------------------------------------------------------
# 3. Room Database & KSP
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public abstract *;
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 4. WorkManager Workers (Instantiated via Reflection)
# ------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ------------------------------------------------------------------------------
# 5. Google AI / Gemini Generative AI SDK
# ------------------------------------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ------------------------------------------------------------------------------
# 6. AndroidX Fragment & Navigation
# ------------------------------------------------------------------------------
-keep class androidx.fragment.app.** { *; }
-dontwarn androidx.fragment.app.**