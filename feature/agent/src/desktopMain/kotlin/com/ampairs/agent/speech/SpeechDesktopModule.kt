package com.ampairs.agent.speech

import com.ampairs.agent.speech.whisper.AudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.DesktopAudioCapture
import com.ampairs.agent.speech.whisper.DesktopAudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.DesktopWhisperTranscriber
import com.ampairs.agent.speech.whisper.WhisperModelCatalog
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.speech.whisper.WhisperModelSet
import com.ampairs.agent.speech.whisper.WhisperSttEngine
import com.ampairs.agent.speech.vosk.VoskModelCatalog
import com.ampairs.agent.speech.vosk.VoskModelRegistry
import com.ampairs.agent.speech.vosk.VoskModelSet
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Desktop has no native recognizer, so it offers two **offline** STT adapters: Whisper (whisper.cpp via
 * `whisper-jni`, ggml models — the default) and Vosk (Kaldi via `com.alphacephei:vosk` — live partials).
 * Both download their models on demand through the shared archive-aware `ModelManager`
 * ([WhisperModelRegistry] / [VoskModelRegistry]; Vosk models are zip directories). The mic input device
 * is user-selectable ([DesktopAudioInputDeviceProvider]); the chosen device drives capture for both.
 */
@ContributesTo(AppScope::class)
interface SpeechDesktopModule {
    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun provideWhisperModelSet(): WhisperModelSet = WhisperModelSet(WhisperModelCatalog.ggml)

        @Provides
        @SingleIn(AppScope::class)
        fun provideVoskModelSet(): VoskModelSet = VoskModelSet(VoskModelCatalog.models)

        @Provides
        @SingleIn(AppScope::class)
        fun provideAudioInputDeviceProvider(prefs: AppPreferencesDataStore): AudioInputDeviceProvider =
            DesktopAudioInputDeviceProvider(prefs)

        @Provides
        @SingleIn(AppScope::class)
        fun provideSttAdapters(
            whisperRegistry: WhisperModelRegistry,
            voskRegistry: VoskModelRegistry,
            prefs: AppPreferencesDataStore,
        ): List<SttAdapterEntry> = buildList {
            add(
                SttAdapterEntry(
                    id = "whisper",
                    label = "Whisper (offline)",
                    engine = WhisperSttEngine(whisperRegistry, DesktopWhisperTranscriber(), DesktopAudioCapture(prefs)),
                ),
            )
            // Second offline engine: Vosk streams live partials and self-endpoints on silence. Its model
            // (a zip directory) downloads on demand via VoskModelRegistry; Whisper stays the default. Its
            // own DesktopAudioCapture instance (no shared capture state). Gated off where the prebuilt
            // libvosk native is unreliable (see [voskNativeUsable]) — a SIGSEGV in model load can't be
            // caught, so we must not expose it there.
            if (voskNativeUsable()) {
                add(
                    SttAdapterEntry(
                        id = "vosk",
                        label = "Vosk (offline)",
                        engine = VoskSpeechToText(voskRegistry, DesktopAudioCapture(prefs)),
                    ),
                )
            }
        }

        @Provides
        @SingleIn(AppScope::class)
        fun provideTtsAdapters(): List<TtsAdapterEntry> = listOf(
            TtsAdapterEntry(id = "native", label = "Device", engine = DesktopTextToSpeech()),
        )

        /**
         * Whether the prebuilt `com.alphacephei:vosk` native is usable on this OS/arch. It crashes
         * (hard SIGSEGV in `vosk_model_new` → `Model::ReadDataFiles`) loading models on **macOS/arm64**
         * — the bundled Apple-Silicon dylib is the broken 2022 build and upstream ships no fixed macOS
         * native past 0.3.42. A native SIGSEGV can't be caught, so Vosk is hidden there (Whisper stays
         * the default); it works on Linux, Windows, and Intel macOS.
         */
        private fun voskNativeUsable(): Boolean {
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val arch = System.getProperty("os.arch").orEmpty().lowercase()
            val isMacArm = os.contains("mac") && (arch == "aarch64" || arch == "arm64")
            return !isMacArm
        }
    }
}
