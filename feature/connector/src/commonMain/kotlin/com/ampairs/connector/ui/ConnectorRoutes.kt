package com.ampairs.connector.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Navigation 3 routes for the generic connector UI (config + mapping), used by both hosting types. */

/** Connection settings for an installation. [connectorType] lets the form load the connection schema. */
@Serializable
data class ConnectorConfigRoute(
    val installationUid: String,
    val connectorType: String = "",
) : NavKey

/** Field-mapping editor (T028b) for an installation. */
@Serializable
data class ConnectorMappingRoute(
    val installationUid: String,
) : NavKey
