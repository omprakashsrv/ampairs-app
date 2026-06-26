# Form Agentic Integration Guide

How to let users fill any entry form by **describing the record in plain words** (typed now, voice
later). The on-device assistant predicts values for the form's fields from the conversation and the
form's own schema — multiple fields in one turn.

## Overview

The user opens the assistant bubble on an edit screen and says/types something like:

- "Acme Traders in Mumbai, phone 9876543210, type wholesale"
- "name Rohit Sharma, email rohit@acme.com"

The model returns a map of `fieldKey -> value` for **only** the fields the user actually described,
grounded in that form's schema (field keys, types, choice options). Each predicted value is validated
against the field's type before it is applied.

The pieces:

1. **FormAgentViewModel** (`ui/FormAgentViewModel.kt`) — entity-scoped (`@AssistedInject`, keyed by
   `entityType`). Loads the form schema, builds a grounding prompt, calls the agent module's
   constrained-generation engine, validates each predicted value, and emits `FormFieldFill` events.
2. **FormAgentChatBubble** (`ui/FormAgentChatBubble.kt`) — the FAB + chat dialog. Self-contained: it
   creates the ViewModel, shows the conversation, and forwards each predicted fill to `onFieldFill`.
3. **FormValueState.applyAgentFill** (`render/FormValueState.kt`) — generic sink that drops a predicted
   value into the right slot (single vs MULTI_CHOICE) with validation. This is the recommended wiring
   target for any schema-driven screen.

## Architecture

```
User types/says a description
    ↓
FormAgentChatBubble → FormAgentViewModel.submit(text)
    ↓
ConfigRepository.observeSchema(entityType)  → the open form's fields
    ↓
LlmEngine.generateConstrained(prompt, OutputSchema)   (agent module, on-device)
    ↓  {"params": {"name":"Acme","city":"Mumbai", ...}, "reply": "..."}
validate each value vs field type/options (choice → exact option, boolean → true/false, number → numeric)
    ↓
emit FormFieldFill(fieldKey, value)  per field
    ↓
onFieldFill → FormValueState.applyAgentFill(fieldKey, value)
    ↓
form fields re-render with predicted values
```

Why a dedicated, entity-keyed ViewModel (not the global `ChatViewModel`): the prediction is inherently
screen-scoped — it needs the schema of the form that is open to ground the model, and the values must
flow back into THIS screen's state. The global chat has no per-screen context and a static prompt.

## Integration (one line on a schema-driven screen)

Any screen that already renders through `DynamicFormRenderer` + `FormValueState` just routes the
callback into the state:

```kotlin
val state = rememberFormValueState(schema, initialValues)

Box(modifier = Modifier.fillMaxSize()) {
    Column {
        TopAppBar(title = { Text(...) })
        DynamicFormRenderer(schema, state, optionRegistry, widgetRegistry)
    }

    FormAgentChatBubble(
        entityType = schema.entityType,
        onFieldFill = { fieldKey, value -> state.applyAgentFill(fieldKey, value) },
        modifier = Modifier.align(Alignment.TopEnd),
    )
}
```

`applyAgentFill` ignores unknown keys, so a stray prediction can never corrupt the form.

### Screens with bespoke (non-schema) state

If a screen holds its own form-state data class instead of `FormValueState`, implement a small
`updateField(fieldKey, value)` that maps each `fieldKey` to a `.copy(...)`:

```kotlin
fun updateField(fieldKey: String, value: String) {
    _formState.update {
        when (fieldKey) {
            "name" -> it.copy(name = value)
            "email" -> it.copy(email = value)
            "city" -> it.copy(city = value)
            else -> it  // custom fields → it.copy(attributes = it.attributes + (fieldKey to value))
        }
    }
}
```

### Entity type naming

Use the backend entity type consistently (it is the form-schema key): `customer`, `customer_group`,
`customer_type`, `product`, `order`, `invoice`, `business`.

## Field types & validation

The assistant predicts and normalizes by field `dataType` before emitting:

- **TEXT / TEXTAREA / DATE** — passed through as text.
- **NUMBER** — kept only if the value parses as a number.
- **BOOLEAN** — normalized to `"true"` / `"false"` (accepts yes/no, 1/0, on/off).
- **CHOICE / MULTI_CHOICE** — matched (case-insensitive) to one of the field's `enumValues`; an
  unmatched value is dropped. Dynamic-option choices are passed through for the host to validate.

Required/length/format validation still runs in `FormValueState`/`ValidationEngine` on apply, so the
inline errors a manual edit would show also apply to assistant fills.

## Requirements / limitations

- Needs the on-device LLM (agent module). When no model is loaded (`engineOrNull()` is null — e.g.
  low-RAM device, or iOS/Desktop without an engine backend), the bubble explains that the assistant
  isn't ready instead of failing. The user can download a model from the AI assistant screen.
- Prediction quality tracks model size; the schema grounding + per-type validation keep results safe
  regardless (only known field keys, only valid options/types are ever applied).
- Voice is the primary input: opening the dialog starts the mic (`FormAgentViewModel.startVoice()`),
  streams the live transcript, and on the final utterance feeds the same multi-field prediction — speak
  "name … phone … PAN … address …" and every matching field fills in one shot. STT comes from the agent
  module's `SpeechAdapterRegistry` (device recognizer when online, Whisper offline); on platforms with
  no recognizer (Desktop) the dialog falls back to the text box. Mic access is gated by
  `MicPermissionController`.

## The global `FormActionHandler`

`agent/FormActionHandler.kt` remains registered with the **global** assistant (`@ActionHandlerKey("form")`)
for single-field reads/fills via the main chat (READ schema / UPDATE one field / SEARCH mandatory
fields). The screen-scoped multi-field prediction above is the path for "fill the open form from
conversation"; the two coexist.

## Code references

- **ViewModel**: `feature/form/src/commonMain/kotlin/com/ampairs/form/ui/FormAgentViewModel.kt`
- **UI component**: `feature/form/src/commonMain/kotlin/com/ampairs/form/ui/FormAgentChatBubble.kt`
- **Generic sink**: `feature/form/src/commonMain/kotlin/com/ampairs/form/render/FormValueState.kt` (`applyAgentFill`)
- **Global handler**: `feature/form/src/commonMain/kotlin/com/ampairs/form/agent/FormActionHandler.kt`
- **Engine API**: `feature/agent/src/commonMain/kotlin/com/ampairs/agent/llm/{LlmEngine,ProviderRegistry,OutputSchema}.kt`
- **Form schema**: `feature/form-api/src/commonMain/kotlin/com/ampairs/form/domain/FormSchema.kt`
