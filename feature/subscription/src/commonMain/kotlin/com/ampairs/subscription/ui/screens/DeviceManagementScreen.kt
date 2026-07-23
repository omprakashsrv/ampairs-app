package com.ampairs.subscription.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.navigation.ScreenBackButton
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.viewmodel.SubscriptionEvent
import com.ampairs.subscription.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * Screen for managing registered devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    viewModel: SubscriptionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlans: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    var showDeactivateDialog by remember { mutableStateOf(false) }
    var deviceToDeactivate by remember { mutableStateOf<DeviceRegistration?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SubscriptionEvent.DeviceDeactivated -> {
                    snackbarHostState.showSnackbar("Device removed successfully")
                }
                is SubscriptionEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Devices") },
                navigationIcon = {
                    ScreenBackButton(onClick = onNavigateBack, contentDescription = "Back")
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Device limit info
                item {
                    subscription?.let { sub ->
                        DeviceLimitCard(
                            currentCount = devices.size,
                            maxDevices = sub.getLimits().maxDevices,
                            onUpgrade = onNavigateToPlans
                        )
                    }
                }

                // Section header
                item {
                    Text(
                        text = "Registered Devices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (devices.isEmpty()) {
                    item {
                        EmptyDevicesCard()
                    }
                } else {
                    items(devices) { device ->
                        DeviceCard(
                            device = device,
                            onDeactivate = {
                                deviceToDeactivate = device
                                showDeactivateDialog = true
                            }
                        )
                    }
                }

                // Info card
                item {
                    DeviceInfoCard()
                }
            }
        }
    }

    // Deactivate confirmation dialog
    if (showDeactivateDialog && deviceToDeactivate != null) {
        AlertDialog(
            onDismissRequest = {
                showDeactivateDialog = false
                deviceToDeactivate = null
            },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null)
            },
            title = { Text("Remove Device?") },
            text = {
                Column {
                    Text("Are you sure you want to remove this device?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = deviceToDeactivate?.deviceName ?: deviceToDeactivate?.deviceModel ?: "Unknown Device",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The device will need to sign in again to access this workspace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deviceToDeactivate?.let { device ->
                            viewModel.deactivateDevice(device.uid)
                        }
                        showDeactivateDialog = false
                        deviceToDeactivate = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeactivateDialog = false
                        deviceToDeactivate = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Processing overlay
    if (uiState.isProcessing) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Processing...")
                }
            }
        }
    }
}

@Composable
private fun DeviceLimitCard(
    currentCount: Int,
    maxDevices: Int,
    onUpgrade: () -> Unit
) {
    val isUnlimited = maxDevices == -1
    val isAtLimit = !isUnlimited && currentCount >= maxDevices
    val isNearLimit = !isUnlimited && currentCount >= (maxDevices * 0.8).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAtLimit -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                isNearLimit -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Limit",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isUnlimited) "$currentCount / Unlimited" else "$currentCount / $maxDevices",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isAtLimit || isNearLimit) {
                    FilledTonalButton(onClick = onUpgrade) {
                        Icon(Icons.Default.Upgrade, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Upgrade")
                    }
                }
            }

            if (!isUnlimited) {
                LinearProgressIndicator(
                    progress = { (currentCount.toFloat() / maxDevices).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = when {
                        isAtLimit -> MaterialTheme.colorScheme.error
                        isNearLimit -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            if (isAtLimit) {
                Text(
                    text = "You've reached your device limit. Remove a device or upgrade your plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyDevicesCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No Devices Registered",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Devices will appear here when you sign in from different devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeviceCard(
    device: DeviceRegistration,
    onDeactivate: () -> Unit
) {
    val accessMode = device.calculateAccessMode()
    val isCurrentDevice = false // TODO: Compare with current device ID

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Platform icon
            Surface(
                color = when (device.platform) {
                    DevicePlatform.ANDROID -> Color(0xFF3DDC84).copy(alpha = 0.2f)
                    DevicePlatform.IOS -> MaterialTheme.colorScheme.surfaceVariant
                    DevicePlatform.DESKTOP_WINDOWS, DevicePlatform.DESKTOP_MAC, DevicePlatform.DESKTOP_LINUX -> MaterialTheme.colorScheme.primaryContainer
                    DevicePlatform.WEB -> MaterialTheme.colorScheme.secondaryContainer
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = when (device.platform) {
                        DevicePlatform.ANDROID -> Icons.Default.PhoneAndroid
                        DevicePlatform.IOS -> Icons.Default.PhoneIphone
                        DevicePlatform.DESKTOP_WINDOWS, DevicePlatform.DESKTOP_MAC, DevicePlatform.DESKTOP_LINUX -> Icons.Default.Computer
                        DevicePlatform.WEB -> Icons.Default.Language
                    },
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = when (device.platform) {
                        DevicePlatform.ANDROID -> Color(0xFF3DDC84)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Device details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = device.deviceName ?: device.deviceModel ?: "Unknown Device",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (isCurrentDevice) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "This device",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(
                    text = "${getPlatformDisplayName(device.platform)} • ${device.osVersion ?: "Unknown OS"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                device.lastActivityAt?.let {
                    Text(
                        text = "Last active: ${formatRelativeTime(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Access mode status
                Surface(
                    color = when (accessMode) {
                        SubscriptionAccessMode.FULL_ACCESS -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        SubscriptionAccessMode.OFFLINE_GRACE -> MaterialTheme.colorScheme.tertiaryContainer
                        SubscriptionAccessMode.READ_ONLY -> MaterialTheme.colorScheme.secondaryContainer
                        SubscriptionAccessMode.LOCKED -> MaterialTheme.colorScheme.errorContainer
                    },
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = getAccessModeDisplayName(accessMode),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (accessMode) {
                            SubscriptionAccessMode.FULL_ACCESS -> Color(0xFF4CAF50)
                            SubscriptionAccessMode.LOCKED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Actions
            if (!isCurrentDevice) {
                IconButton(
                    onClick = onDeactivate
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove device",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "About Device Management",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Each device requires a valid token to access your workspace. Tokens are automatically renewed when online.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "• Full Access: Token is valid and device is connected\n• Grace Period: Token expired, limited offline access\n• Locked: Must go online to continue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(isoDate: String): String {
    // Simple relative time formatting
    return try {
        val date = isoDate.take(10)
        date
    } catch (e: Exception) {
        isoDate.take(10)
    }
}

private fun getPlatformDisplayName(platform: DevicePlatform): String {
    return when (platform) {
        DevicePlatform.ANDROID -> "Android"
        DevicePlatform.IOS -> "iOS"
        DevicePlatform.DESKTOP_WINDOWS -> "Windows"
        DevicePlatform.DESKTOP_MAC -> "macOS"
        DevicePlatform.DESKTOP_LINUX -> "Linux"
        DevicePlatform.WEB -> "Web"
    }
}

private fun getAccessModeDisplayName(accessMode: SubscriptionAccessMode): String {
    return when (accessMode) {
        SubscriptionAccessMode.FULL_ACCESS -> "Full Access"
        SubscriptionAccessMode.OFFLINE_GRACE -> "Grace Period"
        SubscriptionAccessMode.READ_ONLY -> "Read Only"
        SubscriptionAccessMode.LOCKED -> "Locked"
    }
}
