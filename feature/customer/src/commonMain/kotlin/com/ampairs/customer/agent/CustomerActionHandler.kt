package com.ampairs.customer.agent

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
import com.ampairs.customer.data.repository.CustomerRepository
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import kotlinx.coroutines.flow.first

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ActionHandlerKey("customer")
class CustomerActionHandler(
    private val customerRepository: CustomerRepository,
    private val syncService: CentralSyncService,
) : ActionHandler {

    override val moduleName = "customer"

    override val supportedActions: List<ActionDescriptor> get() = ACTIONS

    override suspend fun execute(action: AgentAction): ActionResult = when (action.actionType) {
        ActionType.CREATE -> createCustomer(action.params)
        ActionType.SEARCH -> searchCustomers(action.params)
        ActionType.READ -> getCustomer(action.params)
        ActionType.UPDATE -> updateCustomer(action.params)
        ActionType.DELETE -> deleteCustomer(action.params)
        ActionType.COUNT -> countCustomers()
        ActionType.SYNC -> syncCustomers()
        else -> ActionResult.Error("Unsupported action: ${action.actionType}")
    }

    private suspend fun createCustomer(params: Map<String, String>): ActionResult {
        val name = params["name"]
            ?: return ActionResult.NeedsInput("What is the customer's name?", listOf("name"))

        val uid = UidGenerator.generateUid(CustomerConstants.UID_PREFIX)
        val customer = Customer(
            uid = uid,
            name = name,
            phone = params["phone"],
            email = params["email"],
            city = params["city"],
            gstNumber = params["gstNumber"],
        )

        val result = customerRepository.createCustomer(customer)
        return if (result.isSuccess) {
            ActionResult.Success(
                summary = "Created customer '$name' successfully.",
                navigationTarget = NavigationTarget(
                    routeDescription = "CustomerDetails",
                    routeData = mapOf("customerId" to uid),
                ),
            )
        } else {
            ActionResult.Error("Failed to create customer: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun searchCustomers(params: Map<String, String>): ActionResult {
        val query = params["query"]
            ?: return ActionResult.NeedsInput("What should I search for?", listOf("query"))

        val customers = customerRepository.searchCustomers(query).first()
        return if (customers.isEmpty()) {
            ActionResult.Success("No customers found matching '$query'.")
        } else {
            val summary = buildString {
                appendLine("Found ${customers.size} customer(s):")
                customers.take(5).forEach { c ->
                    appendLine("  - ${c.name} (${c.phone ?: "no phone"}) - ${c.city ?: "no city"}")
                }
                if (customers.size > 5) {
                    appendLine("  ...and ${customers.size - 5} more")
                }
            }
            ActionResult.Success(summary)
        }
    }

    private suspend fun getCustomer(params: Map<String, String>): ActionResult {
        // Try by ID first, then by name search
        val customerId = params["customerId"]
        val searchName = params["searchName"]

        val customer = when {
            customerId != null -> customerRepository.getCustomer(customerId)
            searchName != null -> {
                val results = customerRepository.searchCustomers(searchName).first()
                if (results.isEmpty()) return ActionResult.Success("No customer found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.Success("Multiple customers found:\n$listing\nPlease specify the customer ID.")
                }
                customerRepository.getCustomer(results.first().id)
            }
            else -> return ActionResult.NeedsInput("Which customer? Provide a name or ID.", listOf("searchName"))
        }

        return if (customer != null) {
            ActionResult.Success(
                summary = buildString {
                    appendLine("Customer: ${customer.name}")
                    customer.phone?.let { appendLine("  Phone: $it") }
                    customer.email?.let { appendLine("  Email: $it") }
                    customer.city?.let { appendLine("  City: $it") }
                    customer.gstNumber?.let { appendLine("  GST: $it") }
                },
                navigationTarget = NavigationTarget(
                    routeDescription = "CustomerDetails",
                    routeData = mapOf("customerId" to customer.uid),
                ),
            )
        } else {
            ActionResult.Error("Customer not found.")
        }
    }

    private suspend fun updateCustomer(params: Map<String, String>): ActionResult {
        val customerId = params["customerId"]
        val searchName = params["searchName"]

        val existing = when {
            customerId != null -> customerRepository.getCustomer(customerId)
            searchName != null -> {
                val results = customerRepository.searchCustomers(searchName).first()
                if (results.isEmpty()) return ActionResult.Error("No customer found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.NeedsInput("Multiple customers found:\n$listing\nWhich customer ID?", listOf("customerId"))
                }
                customerRepository.getCustomer(results.first().id)
            }
            else -> return ActionResult.NeedsInput("Which customer do you want to update?", listOf("searchName"))
        } ?: return ActionResult.Error("Customer not found.")

        val updated = existing.copy(
            name = params["name"] ?: existing.name,
            phone = params["phone"] ?: existing.phone,
            email = params["email"] ?: existing.email,
            city = params["city"] ?: existing.city,
            gstNumber = params["gstNumber"] ?: existing.gstNumber,
        )

        val result = customerRepository.updateCustomer(updated)
        return if (result.isSuccess) {
            ActionResult.Success("Updated customer '${updated.name}' successfully.")
        } else {
            ActionResult.Error("Failed to update: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun deleteCustomer(params: Map<String, String>): ActionResult {
        val customerId = params["customerId"]
        val searchName = params["searchName"]

        val resolvedId = when {
            customerId != null -> customerId
            searchName != null -> {
                val results = customerRepository.searchCustomers(searchName).first()
                if (results.isEmpty()) return ActionResult.Error("No customer found matching '$searchName'.")
                if (results.size > 1) {
                    val listing = results.take(5).joinToString("\n") { "  - ${it.name} (${it.id})" }
                    return ActionResult.NeedsInput("Multiple customers found:\n$listing\nWhich customer ID?", listOf("customerId"))
                }
                results.first().id
            }
            else -> return ActionResult.NeedsInput("Which customer do you want to delete?", listOf("searchName"))
        }

        val result = customerRepository.deleteCustomer(resolvedId)
        return if (result.isSuccess) {
            ActionResult.Success("Customer deleted successfully.")
        } else {
            ActionResult.Error("Failed to delete: ${result.exceptionOrNull()?.message}")
        }
    }

    private suspend fun countCustomers(): ActionResult {
        val count = customerRepository.getCustomerCount()
        return ActionResult.Success("You have $count customer(s).")
    }

    private fun syncCustomers(): ActionResult {
        // Push + pull are coordinated by CentralSyncService; this just kicks off a full sync.
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.CUSTOMER))
        return ActionResult.Success("Customer sync started.")
    }

    companion object {
        val ACTIONS = listOf(
            ActionDescriptor(
                actionType = ActionType.CREATE,
                moduleName = "customer",
                description = "Create a new customer",
                parameters = listOf(
                    ActionParameter("name", ParameterType.STRING, required = true, "Customer name"),
                    ActionParameter("phone", ParameterType.STRING, required = false, "Phone number"),
                    ActionParameter("email", ParameterType.STRING, required = false, "Email address"),
                    ActionParameter("city", ParameterType.STRING, required = false, "City"),
                    ActionParameter("gstNumber", ParameterType.STRING, required = false, "GST number"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.SEARCH,
                moduleName = "customer",
                description = "Search customers by name, phone, or email",
                parameters = listOf(
                    ActionParameter("query", ParameterType.STRING, required = true, "Search query"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.READ,
                moduleName = "customer",
                description = "Get customer details by ID or name",
                parameters = listOf(
                    ActionParameter("customerId", ParameterType.STRING, required = false, "Customer UID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Customer name to search"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.UPDATE,
                moduleName = "customer",
                description = "Update a customer's information",
                parameters = listOf(
                    ActionParameter("customerId", ParameterType.STRING, required = false, "Customer UID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Customer name to find"),
                    ActionParameter("name", ParameterType.STRING, required = false, "New name"),
                    ActionParameter("phone", ParameterType.STRING, required = false, "New phone"),
                    ActionParameter("email", ParameterType.STRING, required = false, "New email"),
                    ActionParameter("city", ParameterType.STRING, required = false, "New city"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.DELETE,
                moduleName = "customer",
                description = "Delete a customer",
                parameters = listOf(
                    ActionParameter("customerId", ParameterType.STRING, required = false, "Customer UID"),
                    ActionParameter("searchName", ParameterType.STRING, required = false, "Customer name to find"),
                ),
            ),
            ActionDescriptor(
                actionType = ActionType.COUNT,
                moduleName = "customer",
                description = "Get total customer count",
                parameters = emptyList(),
            ),
            ActionDescriptor(
                actionType = ActionType.SYNC,
                moduleName = "customer",
                description = "Sync customers with server",
                parameters = emptyList(),
            ),
        )
    }
}
