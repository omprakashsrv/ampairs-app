package com.ampairs.invoice.print

import com.ampairs.common.format.toDecimal
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import kotlin.math.roundToLong

/** A per-rate row of the GST summary table (grouped by component name + percentage). */
internal data class TaxSummaryRow(val name: String, val percentage: Double, val taxable: Double, val amount: Double)

/** True when the document is inter-state (IGST) rather than intra-state (CGST + SGST). */
internal fun Invoice.isInterState(): Boolean =
    (taxInfos?.any { it.name.equals("IGST", ignoreCase = true) } == true) ||
        items.any { it -> it.taxInfos.any { c -> c.name.equals("IGST", ignoreCase = true) } }

/** Group every line's tax components by (name, percentage) for the summary table. */
internal fun Invoice.taxSummary(): List<TaxSummaryRow> =
    items.flatMap { item -> item.taxInfos.map { c -> Triple(c.name, c.percentage, item.basePrice to (c.value ?: 0.0)) } }
        .groupBy { it.first to it.second }
        .map { (key, rows) ->
            TaxSummaryRow(
                name = key.first,
                percentage = key.second,
                taxable = rows.sumOf { it.third.first },
                amount = rows.sumOf { it.third.second },
            )
        }
        .sortedWith(compareBy({ taxOrder(it.name) }, { it.percentage }))

private fun taxOrder(name: String): Int = when (name.uppercase()) {
    "CGST" -> 0; "SGST" -> 1; "IGST" -> 2; else -> 3
}

private fun esc(s: String?): String = (s ?: "")
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Build a self-contained, print-ready HTML document for a GST tax invoice. */
fun buildInvoiceHtml(invoice: Invoice, workspaceName: String, currencySymbol: String = "₹"): String {
    // Seller is the implicit current workspace (name passed in); only the buyer is stored.
    val buyer = invoice.customer
    val inter = invoice.isInterState()
    val date = DateTimeAdapter.toDateTimeString(invoice.invoiceDate)

    val rows = StringBuilder()
    invoice.items.forEachIndexed { i, item ->
        val unit = item.unitName.ifBlank { "" }
        rows.append(
            """
            <tr>
              <td>${i + 1}</td>
              <td class="l">${esc(item.description)}${if (item.variantSku != null) " <span class='muted'>(${esc(item.variantSku)})</span>" else ""}</td>
              <td>${esc(item.product?.taxCode)}</td>
              <td class="r">${item.quantity.toDecimal()} ${esc(unit)}</td>
              <td class="r">${item.price.toDecimal()}</td>
              <td class="r">${item.basePrice.toDecimal()}</td>
              <td class="r">${item.totalTax.toDecimal()}</td>
              <td class="r">${item.totalCost.toDecimal()}</td>
            </tr>
            """.trimIndent()
        )
    }

    val summary = StringBuilder()
    invoice.taxSummary().forEach { row ->
        summary.append(
            """
            <tr>
              <td class="l">${esc(row.name)} @ ${row.percentage.toDecimal()}%</td>
              <td class="r">${row.taxable.toDecimal()}</td>
              <td class="r">${row.amount.toDecimal()}</td>
            </tr>
            """.trimIndent()
        )
    }

    // The line table aggregates tax into a single column for compactness; the per-rate split lives in
    // the summary table below, which is the GST-compliant breakup.
    val discountTotal = invoice.discount?.sumOf { it.value } ?: 0.0

    return """
    <!DOCTYPE html><html><head><meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
      * { box-sizing: border-box; }
      body { font-family: -apple-system, Roboto, Arial, sans-serif; color:#1b1b1b; margin:0; padding:24px; font-size:12px; }
      .doc { max-width: 800px; margin:0 auto; border:1px solid #ccc; padding:20px; }
      h1 { font-size:16px; text-align:center; margin:0 0 4px; letter-spacing:1px; }
      .sub { text-align:center; color:#666; margin-bottom:12px; }
      .grid { display:flex; justify-content:space-between; gap:16px; margin-bottom:12px; }
      .box { flex:1; border:1px solid #e0e0e0; border-radius:6px; padding:10px; }
      .box h3 { margin:0 0 4px; font-size:11px; text-transform:uppercase; color:#888; }
      .muted { color:#888; }
      table { width:100%; border-collapse:collapse; margin-top:8px; }
      th, td { border:1px solid #e0e0e0; padding:6px 8px; text-align:center; }
      th { background:#f5f5f5; font-size:11px; }
      td.l, th.l { text-align:left; } td.r { text-align:right; }
      .totals { width:50%; margin-left:auto; margin-top:12px; }
      .totals td { border:none; padding:3px 8px; }
      .grand { font-weight:bold; font-size:14px; border-top:2px solid #333 !important; }
      .words { margin-top:10px; font-style:italic; }
      @media print { body { padding:0; } .doc { border:none; } }
    </style></head><body><div class="doc">
      <h1>TAX INVOICE</h1>
      <div class="sub">${esc(workspaceName)}</div>
      <div class="grid">
        <div class="box">
          <h3>Seller</h3>
          <div><b>${esc(invoice.sellerName?.takeIf { it.isNotBlank() } ?: workspaceName)}</b></div>
          <div>${esc(invoice.sellerAddress)}</div>
          <div>GSTIN: ${esc(invoice.sellerGst)}</div>
        </div>
        <div class="box">
          <h3>Bill To</h3>
          <div><b>${esc(buyer?.name)}</b></div>
          <div>${esc(buyer?.address)}</div>
          <div>${esc(listOfNotNull(buyer?.city, buyer?.state, buyer?.pincode).joinToString(", "))}</div>
          <div>GSTIN: ${esc(buyer?.gstNumber)}</div>
        </div>
        <div class="box">
          <h3>Invoice</h3>
          <div>No: <b>${esc(invoice.invoiceNumber ?: "—")}</b></div>
          <div>Date: ${esc(date)}</div>
          <div>Type: ${if (inter) "Inter-state (IGST)" else "Intra-state (CGST + SGST)"}</div>
        </div>
      </div>
      <table>
        <thead><tr>
          <th>#</th><th class="l">Particulars</th><th>HSN</th><th>Qty</th>
          <th>Rate</th><th>Taxable</th><th>Tax</th><th>Amount</th>
        </tr></thead>
        <tbody>$rows</tbody>
      </table>

      <table style="margin-top:14px;">
        <thead><tr><th class="l">Tax component</th><th>Taxable value</th><th>Tax amount</th></tr></thead>
        <tbody>$summary</tbody>
      </table>

      <table class="totals">
        <tr><td>Taxable subtotal</td><td class="r">${invoice.basePrice.toDecimal()}</td></tr>
        ${if (discountTotal > 0) "<tr><td>Discount</td><td class=\"r\">- ${discountTotal.toDecimal()}</td></tr>" else ""}
        <tr><td>Total tax</td><td class="r">${invoice.totalTax.toDecimal()}</td></tr>
        <tr class="grand"><td>Grand total</td><td class="r">${currencySymbol} ${invoice.totalCost.toDecimal()}</td></tr>
      </table>
      <div class="words">Amount in words: ${esc(amountInWords(invoice.totalCost))}</div>
      <p class="muted" style="margin-top:18px;">This is a computer-generated invoice.</p>
    </div></body></html>
    """.trimIndent()
}

