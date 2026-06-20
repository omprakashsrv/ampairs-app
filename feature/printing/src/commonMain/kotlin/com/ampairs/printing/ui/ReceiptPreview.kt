package com.ampairs.printing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintElement

/**
 * A faithful, monospaced rendering of the resolved thermal [PrintElement]s — the receipt counterpart
 * to the page WebView preview. Because a thermal printout *is* a top-to-bottom stream of styled lines,
 * this Compose layout matches the ESC/POS output closely enough to edit against.
 */
@Composable
fun ReceiptPreview(elements: List<PrintElement>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        elements.forEach { element -> ReceiptElement(element) }
    }
}

@Composable
private fun ReceiptElement(element: PrintElement) {
    val fmt = PlainValueFormatter
    when (element) {
        is PrintElement.TextLine -> MonoText(
            text = fmt.format(element.content),
            bold = element.style.bold,
            align = element.style.align,
            scale = element.style.heightScale,
        )

        is PrintElement.KeyValueRow -> Row(Modifier.fillMaxWidth()) {
            MonoText(fmt.format(element.label), bold = element.style.bold, modifier = Modifier.weight(1f))
            MonoText(fmt.format(element.value), bold = element.style.bold, align = Align.RIGHT, modifier = Modifier.weight(1f))
        }

        is PrintElement.Table -> {
            Row(Modifier.fillMaxWidth()) {
                element.columns.forEach { col ->
                    MonoText(col.title, bold = true, align = col.style.align, modifier = Modifier.weight(col.style.weight.toFloat()))
                }
            }
            element.rows.forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    element.columns.forEachIndexed { i, col ->
                        MonoText(
                            text = row.getOrNull(i)?.let { fmt.format(it) } ?: "",
                            align = col.style.align,
                            modifier = Modifier.weight(col.style.weight.toFloat()),
                        )
                    }
                }
            }
        }

        is PrintElement.Grid -> element.cells.forEach { cell ->
            val text = if (cell.label.isNotBlank()) "${cell.label}: ${fmt.format(cell.value)}" else fmt.format(cell.value)
            MonoText(text)
        }

        is PrintElement.Divider -> MonoText(element.char.toString().repeat(32))
        is PrintElement.Spacer -> Spacer(Modifier.height((element.lines * 8).dp))
        is PrintElement.Feed -> Spacer(Modifier.height((element.lines * 8).dp))
        is PrintElement.Image -> MonoText("[ logo ]", align = Align.CENTER)
        is PrintElement.Barcode -> MonoText("|| ||| | || ${element.value}", align = Align.CENTER)
        is PrintElement.Qr -> MonoText("[QR] ${element.value}", align = Align.CENTER)
        is PrintElement.Cut -> MonoText("- - - - - - - - - - - -", align = Align.CENTER)
        is PrintElement.CashDrawerKick -> Unit
    }
}

@Composable
private fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    bold: Boolean = false,
    align: Align = Align.LEFT,
    scale: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = Color.Black,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontSize = (13 * scale.coerceIn(1, 2)).sp,
        textAlign = when (align) {
            Align.LEFT -> TextAlign.Start
            Align.CENTER -> TextAlign.Center
            Align.RIGHT -> TextAlign.End
        },
    )
}
