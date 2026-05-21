package com.ampairs.update.service

import com.ampairs.update.domain.UpdateInfo
import com.ampairs.update.domain.UpdateInstallState
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to install downloaded updates
 *
 * Installation behavior:
 * - Opens the downloaded installer file
 * - Guides user through manual installation
 * - Platform-specific: .dmg (macOS), .msi (Windows), .deb (Linux)
 */
@Inject
class UpdateInstaller {
    private val _installState = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    val installState: StateFlow<UpdateInstallState> = _installState.asStateFlow()

    /**
     * Install update from downloaded file
     *
     * This will:
     * 1. Verify file exists
     * 2. Open installer with platform-specific command
     * 3. Guide user to complete installation
     *
     * @param filePath Path to downloaded update file
     * @param updateInfo Update information
     * @return true if installer opened successfully, false otherwise
     */
    suspend fun installUpdate(filePath: String, updateInfo: UpdateInfo): Boolean {
        println("🔧 Starting update installation...")
        println("   File: $filePath")
        println("   Version: ${updateInfo.version}")

        _installState.value = UpdateInstallState.Installing

        return try {
            val success = openInstaller(filePath, updateInfo)

            if (success) {
                println("✅ Installer opened successfully")
                println("👤 Please follow the installer instructions")
                _installState.value = UpdateInstallState.Installed
                true
            } else {
                println("❌ Failed to open installer")
                _installState.value = UpdateInstallState.Failed("Failed to open installer")
                false
            }
        } catch (e: Exception) {
            println("❌ Installation error: ${e.message}")
            _installState.value = UpdateInstallState.Failed(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Reset installation state
     */
    fun resetState() {
        _installState.value = UpdateInstallState.Idle
    }

    /**
     * Platform-specific installer opening
     * This is implemented using expect/actual pattern
     */
    private fun openInstaller(filePath: String, updateInfo: UpdateInfo): Boolean {
        return openInstallerImpl(filePath, updateInfo)
    }
}

/**
 * Platform-specific installer implementation
 *
 * macOS: Opens .dmg file with `open` command
 * Windows: Runs .msi file with `msiexec` or opens .exe
 * Linux: Opens .deb with package manager or shows file location
 */
expect fun openInstallerImpl(filePath: String, updateInfo: UpdateInfo): Boolean
