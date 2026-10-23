package com.example.mini_fadfed_android.ui.match.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.mini_fadfed_android.domain.model.MatchUiState
import com.example.mini_fadfed_android.ui.match.MatchViewModel

@Composable
fun MatchRootScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val matchState by viewModel.matchState.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val messages by viewModel.messages.collectAsState()

    // Handle back button behavior cleanly without creating orphan sessions
    BackHandler(enabled = matchState !is MatchUiState.Idle) {
        when (matchState) {
            is MatchUiState.Searching -> viewModel.cancelSearch()
            is MatchUiState.Matched -> viewModel.skipMatch()
            is MatchUiState.Connected -> viewModel.leaveConversation()
            MatchUiState.Idle -> {}
        }
    }

    when (val state = matchState) {
        is MatchUiState.Idle -> {
            HomeScreen(
                userName = viewModel.userName,
                userGender = viewModel.userGender,
                wallet = wallet,
                errorMessage = errorMessage,
                onDismissError = viewModel::clearError,
                onStartSearch = viewModel::startSearch,
                modifier = modifier
            )
        }
        is MatchUiState.Searching -> {
            SearchingScreen(
                filter = state.filter,
                onCancel = viewModel::cancelSearch,
                modifier = modifier
            )
        }
        is MatchUiState.Matched -> {
            MatchFoundScreen(
                peerName = state.peerName,
                peerGender = state.peerGender,
                youAccepted = state.youAccepted,
                peerAccepted = state.peerAccepted,
                onAccept = viewModel::acceptMatch,
                onSkip = viewModel::skipMatch,
                modifier = modifier
            )
        }
        is MatchUiState.Connected -> {
            ConversationScreen(
                peerName = state.peerName,
                chatId = state.chatId,
                messages = messages,
                onSendMessage = viewModel::sendMessage,
                onRetryMessage = viewModel::retryMessage,
                onMarkMessageVisible = viewModel::markMessageVisible,
                onSetVisible = viewModel::setConversationVisible,
                onEndChat = viewModel::leaveConversation,
                modifier = modifier
            )
        }
    }
}
