package com.ampairs.agent.speech.whisper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** A selectable microphone input device (Desktop). [id] is the platform mixer name; [label] is shown. */
data class AudioInputDevice(val id: String, val label: String)

/**
 * Lists and selects the microphone input device. Only **Desktop** supports choosing a device (via
 * `javax.sound.sampled` mixers); Android/iOS let the OS pick the mic, so they bind [NoopAudioInputDeviceProvider]
 * ([isSupported] = false) and the settings UI hides the section.
 */
interface AudioInputDeviceProvider {
    /** Whether this platform lets the user pick a mic device (true only on Desktop). */
    val isSupported: Boolean

    /** Available input devices; empty when unsupported or none found. */
    suspend fun devices(): List<AudioInputDevice>

    /** Persisted selected device id, or null for the system default. */
    fun selectedId(): Flow<String?>

    /** Persist the chosen device id (null → system default). */
    suspend fun setSelected(id: String?)
}

/** Android/iOS: device selection isn't applicable — the OS chooses the mic. */
class NoopAudioInputDeviceProvider : AudioInputDeviceProvider {
    override val isSupported: Boolean = false
    override suspend fun devices(): List<AudioInputDevice> = emptyList()
    override fun selectedId(): Flow<String?> = flowOf(null)
    override suspend fun setSelected(id: String?) {}
}
