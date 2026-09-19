package com.ampairs.aiops.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.aiops.AiOpsAutonomyLevel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.aiops.generated.resources.Res
import ampairsapp.feature.aiops.generated.resources.aiops_settings_cd_back
import ampairsapp.feature.aiops.generated.resources.aiops_settings_footnote
import ampairsapp.feature.aiops.generated.resources.aiops_settings_header
import ampairsapp.feature.aiops.generated.resources.aiops_settings_title
import ampairsapp.feature.aiops.generated.resources.aiops_level_observe_desc
import ampairsapp.feature.aiops.generated.resources.aiops_level_observe_title
import ampairsapp.feature.aiops.generated.resources.aiops_level_recommend_desc
import ampairsapp.feature.aiops.generated.resources.aiops_level_recommend_title
import ampairsapp.feature.aiops.generated.resources.aiops_level_autocorrect_desc
import ampairsapp.feature.aiops.generated.resources.aiops_level_autocorrect_title

@Composable
fun AiOpsSettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AiOpsSettingsViewModel = metroViewModel(),
    modifier: Modifier = Modifier,
) {
    val selected by viewModel.level.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.aiops_settings_cd_back),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.aiops_settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.aiops_settings_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            viewModel.selectableLevels.forEach { level ->
                AutonomyLevelRow(
                    title = stringResource(level.titleRes()),
                    description = stringResource(level.descriptionRes()),
                    selected = level == selected,
                    onSelect = { viewModel.setLevel(level) },
                )
            }

            Text(
                text = stringResource(Res.string.aiops_settings_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun AutonomyLevelRow(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun AiOpsAutonomyLevel.titleRes(): StringResource = when (this) {
    AiOpsAutonomyLevel.OBSERVE -> Res.string.aiops_level_observe_title
    AiOpsAutonomyLevel.AUTO_CORRECT -> Res.string.aiops_level_autocorrect_title
    else -> Res.string.aiops_level_recommend_title
}

private fun AiOpsAutonomyLevel.descriptionRes(): StringResource = when (this) {
    AiOpsAutonomyLevel.OBSERVE -> Res.string.aiops_level_observe_desc
    AiOpsAutonomyLevel.AUTO_CORRECT -> Res.string.aiops_level_autocorrect_desc
    else -> Res.string.aiops_level_recommend_desc
}
