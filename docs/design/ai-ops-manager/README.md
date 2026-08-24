# AI Business Operations Manager — app-side design docs

The AI Ops Manager's **near-term work runs app-side** (one workspace per instance, full master resident
in Room, existing `LlmEngine`, offline-sync write path). This directory holds the **Epic-1 slice design
docs** for the app; the program's source of truth lives in the backend repo.

- **Program roadmap (source of truth):** `ampairs/docs/ai-ops-manager/README.md`
- **Engine design:** `ampairs/docs/ai-ops-manager/framework.md`
- **Decisions:** `ampairs/docs/adr/0005-*` (app-side orchestration), `0006-*` (shared KMP engine)

## Why app-side (recap)
Each app instance is scoped to one workspace and already holds everything the agent needs: the workspace
master in Room (offline-first), an on-device `LlmEngine` + cloud tier
(`feature/agent/.../llm/LlmEngine.kt`) for hard reasoning, and an audited write→sync path
(`data/sync`, `/offline-sync`). So detection, reasoning, correction, and audit all happen on device —
no backend module needed. See ADR 0005.

## The engine, once (ADR 0006)
A host-agnostic Kotlin SPI in a shared KMP module `feature/aiops` (`commonMain`, **no `java.*`/Koog**):
`Detector → ContextGatherer → CandidateGenerator → CandidateValidator → ConfidenceScorer → RiskPolicy →
(Executor | ReviewQueue) → AuditWriter → FeedbackStore`, with two app-provided ports:
`Reasoner` = `LlmEngine`, `Executor` = repository write + offline-sync.

## Epic-1 slices (each gets a design doc here → review → implement)

| # | Slice | Doc | Status |
|---|---|---|---|
| 1 | Shared engine SPI + **unit standardization** (the proof) | [01-engine-and-unit-standardization.md](01-engine-and-unit-standardization.md) | draft |
| 2 | Capitalization/format + customer field validation | _tbd_ | — |
| 3 | Contact card → capture + extract + normalize | _tbd_ | — |
| 4 | Product & customer **dedup** (resident master; gated on "fully synced") | _tbd_ | — |
| 5 | HSN/GST **advisory-suggest** (never auto) | _tbd_ | — |

## Method (per slice)
**DoR:** design doc here, reviewed & approved. **Build:** implement in `feature/aiops` following
`/cmp-practices`, `/metro-di`, `/offline-sync`. **DoD:** compiles all 3 targets, the capability runs the
full pipeline, audit + undo work, a test exists, and the roadmap backlog row is updated.

## Non-negotiables
LLM is one **capped** signal (never sole authority); tax/HSN **advisory-only**. Auto-fix only when
**confidence high AND risk low AND reversible**, per the workspace autonomy level (default **L1
Recommend** — propose, never change). Every mutation **audited + reversible**, applied through the
entity's normal repository + offline-sync path — never a foreign module's DAO.
