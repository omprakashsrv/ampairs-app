package com.ampairs.agent.offline

import com.ampairs.agent.core.ActionRegistry
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.IntentResolver
import com.ampairs.agent.core.ResolvedIntent
import com.ampairs.agent.llm.AgentSchemaBuilder
import com.ampairs.agent.llm.LlmEngine
import com.ampairs.common.agent.ActionDescriptor
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.agent.ActionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * On-device NLU resolver (FR-003): asks the [LlmEngine] for a constrained `AgentAction` JSON (via
 * [AgentSchemaBuilder]'s schema), parses it, and validates it against the registry before dispatch.
 * The validation is the guarantee behind SC-003 — output that isn't a registered action/module, or
 * is missing a required param, becomes a [ResolvedIntent.Clarification] rather than a wrong action
 * (the A1 "no probabilistic confidence" decision, plan §4.2). One re-ask on parse failure.
 *
 * Engine-agnostic: it only sees the [LlmEngine] port, resolved lazily via [engineProvider] (wired to
 * `ProviderRegistry` in T023/T030). When no model is loaded the composite resolver (T030) routes to
 * the rule-based resolver instead, so here a null engine simply yields a Clarification.
 */
class LlmIntentResolver(
    private val actionRegistry: ActionRegistry,
    private val engineProvider: suspend () -> LlmEngine?,
    private val json: Json = DEFAULT_JSON,
) : IntentResolver {

    override suspend fun resolve(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        availableActions: List<ActionDescriptor>,
    ): ResolvedIntent {
        val actions = availableActions.ifEmpty { actionRegistry.getAllActions() }
        val engine = engineProvider()
            ?: return ResolvedIntent.Clarification("The assistant model isn't ready yet. Try again in a moment.")

        val schema = AgentSchemaBuilder.build(actions)
        val prompt = buildString {
            append(AgentSchemaBuilder.systemPrompt(actions))
            append("\n\n")
            conversationHistory.takeLast(HISTORY_TURNS).forEach { msg ->
                append(if (msg.isFromUser) "User: " else "Assistant: ").append(msg.text).append('\n')
            }
            append("User: ").append(userMessage).append("\nJSON:")
        }

        val raw = engine.generateConstrained(prompt, schema)
        val parsed = parse(raw)
        if (parsed != null) return toIntent(parsed, raw, actions)

        // The model didn't return parseable intent JSON — it most likely just answered in prose.
        // Surface that as a conversational reply instead of a canned "didn't catch that", so plain
        // chat works even when the model ignores the JSON instruction.
        val prose = raw.trim()
        return if (prose.isNotEmpty()) {
            ResolvedIntent.Conversation(prose)
        } else {
            ResolvedIntent.Clarification("Sorry, I didn't catch that — could you rephrase?")
        }
    }

    private fun parse(raw: String): RawIntent? {
        val jsonText = extractJsonObject(raw) ?: return null
        return runCatching { json.decodeFromString<RawIntent>(jsonText) }.getOrNull()
    }

    private fun toIntent(parsed: RawIntent, raw: String, actions: List<ActionDescriptor>): ResolvedIntent {
        when (parsed.intent?.lowercase()) {
            "conversation" -> return ResolvedIntent.Conversation(
                // Prefer the model's reply; if it left it blank, fall back to any prose it wrote
                // around the JSON, then a generic prompt — never a silent empty bubble.
                parsed.reply?.ifBlank { null } ?: stripJsonObject(raw).ifBlank { null } ?: "How can I help?",
            )
            "clarify" -> return ResolvedIntent.Clarification(parsed.reply?.ifBlank { null } ?: "Could you give me a bit more detail?")
        }

        // intent == "action" (or unspecified) → try to build a valid, registered action. If any
        // piece is missing/unknown, DON'T bounce the user with a canned "which action?" — a small
        // model frequently emits imperfect JSON for plain chat, so fall back to its own words as a
        // conversational reply (clarify only when there is genuinely nothing to say).
        val actionType = parsed.actionType?.let { runCatching { ActionType.valueOf(it) }.getOrNull() }
            ?: return conversationOrClarify(parsed, raw)
        val module = parsed.moduleName ?: return conversationOrClarify(parsed, raw)

        val descriptor = actions.firstOrNull { it.actionType == actionType && it.moduleName == module }
            ?: return conversationOrClarify(parsed, raw) // unregistered (actionType, module) pair

        val missing = descriptor.parameters
            .filter { it.required && parsed.params[it.name].isNullOrBlank() }
            .map { it.name }
        if (missing.isNotEmpty()) {
            return ResolvedIntent.Clarification("I need a bit more to do that — please provide: ${missing.joinToString(", ")}.")
        }

        return ResolvedIntent.Action(AgentAction(actionType, module, parsed.params))
    }

    /** No valid action could be built: prefer the model's reply/prose as chat, else a gentle clarify. */
    private fun conversationOrClarify(parsed: RawIntent, raw: String): ResolvedIntent {
        val reply = parsed.reply?.ifBlank { null } ?: stripJsonObject(raw).ifBlank { null }
        return if (reply != null) ResolvedIntent.Conversation(reply) else clarifyUnresolved()
    }

    private fun clarifyUnresolved(): ResolvedIntent =
        ResolvedIntent.Clarification("I'm not sure which action you mean — could you rephrase?")

    /** Return the model's text with the first JSON object removed — any conversational prose it wrote. */
    private fun stripJsonObject(raw: String): String {
        val obj = extractJsonObject(raw) ?: return raw.trim()
        return raw.replace(obj, "").trim()
    }

    /** Pull the first balanced `{...}` block out of the model's text (it may add prose around JSON). */
    private fun extractJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until raw.length) {
            when (raw[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(start, i + 1)
                }
            }
        }
        return null
    }

    @Serializable
    private data class RawIntent(
        val intent: String? = null,
        val actionType: String? = null,
        val moduleName: String? = null,
        val params: Map<String, String> = emptyMap(),
        /** Optional model text for conversation/clarify intents. */
        val reply: String? = null,
    )

    companion object {
        private const val HISTORY_TURNS = 6
        private val DEFAULT_JSON = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
