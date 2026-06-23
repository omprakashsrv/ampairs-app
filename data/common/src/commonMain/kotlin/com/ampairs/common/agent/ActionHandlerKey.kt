package com.ampairs.common.agent

import dev.zacsweers.metro.MapKey

/**
 * Metro map key for contributing [ActionHandler]s into the agent's action map, keyed by module name
 * (e.g. "customer", "invoice"). Mirrors the SyncEntityKey pattern used for sync delegates.
 *
 * Usage on a handler:
 * ```
 * @Inject
 * @ContributesIntoMap(WorkspaceScope::class)
 * @ActionHandlerKey("invoice")
 * class InvoiceActionHandler(...) : ActionHandler
 * ```
 * The resulting `Map<String, ActionHandler>` is consumed by `ActionRegistry`.
 */
@MapKey
annotation class ActionHandlerKey(val value: String)
