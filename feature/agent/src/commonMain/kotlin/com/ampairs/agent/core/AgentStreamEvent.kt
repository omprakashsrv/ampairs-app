package com.ampairs.agent.core

import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction

/**
 * One event in a streaming, tool-calling chat turn (the Gallery-style path on LiteRT-LM). The model
 * streams [TextDelta]s as it types; when it invokes a tool, the [com.ampairs.agent.llm.LlmEngine]
 * dispatches it through the registry and emits [ActionProposed] (money/destructive — awaits the
 * confirm bar before persisting, FR-006) or [ActionExecuted] (read/non-destructive — already ran).
 */
sealed interface AgentStreamEvent {
    /** An incremental chunk of the assistant's reply text, to append to the live bubble. */
    data class TextDelta(val text: String) : AgentStreamEvent

    /** A tool call that resolved a money/destructive action but did NOT persist — needs confirm. */
    data class ActionProposed(val action: AgentAction, val summary: String, val amount: Double?) : AgentStreamEvent

    /** A tool call that already executed (read/list/count/navigation/non-destructive). */
    data class ActionExecuted(val summary: String, val result: ActionResult, val action: AgentAction) : AgentStreamEvent

    /** The turn finished (model done streaming and any tool calls dispatched). */
    data object Done : AgentStreamEvent

    /** The turn failed (inference/tool error); the caller may fall back to the rule-based path. */
    data class Failed(val message: String) : AgentStreamEvent
}
