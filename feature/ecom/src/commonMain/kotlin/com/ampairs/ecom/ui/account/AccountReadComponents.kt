package com.ampairs.ecom.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_account_not_linked
import ampairsapp.feature.ecom.generated.resources.ecom_account_read_error
import ampairsapp.feature.ecom.generated.resources.ecom_retry
import org.jetbrains.compose.resources.stringResource

/**
 * Shared empty/error state for the buyer invoice & statement reads (spec 029). Two distinct cases:
 * [notLinked] true → the buyer isn't linked to a CRM account in this store (server 403) → show the
 * "link your account" hint; otherwise a transient/network error → show a generic "couldn't load"
 * message. Both offer a retry. (Previously this always showed the not-linked hint, misreporting
 * transient failures as "not linked".)
 */
@Composable
fun AccountReadError(notLinked: Boolean, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(if (notLinked) Res.string.ecom_account_not_linked else Res.string.ecom_account_read_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(Res.string.ecom_retry))
        }
    }
}
