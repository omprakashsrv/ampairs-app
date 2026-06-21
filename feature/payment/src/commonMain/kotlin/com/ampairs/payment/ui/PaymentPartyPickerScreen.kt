package com.ampairs.payment.ui

import ampairsapp.feature.payment.generated.resources.Res
import ampairsapp.feature.payment.generated.resources.payment_select_party
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/** Pick the party (customer) to record a collection against. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentPartyPickerScreen(
    onPartySelected: (String) -> Unit,
    viewModel: PaymentPartyPickerViewModel = metroViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.payment_select_party)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(Res.string.payment_select_party)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(customers, key = { it.id }) { customer ->
                    val phone = customer.phone
                    ListItem(
                        headlineContent = { Text(customer.name) },
                        supportingContent = if (phone != null) {
                            { Text(phone) }
                        } else null,
                        modifier = Modifier.clickable { onPartySelected(customer.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
