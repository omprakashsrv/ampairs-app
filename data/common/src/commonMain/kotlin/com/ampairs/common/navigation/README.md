# Cross-Platform Back Navigation System

This system provides consistent back navigation behavior across Android and Desktop platforms in our Kotlin
Multiplatform Compose application.

## Features

### Android Support

- **Hardware Back Button**: Automatically handles physical back button presses
- **Gesture Navigation**: Works with Android gesture navigation
- **System Integration**: Uses `BackHandler` from Activity Compose

### Desktop Support

- **Escape Key**: Primary back navigation key (common pattern)
- **Alt+Left Arrow**: Browser-style back navigation
- **Backspace**: Back navigation when no text field is focused
- **Keyboard Focus Management**: Smart focus handling to avoid conflicts

## Usage

### Simple Screen with Back Navigation

```kotlin
@Composable
fun MyScreen(navController: NavController) {
    ScreenWithBackNavigation(
        navController = navController,
        fallbackRoute = Route.Home  // Where to go if no back stack
    ) {
        // Your screen content here
        MyScreenContent()
    }
}
```

### Custom Back Navigation Logic

```kotlin
@Composable
fun MyScreen(navController: NavController) {
    ScreenWithCustomBackNavigation(
        onBackPressed = {
            // Custom logic here
            if (hasUnsavedChanges) {
                showSaveDialog()
            } else {
                navController.popBackStack()
            }
        }
    ) {
        // Your screen content here
        MyScreenContent()
    }
}
```

### Advanced Navigation Control

```kotlin
@Composable
fun MyScreen(navController: NavController) {
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    
    ScreenWithBackNavigation(
        navController = navController,
        enabled = !isLoading,  // Disable during loading
        fallbackRoute = Route.Home,
        onBackPressed = if (hasUnsavedChanges) {
            { showConfirmDialog() }
        } else null  // Use default navigation
    ) {
        MyScreenContent()
    }
}
```

## Navigation Utilities

### Safe Back Navigation

```kotlin
// Navigate back safely, returns true if successful
val success = navController.navigateBack()

if (!success) {
    // Handle case where there's no back stack
    navController.navigate(Route.Home)
}
```

### Navigate Back to Specific Route

```kotlin
// Navigate back to a specific route, with fallback
navController.navigateBackTo(
    route = Route.Home,
    fallbackRoute = Route.Login
)
```

## Platform-Specific Behavior

### Android

- Respects system back button behavior
- Integrates with Android's activity lifecycle
- Handles edge-to-edge gestures correctly

### Desktop

- **Escape**: Universal back action
- **Alt+Left**: Browser-style back (familiar to users)
- **Backspace**: Back when not typing (careful focus management)

## Implementation in Existing Screens

### Before (Manual Implementation)

```kotlin
@Composable
fun WorkspaceCreateScreen(
    onNavigateBack: () -> Unit,
    // ... other params
) {
    // Manual back handling per screen
}
```

### After (Unified System)

```kotlin
@Composable
fun WorkspaceCreateScreen(
    navController: NavController,
    // ... other params
) {
    ScreenWithBackNavigation(navController) {
        // Screen content - back navigation handled automatically
    }
}
```

## Best Practices

1. **Use fallbackRoute**: Always provide a fallback route for when there's no back stack
2. **Handle Loading States**: Disable back navigation during critical operations
3. **Unsaved Changes**: Implement custom back logic for forms with unsaved data
4. **Consistent UX**: Use the same back navigation pattern throughout the app

## Migration Guide

1. Replace manual `BackHandler` implementations with `ScreenWithBackNavigation`
2. Remove platform-specific back button handling from individual screens
3. Use `NavigationUtils` for programmatic navigation
4. Test on both Android and Desktop platforms

## Examples in Codebase

- `WorkspaceListScreen`: Basic back navigation with fallback
- `WorkspaceCreateScreen`: Back navigation with form handling
- `AppNavigation.kt`: Root navigation setup with error handling

This system ensures a consistent and platform-appropriate back navigation experience across the entire application.