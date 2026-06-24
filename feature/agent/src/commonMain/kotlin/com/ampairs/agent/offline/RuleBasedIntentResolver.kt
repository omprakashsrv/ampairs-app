package com.ampairs.agent.offline

import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.ActionType
import com.ampairs.common.agent.AgentAction
import dev.zacsweers.metro.Inject

/**
 * Simple keyword/pattern-based intent resolver for offline use.
 * Handles common CRUD patterns without needing an LLM.
 */
@Inject
class RuleBasedIntentResolver : IntentResolver {

    override suspend fun resolve(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        availableActions: List<ActionDescriptor>,
    ): ResolvedIntent {
        val trimmed = userMessage.trim()
        if (trimmed.isBlank()) {
            return ResolvedIntent.Conversation("Please type a command or question.")
        }

        return tryCartShow(trimmed)
            ?: tryCartClear(trimmed)
            ?: tryCartRemoveLast(trimmed)
            ?: tryFinalizeDocument(trimmed)
            ?: tryCartSetCustomer(trimmed)
            ?: tryCartAddItem(trimmed)
            ?: tryCreate(trimmed)
            ?: trySearch(trimmed)
            ?: tryCount(trimmed)
            ?: tryList(trimmed)
            ?: tryGetInventory(trimmed)
            ?: tryUpdate(trimmed)
            ?: tryDelete(trimmed)
            ?: trySync(trimmed)
            ?: tryRead(trimmed)
            ?: fallback()
    }

    // ── Conversational cart (multi-item ordering) ──
    // The flow: "add 2 widgets" → builds a cart, "customer John" → sets the buyer, "create invoice"
    // / "create order" → finalizes. The handlers do the real product/customer matching against local
    // data; these patterns only extract the action + slots (mirrors what the on-device LLM emits).

    // ── Pattern: "create/make/new an invoice/bill/order [for] {customer?}" → finalize the cart ──
    private fun tryFinalizeDocument(input: String): ResolvedIntent? {
        val pattern = Regex(
            """(?i)(?:create|make|generate|raise|place|checkout|finali[sz]e|new)\s+(?:an?\s+|as\s+)?(invoice|bill|order)(?:\s+(?:for\s+)?(.+))?$"""
        )
        val match = pattern.find(input) ?: return null
        val module = if (match.groupValues[1].lowercase() == "order") "order" else "invoice"
        val customer = match.groupValues[2].trim()
        val params = if (customer.isNotBlank()) mapOf("customer" to customer) else emptyMap()
        return ResolvedIntent.Action(AgentAction(ActionType.CREATE, module, params))
    }

    // ── Pattern: "add {qty} [x] {product} [at|@|for {price}]" → add a line to the cart ──
    private fun tryCartAddItem(input: String): ResolvedIntent? {
        val pattern = Regex(
            """(?i)^(?:add|put|include)\s+(\d+(?:\.\d+)?)\s+(?:x\s+|of\s+)?(.+?)(?:\s+(?:at|@|for|each\s+at|priced?\s+at)\s+(?:₹|rs\.?|inr|\$)?\s*(\d+(?:\.\d+)?))?$"""
        )
        val match = pattern.find(input) ?: return null
        val qty = match.groupValues[1].trim()
        val product = match.groupValues[2].trim()
        val price = match.groupValues[3].trim()
        if (product.isBlank()) return null
        val params = buildMap {
            put("product", product)
            put("quantity", qty)
            if (price.isNotBlank()) put("price", price)
        }
        return ResolvedIntent.Action(AgentAction(ActionType.ADD_ITEM, "cart", params))
    }

    // ── Pattern: "customer John" / "bill to John" / "set customer to John" → set the cart's buyer ──
    private fun tryCartSetCustomer(input: String): ResolvedIntent? {
        val pattern = Regex(
            """(?i)^(?:bill\s+to|set\s+customer(?:\s+to)?|customer(?:\s+is)?|for\s+customer)\s+(.+)$"""
        )
        val match = pattern.find(input) ?: return null
        val customer = match.groupValues[1].trim()
        if (customer.isBlank()) return null
        return ResolvedIntent.Action(AgentAction(ActionType.SET_CUSTOMER, "cart", mapOf("customer" to customer)))
    }

    // ── Pattern: "show cart" / "view bill" / "what's in my cart" ──
    private fun tryCartShow(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)^(?:show|view|see|open|what'?s\s+in)\s+(?:my\s+|the\s+|current\s+)?(?:cart|bill|draft|basket)$""")
        if (pattern.find(input) == null) return null
        return ResolvedIntent.Action(AgentAction(ActionType.LIST, "cart"))
    }

