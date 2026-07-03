# Client ecom app — release ProGuard rules.
# Metro, Room, Ktor and kotlinx.serialization keep rules are contributed by the library modules.

# Keep @Serializable NavKey routes (reflectively referenced by Navigation3 saved state).
-keep,includedescriptorclasses class com.ampairs.storefront.nav.** { *; }
