package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.data.remote.ws.FadfedWebSocketClient
import com.example.mini_fadfed_android.data.remote.ws.WsConnectionState
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.example.mini_fadfed_android.domain.model.ChatMessage
import com.example.mini_fadfed_android.domain.model.MessageStatus
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ConversationManager(
    private val wsClient: FadfedWebSocketClient,
    private val identityManager: IdentityManager,
    private val scope: CoroutineScope,
    private val eventScope: CoroutineScope = scope
) {
    companion object {
        const val ACK_TIMEOUT_MS = 5000L
        const val MAX_RETRIES = 3
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    var activeChatId: String? = null
        private set
    var activePeerUdid: String? = null
        private set
    var activePeerName: String? = null
        private set

    private var isConversationVisible: Boolean = true
    private val receivedMessageIds = mutableSetOf<String>()
    private val readAckSentIds = mutableSetOf<String>()
    private val retryJobs = ConcurrentHashMap<String, Job>()

    private val eventCollectionJob = eventScope.launch {
        wsClient.incomingEvents.collect { event ->
            handleInboundEvent(event)
        }
    }

    fun startConversation(chatId: String, peerUdid: String, peerName: String) {
        if (activeChatId == chatId) return

        endConversation()

        activeChatId = chatId
        activePeerUdid = peerUdid
        activePeerName = peerName
        _messages.value = emptyList()
        receivedMessageIds.clear()
        readAckSentIds.clear()
    }

    fun endConversation() {
        activeChatId = null
        activePeerUdid = null
        activePeerName = null
        cancelAllRetryJobs()
    }

    fun cleanup() {
        endConversation()
        eventCollectionJob.cancel()
    }

    fun setConversationVisible(visible: Boolean) {
        isConversationVisible = visible
        if (visible) {
            // Send read acknowledgements for any visible incoming messages
            val unreadIncoming = _messages.value.filter { !it.isFromMe && !readAckSentIds.contains(it.id) }
            unreadIncoming.forEach { msg ->
                markMessageVisible(msg.id)
            }
        }
    }

    fun markMessageVisible(messageId: String) {
        val chatId = activeChatId ?: return
        if (readAckSentIds.add(messageId)) {
            sendAck(messageId, "read")
        }
    }

    /**
     * Sends an outgoing message to the peer.
     * Generates a unique message ID and sends the required payload:
     * ["send", {"id": "...", "content": "...", "chatId": "...", "to": "...", "ts": ...}]
     */
    fun sendMessage(content: String): Boolean {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return false

        val chatId = activeChatId ?: return false
        val peerUdid = activePeerUdid ?: return false

        val messageId = UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()

        val msg = ChatMessage(
            id = messageId,
            senderUdid = identityManager.devId,
            content = trimmed,
            timestamp = ts,
            isFromMe = true,
            status = MessageStatus.SENDING,
            retryCount = 0
        )

        _messages.value = _messages.value + msg

        val payload = createSendPayload(msg.id, msg.content, chatId, peerUdid, msg.timestamp)
        val written = wsClient.send("send", payload)

        if (written) {
            updateMessage(messageId) { it.copy(status = MessageStatus.SENT) }
            startRetryJob(msg)
        } else {
            // Socket not yet ready or connecting: remain in SENDING and start retry timer
            startRetryJob(msg)
        }

        return true
    }

    /**
     * Manually retries a failed message.
     */
    fun retryMessage(messageId: String) {
        val msg = _messages.value.find { it.id == messageId && it.isFromMe } ?: return
        val chatId = activeChatId ?: return
        val peerUdid = activePeerUdid ?: return

        updateMessage(messageId) {
            it.copy(status = MessageStatus.SENDING, retryCount = 0)
        }

        val payload = createSendPayload(msg.id, msg.content, chatId, peerUdid, msg.timestamp)
        val written = wsClient.send("send", payload)

        if (written) {
            updateMessage(messageId) { it.copy(status = MessageStatus.SENT) }
        }

        startRetryJob(msg)
    }

    private fun startRetryJob(msg: ChatMessage) {
        retryJobs[msg.id]?.cancel()

        val job = scope.launch {
            for (attempt in 1..MAX_RETRIES) {
                delay(ACK_TIMEOUT_MS)

                val current = _messages.value.find { it.id == msg.id } ?: break
                if (current.status == MessageStatus.READ || current.status == MessageStatus.FAILED) {
                    break
                }
                val chatId = activeChatId ?: break
                val peerUdid = activePeerUdid ?: break

                val payload = createSendPayload(msg.id, msg.content, chatId, peerUdid, msg.timestamp)
                val written = wsClient.send("send", payload)

                updateMessage(msg.id) {
                    it.copy(
                        retryCount = attempt,
                        status = if (written) MessageStatus.SENT else MessageStatus.SENDING
                    )
                }
            }

            // Final 5s timeout after 3rd retry
            delay(ACK_TIMEOUT_MS)

            val finalMsg = _messages.value.find { it.id == msg.id }
            if (finalMsg != null && finalMsg.status != MessageStatus.READ) {
                updateMessage(msg.id) { it.copy(status = MessageStatus.FAILED) }
            }

            retryJobs.remove(msg.id)
        }

        retryJobs[msg.id] = job
    }

    private fun handleInboundEvent(event: FadfedWsEvent) {
        when (event) {
            is FadfedWsEvent.Message -> {
                handleIncomingMessage(event)
            }
            is FadfedWsEvent.Ack -> {
                handleIncomingAck(event)
            }
            is FadfedWsEvent.Leave -> {
                if (event.friends == true) return
                if (event.chatId == null || event.chatId == activeChatId) {
                    endConversation()
                }
            }
            else -> {}
        }
    }

    private fun handleIncomingMessage(event: FadfedWsEvent.Message) {
        val currentChat = activeChatId ?: return
        // Stale conversation protection: discard messages from old or mismatched chats
        if (event.chatId != null && event.chatId != currentChat) return

        val rawMsgId = event.id ?: return
        val senderUdid = event.by ?: return
        val text = event.content ?: return
        val ts = event.ts ?: System.currentTimeMillis()

        // Exact server-rewritten ID preserved:
        // When acknowledging, ref must be this received id verbatim (§8.6)
        sendAck(rawMsgId, "received")

        // Deduplication: prevent duplicate messages caused by peer retries
        if (receivedMessageIds.contains(rawMsgId)) {
            if (isConversationVisible && readAckSentIds.contains(rawMsgId)) {
                sendAck(rawMsgId, "read")
            }
            return
        }

        receivedMessageIds.add(rawMsgId)

        val incomingMessage = ChatMessage(
            id = rawMsgId,
            senderUdid = senderUdid,
            content = text,
            timestamp = ts,
            isFromMe = false,
            status = MessageStatus.SENT
        )

        // Maintain ascending timestamp order for out-of-order deliveries
        _messages.value = (_messages.value + incomingMessage).sortedBy { it.timestamp }

        // Acknowledge read only when rendered/visible
        if (isConversationVisible) {
            markMessageVisible(rawMsgId)
        }
    }

    private fun handleIncomingAck(event: FadfedWsEvent.Ack) {
        if (activeChatId == null) return

        val ref = event.ref ?: return
        val status = event.status ?: return

        if (status == "read") {
            // Find our outgoing message matching ref
            val matchedMsg = _messages.value.find { it.id == ref && it.isFromMe }
            if (matchedMsg != null) {
                // Cancel pending retry job immediately on terminal state
                retryJobs.remove(ref)?.cancel()
                updateMessage(ref) { it.copy(status = MessageStatus.READ) }
            }
        }
    }

    private fun sendAck(refId: String, status: String) {
        val ackPayload = JsonObject().apply {
            addProperty("ref", refId)
            addProperty("id", UUID.randomUUID().toString())
            addProperty("status", status)
        }
        wsClient.send("ack", ackPayload)
    }

    private fun createSendPayload(
        id: String,
        content: String,
        chatId: String,
        to: String,
        ts: Long
    ): JsonObject {
        return JsonObject().apply {
            addProperty("id", id)
            addProperty("content", content)
            addProperty("chatId", chatId)
            addProperty("to", to)
            addProperty("ts", ts)
        }
    }

    private fun updateMessage(id: String, transform: (ChatMessage) -> ChatMessage) {
        _messages.value = _messages.value.map {
            if (it.id == id) transform(it) else it
        }
    }

    private fun cancelAllRetryJobs() {
        retryJobs.values.forEach { it.cancel() }
        retryJobs.clear()
    }
}
