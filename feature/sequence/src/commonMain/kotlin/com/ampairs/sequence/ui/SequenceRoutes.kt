package com.ampairs.sequence.ui

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object SequenceSettingsRoute : NavKey

@Serializable
data class SequenceDefinitionFormRoute(
    val definitionUid: String? = null
) : NavKey
