package com.ampairs.navigation.providers

import BusinessRoute
import Route
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.business.ui.BusinessCustomAttributesScreen
import com.ampairs.business.ui.BusinessImagesScreen
import com.ampairs.business.ui.BusinessOperationsScreen
import com.ampairs.business.ui.BusinessOverviewScreen
import com.ampairs.business.ui.BusinessProfileFormScreen
import com.ampairs.business.ui.BusinessTaxConfigScreen
import com.ampairs.di.LocalAppGraph

/**
 * Entry provider for Business module routes in Navigation 3.
 * Returns NavEntry for business routes or null if route doesn't match.
 */
fun businessEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is BusinessRoute.Overview -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessOverviewViewModel() }
        BusinessOverviewScreen(
            viewModel = viewModel,
            configRepository = graph.configRepository,
            onNavigateToProfile = {
                backStack.add(BusinessRoute.Profile)
            },
            onNavigateToOperations = {
                backStack.add(BusinessRoute.Operations)
            },
            onNavigateToTax = {
                backStack.add(BusinessRoute.TaxConfig)
            },
            onNavigateToCustomAttributes = {
                backStack.add(BusinessRoute.CustomAttributes)
            },
            onNavigateToFormConfig = {
                backStack.add(Route.FormConfig("business"))
            },
            onNavigateToImages = {
                backStack.add(BusinessRoute.Images)
            },
            modifier = Modifier
        )
    }

    is BusinessRoute.Profile -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessProfileViewModel() }
        BusinessProfileFormScreen(
            viewModel = viewModel,
            modifier = Modifier
        )
    }

    is BusinessRoute.Operations -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessOperationsViewModel() }
        BusinessOperationsScreen(
            viewModel = viewModel,
            modifier = Modifier
        )
    }

    is BusinessRoute.TaxConfig -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessTaxConfigViewModel() }
        BusinessTaxConfigScreen(
            viewModel = viewModel,
            modifier = Modifier
        )
    }

    is BusinessRoute.CustomAttributes -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessCustomAttributesViewModel() }
        BusinessCustomAttributesScreen(
            viewModel = viewModel,
            modifier = Modifier
        )
    }

    is BusinessRoute.Images -> NavEntry(key) {
        val graph = LocalAppGraph.current
        val viewModel = viewModel { graph.createBusinessImagesViewModel() }
        BusinessImagesScreen(
            viewModel = viewModel,
            modifier = Modifier
        )
    }

    else -> null
}
