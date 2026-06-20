package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.engine.PrintEngine
import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PrintElement
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateBlock
import com.ampairs.printing.core.model.TemplateStyle
import com.ampairs.printing.core.render.RenderedOutput
import com.ampairs.printing.data.repository.TemplateRepository
import com.ampairs.printing.preview.SampleValueProvider
import com.ampairs.printing.render.html.HtmlRenderer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateEditUiState(
    val loading: Boolean = true,
    val template: Template? = null,
    val saving: Boolean = false,
    /** Resolved elements for the thermal/label receipt preview. */
    val previewElements: List<PrintElement> = emptyList(),
    /** Rendered HTML for the page (inkjet/laser) preview; null for non-page templates. */
    val previewHtml: String? = null,
)

/**
 * Editor for one [Template]. Holds a working copy in [uiState]; every intent rewrites it immutably.
 * On save the working copy is persisted via [TemplateRepository.save] (Room write + PENDING_PUSH),
 * so the edited formatting syncs to the backend like any other entity.
 */
@AssistedInject
class TemplateEditViewModel(
    @Assisted val templateId: String,
    private val repository: TemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateEditUiState())
    val uiState: StateFlow<TemplateEditUiState> = _uiState.asStateFlow()

    private val _savedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val savedEvent: SharedFlow<Unit> = _savedEvent.asSharedFlow()

    // Real renderer + sample data so the live preview matches the actual printout (page → HTML,
    // thermal/label → resolved elements). Instantiated directly — both are pure, dependency-free.
    private val engine = PrintEngine()
    private val htmlRenderer = HtmlRenderer()
    private val sampleProvider = SampleValueProvider()
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val template = repository.getTemplate(templateId)
            _uiState.update { it.copy(loading = false, template = template) }
            refreshPreview()
        }
    }

    fun updateName(name: String) = mutate { it.copy(name = name) }

    /** Update the primary editable text of a block (StaticText.text, KeyValue.label, etc.). */
    fun updateBlockText(index: Int, text: String) = updateBlock(index) { block ->
        when (block) {
            is TemplateBlock.StaticText -> block.copy(text = text)
            is TemplateBlock.KeyValue -> block.copy(label = text)
            is TemplateBlock.Divider -> block.copy(char = text.firstOrNull() ?: '-')
            is TemplateBlock.Spacer -> block.copy(lines = text.toIntOrNull()?.coerceIn(1, 10) ?: block.lines)
            else -> block
        }
    }

    fun toggleBold(index: Int) = updateStyle(index) { it.copy(bold = !it.bold) }

    fun setAlign(index: Int, align: Align) = updateStyle(index) { it.copy(align = align) }

    /** Toggle a block between normal (1×) and large (2×) print size. */
    fun toggleLarge(index: Int) = updateStyle(index) { style ->
        val large = style.widthScale > 1 || style.heightScale > 1
        if (large) style.copy(widthScale = 1, heightScale = 1)
        else style.copy(widthScale = 2, heightScale = 2)
    }

    fun moveUp(index: Int) = reorder(index, index - 1)

    fun moveDown(index: Int) = reorder(index, index + 1)

    fun removeBlock(index: Int) = mutate { template ->
        template.copy(blocks = template.blocks.toMutableList().apply { removeAt(index) })
    }

    fun addText() = appendBlock(TemplateBlock.StaticText(text = ""))

    fun addDivider() = appendBlock(TemplateBlock.Divider())

    fun addSpacer() = appendBlock(TemplateBlock.Spacer(lines = 1))

    fun save() {
        val template = _uiState.value.template ?: return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            repository.save(template)
            _uiState.update { it.copy(saving = false) }
            _savedEvent.tryEmit(Unit)
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun appendBlock(block: TemplateBlock) = mutate { it.copy(blocks = it.blocks + block) }

    private fun reorder(from: Int, to: Int) = mutate { template ->
        if (to < 0 || to >= template.blocks.size) return@mutate template
        template.copy(blocks = template.blocks.toMutableList().apply { add(to, removeAt(from)) })
    }

    private fun updateBlock(index: Int, transform: (TemplateBlock) -> TemplateBlock) = mutate { template ->
        if (index !in template.blocks.indices) return@mutate template
        template.copy(blocks = template.blocks.toMutableList().apply { this[index] = transform(this[index]) })
    }

    private fun updateStyle(index: Int, transform: (TemplateStyle) -> TemplateStyle) = updateBlock(index) { block ->
        when (block) {
            is TemplateBlock.StaticText -> block.copy(style = transform(block.style))
            is TemplateBlock.BoundText -> block.copy(style = transform(block.style))
            is TemplateBlock.KeyValue -> block.copy(style = transform(block.style))
            else -> block
        }
    }

    private fun mutate(transform: (Template) -> Template) {
        _uiState.update { state -> state.template?.let { state.copy(template = transform(it)) } ?: state }
        refreshPreview()
    }

    /** Re-resolve the working template against sample data through the real renderer. */
    private fun refreshPreview() {
        val template = _uiState.value.template ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val document = engine.build(template, "preview", sampleProvider)
            if (template.printerClass == PrinterClass.PAGE) {
                val html = (htmlRenderer.render(document, previewProfile(template)) as? RenderedOutput.Markup)?.html
                _uiState.update { it.copy(previewHtml = html, previewElements = emptyList()) }
            } else {
                _uiState.update { it.copy(previewElements = document.blocks, previewHtml = null) }
            }
        }
    }

    private fun previewProfile(template: Template) = PrinterProfile(
        id = "preview",
        name = "Preview",
        printerClass = template.printerClass,
        connectionType = ConnectionType.OS_PRINT,
        paper = template.paper,
    )

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(templateId: String): TemplateEditViewModel
    }
}
