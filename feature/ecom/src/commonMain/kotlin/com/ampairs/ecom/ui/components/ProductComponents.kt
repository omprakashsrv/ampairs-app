package com.ampairs.ecom.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.ecom.api.model.StockStatus

/**
 * Direction-A catalog grid card: square thumb, brand/name, mono price + strikethrough MRP,
 * unit, and the inline quick-add control (ADD ⇄ – qty +). Out-of-stock disables add.
 */
@Composable
fun ProductGridCard(
    name: String,
    brand: String?,
    unit: String?,
    price: Double,
    mrp: Double?,
    stockStatus: String,
    imageUrl: String?,
    quantityInCart: Int,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalAppLocale.current
    val outOfStock = stockStatus == StockStatus.OUT_OF_STOCK.name
    Column(
        modifier = modifier
            .clip(EcomDimens.cornerMd)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onOpen),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(46.dp))
            } else {
                AsyncImage(model = imageUrl, contentDescription = name, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
            }
            if (outOfStock) {
                Text(
                    text = "Out of stock",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer, EcomDimens.cornerSm)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (!brand.isNullOrBlank()) {
                Text(brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!unit.isNullOrBlank()) {
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(formatMoney(price, locale), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (mrp != null && mrp > price) {
                        Text(
                            formatMoney(mrp, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
                QuickAddControl(
                    quantity = quantityInCart,
                    enabled = !outOfStock,
                    onAdd = onAdd,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                )
            }
        }
    }
}

/** ADD button that flips to a compact – qty + stepper in place. */
@Composable
fun QuickAddControl(
    quantity: Int,
    enabled: Boolean,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    if (quantity <= 0) {
        Text(
            text = "ADD",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .clip(EcomDimens.cornerSm)
                .let { if (enabled) it.clickable(onClick = onAdd) else it }
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, EcomDimens.cornerSm)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    } else {
        Row(
            modifier = Modifier
                .clip(EcomDimens.cornerSm)
                .background(MaterialTheme.colorScheme.primary)
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepBtn(Icons.Filled.Remove, "Decrease quantity", onDecrement)
            Text("$quantity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.width(20.dp), maxLines = 1)
            StepBtn(Icons.Filled.Add, "Increase quantity", onIncrement)
        }
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(modifier = Modifier.size(30.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = desc, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(17.dp))
    }
}

/** Floating "N items · ₹total → View Cart" bar shown once the cart has items. */
@Composable
fun StickyCartBar(itemCount: Int, total: Double, viewCartLabel: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val locale = LocalAppLocale.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(EcomDimens.cornerMd)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Column {
                Text("$itemCount item${if (itemCount == 1) "" else "s"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text(formatMoney(total, locale), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(viewCartLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** Square category tile for the browse home. */
@Composable
fun CategoryTile(name: String, count: Int, imageUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(EcomDimens.cornerMd)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl.isNullOrBlank()) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp))
            } else {
                AsyncImage(model = imageUrl, contentDescription = name, modifier = Modifier.size(50.dp).clip(androidx.compose.foundation.shape.CircleShape))
            }
        }
        Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/** Round brand chip for the horizontal brand row. */
@Composable
fun BrandChip(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(64.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
