plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
}

// OpenJFX ships per-OS native jars; pick the classifier for the machine doing the build/run.
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
        namespace = "com.ampairs.printing.transport"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.printing.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.ktor.network)
                implementation(libs.kermit)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val desktopMain by getting {
            dependencies {
                // JavaFX WebView prints PAGE templates so the desktop printout matches the preview.
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
        }
    }
}
