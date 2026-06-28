plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.analytics"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.data.common)
                implementation(projects.data.sync)
                implementation(libs.metro.runtime)
                implementation(libs.kotlinx.dateTime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kermit)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
