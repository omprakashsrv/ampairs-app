package com.ampairs.agent.speech.whisper

import com.ampairs.common.config.AppPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Desktop mic-device enumeration/selection over `javax.sound.sampled`. Lists the mixers that can
 * provide a 16 kHz mono [TargetDataLine] (what [DesktopAudioCapture] records), keyed by mixer name.
 * Selection persists via [AppPreferencesDataStore]; [DesktopAudioCapture] reads it to open that mixer.
 */
class DesktopAudioInputDeviceProvider(
    private val prefs: AppPreferencesDataStore,
) : AudioInputDeviceProvider {

    override val isSupported: Boolean = true

    override suspend fun devices(): List<AudioInputDevice> = withContext(Dispatchers.IO) {
        val info = DataLine.Info(TargetDataLine::class.java, CAPTURE_FORMAT)
        AudioSystem.getMixerInfo().mapNotNull { mixerInfo ->
            val mixer = runCatching { AudioSystem.getMixer(mixerInfo) }.getOrNull() ?: return@mapNotNull null
            // Keep only mixers that actually expose a capture line in our format (drops output-only devices).
            if (!runCatching { mixer.isLineSupported(info) }.getOrDefault(false)) return@mapNotNull null
            AudioInputDevice(id = mixerInfo.name, label = mixerInfo.name)
        }.distinctBy { it.id }
    }

    override fun selectedId(): Flow<String?> = prefs.getSelectedAudioInputDeviceId()

    override suspend fun setSelected(id: String?) = prefs.setSelectedAudioInputDeviceId(id)

    private companion object {
        val CAPTURE_FORMAT = AudioFormat(16_000f, 16, 1, true, false)
    }
}
