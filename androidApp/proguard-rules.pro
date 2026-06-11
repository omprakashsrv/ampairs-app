-dontwarn java.net.http.**

# ============================================================
# Custom Application ProGuard RulesIs
# ============================================================
# NOTE: Firebase SDKs are already ProGuard-friendly and include
# their own keep rules. No need to add explicit Firebase rules.
# See: https://github.com/firebase/firebase-android-sdk

# Keep only your custom wrapper classes that implement callbacks
-keep class com.ampairs.auth.firebase.FirebaseAuthProvider { *; }
-keep class com.ampairs.auth.firebase.FirebaseAuthProvider$* { *; }
-keep class com.ampairs.auth.domain.** { *; }

# ============================================================
# Library keep rules added for the minified release build.
# These libraries rely on reflection/codegen that R8 can strip
# without explicit keeps, causing release-only crashes that never
# show up in debug. Additive — safe to keep.
# ============================================================

# --- kotlinx.serialization -------------------------------------------------
# Generated serializers are looked up reflectively; without these, @Serializable
# models fail at runtime in release only.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.ampairs.**$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class com.ampairs.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Ktor client -----------------------------------------------------------
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**

# --- Room ------------------------------------------------------------------
# Room generates *_Impl classes and references entities/DAOs by name.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# --- Metro DI (dev.zacsweers.metro) ----------------------------------------
# DI graph/factory metadata is reflection/annotation driven.
-keep class dev.zacsweers.metro.** { *; }
-keep @dev.zacsweers.metro.* class * { *; }
-keepclassmembers class * {
    @dev.zacsweers.metro.* <init>(...);
}
-dontwarn dev.zacsweers.metro.**

# --- Navigation3 serializable routes ---------------------------------------
# Routes are @Serializable NavKeys restored from saved state by name.
-keep class androidx.navigation3.** { *; }
-dontwarn androidx.navigation3.**

# --- Keep all @Serializable app models -------------------------------------
-keep @kotlinx.serialization.Serializable class com.ampairs.** { *; }
