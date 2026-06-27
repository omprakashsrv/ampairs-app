package com.ampairs.form.agent

import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.Inject
import com.ampairs.form.data.db.FormDatabase

/**
 * Agent-facing DAO for form queries. Provides quick access to form metadata
 * without going through the full repository layer.
 *
 * Currently minimal as form operations are mostly driven by the client.
 * Extend as needed for more agent-driven form discovery/analysis.
 */
@Inject
class FormAgentDao(
    private val db: FormDatabase,
) {
    // Placeholder for future queries; form operations are mostly driven by voice intent
    // and don't require bulk form discovery at this time.
}
