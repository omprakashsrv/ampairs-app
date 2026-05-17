# Repository Guidelines
## Architecture Overview
- Offline-first MVI flow: Compose presentation → Store5 cache/invalidation → data sources (Room, Ktor) with JWT-authenticated REST and WebSocket sync.
- Koin scopes wire domain components; inject Store5 instances per feature and surface only `StateFlow` contracts to the UI.
- Multi-tenant context is threaded via `tenantId`/`workspace` headers and stored securely; reuse the existing DataStore + encrypted Room setup.
- Android uses WorkManager, iOS uses background refresh, and Desktop uses timers for sync; keep business logic inside shared modules to retain parity.
## Project Structure & Module Organization
- `composeApp/` holds shared UI (`commonMain`) plus platform shims (`androidMain`, `iosMain`, `desktopMain`); keep new composables under `com/ampairs/...`.
- `shared/{common,prod,qa}` expose environment-specific bindings; prefer swapping Koin modules over toggling flags.
- `core/*` supplies analytics, logging, permissions, notifications, and dispatchers; `common/*` hosts design primitives and image loading.
- `desktop-app/`, `tallyModule/`, `tasks/`, and `thirdparty/` house desktop packaging, tally sync, scheduled jobs, and vendored sources; shared assets live in `resources/`.
## Build, Test, and Development Commands
- Use Java 25 (`sdk use java 25`) before Gradle tasks.
- `./gradlew :composeApp:run` for desktop previews, `:composeApp:assembleDebug` for Android, and open `iosApp/iosApp.xcodeproj` for iOS targets.
- `./gradlew :desktop-app:package` creates native installers; `./gradlew :composeApp:check` runs unit tests and static checks.
- `./cleanup.sh` clears build artifacts when switching branches.
## Coding Style & Naming Conventions
- Kotlin 2.2.20 + Compose: 4-space indentation, trailing commas on multi-line args, IDE-managed imports, and expect/actual for platform forks.
- Name composables with PascalCase (`DeviceListScreen`); view models end with `ViewModel`; Store5 wrappers adopt `*Store`.
- Constructor parameters usually follow `dispatchers → repositories → use cases → presenters`; wire dependencies through Koin modules.
- Add dependencies via `gradle/libs.versions.toml` before referencing them in build scripts.
## Testing Guidelines
- Place shared tests in `composeApp/src/commonTest/kotlin`, mirroring source packages; add platform suites only when behavior diverges.
- Use `kotlin.test` + `kotlinx.coroutines.test` with `AppCoroutineDispatchers` fakes; stub Ktor via `MockEngine`.
- Name tests `FeatureBehaviorTest` and follow `given_when_then` method naming.
- Run `./gradlew :composeApp:check` and attach failing seeds or repro steps in PRs.
## Commit & Pull Request Guidelines
- Follow Conventional Commits with module scopes when helpful (`feat(shared.common): add customer sync store`).
- Keep commits focused and update specs (`ENVIRONMENT_CONFIG.md`, feature briefs) alongside code changes.
- PRs should outline motivation, affected modules, manual test matrix, and screenshots for UI updates.
- Flag environment or Gradle impacts (new plugins, API hosts) so reviewers can adjust `local.properties`.
## Environment & Configuration Tips
- API hosts and environment flags live in `composeApp/build.gradle.kts` `buildConfigField`s; document any edits.
- Secrets stay in `local.properties`; consult `ENVIRONMENT_CONFIG.md` and reuse DataStore helpers for secure storage.
- Regenerate signing and resource artifacts via scripts under `tasks/`; never commit keystore changes.
