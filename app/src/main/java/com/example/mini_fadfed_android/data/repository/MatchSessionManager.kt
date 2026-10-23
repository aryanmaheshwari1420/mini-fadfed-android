package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.data.remote.ws.FadfedWebSocketClient
import com.example.mini_fadfed_android.data.remote.ws.WsConnectionState
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.example.mini_fadfed_android.domain.model.MatchFilter
import com.example.mini_fadfed_android.domain.model.MatchUiState
import com.example.mini_fadfed_android.domain.model.UserWallet
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MatchSessionManager(
    private val wsClient: FadfedWebSocketClient,
    private val scope: CoroutineScope,
    private val conversationManager: ConversationManager? = null
) {
    private val _matchState = MutableStateFlow<MatchUiState>(MatchUiState.Idle)
    val matchState: StateFlow<MatchUiState> = _matchState.asStateFlow()

    private val _wallet = MutableStateFlow(UserWallet())
    val wallet: StateFlow<UserWallet> = _wallet.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastFilter: MatchFilter = MatchFilter.BOTH
    private var currentRequestId: String? = null

    private val eventCollectionJob = scope.launch {
        wsClient.incomingEvents.collect { event ->
            handleInboundEvent(event)
        }
    }

    fun cleanup() {
        eventCollectionJob.cancel()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Starts matchmaking with the chosen filter.
     * "Both is free. Male and Female are paid filters..."
     * Server keys pricing off presence of 'gender' key:
     * Omit gender key completely for Both!
     */
    fun startSearch(filter: MatchFilter = MatchFilter.BOTH) {
        val current = _matchState.value
        if (current is MatchUiState.Searching || current is MatchUiState.Matched || current is MatchUiState.Connected) {
            // Prevent duplicate search requests caused by repeated taps
            return
        }

        lastFilter = filter
        _errorMessage.value = null

        val requestId = UUID.randomUUID().toString()
        currentRequestId = requestId

        val payload = JsonObject().apply {
            addProperty("id", requestId)
            addProperty("algo", "R")
            if (filter.genderParam != null) {
                addProperty("gender", filter.genderParam)
            }
        }

        _matchState.value = MatchUiState.Searching(filter = filter, requestId = requestId)

        // Ensure WebSocket is connected
        if (wsClient.connectionState.value is WsConnectionState.Disconnected ||
            wsClient.connectionState.value is WsConnectionState.Error
        ) {
            wsClient.connect()
        }

        wsClient.send("match", payload)
    }

    /**
     * Cancels an ongoing search.
     * Assignment rule: "send leave with no chatId, or an empty object."
     */
    fun cancelSearch() {
        val current = _matchState.value
        if (current is MatchUiState.Searching) {
            wsClient.send("leave", JsonObject())
            _matchState.value = MatchUiState.Idle
            currentRequestId = null
        }
    }

    /**
     * Sends local acceptance.
     * Assignment rule: "Enter the conversation only when both conditions hold:
     * you sent accept, and you received accepted: true."
     */
    fun acceptMatch() {
        val current = _matchState.value
        if (current !is MatchUiState.Matched || current.youAccepted) {
            return
        }

        val acceptPayload = JsonObject().apply {
            addProperty("chatId", current.chatId)
        }
        wsClient.send("accept", acceptPayload)

        if (current.peerAccepted) {
            // Both sides have accepted! Transition to conversation
            _matchState.value = MatchUiState.Connected(
                chatId = current.chatId,
                peerUdid = current.peerUdid,
                peerName = current.peerName
            )
            conversationManager?.startConversation(current.chatId, current.peerUdid, current.peerName)
        } else {
            _matchState.value = current.copy(youAccepted = true)
        }
    }

    /**
     * Skips current match.
     * Assignment rule: "Skip sends leave and immediately starts a new search."
     */
    fun skipMatch() {
        val current = _matchState.value
        if (current !is MatchUiState.Matched) {
            return
        }

        val leavePayload = JsonObject().apply {
            addProperty("chatId", current.chatId)
        }
        wsClient.send("leave", leavePayload)

        // Reset and immediately restart search
        _matchState.value = MatchUiState.Idle
        startSearch(lastFilter)
    }

    /**
     * User leaves active conversation.
     */
    fun leaveConversation() {
        val current = _matchState.value
        if (current is MatchUiState.Connected) {
            val leavePayload = JsonObject().apply {
                addProperty("chatId", current.chatId)
            }
            wsClient.send("leave", leavePayload)
            conversationManager?.endConversation()
            _matchState.value = MatchUiState.Idle
        }
    }

    private fun handleInboundEvent(event: FadfedWsEvent) {
        when (event) {
            is FadfedWsEvent.Status -> {
                _wallet.value = UserWallet(
                    coins = event.coins,
                    genderCost = event.genderCost,
                    countryCost = event.countryCost
                )
            }
            is FadfedWsEvent.Matched -> {
                val chatId = event.chatId ?: return
                val current = _matchState.value

                if (current is MatchUiState.Matched && current.chatId == chatId) {
                    // Second matched event for same chat carrying peer's acceptance status
                    val peerNowAccepted = event.accepted
                    if (peerNowAccepted && current.youAccepted) {
                        _matchState.value = MatchUiState.Connected(
                            chatId = chatId,
                            peerUdid = current.peerUdid,
                            peerName = current.peerName
                        )
                        conversationManager?.startConversation(chatId, current.peerUdid, current.peerName)
                    } else {
                        _matchState.value = current.copy(peerAccepted = peerNowAccepted)
                    }
                } else if (current is MatchUiState.Searching) {
                    // First matched event arrives!
                    val peerAlreadyAccepted = event.accepted
                    _matchState.value = MatchUiState.Matched(
                        chatId = chatId,
                        peerUdid = event.udid.orEmpty(),
                        peerName = "Stranger",
                        youAccepted = false,
                        peerAccepted = peerAlreadyAccepted
                    )
                }
            }
            is FadfedWsEvent.Sync -> {
                if (event.dataset == "\$profile" && event.item != null) {
                    val peerUdid = event.item
                    val name = event.data?.get("name")?.takeIf { !it.isJsonNull }?.asString
                    val gender = event.data?.get("gender")?.takeIf { !it.isJsonNull }?.asString

                    val current = _matchState.value
                    if (current is MatchUiState.Matched && current.peerUdid == peerUdid) {
                        _matchState.value = current.copy(
                            peerName = name ?: current.peerName,
                            peerGender = gender ?: current.peerGender
                        )
                    } else if (current is MatchUiState.Connected && current.peerUdid == peerUdid) {
                        _matchState.value = current.copy(
                            peerName = name ?: current.peerName
                        )
                    }
                }
            }
            is FadfedWsEvent.Leave -> {
                if (event.friends == true) {
                    // "A leave carrying 'friends': true means chat transitioned to friend mode. Ignore those." (§8.5)
                    return
                }

                val current = _matchState.value
                if (current is MatchUiState.Matched && (event.chatId == null || event.chatId == current.chatId)) {
                    // "If the peer skips first you receive leave — go back to searching automatically." (§2.4)
                    _matchState.value = MatchUiState.Idle
                    startSearch(lastFilter)
                } else if (current is MatchUiState.Connected && (event.chatId == null || event.chatId == current.chatId)) {
                    conversationManager?.endConversation()
                    _matchState.value = MatchUiState.Idle
                    _errorMessage.value = "Peer left the conversation."
                }
            }
            is FadfedWsEvent.Error -> {
                handleMatchError(event.code)
            }
            else -> {}
        }
    }

    private fun handleMatchError(code: String?) {
        val current = _matchState.value
        if (current is MatchUiState.Searching) {
            _matchState.value = MatchUiState.Idle
            currentRequestId = null
        }

        _errorMessage.value = when (code) {
            "insufficient_coins" -> "Insufficient coins for gender filter. Use 'Both' for a free search."
            "blocked" -> "Your account is blocked."
            "invalid_level" -> "Malformed level in match request."
            "invalid_age_range" -> "Invalid age range configuration."
            "unsupported_version" -> "App version unsupported. Please update."
            "temporary_error" -> "Transient server error. Please retry."
            else -> "Matchmaking error: ${code ?: "Unknown"}"
        }
    }
}
