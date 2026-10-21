package com.example.mini_fadfed_android.data.remote.ws

sealed class WsConnectionState {
    object Disconnected : WsConnectionState()
    object Connecting : WsConnectionState()
    object Connected : WsConnectionState() // Socket opened, waiting for session ready
    object Ready : WsConnectionState()     // session:ready received and accept gate set
    object Replaced : WsConnectionState()  // Terminal state: account connected elsewhere
    data class Error(val message: String, val code: Int? = null, val canRetry: Boolean = true) : WsConnectionState()
}
