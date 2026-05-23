package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampairs.subscription.domain.model.SubscriptionStatus
import com.ampairs.subscription.util.SubscriptionOnboardingLookup
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch

/**
 * Onboarding screen shown after workspace creation or selection
 * Displays subscription plan info and allows user to start trial or upgrade
 */
@Composable
fun SubscriptionOnboardingScreen(
    workspaceId: String,
    onNavigateToPlanSelection: () -> Unit,
    onContinueWithFree: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: SubscriptionViewModel,
    onboardingManager: SubscriptionOnboardingLookup
) {
    val subscription by viewModel.subscription.collectAsState()
    val currentPlan by viewModel.currentPlan.collectAsState()
    val scope = rememberCoroutineScope()

    var shouldShow by remember { mutableStateOf<Boolean?>(null) }

    // Check if we should show onboarding
    LaunchedEffect(workspaceId, subscription) {
        subscription?.let { sub ->
            val planCode = currentPlan
            shouldShow = onboardingManager.shouldShowPlanSelection(workspaceId, planCode.name)
        }
    }

    // If we shouldn't show, dismiss immediately
    LaunchedEffect(shouldShow) {
        if (shouldShow == false) {
            onDismiss()
        }
    }

    // Only render if we should show
    if (shouldShow == true && subscription != null && currentPlan != null) {
        SubscriptionOnboardingDialog(
            planName = currentPlan?.displayName ?: "Free",
            planCode = currentPlan?.name ?: "FREE",
            subscriptionStatus = subscription?.status ?: SubscriptionStatus.ACTIVE,
            onStartTrial = {
                scope.launch {
                    onboardingManager.markPlanSelectionSeen(workspaceId)
                    onNavigateToPlanSelection()
                }
            },
            onUpgrade = {
                scope.launch {
                    onboardingManager.markPlanSelectionSeen(workspaceId)
                    onNavigateToPlanSelection()
                }
            },
            onContinueWithFree = {
                scope.launch {
                    onboardingManager.markPlanSelectionSeen(workspaceId)
                    onContinueWithFree()
                }
            },
            onDismiss = {
                scope.launch {
                    onboardingManager.markPlanSelectionSeen(workspaceId)
                    onDismiss()
                }
            }
        )
    }
}

@Composable
private fun SubscriptionOnboardingDialog(
    planName: String,
    planCode: String,
    subscriptionStatus: SubscriptionStatus,
    onStartTrial: () -> Unit,
    onUpgrade: () -> Unit,
    onContinueWithFree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Welcome to Your Workspace!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "You're currently on the $planName plan.",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (planCode.equals("FREE", ignoreCase = true)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Free Plan Includes:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("• 50 Customers", style = MaterialTheme.typography.bodyMedium)
                            Text("• 50 Products", style = MaterialTheme.typography.bodyMedium)
                            Text("• 20 Invoices/month", style = MaterialTheme.typography.bodyMedium)
                            Text("• 2 Devices", style = MaterialTheme.typography.bodyMedium)
                            Text("• Basic Modules", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Want more? Upgrade to unlock unlimited customers, advanced features, and priority support!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (planCode.equals("FREE", ignoreCase = true)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartTrial,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start 14-Day Trial")
                    }

                    OutlinedButton(
                        onClick = onUpgrade,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View Plans")
                    }

                    TextButton(
                        onClick = onContinueWithFree,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue with Free")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Started")
                }
            }
        }
    )
}
