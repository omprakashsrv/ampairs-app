# feature:agent

Agentic chat framework that powers the AI assistant in Ampairs. Provides an intent resolution + action dispatch pipeline supporting both offline (rule-based) and online (LLM) modes.

## Responsibilities

- Parse natural-language user messages into structured `AgentAction` objects
- Dispatch actions to registered module handlers (`ActionHandler`)
- Return human-readable `ActionResult` with optional navigation targets
- Present a chat UI (`ChatScreen`) with message history and action result cards

## Architecture

```
User message
    ↓
AgentOrchestrator
    ↓
IntentResolver (RuleBasedIntentResolver offline / LlmIntentResolver online)
    ↓
ActionRegistry → ActionHandler (per module)
    ↓
ActionResult (Success / Error / NeedsInput)
```

## Key Classes

| Class | Purpose |
|---|---|
| `AgentOrchestrator` | Top-level coordinator for message → result pipeline |
| `ActionRegistry` | Collects all `ActionHandlerProvider`s from Koin and routes actions |
| `ActionHandler` | Interface each feature module implements |
| `ActionHandlerProvider` | Koin-injectable factory wrapper for lazy handler creation |
| `RuleBasedIntentResolver` | Regex-based offline intent resolver |
| `IntentResolver` | Interface for pluggable online/offline resolvers |
| `ChatViewModel` | UI state management |
| `ChatScreen` | Compose chat UI |

## Koin Module

```kotlin
// feature/agent
agentModule  // in com.ampairs.agent.di
```

## Adding a New Module Handler

1. Implement `ActionHandler` in your feature module
2. Register as `ActionHandlerProvider` in your Koin module:
   ```kotlin
   factory<ActionHandlerProvider> {
       ActionHandlerProvider("mymodule", MyActionHandler.ACTIONS) {
           MyActionHandler(get())
       }
   } bind ActionHandlerProvider::class
   ```
3. Add `implementation(projects.feature.agent)` to your `build.gradle.kts`

## Supported Action Types

`CREATE`, `READ`, `UPDATE`, `DELETE`, `SEARCH`, `LIST`, `COUNT`, `SYNC`, `CALCULATE_TAX`, `GET_INVENTORY`

## Navigation

Action results can include a `NavigationTarget` with a `routeDescription` and `routeData` map. The app's `AgentEntryProvider` maps these to typed Navigation 3 routes:

| routeData key | Navigates to |
|---|---|
| `customerId` | `CustomerDetailsRoute` |
| `productId` | `ProductRoute.ProductDetails` |
| `orderId` | `OrderRoute.OrderView` |
| `invoiceId` | `InvoiceRoute.InvoiceView` |
