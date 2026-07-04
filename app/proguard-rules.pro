# ===================================================================
# AhSilence — ProGuard / R8 Rules
# ===================================================================

# Preserve source file names and line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable

# Hide original source file names in stack traces
-renamesourcefileattribute SourceFile

# Keep the DSP engine classes (they use @Volatile and reflection-sensitive patterns)
-keep class com.bted.ahsilence.data.engine.** { *; }
-keep class com.bted.ahsilence.core.di.** { *; }

# Keep the domain layer (contracts/interfaces used via DI)
-keep interface com.bted.ahsilence.domain.port.AudioEngine { *; }
-keep class com.bted.ahsilence.domain.model.** { *; }