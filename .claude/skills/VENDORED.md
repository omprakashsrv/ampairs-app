# Vendored agent skills

Copied verbatim from upstream (Apache-2.0; licences alongside). **Do not edit these skill
folders** — project-specific guidance belongs in `cmp-practices`, `metro-di`, `offline-sync`.
To refresh: re-copy from the repos below and update the commits.

| Source | Commit | Date | Licence |
|---|---|---|---|
| https://github.com/android/skills | `c72942584fff` | 2026-11-15 | `LICENSE-android-skills.txt` |
| https://github.com/Kotlin/kotlin-agent-skills | `087f5689c193` | 2026-10-30 | `LICENSE-kotlin-agent-skills.txt` |

| Skill | From | Upstream path |
|---|---|---|
| `adaptive` | android | `jetpack-compose/adaptive` |
| `agp-9-upgrade` | android | `build-system/agp/agp-9-upgrade` |
| `android-cli` | android | `devtools/android-cli` |
| `android-for-cars-parked-apps` | android | `cars/android-for-cars-parked-apps` |
| `android-intent-security` | android | `security/android-intent-security` |
| `android-permissions-security` | android | `security/android-permissions-security` |
| `android-profiler` | android | `profilers/android-profiler` |
| `anr-audit-and-fix` | android | `performance/anr-audit-and-fix` |
| `appfunctions` | android | `device-ai/appfunctions` |
| `camerax` | android | `camera/camerax` |
| `display-glasses-with-jetpack-compose-glimmer` | android | `xr/display-glasses-with-jetpack-compose-glimmer` |
| `edge-to-edge` | android | `system/edge-to-edge` |
| `engage-sdk-integration` | android | `play/engage-sdk-integration` |
| `glance-appwidget-updates-and-memory` | android | `system/glance-appwidget-updates-and-memory` |
| `glance-canonical-layouts` | android | `system/glance-canonical-layouts` |
| `ink-app-builder` | android | `jetpack-compose/ink/ink-app-builder` |
| `ink-custom-brush-builder` | android | `jetpack-compose/ink/ink-custom-brush-builder` |
| `ios-to-android` | android | `migration/ios-to-android` |
| `jetpack-compose-performance` | android | `jetpack-compose/jetpack-compose-performance` |
| `kotlin-backend-jpa-entity-mapping` | kotlin | `skills/kotlin-backend-jpa-entity-mapping` |
| `kotlin-tooling-agp9-migration` | kotlin | `skills/kotlin-tooling-agp9-migration` |
| `kotlin-tooling-cocoapods-spm-migration` | kotlin | `skills/kotlin-tooling-cocoapods-spm-migration` |
| `kotlin-tooling-gradle-to-kotlin-toolchain-plugin` | kotlin | `skills/kotlin-tooling-gradle-to-kotlin-toolchain-plugin` |
| `kotlin-tooling-gradle-to-kotlin-toolchain-project` | kotlin | `skills/kotlin-tooling-gradle-to-kotlin-toolchain-project` |
| `kotlin-tooling-immutable-collections-0-5-x-migration` | kotlin | `skills/kotlin-tooling-immutable-collections-0-5-x-migration` |
| `kotlin-tooling-java-to-kotlin` | kotlin | `skills/kotlin-tooling-java-to-kotlin` |
| `kotlin-tooling-kotlin-toolchain` | kotlin | `skills/kotlin-tooling-kotlin-toolchain` |
| `kotlin-tooling-kotlin-toolchain-plugin-authoring` | kotlin | `skills/kotlin-tooling-kotlin-toolchain-plugin-authoring` |
| `kotlin-tooling-native-build-performance` | kotlin | `skills/kotlin-tooling-native-build-performance` |
| `leanback-to-compose-tv-migration` | android | `tv/leanback-to-compose-tv-migration` |
| `litert-lm` | android | `device-ai/litert/litert-lm` |
| `litert-runtime` | android | `device-ai/litert/litert-runtime` |
| `media3-cast-integration` | android | `media/media3-cast-integration` |
| `migrate-xml-views-to-jetpack-compose` | android | `jetpack-compose/migration/migrate-xml-views-to-jetpack-compose` |
| `ml-kit-genai-prompt-api` | android | `device-ai/ml-kit-genai-prompt-api` |
| `mlkit-agent-api` | android | `device-ai/mlkit-agent-api` |
| `navigation-3` | android | `navigation/navigation-3` |
| `navigation-event` | android | `navigation/navigation-event` |
| `play-billing-library-version-upgrade` | android | `play/play-billing-library-version-upgrade` |
| `play-policy-insights` | android | `play/play-policy-insights` |
| `projected-hardware` | android | `xr/projected-hardware` |
| `r8-analyzer` | android | `performance/r8-analyzer` |
| `restore-credentials` | android | `identity/restore-credentials` |
| `testing-setup` | android | `testing/testing-setup` |
| `verified-email` | android | `identity/verified-email` |
| `wear-compose-m3` | android | `wear/wear-compose-m3` |
| `wear-widgets` | android | `wear/wear-widgets` |

Scripts reviewed on refresh (all read-only analysers/report writers): `r8-analyzer/scripts/*.py`,
`play-policy-insights/scripts/*.py` (`play_store_scraper.py` fetches the app's public Play Store
page; TLS verified by default), `kotlin-tooling-agp9-migration/scripts/analyze-project.sh`,
`kotlin-tooling-native-build-performance/scripts/audit-native-build.sh`.

**Downloads a binary when run:** `android-profiler/references/perfetto/bin/trace_processor` is
Perfetto's official launcher — it fetches a pinned `trace_processor_shell` (v58.3) from Google's
`perfetto-luci-artifacts` bucket, verifies its SHA-256 (raises on mismatch), then executes it.
