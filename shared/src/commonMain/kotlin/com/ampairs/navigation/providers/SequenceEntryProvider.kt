package com.ampairs.navigation.providers

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.ampairs.sequence.ui.SequenceDefinitionFormRoute
import com.ampairs.sequence.ui.SequenceSettingsRoute
import com.ampairs.sequence.ui.form.SequenceDefinitionFormScreen
import com.ampairs.sequence.ui.settings.SequenceSettingsScreen

fun sequenceEntryProvider(
    key: NavKey,
    backStack: MutableList<NavKey>
): NavEntry<NavKey>? = when (key) {
    is SequenceSettingsRoute -> NavEntry(key) {
        SequenceSettingsScreen(
            onDefinitionClick = { definitionUid ->
                backStack.add(SequenceDefinitionFormRoute(definitionUid))
            },
            onAddDefinition = {
                backStack.add(SequenceDefinitionFormRoute())
            },
            modifier = Modifier
        )
    }

    is SequenceDefinitionFormRoute -> NavEntry(key) {
        SequenceDefinitionFormScreen(
            definitionUid = key.definitionUid,
            onSaveSuccess = {
                backStack.removeLastOrNull()
            },
            modifier = Modifier
        )
    }

    else -> null
}
