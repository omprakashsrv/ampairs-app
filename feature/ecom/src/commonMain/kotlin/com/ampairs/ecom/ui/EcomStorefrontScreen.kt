package com.ampairs.ecom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_pending_body
import ampairsapp.feature.ecom.generated.resources.ecom_pending_title
import ampairsapp.feature.ecom.generated.resources.ecom_request_access_btn
import ampairsapp.feature.ecom.generated.resources.ecom_request_access_title
import ampairsapp.feature.ecom.generated.resources.ecom_store_unavailable_body
import ampairsapp.feature.ecom.generated.resources.ecom_store_unavailable_title
import com.ampairs.ecom.ui.catalog.DrillDownArgs
import com.ampairs.ecom.ui.gate.GatePhase
import com.ampairs.ecom.ui.gate.StorefrontGateViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Storefront entry point. Resolves the access gate, then hosts the shop shell.
 * All cross-screen navigation is delegated to the entry provider via callbacks.
 */
@Composable
fun EcomStorefrontScreen(
    slug: String,
    onRequireLogin: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onOpenDrillDown: (DrillDownArgs) -> Unit,
    onOpenOrder: (String) -> Unit,
    onOpenAddresses: () -> Unit,
    viewModel: StorefrontGateViewModel =
        assistedMetroViewModel<StorefrontGateViewModel, StorefrontGateViewModel.Factory>(key = slug) { create(slug) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.phase) {
        if (state.phase == GatePhase.RequireLogin) onRequireLogin()
    }

    when (state.phase) {
        GatePhase.Shop -> EcomShell(
            onOpenSearch = onOpenSearch,
            onOpenCart = onOpenCart,
            onOpenProduct = onOpenProduct,
            onOpenDrillDown = onOpenDrillDown,
            onOpenOrder = onOpenOrder,
            onOpenAddresses = onOpenAddresses,
            onLogin = onRequireLogin,
            onLoggedOut = { viewModel.resolve() },
        )

        GatePhase.StoreUnavailable -> GateStatus(
            icon = Icons.Filled.Lock,
            title = stringResource(Res.string.ecom_store_unavailable_title),
            body = stringResource(Res.string.ecom_store_unavailable_body),
        )

        GatePhase.RequestAccess -> GateStatus(
            icon = Icons.Filled.Lock,
            title = stringResource(Res.string.ecom_request_access_title),
            body = slug,
            actionLabel = stringResource(Res.string.ecom_request_access_btn),
            onAction = viewModel::requestAccess,
        )

        GatePhase.PendingAccess -> GateStatus(
            icon = Icons.Filled.HourglassEmpty,
            title = stringResource(Res.string.ecom_pending_title),
            body = stringResource(Res.string.ecom_pending_body),
        )

        GatePhase.Loading, GatePhase.RequireLogin ->
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
    }
}

@Composable
private fun GateStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.size(92.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(50.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.padding(top = 24.dp)) { Text(actionLabel) }
        }
    }
}
