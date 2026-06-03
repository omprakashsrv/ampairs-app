package com.ampairs.ecom.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.TaxonomyType
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.data.repository.CatalogRepository
import com.ampairs.ecom.data.repository.StorefrontRepository
import com.ampairs.ecom.domain.EcomSession
import com.ampairs.ecom.domain.ProductRow
import com.ampairs.ecom.domain.withCartQty
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Drill-down filter set (one of category / brand / subcategory / query is typically set). */
data class DrillDownArgs(
    val category: String? = null,
    val brand: String? = null,
    val subcategory: String? = null,
    val query: String? = null,
) {
    val title: String get() = category ?: brand ?: query?.let { "\"$it\"" } ?: "Products"
    val key: String get() = "c=$category;b=$brand;s=$subcategory;q=$query"
}

data class DrillDownUiState(
    val title: String = "",
    val products: List<ProductRow> = emptyList(),
    val refineOptions: List<String> = emptyList(),
    val activeRefine: String? = null,
    val cartCount: Int = 0,
    val cartTotal: Double = 0.0,
)

@AssistedInject
class DrillDownViewModel(
    @Assisted val args: DrillDownArgs,
    private val catalogRepository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val storefrontRepository: StorefrontRepository,
    private val session: EcomSession,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val refine = MutableStateFlow(args.subcategory)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DrillDownUiState> =
        combine(session.active, refine) { active, r -> active to r }
            .flatMapLatest { (active, activeRefine) ->
                if (active == null) {
                    flowOf(DrillDownUiState(title = args.title))
                } else {
                    val id = active.storefrontId
                    val productsFlow = if (args.query != null) {
                        catalogRepository.observeVisible(id).map { list ->
                            val q = args.query.trim().lowercase()
                            list.filter { it.name.lowercase().contains(q) || (it.brand?.lowercase()?.contains(q) == true) }
                        }
                    } else {
                        catalogRepository.observeFiltered(id, args.category, args.brand, activeRefine)
                    }
                    combine(
                        productsFlow,
                        cartRepository.observeItems(id),
                        storefrontRepository.observeTaxonomy(id),
                    ) { products, cartItems, taxonomy ->
                        val refineOptions = when {
                            args.category != null -> taxonomy
                                .filter { it.type == TaxonomyType.SUBCATEGORY.name && it.parent_name == args.category }
                                .map { it.name }
                            args.brand != null -> products.mapNotNull { it.category }.distinct()
                            else -> emptyList()
                        }
                        DrillDownUiState(
                            title = args.title,
                            products = products.withCartQty(cartItems),
                            refineOptions = refineOptions,
                            activeRefine = activeRefine,
                            cartCount = cartItems.sumOf { it.quantity },
                            cartTotal = cartItems.sumOf { it.unit_price * it.quantity },
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrillDownUiState(title = args.title))

    fun toggleRefine(value: String) {
        refine.value = if (refine.value == value) null else value
    }

    fun setQuantity(productId: String, quantity: Int) {
        val slug = session.activeSlug ?: return
        val storefrontId = session.activeStorefrontId ?: return
        viewModelScope.launch {
            cartRepository.setItem(slug, storefrontId, productId, quantity)
                .onFailure { _messages.tryEmit(it.message ?: "Couldn't update cart") }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(args: DrillDownArgs): DrillDownViewModel
    }
}
