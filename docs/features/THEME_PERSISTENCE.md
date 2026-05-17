# Theme Persistence with DataStore

This document explains how theme preferences are now persisted using DataStore in the Ampairs KMP app.

## ✅ **Implementation Status**

### **Completed Features**
- ✅ **Theme persistence interface** - `ThemePreferencesDataStore`
- ✅ **Android DataStore implementation** - Full DataStore support with preferences
- ✅ **In-memory fallback** - For Desktop and iOS platforms
- ✅ **Dependency injection** - Koin modules for all platforms
- ✅ **Theme state management** - Reactive theme updates with Flow
- ✅ **Compilation verified** - All platforms compile successfully

### **Platform Support**

| Platform | DataStore Type | Persistence | Status |
|----------|---------------|-------------|---------|
| **Android** | DataStore Preferences | ✅ Persistent | **Ready** |
| **Desktop** | In-Memory | ❌ Session-only | **Functional** |
| **iOS** | In-Memory | ❌ Session-only | **Functional** |

## 🏗️ **Architecture**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Presentation Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   Compose   │  │ ThemeManager│  │   UI State  │             │
│  │ Multiplatform │  │ Singleton   │  │ Management  │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│                Repository Layer                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ThemeRepository│  │   Flow      │  │ Reactive    │           │
│  │    (DI)     │  │ Handling    │  │ Updates     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│               DataStore Layer                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │  Platform   │  │   Android   │  │ Desktop/iOS │             │
│  │ Interface   │  │ DataStore   │  │ In-Memory   │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

## 📱 **Android DataStore Implementation**

The Android implementation uses DataStore Preferences for true persistence:

```kotlin
class AndroidThemeDataStore(context: Context) : ThemePreferencesDataStore {

    override fun getThemePreference(): Flow<ThemePreference> {
        return context.dataStore.data.map { preferences ->
            val preferenceString = preferences[THEME_PREFERENCE_KEY] ?: DEFAULT_THEME_PREFERENCE
            ThemePreference.valueOf(preferenceString)
        }
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = preference.name
        }
    }
}
```

**Storage Location (Android):**
- Path: `/data/data/com.ampairs.app/files/datastore/theme_preferences.preferences_pb`
- Format: Protocol Buffers (DataStore format)
- Persistent across app launches and device reboots

## 🖥️ **Desktop & iOS Fallback**

Desktop and iOS currently use in-memory storage that resets on app restart:

```kotlin
private class InMemoryThemeDataStore : ThemePreferencesDataStore {
    private val _themePreference = MutableStateFlow(ThemePreference.LIGHT)

    override fun getThemePreference() = _themePreference

    override suspend fun setThemePreference(preference: ThemePreference) {
        _themePreference.value = preference
        println("💻/📱 Theme preference: $preference")
    }
}
```

## 🔧 **Usage**

### **In UI Code**
```kotlin
@Composable
fun MyScreen() {
    val themeManager = remember { ThemeManager.getInstance() }
    val isDarkTheme = themeManager.isDarkTheme()

    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        // Your UI content
    }
}
```

### **Changing Theme**
```kotlin
val themeManager = ThemeManager.getInstance()

// Set to dark theme
themeManager.setThemePreference(ThemePreference.DARK)

// Set to light theme
themeManager.setThemePreference(ThemePreference.LIGHT)

// Set to follow system theme
themeManager.setThemePreference(ThemePreference.SYSTEM)
```

### **Observing Theme Changes**
```kotlin
@Composable
fun ThemeAwareComponent() {
    val themeManager = remember { ThemeManager.getInstance() }
    val themePreference by themeManager.themePreference.collectAsState()

    Text("Current theme: $themePreference")
}
```

## 🚀 **Dependency Injection Setup**

### **Add to Koin Modules**

Make sure to include the theme modules in your app initialization:

```kotlin
// In your app initialization
startKoin {
    modules(
        // ... other modules
        themeModule,            // Common theme module
        androidThemeModule,     // Android-specific (when on Android)
        desktopThemeModule,     // Desktop-specific (when on Desktop)
        iosThemeModule         // iOS-specific (when on iOS)
    )
}
```

### **Android Context Requirement**

For Android, ensure the Application context is provided to Koin:

```kotlin
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(
                themeModule,
                androidThemeModule,
                // ... other modules
            )
        }
    }
}
```

## 🔮 **Future Enhancements**

### **Desktop DataStore Implementation**
```kotlin
// TODO: Implement proper DataStore for Desktop
class DesktopThemeDataStore : ThemePreferencesDataStore {
    private val dataStore = PreferenceDataStoreFactory.createWithPath {
        Paths.get(System.getProperty("user.home"), ".ampairs", "theme_preferences.preferences_pb")
    }

    // Implementation similar to Android
}
```

### **iOS DataStore Implementation**
```kotlin
// TODO: Implement proper DataStore for iOS
class IosThemeDataStore : ThemePreferencesDataStore {
    private val dataStore = PreferenceDataStoreFactory.createWithPath {
        // Use iOS Documents directory
        getIosDocumentsPath().resolve("theme_preferences.preferences_pb")
    }

    // Implementation similar to Android
}
```

## 🧪 **Testing**

### **Manual Testing**

1. **Android Testing:**
   ```bash
   ./gradlew composeApp:assembleDebug
   ./gradlew composeApp:installDebug
   ```
   - Change theme in app
   - Force-close app
   - Reopen app
   - ✅ Theme should be preserved

2. **Desktop Testing:**
   ```bash
   ./gradlew composeApp:run
   ```
   - Change theme in app
   - Close app
   - Reopen app
   - ❌ Theme resets to LIGHT (expected behavior)

### **Unit Testing**
```kotlin
@Test
fun testThemeRepository() = runTest {
    val dataStore = InMemoryThemeDataStore()
    val repository = ThemeRepository(dataStore)

    // Test setting theme
    repository.setThemePreference(ThemePreference.DARK)

    // Test getting theme
    val theme = repository.themePreference.first()
    assertEquals(ThemePreference.DARK, theme)
}
```

## 📋 **Migration Notes**

### **From Previous Implementation**
The old `ThemeManager` had a TODO comment:
```kotlin
// TODO: Persist preference to local storage
```

This has been **fully implemented** with:
- ✅ DataStore integration
- ✅ Platform-specific implementations
- ✅ Dependency injection setup
- ✅ Reactive state management
- ✅ Proper error handling

### **Breaking Changes**
None - the `ThemeManager.getInstance()` API remains the same, but now includes persistence.

## 🐛 **Troubleshooting**

### **"KoinApplication has not been started"**
Make sure theme modules are included in Koin initialization:
```kotlin
startKoin {
    modules(themeModule, androidThemeModule)
}
```

### **Theme Not Persisting on Android**
Check that:
1. Android context is provided to Koin
2. App has proper storage permissions
3. DataStore preferences dependency is included

### **Compilation Errors**
Ensure DataStore dependencies are in `build.gradle.kts`:
```kotlin
implementation(libs.datastore)
implementation(libs.datastore.preferences)
```

## 🎯 **Summary**

✅ **Theme persistence is now fully functional on Android** with DataStore
✅ **All platforms compile and run** with appropriate fallbacks
✅ **API remains backward compatible** with existing theme usage
🔄 **Desktop and iOS persistence** can be added in future iterations

The theme preference will now persist across app launches on Android, solving the original issue of theme resetting on each launch.