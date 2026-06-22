package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.printing.ui.PrintQueueRoute
import com.ampairs.printing.ui.PrintQueueScreen
import com.ampairs.printing.ui.PrinterDetailRoute
import com.ampairs.printing.ui.PrinterDetailScreen
import com.ampairs.printing.ui.PrinterListRoute
import com.ampairs.printing.ui.PrinterListScreen
import com.ampairs.printing.ui.TemplateEditRoute
import com.ampairs.printing.ui.TemplateEditScreen
import com.ampairs.printing.ui.TemplateListRoute
import com.ampairs.printing.ui.TemplateListScreen

/**
 * Entry provider for Printing module routes in Navigation 3.
 * Returns a NavEntry for printing routes or null if the route doesn't match.
 */
fun printingEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is PrinterListRoute -> NavEntry(key) {
        PrinterListScreen(
            onOpenQueue = { backStack.add(PrintQueueRoute) },
            onOpenTemplates = { backStack.add(TemplateListRoute) },
            onOpenPrinter = { printerId -> backStack.add(PrinterDetailRoute(printerId)) },
            modifier = Modifier,
        )
    }

    is PrinterDetailRoute -> NavEntry(key) {
        PrinterDetailScreen(
            printerId = key.printerId,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    is PrintQueueRoute -> NavEntry(key) {
        PrintQueueScreen(modifier = Modifier)
    }

    is TemplateListRoute -> NavEntry(key) {
        TemplateListScreen(
            onEditTemplate = { templateId -> backStack.add(TemplateEditRoute(templateId)) },
            modifier = Modifier,
        )
    }

    is TemplateEditRoute -> NavEntry(key) {
        TemplateEditScreen(
            templateId = key.templateId,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
