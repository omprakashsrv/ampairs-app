package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.cbstore.ui.CbStoreFormRoute
import com.ampairs.cbstore.ui.CbStoreListRoute
import com.ampairs.cbstore.ui.form.CbStoreFormScreen
import com.ampairs.cbstore.ui.list.CbStoreListScreen

/** Entry provider for cb-store routes (California Burrito outlets). */
fun cbStoreEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is CbStoreListRoute -> NavEntry(key) {
        CbStoreListScreen(
            onStoreClick = { id -> backStack.add(CbStoreFormRoute(id)) },
            onAddStore = { backStack.add(CbStoreFormRoute()) },
            modifier = Modifier,
        )
    }

    is CbStoreFormRoute -> NavEntry(key) {
        CbStoreFormScreen(
            storeId = key.storeId,
            onDone = { backStack.removeLastOrNull() },
            modifier = Modifier,
        )
    }

    else -> null
}
