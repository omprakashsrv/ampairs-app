package com.ampairs.agent.core

/**
 * Lightweight provider that holds metadata + a factory lambda.
 * No heavy objects (repositories, databases) in memory until first use.
 *
 * DI-agnostic: works with Koin, Dagger, kotlin-inject, or manual wiring.
 *
 * Cost per provider: ~200 bytes (strings + list of descriptors).
 * Handler is created lazily on first dispatch to this module.
 */
class ActionHandlerProvider(
    val moduleName: String,
    val supportedActions: List<ActionDescriptor>,
    private val factory: () -> ActionHandler,
) {
    /** Handler is NOT created until dispatch needs it */
    val handler: ActionHandler by lazy { factory() }
}
