package com.ampairs.di

import androidx.lifecycle.ViewModel
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelMultibindings
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * Merges two [MetroViewModelMultibindings] sources into a single [MetroViewModelFactory].
 *
 * Both [WorkspaceGraph] and [AppGraph] implement [MetroViewModelMultibindings] via [ViewModelGraph],
 * which exposes the VM provider maps as public interface members — unlike [MetroViewModelFactory]
 * whose maps are protected. Passing the graphs directly avoids the protected-member access issue.
 *
 * Primary contributions override fallback contributions for duplicate ViewModel keys.
 */
class CombinedMetroViewModelFactory(
    private val primary: MetroViewModelMultibindings,
    private val fallback: MetroViewModelMultibindings,
) : MetroViewModelFactory() {
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel> = buildMap {
        putAll(fallback.viewModelProviders)
        putAll(primary.viewModelProviders)
    }
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory> = buildMap {
        putAll(fallback.assistedFactoryProviders)
        putAll(primary.assistedFactoryProviders)
    }
    override val manualAssistedFactoryProviders: Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory> = buildMap {
        putAll(fallback.manualAssistedFactoryProviders)
        putAll(primary.manualAssistedFactoryProviders)
    }
}
