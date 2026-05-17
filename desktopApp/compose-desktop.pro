-keep class org.sqlite.** { *; }
-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn nl.adaptivity.xmlutil.**
-dontwarn org.lwjgl.**

-keep class kotlinx.coroutines.swing.SwingDispatcherFactory {}
-keep class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class io.ktor.serialization.kotlinx.KotlinxSerializationExtension {}
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider {}
-keep class io.ktor.** { *; }


# Kotlin serialization looks up the generated serializer classes through a function on companion
# objects. The companions are looked up reflectively so we need to explicitly keep these functions.
-keepclasseswithmembers class **.*$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}
# If a companion has the serializer function, keep the companion field on the original type so that
# the reflective lookup succeeds.
-if class **.*$Companion {
  kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class <1>.<2> {
  <1>.<2>$Companion Companion;
}

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}

-keep public enum com.ampairs.** {
    *;
}
