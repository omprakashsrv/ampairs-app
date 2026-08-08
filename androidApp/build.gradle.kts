import com.github.triplet.gradle.androidpublisher.ReleaseStatus
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
    alias(libs.plugins.metro)
    alias(libs.plugins.playPublisher)
}

kotlin {
    jvmToolchain(21)
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { localProperties.load(it) }

android {
    namespace = "com.ampairs.app"
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
            // Both litert and litertlm-android bundle their own copy of libLiteRt.so.
            // They are the same LiteRT runtime, so keep the first occurrence.
            pickFirsts += "**/libLiteRt.so"
        }
    }

    defaultConfig {
        applicationId = "com.ampairs.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 100024
        versionName = "1.0.24"

        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
    }

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

            ndk {
                debugSymbolLevel = "FULL"
            }
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

            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
}

// ── Play Console publishing (gradle-play-publisher) ────────────────────────
// Same service account as clientApp, granted access to this app (com.ampairs.app) separately
// in Play Console (Setup > API access). Credentials file is shared, gitignored, repo-root.
//   ./gradlew :androidApp:publishBundle                       (internal track)
//   ./gradlew :androidApp:publishBundle -PplayTrack=production (draft — requires a manual
//     "Start rollout" click in Play Console; never auto-completes production)
play {
    serviceAccountCredentials.set(rootProject.file("play-service-account.json"))
    val trackName = (findProperty("playTrack") as String?) ?: "internal"
    track.set(trackName)
    defaultToAppBundles.set(true)
    releaseStatus.set(if (trackName == "production") ReleaseStatus.DRAFT else ReleaseStatus.COMPLETED)
}

dependencies {
    implementation(projects.shared)

    // Feature modules used directly in MainActivity/MainApp
    implementation(projects.data.common)
    implementation(projects.feature.auth)
    implementation(projects.feature.customer)
    implementation(projects.feature.formwidgets)

    // Android Activity + Compose integration
    implementation(libs.androidx.activity.compose)

    // Metro
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel.compose)

    // Coil image loading
    implementation(libs.coil.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // FileKit
    implementation(libs.filekit.core)
    implementation(libs.filekit.dialogs)

    // In-app updates + coroutines-play-services (for tasks.await in InAppUpdateManager)
    implementation(libs.app.update)
    implementation(libs.play.services.coroutines)

    // Ktor OkHttp engine for Android
    implementation(libs.ktor.client.okHttp)

    // FCM push notifications: AmpairsFirebaseMessagingService + NotificationCompat.
    // `shared` depends on firebase-messaging via `implementation` (not `api`), so the service
    // base class isn't visible transitively — declare it directly here.
    implementation(libs.google.firebase.messaging)
    implementation(libs.androidx.core)
}