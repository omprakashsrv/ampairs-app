plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.firebasePerf)
}

kotlin {
    jvmToolchain(21)
}

val localProperties = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

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
        versionCode = 17
        versionName = "1.0.0.17"

        buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.11:8080\"")
        buildConfigField("String", "ENVIRONMENT", "\"dev\"")
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
    }

    signingConfigs {
        val release by creating {
            storeFile = file("$rootDir/ampairs.jks")
            storePassword = "SKFNNFJ234329898g723g47823gr8"
            keyPassword = "SKFNNFJ234329898g723g47823gr8"
            keyAlias = "ampairs"
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.11:8080\"")
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

    // Compose Material3 (AlertDialog, Text, TextButton in MainActivity)
    implementation(libs.compose.material3)

    // Koin Android
    implementation(libs.koin.android)
    implementation(libs.koin.core)

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