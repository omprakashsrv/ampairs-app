package com.ampairs.subscription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.model.InvoiceSummary
import com.ampairs.subscription.domain.model.PaymentLinkResponse
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.subscription.repository.InvoiceRepository
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock

/**
 * ViewModel for invoice operations
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
@OptIn(kotlin.time.ExperimentalTime::class)
class InvoiceViewModel(
    private val repository: InvoiceRepository,
) : ViewModel() {

    companion object {
        // Cache duration for invoice data (3 minutes - invoices change less frequently)
        private val CACHE_DURATION = 3.minutes

        // Singleton cache for invoice summary
        private val lastSummarySync = mutableMapOf<String, kotlin.time.Instant>()

        /**
         * Check if summary needs refresh
         */
        private fun needsSummaryRefresh(workspaceId: String): Boolean {
            val lastSync = lastSummarySync[workspaceId] ?: return true
            val now = Clock.System.now()
            return (now - lastSync) > CACHE_DURATION
        }

        /**
         * Update summary sync time
         */
        private fun updateSummaryTime(workspaceId: String) {
            lastSummarySync[workspaceId] = Clock.System.now()
        }

        /**
         * Clear cache when payment is made or invoice status changes
         */
        fun clearSummaryCache(workspaceId: String) {
            lastSummarySync.remove(workspaceId)
        }
    }

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _selectedInvoice = MutableStateFlow<Invoice?>(null)
    val selectedInvoice: StateFlow<Invoice?> = _selectedInvoice.asStateFlow()

    private val _invoiceSummary = MutableStateFlow<InvoiceSummary?>(null)
    val invoiceSummary: StateFlow<InvoiceSummary?> = _invoiceSummary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _paymentLink = MutableStateFlow<PaymentLinkResponse?>(null)
    val paymentLink: StateFlow<PaymentLinkResponse?> = _paymentLink.asStateFlow()

    private val _selectedStatus = MutableStateFlow<InvoiceStatus?>(null)
    val selectedStatus: StateFlow<InvoiceStatus?> = _selectedStatus.asStateFlow()

    /**
     * Get current workspace ID
     */
    private fun getWorkspaceId(): String = ""

    /**
     * Load invoices for current workspace
     */
    fun loadInvoices(status: InvoiceStatus? = null) {
        _selectedStatus.value = status
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getInvoices(
                workspaceId = getWorkspaceId(),
                status = status
            ).collect { result ->
                _isLoading.value = false
                result.fold(
                    onSuccess = { invoices ->
                        _invoices.value = invoices
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load invoices"
                    }
                )
            }
        }
    }

    /**
     * Load invoice summary (with caching)
     */
    fun loadInvoiceSummary(force: Boolean = false) {
        val workspaceId = getWorkspaceId()

        // Skip if cache is still valid (unless forced)
        if (!force && !needsSummaryRefresh(workspaceId)) {
            return
        }

        viewModelScope.launch {
            repository.getInvoiceSummary(workspaceId).collect { result ->
                result.fold(
                    onSuccess = { summary ->
                        _invoiceSummary.value = summary
                        updateSummaryTime(workspaceId)
                    },
                    onFailure = { exception ->
                        println("Failed to load summary: ${exception.message}")
                    }
                )
            }
        }
    }

    /**
     * Load single invoice by UID
     */
    fun loadInvoice(invoiceUid: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getInvoice(invoiceUid).collect { result ->
                _isLoading.value = false
                result.fold(
                    onSuccess = { invoice ->
                        _selectedInvoice.value = invoice
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Failed to load invoice"
                    }
                )
            }
        }
    }

    /**
     * Pay an invoice
     */
    fun payInvoice(invoiceUid: String, useAutoCharge: Boolean = false) {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            _error.value = null
            _paymentLink.value = null

            val result = repository.payInvoice(invoiceUid, useAutoCharge)
            _isProcessingPayment.value = false

            result.fold(
                onSuccess = { paymentLinkResponse ->
                    if (paymentLinkResponse.paymentLinkUrl.isNotEmpty()) {
                        // Payment link generated, user needs to complete payment
                        _paymentLink.value = paymentLinkResponse
                    } else {
                        // Auto-charge was successful - clear cache and reload
                        clearSummaryCache(getWorkspaceId())
                        _error.value = "Payment processed successfully"
                        // Reload invoice to get updated status
                        loadInvoice(invoiceUid)
                        // Reload invoice list
                        loadInvoices(_selectedStatus.value)
                        // Force reload summary
                        loadInvoiceSummary(force = true)
                    }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to process payment"
                }
            )
        }
    }

    /**
     * Retry failed payment
     */
    fun retryPayment(invoiceUid: String) {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            _error.value = null
            _paymentLink.value = null

            val result = repository.retryPayment(invoiceUid)
            _isProcessingPayment.value = false

            result.fold(
                onSuccess = { paymentLinkResponse ->
                    if (paymentLinkResponse.paymentLinkUrl.isNotEmpty()) {
                        _paymentLink.value = paymentLinkResponse
                    } else {
                        // Payment successful - clear cache and reload
                        clearSummaryCache(getWorkspaceId())
                        _error.value = "Payment processed successfully"
                        loadInvoice(invoiceUid)
                        loadInvoices(_selectedStatus.value)
                        loadInvoiceSummary(force = true)
                    }
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to retry payment"
                }
            )
        }
    }

    /**
     * Clear payment link
     */
    fun clearPaymentLink() {
        _paymentLink.value = null
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Filter invoices by status
     */
    fun filterByStatus(status: InvoiceStatus?) {
        loadInvoices(status)
    }

    /**
     * Refresh invoices
     */
    fun refresh() {
        loadInvoices(_selectedStatus.value)
        loadInvoiceSummary()
    }
}
