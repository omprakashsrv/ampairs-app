package com.ampairs.agent.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ampairsapp.feature.agent.generated.resources.Res
import ampairsapp.feature.agent.generated.resources.agent_total
import com.ampairs.agent.core.ChatMessage
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import org.jetbrains.compose.resources.stringResource

@Composable
fun MessageBubble(
    message: ChatMessage,
    onActionClick: ((Map<String, String>) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp,
                ),
                color = when {
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    message.isFromUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                tonalElevation = if (message.isFromUser) 0.dp else 1.dp,
            ) {
                val onColor = when {
                    message.isError -> MaterialTheme.colorScheme.onErrorContainer
                    message.isFromUser -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = message.text,
                        color = onColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    // Ranked report lines (top customers/products/debtors). Each amount is rendered in
                    // the active workspace's business currency here, in the composable layer (FR-013) —
                    // handlers never bake a currency symbol into text.
                    message.rows?.takeIf { it.isNotEmpty() }?.let { rows ->
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            rows.forEachIndexed { index, row ->
                                Text(
                                    text = "${index + 1}. ${row.label} — ${formatMoney(row.amount, LocalAppLocale.current)}",
                                    color = onColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    // Money total rendered in the active workspace's business currency (FR-013).
                    message.amount?.let { amount ->
                        Text(
                            text = "${stringResource(Res.string.agent_total)}: ${formatMoney(amount, LocalAppLocale.current)}",
                            color = onColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            // Show action result card if present
            message.actionResult?.let { result ->
                ActionResultCard(
                    result = result,
                    onViewClick = onActionClick,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
