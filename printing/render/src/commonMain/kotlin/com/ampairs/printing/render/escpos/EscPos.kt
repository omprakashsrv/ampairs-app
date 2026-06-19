package com.ampairs.printing.render.escpos

import com.ampairs.printing.core.model.Align

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

    fun text(s: String): ByteArray = s.encodeToByteArray()
    val NEWLINE = byteArrayOf(LF)
}
