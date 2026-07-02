package com.ampairs.form.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.agent.llm.OutputSchema
import com.ampairs.agent.llm.ProviderRegistry
import com.ampairs.agent.permission.MicPermissionController
import com.ampairs.agent.speech.SpeechAdapterRegistry
import com.ampairs.agent.speech.SpeechToText
import com.ampairs.agent.speech.SttEvent
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.data.repository.ConfigRepository
import com.ampairs.form.domain.FieldDataType
import com.ampairs.form.domain.FormField
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Screen-scoped assistant for a single entity form. Turns the running conversation into predicted
 * values for the form's fields: the on-device LLM is asked to extract a JSON map of
 * `fieldKey -> value` for ONLY the fields the user described, constrained to this form's schema.
 *
 * Why a dedicated, entity-keyed ViewModel rather than the global [com.ampairs.agent.ui.ChatViewModel]:
 * the prediction is inherently screen-scoped — it needs the schema of the form that is open (field
 * keys, types, choice options) to ground the model, and the predicted values must flow straight back
 * into THIS screen's form state. The global chat has no per-screen context and a static prompt.
 *
 * It reuses the agent module's constrained-generation path verbatim: [OutputSchema] forces the model
 * to emit `{"intent","actionType","moduleName","params":{...},"reply"}`, and `params` is a free-form
 * `{string:string}` map — exactly the `fieldKey -> value` shape we want. Each predicted value is then
 * validated against the field's type/options before it is emitted as a fill.
 */
