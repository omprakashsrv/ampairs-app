# Workspace Context Management Architecture

## Overview

This package implements **state-based workspace context management** as an alternative to URL-based workspace routing. This approach solves the deep linking limitations identified with URL-based workspace context patterns like `/w/{slug}/customer/edit/123`.

## Architecture Components

### 1. WorkspaceContextManager (Singleton)
- **Purpose**: Global state management for current workspace context
- **State**: Reactive StateFlow for workspace context, selection status, and loading states
- **Lifecycle**: Persists across navigation, cleared only on logout or manual reset

### 2. WorkspaceAwareContent (Composable)
- **Purpose**: Wrapper component that ensures workspace context before rendering content
- **Behavior**: Automatically redirects to workspace selection if no workspace is selected
- **Usage**: Wrap any screen that requires workspace context

### 3. DeepLinkingStrategy (Object)
- **Purpose**: Handle deep links while preserving workspace context
- **Approach**: Workspace-agnostic URLs with app state-based context
- **Benefits**: Clean URLs, reliable deep linking, preserved workspace sessions

## Key Benefits

### 1. Deep Linking Support
**Problem with URL-based approach:**
```
/w/acme-corp/customer/edit/123
- Workspace context must be extracted from URL
- Complex URL parsing and validation
- Workspace slug can change, breaking links
- Difficult to handle missing workspace context in URL
```

**Solution with state-based approach:**
```
/customer/edit/123
- Clean, simple URLs
- Workspace context maintained in app state
- Reliable deep linking regardless of workspace
- Automatic workspace selection if needed
```

### 2. Navigation Simplicity
**Before:**
```kotlin
// Complex URL construction with workspace context
navController.navigate("/w/${workspaceSlug}/customer/edit/${customerId}")
```

**After:**
```kotlin
// Simple navigation, workspace context in state
navController.navigate(CustomerRoute.Edit(customerId))
```

### 3. Cross-Module Navigation
**Before:**
```kotlin
// Workspace context lost when navigating between modules
navController.navigate(Route.Home) // Loses workspace context
```

**After:**
```kotlin
// Workspace context preserved across all navigation
navController.navigate(Route.Home) // Workspace context remains
```

## Implementation Guide

### 1. Setting Workspace Context

```kotlin
// In WorkspaceListScreen when user selects workspace
onWorkspaceSelected = { workspace ->
    WorkspaceContextIntegration.setWorkspaceFromDomain(workspace)
    navController.navigate(Route.Home) // or any other route
}
```

### 2. Using Workspace Context in Screens

```kotlin
@Composable
fun CustomerListScreen() {
    WorkspaceAwareContent(
        onWorkspaceSelectionRequired = {
            navController.navigate(Route.Workspace)
        }
    ) { workspaceContext ->
        // Screen content with guaranteed workspace context
        val viewModel: CustomerListViewModel = koinInject {
            parametersOf(workspaceContext.id)
        }
        // ... rest of screen
    }
}
```

### 3. Handling Deep Links

```kotlin
// Deep link: /customer/edit/123
DeepLinkingStrategy.handleDeepLink(
    deepLinkUrl = "/customer/edit/123",
    onNavigateToWorkspaceSelection = { returnPath ->
        // No workspace context, redirect to workspace selection
        navController.navigate(Route.Workspace)
        // Store returnPath for navigation after workspace selection
    },
    onNavigateToTarget = { route ->
        // Workspace context exists, navigate directly
        navController.navigate(route)
    }
)
```

## Migration from URL-based Routing

### Phase 1: Parallel Implementation
1. Keep existing WorkspaceRoute with workspaceId parameters for backward compatibility
2. Add WorkspaceContextManager alongside existing routing
3. Update screens to use WorkspaceAwareContent where beneficial

### Phase 2: Gradual Migration
1. Update WorkspaceListScreen to set workspace context on selection
2. Migrate high-traffic screens to use workspace context from state
3. Update deep linking handlers to use DeepLinkingStrategy

### Phase 3: Complete Migration
1. Remove workspaceId parameters from routes where no longer needed
2. Simplify navigation patterns across the app
3. Update all deep linking to use workspace-agnostic URLs

## Comparison Table

| Aspect | URL-based (`/w/{slug}/...`) | State-based (this approach) |
|--------|------------------------------|------------------------------|
| **Deep Links** | Complex parsing, fragile | Simple, reliable |
| **URL Cleanliness** | Long, complex URLs | Clean, simple URLs |
| **Navigation** | Workspace slug required | Automatic from state |
| **Context Persistence** | Lost on URL change | Persists across navigation |
| **Error Handling** | Complex URL validation | Simple state validation |
| **Performance** | URL parsing overhead | Direct state access |
| **User Experience** | Confusing URLs | Intuitive navigation |

## Best Practices

### 1. Always Check Workspace Context
```kotlin
// In ViewModels that need workspace context
class CustomerListViewModel(
    private val workspaceId: String? = WorkspaceContextIntegration.getCurrentWorkspaceId()
) {
    init {
        require(workspaceId != null) { "Workspace context required" }
    }
}
```

### 2. Handle Workspace Selection Gracefully
```kotlin
// In screens that require workspace
@Composable
fun MyScreen() {
    if (WorkspaceContextIntegration.isWorkspaceSelected()) {
        // Show main content
        MainContent()
    } else {
        // Show workspace selection prompt
        WorkspaceSelectionPrompt()
    }
}
```

### 3. Clear Context on Logout
```kotlin
// In logout handler
fun logout() {
    WorkspaceContextIntegration.clearWorkspaceContext()
    navController.navigate(Route.Login) {
        popUpTo(0)
    }
}
```

## Future Enhancements

1. **Persistent Storage**: Save workspace context to local storage for app restart persistence
2. **Multi-workspace Sessions**: Support multiple workspace contexts for power users
3. **Workspace Switching**: Quick workspace switching without full navigation reset
4. **Context Validation**: Automatic workspace context refresh and validation
5. **Analytics Integration**: Track workspace usage patterns and navigation flows

This architecture provides a robust foundation for workspace-aware navigation while maintaining clean URLs and reliable deep linking capabilities.