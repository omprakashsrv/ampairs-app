package com.ampairs.common.agent

import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One resolved line in the conversational document draft (cart). */
data class DraftLine(
    val productId: String,
    val productName: String,
    val quantity: Double,
    /** Per-unit price already resolved at add time (override, else the product's selling price). */
    val unitPrice: Double,
) {
    val lineSubtotal: Double get() = quantity * unitPrice
}

/**
 * In-memory conversational document draft ("cart") shared by the agent's cart/invoice/order handlers
 * so the user can keep adding items across turns and then finalize into an invoice or order.
 *
 * `@SingleIn(WorkspaceScope::class)` — one cart per active workspace, reset on workspace switch. It is
 * deliberately NOT persisted: a draft lives only for the current chat session until it is finalized
 * (then cleared) or explicitly cleared. Single-message processing means no locking is needed.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class DraftDocumentStore {

    private val _lines = mutableListOf<DraftLine>()
    val lines: List<DraftLine> get() = _lines.toList()

    var customerId: String? = null
        private set
    var customerName: String? = null
        private set

    private val _state = MutableStateFlow(this)
    val state: StateFlow<DraftDocumentStore> = _state.asStateFlow()

    val isEmpty: Boolean get() = _lines.isEmpty()
    val itemCount: Int get() = _lines.size
    val totalQuantity: Double get() = _lines.sumOf { it.quantity }

    /** Indicative pre-tax subtotal; the authoritative total is computed at finalize via the tax engine. */
    val subtotal: Double get() = _lines.sumOf { it.lineSubtotal }

    fun addLine(line: DraftLine) {
        _lines.add(line)
        _state.value = this
    }

    fun setCustomer(id: String, name: String) {
        customerId = id
        customerName = name
        _state.value = this
    }

    /** Remove the most recently added line, or null when the cart is already empty. */
    fun removeLast(): DraftLine? {
        val result = if (_lines.isEmpty()) null else _lines.removeAt(_lines.lastIndex)
        if (result != null) _state.value = this
        return result
    }

    fun clear() {
        _lines.clear()
        customerId = null
        customerName = null
        _state.value = this
    }
}
