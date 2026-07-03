# Client (white-label) builds

The customer storefront app is **one** Gradle module, `:clientApp`, built once per client by
selecting a client at build time. Nothing here duplicates code — `:clientApp` and `:shared-ecom`
are 100% shared across every client. This scales to N clients with **no new modules and no product
flavors** (which would explode into 2×N build variants).

## Build a client

```bash
./gradlew :clientApp:assembleDebug                 # defaults to client 'ambika'
./gradlew :clientApp:bundleRelease -Pclient=ambika # release AAB for a specific client
```

The IDE configures a single variant, so sync/build stays fast no matter how many clients exist.

## What a client provides

Everything client-specific lives in `clients/<id>/`:

```
clients/<id>/
  config.properties     # applicationId, appName, workspaceSlug, versionCode/Name, themeColorArgb
  res/mipmap-*/         # launcher icons (ic_launcher, _foreground, _background, _monochrome, _round)
```

`config.properties` keys (all required unless noted):

| key            | used for                                                        |
|----------------|-----------------------------------------------------------------|
| `applicationId`| the unique Play Store package name (one listing per id)         |
| `appName`      | home-screen label (manifest `android:label` placeholder)        |
| `workspaceSlug`| backend storefront slug → `BuildConfig.WORKSPACE_SLUG`          |
| `versionCode`  | independent version per client                                  |
| `versionName`  | independent version per client                                  |
| `themeColorArgb`| brand seed color (forward-looking; wiring into the theme is a small `:shared-ecom` follow-up) |

## Onboard a new client

1. `cp -r clients/ambika clients/<id>` and edit `config.properties` (new `applicationId`, name, slug).
2. Drop the client's launcher icons into `clients/<id>/res/mipmap-*`.
3. Register the new `applicationId` in the Firebase console (needed for Phone-Auth SHA-1/256), then
   re-download `google-services.json` into `clientApp/` — one file holds a `client` block per
   package name (same Firebase project), so it just gains another entry.
4. `./gradlew :clientApp:bundleRelease -Pclient=<id>` and upload the AAB to that client's Play listing.

No Kotlin/Gradle module changes are needed to add a client.

## CI

Loop over `clients/*/` and build + publish each:

```bash
for dir in clients/*/; do
  id=$(basename "$dir")
  ./gradlew :clientApp:bundleRelease -Pclient="$id"
  # then upload clientApp/build/outputs/bundle/release/*.aab to the $id Play listing
done
```
