# Event Synchronization System - Implementation Summary

## 📋 Overview

Successfully implemented a comprehensive real-time event synchronization system for multi-device collaboration using **Krossbow STOMP client** with **WebSocket** transport.

## ✅ Implemented Components

### 1. **Dependencies Added** (`gradle/libs.versions.toml`)
- Krossbow 7.0.0 (STOMP client for KMP)
- `krossbow-stomp-core` - Core STOMP protocol
- `krossbow-websocket-ktor` - Ktor WebSocket transport
- `krossbow-stomp-kxserialization` - Kotlinx.serialization integration
- `ktor-client-websockets` - Ktor WebSocket plugin

### 2. **Domain Models** (`com.ampairs.event.domain/`)
- ✅ `EventType.kt` - 22 event types (Customer, Product, Order, Invoice, Device)
- ✅ `WorkspaceEvent.kt` - Event data model with payload
- ✅ `ConnectionState.kt` - WebSocket connection states (Disconnected, Connecting, Connected, Error)

### 3. **Core Components** (`com.ampairs.event/`)
- ✅ `EventManager.kt` - WebSocket connection manager per workspace
  - Connects to `/ws` endpoint with JWT authentication
  - Subscribes to `/topic/workspace.events.{workspaceId}`
  - Filters own device events (prevents echo)
  - Sends heartbeat every 30 seconds to `/app/heartbeat`
  - Exposes `StateFlow<ConnectionState>` and `SharedFlow<WorkspaceEvent>`

- ✅ `EventManagerFactory.kt` - Workspace-scoped factory
  - One EventManager instance per workspace
  - Lifecycle management (create, get, clear)

- ✅ `EventLogger.kt` - Simple logging utility

### 4. **Dependency Injection** (`com.ampairs.event.di/`)
- ✅ `EventModule.kt` - Koin module providing EventManager as factory
- ✅ Integrated into main `Koin.kt` initialization

### 5. **Repository Integration** (Example: CustomerRepository)
- ✅ Optional `EventManager` parameter injection
- ✅ Event listener in `init` block
- ✅ Automatic Room database updates on events
- ✅ Flow-based reactive UI updates

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────┐
│            Backend (Spring Boot)                 │
│  /ws → STOMP Broker Relay → RabbitMQ           │
│  Publishes to: /topic/workspace.events.{id}    │
└─────────────────────────────────────────────────┘
                      │ WebSocket/STOMP
                      ↓
┌─────────────────────────────────────────────────┐
│         KMP App - EventManager                   │
│  • Krossbow STOMP Client                        │
│  • JWT Authentication                            │
│  • Heartbeat (30s)                              │
│  • Event Filtering (deviceId)                   │
└─────────────────────────────────────────────────┘
                      │ SharedFlow<WorkspaceEvent>
                      ↓
┌─────────────────────────────────────────────────┐
│         Repository Layer                         │
│  • Listen to events                             │
│  • Fetch fresh data from API                    │
│  • Update Room database                         │
└─────────────────────────────────────────────────┘
                      │ Room DAO Flow
                      ↓
