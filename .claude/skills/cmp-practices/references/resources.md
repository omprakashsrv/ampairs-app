# Compose Multiplatform Resources

> **Project rule:** all user-visible strings via `Res.string.*`. Never `R.string` or hardcoded strings. Strings in `feature/{name}/src/commonMain/composeResources/values/strings.xml`.

## Android R vs CMP Res

| Concern | Android | Compose Multiplatform |
|---|---|---|
| Generated class | `R` (integer IDs) | `Res` (typed accessors) |
| String | `stringResource(R.string.name)` | `stringResource(Res.string.name)` |
| Drawable | `painterResource(R.drawable.icon)` | `painterResource(Res.drawable.icon)` |
| Plural | `pluralStringResource(R.plurals.x, count)` | `pluralStringResource(Res.plurals.x, count)` |
| Font | `Font(R.font.inter)` | `Font(Res.font.inter)` ← composable in CMP |
| Resource directory | `res/` | `composeResources/` |
| Import | `import com.example.app.R` | `import ampairsapp.{module.path}.generated.resources.*` |
| Suspend access | N/A | `getString(Res.string.name)` — suspend |

> **Project import pattern:** `import ampairsapp.feature.{name}.generated.resources.*`  
> **KMP android library:** no `R` class is generated — always use `Res`.

## Directory Structure

```text
feature/{name}/src/commonMain/composeResources/
├── drawable/              PNG, JPG, WebP, SVG, Android XML vector
│   └── drawable-dark/     dark theme variants
├── font/                  TTF, OTF
├── values/                strings.xml — base locale
│   ├── values-hi/         Hindi
│   ├── values-ta/         Tamil
│   └── values-en-rIN/     English (India)
└── files/                 raw files
```

## Strings API

```xml
<!-- composeResources/values/strings.xml -->
<resources>
    <string name="customer_list_title">Customers</string>
    <string name="customer_create_success">%1$s created successfully</string>
    <plurals name="customer_count">
        <item quantity="one">%1$d customer</item>
        <item quantity="other">%1$d customers</item>
    </plurals>
</resources>
```

```kotlin
// ✅ Composable context
Text(text = stringResource(Res.string.customer_list_title))
Text(text = stringResource(Res.string.customer_create_success, customer.name))

// ✅ Plural
Text(text = pluralStringResource(Res.plurals.customer_count, count, count))

// ✅ Non-composable suspend context (e.g. androidMain service / notification)
val msg = getString(Res.string.customer_create_success, name)   // call before suspendCancellableCoroutine

// ❌ Hardcoded string
Text("Customers")

// ❌ Android R class — no R generated for KMP android library
context.getString(R.string.customer_list_title)
```

## MVI Integration — Semantic Keys in State

ViewModels never resolve strings. Use semantic enum keys; resolve to strings in UI.

```kotlin
// In ViewModel / state
enum class CustomerErrorKey { NetworkError, Duplicate, Unauthorized }
data class CustomerState(val error: CustomerErrorKey? = null)

// In composable
state.error?.let { key ->
    Text(
        text = stringResource(when (key) {
            CustomerErrorKey.NetworkError -> Res.string.error_network
            CustomerErrorKey.Duplicate -> Res.string.error_customer_duplicate
            CustomerErrorKey.Unauthorized -> Res.string.error_unauthorized
        }),
        color = MaterialTheme.colorScheme.error,
    )
}
```

## Fonts — Composable in CMP

Unlike Android, `Font()` is a composable in CMP. `Typography` must be built inside a composable.

```kotlin
@Composable
fun AppTypography(): Typography {
    val fontFamily = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_bold, FontWeight.Bold),
    )
    return MaterialTheme.typography.copy(
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
    )
}
```

## Drawables and Icons

```kotlin
Image(
    painter = painterResource(Res.drawable.ic_customer),
    contentDescription = null,
)

// Material Symbols XML icons — download Android XML variant from fonts.google.com/icons
// Set android:fillColor="#000000", remove android:tint, place in composeResources/drawable/
Image(
    painter = painterResource(Res.drawable.ic_add_customer),
    contentDescription = stringResource(Res.string.cd_add_customer),
    modifier = Modifier.size(24.dp),
    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
)
```

## Raw Files

```kotlin
val jsonBytes = Res.readBytes("files/default_config.json")
val uri: String = Res.getUri("files/intro.mp4")   // for WebView or media players
```

## Gradle Setup

```kotlin
compose.resources {
    publicResClass = true   // needed for library modules shared across features
    packageOfResClass = "com.ampairs.{feature}.generated.resources"
    generateResClass = auto
}
```

> **CMP 1.11+:** Resources are packed into Android assets, enabling `@Preview` to resolve `Res.*` and `WebView`/media players to access bundled files via URI. No extra configuration needed.

Rebuild after adding new resources — the `Res` class needs regeneration.

## Rules

- Use `composeResources/` for all strings, images, fonts, raw files
- Use typed accessors (`Res.string.name`) for compile-time safety
- Keep resource resolution in composables — call `stringResource()` at render time
- Never resolve strings inside reducers or ViewModels
- Use suspend variants (`getString()`) only in non-composable contexts
- `publicResClass = true` when resources are shared from a library module
- Rebuild after adding new resources
