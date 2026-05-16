package com.ampairs.update.service

import com.ampairs.update.domain.UpdateInfo
import java.awt.Desktop
import java.io.File

/**
 * Desktop implementation for opening installers
 *
 * Platform-specific behavior:
 * - macOS: Opens .dmg with system default application
 * - Windows: Opens .msi or .exe with system default application
 * - Linux: Opens .deb with system default application or shows file manager
 */
actual fun openInstallerImpl(filePath: String, updateInfo: UpdateInfo): Boolean {
    val file = File(filePath)

    if (!file.exists()) {
        println("❌ Installer file not found: $filePath")
        return false
    }

    val osName = System.getProperty("os.name").lowercase()

    return try {
        when {
            osName.contains("mac") || osName.contains("darwin") -> {
                // macOS: Use 'open' command to mount DMG
                println("🍎 Opening macOS installer...")
                val process = ProcessBuilder("open", filePath).start()
                process.waitFor()
                println("   DMG mounted. Please drag app to Applications folder.")
                true
            }

            osName.contains("win") -> {
                // Windows: Open MSI or EXE installer
                println("🪟 Opening Windows installer...")
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                    println("   Installer launched. Follow the installation wizard.")
                    true
                } else {
                    // Fallback: Use Runtime.exec
                    val process = ProcessBuilder("cmd", "/c", "start", "", filePath).start()
                    process.waitFor()
                    true
                }
            }

            osName.contains("nux") || osName.contains("nix") -> {
                // Linux: Try to open with package manager or file manager
                println("🐧 Opening Linux installer...")

                // Try different package managers
                val commands = listOf(
                    listOf("xdg-open", filePath),  // Generic file opener
                    listOf("gdebi-gtk", filePath),  // Debian GUI package installer
                    listOf("gnome-software", "--local-filename=$filePath"),  // GNOME Software
                    listOf("nemo", filePath),  // File manager fallback
                    listOf("nautilus", filePath),  // File manager fallback
                )

                for (command in commands) {
                    try {
                        val process = ProcessBuilder(command).start()
                        val exitCode = process.waitFor()
                        if (exitCode == 0) {
                            println("   Installer opened with: ${command.first()}")
                            return true
                        }
                    } catch (e: Exception) {
                        // Try next command
                        continue
                    }
                }

                // If all else fails, open containing directory
                println("   Could not open installer directly.")
                println("   Opening file location: ${file.parent}")
                val parentDir = file.parentFile
                if (parentDir.exists()) {
                    Desktop.getDesktop().open(parentDir)
                    println("   Please install manually: ${file.name}")
                    true
                } else {
                    false
                }
            }

            else -> {
                // Unknown platform: Try Desktop API
                println("❓ Unknown platform. Attempting to open installer...")
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file)
                    true
                } else {
                    false
                }
            }
        }
    } catch (e: Exception) {
        println("❌ Failed to open installer: ${e.message}")
        e.printStackTrace()

        // Last resort: Show file location
        try {
            val parentDir = file.parentFile
            if (parentDir.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(parentDir)
                println("   Opened file location. Please install manually.")
                return true
            }
        } catch (fallbackException: Exception) {
            println("❌ All methods failed: ${fallbackException.message}")
        }

        false
    }
}
