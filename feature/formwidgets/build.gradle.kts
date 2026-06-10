plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.formwidgets"
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
                // Form contracts (FormField) + render SPI (CustomFieldWidget, registries)
                api(projects.feature.formApi)
                implementation(projects.feature.form)
                implementation(projects.data.common)
                implementation(libs.kermit)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
            }
        }
        androidMain {
            dependencies {
                // Google Play Services - Location
                implementation(libs.play.services.location)
                // Google Maps Compose
                implementation(libs.maps.compose)
                // Accompanist Permissions
                implementation(libs.accompanist.permissions)
                // Coroutines support for GMS Tasks (.await())
                implementation(libs.play.services.coroutines)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(files("$rootDir/libs/jmapviewer-2.24.jar"))
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}
