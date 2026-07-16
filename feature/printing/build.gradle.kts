plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
}

// OpenJFX ships per-OS native jars; pick the classifier for the machine doing the build/run
// (each dev + CI builds for its own OS — Mac dev gets mac-aarch64, CI/Linux gets linux).
fun javafxClassifier(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val arm = arch.contains("aarch64") || arch.contains("arm")
    return when {
        os.contains("mac") -> if (arm) "mac-aarch64" else "mac"
        os.contains("win") -> "win"
        else -> if (arm) "linux-aarch64" else "linux"
    }
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.printing"
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
                api(projects.printing.core)
                implementation(projects.printing.render)
                implementation(projects.printing.transport)
                implementation(projects.data.common)
                implementation(projects.data.database)
                implementation(projects.data.sync)
                implementation(projects.feature.authApi)
                implementation(projects.feature.fileApi)
                // FileKit — pick the .html file for static templates.
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.bundles.ktor.common)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.dateTime)
                implementation(libs.kermit)
                implementation(libs.room.runtime)
                implementation(libs.sqlite.bundled)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.material3.adaptive)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.lifecycle.runtime.compose)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okHttp)
                // Runtime permissions (Bluetooth) — Android only; iOS/Desktop need no app-level grant.
                implementation(libs.grant.core)
                implementation(libs.grant.bluetooth)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
                // JavaFX WebView (WebKit) renders the page-template preview with real CSS + zoom.
                val fxVersion = libs.versions.openjfx.get()
                val fxClassifier = javafxClassifier()
                listOf("base", "graphics", "controls", "media", "web", "swing").forEach { mod ->
                    implementation("org.openjfx:javafx-$mod:$fxVersion:$fxClassifier")
                }
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

