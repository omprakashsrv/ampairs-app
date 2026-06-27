# Agent on-device model catalogs (LLM vs Whisper) — learnings

The `feature/agent` module has **two completely independent model-catalog systems**. Confusing them
wastes time ("why isn't model X in the list?"). Know which one you're touching before editing.

---

## Rule 1: There are TWO catalogs — LLM (server-driven) vs Whisper STT (hardcoded)

| | LLM chat/intent models | Whisper STT (voice) models |
|---|---|---|
| Source of truth | **Backend manifest** `GET /api/agent/v1/models` | **Hardcoded** `WhisperModelCatalog` object (in app) |
| App entry point | `ModelCatalogProvider.all()` | `WhisperModelSet` → `WhisperModelRegistry` |
| Fallback | bundled `ModelCatalog.kt` (first launch only) | none — the object *is* the list |
| Download URL | backend proxy `v1/models/{id}/download` | direct HuggingFace URL in the catalog |
| Changeable without app release? | **Yes** (backend manifest) | **No** (ships in the binary) |

- `ModelCatalogProvider` governs **only** the LLM catalog. It is never consulted for Whisper.
- They share only low-level plumbing: `ModelDescriptor` and `ModelManager` (the resumable downloader).
- **"A model isn't showing in the picker"** → for LLMs, check the **backend manifest** first: once one
  pull succeeds, the server list *fully replaces* the bundled `ModelCatalog.kt`. The bundled list is
  cold-start-only. The picker does **not** filter by role — `ChatViewModel.rebuildModelUi` maps every
  catalog entry, INTENT included.

---

## Rule 2: The backend model catalog is hardcoded Kotlin, not a DB table

`ampairs/agent/src/main/.../domain/model/AiModelCatalog.kt` (`object AiModelCatalog.MODELS`).
The `agent` backend module owns **no DB tables / no Flyway**. "Seed the server model list" = **edit that
Kotlin list and ship a backend release** — there is no SQL seed.

The list mirrors the **Google AI Edge Gallery `model_allowlists/{appVersion}.json`** (e.g.
`1_0_9.json`). That allowlist has **no download-URL field** — the gallery builds it as
`https://huggingface.co/{modelId}/resolve/main/{modelFile}`. The 270M tool-caller there is
**`TinyGarden-270M`** (`google/functiongemma-270m-it` / `tiny_garden.litertlm`), *not* a literal
"function-gemma-270m" — don't search for the latter and conclude it's missing.

---

## Rule 3: Model `sizeBytes` must match the upstream file BYTE-FOR-BYTE

The app's downloader validates the downloaded size against the descriptor (and/or `Content-Length`).
A wrong `sizeBytes` **fails the download at runtime** (never at build). Copy `sizeInBytes` verbatim from
the allowlist JSON. A wrong `sourceUrl` only surfaces as a runtime 502 through the proxy — also never a
build failure. So neither is caught by CI; verify new entries on-device.

---

## Rule 4: The manifest carries a `role` (INTENT/CHAT/FALLBACK); the app honors it

Backend `AiModelDescriptor.role` + `AiModelResponse.role` (enum, serialized as `"INTENT"`/`"CHAT"`/
`"FALLBACK"`). App: `AiModelResponse.role: String` → `roleFromManifest()` in `toDescriptor()` (default
CHAT on unknown/blank). **Why it matters:** `ProviderRegistry.selectedChatModel()` auto-picks only
`role == CHAT` models. The tiny tool-caller (FunctionGemma-270M) must be `INTENT` or it gets auto-selected
as a (terrible) chat model. Before this session the app hardcoded every server model to CHAT.

The role is persisted in the offline cache (`AiModelEntity.role`) so it survives offline — otherwise a
re-launched-offline 270M would default to CHAT.

---

## Rule 5: `AgentCatalogDatabase` is a disposable cache → use destructive migration

It only mirrors the backend manifest (re-pulled on next launch), so on any schema change bump the
`@Database(version=...)` and add `.fallbackToDestructiveMigration(dropAllTables = true)` to all three
platform builders (`AgentCatalogDbModule.{android,ios,desktop}.kt`) — **don't** write a Room migration.
(Room 2.8.x signature requires the `dropAllTables` boolean.)

---

## Rule 6: The app can't be built in the sandbox; the backend can

