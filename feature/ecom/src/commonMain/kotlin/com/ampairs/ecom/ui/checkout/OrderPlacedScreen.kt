package com.ampairs.ecom.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_continue_shopping
import ampairsapp.feature.ecom.generated.resources.ecom_order_placed_sub
import ampairsapp.feature.ecom.generated.resources.ecom_order_placed_title
import ampairsapp.feature.ecom.generated.resources.ecom_order_ref
import ampairsapp.feature.ecom.generated.resources.ecom_track_order
import com.ampairs.ecom.ui.components.EcomDimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun OrderPlacedScreen(
    orderRef: String,
    orderNumber: String,
    onTrack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
        }
        Text(
            stringResource(Res.string.ecom_order_placed_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.ecom_order_placed_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Column(
            Modifier.padding(top = 18.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, EcomDimens.cornerXl)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(Res.string.ecom_order_ref), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(orderNumber.ifBlank { orderRef }, style = MaterialTheme.typography.titleMedium)
        }
        Button(onClick = onTrack, modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
            Text(stringResource(Res.string.ecom_track_order))
        }
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(stringResource(Res.string.ecom_continue_shopping))
        }
    }
}
