package com.ampairs.navigation.providers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.tax.ui.calculator.TaxCalculatorScreen
import com.ampairs.tax.ui.configuration.TaxConfigurationScreen
import com.ampairs.tax.ui.detail.TaxCodeDetailScreen
import com.ampairs.tax.ui.list.MyTaxCodesScreen
import com.ampairs.tax.ui.navigation.MyTaxCodesRoute
import com.ampairs.tax.ui.navigation.TaxCalculatorRoute
import com.ampairs.tax.ui.navigation.TaxCodeDetailRoute
import com.ampairs.tax.ui.navigation.TaxCodeSearchRoute
import com.ampairs.tax.ui.navigation.TaxConfigurationRoute
import com.ampairs.tax.ui.navigation.TaxListRoute
import com.ampairs.tax.ui.navigation.TaxModuleScreen
import com.ampairs.tax.ui.search.TaxCodeSearchScreen

/**
 * Entry provider for Tax module routes in Navigation 3.
 * Returns NavEntry for tax routes or null if route doesn't match.
 */
fun taxEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is TaxListRoute -> NavEntry(key) {
        TaxModuleScreen(
            onNavigateToCalculator = {
                backStack.add(TaxCalculatorRoute)
            },
            onNavigateToMyTaxCodes = {
                backStack.add(MyTaxCodesRoute)
            },
            onNavigateToSearch = {
                backStack.add(TaxCodeSearchRoute())
            },
            onNavigateToConfiguration = {
                backStack.add(TaxConfigurationRoute)
            },
            modifier = Modifier
        )
    }

    is TaxConfigurationRoute -> NavEntry(key) {
        TaxConfigurationScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    is TaxCalculatorRoute -> NavEntry(key) {
        TaxCalculatorScreen(
            modifier = Modifier
        )
    }

    is MyTaxCodesRoute -> NavEntry(key) {
        MyTaxCodesScreen(
            onNavigateToEdit = { codeId ->
                backStack.add(TaxCodeDetailRoute(taxCodeId = codeId))
            },
            onNavigateToSearch = {
                backStack.add(TaxCodeSearchRoute())
            },
            modifier = Modifier
        )
    }

    is TaxCodeSearchRoute -> NavEntry(key) {
        TaxCodeSearchScreen(
            onCodeSelected = { codeId ->
                backStack.add(TaxCodeDetailRoute(taxCodeId = codeId))
            },
            modifier = Modifier
        )
    }

    is TaxCodeDetailRoute -> NavEntry(key) {
        TaxCodeDetailScreen(
            taxCodeId = key.taxCodeId,
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier
        )
    }

    else -> null
}
