package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FilePickerResult
import com.ampairs.file.api.FileRepository
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.core.model.TemplateKind
import com.ampairs.printing.data.repository.TemplateRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateListUiState(
    val templates: List<Template> = emptyList(),
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TemplateListViewModel(
    private val repository: TemplateRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.seedDefaultsIfEmpty() }
        repository.observeAll()
            .onEach { templates -> _uiState.update { it.copy(templates = templates) } }
            .launchIn(viewModelScope)
    }

    /** Re-seed the built-in templates (overwrites by id) and flag them for backend push. */
    fun restoreDefaults() {
        viewModelScope.launch { repository.restoreDefaults() }
    }

    /** Choose [templateId] as the default for its document type + printer class. */
    fun setDefault(templateId: String) {
        viewModelScope.launch { repository.setDefault(templateId) }
    }

    /**
     * Import a static HTML template: pick a `.html` file, store it via the file module (which uploads
     * it to the backend), and create a STATIC [Template] referencing that file for [documentType].
     */
    fun importStaticTemplate(documentType: DocumentType) {
        viewModelScope.launch {
            val file = FileKit.openFilePicker(type = FileKitType.File(setOf("html", "htm"))) ?: return@launch
            val bytes = file.readBytes()
            val fileName = file.name
            val templateId = UidGenerator.generateUid("PTPL")
            val saved = fileRepository.saveLocally(
                entityType = FileEntityType.PRINT_TEMPLATE,
                entityUid = templateId,
                result = FilePickerResult(
                    fileName = fileName,
                    contentType = "text/html",
                    fileSize = file.size(),
                    imageData = bytes,
                ),
                isPrimary = true,
            ).getOrNull() ?: return@launch
            repository.save(
                Template(
                    id = templateId,
                    name = fileName.substringBeforeLast("."),
                    documentType = documentType,
                    printerClass = PrinterClass.PAGE,
                    paper = PaperSpec.Page(),
                    kind = TemplateKind.STATIC,
                    htmlFileUid = saved.uid,
                ),
            )
        }
    }
}