@AssistedInject
class FormAgentViewModel(
    @Assisted private val entityType: String,
    private val providerRegistry: ProviderRegistry,
    private val configRepository: ConfigRepository,
    private val speechAdapters: SpeechAdapterRegistry,
    private val micPermission: MicPermissionController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FormAgentUiState(voiceAvailable = speechAdapters.sttOptions.isNotEmpty()),
    )
    val uiState: StateFlow<FormAgentUiState> = _uiState.asStateFlow()

    /**
     * One-off fill events the host screen applies to its form state (`viewModel.updateField(...)`).
     * Buffered so a multi-field prediction never drops events if the collector is momentarily busy.
     */
    private val _fills = MutableSharedFlow<FormFieldFill>(extraBufferCapacity = 64)
    val fills: SharedFlow<FormFieldFill> = _fills.asSharedFlow()

    private var listenJob: Job? = null
    private var sttInUse: SpeechToText? = null

    /**
     * Start voice capture: stream the recognizer's partials into the UI and, on the final transcript,
     * hand the whole utterance to [submit] — which predicts values for every field the user mentioned
     * (name, phone, PAN, address, …) and fills them in one shot. Errors fall back to text input.
     */
    fun startVoice() {
        if (_uiState.value.isListening) return
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            val stt = speechAdapters.stt()
            if (stt == null) {
                appendAssistant("Voice input isn't available here — type the details instead.")
                return@launch
            }
            if (!micPermission.ensureMicGranted()) {
                appendAssistant("Microphone access is needed for voice input. You can also type the details.")
                return@launch
            }
            sttInUse = stt
            _uiState.update { it.copy(isListening = true, partialTranscript = "") }
            var finalText: String? = null
            stt.listen()
                .catch { /* surfaced as null below → user can retry or type */ }
                .collect { event ->
                    when (event) {
                        is SttEvent.Partial -> _uiState.update { it.copy(partialTranscript = event.text) }
                        is SttEvent.Final -> finalText = event.text.trim()
                        is SttEvent.Error -> { /* finalText stays null */ }
                        SttEvent.EndOfSpeech -> { /* keep collecting until Final/Error */ }
                    }
                }
            _uiState.update { it.copy(isListening = false, partialTranscript = "") }
            finalText?.takeIf { it.isNotBlank() }?.let { submit(it) }
        }
    }

    /** Stop an in-progress recognition early; whatever was heard so far is committed and submitted. */
    fun stopVoice() {
        sttInUse?.stop()
    }

    /** Submit a conversation turn (typed, or a finalized voice transcript). */
    fun submit(message: String) {
        val text = message.trim()
        if (text.isBlank() || _uiState.value.isProcessing) return
        _uiState.update {
            it.copy(
                messages = it.messages + FormChatMessage(text = text, isUser = true),
                isProcessing = true,
            )
        }
        viewModelScope.launch {
            runCatching { predict(text) }
                .onFailure { appendAssistant("Sorry, I couldn't process that — please try rephrasing.") }
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    private suspend fun predict(userMessage: String) {
        val schema = configRepository.observeSchema(entityType).first()
        val fields = schema?.visibleFields()?.filter { it.enabled } ?: emptyList()
        if (fields.isEmpty()) {
            appendAssistant("No editable fields are configured for this form yet.")
            return
        }

        val engine = providerRegistry.engineOrNull()
        if (engine == null) {
            appendAssistant(
                "The on-device assistant isn't ready. Open the AI assistant to download a model, " +
                    "then try filling the form by voice or text.",
            )
            return
        }

        val prompt = buildPrompt(userMessage, fields)
        val output = buildOutputSchema(fields)
        val raw = engine.generateConstrained(prompt, output)

        val parsed = extractJsonObject(raw)?.let { obj ->
            runCatching { JSON.decodeFromString<RawFill>(obj) }.getOrNull()
        }
        if (parsed == null) {
            val prose = stripJsonObject(raw)
            appendAssistant(prose.ifBlank { "I couldn't work out any field values from that." })
            return
        }

        val byKey = fields.associateBy { it.fieldKey }
        val applied = LinkedHashMap<String, String>()
        for ((key, rawValue) in parsed.params) {
            val field = byKey[key] ?: continue
            val value = coerce(field, rawValue) ?: continue
            applied[field.fieldKey] = value
        }

        if (applied.isEmpty()) {
            val reply = parsed.reply?.takeIf { it.isNotBlank() }
                ?: "I didn't catch any values to fill. Try, for example, \"name is Acme, city Mumbai\"."
            appendAssistant(reply)
            return
        }

        applied.forEach { (fieldKey, value) -> _fills.emit(FormFieldFill(fieldKey, value)) }

        val summary = applied.entries.joinToString(", ") { (key, value) ->
            "${byKey[key]?.displayName ?: key} = $value"
        }
        val reply = parsed.reply?.takeIf { it.isNotBlank() }?.let { "$it\n" } ?: ""
        appendAssistant("${reply}Filled: $summary")
    }

    /** Validate / normalize a predicted value for [field]; null = reject (don't fill). */
    private fun coerce(field: FormField, rawValue: String): String? {
        val value = rawValue.trim()
        if (value.isBlank()) return null
        return when (field.dataType) {
            FieldDataType.BOOLEAN -> when (value.lowercase()) {
                "true", "yes", "y", "1", "on", "enabled" -> "true"
                "false", "no", "n", "0", "off", "disabled" -> "false"
                else -> null
            }
            FieldDataType.CHOICE, FieldDataType.MULTI_CHOICE -> {
                val options = field.enumValues.orEmpty()
                if (options.isEmpty()) value // dynamic options: trust the model, host validates
                else options.firstOrNull { it.equals(value, ignoreCase = true) }
            }
            FieldDataType.NUMBER -> {
                val cleaned = value.filter { it.isDigit() || it == '.' || it == '-' }
                cleaned.toDoubleOrNull()?.let { value } // keep original text if it parses as a number
            }
            else -> value
        }
    }

    private fun appendAssistant(text: String) {
        _uiState.update { it.copy(messages = it.messages + FormChatMessage(text = text, isUser = false)) }
    }

    /**
     * Minimal, memory-free prompt: just the allowed field keys (+ a short label and choice options)
     * and the single utterance. No conversation history, no verbose rules — smaller prompt = faster
     * on-device inference. The constrained [OutputSchema] guarantees valid JSON, so we don't spend
     * tokens describing the JSON shape beyond one example line.
     */
    private fun buildPrompt(userMessage: String, fields: List<FormField>): String = buildString {
        append("Extract ").append(entityType).appendLine(" details the user states. Output ONE JSON object only:")
        appendLine("{\"params\":{\"field_key\":\"value\"}}")
        appendLine("Use only these keys; include a key only if the user said its value:")
        fields.forEach { field ->
            append("- ").append(field.fieldKey).append(" (").append(field.displayName).append(')')
            if (field.dataType.isChoice) {
                val options = field.enumValues.orEmpty()
                if (options.isNotEmpty()) append(" = one of [").append(options.joinToString(", ")).append(']')
            }
            appendLine()
        }
        append("User: ").append(userMessage)
    }

    private fun buildOutputSchema(fields: List<FormField>): OutputSchema = OutputSchema(
        intents = listOf("action"),
        actionTypes = listOf("UPDATE"),
        modules = listOf(entityType),
        paramsByModule = mapOf(entityType to fields.map { it.fieldKey }),
    )

    /** Pull the first balanced `{...}` block out of the model's text (it may add prose around JSON). */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until raw.length) {
            when (raw[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun stripJsonObject(raw: String): String {
        val obj = extractJsonObject(raw) ?: return raw.trim()
        return raw.replace(obj, "").trim()
    }

    override fun onCleared() {
        listenJob?.cancel()
        sttInUse?.stop()
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(entityType: String): FormAgentViewModel
    }

    @Serializable
    private data class RawFill(
        val params: Map<String, String> = emptyMap(),
        val reply: String? = null,
    )

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

/** A single predicted form fill the host screen applies to its form state. */
data class FormFieldFill(
    val fieldKey: String,
    val value: String,
)

/** A message shown in the form-assistant chat. */
data class FormChatMessage(
    val text: String,
    val isUser: Boolean,
)

data class FormAgentUiState(
    val messages: List<FormChatMessage> = emptyList(),
    val isProcessing: Boolean = false,
    /** True while the microphone is open and streaming the live transcript. */
    val isListening: Boolean = false,
    /** Live (interim) speech transcript shown while listening. */
    val partialTranscript: String = "",
    /** Whether this platform has any speech-to-text engine (false on Desktop). */
    val voiceAvailable: Boolean = false,
)
