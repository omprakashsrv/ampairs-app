# Event Synchronization Integration Example

## Complete Integration Example: Workspace Selection with Event Sync

This example shows how to wire up EventManager and repository event listeners when a workspace is selected.

### Step 1: Create EventConnectionManager Helper

```kotlin
// com/ampairs/event/EventConnectionManager.kt
package com.ampairs.event

import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.event.util.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/**
 * Helper class to manage EventManager connection and repository event listeners.
 * Simplifies workspace selection by centralizing event setup.
 */
class EventConnectionManager : KoinComponent {
    private var connectionJob: Job? = null
    private var currentEventManager: EventManager? = null

    /**
     * Connect to workspace events and setup all repository listeners.
     *
     * @param workspaceId Workspace UID
     * @param userId Current user UID
     * @param deviceId Current device UID
     */
    fun connectToWorkspace(
        workspaceId: String,
        userId: String,
        deviceId: String,
        scope: CoroutineScope
    ) {
        // Disconnect from previous workspace if any
        disconnect()

        connectionJob = scope.launch(Dispatchers.Default) {
            try {
                // 1. Get EventManager for this workspace
                val eventManager: EventManager by inject {
                    parametersOf(workspaceId, userId, deviceId)
                }
                currentEventManager = eventManager

                // 2. Connect to WebSocket
                EventLogger.i("EventConnectionManager", "Connecting to workspace: $workspaceId")
                eventManager.connect()

                // 3. Setup repository event listeners
                setupRepositoryListeners(eventManager)

                EventLogger.i("EventConnectionManager", "✅ Event sync ready for workspace: $workspaceId")

            } catch (e: Exception) {
                EventLogger.e("EventConnectionManager", "Failed to connect to workspace events", e)
            }
        }
    }

    /**
     * Setup event listeners for all repositories that need real-time sync.
     */
    private fun setupRepositoryListeners(eventManager: EventManager) {
        try {
            // Customer module
            val customerRepo: CustomerRepository by inject()
            customerRepo.setupEventListener(eventManager)

            // Add other repositories as they implement event listeners:
            // val productRepo: ProductRepository by inject()
            // productRepo.setupEventListener(eventManager)

            // val orderRepo: OrderRepository by inject()
            // orderRepo.setupEventListener(eventManager)

            EventLogger.i("EventConnectionManager", "Repository event listeners configured")
        } catch (e: Exception) {
            EventLogger.w("EventConnectionManager", "Error setting up repository listeners", e)
        }
    }

    /**
     * Disconnect from workspace events and cleanup.
     */
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null

        currentEventManager?.let { manager ->
            CoroutineScope(Dispatchers.Default).launch {
                manager.disconnect()
            }
        }
        currentEventManager = null

        // Stop repository listeners
        try {
            val customerRepo: CustomerRepository by inject()
            customerRepo.stopEventListener()
        } catch (e: Exception) {
            // Ignore if repository not available
        }

        EventLogger.i("EventConnectionManager", "Disconnected from workspace events")
    }

    /**
     * Get current connection state.
     */
    fun isConnected(): Boolean {
        return currentEventManager?.isConnected() ?: false
    }
}
```

### Step 2: Use in Workspace Selection

```kotlin
// WorkspaceListViewModel.kt
class WorkspaceListViewModel(
    private val workspaceRepository: WorkspaceRepository,
    private val authRepository: AuthRepository,
    private val deviceService: DeviceService
) : ViewModel() {

    private val eventConnectionManager = EventConnectionManager()

    fun selectWorkspace(workspace: Workspace, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Set workspace context for database isolation
                WorkspaceContextManager.setCurrentWorkspace(workspace)

                // 2. Connect to event sync (WebSocket + repository listeners)
                val userId = authRepository.getCurrentUserId()
                val deviceId = deviceService.getDeviceId()

                eventConnectionManager.connectToWorkspace(
                    workspaceId = workspace.uid,
                    userId = userId,
                    deviceId = deviceId,
                    scope = viewModelScope
                )

                // 3. Navigate to workspace
                onSuccess()

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onWorkspaceDeselected() {
        eventConnectionManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        eventConnectionManager.disconnect()
    }
}
```

