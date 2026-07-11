# Material 3 Theming & Components

This project themes with **Material 3** + **Material Kolor** (dynamic color generation) across all
platforms — never Android-only system dynamic color. Theming is fully cross-platform.

## Project Theme Pattern

The app theme lives in `shared/src/commonMain/kotlin/com/ampairs/ui/theme/Theme.kt`. Apply it with
`PlatformAmpairsTheme` (which adds density awareness) at the root of `App.kt`; `AmpairsTheme` is the
inner Material wrapper.

```kotlin
@Composable
fun AmpairsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    density: MaterialDensity = MaterialDensity.DEFAULT,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = createDensityAwareTypography(density)
    DensityTheme(density = density) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}

// root usage — darkTheme comes from the injected ThemeManager
PlatformAmpairsTheme(darkTheme = themeManager.isDarkTheme()) { /* App content */ }
```

### Key Rules

- The dark/light `ColorScheme`s are generated with Material Kolor (`3.0.1`) and work identically on
  Android, iOS, and Desktop — no platform conditionals.
- `isSystemInDarkTheme()` is available in `commonMain` via Compose Multiplatform, but the effective
  dark/light choice flows from `ThemeManager` (`SYSTEM`/`LIGHT`/`DARK`), injected via Metro — do NOT
  read `LocalAppGraph` for it.
- **Do not use** `dynamicDarkColorScheme(context)` or `Build.VERSION.SDK_INT >= …S` — that is
  Android-12-only and breaks cross-platform consistency.

### Material Kolor vs Android Dynamic Color

| | Material Kolor (this project) | Android Dynamic Color |
|---|---|---|
| Platforms | All (Android, iOS, Desktop) | Android 12+ only |
| Color source | Seed color you define | Extracted from wallpaper |
| Consistency | Same colors on all platforms | Varies per device |

## Color Roles and Dark/Light

Always pair container + content colors correctly:

| Container | Content on it |
|---|---|
| `primary` | `onPrimary` |
| `primaryContainer` | `onPrimaryContainer` |
| `secondary` | `onSecondary` |
| `secondaryContainer` | `onSecondaryContainer` |
| `surface` | `onSurface` |
| `surfaceVariant` | `onSurfaceVariant` |
| `error` | `onError` |
| `errorContainer` | `onErrorContainer` |

Access via `MaterialTheme.colorScheme.*`. Never hardcode hex colors in components.

## Typography and Shapes

M3 defines 15 text styles across 5 categories. Ampairs scales them per device density via
`createDensityAwareTypography(density)` — read styles through `MaterialTheme.typography.*`, don't
build ad-hoc `TextStyle`s inline.

| Category | Sizes |
|---|---|
| Display | `displayLarge`, `displayMedium`, `displaySmall` |
| Headline | `headlineLarge`, `headlineMedium`, `headlineSmall` |
| Title | `titleLarge`, `titleMedium`, `titleSmall` |
| Body | `bodyLarge`, `bodyMedium`, `bodySmall` |
| Label | `labelLarge`, `labelMedium`, `labelSmall` |

M3 shape scale: `extraSmall`, `small`, `medium`, `large`, `extraLarge`. Use defaults; override only
for brand corner radii.

## Component Decision Matrix

### Scaffold

| Slot | When to use |
|---|---|
| `topBar` | Screen has a top app bar |
| `bottomBar` | Bottom navigation / bottom app bar |
| `floatingActionButton` | Primary action needs a FAB |
| `snackbarHost` | Screen can show snackbars |
| `content` | Main content; **always apply the `PaddingValues` it hands you** |

> In Ampairs, prefer the shared `AppScreenWithHeader` wrapper for form/list screens (see the Form UI
> Standards in the root `CLAUDE.md`) — it standardizes the top bar and drawer navigation.

### Top App Bar

| Variant | Use case |
|---|---|
| `TopAppBar` (small) | Simple screens, minimal actions |
| `CenterAlignedTopAppBar` | Single primary action, centered title |
| `MediumTopAppBar` | Moderate navigation, collapsible on scroll |
| `LargeTopAppBar` | Hero screens, prominent collapsible title |

Do not add a redundant `navigationIcon` when the global nav drawer already handles back/up.

### Navigation

| Window size | Component |
|---|---|
| Compact (phones portrait) | `NavigationBar` (bottom) |
| Medium/Expanded (tablets, landscape) | `NavigationRail` (side) |
| Auto-switch | `NavigationSuiteScaffold` |

### Bottom Sheet / Snackbar / Dialog (MVI wiring)

- **Bottom sheet:** ViewModel emits a `UiEvent.ShowSheet`; the Route calls `sheetState.show()` in a
  `LaunchedEffect` collecting `events`.
- **Snackbar:** hold `SnackbarHostState` at the Route; collect `viewModel.events` and call
  `snackbarHostState.showSnackbar(...)` on a `ShowError`/`ShowMessage` event.
- **Dialog:** visibility is driven by `state.showDialog: Boolean`; confirm/dismiss dispatch intents
  via `viewModel.onIntent(...)`.

(Ampairs MVI uses `onIntent()` + `StateFlow<UiState>` + `SharedFlow<UiEvent>` — see SKILL §4.)

## Adaptive Layout Defaults

| Class | Width breakpoint | Typical devices |
|---|---|---|
| Compact | < 600dp | Phones portrait |
| Medium | 600dp – 840dp | Tablets portrait, large unfolded |
| Expanded | ≥ 840dp | Tablets landscape, desktop |

Compute `WindowSizeClass` once via `currentWindowAdaptiveInfo()` and pass layout decisions down as
state. Canonical layouts: `ListDetailPaneScaffold` (list-detail), `SupportingPaneScaffold`
(supporting pane), `LazyVerticalGrid` (feed). For deeper adaptive guidance see the official
`android-official/adaptive` skill.

## Anti-Patterns

| Anti-pattern | Fix |
|---|---|
| `Build.VERSION.SDK_INT` for dynamic color | Static M3 `ColorScheme` (`AmpairsTheme`) — already KMP |
| Hardcoding hex colors in components | `MaterialTheme.colorScheme.*` |
| Reading `ThemeManager` via `LocalAppGraph` in a composable | Injected VM / provided `CompositionLocal` |
| Multiple theme wrappers | Single `PlatformAmpairsTheme` at the root of `App()` |
| Ignoring `Scaffold` `innerPadding` | Always apply it to the content root |
