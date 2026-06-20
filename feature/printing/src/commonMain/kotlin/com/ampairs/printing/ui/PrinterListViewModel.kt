package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.PaperSpec
import com.ampairs.printing.core.model.PrinterCapabilities
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.data.repository.PrinterRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterListUiState(
    val printers: List<PrinterProfile> = emptyList(),
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PrinterListViewModel(
    private val repository: PrinterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterListUiState())
    val uiState: StateFlow<PrinterListUiState> = _uiState.asStateFlow()

    init {
        repository.observePrinters()
            .onEach { printers -> _uiState.update { it.copy(printers = printers) } }
            .launchIn(viewModelScope)
    }

    /** Add a network (WiFi/Ethernet) thermal printer reachable at host:port (default port 9100). */
    fun addNetworkPrinter(name: String, address: String, paperWidthMm: Int) {
        if (name.isBlank() || address.isBlank()) return
        viewModelScope.launch {
            val paper = if (paperWidthMm == 58) PaperSpec.THERMAL_58 else PaperSpec.THERMAL_80
            val profile = PrinterProfile(
                id = UidGenerator.generateUid("PRN"),
                name = name.trim(),
                printerClass = PrinterClass.THERMAL,
                connectionType = ConnectionType.NETWORK,
                address = address.trim(),
                paper = paper,
                capabilities = PrinterCapabilities(charsPerLine = paper.widthChars),
            )
            repository.savePrinter(profile)
        }
    }

    fun delete(printerId: String) {
        viewModelScope.launch { repository.removePrinter(printerId) }
    }

    /** Route the common document types to this printer. */
    fun setAsDefault(printerId: String) {
        viewModelScope.launch {
            repository.setRoute("invoice", printerId)
            repository.setRoute("receipt", printerId)
        }
    }
}