App KMP build requires a **JetBrains-vendor JDK toolchain** that the egress policy blocks
(`api.foojay.io` → 403); only a system OpenJDK is present, which won't satisfy the vendor match. So:
- **App** (`ampairs-app`): rely on **CI** to validate. The coverage bot only posts *after* compile +
  tests pass, so a green coverage comment = the build/tests passed.
- **Backend** (`ampairs`): builds + tests locally with the system JDK 21 — run
  `./gradlew :agent:compileKotlin :agent:test` before pushing.

---

## Rule 7: The assistant's data-query (text-to-SQL) path — how it works + how to extend it

The assistant answers data questions ("how many customers", "total sales this month", "top products")
via a **SAFE_QUERY** path: the on-device LLM emits a read-only `SELECT` for ONE module, which is
validated and run on a Room **reader** connection. Two intent kinds reach the DB:
- **Actions** (`ResolvedIntent.Action`) — a fixed set of named CRUD/COUNT handlers per module.
- **Queries** (`ResolvedIntent.SafeQuery`) — free-form SELECT for arbitrary counts/sums/averages/
  filters/group-by. This is what gives "very large" coverage.

**Pipeline:** `LlmIntentResolver` (intent=query → `SafeQuery`) → `AgentOrchestrator` →
`SafeQueryService` (looks up the module's `ModuleQuerySchema`, validates with `SafeSqlValidator`
— SELECT-only, single-statement, allow-listed tables, enforced LIMIT — then runs the module's
`ModuleQueryExecutor.executeReadOnly` on a reader connection). Files: `feature/agent/.../query/*`,
`feature/agent/.../offline/LlmIntentResolver.kt`, `feature/agent/.../llm/{AgentSchemaBuilder,OutputSchema}.kt`.

**The trap that hid this for a long time:** the whole engine existed but was **dead code** — nothing
emitted `SafeQuery`. The LLM's `OutputSchema`/prompt only allowed `intent=action|conversation|clarify`
(no `query` intent, no `sql` field), so it could never request a query. Activating it required adding
the `query` intent + `sql` field to `OutputSchema` (JSON-schema **and** GBNF), rendering each module's
table/column schema into `AgentSchemaBuilder.systemPrompt`, parsing `intent=query` in
`LlmIntentResolver`, and injecting the `Map<String, ModuleQuerySchema>` into the resolver via
`AgentLlmModule`. Lesson: a query path is only as live as what the model is *told it can emit* — the
constrained `OutputSchema` + system prompt are the gate, not just the executor map.

**To make a NEW module queryable (mechanical, ~2 files, no central wiring):**
1. `feature/{m}/.../agent/{M}QueryExecutor.kt` — `@Inject @ContributesIntoMap(WorkspaceScope::class)
   @QueryExecutorKey("{m}")`, inject the module's Room DB, copy the `useReaderConnection { usePrepared }`
   body verbatim from `CustomerQueryExecutor`.
2. `feature/{m}/.../agent/{M}QuerySchemaModule.kt` — `@ContributesTo(WorkspaceScope::class)` +
   `@Provides @IntoMap @QuerySchemaKey("{m}")` returning a `ModuleQuerySchema` of curated tables/columns.
The injected schema map auto-feeds the prompt + moduleName enum — no edit to AgentSchemaBuilder/
SafeQueryService needed. Reference: customer/product/invoice/inventory (original 4) + order, payment,
unit, tax, business, subscription, ecom (added together).

**Schema-authoring rules (each is a real failure mode):**
- Use the **exact Room column name** (the `@ColumnInfo(name=...)` value, else the property name). The
  validator only checks *table* names, so a wrong/omitted column isn't caught until runtime → the
  executor throws "no such column" → `SafeQueryOutcome.Failed`. Source columns from the actual `@Entity`.
- **Omit** internal columns (`synced`, `active`/soft-delete, audit) and JSON blobs — keeps the prompt
  small and stops the model querying plumbing.
- Document **units in the column description** when storage ≠ display (e.g. payment `*_minor` is paise →
  divide by 100); the model reads these descriptions.
- Each query hits **one module's DB** — cross-module joins are impossible by construction. "Sales by
  customer name" (invoice⨝customer) can't be one query.

**Hard dependency:** SQL is generated **only** by the LLM resolver — the rule-based fallback can't.
So the data-query path needs a chat model loaded (RAM ≥ 3 GB per `RamTiers`). With no model, only the
hardcoded named actions work. Accuracy also tracks model size (tiny models write iffy SQL); the
SELECT-only + allow-list + LIMIT + reader-connection guardrails keep it *safe* regardless.
