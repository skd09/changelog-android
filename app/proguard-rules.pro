# ── The Changelog — ProGuard Rules ──────────────────────────────────────────

# Keep line numbers for crash reports (but hide source file names)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlinx Serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class **$$serializer {
    *** INSTANCE;
}

-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Ktor ─────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# ── Firebase ─────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ── Google Mobile Ads ────────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

# ── Google Play Core (in-app review) ─────────────────────────────────────────
-keep class com.google.android.play.core.** { *; }

# ── Coil (image loading) ────────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── Data classes used in API responses (keep field names for JSON) ───────────
-keep class com.sharvari.changelog.data.response.** { *; }
-keep class com.sharvari.changelog.data.request.** { *; }
-keep class com.sharvari.changelog.model.** { *; }

# ── Obfuscate everything else ────────────────────────────────────────────────
# R8 will rename all other classes, methods, and fields
# making reverse engineering significantly harder

# ── Remove logging in release ────────────────────────────────────────────────
-assumenosideeffects class timber.log.Timber* {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ── Strip debug info from third-party libs ───────────────────────────────────
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
