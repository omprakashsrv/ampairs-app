import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)

    // Feature modules used directly in main.kt
    implementation(projects.data.common)
    implementation(projects.feature.auth)
    implementation(projects.feature.workspace)

    // Compose Desktop runtime
    implementation(compose.desktop.currentOs)

    // Metro
    implementation(libs.metro.runtime)
    implementation(libs.metrox.viewmodel.compose)
    implementation(kotlin("reflect"))

    // Coil image loading
    implementation(libs.coil.core)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Ampairs"
            packageVersion = "1.0.15"
            description = "Empowering Retail, One byte at a time"
            copyright = "Copyright 2025 Ampairs. All rights reserved."
            vendor = "Ampairs"
            modules("java.sql", "jdk.unsupported", "java.naming", "jdk.crypto.ec")

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

compose.resources {
    packageOfResClass = "com.ampairs.app.generated.resources"
}

tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
    // Forward -Dampairs.* system properties from the Gradle JVM to the app JVM.
    // Without this, -D flags passed on the command line only reach Gradle, not the app.
    listOf("ampairs.environment", "ampairs.api.baseUrl", "ampairs.web.authUrl").forEach { key ->
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
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
