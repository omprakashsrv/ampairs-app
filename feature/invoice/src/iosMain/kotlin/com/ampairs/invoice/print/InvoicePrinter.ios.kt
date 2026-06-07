package com.ampairs.invoice.print

import androidx.compose.runtime.Composable
import platform.UIKit.UIMarkupTextPrintFormatter
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInfoOutputType
import platform.UIKit.UIPrintInteractionController

@Composable
actual fun rememberInvoicePrinter(): (html: String) -> Unit = { html ->
    val controller = UIPrintInteractionController.sharedPrintController()
    val info = UIPrintInfo.printInfo()
    info.outputType = UIPrintInfoOutputType.UIPrintInfoOutputGeneral
    info.jobName = "Tax Invoice"
    controller.printInfo = info
    controller.printFormatter = UIMarkupTextPrintFormatter(markupText = html)
    controller.presentAnimated(animated = true, completionHandler = null)
}
