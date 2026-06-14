package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.subscription.domain.model.PaymentMethod
import com.ampairs.subscription.domain.model.PaymentMethodType
import com.ampairs.subscription.viewmodel.SubscriptionEvent
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

/**
 * Screen to manage saved payment methods
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit
) {
    val methods by viewModel.paymentMethods.collectAsState()
    val defaultMethod by viewModel.defaultPaymentMethod.collectAsState()
    val isLoading by viewModel.isLoadingPayments.collectAsState()

    val scope = rememberCoroutineScope()
    var methodToDelete by remember { mutableStateOf<PaymentMethod?>(null) }

    // Load payment methods on screen entry
    LaunchedEffect(Unit) {
        viewModel.loadPaymentMethods()
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SubscriptionEvent.PaymentMethodUpdated -> {
                    // Refresh list after default change
                    viewModel.loadPaymentMethods()
                }
                is SubscriptionEvent.PaymentMethodRemoved -> {
                    // Refresh list after removal
                    viewModel.loadPaymentMethods()
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Methods") },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = "Back")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (isLoading && methods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (methods.isEmpty()) {
            EmptyPaymentMethodsState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(methods, key = { it.uid }) { method ->
                    PaymentMethodCard(
                        method = method,
                        isDefault = method.uid == defaultMethod?.uid,
                        onSetDefault = {
                            scope.launch {
                                viewModel.setDefaultPaymentMethod(method.uid)
                            }
                        },
                        onRemove = {
                            methodToDelete = method
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Delete confirmation dialog
    methodToDelete?.let { method ->
        AlertDialog(
            onDismissRequest = { methodToDelete = null },
            title = { Text("Remove Payment Method?") },
            text = {
                Text("Are you sure you want to remove ${method.displayName}? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.removePaymentMethod(method.uid)
                            methodToDelete = null
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { methodToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Payment method icon
                    PaymentMethodIcon(method = method)

                    Column {
                        Text(
                            text = method.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Additional info based on type
                        when {
                            method.last4 != null -> {
                                Text(
                                    text = "**** **** **** ${method.last4}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            method.upiId != null -> {
                                Text(
                                    text = method.upiId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            method.bankName != null -> {
                                Text(
                                    text = method.bankName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Show expiry if card
                        if (method.expMonth != null && method.expYear != null) {
                            val expiryText = "Expires ${method.expMonth}/${method.expYear}"
                            val isExpired = method.isExpired
                            Text(
                                text = expiryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isExpired) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                }

                // Default badge
                if (isDefault) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            // Actions
            if (!isDefault) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onSetDefault,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Set as Default")
                    }

                    OutlinedButton(
                        onClick = onRemove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodIcon(method: PaymentMethod) {
    val icon = when (method.type) {
        PaymentMethodType.CARD -> Icons.Default.CreditCard
        PaymentMethodType.UPI -> Icons.Default.AccountBalance
        PaymentMethodType.NET_BANKING -> Icons.Default.AccountBalance
        else -> Icons.Default.Payment
    }

    val backgroundColor = when (method.brand?.uppercase()) {
        "VISA" -> Color(0xFF1A1F71)
        "MASTERCARD" -> Color(0xFFEB001B)
        "AMEX" -> Color(0xFF006FCF)
        "DISCOVER" -> Color(0xFFFF6000)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (method.brand != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EmptyPaymentMethodsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Payment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Payment Methods",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Payment methods will be saved automatically when you make a purchase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}
