package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.viewmodel.InvoiceViewModel
import com.ampairs.common.util.formatCurrencyWithCode
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Screen showing invoice details and payment options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceUid: String,
    onNavigateBack: () -> Unit,
    viewModel: InvoiceViewModel = metroViewModel(),
) {
    val invoice by viewModel.selectedInvoice.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsState()
    val error by viewModel.error.collectAsState()
    val paymentLink by viewModel.paymentLink.collectAsState()

    val uriHandler = LocalUriHandler.current
    var showPaymentDialog by remember { mutableStateOf(false) }

    // Load invoice on first composition
    LaunchedEffect(invoiceUid) {
        viewModel.loadInvoice(invoiceUid)
    }

    // Handle payment link
    LaunchedEffect(paymentLink) {
        paymentLink?.let { link ->
            if (link.paymentLinkUrl.isNotEmpty()) {
                // Open payment link in browser
                uriHandler.openUri(link.paymentLinkUrl)
                viewModel.clearPaymentLink()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && invoice == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                invoice?.let { inv ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Invoice header
                        InvoiceHeaderSection(invoice = inv)

                        // Billing period
                        BillingPeriodSection(invoice = inv)

                        // Amount details
                        AmountDetailsSection(invoice = inv)

                        // Payment status
                        PaymentStatusSection(invoice = inv)

                        // Error message
                        error?.let { errorMessage ->
                            if (!errorMessage.contains("successfully", ignoreCase = true)) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(
                                        text = errorMessage,
                                        modifier = Modifier.padding(16.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        // Payment actions
                        if (inv.canBePaid()) {
                            PaymentActionsSection(
                                invoice = inv,
                                isProcessingPayment = isProcessingPayment,
                                onPayNow = { showPaymentDialog = true },
                                onRetryPayment = {
                                    viewModel.retryPayment(inv.uid)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Payment method selection dialog
    if (showPaymentDialog) {
        PaymentMethodDialog(
            invoice = invoice,
            onDismiss = { showPaymentDialog = false },
            onPayWithLink = {
                showPaymentDialog = false
                invoice?.let { viewModel.payInvoice(it.uid, useAutoCharge = false) }
            },
            onPayWithSavedMethod = {
                showPaymentDialog = false
                invoice?.let { viewModel.payInvoice(it.uid, useAutoCharge = true) }
            }
        )
    }
}

@Composable
private fun InvoiceHeaderSection(invoice: Invoice) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (invoice.status) {
                InvoiceStatus.PAID -> MaterialTheme.colorScheme.primaryContainer
                InvoiceStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Invoice Number",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = invoice.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingPeriodSection(invoice: Invoice) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Billing Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "From",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(invoice.billingPeriodStart),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "To",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(invoice.billingPeriodEnd),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            HorizontalDivider()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (invoice.isOverdue())
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Due Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDate(invoice.dueDate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (invoice.isOverdue())
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountDetailsSection(invoice: Invoice) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Amount Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AmountRow(
                label = "Total Amount",
                amount = formatCurrencyWithCode(invoice.totalAmount, invoice.currency)
            )

            if (invoice.paidAmount > 0) {
                AmountRow(
                    label = "Paid Amount",
                    amount = formatCurrencyWithCode(invoice.paidAmount, invoice.currency),
                    amountColor = MaterialTheme.colorScheme.primary
                )
            }

            if (invoice.remainingBalance > 0) {
                HorizontalDivider()
                AmountRow(
                    label = "Remaining Balance",
                    amount = formatCurrencyWithCode(invoice.remainingBalance, invoice.currency),
                    amountColor = MaterialTheme.colorScheme.error,
                    labelStyle = MaterialTheme.typography.titleMedium,
                    amountStyle = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: String,
    amountColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    labelStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    amountStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = amountStyle,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
private fun PaymentStatusSection(invoice: Invoice) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (invoice.autoPaymentEnabled && invoice.paymentMethodId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Auto-payment enabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (invoice.paidAt != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Paid on",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDate(invoice.paidAt),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentActionsSection(
    invoice: Invoice,
    isProcessingPayment: Boolean,
    onPayNow: () -> Unit,
    onRetryPayment: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = if (invoice.isOverdue())
                        "This invoice is overdue. Please pay now to avoid service interruption."
                    else
                        "Pay this invoice to continue using premium features.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onPayNow,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessingPayment
            ) {
                if (isProcessingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isProcessingPayment) "Processing..." else "Pay Now")
            }

            if (invoice.status == InvoiceStatus.OVERDUE) {
                OutlinedButton(
                    onClick = onRetryPayment,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessingPayment
                ) {
                    Text("Retry Payment")
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodDialog(
    invoice: Invoice?,
    onDismiss: () -> Unit,
    onPayWithLink: () -> Unit,
    onPayWithSavedMethod: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Payment Method") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How would you like to pay this invoice?",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (invoice?.autoPaymentEnabled == true && invoice.paymentMethodId != null) {
                    Text(
                        text = "You have a saved payment method for auto-payment.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPayWithLink,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pay with Payment Link")
                }

                if (invoice?.autoPaymentEnabled == true && invoice.paymentMethodId != null) {
                    OutlinedButton(
                        onClick = onPayWithSavedMethod,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pay with Saved Method")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Format ISO date string to readable format
 */
private fun formatDate(isoDate: String): String {
    // TODO: Implement proper date formatting using kotlinx.datetime
    return isoDate.substringBefore("T")
}
