import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.googleServices) // Firebase Google Services plugin
    alias(libs.plugins.firebaseCrashlytics) // Firebase Crashlytics plugin
    alias(libs.plugins.firebasePerf) // Firebase Performance Monitoring plugin
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.sentryPlugin) // Sentry error tracking
}

configurations.all {
    exclude(group = "androidx.compose.ui", module = "ui-test-android")
}


kotlin {
    androidTarget()

    jvm("desktop")

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Ampairs Compose Multiplatform App"
        version = "1.0.0" // 👈 Podspec version
        homepage = "https://ampairs.in"

        ios.deploymentTarget = "16.0"
        framework {
            baseName = "ComposeApp"
            isStatic = true
        }
//        podfile = project.file("../iosApp/Podfile")

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
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.animation)
                implementation(compose.materialIconsExtended)
                implementation(compose.material3)
                implementation(compose.components.resources)

                implementation(libs.kotlinx.dateTime)
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
                implementation(libs.koin.compose.viewmodel)

                implementation(libs.bundles.ktor.common)

                implementation(libs.paging.common)
                implementation(libs.image.loader)

                // Coil for efficient image loading and caching
                implementation(libs.coil.core)
                implementation(libs.coil.compose)
                implementation(libs.coil.network)

                // FileKit for platform-specific file picking
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.file.picker)
                implementation(libs.uuid)
                implementation(libs.material3.adaptive)
                implementation(libs.material3.adaptive.layout)
                implementation(libs.material3.adaptive.navigation)
                implementation(libs.navigation.compose)
                implementation(libs.lifecycle.viewmodel)
                implementation(libs.savedstate)
                implementation(libs.savedstate.compose)
                implementation(projects.thirdparty.androidx.paging.compose)

                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.sqlite.bundled)

                // Store5 for offline-first caching
                implementation(libs.store5)

                // Krossbow for WebSocket/STOMP support
                implementation(libs.bundles.krossbow)

                // DataStore for preferences
                implementation(libs.datastore)
                implementation(libs.datastore.preferences)

                // kotlinx-datetime for cross-platform time handling
                implementation(libs.kotlinx.dateTime)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.koin.core)
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.okHttp)
                implementation(libs.aws.s3)
                // Library for OpenStreetMap - now using JOSM repository
                implementation(libs.jmapviewer)

                implementation(project(":tallyModule"))
            }
        }


        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
                // Note: Using Room database now, no longer need SQLDelight
            }
        }
    }
}

android {

    compileSdk = libs.versions.android.compileSdk.get().toInt()
    namespace = "com.ampairs.app"

//    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//    sourceSets["main"].res.srcDirs("src/androidMain/res")
//    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/versions/*"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }

    defaultConfig {
        applicationId = "com.ampairs.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 9
        versionName = "1.0.0.9"

        // Environment configuration
        buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.3:8080\"")
        buildConfigField("String", "ENVIRONMENT", "\"dev\"")
    }

    signingConfigs {
        val release by creating {
            storeFile = file("$rootDir/ampairs.jks")
            storePassword = "SKFNNFJ234329898g723g47823gr8"
            keyPassword = "SKFNNFJ234329898g723g47823gr8"
            keyAlias = "ampairs"
        }
    }
    buildTypes {
        val debug by getting {
            buildConfigField("String", "API_BASE_URL", "\"http://10.50.51.3:8080\"")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            signingConfig = signingConfigs["release"]
        }
        val release by getting {
            buildConfigField("String", "API_BASE_URL", "\"https://api.ampairs.in\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"]
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

                // URL Protocol (Deep Link) Registration for Windows
                // This registers the ampairs:// custom URL scheme handler
                perUserInstall = false // Install for all users to register protocol
            }

            macOS {
                bundleID = "com.ampairs.app"
                packageName = rootProject.name
                iconFile.set(rootProject.file("resources/icon.icns"))

                // URL Scheme Configuration via Info.plist
                // Creates custom Info.plist with CFBundleURLTypes for ampairs:// scheme
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

                // Linux .desktop file will include URL scheme via MimeType
                // Format: x-scheme-handler/ampairs
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

    // Android
    add("kspAndroid", libs.room.compiler)

    // Desktop JVM
    add("kspDesktop", libs.room.compiler)

    // iOS targets
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// Fix KSP and Compose resource generation dependency issues
tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    dependsOn(tasks.matching { it.name.startsWith("generateComposeResClass") })
    dependsOn(tasks.matching { it.name.startsWith("generateResourceAccessorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateActualResourceCollectorsFor") })
    dependsOn(tasks.matching { it.name.startsWith("generateExpectResourceCollectorsFor") })
}

// ============================================================================
// Windows Code Signing Task (prevents "Unknown Publisher" warning)
// ============================================================================
//
// Usage:
//   1. Purchase a code signing certificate from a trusted CA (e.g., DigiCert, Sectigo)
//   2. Export as .pfx or .p12 file with private key
//   3. Set environment variables:
//      - WINDOWS_SIGN_CERT_FILE: Path to .pfx/.p12 certificate file
//      - WINDOWS_SIGN_PASSWORD: Certificate password
//   4. Ensure signtool.exe is in PATH (comes with Windows SDK)
//   5. Run: ./gradlew packageMsi
//
// The MSI will be automatically signed after packaging completes.
// ============================================================================

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

                // Find the MSI file in the build output directory
                // Use project.buildDir instead of layout.buildDirectory for config cache compatibility
                val buildDir = project.layout.buildDirectory.asFile.get()
                val msiDir = File(buildDir, "compose/binaries/main")
                val msiFiles = msiDir.walkTopDown()
                    .filter { it.extension == "msi" }
                    .toList()

                if (msiFiles.isEmpty()) {
                    logger.warn("No MSI file found in build output: ${msiDir.absolutePath}")
                    return@doLast
                }

                val msiFile = msiFiles.first()
                logger.lifecycle("Signing MSI installer: ${msiFile.name}")

                // Sign using signtool.exe (Windows SDK required)
                // /f: certificate file
                // /p: password
                // /fd: file digest algorithm (SHA256)
                // /tr: timestamp server URL (ensures signature validity after cert expires)
                // /td: timestamp digest algorithm (SHA256)
                // /d: description
                val signCommand = listOf(
                    "signtool.exe",
                    "sign",
                    "/f", certFile,
                    "/p", certPassword,
                    "/fd", "SHA256",
                    "/tr", "http://timestamp.digicert.com",
                    "/td", "SHA256",
                    "/d", "Ampairs Desktop Application",
                    msiFile.absolutePath
                )

                try {
                    val process = ProcessBuilder(signCommand)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start()

                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        logger.lifecycle("✅ MSI signed successfully: ${msiFile.name}")
                    } else {
                        logger.error("❌ MSI signing failed with exit code: $exitCode")
                        logger.error("Make sure signtool.exe is in PATH (install Windows SDK)")
                    }
                } catch (e: Exception) {
                    logger.error("❌ Failed to sign MSI: ${e.message}")
                    logger.error("Make sure signtool.exe is available in PATH")
                    logger.error("Install Windows SDK: https://developer.microsoft.com/en-us/windows/downloads/windows-sdk/")
                }
            } else {
                logger.warn("⚠️  Windows code signing skipped")
                logger.warn("Set WINDOWS_SIGN_CERT_FILE and WINDOWS_SIGN_PASSWORD to enable signing")
                logger.warn("MSI will show 'Unknown Publisher' warning during installation")
            }
        }
    }
}