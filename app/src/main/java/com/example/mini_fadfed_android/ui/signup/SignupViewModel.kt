package com.example.mini_fadfed_android.ui.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mini_fadfed_android.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignupUiState(
    val name: String = "",
    val gender: String = "m",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistered: Boolean = false
)

class SignupViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SignupUiState(
            isRegistered = authRepository.isRegistered(),
            name = authRepository.getUserName().orEmpty(),
            gender = authRepository.getUserGender() ?: "m"
        )
    )
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName, errorMessage = null) }
    }

    fun onGenderChanged(newGender: String) {
        if (newGender == "m" || newGender == "f") {
            _uiState.update { it.copy(gender = newGender, errorMessage = null) }
        }
    }

    fun onContinueClicked() {
        val current = _uiState.value
        if (current.isLoading) return

        val trimmedName = current.name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please enter your name.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signup(trimmedName, current.gender)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistered = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Registration failed. Please try again."
                        )
                    }
                }
            )
        }
    }
}
