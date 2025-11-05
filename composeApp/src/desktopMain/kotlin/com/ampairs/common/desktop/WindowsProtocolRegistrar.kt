package com.ampairs.common.desktop

import OperatingSystem
import currentOperatingSystem
import java.io.File
import java.util.Optional

/**
 * Registers the custom ampairs:// URL scheme on Windows via the HKCU registry hive.
 * Compose's MSI installer does not yet expose protocol registration hooks, so we
 * perform the registration lazily on first launch. This keeps parity across
 * developer machines and packaged installs without requiring elevated privileges.
 */
object WindowsProtocolRegistrar {
    private const val PROTOCOL_KEY_ROOT = "HKCU\\Software\\Classes"

    fun ensureRegistered(
        protocol: String,
        appName: String
    ) {
        if (currentOperatingSystem != OperatingSystem.Windows) return

        val protocolKey = "$PROTOCOL_KEY_ROOT\\$protocol"

        try {
            if (registryKeyExists(protocolKey)) {
                println("WindowsProtocolRegistrar: $protocol already registered")
                return
            }

            val executable = detectExecutablePath() ?: run {
                println("WindowsProtocolRegistrar: Unable to determine executable path; skipping registration")
                return
            }

            println("WindowsProtocolRegistrar: Registering $protocol for $executable")

            // Root key describing the custom protocol
            runReg(
                "add",
                protocolKey,
                "/ve",
                "/t",
                "REG_SZ",
                "/d",
                "URL:$appName Protocol",
                "/f"
            )
            runReg(
                "add",
                protocolKey,
                "/v",
                "URL Protocol",
                "/t",
                "REG_SZ",
                "/d",
                "",
                "/f"
            )

            // Icon displayed for the protocol
            val defaultIconKey = "$protocolKey\\DefaultIcon"
            runReg(
                "add",
                defaultIconKey,
                "/ve",
                "/t",
                "REG_SZ",
                "/d",
                executable,
                "/f"
            )

            // Shell open command
            val commandKey = "$protocolKey\\shell\\open\\command"
            val commandValue = "\"$executable\" \"%1\""
            runReg(
                "add",
                commandKey,
                "/ve",
                "/t",
                "REG_SZ",
                "/d",
                commandValue,
                "/f"
            )

            println("WindowsProtocolRegistrar: Registration completed")
        } catch (e: Exception) {
            println("WindowsProtocolRegistrar: Failed to register protocol: ${e.message}")
        }
    }

    private fun registryKeyExists(key: String): Boolean =
        runReg("query", key).second == 0

    private fun detectExecutablePath(): String? {
        val info: Optional<String> = ProcessHandle.current().info().command()
        if (info.isPresent) {
            val command = info.get()
            if (command.endsWith(".exe", ignoreCase = true)) {
                return command
            }
        }

        // Fallback: try to locate Ampairs.exe next to the current working directory
        val workingDir = File(System.getProperty("user.dir"))
        val candidate = workingDir.resolve("Ampairs.exe")
        if (candidate.exists()) {
            return candidate.absolutePath
        }

        return null
    }

    private fun runReg(vararg args: String): Pair<String, Int> {
        val command = mutableListOf("reg")
        command.addAll(args)
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            println("WindowsProtocolRegistrar: reg ${args.joinToString(" ")} failed with $exitCode: $output")
        }
        return output to exitCode
    }
}
