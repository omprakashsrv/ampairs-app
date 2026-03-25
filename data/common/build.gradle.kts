plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    androidTarget()
    jvm("desktop")   // must match composeApp — named target
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Compose
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                // Koin
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                // Ktor HTTP client
                implementation(libs.bundles.ktor.common)

                // Room runtime (infrastructure only — no @Database here)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)

                // DataStore preferences
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                // Coroutines & datetime
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)

                // Coil image loading
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                // UUID generation
                implementation(libs.uuid)

                // Haze (glassmorphism effect used in components)
                implementation(libs.haze)

                // Sentry KMP
                implementation(libs.sentry.kmp)

                // Kermit logging
                implementation(libs.kermit)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okHttp)
                // In-app update (InAppUpdateManager)
                implementation(libs.app.update)
                implementation(libs.play.services.coroutines)
            }
        }

        // Desktop — jvm("desktop") creates desktopMain
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
            }
        }

        // iOS — manual wiring required (applyDefaultHierarchyTemplate=false)
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)

            }
        }
    }
}

android {
    namespace = "com.ampairs.data.common"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
