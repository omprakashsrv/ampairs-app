package com.ampairs.product.agent

import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionHandler
import com.ampairs.common.agent.ActionParameter
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.NavigationTarget
import com.ampairs.common.agent.ParameterType
import com.ampairs.common.agent.ActionHandlerKey
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.Product
import kotlinx.coroutines.flow.first

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("product")
class ProductActionHandler(
    private val productRepository: ProductRepository,
) : ActionHandler {

    override val moduleName = "product"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.CREATE -> createProduct(action.params)
        ActionType.SEARCH -> searchProducts(action.params)
        ActionType.READ -> getProduct(action.params)
        ActionType.UPDATE -> updateProduct(action.params)
        ActionType.DELETE -> deleteProduct(action.params)
        ActionType.COUNT -> countProducts()
        ActionType.LOW_STOCK -> reportLowStock(action.params)
        ActionType.OUT_OF_STOCK -> reportOutOfStock()
        ActionType.INVENTORY_VALUE -> reportInventoryValue()
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    // ── Curated stock reports (deterministic; preferred over free SQL) ────────────────────────────

    private suspend fun reportLowStock(params: Map<String, String>): ActionResult {
        val limit = params["limit"]?.trim()?.toIntOrNull()?.coerceIn(1, 20) ?: 10
        val total = productRepository.countLowStock()
        val items = productRepository.lowStockProducts(limit)
        return if (items.isEmpty()) {
            ActionResult.Success("No products are low on stock.")
        } else {
            val lines = items.joinToString("\n") { "• ${it.name} — ${formatQty(it.quantity)} left" }
            val more = if (total > items.size) "\n… and ${total - items.size} more" else ""
            ActionResult.Success("$total product(s) low on stock:\n$lines$more")
        }
    }

    private suspend fun reportOutOfStock(): ActionResult {
        val count = productRepository.countOutOfStock()
        return ActionResult.Success("$count product(s) are out of stock.")
    }

    private suspend fun reportInventoryValue(): ActionResult {
        val value = productRepository.inventoryValueAtCost()
        return ActionResult.Success(summary = "Total inventory value (at cost).", amount = value)
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private suspend fun createProduct(params: Map<String, String>): ActionResult {
        val name = params["name"]
            ?: return ActionResult.NeedsInput("What is the product name?", listOf("name"))

        val uid = UidGenerator.generateUid(Constants.PRODUCT_PREFIX)
        val product = Product(
            id = uid,
            name = name,
            code = params["code"] ?: "",
            sellingPrice = params["sellingPrice"]?.toDoubleOrNull() ?: 0.0,
            mrp = params["mrp"]?.toDoubleOrNull() ?: params["sellingPrice"]?.toDoubleOrNull() ?: 0.0,
            description = params["description"] ?: "",
        )

        val result = productRepository.createProduct(product)
        return if (result.isSuccess) {
            ActionResult.Success(
                summary = "Created product '$name' successfully.",
                navigationTarget = NavigationTarget(
                    routeDescription = "ProductDetails",
                    routeData = mapOf("productId" to uid),
                ),
            )
        } else {
            ActionResult.Error("Failed to create product: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun searchProducts(params: Map<String, String>): ActionResult {
        val query = params["query"]
            ?: return ActionResult.NeedsInput("What should I search for?", listOf("query"))

        val products = productRepository.searchProducts(query).first()
        return if (products.isEmpty()) {
            ActionResult.Success("No products found matching '$query'.")
        } else {
            val summary = buildString {
                appendLine("Found ${products.size} product(s):")
                products.take(5).forEach { p ->
                    appendLine("  - ${p.name} (${p.code}) - ₹${p.sellingPrice}")
                }
                if (products.size > 5) {
                    appendLine("  ...and ${products.size - 5} more")
                }
            }
            ActionResult.Success(summary)
        }
    }

    private suspend fun getProduct(params: Map<String, String>): ActionResult {
        val productId = params["productId"]
        val searchName = params["searchName"]

        val product = when {
            productId != null -> productRepository.getProduct(productId)
            searchName != null -> {
                val results = productRepository.searchProducts(searchName).first()
                if (results.isEmpty()) return ActionResult.Success("No product found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.Success("Multiple products found:\n$listing\nPlease specify the product ID.")
                }
                productRepository.getProduct(results.first().id)
            }
            else -> return ActionResult.NeedsInput("Which product? Provide a name or ID.", listOf("searchName"))
        }

        return if (product != null) {
            ActionResult.Success(
                summary = buildString {
                    appendLine("Product: ${product.name}")
                    if (product.code.isNotBlank()) appendLine("  Code: ${product.code}")
                    appendLine("  Selling Price: ₹${product.sellingPrice}")
                    if (product.mrp > 0) appendLine("  MRP: ₹${product.mrp}")
                    product.categoryName?.let { appendLine("  Category: $it") }
                    product.stockQuantity?.let { appendLine("  Stock: $it") }
                },
                navigationTarget = NavigationTarget(
                    routeDescription = "ProductDetails",
                    routeData = mapOf("productId" to product.id),
                ),
            )
        } else {
            ActionResult.Error("Product not found.")
        }
    }

    private suspend fun updateProduct(params: Map<String, String>): ActionResult {
        val productId = params["productId"]
        val searchName = params["searchName"]

        val existing = when {
            productId != null -> productRepository.getProduct(productId)
            searchName != null -> {
                val results = productRepository.searchProducts(searchName).first()
                if (results.isEmpty()) return ActionResult.Error("No product found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.NeedsInput("Multiple products found:\n$listing\nWhich product ID?", listOf("productId"))
                }
                productRepository.getProduct(results.first().id)
            }
            else -> return ActionResult.NeedsInput("Which product do you want to update?", listOf("searchName"))
        } ?: return ActionResult.Error("Product not found.")

        val updated = existing.copy(
            name = params["name"] ?: existing.name,
            code = params["code"] ?: existing.code,
            sellingPrice = params["sellingPrice"]?.toDoubleOrNull() ?: existing.sellingPrice,
            mrp = params["mrp"]?.toDoubleOrNull() ?: existing.mrp,
            description = params["description"] ?: existing.description,
        )

        val result = productRepository.updateProduct(updated)
        return if (result.isSuccess) {
            ActionResult.Success("Updated product '${updated.name}' successfully.")
        } else {
            ActionResult.Error("Failed to update: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun deleteProduct(params: Map<String, String>): ActionResult {
        val productId = params["productId"]
        val searchName = params["searchName"]

        val resolvedId = when {
            productId != null -> productId
            searchName != null -> {
                val results = productRepository.searchProducts(searchName).first()
                if (results.isEmpty()) return ActionResult.Error("No product found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.NeedsInput("Multiple products found:\n$listing\nWhich product ID?", listOf("productId"))
                }
                results.first().id
            }
            else -> return ActionResult.NeedsInput("Which product do you want to delete?", listOf("searchName"))
        }

        val result = productRepository.deleteProduct(resolvedId)
        return if (result.isSuccess) {
            ActionResult.Success("Product deleted successfully.")
        } else {
            ActionResult.Error("Failed to delete: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun countProducts(): ActionResult {
        val count = productRepository.getProductCount()
        return ActionResult.Success("You have $count product(s).")
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE,
                moduleName = "product",
                description = "Create a new product",
                parameters = listOf(
                    ActionParameter("name", ParameterType.STRING, required = true, "Product name"),
                    ActionParameter("code", ParameterType.STRING, required = false, "Product code/SKU"),
                    ActionParameter("sellingPrice", ParameterType.NUMBER, required = false, "Selling price"),
                    ActionParameter("mrp", ParameterType.NUMBER, required = false, "MRP"),
                    ActionParameter("description", ParameterType.STRING, required = false, "Description"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "product",
                description = "Search products by name or code",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "product",
                description = "Get product details by ID or name",
                parameters = listOf(
                    ActionParameter("productId", ParameterType.STRING, required = false, "Product ID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Product name to search"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.UPDATE,
                moduleName = "product",
                description = "Update a product's information",
                parameters = listOf(
                    ActionParameter("productId", ParameterType.STRING, required = false, "Product ID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Product name to find"),
                    ActionParameter("name", ParameterType.STRING, required = false, "New name"),
                    ActionParameter("sellingPrice", ParameterType.NUMBER, required = false, "New selling price"),
                    ActionParameter("mrp", ParameterType.NUMBER, required = false, "New MRP"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.DELETE,
                moduleName = "product",
                description = "Delete a product",
                parameters = listOf(
                    ActionParameter("productId", ParameterType.STRING, required = false, "Product ID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Product name to find"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "product",
                description = "Get total product count",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.LOW_STOCK,
                moduleName = "product",
                description = "Products at or below their low-stock alert level. Use for \"low stock\", \"running low\", \"what needs reordering\".",
                parameters = listOf(
                    ActionParameter("limit", ParameterType.NUMBER, required = false, "How many to list (default 10, max 20)."),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.OUT_OF_STOCK,
                moduleName = "product",
                description = "Count of products with zero on-hand stock. Use for \"out of stock\", \"sold out items\".",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.INVENTORY_VALUE,
                moduleName = "product",
                description = "Total inventory valuation at cost (Σ on-hand × cost price). Use for \"inventory value\", \"stock value\", \"worth of stock\".",
                parameters = emptyList(),
            ),
        )
    }
}
