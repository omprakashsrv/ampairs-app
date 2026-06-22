package com.ampairs.printing.render.thermal

import com.ampairs.printing.core.model.Align
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThermalLineComposerTest {

    @Test
    fun padLeftRightCenter() {
        assertEquals("ab   ", ThermalLineComposer.pad("ab", 5, Align.LEFT))
        assertEquals("   ab", ThermalLineComposer.pad("ab", 5, Align.RIGHT))
        assertEquals(" ab  ", ThermalLineComposer.pad("ab", 5, Align.CENTER))
    }

    @Test
    fun wrapBreaksOnWords() {
        assertEquals(listOf("hello", "world"), ThermalLineComposer.wrap("hello world", 5))
    }

    @Test
    fun wrapHardSplitsLongWord() {
        assertEquals(listOf("abcde", "fgh"), ThermalLineComposer.wrap("abcdefgh", 5))
    }

    @Test
    fun composeRowIsExactlyTotalWidth() {
        val cells = listOf(
            ColumnText("Item", weight = 3, align = Align.LEFT),
            ColumnText("2", weight = 1, align = Align.RIGHT),
            ColumnText("240.00", weight = 2, align = Align.RIGHT),
        )
        val lines = ThermalLineComposer.composeRow(cells, 32)
        assertTrue(lines.all { it.length == 32 }, "every composed line must equal the grid width")
    }

    @Test
    fun composeRowRightAlignsAmountColumn() {
        val cells = listOf(
            ColumnText("Total", weight = 1, align = Align.LEFT),
            ColumnText("99.50", weight = 1, align = Align.RIGHT),
        )
        val line = ThermalLineComposer.composeRow(cells, 20).single()
        assertEquals(20, line.length)
        assertTrue(line.trimEnd().endsWith("99.50"))
        assertTrue(line.startsWith("Total"))
    }

    @Test
    fun composeRowWrapsTallestColumnAndPadsOthers() {
        val cells = listOf(
            ColumnText("A very long item name here", weight = 2, align = Align.LEFT),
            ColumnText("9", weight = 1, align = Align.RIGHT),
        )
        val lines = ThermalLineComposer.composeRow(cells, 24)
        assertTrue(lines.size >= 2)
        assertTrue(lines.all { it.length == 24 })
    }
}
