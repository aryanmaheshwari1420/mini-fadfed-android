package com.example.mini_fadfed_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.SharedPrefsIdentityStorage
import com.example.mini_fadfed_android.data.remote.FadfedRestClient
import com.example.mini_fadfed_android.data.remote.ws.FadfedWebSocketClient
import com.example.mini_fadfed_android.data.repository.AuthRepository
import com.example.mini_fadfed_android.data.repository.ConversationManager
import com.example.mini_fadfed_android.data.repository.MatchSessionManager
import com.example.mini_fadfed_android.ui.match.MatchViewModel
import com.example.mini_fadfed_android.ui.match.screens.MatchRootScreen
import com.example.mini_fadfed_android.ui.signup.SignupScreen
import com.example.mini_fadfed_android.ui.signup.SignupViewModel
import com.example.mini_fadfed_android.ui.theme.MinifadfedandroidTheme

class MainActivity : ComponentActivity() {

    private val identityStorage by lazy { SharedPrefsIdentityStorage(applicationContext) }
    private val identityManager by lazy { IdentityManager(identityStorage) }
    private val restClient by lazy { FadfedRestClient(identityManager) }
    private val authRepository by lazy { AuthRepository(identityManager, restClient) }
    private val wsClient by lazy { FadfedWebSocketClient(identityManager) }
    private val conversationManager by lazy { ConversationManager(wsClient, identityManager, lifecycleScope) }
    private val matchSessionManager by lazy { MatchSessionManager(wsClient, lifecycleScope, conversationManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinifadfedandroidTheme {
                var isRegistered by remember { mutableStateOf(authRepository.isRegistered()) }

                if (!isRegistered) {
                    val signupViewModel: SignupViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return SignupViewModel(authRepository) as T
                            }
                        }
                    )
                    SignupScreen(
                        viewModel = signupViewModel,
                        onSignupSuccess = {
                            isRegistered = true
                            matchSessionManager.ensureConnected()
                        }
                    )
                } else {
                    val matchViewModel: MatchViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return MatchViewModel(matchSessionManager, conversationManager, identityManager) as T
                            }
                        }
                    )
                    MatchRootScreen(viewModel = matchViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        conversationManager.cleanup()
        matchSessionManager.cleanup()
        wsClient.disconnect()
    }
}