### Step 3: Alternative - Manual Setup in Screen/ViewModel

If you prefer direct control instead of using EventConnectionManager:

```kotlin
// WorkspaceListViewModel.kt (Manual approach)
class WorkspaceListViewModel(...) : ViewModel() {

    private var eventManager: EventManager? = null

    fun selectWorkspace(workspace: Workspace) {
        viewModelScope.launch {
            // 1. Set workspace context
            WorkspaceContextManager.setCurrentWorkspace(workspace)

            // 2. Get EventManager
            eventManager = koinInject {
                parametersOf(
                    workspace.uid,
                    authRepository.getCurrentUserId(),
                    deviceService.getDeviceId()
                )
            }

            // 3. Connect WebSocket
            eventManager?.connect()

            // 4. Setup repository listeners
            val customerRepo: CustomerRepository = koinInject()
            customerRepo.setupEventListener(eventManager!!)

            val productRepo: ProductRepository = koinInject()
            productRepo.setupEventListener(eventManager!!)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            eventManager?.disconnect()
        }
    }
}
```

## Testing the Integration

### Expected Log Output

When everything is wired correctly, you should see these logs:

```
[Event][EventConnectionManager] INFO: Connecting to workspace: workspace-123
[Event][EventManager] INFO: Connecting to workspace: workspace-123
[Event][EventManager] INFO: ✅ Connected to workspace: workspace-123
[Event][EventManager] INFO: Subscribing to: /topic/workspace.events.workspace-123
[Event][EventManager] INFO: ✅ Subscribed to workspace events
[Event][CustomerRepository] INFO: Real-time event listener initialized for customer module
[Event][EventConnectionManager] INFO: Repository event listeners configured
[Event][EventConnectionManager] INFO: ✅ Event sync ready for workspace: workspace-123
[Event][EventManager] DEBUG: 💓 Heartbeat sent
```

### When Event Arrives from Another Device

```
[Event][EventManager] INFO: 📨 Received event: CUSTOMER_UPDATED for customer:CUS20250104... (seq: 42)
[Event][CustomerRepository] INFO: 📨 Received event: CUSTOMER_UPDATED for customer: CUS20250104...
[Event][CustomerRepository] INFO: ✅ Refreshed customer from server: CUS20250104...
```

## Common Issues & Solutions

### Issue: "Could not create instance for EventManager"

**Cause:** EventManager requires parameters (workspaceId, userId, deviceId)

**Solution:** Always use `parametersOf()` when injecting:
```kotlin
val eventManager: EventManager = koinInject {
    parametersOf(workspaceId, userId, deviceId)
}
```

### Issue: Events not received

**Checklist:**
- [ ] Backend RabbitMQ STOMP broker running on port 61613
- [ ] EventManager.connect() called
- [ ] Repository.setupEventListener() called
- [ ] Check logs for "✅ Connected to workspace"
- [ ] Check logs for "✅ Subscribed to workspace events"
- [ ] Verify event is from different deviceId (own events are filtered)

### Issue: UI not updating

**Checklist:**
- [ ] Repository is updating Room database (check logs for "✅ Refreshed...")
- [ ] ViewModel observes Flow from repository
- [ ] UI collectAsState() from ViewModel StateFlow

## Next Steps

1. Create `EventConnectionManager.kt` helper class (optional but recommended)
2. Update `WorkspaceListViewModel` to use event sync
3. Test with two devices/emulators
4. Add event listeners to Product, Order, Invoice repositories (same pattern)
5. Add UI connection status indicator (see EVENT_SYNC_IMPLEMENTATION.md)
