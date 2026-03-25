# Event Synchronization System - Developer Guide

## Quick Start: Add Real-Time Sync to Your Module

### Step 1: Add Event Listener Method to Your Repository

```kotlin
class ProductRepository(
    private val productDao: ProductDao,
    private val productApi: ProductApi
) {
    private var eventListenerJob: Job? = null

    /**
     * Set up real-time event listener for product updates from other devices.
     * Call this after workspace is selected and EventManager is available.
     */
    fun setupEventListener(eventManager: EventManager) {
        eventListenerJob?.cancel() // Cancel existing listener

        eventListenerJob = CoroutineScope(Dispatchers.Default).launch {
            eventManager.events
                .filter { it.entityType == "product" }  // ← Your entity type
                .collect { event ->
                    when (event.eventType) {
                        EventType.PRODUCT_CREATED,
                        EventType.PRODUCT_UPDATED -> refreshProductFromServer(event.entityId)
                        EventType.PRODUCT_DELETED -> productDao.deleteProduct(event.entityId)
                        else -> {}
                    }
                }
        }
    }

    fun stopEventListener() {
        eventListenerJob?.cancel()
        eventListenerJob = null
    }

    private suspend fun refreshProductFromServer(productId: String) {
        try {
            val fresh = productApi.getProduct(productId)
            if (fresh != null) {
                productDao.insertProduct(fresh.toEntity())
                // Room Flow emits automatically → UI updates!
            }
        } catch (e: Exception) {
            // Log error, UI shows cached data
        }
    }
}
```

### Step 2: Connect Event Listener When Workspace Selected

```kotlin
// WorkspaceListViewModel or WorkspaceSelectionScreen
fun selectWorkspace(workspace: Workspace) {
    viewModelScope.launch {
        // 1. Get EventManager
        val eventManager: EventManager = koinInject {
            parametersOf(workspace.uid, userId, deviceId)
        }

        // 2. Connect to WebSocket
        eventManager.connect()

        // 3. Setup repository listeners
        val customerRepo: CustomerRepository = koinInject()
        customerRepo.setupEventListener(eventManager)

        val productRepo: ProductRepository = koinInject()
        productRepo.setupEventListener(eventManager)

        // ... setup other repositories
    }
}
```

### Step 3: That's It!

Your module now receives real-time updates from other devices. The UI updates automatically because Room DAO Flows emit when the database changes.

## How It Works

### Normal Flow
```
Another device updates product
    ↓
Backend publishes PRODUCT_UPDATED event
    ↓
EventManager receives WebSocket message
    ↓
ProductRepository.handleEvent() triggered
    ↓
Fetch fresh data from API
    ↓
Update Room database
    ↓
Room DAO Flow emits
    ↓
UI updates automatically ✅
```

### Connection Resilience
```
WebSocket connection drops (network issue, token expiry, etc.)
    ↓
EventManager detects disconnection
    ↓
Automatic reconnection scheduled (exponential backoff)
    ↓
Refresh token before reconnecting
    ↓
Retry connection (1s → 2s → 4s → ... → 30s max)
    ↓
Keep retrying indefinitely while user is in workspace
    ↓
Reconnection successful → Resume real-time updates ✅
    ↓
ONLY STOPS when user exits workspace (disconnect() called)
```

## Event Types

Use these in your event handling:

```kotlin
// Customer
EventType.CUSTOMER_CREATED
EventType.CUSTOMER_UPDATED
EventType.CUSTOMER_DELETED

// Product
EventType.PRODUCT_CREATED
EventType.PRODUCT_UPDATED
EventType.PRODUCT_DELETED
EventType.PRODUCT_STOCK_CHANGED

// Order
EventType.ORDER_CREATED
EventType.ORDER_UPDATED
EventType.ORDER_DELETED
EventType.ORDER_STATUS_CHANGED

// Invoice
EventType.INVOICE_CREATED
EventType.INVOICE_UPDATED
EventType.INVOICE_DELETED
EventType.INVOICE_PAID
```

## Advanced Usage

### Filter by Multiple Entity Types

```kotlin
manager.events
    .filter { it.entityType in listOf("product", "inventory") }
    .collect { event -> /* handle */ }
```

### Check Connection Status

```kotlin
val connectionState by eventManager.connectionState.collectAsState()

when (connectionState) {
    is ConnectionState.Connected -> showLiveBadge()
    is ConnectionState.Disconnected -> showOfflineBadge()
    is ConnectionState.Error -> showErrorBadge()
}
```

### Manual Connection Control

```kotlin
// Usually auto-connected on workspace selection
eventManager.connect()

// Disconnect when leaving workspace
eventManager.disconnect()
```

## Troubleshooting

### Events Not Arriving

1. **Check backend**: Ensure RabbitMQ STOMP broker is running
2. **Check connection**: Look for "✅ Connected to workspace" in logs
3. **Check subscription**: Look for "✅ Subscribed to workspace events"
4. **Check device ID**: Ensure events from other devices (not filtered out)

### Logs to Look For

**Successful Connection:**
```
[Event][EventManager] INFO: Connecting to workspace: workspace-123
[Event][EventManager] INFO: ✅ Connected to workspace: workspace-123
[Event][EventManager] DEBUG: 💓 Heartbeat sent
[Event][EventManager] INFO: 📨 Received event: PRODUCT_UPDATED for product:PRD...
[Event][ProductRepository] INFO: ✅ Refreshed product from server: PRD...
```

**Automatic Reconnection:**
```
[Event][EventManager] ERROR: Connection failed for workspace: workspace-123
[Event][EventManager] INFO: Scheduling reconnection in 1000ms (attempt 1)
[Event][EventManager] INFO: 🔄 Refreshing token before reconnection
[Event][EventManager] INFO: ✅ Token refreshed, attempting reconnection
[Event][EventManager] INFO: Connecting to workspace: workspace-123 (attempt 2)
[Event][EventManager] INFO: ✅ Connected to workspace: workspace-123
```

### Common Errors

**"No access token available"**
- EventManager needs JWT token to connect
- Ensure user is logged in before connecting
- Token refresh will be attempted automatically

**"Connection failed: timeout"**
- Check backend URL configuration
- Verify network connectivity
- Check backend WebSocket endpoint is accessible
- **Don't worry**: Auto-reconnection will keep trying with exponential backoff

**Connection keeps dropping**
- Check network stability
- Verify backend RabbitMQ is stable
- **Normal behavior**: EventManager will auto-reconnect indefinitely
- Backoff delays: 1s → 2s → 4s → 8s → 16s → 30s (max)
- Only stops when you call `disconnect()` or switch workspaces

## Best Practices

1. **Always use getOrNull()** when injecting EventManager in repositories
2. **Handle API errors gracefully** - offline mode should still work
3. **Use entity type strings** that match backend (e.g., "customer", "product")
4. **Don't block event collection** - use separate coroutine scope
5. **Log important events** for debugging

## Testing Your Integration

1. Run backend with STOMP enabled
2. Open app on two devices/emulators
3. Select same workspace on both
4. Modify entity on Device A
5. Verify real-time update on Device B

Expected: ~1-2 second delay for update to appear on other device.

## Need Help?

See `EVENT_SYNC_IMPLEMENTATION.md` for complete architecture documentation.
