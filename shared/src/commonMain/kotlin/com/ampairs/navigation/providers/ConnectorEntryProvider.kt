package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.connector.ui.ConnectorConfigRoute
import com.ampairs.connector.ui.config.ConnectorConfigScreen

/**
 * Entry provider for the generic connector UI (spec 029). Serves both hosting types — the config form
 * is schema-driven so it renders Tally's host/port and a server-side connector's API-key alike.
 */
fun connectorEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>,
): NavEntry<NavKey>? = when (key) {
    is ConnectorConfigRoute -> NavEntry(key) {
        ConnectorConfigScreen(
            installationUid = key.installationUid,
            connectorType = key.connectorType,
            modifier = Modifier,
        )
    }

    else -> null
}
