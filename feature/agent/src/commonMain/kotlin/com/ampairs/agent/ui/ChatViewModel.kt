package com.ampairs.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.agent.core.ActionResultSummary
import com.ampairs.agent.core.AgentOrchestrator
import com.ampairs.agent.core.ChatMessage
import com.ampairs.agent.core.AgentResponse
import com.ampairs.common.agent.ActionResult
import com.ampairs.common.agent.AgentAction
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val isListening: Boolean = false,
    val isOnline: Boolean = true,
    val error: String? = null,
    /** A money/destructive action awaiting explicit confirm/cancel before it persists (FR-006). */
    val pendingConfirmation: AgentAction? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class ChatViewModel(
    private val orchestrator: AgentOrchestrator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = text,
            isFromUser = true,
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isProcessing = true,
            )
        }

        viewModelScope.launch {
            try {
                appendAgentResponse(
                    orchestrator.processMessage(
                        userMessage = text,
                        conversationHistory = _uiState.value.messages,
                        isOnline = _uiState.value.isOnline,
                    )
                )
            } catch (e: Exception) {
                appendError("Something went wrong: ${e.message}")
            }
        }
    }

    /** User accepted a pending money/destructive action — re-dispatch it so the handler persists. */
    fun confirmPending() {
        val pending = _uiState.value.pendingConfirmation ?: return
        _uiState.update { it.copy(pendingConfirmation = null, isProcessing = true) }
        viewModelScope.launch {
            try {
                appendAgentResponse(orchestrator.confirmAction(pending))
            } catch (e: Exception) {
                appendError("Something went wrong: ${e.message}")
            }
        }
    }

    /** User declined a pending action — discard it; nothing was persisted. */
    fun cancelPending() {
        if (_uiState.value.pendingConfirmation == null) return
        val message = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = "Okay, cancelled. Nothing was saved.",
            isFromUser = false,
        )
        _uiState.update { it.copy(pendingConfirmation = null, messages = it.messages + message) }
    }

    private fun appendAgentResponse(response: AgentResponse) {
        val actionSummary = response.resolvedAction?.let { action ->
            val result = response.actionResult
            ActionResultSummary(
                actionType = action.actionType.name,
                moduleName = action.moduleName,
                success = result is ActionResult.Success,
                navigationRouteDescription = (result as? ActionResult.Success)?.navigationTarget?.routeDescription,
                navigationRouteData = (result as? ActionResult.Success)?.navigationTarget?.routeData,
            )
        }
        val agentMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = response.text,
            isFromUser = false,
            actionResult = actionSummary,
            isError = response.actionResult is ActionResult.Error,
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages + agentMessage,
                isProcessing = false,
                pendingConfirmation = response.pendingConfirmation,
            )
        }
    }

    private fun appendError(text: String) {
        val errorMessage = ChatMessage(
            id = UidGenerator.generateUid("MSG"),
            text = text,
            isFromUser = false,
            isError = true,
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + errorMessage, isProcessing = false)
        }
    }

    fun onVoiceResult(transcribedText: String) {
        _uiState.update { it.copy(inputText = transcribedText, isListening = false) }
    }

    fun setListening(listening: Boolean) {
        _uiState.update { it.copy(isListening = listening) }
    }

    fun setOnlineStatus(online: Boolean) {
        _uiState.update { it.copy(isOnline = online) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
}
