# Quickstart — Validate Scalable Customer Search (FTS)

## Prereqts
- Dev branch `claude/trusting-fermi-7fvvpa`.
- Customer DB at v11 with `CustomerFts` + `Migration10To11` in place.

## 1. Generate & commit the Room schema (v11)
Build with `exportSchema = true` so Room emits the v11 schema, then copy the generated `customer_fts`
table + trigger DDL into `Migration10To11` and commit `11.json`.
```bash
./gradlew :feature:customer:compileDebugKotlinAndroid   # regenerates schemas/.../11.json
```
> If the migration DDL doesn't match `11.json` exactly, Room throws an `IllegalStateException`
> ("Migration didn't properly handle / expected schema") at open time. Copy DDL verbatim.

## 2. Compile gates (all targets)
```bash
./gradlew androidApp:compileDebugKotlinAndroid
./gradlew shared:compileKotlinIosSimulatorArm64
./gradlew desktopApp:compileKotlin
```

## 3. Runtime FTS smoke (must verify on each platform)
Bundled SQLite ships FTS, but verify the migration opens and MATCH works:
- **Android**: `./gradlew androidApp:installDebug`, open Customers, type a partial name → results.
- **Desktop**: `./gradlew desktopApp:run`, same check.
- **iOS**: run the simulator target, same check.
Confirm in each: the v10→v11 migration runs once (no destructive reset), existing customers remain.

## 4. Functional checks
- **Name prefix**: typing `raj` returns "Rajesh …"; typing `esh` returns nothing (prefix, not substring — expected).
- **Phone**: `9876543210` and `98765 43210` both find the same customer.
- **GSTIN**: `29ABCDE` returns the customer whose GSTIN starts with it.
- **Email / address**: partial email and city/state prefixes match.
- **Filters**: with a search term active, toggling a state/type/group filter narrows results; the
  same filters apply on the empty-term browse list.
- **Active only**: a soft-deleted/synced-away customer disappears from results with no manual index code.

## 5. Scale check (the point of the feature)
On a workspace with ~100k customers (or a seeded test DB):
- As-you-type latency stays < ~100 ms (no full-scan stall).
- Browse scrolls smoothly via Paging3 (memory stays flat — not all rows loaded).
- Search shows the first 100 with a "refine search" hint.
- Note the **first full sync** indexes row-by-row (tens of seconds, background) — expected.

## 6. Unit test (commonTest)
Cover `buildFtsQuery`:
- `"raj mum"` → `"raj"* "mum"*`
- `"98765 43210"` → `"9876543210"*`
- input with a `"` is escaped (`a"b` → `"a""b"*`)
- blank input → router takes the browse branch (no MATCH issued).
