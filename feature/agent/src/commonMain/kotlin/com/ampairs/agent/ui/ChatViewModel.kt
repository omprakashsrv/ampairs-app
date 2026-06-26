package com.ampairs.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_cancelled
import ampairsapp.feature.agent.generated.resources.agent_error_generic
import ampairsapp.feature.agent.generated.resources.agent_mic_permission_denied
import ampairsapp.feature.agent.generated.resources.agent_voice_unavailable
import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.ActionResultSummary
import com.ampairs.agent.core.AgentOrchestrator
import com.ampairs.agent.core.AgentStreamEvent
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.AgentResponse
import com.ampairs.agent.data.api.ChatLogRequest
import com.ampairs.agent.data.repository.ChatHistoryRepository
import com.ampairs.agent.llm.LlmEngine
import com.ampairs.agent.llm.ModelCatalogProvider
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import com.ampairs.agent.llm.ProviderRegistry
import com.ampairs.agent.llm.formatModelSize
import com.ampairs.agent.permission.MicPermissionController
import com.ampairs.agent.speech.SpeechAdapterOption
import com.ampairs.agent.speech.SpeechAdapterRegistry
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.agent.speech.TextToSpeech
import com.ampairs.agent.speech.whisper.AudioInputDeviceProvider
import com.ampairs.agent.speech.whisper.WhisperModelRegistry
import com.ampairs.agent.telemetry.ChatTelemetryRecorder
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlin.concurrent.Volatile
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
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
    val isOnline: Boolean = true,
    val error: String? = null,
    /** A money/destructive action awaiting explicit confirm/cancel before it persists (FR-006). */
    val pendingConfirmation: AgentAction? = null,
    /** When true, assistant replies are not read aloud (TTS, FR-009). */
    val isTtsMuted: Boolean = false,
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
    /** Non-null while the full-screen hands-free voice conversation is active (FR-008/009); null otherwise. */
    val voiceMode: VoiceConversationState? = null,
    /** True while the assistant settings sheet (adapter pickers) is open. */
    val showSettings: Boolean = false,
    /** Selectable speech-to-text engines for this platform; empty when none. */
    val sttOptions: List<SpeechAdapterOption> = emptyList(),
    /** Selectable text-to-speech engines for this platform; empty when none. */
    val ttsOptions: List<SpeechAdapterOption> = emptyList(),
    /** Currently selected STT/TTS adapter ids (null → platform default = first option). */
    val selectedSttId: String? = null,
    val selectedTtsId: String? = null,
    /** Downloadable Whisper model sizes (e.g. tiny/base); empty on platforms with no Whisper engine. */
    val whisperModels: List<WhisperModelUi> = emptyList(),
    /** Selectable mic input devices (Desktop only); empty elsewhere → the settings section is hidden. */
    val micDevices: List<SpeechAdapterOption> = emptyList(),
    /** Selected mic device id (the mixer name); null → system default. */
    val selectedMicId: String? = null,
    /** Opt-in: upload chat transcripts to the backend for quality improvement (default OFF). */
    val telemetryEnabled: Boolean = false,
    /** Whether the model's transient "thinking" (reasoning) text is shown while it reasons (default ON). */
    val reasoningEnabled: Boolean = true,
)

/** One Whisper model-size row in the settings sheet (shown only when the Whisper STT engine is active). */
data class WhisperModelUi(
    val id: String,
    val label: String,
    val sizeText: String,
    val status: ModelItemStatus,
    /** True when this size is the active selection (the one a Whisper transcription will use). */
    val isSelected: Boolean,
)

/** State of the hands-free voice conversation overlay: the loop phase plus what's on screen now. */
data class VoiceConversationState(
    val phase: VoicePhase = VoicePhase.Connecting,
    /** Live interim transcript while [VoicePhase.Listening]. */
    val partialTranscript: String = "",
    /** The last committed user utterance (shown briefly while thinking/speaking). */
    val lastUserText: String = "",
    /** The assistant reply currently being / just spoken. */
    val lastReply: String = "",
    /** Set when the loop hit an unrecoverable error (no STT/TTS, mic denied). */
    val error: String? = null,
)

