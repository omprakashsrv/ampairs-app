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
import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.ActionResultSummary
import com.ampairs.agent.core.AgentOrchestrator
import com.ampairs.agent.core.AgentStreamEvent
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.AgentResponse
import com.ampairs.agent.llm.LlmEngine
import com.ampairs.agent.llm.ModelCatalogProvider
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import com.ampairs.agent.llm.ProviderRegistry
import com.ampairs.agent.llm.formatModelSize
import com.ampairs.agent.permission.MicPermissionController
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.agent.speech.TextToSpeech
import com.ampairs.agent.voice.VoiceNoteController
import com.ampairs.agent.voice.VoiceRecordingState
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
    /** First-use on-device model download prompt (consent → progress → failure); null when nothing to show. */
    val llmDownloadPrompt: LlmDownloadPrompt? = null,
    /** The model's transient "thinking" text while it reasons (tool-calling stream); null when none. */
    val streamingThought: String? = null,
    /** Status chip shown in the chat top bar: which model is active and what state it's in. */
    val modelBar: ModelBarState = ModelBarState(),
    /** Catalog models for the model-manager sheet (download / switch / delete). */
    val models: List<ModelUiItem> = emptyList(),
    /** True while the model-manager bottom sheet is open. */
    val showModelSheet: Boolean = false,
    /** False when this platform has no on-device LLM engine (Desktop/iOS today) — manager is informational only. */
    val llmSupported: Boolean = true,
)

/** What the model status chip shows: the active model's display name and its current phase. */
data class ModelBarState(
    val modelName: String = "",
    val phase: ModelPhase = ModelPhase.RULE_BASED,
    /** 0f..1f, only meaningful while [phase] is [ModelPhase.DOWNLOADING]. */
    val downloadProgress: Float = 0f,
)

/** Lifecycle phase of the active on-device model, surfaced by the top-bar chip. */
enum class ModelPhase {
    /** This platform has no on-device LLM engine yet (Desktop/iOS) — on-device AI is Android-only. */
    UNSUPPORTED,

    /** No model can run on this device — chat uses the rule-based path. */
    RULE_BASED,

    /** A model is selected but its file isn't downloaded yet. */
    NOT_DOWNLOADED,

    /** The selected model's file is downloading. */
    DOWNLOADING,

    /** The file is present and the engine is loading into memory. */
    LOADING,

    /** The engine is loaded and ready for full conversation. */
    READY,

    /** Download or engine load failed. */
    FAILED,
}

/** One catalog model row in the model-manager sheet. */
data class ModelUiItem(
    val id: String,
    val displayName: String,
    val sizeText: String,
    val role: String,
    val recommended: Boolean,
    val status: ModelItemStatus,
    /** True when this is the currently selected/active model. */
    val isActive: Boolean,
)

/** Install state of a single catalog model in the manager sheet. */
sealed interface ModelItemStatus {
    data object NotInstalled : ModelItemStatus
    data class Downloading(val progress: Float) : ModelItemStatus
    data object Installed : ModelItemStatus
    data class Failed(val message: String) : ModelItemStatus
}

/** UI state for the one-time on-device AI model download flow (FR: first-use consent + auto-download). */
sealed interface LlmDownloadPrompt {
    /** Ask the user to consent to downloading the model. */
    data class Consent(val modelName: String, val sizeText: String) : LlmDownloadPrompt

    /** Download in progress; [progress] is 0f..1f (0f when total size unknown). */
    data class Downloading(val progress: Float) : LlmDownloadPrompt

