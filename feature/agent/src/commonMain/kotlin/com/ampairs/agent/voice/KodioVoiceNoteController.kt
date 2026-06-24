package com.ampairs.agent.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.kodio.core.AudioQuality
import space.kodio.core.AudioRecording
import space.kodio.core.Kodio
import space.kodio.core.Recorder

/**
 * kodio-backed [VoiceNoteController]. Records with [AudioQuality.Voice] (mono/16k/16-bit — ideal for
 * voice messages) and keeps each finished [AudioRecording] in memory keyed by note id.
 *
 * `Dispatchers.Default` is used for the playback coroutine — safe on every target (on iOS/Native
 * `Dispatchers.IO` is internal; `Default` is the correct choice in shared code per the KMP rules).
 */
class KodioVoiceNoteController : VoiceNoteController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val recordings = mutableMapOf<String, AudioRecording>()
    private var recorder: Recorder? = null
    private var playbackJob: Job? = null

    private val _recordingState = MutableStateFlow(VoiceRecordingState.Idle)
    override val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()

    private val _playingNoteId = MutableStateFlow<String?>(null)
    override val playingNoteId: StateFlow<String?> = _playingNoteId.asStateFlow()

    override suspend fun startRecording() {
        if (_recordingState.value == VoiceRecordingState.Recording) return
        // Fresh recorder per take — kodio refuses start() on a recorder that already produced one.
        val newRecorder = Kodio.recorder(quality = AudioQuality.Voice)
        try {
            newRecorder.start()
        } catch (e: Throwable) {
            newRecorder.release()
            throw e
        }
        recorder = newRecorder
        _recordingState.value = VoiceRecordingState.Recording
    }

    override suspend fun stopRecording(noteId: String): VoiceNote? {
        val active = recorder ?: return null
        recorder = null
        _recordingState.value = VoiceRecordingState.Idle
        active.stop()
        val recording = try {
            active.getRecording()
        } finally {
            active.release()
        }
        if (recording == null || recording.isEmpty) return null
        recordings[noteId] = recording
        return VoiceNote(id = noteId, durationMs = recording.calculatedDuration.inWholeMilliseconds)
    }

    override fun cancelRecording() {
        recorder?.release()
        recorder = null
        _recordingState.value = VoiceRecordingState.Idle
    }

    override fun play(noteId: String) {
        val recording = recordings[noteId] ?: return
        stopPlayback()
        _playingNoteId.value = noteId
        playbackJob = scope.launch {
            try {
                Kodio.play(recording)
            } catch (_: Throwable) {
                // Playback failure is non-fatal — just clear the playing state below.
            } finally {
                if (_playingNoteId.value == noteId) _playingNoteId.value = null
            }
        }
    }

    override fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _playingNoteId.value = null
    }
}
