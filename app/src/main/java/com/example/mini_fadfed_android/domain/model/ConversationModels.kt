package com.example.mini_fadfed_android.domain.model

enum class MessageStatus {
    SENDING,  // Created locally, not yet written to open socket (clock icon)
    SENT,     // Written to open socket (one tick)
    READ,     // Peer's read acknowledgement received (two ticks)
    FAILED    // Delivery could not be confirmed after timeout and retries
}

data class ChatMessage(
    val id: String,                 // Unique message ID (UUID for outgoing, raw server-rewritten ID for incoming)
    val senderUdid: String,         // UDID of sender
    val content: String,            // Text message content
    val timestamp: Long,            // Epoch milliseconds
    val isFromMe: Boolean,          // True if outgoing message, false if incoming from peer
    val status: MessageStatus = MessageStatus.SENT,
    val retryCount: Int = 0
)
