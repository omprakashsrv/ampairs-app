import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
    alias(libs.plugins.metro)
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
    }

    defaultConfig {
        applicationId = "com.ampairs.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 18
        versionName = "1.0.0.18"

        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
    }

    signingConfigs {
        val release by creating {
            storeFile = file("$rootDir/ampairs.jks")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "ampairs")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.0.107:8080\"")
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

dependencies {
    implementation(projects.shared)

    // Feature modules used directly in MainActivity/MainApp
    implementation(projects.data.common)
    implementation(projects.feature.auth)
    implementation(projects.feature.customer)

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
}