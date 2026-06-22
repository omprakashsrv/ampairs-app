package com.ampairs.printing.core.render

import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.PrintDocument
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrinterProfile
import com.ampairs.printing.core.model.ValueFormatter

/**
 * Turns a [PrintDocument] into output for one printer class. Implementations MUST be pure functions
 * (no IO) so they can be covered by golden-file tests (FR/SC reliability). The locale-aware
 * [ValueFormatter] is supplied by the caller; the renderer never recomputes amounts.
 */
interface Renderer {
    val printerClass: PrinterClass

    fun render(
        document: PrintDocument,
        profile: PrinterProfile,
        formatter: ValueFormatter = PlainValueFormatter,
    ): RenderedOutput
}

/** Renderer output, dispatched to the matching transport family. */
sealed interface RenderedOutput {
    /** Raw device bytes (ESC/POS, TSPL/ZPL) → [com.ampairs.printing.core.transport.PrinterTransport]. */
    class Bytes(val data: ByteArray) : RenderedOutput

    /** HTML markup → OS print service. */
    data class Markup(val html: String) : RenderedOutput

    /** Rendered PDF → share/export or silent print. */
    class Pdf(val data: ByteArray) : RenderedOutput
}
