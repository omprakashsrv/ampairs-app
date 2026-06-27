package com.ampairs.agent.llm

/**
 * Pure, engine-agnostic backend picker used by [ProviderRegistry] (T023). Kept free of DI/native code
 * so the selection policy is unit-testable in isolation (`BackendSelectorTest`).
 *
 * Policy: walk [orderedEngineIds] (config override → `PlatformDefaults.primaryEngineId` →
 * `PlatformDefaults.fallbackEngineId`) and return the first registered backend that both matches the
 * id **and** `supports(model)`. If none of the preferred ids fit, fall back to the model's own
 * declared [ModelDescriptor.backendId], then to any backend that supports the model. `null` means no
 * available backend can run the model (→ rule-based-only mode).
 */
object BackendSelector {

    fun select(
        backends: Collection<LlmBackend>,
        model: ModelDescriptor,
        orderedEngineIds: List<String>,
    ): LlmBackend? {
        if (backends.isEmpty()) return null
        val byId = backends.associateBy { it.id }

        for (id in orderedEngineIds) {
            val backend = byId[id] ?: continue
            if (backend.supports(model)) return backend
        }
        byId[model.backendId]?.takeIf { it.supports(model) }?.let { return it }
        return backends.firstOrNull { it.supports(model) }
    }
}
