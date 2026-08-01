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
        namespace = "com.ampairs.product"
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
                api(projects.feature.productApi)
                api(projects.feature.fileApi)
                implementation(projects.feature.file)
                implementation(projects.data.common)
                implementation(projects.data.database)
                implementation(projects.data.sync)
                implementation(projects.feature.formApi)
                implementation(projects.feature.form)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.bundles.ktor.common)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(projects.feature.authApi)
                implementation(projects.feature.taxApi)
                implementation(projects.feature.unitApi)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                implementation(libs.paging.common)
                implementation(libs.room.paging)
                implementation(libs.kotlinx.dateTime)
                // Coil for image loading
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
                // Adaptive layout
                implementation(libs.material3.adaptive)
                // Logging
                implementation(libs.kermit)
                // FileKit for cross-platform file picking
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okHttp)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}

