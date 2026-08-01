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
        namespace = "com.ampairs.payment"
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
                implementation(projects.data.sync)
                implementation(libs.kotlinx.dateTime)
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
                implementation(projects.feature.customerApi)
                implementation(projects.feature.supplier)
                implementation(projects.feature.store)
                implementation(projects.feature.sequence)
                implementation(projects.feature.invoice)
                implementation(projects.feature.purchase)
                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.sqlite.bundled)
                implementation(libs.paging.common)
                implementation(libs.paging.componse.common)
                // Material3 Adaptive
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
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

