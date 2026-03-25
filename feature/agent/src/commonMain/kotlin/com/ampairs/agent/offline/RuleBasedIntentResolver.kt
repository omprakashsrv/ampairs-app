package com.ampairs.agent.offline

import com.ampairs.agent.core.ActionDescriptor
import com.ampairs.agent.core.ActionType
import com.ampairs.agent.core.AgentAction
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent

/**
 * Simple keyword/pattern-based intent resolver for offline use.
 * Handles common CRUD patterns without needing an LLM.
 */
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

        return tryCreate(trimmed)
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
        "I can help you manage customers, products, orders, invoices, and inventory. Try:\n" +
            "  - \"add customer John\"\n" +
            "  - \"search orders 1001\"\n" +
            "  - \"list invoices with status PENDING\"\n" +
            "  - \"how many invoices\"\n" +
            "  - \"low stock items\"\n" +
            "  - \"stock level for product PRD123\"\n" +
            "  - \"show order details of 1001\"\n" +
            "  - \"sync inventory\""
    )

    private fun normalizeModule(raw: String): String = raw.lowercase().trim()
}
