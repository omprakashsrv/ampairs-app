package com.ampairs.update.ui

import ampairsapp.feature.update.generated.resources.Res
import ampairsapp.feature.update.generated.resources.about_check_failed
import ampairsapp.feature.update.generated.resources.about_check_for_updates
import ampairsapp.feature.update.generated.resources.about_checking
import ampairsapp.feature.update.generated.resources.about_up_to_date
import ampairsapp.feature.update.generated.resources.about_version
import ampairsapp.feature.update.generated.resources.app_logo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.update.viewmodel.AboutViewModel
import com.ampairs.update.viewmodel.UpdateCheckState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * About screen: app logo, name, current version, and a "Check for Updates" action that
 * reuses the existing [UpdateChecker]/[UpdateDialog] download + install flow. Launched from
 * the desktop system tray, but kept platform-agnostic so it can be reused elsewhere.
 */
@Composable
fun AboutScreen(
    appName: String,
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.app_logo),
            contentDescription = appName,
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.about_version, state.appVersion, state.versionCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        val checking = state.checkState is UpdateCheckState.Checking
        Button(
            onClick = { viewModel.checkForUpdates() },
            enabled = !checking,
        ) {
            if (checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.about_checking))
            } else {
                Text(stringResource(Res.string.about_check_for_updates))
            }
        }

        when (state.checkState) {
            is UpdateCheckState.UpToDate -> StatusText(
                text = stringResource(Res.string.about_up_to_date),
                color = MaterialTheme.colorScheme.primary,
            )
            is UpdateCheckState.Failed -> StatusText(
                text = stringResource(Res.string.about_check_failed),
                color = MaterialTheme.colorScheme.error,
            )
            else -> {}
        }
    }

    // Reuse the full download/install dialog when an update is found.
    val checkState = state.checkState
    if (checkState is UpdateCheckState.UpdateAvailable) {
        UpdateDialog(
            updateInfo = checkState.updateInfo,
            onDismiss = { viewModel.dismissResult() },
            // No per-version dismissal here — the user explicitly asked to check.
            onDismissUpdate = {},
        )
    }
}

@Composable
private fun StatusText(text: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}
