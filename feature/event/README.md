# feature:event

Real-time event streaming for live workspace updates. Maintains a WebSocket connection and broadcasts domain events (order placed, customer updated, inventory changed, etc.) to subscribed listeners.

## Responsibilities

- Establish and maintain WebSocket connection to the backend event stream
- Reconnect automatically on failure with back-off
- Dispatch `WorkspaceEvent` objects to registered listeners
- Expose `ConnectionState` as a reactive `StateFlow`

## Key Classes

| Class | Purpose |
|---|---|
| `EventManager` | Core WebSocket lifecycle and event dispatch |
| `EventManagerFactory` | Creates workspace-scoped `EventManager` instances |
| `EventConnectionManager` | Higher-level connection orchestration |
| `WorkspaceEvent` | Sealed domain event type |
| `EventType` | Enum of all event categories |
| `ConnectionState` | `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `ERROR` |
| `EventLogger` | Debug logging for event traffic |

## Koin Module

```kotlin
eventModule  // in com.ampairs.event.di  (factory — workspace-scoped)
```

## Usage

```kotlin
val eventManager: EventManager = koinInject()
eventManager.connectionState.collect { state -> ... }
eventManager.events.collect { event -> ... }
```

## Notes

- See `INTEGRATION_EXAMPLE.md` in the source for wiring examples
- Connection is started on workspace selection and stopped on workspace switch