/** Phase of the hands-free voice loop: listen → think → speak → listen, with manual pause. */
enum class VoicePhase {
    /** Acquiring mic permission / warming up before the first listen. */
    Connecting,

    /** Microphone open, transcribing the user. */
    Listening,

    /** Utterance committed; the assistant is producing a reply. */
    Thinking,

    /** Reading the reply aloud; the mic re-opens when it finishes. */
    Speaking,

    /** User paused the loop; tap to resume. */
    Paused,

    /** Unrecoverable error (no engine / permission denied). */
    Error,
}

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
    private val speechAdapters: SpeechAdapterRegistry,
    private val whisperRegistry: WhisperModelRegistry,
    private val audioInputDevices: AudioInputDeviceProvider,
    private val micPermission: MicPermissionController,
    private val providerRegistry: ProviderRegistry,
    private val modelManager: ModelManager,
    private val modelCatalog: ModelCatalogProvider,
    private val appPreferences: AppPreferencesDataStore,
    private val actionRegistry: ActionRegistry,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val telemetryRecorder: ChatTelemetryRecorder,
) : ViewModel() {

    /** Stable id grouping this chat session's telemetry turns (opt-in). */
    private val telemetrySessionId: String = UidGenerator.generateUid("SESS")

    /** Mirror of the opt-in telemetry preference, read in [recordTelemetry] without suspending. */
    @Volatile
    private var telemetryEnabled: Boolean = false

    /** Mirror of the reasoning-display preference, read in the stream collector without suspending. */
    @Volatile
    private var reasoningEnabled: Boolean = true

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** The STT/TTS engines resolved for the in-flight operation, kept so stop()/mute can reach them. */
    @Volatile private var sttInUse: SpeechToText? = null
    @Volatile private var ttsInUse: TextToSpeech? = null

    /** The in-flight streaming chat turn, cancellable via [stopGeneration]. */
    private var streamJob: Job? = null

    /** The hands-free voice conversation loop (listen → think → speak → listen); null when not running. */
    private var voiceLoopJob: Job? = null

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
        loadPersistedHistory()
        _uiState.update { it.copy(sttOptions = speechAdapters.sttOptions, ttsOptions = speechAdapters.ttsOptions) }
        speechAdapters.selectedSttId()
            .onEach { id -> _uiState.update { it.copy(selectedSttId = id) } }
            .launchIn(viewModelScope)
        speechAdapters.selectedTtsId()
            .onEach { id -> _uiState.update { it.copy(selectedTtsId = id) } }
            .launchIn(viewModelScope)
        observeWhisperModels()
        observeMicDevices()
        initModelManager()
        maybeOfferModelDownload()
        appPreferences.getChatTelemetryEnabled()
            .onEach { enabled ->
                telemetryEnabled = enabled
                _uiState.update { it.copy(telemetryEnabled = enabled) }
            }
            .launchIn(viewModelScope)
        appPreferences.getAssistantReasoningEnabled()
            .onEach { enabled ->
                reasoningEnabled = enabled
                _uiState.update { it.copy(reasoningEnabled = enabled) }
            }
            .launchIn(viewModelScope)
    }

    /** Load the selectable mic devices + track the selection (Desktop only; inert elsewhere). */
    private fun observeMicDevices() {
        if (!audioInputDevices.isSupported) return
        viewModelScope.launch {
            val devices = audioInputDevices.devices().map { SpeechAdapterOption(it.id, it.label) }
            _uiState.update { it.copy(micDevices = devices) }
        }
        audioInputDevices.selectedId()
            .onEach { id -> _uiState.update { it.copy(selectedMicId = id) } }
            .launchIn(viewModelScope)
    }

    /** Pick the mic input device (Desktop); the next capture opens that device. */
    fun onSelectMicDevice(id: String) {
        viewModelScope.launch { audioInputDevices.setSelected(id) }
    }

    /** Keep the settings Whisper-size rows live (selection + per-size download state). Inert when none. */
    private fun observeWhisperModels() {
        if (!whisperRegistry.hasModels) return
        combine(
            whisperRegistry.selectedId(),
            whisperRegistry.statuses,
        ) { selected, statuses -> selected to statuses }
            .onEach { (selected, statuses) -> rebuildWhisperUi(selected, statuses) }
            .launchIn(viewModelScope)
    }

    private suspend fun rebuildWhisperUi(selectedId: String?, statuses: Map<String, ModelInstallStatus>) {
        val installed = whisperRegistry.installedIds()
        val effective = whisperRegistry.resolveSelectedId(selectedId)
        val items = whisperRegistry.options.map { o ->
            val live = statuses[o.id]
            val status = when {
                live is ModelInstallStatus.Downloading -> ModelItemStatus.Downloading(live.fraction)
                o.id in installed -> ModelItemStatus.Installed
                live is ModelInstallStatus.Failed -> ModelItemStatus.Failed(live.message)
                else -> ModelItemStatus.NotInstalled
            }
            WhisperModelUi(o.id, o.label, formatModelSize(o.sizeBytes), status, o.id == effective)
        }
        _uiState.update { it.copy(whisperModels = items) }
    }

    /** Pick the Whisper model size (persisted); the next transcription downloads it if needed. */
    fun onSelectWhisperModel(id: String) {
        viewModelScope.launch { whisperRegistry.setSelected(id) }
    }

    /** Start downloading a Whisper model size now, from the settings sheet. */
    fun onDownloadWhisperModel(id: String) {
        whisperRegistry.download(id)
    }

    /** Remove a downloaded Whisper model size to reclaim space. */
    fun onDeleteWhisperModel(id: String) {
        viewModelScope.launch { whisperRegistry.delete(id) }
    }

    // ---- Assistant settings (switchable speech/LLM adapters) ----

    fun openSettings() { _uiState.update { it.copy(showSettings = true) } }
    fun closeSettings() { _uiState.update { it.copy(showSettings = false) } }

    /** Pick the STT engine (persisted); takes effect on the next listen. */
    fun onSelectSttAdapter(id: String) {
        viewModelScope.launch { speechAdapters.setSttId(id) }
    }

    /** Pick the TTS engine (persisted); takes effect on the next spoken reply. */
    fun onSelectTtsAdapter(id: String) {
        viewModelScope.launch { speechAdapters.setTtsId(id) }
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
        _uiState.update { it.copy(inputText = "") }
        addUserMessage(text)
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val reply = runAssistantTurn(text)
            if (reply != null) speakText(reply)
        }
    }

    /** Append the user's message to the transcript and flip into processing (text + voice paths). */
    private fun addUserMessage(text: String) {
        val userMessage = ChatMessage(id = UidGenerator.generateUid("MSG"), text = text, isFromUser = true)
        _uiState.update { it.copy(messages = it.messages + userMessage, isProcessing = true) }
        persistHistory()
    }

    /**
     * Run one assistant turn for [text]: stream/append the reply into the transcript and return the
     * final speakable text (or null when there's nothing to speak — empty/error/action-only). Prefers
     * the native tool-calling streaming path when a model with tools is loaded; falls back to the
     * rule-based orchestrator otherwise (no model / other platforms) or if streaming fails. Does NOT
     * speak — the caller decides (text path fire-and-forget; the voice loop awaits playback).
     */
    private suspend fun runAssistantTurn(text: String): String? {
        val engine = runCatching { providerRegistry.engineOrNull() }.getOrNull()
        val reply = if (engine != null && engine.supportsToolCalling) streamChat(text, engine)
        else processWithOrchestrator(text)
        recordTelemetry(text, reply)
        return reply
    }

    /**
     * Opt-in telemetry: record one completed turn (user + assistant reply) for backend upload. Gated
     * by the user's preference (default OFF); fire-and-forget via the AppScope recorder. Error/empty
     * turns (null reply) are skipped. Shared by the text and voice paths (both call [runAssistantTurn]).
     */
    @OptIn(ExperimentalTime::class)
    private fun recordTelemetry(userText: String, assistantText: String?) {
        if (!telemetryEnabled || assistantText.isNullOrBlank()) return
        val entry = ChatLogRequest(
            sessionId = telemetrySessionId,
            userMessage = userText,
            assistantMessage = assistantText,
            modelId = _uiState.value.modelBar.modelName.ifBlank { null },
            clientTimestamp = Clock.System.now().toEpochMilliseconds(),
        )
        viewModelScope.launch { telemetryRecorder.record(entry) }
    }

    /** Toggle the opt-in chat-telemetry preference (Assistant settings). */
    fun onToggleTelemetry(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setChatTelemetryEnabled(enabled) }
    }

    /** Toggle whether the model's "thinking" (reasoning) text is shown (Assistant settings). */
    fun onToggleReasoning(enabled: Boolean) {
        viewModelScope.launch { appPreferences.setAssistantReasoningEnabled(enabled) }
    }

    /** Stop an in-flight streaming reply: cancels the flow, whose awaitClose calls cancelProcess(). */
    fun stopGeneration() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(isProcessing = false, streamingThought = null) }
    }

    private suspend fun processWithOrchestrator(text: String): String? {
        return try {
            val response = orchestrator.processMessage(
                userMessage = text,
                conversationHistory = _uiState.value.messages,
                isOnline = _uiState.value.isOnline,
            )
            appendAgentResponse(response)
            if (response.actionResult is ActionResult.Error) null else response.text.ifBlank { null }
        } catch (e: Exception) {
            appendError(errorText(e))
            null
        }
    }

    /**
     * Stream a tool-calling turn (Gallery-style): type the model's reply into a live bubble and route
     * tool events — [AgentStreamEvent.ActionProposed] to the confirm bar (money/destructive, FR-006),
     * [AgentStreamEvent.ActionExecuted] to an action-result message. Falls back to the rule-based
     * orchestrator if the model produced nothing usable or the stream errored.
     */
    private suspend fun streamChat(text: String, engine: LlmEngine): String? {
        val history = _uiState.value.messages
            .dropLast(1) // exclude the user message we just added
            .filter { it.text.isNotBlank() }
            .takeLast(HISTORY_TURNS)
            .map { (if (it.isFromUser) "User: " else "Assistant: ") + it.text }

        val streamId = UidGenerator.generateUid("MSG")
        val buffer = StringBuilder()
        val thought = StringBuilder()
        var bubbleAdded = false
        var producedOutput = false
        // Speakable fallback when the turn produced no prose bubble (an action ran/was proposed).
        var lastActionSummary: String? = null
        try {
            engine.chatStream(text, history, actionRegistry::dispatch).collect { event ->
                when (event) {
                    is AgentStreamEvent.ThoughtDelta -> {
                        // Reasoning display is opt-out (Assistant settings); drop thoughts when off.
                        if (reasoningEnabled) {
                            thought.append(event.text)
                            _uiState.update { it.copy(streamingThought = thought.toString()) }
                        }
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
                        lastActionSummary = event.summary
                        _uiState.update { it.copy(pendingConfirmation = event.action, streamingThought = null) }
                    }

                    is AgentStreamEvent.ActionExecuted -> {
                        producedOutput = true
                        lastActionSummary = event.summary
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
                                    rows = success?.rows,
                                ),
                            )
                        }
                    }

                    AgentStreamEvent.Done -> {
                        _uiState.update { it.copy(isProcessing = false, streamingThought = null) }
                    }

                    is AgentStreamEvent.Failed -> {
                        // Fallback (if any) is handled once after the stream completes, below.
                        _uiState.update { it.copy(streamingThought = null) }
                    }
                }
            }
            // Stream finished. Nothing usable → rule-based reply; else return the speakable text.
            if (!producedOutput) return processWithOrchestrator(text)
            _uiState.update { it.copy(isProcessing = false) }
            persistHistory() // streamed bubble + any action messages
            return buffer.toString().ifBlank { lastActionSummary }
        } catch (c: CancellationException) {
            // User tapped Stop (or the VM was cleared) — keep any partial reply, no error, no fallback.
            _uiState.update { it.copy(streamingThought = null) }
            throw c
        } catch (e: Exception) {
            _uiState.update { it.copy(streamingThought = null) }
            return if (!producedOutput) processWithOrchestrator(text) else { appendError(errorText(e)); null }
        }
    }

    /** User accepted a pending money/destructive action — re-dispatch it so the handler persists. */
    fun confirmPending() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null, isProcessing = true) }
        viewModelScope.launch {
            try {
                val response = orchestrator.confirmAction(pending)
                appendAgentResponse(response)
                if (response.actionResult !is ActionResult.Error) speakText(response.text)
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
            persistHistory()
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
            rows = response.rows,
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages + agentMessage,
                isProcessing = false,
                pendingConfirmation = response.pendingConfirmation,
            )
        }
        persistHistory()
    }

    /** Read [text] aloud (fire-and-forget) unless muted or the platform has no TTS engine. */
    private fun speakText(text: String) {
        if (_uiState.value.isTtsMuted || text.isBlank()) return
        viewModelScope.launch {
            val tts = speechAdapters.tts() ?: return@launch
            ttsInUse = tts
            runCatching { tts.speak(text) }
        }
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
        persistHistory()
    }

    // ---- Full-screen hands-free voice conversation (listen → think → speak → listen) ----

    /**
     * Open the voice-conversation overlay and start the loop. Requires both on-device STT and TTS
     * (Android/iOS); on platforms without them (Desktop) it shows an error state rather than a mic that
     * can't work. TTS is force-unmuted on entry — a silent voice conversation makes no sense.
     */
    fun enterVoiceMode() {
        if (_uiState.value.voiceMode != null) return
        _uiState.update { it.copy(isTtsMuted = false, voiceMode = VoiceConversationState(phase = VoicePhase.Connecting)) }
        viewModelScope.launch {
            // STT is required; TTS is optional. Desktop has Whisper STT but no TTS engine, so gating
            // on TTS too would wrongly block voice there — the loop already handles a null TTS by
            // skipping the spoken reply and showing it as text.
            if (speechAdapters.stt() == null) {
                val msg = getString(Res.string.agent_voice_unavailable)
                _uiState.update { it.copy(voiceMode = VoiceConversationState(phase = VoicePhase.Error, error = msg)) }
                return@launch
            }
            startVoiceLoop()
        }
    }

    /** Cancel the loop, silence mic + speech, and dismiss the overlay. */
    fun exitVoiceMode() {
        voiceLoopJob?.cancel()
        voiceLoopJob = null
        ttsInUse?.stop()
        sttInUse?.stop()
        _uiState.update { it.copy(voiceMode = null) }
    }

    /** Pause an active loop (mic + speech off) or resume a paused one. */
    fun toggleVoicePause() {
        val vm = _uiState.value.voiceMode ?: return
        if (vm.phase == VoicePhase.Paused) {
            startVoiceLoop()
        } else {
            voiceLoopJob?.cancel()
            voiceLoopJob = null
            ttsInUse?.stop()
            sttInUse?.stop()
            updateVoice { it.copy(phase = VoicePhase.Paused, partialTranscript = "") }
        }
    }

    /**
     * The conversation loop: confirm mic permission once, then cycle listen → think → speak forever
     * until paused/exited. Each TTS speak() suspends until playback finishes, so the mic only
     * re-opens after the assistant stops talking; a short gap avoids the recognizer catching the TTS tail.
     */
    private fun startVoiceLoop() {
        voiceLoopJob?.cancel()
        voiceLoopJob = viewModelScope.launch {
            if (!micPermission.ensureMicGranted()) {
                val denied = getString(Res.string.agent_mic_permission_denied)
                updateVoice { it.copy(phase = VoicePhase.Error, error = denied) }
                return@launch
            }
            while (isActive && _uiState.value.voiceMode != null) {
                val utterance = listenOnce()
                if (!isActive || _uiState.value.voiceMode == null) break
                if (utterance.isNullOrBlank()) {
                    delay(MIC_REARM_GAP_MS) // silence / no-match / error → brief pause, then listen again
                    continue
                }
                updateVoice { it.copy(phase = VoicePhase.Thinking, lastUserText = utterance, partialTranscript = "") }
                addUserMessage(utterance)
                val reply = runAssistantTurn(utterance)
                if (!isActive || _uiState.value.voiceMode == null) break
                if (!reply.isNullOrBlank()) {
                    // Surface the reply in the overlay regardless of TTS; speak it only if a TTS engine
                    // exists. Desktop has no TTS — without setting lastReply here the answer was computed
                    // but never shown, so the overlay looked stuck on "Thinking".
                    updateVoice { it.copy(phase = VoicePhase.Speaking, lastReply = reply) }
                    val tts = speechAdapters.tts()
                    if (tts != null) {
                        ttsInUse = tts
                        runCatching { tts.speak(reply) } // suspends until spoken (or stop()/cancel)
                    } else {
                        delay(REPLY_READ_GAP_MS) // no TTS: keep the reply on screen long enough to read
                    }
                }
                delay(MIC_REARM_GAP_MS) // let the TTS tail settle before re-opening the mic
            }
        }
    }

    /**
     * Listen for one utterance, streaming partials into [VoiceConversationState.partialTranscript].
     * Returns the final transcript (trimmed), or null on error/no-match. The cold STT flow closes
     * itself on Final/Error, so the collect returns.
     */
    private suspend fun listenOnce(): String? {
        val stt = speechAdapters.stt() ?: return null
        sttInUse = stt
        updateVoice { it.copy(phase = VoicePhase.Listening, partialTranscript = "") }
        var finalText: String? = null
        stt.listen()
            .catch { /* surfaced as null below → loop re-listens */ }
            .collect { event ->
                when (event) {
                    is SttEvent.Partial -> updateVoice { it.copy(partialTranscript = event.text) }
                    is SttEvent.Final -> finalText = event.text.trim()
                    is SttEvent.Error -> { /* finalText stays null → re-listen */ }
                    SttEvent.EndOfSpeech -> { /* keep collecting until Final/Error */ }
                }
            }
        return finalText
    }

    /** Apply [transform] to the voice-mode sub-state if the overlay is open (no-op otherwise). */
    private fun updateVoice(transform: (VoiceConversationState) -> VoiceConversationState) {
        _uiState.update { st -> st.voiceMode?.let { st.copy(voiceMode = transform(it)) } ?: st }
    }

    /** Mute/unmute spoken responses; muting also stops any in-progress speech. */
    fun toggleMute() {
        val nowMuted = !_uiState.value.isTtsMuted
        if (nowMuted) ttsInUse?.stop()
        _uiState.update { it.copy(isTtsMuted = nowMuted) }
    }

    fun setOnlineStatus(online: Boolean) {
        _uiState.update { it.copy(isOnline = online) }
    }

    fun clearChat() {
        streamJob?.cancel()
        streamJob = null
        _uiState.update { it.copy(messages = emptyList(), isProcessing = false, streamingThought = null) }
        viewModelScope.launch {
            // Drop the retained tool-calling conversation so the next turn starts a fresh KV cache
            // (otherwise the just-cleared turns would still live in the engine's context).
            runCatching { providerRegistry.engineOrNull()?.resetChat() }
            runCatching { chatHistoryRepository.clear() }
        }
    }

    // ---- Transcript persistence (per-workspace, capped; restored on open so the chat continues) ----

    /**
     * On launch: prune the stored thread to the cap (housekeeping), then restore it (only if nothing's
     * in memory yet) from the Room DB so the conversation resumes where it left off.
     */
    private fun loadPersistedHistory() {
        viewModelScope.launch {
            runCatching { chatHistoryRepository.cleanup(MAX_PERSISTED_MESSAGES) }
            val restored = runCatching { chatHistoryRepository.load(MAX_PERSISTED_MESSAGES) }.getOrNull().orEmpty()
            if (restored.isEmpty()) return@launch
            _uiState.update { st -> if (st.messages.isEmpty()) st.copy(messages = restored) else st }
        }
    }

    /** Persist the most recent [MAX_PERSISTED_MESSAGES] messages to the Room DB (fire-and-forget). */
    private fun persistHistory() {
        val snapshot = _uiState.value.messages
        viewModelScope.launch {
            runCatching { chatHistoryRepository.save(snapshot, MAX_PERSISTED_MESSAGES) }
        }
    }

    private companion object {
        /** Recent turns fed to the tool-calling chat as short context (the conversation is stateless). */
        const val HISTORY_TURNS = 6

        /** Pause after the assistant finishes speaking before re-opening the mic, so the recognizer
         *  doesn't pick up the tail of the spoken reply. */
        const val MIC_REARM_GAP_MS = 350L

        /** With no TTS (e.g. Desktop), how long the spoken-reply text stays on screen before re-listening. */
        const val REPLY_READ_GAP_MS = 3500L

        /** How many recent messages are persisted/restored for the chat thread (resume on reopen). */
        const val MAX_PERSISTED_MESSAGES = 50
    }
}
