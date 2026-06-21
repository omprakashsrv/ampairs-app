package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_back
import ampairsapp.feature.payment.generated.resources.payment_search_customer
import ampairsapp.feature.payment.generated.resources.payment_select_party
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.payment.ui.components.PartyAvatar
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/** Pick the party (customer) to record a collection against. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPartyPickerScreen(
    onPartySelected: (String) -> Unit,
    onBack: () -> Unit = {},
    viewModel: PaymentPartyPickerViewModel = metroViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.payment_back))
                    }
                },
                title = { Text(stringResource(Res.string.payment_select_party)) },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(Res.string.payment_search_customer)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(23.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(customers, key = { it.id }) { customer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPartySelected(customer.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PartyAvatar(name = customer.name)
                        Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                            Text(
                                customer.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            customer.phone?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(start = 73.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
