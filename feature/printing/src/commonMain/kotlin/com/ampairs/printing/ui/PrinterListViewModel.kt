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
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrinterDiscoverer
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
    val discovered: List<DiscoveredPrinter> = emptyList(),
    val discovering: Boolean = false,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PrinterListViewModel(
    private val repository: PrinterRepository,
    private val discoverer: PrinterDiscoverer,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrinterListUiState())
    val uiState: StateFlow<PrinterListUiState> = _uiState.asStateFlow()

    init {
        repository.observePrinters()
            .onEach { printers -> _uiState.update { it.copy(printers = printers) } }
            .launchIn(viewModelScope)
    }

    /** Scan the device for USB / Bluetooth / system printers (platform-dependent). */
    fun discover() {
        viewModelScope.launch {
            _uiState.update { it.copy(discovering = true) }
            val found = runCatching { discoverer.discover() }.getOrDefault(emptyList())
            _uiState.update { it.copy(discovering = false, discovered = found) }
        }
    }

    fun clearDiscovered() {
        _uiState.update { it.copy(discovered = emptyList()) }
    }

    /** Persist a discovered printer as a usable profile, inferring its class from the channel. */
    fun addDiscoveredPrinter(discovered: DiscoveredPrinter) {
        viewModelScope.launch {
            val printerClass = discovered.suggestedClass
                ?: if (discovered.connectionType == ConnectionType.OS_PRINT) {
                    PrinterClass.PAGE
                } else {
                    PrinterClass.THERMAL
                }
            val paper = if (printerClass == PrinterClass.THERMAL) PaperSpec.THERMAL_80 else PaperSpec.Page()
            val profile = PrinterProfile(
                id = UidGenerator.generateUid("PRN"),
                name = discovered.name,
                printerClass = printerClass,
                connectionType = discovered.connectionType,
                address = discovered.address,
                paper = paper,
                capabilities = if (printerClass == PrinterClass.THERMAL) {
                    PrinterCapabilities(charsPerLine = (paper as PaperSpec.Thermal).widthChars)
                } else {
                    PrinterCapabilities()
                },
            )
            repository.savePrinter(profile)
            _uiState.update { it.copy(discovered = emptyList()) }
        }
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
