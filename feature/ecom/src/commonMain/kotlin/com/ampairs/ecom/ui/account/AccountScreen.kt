package com.ampairs.ecom.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_account_addresses
import ampairsapp.feature.ecom.generated.resources.ecom_account_cross_store
import ampairsapp.feature.ecom.generated.resources.ecom_account_help
import ampairsapp.feature.ecom.generated.resources.ecom_account_invoices
import ampairsapp.feature.ecom.generated.resources.ecom_account_orders
import ampairsapp.feature.ecom.generated.resources.ecom_account_statement
import ampairsapp.feature.ecom.generated.resources.ecom_account_title
import ampairsapp.feature.ecom.generated.resources.ecom_login
import ampairsapp.feature.ecom.generated.resources.ecom_logout
import ampairsapp.feature.ecom.generated.resources.ecom_signin_body
import ampairsapp.feature.ecom.generated.resources.ecom_signin_title
import com.ampairs.ecom.ui.components.EcomDimens
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountScreen(
    onOpenOrders: () -> Unit,
    onOpenAddresses: () -> Unit,
    onLogin: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenInvoices: () -> Unit = {},
    onOpenStatement: () -> Unit = {},
    viewModel: AccountViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountEvent.NavigateLogin -> onLogin()
                is AccountEvent.LoggedOut -> onLoggedOut()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(stringResource(Res.string.ecom_account_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, modifier = Modifier.padding(16.dp))

        if (!state.isLoggedIn) {
            SignedOut(onLogin = viewModel::login)
            return
        }

        // Profile hero
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(state.displayName.initials(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                state.displayName.ifBlank { "Your account" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Cross-store note (FR-021)
        Row(
            Modifier.fillMaxWidth().padding(16.dp).background(MaterialTheme.colorScheme.tertiaryContainer, EcomDimens.cornerMd).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
            Text(stringResource(Res.string.ecom_account_cross_store), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }

        AccountRow(Icons.Filled.Receipt, stringResource(Res.string.ecom_account_orders), onOpenOrders)
        AccountRow(Icons.AutoMirrored.Filled.ReceiptLong, stringResource(Res.string.ecom_account_invoices), onOpenInvoices)
        AccountRow(Icons.Filled.AccountBalanceWallet, stringResource(Res.string.ecom_account_statement), onOpenStatement)
        AccountRow(Icons.Filled.LocationOn, stringResource(Res.string.ecom_account_addresses), onOpenAddresses)
        AccountRow(Icons.AutoMirrored.Filled.HelpOutline, stringResource(Res.string.ecom_account_help), {})
        AccountRow(Icons.AutoMirrored.Filled.Logout, stringResource(Res.string.ecom_logout), viewModel::logout, danger = true)
    }
}

@Composable
private fun AccountRow(icon: ImageVector, label: String, onClick: () -> Unit, danger: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(
            Modifier.size(40.dp).clip(CircleShape).background(if (danger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (danger) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (!danger) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SignedOut(onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(28.dp).background(MaterialTheme.colorScheme.surfaceContainer, EcomDimens.cornerLg).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
        }
        Text(stringResource(Res.string.ecom_signin_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
        Text(stringResource(Res.string.ecom_signin_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.ecom_login)) }
    }
}

private fun String.initials(): String =
    trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifBlank { "U" }
