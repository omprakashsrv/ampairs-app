package com.ampairs.printing.di

import com.ampairs.printing.core.model.DocumentType
import dev.zacsweers.metro.MapKey

/**
 * Metro map key for the [com.ampairs.printing.core.provider.PrintValueProvider] registry. Each
 * feature contributes its provider with `@ContributesIntoMap(WorkspaceScope::class)` +
 * `@DocumentTypeKey(DocumentType.X)`, letting [com.ampairs.printing.service.PrintCoordinator] rebuild
 * any document (e.g. to retry a spooled job) without depending on the feature module directly.
 */
@MapKey
annotation class DocumentTypeKey(val value: DocumentType)
