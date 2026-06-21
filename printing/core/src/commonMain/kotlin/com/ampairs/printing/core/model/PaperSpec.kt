package com.ampairs.printing.core.model

import kotlinx.serialization.Serializable

/** Paper geometry per printer class. */
@Serializable
sealed interface PaperSpec {
    /** Continuous thermal roll. [widthChars] at the default font; [widthDots] for raster images. */
    @Serializable
    data class Thermal(val widthChars: Int = 48, val widthDots: Int = 576) : PaperSpec

    /** Cut-sheet page printer (inkjet/laser). */
    @Serializable
    data class Page(
        val size: PageSize = PageSize.A4,
        val orientation: Orientation = Orientation.PORTRAIT,
    ) : PaperSpec

    /** Fixed-size label media. */
    @Serializable
    data class Label(val widthMm: Double, val heightMm: Double) : PaperSpec

    companion object {
        val THERMAL_58 = Thermal(widthChars = 32, widthDots = 384)
        val THERMAL_80 = Thermal(widthChars = 48, widthDots = 576)
    }
}
