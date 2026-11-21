plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
    `maven-publish`
}

group = "com.ampairs"
version = "1.0.0"

// group="com.ampairs" makes Kotlin derive the default module name as "com.ampairs:event"
// (colon-separated), which androidApp:buildReleasePreBundle rejects with "Entry name contains
// invalid characters: root/META-INF/com.ampairs:event.kotlin_module". Force a safe name.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.moduleName.set("ampairs-event")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.data.event"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
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
                implementation(libs.bundles.ktor.common)
                implementation(libs.bundles.krossbow)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kermit)
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/omprakashsrv/ampairs-app")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
