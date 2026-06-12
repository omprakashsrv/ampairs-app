package com.ampairs.invoice.editor.entry

/**
 * Tokenizer for the fast-entry composer (spec 010 v2). Tokens are whitespace-separated and
 * order-free except product words:
 *  - bare number → quantity (first one only)
 *  - `N%` → line discount percent
 *  - `@N` or `₹N` → unit-price override
 *  - 8+ consecutive digits → kept as a word (barcode lookup)
 *  - everything else → product search word (lowercased)
 */
object EntryParser {

    private val discountToken = Regex("""^\d+(\.\d+)?%$""")
    private val priceToken = Regex("""^[@₹]\d+(\.\d+)?$""")
    private val barcodeToken = Regex("""^\d{8,}$""")
    private val numberToken = Regex("""^\d+(\.\d+)?$""")

    fun parse(query: String): ParsedEntry {
        var quantity: Double? = null
        var discount: Double? = null
        var price: Double? = null
        val words = mutableListOf<String>()
        query.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }.forEach { raw ->
            val t = raw.lowercase()
            when {
                discountToken.matches(t) -> discount = t.dropLast(1).toDouble()
                priceToken.matches(t) -> price = t.drop(1).toDouble()
                barcodeToken.matches(t) -> words.add(t)
                numberToken.matches(t) && quantity == null -> quantity = t.toDouble()
                else -> words.add(t)
            }
        }
        return ParsedEntry(quantity = quantity, discountPercent = discount, priceOverride = price, words = words)
    }
}
