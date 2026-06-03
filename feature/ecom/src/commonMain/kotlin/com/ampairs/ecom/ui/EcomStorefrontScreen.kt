package com.ampairs.ecom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_pending_body
import ampairsapp.feature.ecom.generated.resources.ecom_pending_title
import ampairsapp.feature.ecom.generated.resources.ecom_request_access_btn
import ampairsapp.feature.ecom.generated.resources.ecom_request_access_title
import ampairsapp.feature.ecom.generated.resources.ecom_shop_placeholder_body
import ampairsapp.feature.ecom.generated.resources.ecom_shop_placeholder_title
import ampairsapp.feature.ecom.generated.resources.ecom_store_unavailable_body
import ampairsapp.feature.ecom.generated.resources.ecom_store_unavailable_title
import com.ampairs.ecom.ui.gate.GatePhase
import com.ampairs.ecom.ui.gate.StorefrontGateViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Storefront entry point. Resolves the access gate and then hosts the shop shell.
 *
 * NOTE (Phase 1 scaffold): the Shop phase renders a placeholder. The full EcomShell
 * (Browse / Orders / Account tabs, catalog, cart, checkout) lands in later phases.
 */
@Composable
fun EcomStorefrontScreen(
    slug: String,
    onRequireLogin: () -> Unit,
    onExitStore: () -> Unit = {},
    viewModel: StorefrontGateViewModel =
        assistedMetroViewModel<StorefrontGateViewModel, StorefrontGateViewModel.Factory> { create(slug) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.phase) {
        if (state.phase == GatePhase.RequireLogin) onRequireLogin()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state.phase) {
            GatePhase.Loading, GatePhase.RequireLogin -> CircularProgressIndicator()

            GatePhase.StoreUnavailable -> GateMessage(
                title = stringResource(Res.string.ecom_store_unavailable_title),
                body = stringResource(Res.string.ecom_store_unavailable_body),
            )

            GatePhase.RequestAccess -> {
                GateMessage(
                    title = stringResource(Res.string.ecom_request_access_title),
                    body = slug,
                )
                Button(onClick = viewModel::requestAccess, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(Res.string.ecom_request_access_btn))
                }
            }

            GatePhase.PendingAccess -> GateMessage(
                title = stringResource(Res.string.ecom_pending_title),
                body = stringResource(Res.string.ecom_pending_body),
            )

            GatePhase.Shop -> GateMessage(
                title = stringResource(Res.string.ecom_shop_placeholder_title),
                body = stringResource(Res.string.ecom_shop_placeholder_body),
            )
        }
    }
}

@Composable
private fun GateMessage(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp),
    )
}
