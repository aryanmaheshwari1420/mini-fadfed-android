package com.example.mini_fadfed_android.ui.match

import androidx.lifecycle.ViewModel
import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.data.repository.ConversationManager
import com.example.mini_fadfed_android.data.repository.MatchSessionManager
import com.example.mini_fadfed_android.domain.model.ChatMessage
import com.example.mini_fadfed_android.domain.model.MatchFilter
import com.example.mini_fadfed_android.domain.model.MatchUiState
import com.example.mini_fadfed_android.domain.model.UserWallet
import kotlinx.coroutines.flow.StateFlow

class MatchViewModel(
    private val matchSessionManager: MatchSessionManager,
    private val conversationManager: ConversationManager,
    private val identityManager: IdentityManager
) : ViewModel() {

    val matchState: StateFlow<MatchUiState> = matchSessionManager.matchState
    val wallet: StateFlow<UserWallet> = matchSessionManager.wallet
    val errorMessage: StateFlow<String?> = matchSessionManager.errorMessage
    val messages: StateFlow<List<ChatMessage>> = conversationManager.messages

    val userName: String = identityManager.getUserName() ?: "User"
    val userGender: String = identityManager.getUserGender() ?: "m"
    val devId: String = identityManager.devId

    fun startSearch(filter: MatchFilter) {
        matchSessionManager.startSearch(filter)
    }

    fun cancelSearch() {
        matchSessionManager.cancelSearch()
    }

    fun acceptMatch() {
        matchSessionManager.acceptMatch()
    }

    fun skipMatch() {
        matchSessionManager.skipMatch()
    }

    fun leaveConversation() {
        matchSessionManager.leaveConversation()
    }

    fun clearError() {
        matchSessionManager.clearError()
    }

    fun sendMessage(content: String): Boolean {
        return conversationManager.sendMessage(content)
    }

    fun retryMessage(messageId: String) {
        conversationManager.retryMessage(messageId)
    }

    fun markMessageVisible(messageId: String) {
        conversationManager.markMessageVisible(messageId)
    }

    fun setConversationVisible(visible: Boolean) {
        conversationManager.setConversationVisible(visible)
    }
}
