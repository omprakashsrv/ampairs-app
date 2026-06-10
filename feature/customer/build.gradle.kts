plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.customer"
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
                api(projects.feature.customerApi)
                api(projects.feature.fileApi)
                api(projects.feature.file)
                implementation(projects.data.common)
                implementation(projects.data.sync)
                implementation(libs.kermit)
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
                implementation(projects.feature.formApi)
                implementation(projects.feature.form)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                // FileKit for cross-platform file picking
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                // Navigation 3 for NavKey
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
                // Coil for image loading
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
                // Material3 Adaptive for WindowSizeClass
                implementation(libs.material3.adaptive)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okHttp)
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
                implementation(libs.ktor.client.okHttp)
                implementation(files("$rootDir/libs/jmapviewer-2.24.jar"))
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

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generateComposeResClass") })
    dependsOn(tasks.matching { it.name.startsWith("generateResourceAccessorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateActualResourceCollectorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateExpectResourceCollectorsFor") })
}
