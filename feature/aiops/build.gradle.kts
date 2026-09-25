plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.aiops"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources.enable = true
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                implementation(projects.data.database)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                // Compose (for the inline suggestion/undo UI added in a later slice)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.lifecycle.runtime.compose)
                // Navigation 3 for NavKey (settings route)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }
    }
}
