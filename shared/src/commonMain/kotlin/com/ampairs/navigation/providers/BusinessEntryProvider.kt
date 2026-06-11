package com.ampairs.navigation.providers

import BusinessRoute
import Route
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.business.ui.BusinessCustomAttributesScreen
import com.ampairs.business.ui.BusinessImagesScreen
import com.ampairs.business.ui.BusinessOperationsScreen
import com.ampairs.business.ui.BusinessOverviewScreen
import com.ampairs.business.ui.BusinessProfileFormScreen
fun businessEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is BusinessRoute.Overview -> NavEntry(key) {
        BusinessOverviewScreen(
            onNavigateToProfile = {
                backStack.add(BusinessRoute.Profile)
            },
            onNavigateToOperations = {
                backStack.add(BusinessRoute.Operations)
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
        BusinessProfileFormScreen(modifier = Modifier)
    }

    is BusinessRoute.Operations -> NavEntry(key) {
        BusinessOperationsScreen(modifier = Modifier)
    }

    is BusinessRoute.CustomAttributes -> NavEntry(key) {
        BusinessCustomAttributesScreen(modifier = Modifier)
    }

    is BusinessRoute.Images -> NavEntry(key) {
        BusinessImagesScreen(modifier = Modifier)
    }

    else -> null
}
