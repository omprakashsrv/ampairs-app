package com.ampairs.printing.render

import com.ampairs.printing.core.engine.StaticHtmlResolver
import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.provider.LineValues
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticHtmlResolverTest {

    @Test
    fun resolvesScalarsNestedAndLineLoopWithEscaping() {
        val html = "<h1>{{seller_name}}</h1>" +
            "No:{{invoice_number}} To:{{customer.customer_name}}" +
            "<table>{{#lines}}<tr><td>{{description}}</td><td>{{quantity}}</td></tr>{{/lines}}</table>"

        val out = StaticHtmlResolver.resolve(
            html = html,
            standard = mapOf(
                "seller_name" to FieldValue.Text("Acme & Co"),
                "invoice_number" to FieldValue.Text("INV-1"),
            ),
            nested = mapOf(EntityRef.CUSTOMER to mapOf("customer_name" to FieldValue.Text("Rajesh"))),
            lines = listOf(
                LineValues(standard = mapOf("description" to FieldValue.Text("Widget"), "quantity" to FieldValue.Number(2.0, 0))),
                LineValues(standard = mapOf("description" to FieldValue.Text("Bolt"), "quantity" to FieldValue.Number(5.0, 0))),
            ),
        )

        assertTrue(out.contains("Acme &amp; Co"), "values must be HTML-escaped")
        assertTrue(out.contains("INV-1"))
        assertTrue(out.contains("Rajesh"))
        assertTrue(out.contains("<td>Widget</td>"))
        assertTrue(out.contains("<td>Bolt</td>"), "line loop must repeat per item")
        assertFalse(out.contains("{{"), "all tokens must be resolved")
    }

    @Test
    fun unknownTokenResolvesToEmpty() {
        val out = StaticHtmlResolver.resolve("a{{nope}}b", standard = emptyMap())
        assertTrue(out == "ab")
    }
}