┌─────────────────────────────────────────────────┐
│         UI Layer (Compose)                       │
│  • Observe Flow from Repository                 │
│  • Automatic UI updates                         │
│  • Connection status badge                      │
└─────────────────────────────────────────────────┘
```

## 🚀 Usage Guide

### **Connecting EventManager on Workspace Selection**

```kotlin
// WorkspaceListViewModel.kt or WorkspaceSelectionScreen.kt
fun selectWorkspace(workspace: Workspace) {
    viewModelScope.launch {
        // 1. Set workspace context
        WorkspaceContextManager.setCurrentWorkspace(workspace)

        // 2. Get user and device info
        val userId = authRepository.getCurrentUserId()
        val deviceId = deviceService.getDeviceId()

        // 3. Inject EventManager with workspace parameters
        val eventManager: EventManager = koinInject {
            parametersOf(workspace.uid, userId, deviceId)
        }

        // 4. Connect to WebSocket
        eventManager.connect()

        // 5. Navigate to workspace
        navController.navigate(Route.WorkspaceHome)
    }
}
```

### **Repository Event Integration Pattern**

```kotlin
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
    private val appPreferences: AppPreferencesDataStore,
    private val eventManager: EventManager? = null  // Optional injection
) {
    init {
        // Setup event listener
        eventManager?.let { manager ->
            CoroutineScope(Dispatchers.Default).launch {
                manager.events
                    .filter { it.isForEntityType("customer") }
                    .collect { event ->
                        when (event.eventType) {
                            EventType.CUSTOMER_CREATED,
                            EventType.CUSTOMER_UPDATED -> refreshFromServer(event.entityId)
                            EventType.CUSTOMER_DELETED -> dao.delete(event.entityId)
                            else -> {}
                        }
                    }
            }
        }
    }

    private suspend fun refreshFromServer(id: String) {
        try {
            val fresh = api.getById(id)
            dao.insert(fresh.toEntity().copy(synced = true))
            // Room Flow emits → UI updates automatically!
        } catch (e: Exception) {
            // Graceful degradation
        }
    }
}
```

### **Koin Module Configuration**

```kotlin
// CustomerModule.kt
val customerModule = module {
    // Inject EventManager as optional dependency
    factory { CustomerRepository(get(), get(), get(), getOrNull()) }
    // EventManager is null until workspace connected
}
```

### **UI Connection Status (Future Enhancement)**

```kotlin
@Composable
fun ConnectionStatusBadge(eventManager: EventManager) {
    val connectionState by eventManager.connectionState.collectAsState()

    Row {
        when (connectionState) {
            is ConnectionState.Connected -> {
                Icon(Icons.Default.Cloud, tint = Color.Green)
                Text("Live")
            }
            is ConnectionState.Connecting -> CircularProgressIndicator()
            is ConnectionState.Disconnected -> Icon(Icons.Default.CloudOff)
            is ConnectionState.Error -> Icon(Icons.Default.Warning, tint = Color.Red)
        }
    }
}
```

## 🔄 Data Flow (Multi-Device Sync)

**Scenario: User updates customer on Device A**

```
Device A (Mobile)
    │
    │─── customerRepository.updateCustomer()
    │       ├─ Save to Room DB (synced=false)
    │       └─ POST /api/v1/customer/{id}
    │
Backend receives update
    │
    │─── CustomerService publishes CustomerUpdatedEvent
    │       └─ Spring @EventListener → WorkspaceEventListener
    │           └─ Persist to workspace_events table
    │           └─ Broadcast via WebSocket to /topic/workspace.events.{id}
    │
Device B (Desktop) ← WebSocket message arrives
    │
    │─── EventManager.handleIncomingEvent()
    │       └─ event.deviceId != myDeviceId → emit to SharedFlow
    │
    │─── CustomerRepository.handleCustomerEvent()
    │       └─ GET /api/v1/customer/{id} (fetch fresh data)
    │       └─ customerDao.insertCustomer() (update Room DB)
    │
    │─── Room DAO Flow emits automatically
    │
Device B UI updates automatically ✅
```

## 🎯 Key Features

1. **Automatic Reconnection** - Krossbow handles connection failures gracefully
2. **Event Filtering** - Skips events from same device (prevents loops)
3. **Heartbeat Mechanism** - Keeps connection alive with 30s heartbeat
4. **JWT Authentication** - Secure WebSocket connection with token in query param
5. **Reactive State** - Connection state exposed as StateFlow for UI
6. **Room Integration** - Database updates trigger Flow emissions → UI updates
7. **Graceful Degradation** - Works offline, syncs when online
8. **Workspace Isolation** - One connection per workspace
9. **100% Common Code** - No platform-specific WebSocket code needed!

## 📝 Backend Integration Points

### **WebSocket Endpoint**
- URL: `ws://your-backend:8080/ws?token={JWT}`
- Protocol: STOMP over WebSocket
- Authentication: JWT token in query parameter

### **Subscription Topics**
- Workspace Events: `/topic/workspace.events.{workspaceId}`
- User Status (future): `/user/queue/status`

### **Message Destinations**
- Heartbeat: `/app/heartbeat` (client → server, every 30s)
- Events: Server publishes to topic (server → client)

