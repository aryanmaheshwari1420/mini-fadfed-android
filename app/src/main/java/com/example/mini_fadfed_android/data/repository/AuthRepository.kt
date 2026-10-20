package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.data.remote.ApiResult
import com.example.mini_fadfed_android.data.remote.FadfedRestClient

class AuthRepository(
    private val identityManager: IdentityManager,
    private val restClient: FadfedRestClient
) {
    fun isRegistered(): Boolean = identityManager.isRegistered()

    fun getUserName(): String? = identityManager.getUserName()

    fun getUserGender(): String? = identityManager.getUserGender()

    suspend fun signup(name: String, gender: String): Result<Unit> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty."))
        }
        if (gender != "m" && gender != "f") {
            return Result.failure(IllegalArgumentException("Please select a valid gender."))
        }

        // Step 1: PUT /api/@fadfedx/auth (Create anonymous account)
        when (val authResult = restClient.createAnonymousAccount()) {
            is ApiResult.Error -> return Result.failure(Exception(authResult.message))
            is ApiResult.Success -> {
                // Anonymous account created successfully, proceed to set profile
            }
        }

        // Step 2: PUT /api/@fadfedx/user (Set name and gender)
        when (val profileResult = restClient.setProfile(trimmedName, gender)) {
            is ApiResult.Error -> return Result.failure(Exception(profileResult.message))
            is ApiResult.Success -> {
                identityManager.saveProfile(trimmedName, gender)
                return Result.success(Unit)
            }
        }
    }
}
