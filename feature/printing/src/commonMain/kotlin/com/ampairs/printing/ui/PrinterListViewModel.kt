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
import com.ampairs.printing.core.spool.SendOutcome
import com.ampairs.printing.core.transport.DiscoveredPrinter
import com.ampairs.printing.core.transport.PrintPermissions
import com.ampairs.printing.core.transport.PrinterDiscoverer
import com.ampairs.printing.data.repository.PrinterRepository
import com.ampairs.printing.service.PrintService
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
    val message: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PrinterListViewModel(
    private val repository: PrinterRepository,
    private val discoverer: PrinterDiscoverer,
    private val printService: PrintService,
    private val permissions: PrintPermissions,
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
            // Request Bluetooth up front so bonded BT printers can be listed on API 31+.
            permissions.ensure(ConnectionType.BLUETOOTH)
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

    /** Print a test page on the given printer to verify it works end-to-end. */
    fun testPrint(printerId: String) {
        viewModelScope.launch {
            val profile = repository.getProfile(printerId) ?: return@launch
            if (!permissions.ensure(profile.connectionType)) {
                _uiState.update { it.copy(message = "Permission required for ${profile.connectionType.name}") }
                return@launch
            }
            val message = runCatching { printService.testPrint(profile) }.fold(
                onSuccess = { outcome ->
                    if (outcome == SendOutcome.SENT_UNCONFIRMED || outcome == SendOutcome.CONFIRMED) {
                        "Test sent to ${profile.name}"
                    } else {
                        "Test failed: ${outcome.name}"
                    }
                },
                onFailure = { it.message ?: "Test failed" },
            )
            _uiState.update { it.copy(message = message) }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    /**
     * Add a printer of any class. Manual add targets network/raw printers (thermal & label over TCP);
     * inkjet/laser printers come from [discover] (the OS print service), not here.
     */
    fun addPrinter(
        name: String,
        printerClass: PrinterClass,
        address: String?,
        paperWidthMm: Int = 80,
        labelWidthMm: Double = 50.0,
        labelHeightMm: Double = 25.0,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val paper = when (printerClass) {
                PrinterClass.THERMAL -> if (paperWidthMm == 58) PaperSpec.THERMAL_58 else PaperSpec.THERMAL_80
                PrinterClass.PAGE -> PaperSpec.Page()
                PrinterClass.LABEL -> PaperSpec.Label(labelWidthMm, labelHeightMm)
            }
            val capabilities = if (paper is PaperSpec.Thermal) {
                PrinterCapabilities(charsPerLine = paper.widthChars)
            } else {
                PrinterCapabilities()
            }
            val profile = PrinterProfile(
                id = UidGenerator.generateUid("PRN"),
                name = name.trim(),
                printerClass = printerClass,
                connectionType = ConnectionType.NETWORK,
                address = address?.trim(),
                paper = paper,
                capabilities = capabilities,
            )
            repository.savePrinter(profile)
        }
    }

    fun delete(printerId: String) {
        viewModelScope.launch { repository.removePrinter(printerId) }
    }
}