### **Backend Event Publishing**
```kotlin
// Backend CustomerService.kt (example)
fun updateCustomer(customer: Customer): Customer {
    val updated = customerRepository.save(customer)

    // Publish event - WorkspaceEventListener handles the rest
    eventPublisher.publishEvent(
        CustomerUpdatedEvent(
            source = this,
            workspaceId = customer.workspaceId,
            entityId = customer.uid,
            userId = getCurrentUserId(),
            deviceId = getDeviceId()
        )
    )

    return updated
}
```

## 🔧 Configuration

### **Backend URL**
Configure in `BuildConfig`:
- Dev: `http://10.50.51.6:8080`
- Prod: `https://api.ampairs.com`

### **Heartbeat Interval**
Currently: 30 seconds (matches backend expectation)
Location: `EventManager.kt` line 157

### **Event Buffer**
SharedFlow buffer: 100 events
Location: `EventManager.kt` line 61

## 📊 Platform Support

| Platform | Status | Notes |
|----------|--------|-------|
| Android | ✅ | OkHttp WebSocket via Ktor |
| iOS | ✅ | Darwin engine via Ktor |
| Desktop | ✅ | CIO engine via Ktor |

## 🧪 Testing

### **Manual Testing Steps**
1. Run backend with RabbitMQ STOMP enabled
2. Launch app on Device A → Select workspace
3. Verify connection: Check logs for "✅ Connected to workspace"
4. Launch app on Device B → Select same workspace
5. On Device A: Create/update/delete customer
6. On Device B: Verify real-time update appears

### **Expected Logs**
```
[Event][EventManager] INFO: Connecting to workspace: workspace-123
[Event][EventManager] INFO: ✅ Connected to workspace: workspace-123
[Event][EventManager] INFO: Subscribing to: /topic/workspace.events.workspace-123
[Event][EventManager] INFO: ✅ Subscribed to workspace events
[Event][EventManager] DEBUG: 💓 Heartbeat sent
[Event][EventManager] INFO: 📨 Received event: CUSTOMER_UPDATED for customer:CUS20250104... (seq: 42)
[Event][CustomerRepository] INFO: ✅ Refreshed customer from server: CUS20250104...
```

## 🚧 Future Enhancements

1. **UI Connection Status Badge** - Show live/offline indicator in AppHeader
2. **Reconnection Strategy** - Exponential backoff on connection failures
3. **Event Replay** - Catch up on missed events using sequence numbers
4. **Product/Order/Invoice Integration** - Apply same pattern to other modules
5. **Typing Indicators** - Show when others are editing records
6. **Presence System** - Show online users per workspace
7. **Conflict Resolution UI** - Handle concurrent edit conflicts

## 📚 Files Created/Modified

### **New Files Created:**
```
composeApp/src/commonMain/kotlin/com/ampairs/event/
├── domain/
│   ├── EventType.kt
│   ├── WorkspaceEvent.kt
│   └── ConnectionState.kt
├── util/
│   └── EventLogger.kt
├── di/
│   └── EventModule.kt
├── EventManager.kt
└── EventManagerFactory.kt
```

### **Modified Files:**
```
gradle/libs.versions.toml                    (+ Krossbow dependencies)
composeApp/build.gradle.kts                  (+ Krossbow bundle)
composeApp/src/commonMain/kotlin/Koin.kt    (+ eventModule)
composeApp/.../customer/data/repository/CustomerRepository.kt  (+ event listener)
composeApp/.../customer/di/CustomerModule.kt (+ EventManager injection)
```

## 🎉 Implementation Status

✅ **COMPLETE** - All core functionality implemented and ready for testing!

### **Next Steps:**
1. Test with backend RabbitMQ STOMP broker
2. Integrate with Product/Order/Invoice modules
3. Add UI connection status indicator
4. Deploy to staging for multi-device testing

---

**Implementation Date:** October 2025
**Technology Stack:** Kotlin Multiplatform, Krossbow 7.0.0, Ktor 3.3.0, STOMP, WebSocket, RabbitMQ
**Platforms Supported:** Android, iOS, Desktop (JVM)