    // ── Pattern: "clear cart" / "empty bill" / "discard draft" ──
    private fun tryCartClear(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)^(?:clear|empty|reset|discard|cancel)\s+(?:my\s+|the\s+)?(?:cart|bill|draft|basket)$""")
        if (pattern.find(input) == null) return null
        return ResolvedIntent.Action(AgentAction(ActionType.DELETE, "cart"))
    }

    // ── Pattern: "remove last [item]" / "undo last" ──
    private fun tryCartRemoveLast(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)^(?:remove|delete|drop|undo)\s+(?:the\s+)?last(?:\s+item)?$""")
        if (pattern.find(input) == null) return null
        return ResolvedIntent.Action(AgentAction(ActionType.DELETE, "cart", mapOf("scope" to "last")))
    }

    // ── Pattern: "add/create/new {module} [called/named] {name}" ──

    private fun tryCreate(input: String): ResolvedIntent? {
        val patterns = listOf(
            Regex("""(?i)(?:add|create|new)\s+(customer|product|unit|order)\s+(?:called|named)\s+(.+)"""),
            Regex("""(?i)(?:add|create|new)\s+(customer|product|unit|order)\s+(.+)"""),
        )
        for (pattern in patterns) {
            pattern.find(input)?.let { match ->
                val module = normalizeModule(match.groupValues[1])
                val name = match.groupValues[2].trim()
                return ResolvedIntent.Action(
                    AgentAction(
                        actionType = ActionType.CREATE,
                        moduleName = module,
                        params = mapOf("name" to name),
                    )
                )
            }
        }
        return null
    }

    // ── Pattern: "search/find/show/list {module}s {query}" ──

    private fun trySearch(input: String): ResolvedIntent? {
        val patterns = listOf(
            Regex("""(?i)(?:search|find|look\s+up|show|list)\s+(?:me\s+)?(?:all\s+)?(?:the\s+)?(customer|product|unit|order|inventory|invoice)s?\s+(.+)"""),
            Regex("""(?i)(?:search|find)\s+(.+?)\s+in\s+(customer|product|unit|order|inventory|invoice)s?"""),
        )
        for (pattern in patterns) {
            pattern.find(input)?.let { match ->
                val module = normalizeModule(match.groupValues[1])
                val query = match.groupValues[2].trim()
                return ResolvedIntent.Action(
                    AgentAction(
                        actionType = ActionType.SEARCH,
                        moduleName = module,
                        params = mapOf("query" to query),
                    )
                )
            }
        }
        return null
    }

    // ── Pattern: "how many {module}s" / "count {module}s" ──

    private fun tryCount(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)(?:how\s+many|count|total)\s+(customer|product|unit|order|invoice|inventory)s?""")
        pattern.find(input)?.let { match ->
            val module = normalizeModule(match.groupValues[1])
            return ResolvedIntent.Action(
                AgentAction(actionType = ActionType.COUNT, moduleName = module)
            )
        }
        return null
    }

    // ── Pattern: "list {module}s [with status {status}]" / "low stock [below {n}]" ──

    private fun tryList(input: String): ResolvedIntent? {
        // Low stock inventory
        Regex("""(?i)(?:list\s+)?low[\s-]stock(?:\s+items?)?(?:\s+below\s+(\d+))?""").find(input)?.let { match ->
            val threshold = match.groupValues[1].ifBlank { null }
            val params = if (threshold != null) mapOf("threshold" to threshold) else emptyMap()
            return ResolvedIntent.Action(AgentAction(ActionType.LIST, "inventory", params))
        }
        // List orders/invoices by status
        Regex("""(?i)list\s+(order|invoice)s?(?:\s+(?:with\s+)?status\s+(.+))?""").find(input)?.let { match ->
            val module = normalizeModule(match.groupValues[1])
            val status = match.groupValues[2].trim().ifBlank { null }
            val params = if (status != null) mapOf("status" to status) else emptyMap()
            return ResolvedIntent.Action(AgentAction(ActionType.LIST, module, params))
        }
        return null
    }

    // ── Pattern: "stock level for {productId}" / "inventory for product {id}" ──

    private fun tryGetInventory(input: String): ResolvedIntent? {
        val patterns = listOf(
            Regex("""(?i)(?:stock|inventory)\s+(?:level\s+)?(?:for|of)\s+(?:product\s+)?(.+)"""),
            Regex("""(?i)how\s+much\s+(?:stock|inventory)\s+(?:for|of)\s+(?:product\s+)?(.+)"""),
        )
        for (pattern in patterns) {
            pattern.find(input)?.let { match ->
                val productId = match.groupValues[1].trim()
                return ResolvedIntent.Action(
                    AgentAction(ActionType.GET_INVENTORY, "inventory", mapOf("productId" to productId))
                )
            }
        }
        return null
    }

    // ── Pattern: "update {module} {name}'s {field} to {value}" ──

    private fun tryUpdate(input: String): ResolvedIntent? {
        val patterns = listOf(
            Regex("""(?i)update\s+(customer|product)\s+(.+?)(?:'s)?\s+(phone|email|name|city|price)\s+(?:to|=)\s+(.+)"""),
            Regex("""(?i)(?:change|set)\s+(?:the\s+)?(phone|email|name|city|price)\s+(?:of|for)\s+(customer|product)\s+(.+?)\s+to\s+(.+)"""),
        )
        patterns[0].find(input)?.let { match ->
            val module = normalizeModule(match.groupValues[1])
            val identifier = match.groupValues[2].trim()
            val field = match.groupValues[3].lowercase()
            val value = match.groupValues[4].trim()
            return ResolvedIntent.Action(
                AgentAction(
                    actionType = ActionType.UPDATE,
                    moduleName = module,
                    params = mapOf(
                        "searchName" to identifier,
                        field to value,
                    ),
                )
            )
        }
        patterns[1].find(input)?.let { match ->
            val field = match.groupValues[1].lowercase()
            val module = normalizeModule(match.groupValues[2])
            val identifier = match.groupValues[3].trim()
            val value = match.groupValues[4].trim()
            return ResolvedIntent.Action(
                AgentAction(
                    actionType = ActionType.UPDATE,
                    moduleName = module,
                    params = mapOf(
                        "searchName" to identifier,
                        field to value,
                    ),
                )
            )
        }
        return null
    }

    // ── Pattern: "delete/remove {module} {name/id}" ──

    private fun tryDelete(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)(?:delete|remove)\s+(customer|product|unit)\s+(.+)""")
        pattern.find(input)?.let { match ->
            val module = normalizeModule(match.groupValues[1])
            val identifier = match.groupValues[2].trim()
            return ResolvedIntent.Action(
                AgentAction(
                    actionType = ActionType.DELETE,
                    moduleName = module,
                    params = mapOf("searchName" to identifier),
                )
            )
        }
        return null
    }

    // ── Pattern: "sync {module}s" ──

    private fun trySync(input: String): ResolvedIntent? {
        val pattern = Regex("""(?i)sync\s+(customer|product|unit|order|invoice|inventory)s?""")
        pattern.find(input)?.let { match ->
            val module = normalizeModule(match.groupValues[1])
            return ResolvedIntent.Action(
                AgentAction(actionType = ActionType.SYNC, moduleName = module)
            )
        }
        return null
    }

    // ── Pattern: "get/show {module} {name/id}" (read single) ──

    private fun tryRead(input: String): ResolvedIntent? {
        val patterns = listOf(
            Regex("""(?i)(?:get|show|view)\s+(?:me\s+)?(?:the\s+)?(customer|product|unit|order|invoice)\s+(?:details?\s+(?:of|for)\s+)?(.+)"""),
            Regex("""(?i)(?:details?\s+(?:of|for))\s+(customer|product|unit|order|invoice)\s+(.+)"""),
        )
        for (pattern in patterns) {
            pattern.find(input)?.let { match ->
                val module = normalizeModule(match.groupValues[1])
                val identifier = match.groupValues[2].trim()
                return ResolvedIntent.Action(
                    AgentAction(
                        actionType = ActionType.READ,
                        moduleName = module,
                        params = mapOf("searchName" to identifier),
                    )
                )
            }
        }
        return null
    }

    private fun fallback(): ResolvedIntent = ResolvedIntent.Conversation(
        "I can help you manage customers, products, orders, invoices, and inventory.\n" +
            "Build a bill or order conversationally:\n" +
            "  - \"add 2 widgets\"  (optionally \"... at 90\")\n" +
            "  - \"add 3 bolts\"\n" +
            "  - \"customer John\"\n" +
            "  - \"show cart\"  /  \"remove last\"  /  \"clear cart\"\n" +
            "  - \"create invoice\"  or  \"create order\"  to finalize\n" +
            "Or single commands:\n" +
            "  - \"add customer John\"\n" +
            "  - \"search orders 1001\"\n" +
            "  - \"how many invoices\"\n" +
            "  - \"low stock items\"\n" +
            "  - \"sync inventory\""
    )

    private fun normalizeModule(raw: String): String = raw.lowercase().trim()
}
