package com.ampairs.common.desktop

import java.io.File
import java.util.prefs.Preferences

/**
 * Manages the user-selected data directory for desktop application.
 *
 * This is critical for desktop applications to avoid using default system directories
 * (like AppData or user home) which can be restored to old versions during system
 * restores, causing database version conflicts and data corruption.
 *
 * The selected directory path is stored in Java Preferences API which is separate
 * from the data directory itself, providing a bootstrap mechanism.
 */
object DataDirectoryManager {
    private const val PREF_KEY_DATA_DIRECTORY = "ampairs.data.directory"
    private val prefs = Preferences.userNodeForPackage(DataDirectoryManager::class.java)

    /**
     * Gets the currently selected data directory, or null if not set
     */
    fun getDataDirectory(): File? {
        val path = prefs.get(PREF_KEY_DATA_DIRECTORY, null)
        return path?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
    }

    /**
     * Sets the data directory and creates it if it doesn't exist
     * @return true if successful, false if directory cannot be created or is invalid
     */
    fun setDataDirectory(directory: File): Boolean {
        return try {
            // Validate directory
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    println("DataDirectoryManager: Failed to create directory: ${directory.absolutePath}")
                    return false
                }
            }

            if (!directory.isDirectory) {
                println("DataDirectoryManager: Path is not a directory: ${directory.absolutePath}")
                return false
            }

            if (!directory.canWrite()) {
                println("DataDirectoryManager: Directory is not writable: ${directory.absolutePath}")
                return false
            }

            // Create subdirectories
            val dbDir = File(directory, "databases")
            val cacheDir = File(directory, "cache")
            val prefsDir = File(directory, "preferences")

            dbDir.mkdirs()
            cacheDir.mkdirs()
            prefsDir.mkdirs()

            // Save to preferences
            prefs.put(PREF_KEY_DATA_DIRECTORY, directory.absolutePath)
            prefs.flush()

            println("DataDirectoryManager: Data directory set to: ${directory.absolutePath}")
            true
        } catch (e: Exception) {
            println("DataDirectoryManager: Error setting data directory: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if a data directory has been selected
     */
    fun isDataDirectorySet(): Boolean {
        return getDataDirectory() != null
    }

    /**
     * Gets the database directory within the data directory
     */
    fun getDatabaseDir(): File {
        val dataDir = getDataDirectory()
            ?: throw IllegalStateException("Data directory not set. Call setDataDirectory first.")
        return File(dataDir, "databases")
    }

    /**
     * Gets the cache directory within the data directory
     */
    fun getCacheDir(): File {
        val dataDir = getDataDirectory()
            ?: throw IllegalStateException("Data directory not set. Call setDataDirectory first.")
        return File(dataDir, "cache")
    }

    /**
     * Gets the preferences directory within the data directory
     */
    fun getPreferencesDir(): File {
        val dataDir = getDataDirectory()
            ?: throw IllegalStateException("Data directory not set. Call setDataDirectory first.")
        return File(dataDir, "preferences")
    }

    /**
     * Clears the stored data directory preference
     * Note: This does NOT delete the actual data directory
     */
    fun clearDataDirectory() {
        prefs.remove(PREF_KEY_DATA_DIRECTORY)
        prefs.flush()
        println("DataDirectoryManager: Data directory preference cleared")
    }
}
