import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleServices)
    // Firebase Crashlytics + Performance SDKs are pulled in transitively via feature:auth. The
    // Crashlytics SDK hard-crashes at startup (missing build ID) unless its Gradle plugin is applied.
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProperties.load(it) }

// ── Per-client selection ──────────────────────────────────────────────────────
// One app module builds every white-label client. Pick the client at build time:
//   ./gradlew :clientApp:assembleDebug            (defaults to 'ambika')
//   ./gradlew :clientApp:bundleRelease -Pclient=ambika
// Everything client-specific lives in clients/<id>/ (config.properties + res/ icons); this module
// and :shared-ecom are 100% shared. Scales to N clients with no new modules/flavors — CI just loops
// over clients/*/ and uploads each AAB to its own Play listing.
val clientId = (findProperty("client") as String?) ?: "ambika"
val clientDir = rootProject.file("clients/$clientId")
require(clientDir.isDirectory) {
    "Unknown client '$clientId'. Expected a config dir at ${clientDir.relativeTo(rootDir)}."
}
val clientConfig = Properties().apply {
    clientDir.resolve("config.properties").inputStream().use { load(it) }
}
fun client(key: String): String = clientConfig.getProperty(key)
    ?: error("clients/$clientId/config.properties is missing required key '$key'")

android {
    // namespace = where R/BuildConfig + relative manifest names resolve. Fixed & generic; the
    // installed package (applicationId) is what varies per client.
    namespace = "com.ampairs.clientapp"
    compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/versions/*"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
        jniLibs {
            // feature:form transitively drags in feature:agent (on-device LLM/STT). The customer
            // ecom app never opens the agent, so R8 strips its code — but native .so libs aren't
            // code-shrunk. Drop the ~120 MB of AI runtimes that are never loaded here.
            excludes += listOf(
                "**/liblitertlm_jni.so",
                "**/libLiteRt.so",
                "**/libLiteRtClGlAccelerator.so",
                "**/libwhisper_jni.so",
                "**/libvosk.so",
            )
        }
    }

    defaultConfig {
        applicationId = client("applicationId")
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = client("versionCode").toInt()
        versionName = client("versionName")

        // Consumed by AndroidManifest (android:label) and StorefrontRoot (workspace + brand color).
        manifestPlaceholders["appName"] = client("appName")
        buildConfigField("String", "WORKSPACE_SLUG", "\"${client("workspaceSlug")}\"")
        // Brand seed color as an ARGB long (e.g. 0xFF1B6C4A) → Compose Color in ClientMainActivity.
        buildConfigField("long", "THEME_COLOR_ARGB", "${client("themeColorArgb")}L")
    }

    // Merge the selected client's icons/overrides on top of the shared res.
    sourceSets["main"].res.srcDirs("src/main/res", rootProject.file("clients/$clientId/res"))

    signingConfigs {
        val release by creating {
            storeFile = file("$rootDir/ampairs.jks")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                ?: System.getenv("KEYSTORE_PASSWORD")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
                ?: System.getenv("KEY_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                ?: System.getenv("KEY_ALIAS")
                ?: "ampairs"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.22:8080\"")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            signingConfig = signingConfigs["release"]
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.ampairs.in\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"]
        }
    }
}

dependencies {
    // Slim shared layer (login + ecom storefront/order + workspace settings)
    implementation(projects.sharedEcom)

    // Android Activity + Compose integration
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    // Metro
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel.compose)

    // Coil image loading (singleton loader factory)
    implementation(libs.coil.core)
    implementation(libs.coil.compose)

    // FileKit (image/contact picking used by ecom address + auth profile screens)
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)

    // Ktor OkHttp engine for Android
    implementation(libs.ktor.client.okHttp)
}
