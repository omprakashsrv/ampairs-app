package com.ampairs.form.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.form.generated.resources.Res
import ampairsapp.feature.form.generated.resources.form_hub_title
import ampairsapp.feature.form.generated.resources.form_hub_subtitle
import ampairsapp.feature.form.generated.resources.form_hub_back_cd
import ampairsapp.feature.form.generated.resources.form_hub_entity_customer
import ampairsapp.feature.form.generated.resources.form_hub_entity_product
import ampairsapp.feature.form.generated.resources.form_hub_entity_order
import ampairsapp.feature.form.generated.resources.form_hub_entity_invoice
import ampairsapp.feature.form.generated.resources.form_hub_entity_business
import ampairsapp.feature.form.generated.resources.form_hub_entity_customer_desc
import ampairsapp.feature.form.generated.resources.form_hub_entity_product_desc
import ampairsapp.feature.form.generated.resources.form_hub_entity_order_desc
import ampairsapp.feature.form.generated.resources.form_hub_entity_invoice_desc
import ampairsapp.feature.form.generated.resources.form_hub_entity_business_desc

/** One configurable entity in the hub — matches the backend's supported EntityType set. */
private data class HubEntity(
    val entityType: String,
    val label: StringResource,
    val description: StringResource,
    val icon: ImageVector,
)

private val HUB_ENTITIES = listOf(
    HubEntity("customer", Res.string.form_hub_entity_customer, Res.string.form_hub_entity_customer_desc, Icons.Default.Person),
    HubEntity("product", Res.string.form_hub_entity_product, Res.string.form_hub_entity_product_desc, Icons.Default.Inventory2),
    HubEntity("order", Res.string.form_hub_entity_order, Res.string.form_hub_entity_order_desc, Icons.Default.ShoppingCart),
    HubEntity("invoice", Res.string.form_hub_entity_invoice, Res.string.form_hub_entity_invoice_desc, Icons.Default.ReceiptLong),
    HubEntity("business", Res.string.form_hub_entity_business, Res.string.form_hub_entity_business_desc, Icons.Default.Business),
)

/**
 * Dedicated entry point for form configuration (spec 011): lists every configurable entity type so
 * admins can reach any form editor directly from navigation (e.g. the More screen), instead of only
 * via each module's own screen. Selecting an entity opens its [FormConfigScreen].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormConfigHubScreen(
    onSelectEntity: (entityType: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.form_hub_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.form_hub_back_cd))
                }
            },
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(Res.string.form_hub_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    HUB_ENTITIES.forEachIndexed { index, entity ->
                        if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                        ListItem(
                            headlineContent = { Text(stringResource(entity.label), style = MaterialTheme.typography.bodyMedium) },
                            supportingContent = {
                                Text(
                                    stringResource(entity.description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingContent = {
                                Icon(entity.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            modifier = Modifier.clickable { onSelectEntity(entity.entityType) },
                        )
                    }
                }
            }
        }
    }
}
