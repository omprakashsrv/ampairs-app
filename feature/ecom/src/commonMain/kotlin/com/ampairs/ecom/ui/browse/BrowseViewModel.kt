package com.ampairs.ecom.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.TaxonomyType
import com.ampairs.ecom.data.db.entity.TaxonomyImageEntity
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.data.repository.CatalogRepository
import com.ampairs.ecom.data.repository.StorefrontRepository
import com.ampairs.ecom.domain.ProductRow
import com.ampairs.ecom.domain.EcomSession
import com.ampairs.ecom.domain.withCartQty
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowseUiState(
    val storeName: String = "",
    val categories: List<TaxonomyImageEntity> = emptyList(),
    val brands: List<TaxonomyImageEntity> = emptyList(),
    val popular: List<ProductRow> = emptyList(),
    val cartCount: Int = 0,
    val cartTotal: Double = 0.0,
    // Catalog-load status, so the shop can show a spinner / offline-retry / empty state instead of
    // a silently blank screen when nothing is cached yet.
    val loadFailed: Boolean = false,
    // A catalog pull has finished (success or failure) at least once this session. Until then an
    // empty catalog is treated as "loading", not "empty" — avoids a blank/empty flash on entry.
    val settled: Boolean = false,
) {
    /** True when there's nothing cached to render (no categories, brands or products). */
    val isEmpty: Boolean get() = categories.isEmpty() && brands.isEmpty() && popular.isEmpty()
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class BrowseViewModel(
    private val storefrontRepository: StorefrontRepository,
    private val catalogRepository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val session: EcomSession,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BrowseUiState> = session.active.flatMapLatest { active ->
        if (active == null) {
            flowOf(BrowseUiState())
        } else {
            combine(
                storefrontRepository.observeStorefront(active.slug),
                storefrontRepository.observeTaxonomy(active.storefrontId),
                catalogRepository.observeVisible(active.storefrontId),
                cartRepository.observeItems(active.storefrontId),
                syncService.observeEntity(SyncEntity.ECOM_PRODUCT),
            ) { storefront, taxonomy, products, cartItems, catalogSync ->
                val status = catalogSync?.status
                BrowseUiState(
                    storeName = storefront?.name ?: "",
                    categories = taxonomy.filter { it.type == TaxonomyType.CATEGORY.name },
                    brands = taxonomy.filter { it.type == TaxonomyType.BRAND.name },
                    popular = products.take(8).withCartQty(cartItems),
                    cartCount = cartItems.sumOf { it.quantity },
                    cartTotal = cartItems.sumOf { it.unit_price * it.quantity },
                    loadFailed = status is SyncStatus.Failed,
                    settled = status is SyncStatus.Success || status is SyncStatus.Failed,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseUiState())

    init {
        val slug = session.activeSlug
        val storefrontId = session.activeStorefrontId
        if (slug != null && storefrontId != null) {
            // Kick a catalog pull on entry so the shop shows a spinner (then products / offline-retry)
            // rather than a blank screen. Idempotent — CentralSyncService serializes it per entity.
            syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_PRODUCT))
            viewModelScope.launch {
                // Make sure a local cart exists for quick-add, and refresh taxonomy tiles.
                cartRepository.ensureCart(storefrontId)
                storefrontRepository.refreshCatalogMeta(slug, storefrontId)
            }
        }
    }

    fun refresh() {
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_PRODUCT))
        session.activeSlug?.let { slug ->
            session.activeStorefrontId?.let { id ->
                viewModelScope.launch { storefrontRepository.refreshCatalogMeta(slug, id) }
            }
        }
    }

    fun setQuantity(productId: String, quantity: Int) {
        val storefrontId = session.activeStorefrontId ?: return
        viewModelScope.launch {
            cartRepository.setItem(storefrontId, productId, quantity)
                .onFailure { _messages.tryEmit(it.message ?: "Couldn't update cart") }
        }
    }
}
