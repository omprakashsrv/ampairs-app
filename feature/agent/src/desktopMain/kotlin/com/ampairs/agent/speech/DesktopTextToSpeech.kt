package com.ampairs.agent.speech

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * Desktop text-to-speech via the OS's built-in voice — there's no bundled JVM TTS, so we shell out to
 * the platform synthesizer:
 * - **macOS:** `say` (always present)
 * - **Windows:** PowerShell `System.Speech.Synthesis.SpeechSynthesizer` (text fed via stdin to avoid
 *   quoting issues)
 * - **Linux:** `spd-say --wait` or `espeak` if installed
 *
 * [speak] suspends until the utterance finishes (the voice loop re-opens the mic only after), and
 * cancelling the coroutine (or [stop]) kills the in-flight process so playback stops immediately.
 */
class DesktopTextToSpeech : TextToSpeech {

    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private val isMac = os.contains("mac")
    private val isWindows = os.contains("win")
    private val isLinux = os.contains("nux") || os.contains("nix")

    @Volatile private var current: Process? = null

    override val isAvailable: Boolean
        get() = isMac || isWindows || (isLinux && (commandExists("spd-say") || commandExists("espeak")))

    override suspend fun speak(text: String, languageTag: String?) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        withContext(Dispatchers.IO) {
            val process = startProcess(trimmed) ?: return@withContext
            current = process
            try {
                runInterruptible { process.waitFor() } // interrupted on coroutine cancel → finally kills it
            } finally {
                runCatching { if (process.isAlive) process.destroy() }
                if (current === process) current = null
            }
        }
    }

    override fun stop() {
        current?.let { runCatching { it.destroy() } }
        current = null
    }

    private fun startProcess(text: String): Process? = runCatching {
        when {
            isMac -> ProcessBuilder("say", text).start()
            isLinux && commandExists("spd-say") -> ProcessBuilder("spd-say", "--wait", text).start()
            isLinux && commandExists("espeak") -> ProcessBuilder("espeak", text).start()
            isWindows -> ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "Add-Type -AssemblyName System.Speech; " +
                    "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "\$s.Speak([Console]::In.ReadToEnd())",
            ).start().also { p ->
                runCatching { p.outputStream.bufferedWriter().use { it.write(text) } }
            }
            else -> null
        }
    }.onFailure { Logger.w(tag = LOG_TAG) { "Desktop TTS failed to start: ${it.message}" } }.getOrNull()

    /** Best-effort PATH check for a CLI tool (POSIX `command -v`); used only on Linux. */
    private fun commandExists(cmd: String): Boolean = runCatching {
        ProcessBuilder("sh", "-c", "command -v $cmd").start().waitFor() == 0
    }.getOrDefault(false)

    private companion object {
        const val LOG_TAG = "AgentTts"
    }
}
