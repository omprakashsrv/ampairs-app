package com.ampairs.printing.render.escpos

import com.ampairs.printing.core.model.Align
import com.ampairs.printing.core.model.Symbology

/** ESC/POS control-byte constants and small command builders. */
internal object EscPos {
    const val ESC: Byte = 0x1B
    const val GS: Byte = 0x1D
    const val LF: Byte = 0x0A

    val INIT = byteArrayOf(ESC, '@'.code.toByte())

    fun align(a: Align): ByteArray = byteArrayOf(ESC, 'a'.code.toByte(), when (a) {
        Align.LEFT -> 0
        Align.CENTER -> 1
        Align.RIGHT -> 2
    })

    fun bold(on: Boolean): ByteArray = byteArrayOf(ESC, 'E'.code.toByte(), if (on) 1 else 0)

    /** GS ! n — width/height magnification (1..8 → 0..7 in the respective nibble). */
    fun size(widthScale: Int, heightScale: Int): ByteArray {
        val w = (widthScale.coerceIn(1, 8) - 1) shl 4
        val h = (heightScale.coerceIn(1, 8) - 1)
        return byteArrayOf(GS, '!'.code.toByte(), (w or h).toByte())
    }

    fun feed(lines: Int): ByteArray = ByteArray(lines.coerceAtLeast(0)) { LF }

    /** GS V — partial (1) or full (0) cut. */
    fun cut(partial: Boolean): ByteArray = byteArrayOf(GS, 'V'.code.toByte(), if (partial) 1 else 0)

    /** ESC p — pulse the cash drawer (pin 0). */
    val CASH_DRAWER = byteArrayOf(ESC, 'p'.code.toByte(), 0, 25, (250).toByte())

    /** ESC B n t — buzzer/beep (n cycles, t × 100 ms each). No-op on printers without a buzzer. */
    fun beep(times: Int = 1, durationUnits: Int = 3): ByteArray =
        byteArrayOf(ESC, 'B'.code.toByte(), times.coerceIn(1, 9).toByte(), durationUnits.coerceIn(1, 9).toByte())

    // ---- 1D barcodes (GS k) -------------------------------------------------
    // Height: GS h n · module width: GS w n · HRI text below: GS H 2

    fun barcodeHeight(dots: Int): ByteArray =
        byteArrayOf(GS, 'h'.code.toByte(), dots.coerceIn(1, 255).toByte())

    fun barcodeWidth(module: Int): ByteArray =
        byteArrayOf(GS, 'w'.code.toByte(), module.coerceIn(2, 6).toByte())

    /** GS H n — Human Readable Interpretation position: 0 none, 1 above, 2 below, 3 both. */
    fun barcodeHriBelow(): ByteArray = byteArrayOf(GS, 'H'.code.toByte(), 2)

    /**
     * GS k m n d… — print a 1D barcode using the length-prefixed form (m = 65..73).
     * CODE128 payloads are prefixed with the `{B` code-set selector per the spec. Returns null for
     * symbologies that are not 1D (e.g. [Symbology.QR], which is emitted via [EscPosRenderer]'s QR path).
     */
    fun barcode(value: String, symbology: Symbology): ByteArray? {
        val (m, payload) = when (symbology) {
            Symbology.CODE128 -> 73 to ("{B".encodeToByteArray() + value.encodeToByteArray())
            Symbology.EAN13 -> 67 to value.encodeToByteArray()
            Symbology.UPCA -> 65 to value.encodeToByteArray()
            Symbology.QR -> return null
        }
        val n = payload.size.coerceIn(0, 255)
        return byteArrayOf(GS, 'k'.code.toByte(), m.toByte(), n.toByte()) + payload.copyOf(n)
    }

    fun text(s: String): ByteArray = s.encodeToByteArray()
    val NEWLINE = byteArrayOf(LF)
}
