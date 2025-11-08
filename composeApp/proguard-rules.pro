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
