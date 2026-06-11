package com.ampairs.business.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive layout primitives for the business module.
 *
 * On phones everything is a single full-width column (unchanged). On wider windows (tablet /
 * desktop) content is capped to a readable [maxContentWidth] and centered, so forms and cards
 * don't stretch edge-to-edge across a large window.
 */

/** Width past which we switch the action cards from one column to two. */
private val TWO_COLUMN_BREAKPOINT = 640.dp

/**
 * A vertically scrolling screen body whose content is capped to [maxContentWidth] and centered.
 * Use as the single child of a screen (replaces a plain `Column(verticalScroll).padding`).
 */
@Composable
fun BusinessScreenContent(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 920.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalSpacing: Dp = 16.dp,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = maxContentWidth)
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content,
        )
    }
}

/** A single tappable navigation action used on the business overview. */
data class BusinessAction(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/**
 * Lays the [actions] out as cards: one column on phones, two columns once the available width
 * crosses [TWO_COLUMN_BREAKPOINT]. A lone trailing card keeps its half-width instead of stretching.
 */
@Composable
fun BusinessActionGrid(
    actions: List<BusinessAction>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= TWO_COLUMN_BREAKPOINT) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.chunked(columns).forEach { rowActions ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowActions.forEach { action ->
                        BusinessActionCard(action = action, modifier = Modifier.weight(1f))
                    }
                    // Keep cell sizing stable when the last row is short of a full set.
                    repeat(columns - rowActions.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BusinessActionCard(
    action: BusinessAction,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier, onClick = action.onClick) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(action.title, style = MaterialTheme.typography.titleMedium)
                Text(action.subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