/** Indian-format amount-in-words (rupees + paise). Pure Kotlin, KMP-safe. */
fun amountInWords(amount: Double): String {
    val rupees = amount.toLong()
    val paise = ((amount - rupees) * 100).roundToLong()
    val sb = StringBuilder()
    sb.append("Rupees ").append(if (rupees == 0L) "Zero" else numberToWordsIndian(rupees))
    if (paise > 0) sb.append(" and ").append(numberToWordsIndian(paise)).append(" Paise")
    sb.append(" Only")
    return sb.toString()
}

private val ones = arrayOf(
    "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
    "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
)
private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

private fun twoDigits(n: Int): String = when {
    n == 0 -> ""
    n < 20 -> ones[n]
    else -> (tens[n / 10] + (if (n % 10 != 0) " " + ones[n % 10] else "")).trim()
}

private fun threeDigits(n: Int): String {
    val h = n / 100
    val rest = n % 100
    val sb = StringBuilder()
    if (h > 0) sb.append(ones[h]).append(" Hundred")
    if (rest > 0) { if (h > 0) sb.append(" "); sb.append(twoDigits(rest)) }
    return sb.toString()
}

/** Indian grouping: crore, lakh, thousand, hundred. */
private fun numberToWordsIndian(value: Long): String {
    if (value == 0L) return "Zero"
    var n = value
    val parts = mutableListOf<String>()
    val crore = (n / 10_000_000).toInt(); n %= 10_000_000
    val lakh = (n / 100_000).toInt(); n %= 100_000
    val thousand = (n / 1000).toInt(); n %= 1000
    val hundreds = n.toInt()
    if (crore > 0) parts.add(numberToWordsIndian(crore.toLong()) + " Crore")
    if (lakh > 0) parts.add(twoDigits(lakh) + " Lakh")
    if (thousand > 0) parts.add(twoDigits(thousand) + " Thousand")
    if (hundreds > 0) parts.add(threeDigits(hundreds))
    return parts.joinToString(" ").trim()
}
