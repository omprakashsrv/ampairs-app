package com.ampairs.printing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A pill chip with an optional leading icon — used for status, class and connection labels. */
@Composable
fun PillChip(
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = content)
    }
}

/** A rounded square holding an icon — the leading avatar on printer / template / job rows. */
@Composable
fun IconTile(
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    size: Int = 46,
    cornerRadius: Int = 13,
    iconSize: Int = 23,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize.dp))
    }
}

/** A centered empty / first-run state: big icon tile, headline, body, and a stack of actions. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    iconContainer: Color,
    iconContent: Color,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(
            icon = icon,
            container = iconContainer,
            content = iconContent,
            size = 104,
            cornerRadius = 30,
            iconSize = 50,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(20.dp))
        actions()
    }
}

/** Vertical content padding shared by the printing list screens. */
val printingListPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
