# Image Loading (Coil 3)

This project uses Coil 3.5.0 with the Ktor 3 network loader (`coil-network-ktor3`), so it is fully
multiplatform. The shared `ImageLoader` is exposed on `AppGraph.imageLoader` and provided as a
`CompositionLocal` in `App.kt` — screens receive it via Metro-injected ViewModels or `AsyncImage`,
never via `LocalAppGraph`.

References:
- [Coil 3 docs](https://coil-kt.github.io/coil/)
- [Coil Compose](https://coil-kt.github.io/coil/compose/)

## Project Setup (already configured — for reference)

```toml
# gradle/libs.versions.toml
coil = "3.5.0"
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
```

`coil-network-ktor3` reuses the project's existing Ktor client engines — no extra setup needed.

## Choose the Right API

| Use case | Best API | Why |
|---|---|---|
| Most image rendering | `AsyncImage` | Best default; resolves image size from constraints |
| Need a `Painter` or manual control | `rememberAsyncImagePainter` | More control, lower-level |
| Need composable slots per loading state | `SubcomposeAsyncImage` | Slot API, but slower — avoid in lists |

**Performance:** `SubcomposeAsyncImage` is unsuitable for dense `LazyColumn`/`LazyGrid` cells.
Prefer `AsyncImage` in lists.

## Default AsyncImage Pattern

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalPlatformContext.current)
        .data(imageUrl)
        .crossfade(true)
        .size(80.dp.toPx().toInt())   // avoid decoding a 4K image into an 80dp slot
        .build(),
    placeholder = painterResource(Res.drawable.ic_placeholder),
    error = painterResource(Res.drawable.ic_image_error),
    contentDescription = customerName,   // null only for purely decorative images
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
)
```

Use `LocalPlatformContext.current` in `commonMain` — NOT the Android-only `LocalContext.current`.

## ImageLoader Configuration

Create one shared `ImageLoader` per app process. In Ampairs it is built once behind
`AppGraph.imageLoader`; if configuring the singleton factory, do it in the platform entry points:

```kotlin
// androidMain: MainActivity
setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
        .crossfade(true)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .build()
}
```

Multiple loaders fragment the memory/disk caches and reduce hit rates.

## CMP Resources with Coil

```kotlin
// Coil model — use a string URI, not the Res.drawable handle
AsyncImage(model = Res.getUri("drawable/sample.jpg"), contentDescription = null)

// Direct resource render without Coil
Image(painterResource(Res.drawable.ic_document), contentDescription = null)
```

`Res.drawable.xxx` handles are NOT valid Coil models — use `Res.getUri("drawable/xxx.png")`.

## Stable Keys for Smooth List → Detail Transitions

```kotlin
ImageRequest.Builder(LocalPlatformContext.current)
    .data(url)
    .memoryCacheKey("customer-image-$uid")
    .placeholderMemoryCacheKey("customer-image-$uid")
    .build()
```

`placeholderMemoryCacheKey` reuses the in-memory result as the placeholder, avoiding a flash.

## Extended Pipeline

```kotlin
ImageLoader.Builder(context)
    .components {
        add(SvgDecoder.Factory())     // coil3:coil-svg dependency
        add(CustomerImageMapper())    // domain object → URL string
        add(AuthInterceptor(tokenProvider))
    }
    .build()
```

| Need | Customize |
|---|---|
| Retry / short-circuit / global policy | `Interceptor` |
| Accept a custom model type in `.data(...)` | `Mapper` |
| Custom source/protocol | `Fetcher.Factory<T>` |
| Decode a custom format (SVG, etc.) | `Decoder.Factory` |
| Auth headers on all image requests | Interceptor on the shared client |

## Preview / Testing

```kotlin
@Preview @Composable
fun CustomerCardPreview() {
    CompositionLocalProvider(
        LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler {
            painterResource(Res.drawable.ic_placeholder)
        }
    ) { CustomerCard(/* ... */) }
}
```

Inject a fake `ImageLoader` for tests instead of relying on the global singleton.

## Anti-Patterns

| Anti-pattern | Fix |
|---|---|
| `SubcomposeAsyncImage` in dense lists | `AsyncImage` |
| Multiple `ImageLoader` instances | Single factory / `AppGraph.imageLoader` |
| `LocalContext.current` in commonMain | `LocalPlatformContext.current` |
| `Res.drawable.xxx` passed to Coil | `Res.getUri("drawable/xxx.png")` |
| No `.size(...)` hint for thumbnails | Constrain decode size to the slot |
| `LocalAppGraph.current.imageLoader` in a composable | Provided `CompositionLocal` / injected VM |
