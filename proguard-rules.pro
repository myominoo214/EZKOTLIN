# ProGuard configuration for YOTTA Ledger
# Comprehensive obfuscation and protection against reverse engineering

# Keep main class
-keep class MainKt {
    public static void main(java.lang.String[]);
}

# Keep Compose runtime classes
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# Keep Kotlin serialization
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * {
    *;
}

# Keep Ktor client classes
-keep class io.ktor.** { *; }

# Keep SLF4J logging
-keep class org.slf4j.** { *; }

# Aggressive obfuscation settings
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# String encryption
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# Control flow obfuscation
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively

# Anti-debugging and anti-tampering
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remove debug information
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable classes
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Application-specific keeps (customize based on your app structure)
-keep class com.example.** {
    public <methods>;
    public <fields>;
}

# Keep data classes and their properties
-keep class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Advanced protection
-printmapping mapping.txt
-printseeds seeds.txt
-printusage usage.txt

# Anti-reflection protection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Kotlin metadata protection
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# Additional security measures
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-forceprocessing

# Resource protection
-keepdirectories
-keeppackagenames

# Final optimization
-dontpreverify
-verbose