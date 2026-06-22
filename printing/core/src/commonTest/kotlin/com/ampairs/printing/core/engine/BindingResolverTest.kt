package com.ampairs.printing.core.engine

import com.ampairs.printing.core.model.BindingScope
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldBinding
import com.ampairs.printing.core.model.FieldSource
import com.ampairs.printing.core.model.FieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BindingResolverTest {

    private val standard = mapOf(
        "invoice_number" to FieldValue.Text("INV-001"),
        "grand_total" to FieldValue.Money(1234.5),
    )
    private val custom = mapOf("vehicle_no" to "KA01AB1234")
    private val nested = mapOf(
        EntityRef.CUSTOMER to mapOf("name" to FieldValue.Text("Acme Corp")),
    )

    @Test
    fun resolvesStandardSelfField() {
        val b = FieldBinding(fieldKey = "invoice_number", source = FieldSource.STANDARD)
        assertEquals(FieldValue.Text("INV-001"), BindingResolver.resolve(b, standard, custom, nested))
    }

    @Test
    fun resolvesMoneyAsTypedValueNotRecomputed() {
        val b = FieldBinding(fieldKey = "grand_total", source = FieldSource.STANDARD)
        val v = BindingResolver.resolve(b, standard, custom, nested)
        assertEquals(FieldValue.Money(1234.5), v) // engine never recomputes; renderer formats with locale
    }

    @Test
    fun resolvesCustomAttribute() {
        val b = FieldBinding(fieldKey = "vehicle_no", source = FieldSource.CUSTOM)
        assertEquals(FieldValue.Text("KA01AB1234"), BindingResolver.resolve(b, standard, custom, nested))
    }

    @Test
    fun resolvesNestedEntityField() {
        val b = FieldBinding(fieldKey = "name", source = FieldSource.STANDARD, entityRef = EntityRef.CUSTOMER)
        assertEquals(FieldValue.Text("Acme Corp"), BindingResolver.resolve(b, standard, custom, nested))
    }

    @Test
    fun usesComputedValueWhenSourceComputed() {
        val b = FieldBinding(fieldKey = "upi_qr", source = FieldSource.COMPUTED)
        val computed = FieldValue.Text("upi://pay?pa=shop@bank&am=1234.50")
        assertEquals(computed, BindingResolver.resolve(b, standard, custom, nested, computed))
    }

    @Test
    fun fallsBackWhenMissingOrBlank() {
        val b = FieldBinding(fieldKey = "absent", source = FieldSource.STANDARD, fallback = "N/A")
        assertEquals(FieldValue.Text("N/A"), BindingResolver.resolve(b, standard, custom, nested))
    }

    @Test
    fun returnsEmptyWhenMissingAndNoFallback() {
        val b = FieldBinding(fieldKey = "absent", source = FieldSource.STANDARD)
        assertTrue(BindingResolver.resolve(b, standard, custom, nested) is FieldValue.Empty)
    }

    @Test
    fun lineScopeBindingResolvesAgainstLineMaps() {
        val b = FieldBinding(fieldKey = "qty", scope = BindingScope.LINE, source = FieldSource.STANDARD)
        val line = mapOf("qty" to FieldValue.Number(3.0, decimals = 0))
        assertEquals(FieldValue.Number(3.0, decimals = 0), BindingResolver.resolve(b, line, emptyMap()))
    }
}
