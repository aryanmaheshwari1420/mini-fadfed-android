package com.example.mini_fadfed_android.domain.model

enum class MatchFilter(val label: String, val genderParam: String?) {
    BOTH("Both (Free)", null),
    MALE("Male", "m"),
    FEMALE("Female", "f")
}

sealed class MatchUiState {
    object Idle : MatchUiState()

    data class Searching(
        val filter: MatchFilter,
        val requestId: String
    ) : MatchUiState()

    data class Matched(
        val chatId: String,
        val peerUdid: String,
        val peerName: String = "Stranger",
        val peerGender: String? = null,
        val youAccepted: Boolean = false,
        val peerAccepted: Boolean = false
    ) : MatchUiState()

    data class Connected(
        val chatId: String,
        val peerUdid: String,
        val peerName: String
    ) : MatchUiState()
}

data class UserWallet(
    val coins: Int = 0,
    val genderCost: Int = 0,
    val countryCost: Int = 0
)
