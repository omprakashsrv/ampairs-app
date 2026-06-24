package com.ampairs.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_cancelled
import ampairsapp.feature.agent.generated.resources.agent_error_generic
import ampairsapp.feature.agent.generated.resources.agent_mic_permission_denied
import ampairsapp.feature.agent.generated.resources.agent_speech_unavailable
import ampairsapp.feature.agent.generated.resources.agent_voice_note_empty
import ampairsapp.feature.agent.generated.resources.agent_voice_note_failed
import com.ampairs.agent.core.ActionResultSummary
import com.ampairs.agent.core.AgentOrchestrator
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.AgentResponse
import com.ampairs.agent.permission.MicPermissionController
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.agent.speech.TextToSpeech
import com.ampairs.agent.voice.VoiceNoteController
import com.ampairs.agent.voice.VoiceRecordingState
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val isListening: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null,
    /** A money/destructive action awaiting explicit confirm/cancel before it persists (FR-006). */
    val pendingConfirmation: AgentAction? = null,
    /** Live interim transcript shown while listening (FR-008). */
    val liveTranscript: String = "",
    /** When true, assistant replies are not read aloud (TTS, FR-009). */
    val isTtsMuted: Boolean = false,
    /** True while a voice note is being recorded (distinct from STT [isListening]). */
    val isRecordingVoiceNote: Boolean = false,
    /** Id of the voice-note message currently playing, or null. */
    val playingVoiceNoteId: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ChatViewModel(
    private val orchestrator: AgentOrchestrator,
    private val speechToText: SpeechToText,
    private val textToSpeech: TextToSpeech,
    private val micPermission: MicPermissionController,
    private val voiceNoteController: VoiceNoteController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var voiceJob: Job? = null

    init {
        voiceNoteController.recordingState
            .onEach { state ->
                _uiState.update { it.copy(isRecordingVoiceNote = state == VoiceRecordingState.Recording) }
            }
            .launchIn(viewModelScope)
        voiceNoteController.playingNoteId
            .onEach { id -> _uiState.update { it.copy(playingVoiceNoteId = id) } }
            .launchIn(viewModelScope)
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = text,
            isFromUser = true,
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isProcessing = true,
            )
        }

        viewModelScope.launch {
            try {
                appendAgentResponse(
                    orchestrator.processMessage(
                        userMessage = text,
                        conversationHistory = _uiState.value.messages,
                        isOnline = _uiState.value.isOnline,
                    )
                )
            } catch (e: Exception) {
                appendError(errorText(e))
            }
        }
    }

    /** User accepted a pending money/destructive action — re-dispatch it so the handler persists. */
    fun confirmPending() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null, isProcessing = true) }
        viewModelScope.launch {
            try {
                appendAgentResponse(orchestrator.confirmAction(pending))
            } catch (e: Exception) {
                appendError(errorText(e))
            }
        }
    }

    /** User declined a pending action — discard it; nothing was persisted. */
    fun cancelPending() {
        if (_uiState.value.pendingConfirmation == null) return
        _uiState.update { it.copy(pendingConfirmation = null) }
        viewModelScope.launch {
            val message = ChatMessage(
                id = UidGenerator.generateUid("MSG"),
                text = getString(Res.string.agent_cancelled),
                isFromUser = false,
            )
            _uiState.update { it.copy(messages = it.messages + message) }
        }
    }

    private suspend fun errorText(e: Exception): String =
        getString(Res.string.agent_error_generic) + (e.message?.let { ": $it" } ?: "")

    private fun appendAgentResponse(response: AgentResponse) {
        val actionSummary = response.resolvedAction?.let { action ->
            val result = response.actionResult
            ActionResultSummary(
                actionType = action.actionType.name,
                moduleName = action.moduleName,
                success = result is ActionResult.Success,
                navigationRouteDescription = (result as? ActionResult.Success)?.navigationTarget?.routeDescription,
                navigationRouteData = (result as? ActionResult.Success)?.navigationTarget?.routeData,
            )
        }
        val agentMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = response.text,
            isFromUser = false,
            actionResult = actionSummary,
            isError = response.actionResult is ActionResult.Error,
            amount = response.amount,
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages + agentMessage,
                isProcessing = false,
                pendingConfirmation = response.pendingConfirmation,
            )
        }
        speakIfEnabled(agentMessage)
    }

    /** Read an assistant reply aloud unless muted, the platform lacks TTS, or it's an error message. */
    private fun speakIfEnabled(message: ChatMessage) {
        if (message.isError || _uiState.value.isTtsMuted || !textToSpeech.isAvailable) return
        if (message.text.isBlank()) return
        viewModelScope.launch { runCatching { textToSpeech.speak(message.text) } }
    }

    private fun appendError(text: String) {
        val errorMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = text,
            isFromUser = false,
            isError = true,
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + errorMessage, isProcessing = false)
        }
    }

    /** Toggle voice input: start listening if idle, otherwise stop the in-progress recognition. */
    fun toggleVoiceInput() {
        if (_uiState.value.isListening) stopVoiceInput() else startVoiceInput()
    }

    private fun startVoiceInput() {
        if (!speechToText.isAvailable) {
            viewModelScope.launch { appendError(getString(Res.string.agent_speech_unavailable)) }
            return
        }
        voiceJob?.cancel()
        voiceJob = viewModelScope.launch {
            if (!micPermission.ensureMicGranted()) {
                appendError(getString(Res.string.agent_mic_permission_denied))
                return@launch
            }
            _uiState.update { it.copy(isListening = true, liveTranscript = "") }
            speechToText.listen()
                .catch { stopListeningState() }
                .collect { event -> handleSttEvent(event) }
        }
    }

    private fun stopVoiceInput() {
        speechToText.stop()
        voiceJob?.cancel()
        voiceJob = null
        stopListeningState()
    }

    private fun handleSttEvent(event: SttEvent) {
        when (event) {
            is SttEvent.Partial -> _uiState.update { it.copy(liveTranscript = event.text) }
            is SttEvent.Final -> {
                stopListeningState()
                val text = event.text.trim()
                if (text.isNotBlank()) {
                    _uiState.update { it.copy(inputText = text) }
                    sendMessage()
                }
            }
            is SttEvent.Error -> stopListeningState()
            SttEvent.EndOfSpeech -> { /* keep listening until Final/Error */ }
        }
    }

    private fun stopListeningState() {
        _uiState.update { it.copy(isListening = false, liveTranscript = "") }
    }

    /** Mute/unmute spoken responses; muting also stops any in-progress speech. */
    fun toggleMute() {
        val nowMuted = !_uiState.value.isTtsMuted
        if (nowMuted) textToSpeech.stop()
        _uiState.update { it.copy(isTtsMuted = nowMuted) }
    }

    /** Programmatically set the transcribed input (used by tests / external callers). */
    fun onVoiceResult(transcribedText: String) {
        _uiState.update { it.copy(inputText = transcribedText, isListening = false, liveTranscript = "") }
    }

    fun setOnlineStatus(online: Boolean) {
        _uiState.update { it.copy(isOnline = online) }
    }

    // ---- Voice notes (record + replay an audio bubble; no transcription) ----

    /** Start recording a voice note after confirming mic permission. */
    fun startVoiceNote() {
        if (_uiState.value.isRecordingVoiceNote) return
        viewModelScope.launch {
            if (!micPermission.ensureMicGranted()) {
                appendError(getString(Res.string.agent_mic_permission_denied))
                return@launch
            }
            runCatching { voiceNoteController.startRecording() }
                .onFailure { appendError(getString(Res.string.agent_voice_note_failed)) }
        }
    }

    /** Stop recording and post the captured audio as a voice-note bubble. */
    fun stopAndSendVoiceNote() {
        if (!_uiState.value.isRecordingVoiceNote) return
        val noteId = UidGenerator.generateUid("VN")
        viewModelScope.launch {
            val note = runCatching { voiceNoteController.stopRecording(noteId) }.getOrNull()
            if (note == null) {
                appendError(getString(Res.string.agent_voice_note_empty))
                return@launch
            }
            val message = ChatMessage(
                id = note.id,
                text = "",
                isFromUser = true,
                isVoiceNote = true,
                voiceNoteDurationMs = note.durationMs,
            )
            _uiState.update { it.copy(messages = it.messages + message) }
        }
    }

    /** Discard an in-progress voice-note recording without posting it. */
    fun cancelVoiceNote() {
        voiceNoteController.cancelRecording()
    }

    /** Toggle playback of the voice note in [messageId]'s bubble. */
    fun toggleVoiceNotePlayback(messageId: String) {
        if (_uiState.value.playingVoiceNoteId == messageId) {
            voiceNoteController.stopPlayback()
        } else {
            voiceNoteController.play(messageId)
        }
    }

    fun clearChat() {
        voiceNoteController.stopPlayback()
        _uiState.update { it.copy(messages = emptyList()) }
    }
}
