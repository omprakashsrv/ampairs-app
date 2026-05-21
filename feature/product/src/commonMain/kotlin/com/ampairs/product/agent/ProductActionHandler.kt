package com.ampairs.product.agent

import com.ampairs.agent.core.ActionDescriptor
import com.ampairs.agent.core.ActionHandler
import com.ampairs.agent.core.ActionParameter
import com.ampairs.agent.core.ActionResult
import com.ampairs.agent.core.ActionType
import com.ampairs.agent.core.AgentAction
import com.ampairs.agent.core.NavigationTarget
import com.ampairs.agent.core.ParameterType
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.Inject
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.repository.ProductRepository
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.Product
import kotlinx.coroutines.flow.first

@Inject
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
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

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
        )
    }
}
