plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    alias(libs.plugins.kover)
}

// OpenJFX ships per-OS native jars; pick the classifier for the machine doing the build/run.
// (Copied from :feature:printing — the desktop image-search WebView uses the same JavaFX WebView.)
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
        namespace = "com.ampairs.imagesearch"
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
                // The picked web image feeds the existing file pipeline (saveLocally + sync).
                api(projects.feature.fileApi)
                implementation(projects.data.common)
                implementation(projects.data.sync)
                implementation(libs.kermit)
                implementation(libs.metro.runtime)
                implementation(libs.metrox.viewmodel.compose)
                implementation(libs.bundles.ktor.common)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                // Compose
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.components.resources)
                implementation(libs.material3.adaptive)
                // Navigation 3 for NavKey (route)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.lifecycle.runtime.compose)
                // Coil for thumbnail rendering
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
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
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okHttp)
                // JavaFX WebView (WebKit) hosts the headless scrape engine on Desktop.
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
