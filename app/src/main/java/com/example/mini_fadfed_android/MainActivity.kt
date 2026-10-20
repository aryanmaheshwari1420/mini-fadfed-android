package com.example.mini_fadfed_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.SharedPrefsIdentityStorage
import com.example.mini_fadfed_android.data.remote.FadfedRestClient
import com.example.mini_fadfed_android.data.repository.AuthRepository
import com.example.mini_fadfed_android.ui.signup.SignupScreen
import com.example.mini_fadfed_android.ui.signup.SignupViewModel
import com.example.mini_fadfed_android.ui.theme.MinifadfedandroidTheme

class MainActivity : ComponentActivity() {

    private val identityStorage by lazy { SharedPrefsIdentityStorage(applicationContext) }
    private val identityManager by lazy { IdentityManager(identityStorage) }
    private val restClient by lazy { FadfedRestClient(identityManager) }
    private val authRepository by lazy { AuthRepository(identityManager, restClient) }

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
                        }
                    )
                } else {
                    RegisteredPlaceholderScreen(
                        name = authRepository.getUserName() ?: "User",
                        gender = if (authRepository.getUserGender() == "f") "Female" else "Male",
                        devId = identityManager.devId
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisteredPlaceholderScreen(
    name: String,
    gender: String,
    devId: String,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome, $name!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Gender: $gender",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device ID: $devId",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Profile registered successfully. Ready for Milestone 2 & 3.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}