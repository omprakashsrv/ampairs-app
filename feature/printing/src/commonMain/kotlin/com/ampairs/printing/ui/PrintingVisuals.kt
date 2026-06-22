package com.ampairs.printing.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.ampairs.printing.core.model.ConnectionType
import com.ampairs.printing.core.model.DocumentType
import com.ampairs.printing.core.model.PrinterClass
import com.ampairs.printing.core.model.PrintJobState
import com.ampairs.printing.core.model.TemplateBlock

/**
 * Shared visual mappings for the printing UI — semantic status colors, document/connection icons,
 * and the chip palette. Kept in one place so the list, detail and queue screens stay consistent with
 * the design system (see the `Ampairs Printing` design tokens: success/warning + M3 container roles).
 */

private val isDarkSurface: Boolean
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/** M3 has no success/warning roles — resolve them per theme to match the design tokens. */
val successColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkSurface) Color(0xFF6DD58C) else Color(0xFF2E7D32)

val warningColor: Color
    @Composable @ReadOnlyComposable
    get() = if (isDarkSurface) Color(0xFFFFB74D) else Color(0xFFEF8E00)

/** A document type rendered for display, e.g. DELIVERY_CHALLAN -> "Delivery Challan". */
val DocumentType.displayName: String
    get() = name.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }

val DocumentType.icon: ImageVector
    get() = when (this) {
        DocumentType.INVOICE -> Icons.Filled.ReceiptLong
        DocumentType.ORDER -> Icons.Filled.ShoppingCart
        DocumentType.RECEIPT, DocumentType.PAYMENT_RECEIPT, DocumentType.GIFT_RECEIPT -> Icons.Filled.Payments
        DocumentType.QUOTATION -> Icons.Filled.RequestQuote
        DocumentType.DELIVERY_CHALLAN -> Icons.Filled.LocalShipping
        else -> Icons.Filled.Description
    }

val ConnectionType.icon: ImageVector
    get() = when (this) {
        ConnectionType.NETWORK -> Icons.Filled.Lan
        ConnectionType.BLUETOOTH -> Icons.Filled.Bluetooth
        ConnectionType.USB -> Icons.Filled.Usb
        ConnectionType.OS_PRINT -> Icons.Filled.DesktopWindows
        ConnectionType.SHARE -> Icons.Filled.Share
    }

val ConnectionType.label: String
    get() = when (this) {
        ConnectionType.NETWORK -> "Network"
        ConnectionType.BLUETOOTH -> "Bluetooth"
        ConnectionType.USB -> "USB"
        ConnectionType.OS_PRINT -> "OS Print"
        ConnectionType.SHARE -> "Share"
    }

val PrinterClass.label: String
    get() = when (this) {
        PrinterClass.THERMAL -> "Thermal"
        PrinterClass.PAGE -> "Page"
        PrinterClass.LABEL -> "Label"
    }

/** Container colors for the printer-class chip (thermal stands out via the tertiary role). */
data class ChipColors(val container: Color, val content: Color)

val PrinterClass.chipColors: ChipColors
    @Composable @ReadOnlyComposable
    get() = if (this == PrinterClass.THERMAL) {
        ChipColors(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
    } else {
        ChipColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }

/** Label / icon / accent color for a print-job state, used by the queue's status bar + chip. */
data class JobStatusVisual(val label: String, val icon: ImageVector, val color: Color, val spinning: Boolean)

val PrintJobState.statusVisual: JobStatusVisual
    @Composable @ReadOnlyComposable
    get() = when (this) {
        PrintJobState.QUEUED -> JobStatusVisual("Queued", Icons.Filled.Schedule, warningColor, false)
        PrintJobState.SENDING -> JobStatusVisual("Sending", Icons.Filled.Sync, MaterialTheme.colorScheme.primary, true)
        PrintJobState.SENT -> JobStatusVisual("Awaiting", Icons.Filled.Schedule, warningColor, false)
        PrintJobState.UNCONFIRMED -> JobStatusVisual("Check", Icons.Filled.Schedule, warningColor, false)
        PrintJobState.CONFIRMED -> JobStatusVisual("Printed", Icons.Filled.CheckCircle, successColor, false)
        PrintJobState.FAILED -> JobStatusVisual("Failed", Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error, false)
        PrintJobState.DEAD -> JobStatusVisual("Given up", Icons.Filled.Block, MaterialTheme.colorScheme.onSurfaceVariant, false)
    }

val PrintJobState.docIcon: ImageVector
    get() = Icons.Filled.Print

/** Leading icon for a template block, by its kind — used in the editor's block list. */
val TemplateBlock.icon: ImageVector
    get() = when (this) {
        is TemplateBlock.StaticText -> Icons.Filled.Notes
        is TemplateBlock.BoundText -> Icons.Filled.DataObject
        is TemplateBlock.KeyValue -> Icons.Filled.Label
        is TemplateBlock.LineTable -> Icons.Filled.TableChart
        is TemplateBlock.InfoGrid -> Icons.Filled.GridView
        is TemplateBlock.Divider -> Icons.Filled.HorizontalRule
        is TemplateBlock.Spacer -> Icons.Filled.SpaceBar
        is TemplateBlock.Logo -> Icons.Filled.Storefront
        is TemplateBlock.BarcodeField -> Icons.Filled.ViewWeek
        is TemplateBlock.QrField -> Icons.Filled.QrCode2
        is TemplateBlock.CutMark -> Icons.Filled.ContentCut
        is TemplateBlock.CashDrawer -> Icons.Filled.PointOfSale
    }
