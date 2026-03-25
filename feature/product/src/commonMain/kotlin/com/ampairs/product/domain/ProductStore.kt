package com.ampairs.product.domain

import com.ampairs.product.data.repository.ProductRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

data class ProductKey(val productId: String)
data class ProductListKey(val searchQuery: String = "", val categoryIds: List<String> = emptyList())

@OptIn(ExperimentalStoreApi::class)
class ProductStore(private val repository: ProductRepository) {

    val productListStore: Store<ProductListKey, List<ProductListItem>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key: ProductListKey ->
                when {
                    key.searchQuery.isNotBlank() -> repository.searchProducts(key.searchQuery)
                    key.categoryIds.isNotEmpty() -> kotlinx.coroutines.flow.flow {
                        emit(repository.getProductsByCategory(key.categoryIds))
                    }
                    else -> repository.observeProducts()
                }
            }
        ).build()

    val productStore: Store<ProductKey, Product> = StoreBuilder
        .from<ProductKey, Product>(
            fetcher = Fetcher.ofFlow { key: ProductKey ->
                kotlinx.coroutines.flow.flow {
                    val product = repository.getProduct(key.productId)
                    emit(product ?: throw Exception("Product not found: ${key.productId}"))
                }
            }
        ).build()

    suspend fun createProduct(product: Product): Result<Product> {
        val result = repository.createProduct(product)
        if (result.isSuccess) {
            // Invalidate store to refresh data
            productListStore.clear()
        }
        return result
    }

    suspend fun updateProduct(product: Product): Result<Product> {
        val result = repository.updateProduct(product)
        if (result.isSuccess) {
            // Invalidate stores to refresh data
            productListStore.clear()
            productStore.clear()
        }
        return result
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val result = repository.deleteProduct(productId)
        if (result.isSuccess) {
            // Invalidate stores to refresh data
            productListStore.clear()
            productStore.clear()
        }
        return result
    }
}

data class ProductListItem(
    val id: String,
    val name: String,
    val code: String,
    val mrp: Double,
    val sellingPrice: Double,
    val categoryName: String? = null,
    val brandName: String? = null,
    val stockQuantity: Double? = null,
    val imageUrl: String? = null,
    val active: Boolean = true
)