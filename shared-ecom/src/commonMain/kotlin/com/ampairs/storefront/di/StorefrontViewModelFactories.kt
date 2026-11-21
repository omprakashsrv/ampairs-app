package com.ampairs.storefront.di

import androidx.lifecycle.ViewModel
import com.ampairs.common.di.AppScope
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * Concrete [MetroViewModelFactory] bindings — one per scope. Mirror the pair in :shared. Each graph
 * (`ViewModelGraph`) resolves `metroViewModelFactory` from these, wiring the `@ContributesIntoMap`
 * ViewModels of its scope.
 */
@Inject
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
class StorefrontAppViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()

@Inject
@ContributesBinding(WorkspaceScope::class)
@SingleIn(WorkspaceScope::class)
class StorefrontWorkspaceViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