    /** Last download attempt failed. */
    data class Failed(val message: String) : LlmDownloadPrompt
}

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ChatViewModel(
    private val orchestrator: AgentOrchestrator,
    private val speechToText: SpeechToText,
    private val textToSpeech: TextToSpeech,
    private val micPermission: MicPermissionController,
    private val voiceNoteController: VoiceNoteController,
    private val providerRegistry: ProviderRegistry,
    private val modelManager: ModelManager,
    private val modelCatalog: ModelCatalogProvider,
    private val appPreferences: AppPreferencesDataStore,
    private val actionRegistry: ActionRegistry,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var voiceJob: Job? = null

    /** The in-flight streaming chat turn, cancellable via [stopGeneration]. */
    private var streamJob: Job? = null

    /** The model this device can run, resolved once on open; null when the device can't run an LLM. */
    private var offeredModel: ModelDescriptor? = null

    /** Full server-driven catalog, cached for the model-manager sheet. */
    private var catalog: List<ModelDescriptor> = emptyList()

    /** The model currently selected for chat (user override or RAM-gated auto-pick); null → rule-based. */
    private var activeModel: ModelDescriptor? = null

    /** Tracks the engine load attempt so the chip can show LOADING → READY / FAILED. */
    private enum class EngineLoad { UNKNOWN, LOADING, READY, FAILED }
    private var engineLoad = EngineLoad.UNKNOWN

    /** False on platforms with no LLM engine backend (Desktop/iOS today) — manager is informational. */
    private var llmSupported = true

    init {
        voiceNoteController.recordingState
            .onEach { state ->
                _uiState.update { it.copy(isRecordingVoiceNote = state == VoiceRecordingState.Recording) }
            }
            .launchIn(viewModelScope)
        voiceNoteController.playingNoteId
            .onEach { id -> _uiState.update { it.copy(playingVoiceNoteId = id) } }
            .launchIn(viewModelScope)
        initModelManager()
        maybeOfferModelDownload()
    }

    // ---- Model manager (status chip + download / switch / delete sheet) ----

    /**
     * Load the catalog, reconcile install state from disk, resolve the active model, and start
     * observing install-status changes so the chip + sheet stay live. When the active model's file is
     * present, eagerly load the engine so the chip can report READY (or surface a load FAILED) — this
     * is also what warms the model before the user's first message.
     */
    private fun initModelManager() {
        llmSupported = providerRegistry.hasLlmBackend()
        if (!llmSupported) {
            // No on-device engine on this platform (Desktop/iOS) — show "Android only", offer nothing.
            _uiState.update { it.copy(llmSupported = false, modelBar = ModelBarState(phase = ModelPhase.UNSUPPORTED)) }
            return
        }
        viewModelScope.launch {
            catalog = runCatching { modelCatalog.all() }.getOrDefault(emptyList())
            runCatching { modelManager.refresh() }
            activeModel = runCatching { providerRegistry.selectedChatModel() }.getOrNull()
            rebuildModelUi(modelManager.statuses.value)
            ensureEngineLoaded()
        }
        modelManager.statuses
            .onEach { statuses ->
                rebuildModelUi(statuses)
                // When the active model's download just finished, load the engine (chip → READY).
                val active = activeModel
                if (active != null &&
                    statuses[active.id] is ModelInstallStatus.Installed &&
                    engineLoad == EngineLoad.UNKNOWN
                ) {
                    ensureEngineLoaded()
                }
            }
            .launchIn(viewModelScope)
    }

    /** Load the engine for the active model if its file is present and we haven't loaded it yet. */
    private suspend fun ensureEngineLoaded() {
        val active = activeModel ?: return
        if (modelManager.localPathOrNull(active) == null) return
        if (engineLoad == EngineLoad.READY || engineLoad == EngineLoad.LOADING) return
        engineLoad = EngineLoad.LOADING
        rebuildModelUi(modelManager.statuses.value)
        val ready = runCatching { providerRegistry.isLlmReady() }.getOrDefault(false)
        engineLoad = if (ready) EngineLoad.READY else EngineLoad.FAILED
        rebuildModelUi(modelManager.statuses.value)
    }

    /** Recompute the chip + sheet rows from the catalog, the active model, and live install status. */
    private fun rebuildModelUi(statuses: Map<String, ModelInstallStatus>) {
        val active = activeModel
        val items = catalog.map { m ->
            val live = statuses[m.id]
            val status = when {
                live is ModelInstallStatus.Downloading -> ModelItemStatus.Downloading(live.fraction)
                // Filesystem truth: a present file ⇒ installed, even when `statuses` hasn't been
                // reconciled for this server-catalog id (e.g. right after an app restart, where the
                // manager's refresh keys off the bundled fallback catalog). This matches what the
                // engine actually loads — otherwise a downloaded model shows "Download" on relaunch.
                modelManager.localPathOrNull(m) != null -> ModelItemStatus.Installed
                live is ModelInstallStatus.Failed -> ModelItemStatus.Failed(live.message)
                else -> ModelItemStatus.NotInstalled
            }
            ModelUiItem(
                id = m.id,
                displayName = m.displayName,
                sizeText = formatModelSize(m.sizeBytes),
                role = m.role.name,
                recommended = m.recommended,
                status = status,
                isActive = m.id == active?.id,
            )
        }
        _uiState.update { it.copy(models = items, modelBar = computeChip(active, statuses)) }
    }

    private fun computeChip(active: ModelDescriptor?, statuses: Map<String, ModelInstallStatus>): ModelBarState {
        if (!llmSupported) return ModelBarState(phase = ModelPhase.UNSUPPORTED)
        if (active == null) return ModelBarState(phase = ModelPhase.RULE_BASED)
        val status = statuses[active.id]
        if (status is ModelInstallStatus.Downloading) {
            return ModelBarState(active.displayName, ModelPhase.DOWNLOADING, status.fraction)
        }
        val installed = status is ModelInstallStatus.Installed || modelManager.localPathOrNull(active) != null
        return when {
            installed && engineLoad == EngineLoad.READY -> ModelBarState(active.displayName, ModelPhase.READY)
            installed && engineLoad == EngineLoad.FAILED -> ModelBarState(active.displayName, ModelPhase.FAILED)
            installed -> ModelBarState(active.displayName, ModelPhase.LOADING)
            status is ModelInstallStatus.Failed -> ModelBarState(active.displayName, ModelPhase.FAILED)
            else -> ModelBarState(active.displayName, ModelPhase.NOT_DOWNLOADED)
        }
    }

    /** Open the model-manager sheet, refreshing the catalog + on-disk state first. */
    fun openModelManager() {
        _uiState.update { it.copy(showModelSheet = true) }
        if (!llmSupported) return // informational sheet only — nothing to refresh
        viewModelScope.launch {
            if (catalog.isEmpty()) catalog = runCatching { modelCatalog.all() }.getOrDefault(emptyList())
            runCatching { modelManager.refresh() }
            rebuildModelUi(modelManager.statuses.value)
        }
    }

    fun closeModelManager() {
        _uiState.update { it.copy(showModelSheet = false) }
    }

    /** Download a catalog model (records consent so it can auto-resume) without switching to it. */
    fun onDownloadModel(modelId: String) {
        val model = catalog.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            appPreferences.setLlmModelDownloadConsent(true)
            modelManager.download(model)
        }
    }

    /** Make [modelId] the active chat model: persist the choice, reload the engine, download if needed. */
    fun onUseModel(modelId: String) {
        val model = catalog.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            appPreferences.setSelectedLlmModelId(modelId)
            activeModel = model
            engineLoad = EngineLoad.UNKNOWN
            providerRegistry.reload()
            if (modelManager.localPathOrNull(model) == null) {
                appPreferences.setLlmModelDownloadConsent(true)
                modelManager.download(model)
            }
            rebuildModelUi(modelManager.statuses.value)
            ensureEngineLoaded()
        }
    }

    /** Delete a model's file. If it was the active one, clear the override and resume auto-pick. */
    fun onDeleteModel(modelId: String) {
        val model = catalog.firstOrNull { it.id == modelId } ?: return
        viewModelScope.launch {
            val wasActive = activeModel?.id == modelId
            modelManager.delete(model)
            if (wasActive) {
                appPreferences.setSelectedLlmModelId(null)
                providerRegistry.reload()
                engineLoad = EngineLoad.UNKNOWN
                activeModel = runCatching { providerRegistry.selectedChatModel() }.getOrNull()
                ensureEngineLoaded()
            }
            rebuildModelUi(modelManager.statuses.value)
        }
    }

    /**
     * On chat open, decide whether to offer the on-device model. Selection is server-driven (the
     * catalog is pulled lazily by [ProviderRegistry.selectedChatModel]); if the device can run an
     * LLM and the model isn't installed yet, honor the stored consent: ask on first use, auto-resume
     * if previously granted, stay rule-based if previously declined. Chat keeps working either way.
     */
    private fun maybeOfferModelDownload() {
        viewModelScope.launch {
            // No engine on this platform → never offer a download that can't be loaded.
            if (!providerRegistry.hasLlmBackend()) return@launch
            val model = providerRegistry.selectedChatModel() ?: return@launch
            offeredModel = model
            // Filesystem truth (survives restarts even before statuses are refreshed).
            if (modelManager.localPathOrNull(model) != null) return@launch

            modelManager.statuses
                .onEach { statuses ->
                    when (val status = statuses[model.id]) {
                        is ModelInstallStatus.Downloading ->
                            _uiState.update { it.copy(llmDownloadPrompt = LlmDownloadPrompt.Downloading(status.fraction)) }
                        is ModelInstallStatus.Installed ->
                            _uiState.update { it.copy(llmDownloadPrompt = null) }
                        is ModelInstallStatus.Failed ->
                            _uiState.update { it.copy(llmDownloadPrompt = LlmDownloadPrompt.Failed(status.message)) }
                        else -> { /* NotInstalled / absent — leave any consent prompt as-is */ }
                    }
                }
                .launchIn(viewModelScope)

            when (appPreferences.getLlmModelDownloadConsent().first()) {
                true -> startModelDownload(model)
                false -> { /* declined earlier — stay rule-based, never re-prompt */ }
                null -> _uiState.update {
                    it.copy(
                        llmDownloadPrompt = LlmDownloadPrompt.Consent(
                            modelName = model.displayName,
                            sizeText = formatBytes(model.sizeBytes),
                        ),
                    )
                }
            }
        }
    }

    /** User consented to the one-time model download. Persists consent and starts the download. */
    fun onAcceptModelDownload() {
        val model = offeredModel ?: return
        viewModelScope.launch {
            appPreferences.setLlmModelDownloadConsent(true)
            startModelDownload(model)
        }
    }

    /** User declined the download. Persists the choice so we don't ask again; chat stays rule-based. */
    fun onDeclineModelDownload() {
        viewModelScope.launch { appPreferences.setLlmModelDownloadConsent(false) }
        _uiState.update { it.copy(llmDownloadPrompt = null) }
    }

    /** Dismiss the progress/failure prompt; an in-progress download continues in the background. */
    fun dismissModelDownloadPrompt() {
        _uiState.update { it.copy(llmDownloadPrompt = null) }
    }

    private fun startModelDownload(model: ModelDescriptor) {
        _uiState.update { it.copy(llmDownloadPrompt = LlmDownloadPrompt.Downloading(0f)) }
        modelManager.download(model)
    }

    /** Human-readable size (e.g. "3.5 GB", "300 MB") for the consent prompt. Decimal units. */
    private fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / 1_000_000_000.0
        if (gb >= 1.0) {
            val rounded = (gb * 10).toLong() / 10.0
            return "$rounded GB"
        }
        val mb = bytes.toDouble() / 1_000_000.0
        return "${mb.toLong()} MB"
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

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            // Prefer the native tool-calling streaming path when a model with tools is loaded; it
            // types the reply out live and dispatches actions via tools. Fall back to the rule-based
            // orchestrator otherwise (no model / other platforms) or if streaming fails.
            val engine = runCatching { providerRegistry.engineOrNull() }.getOrNull()
            if (engine != null && engine.supportsToolCalling) {
                streamChat(text, engine)
            } else {
                processWithOrchestrator(text)
            }
        }
    }

    /** Stop an in-flight streaming reply: cancels the flow, whose awaitClose calls cancelProcess(). */
    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(isProcessing = false, streamingThought = null) }
    }

    private suspend fun processWithOrchestrator(text: String) {
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

    /**
     * Stream a tool-calling turn (Gallery-style): type the model's reply into a live bubble and route
     * tool events — [AgentStreamEvent.ActionProposed] to the confirm bar (money/destructive, FR-006),
     * [AgentStreamEvent.ActionExecuted] to an action-result message. Falls back to the rule-based
     * orchestrator if the model produced nothing usable or the stream errored.
     */
    private suspend fun streamChat(text: String, engine: LlmEngine) {
        val history = _uiState.value.messages
            .dropLast(1) // exclude the user message we just added
            .filter { it.text.isNotBlank() && !it.isVoiceNote }
            .takeLast(HISTORY_TURNS)
            .map { (if (it.isFromUser) "User: " else "Assistant: ") + it.text }

        val streamId = UidGenerator.generateUid("MSG")
        val buffer = StringBuilder()
        val thought = StringBuilder()
        var bubbleAdded = false
        var producedOutput = false
        try {
            engine.chatStream(text, history, actionRegistry::dispatch).collect { event ->
                when (event) {
                    is AgentStreamEvent.ThoughtDelta -> {
                        thought.append(event.text)
                        _uiState.update { it.copy(streamingThought = thought.toString()) }
                    }

                    is AgentStreamEvent.TextDelta -> {
                        buffer.append(event.text)
                        producedOutput = true
                        if (!bubbleAdded) {
                            bubbleAdded = true
                            _uiState.update {
                                it.copy(
                                    streamingThought = null, // real answer started — drop the thinking line
                                    messages = it.messages + ChatMessage(id = streamId, text = buffer.toString(), isFromUser = false),
                                )
                            }
                        } else {
                            val current = buffer.toString()
                            _uiState.update { st ->
                                st.copy(messages = st.messages.map { if (it.id == streamId) it.copy(text = current) else it })
                            }
                        }
                    }

                    is AgentStreamEvent.ActionProposed -> {
                        producedOutput = true
                        _uiState.update { it.copy(pendingConfirmation = event.action, streamingThought = null) }
                    }

                    is AgentStreamEvent.ActionExecuted -> {
                        producedOutput = true
                        val success = event.result as? ActionResult.Success
                        val summary = ActionResultSummary(
                            actionType = event.action.actionType.name,
                            moduleName = event.action.moduleName,
                            success = event.result is ActionResult.Success,
                            navigationRouteDescription = success?.navigationTarget?.routeDescription,
                            navigationRouteData = success?.navigationTarget?.routeData,
                        )
                        _uiState.update {
                            it.copy(
                                streamingThought = null,
                                messages = it.messages + ChatMessage(
                                    id = UidGenerator.generateUid("MSG"),
                                    text = event.summary,
                                    isFromUser = false,
                                    actionResult = summary,
                                    amount = success?.amount,
                                ),
                            )
                        }
                    }

                    AgentStreamEvent.Done -> {
                        _uiState.update { it.copy(isProcessing = false, streamingThought = null) }
                        if (bubbleAdded) {
                            _uiState.value.messages.firstOrNull { it.id == streamId }?.let { speakIfEnabled(it) }
                        }
                    }

                    is AgentStreamEvent.Failed -> {
                        _uiState.update { it.copy(streamingThought = null) }
                        if (!producedOutput) processWithOrchestrator(text) else _uiState.update { it.copy(isProcessing = false) }
                    }
                }
            }
            if (!producedOutput) processWithOrchestrator(text) // model said nothing → rule-based reply
        } catch (c: CancellationException) {
            // User tapped Stop (or the VM was cleared) — keep any partial reply, no error, no fallback.
            _uiState.update { it.copy(streamingThought = null) }
            throw c
        } catch (e: Exception) {
            _uiState.update { it.copy(streamingThought = null) }
            if (!producedOutput) processWithOrchestrator(text) else appendError(errorText(e))
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
        streamJob?.cancel()
        streamJob = null
        voiceNoteController.stopPlayback()
        _uiState.update { it.copy(messages = emptyList(), isProcessing = false, streamingThought = null) }
    }

    private companion object {
        /** Recent turns fed to the tool-calling chat as short context (the conversation is stateless). */
        const val HISTORY_TURNS = 6
    }
}
