import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.sentryPlugin)
}

configurations.all {
    exclude(group = "androidx.compose.ui", module = "ui-test-android")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.ampairs.shared"
        compileSdk { version = release(libs.versions.android.compileSdk.get().toInt()) }
        androidResources.enable = true
    }

    jvm("desktop")

    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Ampairs Compose Multiplatform App"
        version = "1.0.0"
        homepage = "https://ampairs.in"

        ios.deploymentTarget = "16.0"
        framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        pod("FirebaseCore") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("FirebaseAuth") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("FirebaseAnalytics") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("FirebaseCrashlytics") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("FirebasePerformance") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("FirebaseMessaging") {
            version = "~> 11.13"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        pod("Sentry") {
            version = "~> 8.0"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)
                implementation(libs.ktor.client.okHttp)
                implementation(libs.splash.screen)
                implementation(libs.aws.s3)

                // Location and Maps
                implementation(libs.play.services.location)
                implementation(libs.play.services.coroutines)
                implementation(libs.play.services.integrity)
                implementation(libs.play.services.auth)
                implementation(libs.maps.compose)
                implementation(libs.accompanist.permissions)

                // In-app updates
                implementation(libs.app.update)

                // Google Play Billing
                implementation(libs.billing.ktx)

                // Firebase - Native Android SDK
                implementation(libs.firebase.auth)
                implementation(libs.google.firebase.analytics)
                implementation(libs.google.firebase.crashlytics)
                implementation(libs.google.firebase.perf)
                implementation(libs.google.firebase.messaging)
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(projects.data.common)

                // Feature modules
                implementation(projects.feature.auth)
                implementation(projects.feature.agent)
                implementation(projects.feature.aws)
                implementation(projects.feature.form)
                implementation(projects.feature.unit)
                implementation(projects.feature.update)
                implementation(projects.feature.event)
                implementation(projects.feature.tax)
                implementation(projects.feature.subscription)
                implementation(projects.feature.business)
                implementation(projects.feature.product)
                implementation(projects.feature.customer)
                implementation(projects.feature.inventory)
                implementation(projects.feature.order)
                implementation(projects.feature.invoice)
                implementation(projects.feature.workspace)

                implementation(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.animation)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)
                implementation(libs.compose.components.resources)

                implementation(libs.kotlinx.dateTime)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.bundles.ktor.common)

                implementation(libs.paging.common)
                implementation(libs.image.loader)

                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.file.picker)
                implementation(libs.uuid)
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
                implementation(libs.navigation3.ui)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.lifecycle.viewmodel.navigation3)
                implementation(libs.savedstate)
                implementation(libs.savedstate.compose)
                implementation(projects.thirdparty.androidx.paging.compose)

                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.sqlite.bundled)

                implementation(libs.store5)

                implementation(libs.bundles.krossbow)

                implementation(libs.datastore)
                implementation(libs.datastore.preferences)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.koin.core)
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okHttp)
                implementation(libs.aws.s3)
                implementation(libs.jmapviewer)

                implementation(project(":tallyModule"))
            }
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Ampairs"
            packageVersion = "1.0.0"
            description = "Empowering Retail, One byte at a time"
            copyright = "Copyright 2025 Ampairs. All rights reserved."
            vendor = "Ampairs"
            modules("java.sql")

            windows {
                dirChooser = true
                upgradeUuid = "FEEF6607-E845-4EF5-B62B-B7F48D654796"
                shortcut = true
                menu = true
                iconFile.set(rootProject.file("resources/icon.ico"))
                menuGroup = packageName
                perUserInstall = false
            }

            macOS {
                bundleID = "com.ampairs.app"
                packageName = rootProject.name
                iconFile.set(rootProject.file("resources/icon.icns"))

                infoPlist {
                    extraKeysRawXml = """
                        <key>CFBundleURLTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleURLName</key>
                                <string>com.ampairs.auth</string>
                                <key>CFBundleURLSchemes</key>
                                <array>
                                    <string>ampairs</string>
                                </array>
                                <key>CFBundleTypeRole</key>
                                <string>Viewer</string>
                            </dict>
                        </array>
                    """.trimIndent()
                }
            }

            linux {
                iconFile.set(rootProject.file("resources/icon.png"))
            }
        }

        buildTypes.release {
            proguard {
                obfuscate.set(true)
                optimize.set(true)
                configurationFiles.from("compose-desktop.pro")
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspCommonMainMetadata", libs.room.compiler)
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

// Windows MSI code signing — set WINDOWS_SIGN_CERT_FILE and WINDOWS_SIGN_PASSWORD env vars to enable
afterEvaluate {
    tasks.matching { it.name == "packageMsi" || it.name == "packageReleaseMsi" }.configureEach {
        doLast {
            val certFile = System.getenv("WINDOWS_SIGN_CERT_FILE")
            val certPassword = System.getenv("WINDOWS_SIGN_PASSWORD")

            if (certFile != null && certPassword != null) {
                val certPath = File(certFile)
                if (!certPath.exists()) {
                    logger.warn("Certificate file not found: $certFile")
                    return@doLast
                }

                val buildDir = project.layout.buildDirectory.asFile.get()
                val msiFiles = File(buildDir, "compose/binaries/main")
                    .walkTopDown()
                    .filter { it.extension == "msi" }
                    .toList()

                if (msiFiles.isEmpty()) {
                    logger.warn("No MSI file found in build output")
                    return@doLast
                }

                val msiFile = msiFiles.first()
                logger.lifecycle("Signing MSI installer: ${msiFile.name}")

                val exitCode = ProcessBuilder(
                    "signtool.exe", "sign",
                    "/f", certFile, "/p", certPassword,
                    "/fd", "SHA256",
                    "/tr", "http://timestamp.digicert.com",
                    "/td", "SHA256",
                    "/d", "Ampairs Desktop Application",
                    msiFile.absolutePath
                )
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor()

                if (exitCode == 0) {
                    logger.lifecycle("MSI signed successfully: ${msiFile.name}")
                } else {
                    logger.error("MSI signing failed (exit $exitCode) — ensure signtool.exe is in PATH")
                }
            } else {
                logger.warn("Windows code signing skipped — set WINDOWS_SIGN_CERT_FILE and WINDOWS_SIGN_PASSWORD to enable")
            }
        }
    }
}
