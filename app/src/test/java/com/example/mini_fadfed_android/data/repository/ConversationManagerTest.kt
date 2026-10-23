package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import com.example.mini_fadfed_android.data.remote.ws.FadfedWebSocketClient
import com.example.mini_fadfed_android.data.remote.ws.WsConnectionState
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.example.mini_fadfed_android.domain.model.MessageStatus
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationManagerTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeWsClient: FakeWebSocketClient
    private lateinit var identityManager: IdentityManager
    private lateinit var unconfinedScope: CoroutineScope

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        unconfinedScope = CoroutineScope(Dispatchers.Unconfined + kotlinx.coroutines.Job())

        val fakeStorage = object : IdentityStorage {
            override fun getDevId(): String = "my-devid-123"
            override fun saveDevId(devId: String) {}
            override fun isRegistered(): Boolean = true
            override fun setRegistered(registered: Boolean) {}
            override fun getUserName(): String = "TestUser"
            override fun setUserName(name: String) {}
            override fun getUserGender(): String = "m"
            override fun setUserGender(gender: String) {}
            override fun clear() {}
        }
        identityManager = IdentityManager(fakeStorage, initialSessionId = "session-test")
        fakeWsClient = FakeWebSocketClient(identityManager)
        fakeWsClient.setReady()
    }

    @After
    fun tearDown() {
        unconfinedScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun outgoingMessage_containsRequiredFieldsAndSerializesCorrectly() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        val sent = conversationManager.sendMessage("Hello there!")
        assertTrue(sent)

        val messages = conversationManager.messages.value
        assertEquals(1, messages.size)
        val msg = messages[0]
        assertEquals("Hello there!", msg.content)
        assertTrue(msg.isFromMe)
        assertEquals(MessageStatus.SENT, msg.status)

        // Verify WebSocket frame
        val frame = fakeWsClient.sentFrames.find { it.first == "send" }
        assertNotNull(frame)
        val payload = frame!!.second
        assertEquals(msg.id, payload.get("id").asString)
        assertEquals("Hello there!", payload.get("content").asString)
        assertEquals("chat-100", payload.get("chatId").asString)
        assertEquals("peer-udid-1", payload.get("to").asString)
        assertTrue(payload.get("ts").asLong > 0)
    }

    @Test
    fun sendingToSentToRead_transitionsProperly() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        conversationManager.sendMessage("Checking ticks")

        var msg = conversationManager.messages.value[0]
        assertEquals(MessageStatus.SENT, msg.status)

        // Peer sends ack with status: "read"
        fakeWsClient.emitEvent(
            FadfedWsEvent.Ack(
                ref = msg.id,
                id = "ack-id-1",
                status = "read",
                by = "peer-udid-1",
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        msg = conversationManager.messages.value[0]
        assertEquals("Message must transition to READ upon receiving peer's read ack", MessageStatus.READ, msg.status)
    }

    @Test
    fun incomingMessage_exactServerRewrittenIdInAcks() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")

        // Server sends incoming message with rewritten ID: "<by>:<originalId>"
        val serverRewrittenId = "peer-udid-1:original-msg-777"
        fakeWsClient.emitEvent(
            FadfedWsEvent.Message(
                id = serverRewrittenId,
                content = "Hello from peer",
                by = "peer-udid-1",
                chatId = "chat-100",
                ts = 1690000000000L,
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        val messages = conversationManager.messages.value
        assertEquals(1, messages.size)
        assertEquals("Hello from peer", messages[0].content)
        assertFalse(messages[0].isFromMe)
        assertEquals(serverRewrittenId, messages[0].id)

        // Verify received ack was sent with exact rewritten ID
        val receivedAck = fakeWsClient.sentFrames.find {
            it.first == "ack" && it.second.get("status")?.asString == "received"
        }
        assertNotNull("Must send 'received' ack", receivedAck)
        assertEquals(
            "Ack ref must be the exact server-rewritten ID verbatim (§8.6)",
            serverRewrittenId,
            receivedAck!!.second.get("ref").asString
        )
    }

    @Test
    fun readAcknowledgement_sentOnlyWhenMessageBecomesVisible() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        // User currently not looking at screen (e.g. backgrounded)
        conversationManager.setConversationVisible(false)

        val msgId = "peer-udid-1:msg-hidden"
        fakeWsClient.emitEvent(
            FadfedWsEvent.Message(
                id = msgId,
                content = "Secret message",
                by = "peer-udid-1",
                chatId = "chat-100",
                ts = 1690000000000L,
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        // 'received' ack should be sent, but NOT 'read' ack
        val receivedAck = fakeWsClient.sentFrames.find {
            it.first == "ack" && it.second.get("ref")?.asString == msgId && it.second.get("status")?.asString == "received"
        }
        assertNotNull(receivedAck)

        val readAckBeforeVisible = fakeWsClient.sentFrames.find {
            it.first == "ack" && it.second.get("ref")?.asString == msgId && it.second.get("status")?.asString == "read"
        }
        assertNull("Read ack must NOT be sent before message becomes visible", readAckBeforeVisible)

        // Now message becomes visible
        conversationManager.markMessageVisible(msgId)
        advanceUntilIdle()

        val readAckAfterVisible = fakeWsClient.sentFrames.find {
            it.first == "ack" && it.second.get("ref")?.asString == msgId && it.second.get("status")?.asString == "read"
        }
        assertNotNull("Read ack must be sent when message becomes visible", readAckAfterVisible)
        assertEquals(msgId, readAckAfterVisible!!.second.get("ref").asString)
    }

    @Test
    fun deduplication_duplicateIncomingMessagesIgnored() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")

        val duplicateId = "peer-udid-1:dup-123"
        val messageEvent = FadfedWsEvent.Message(
            id = duplicateId,
            content = "Duplicate content",
            by = "peer-udid-1",
            chatId = "chat-100",
            ts = 1690000000000L,
            raw = JsonObject()
        )

        // Emit first time
        fakeWsClient.emitEvent(messageEvent)
        advanceUntilIdle()
        assertEquals(1, conversationManager.messages.value.size)

        // Emit duplicate (e.g. peer retried)
        fakeWsClient.emitEvent(messageEvent)
        advanceUntilIdle()
        assertEquals("Duplicate message with same ID must not create duplicate bubble", 1, conversationManager.messages.value.size)
    }

    @Test
    fun outOfOrder_incomingMessagesSortedByTimestamp() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")

        // First event arrives with later timestamp (2000)
        fakeWsClient.emitEvent(
            FadfedWsEvent.Message(
                id = "peer-udid-1:msg-2",
                content = "Second message",
                by = "peer-udid-1",
                chatId = "chat-100",
                ts = 2000L,
                raw = JsonObject()
            )
        )
        // Second event arrives with earlier timestamp (1000)
        fakeWsClient.emitEvent(
            FadfedWsEvent.Message(
                id = "peer-udid-1:msg-1",
                content = "First message",
                by = "peer-udid-1",
                chatId = "chat-100",
                ts = 1000L,
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        val messages = conversationManager.messages.value
        assertEquals(2, messages.size)
        assertEquals("First message", messages[0].content)
        assertEquals("Second message", messages[1].content)
    }

    @Test
    fun timeoutAndBoundedRetries_retriesThreeTimesThenFails() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        conversationManager.sendMessage("Will time out")

        // Initial send: 1 frame sent
        val sendsInitial = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals(1, sendsInitial)
        assertEquals(0, conversationManager.messages.value[0].retryCount)
        assertEquals(MessageStatus.SENT, conversationManager.messages.value[0].status)

        // 1st retry after 5s
        advanceTimeBy(5001L)
        val sendsAfter1 = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals("Retry 1 should send a frame", 2, sendsAfter1)
        assertEquals(1, conversationManager.messages.value[0].retryCount)

        // 2nd retry after 10s
        advanceTimeBy(5001L)
        val sendsAfter2 = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals("Retry 2 should send a frame", 3, sendsAfter2)
        assertEquals(2, conversationManager.messages.value[0].retryCount)

        // 3rd retry after 15s (maximum 3 bounded retries)
        advanceTimeBy(5001L)
        val sendsAfter3 = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals("Retry 3 should send a frame", 4, sendsAfter3)
        assertEquals(3, conversationManager.messages.value[0].retryCount)

        // Final 5s timeout expires without ack -> transitions to FAILED
        advanceTimeBy(5001L)
        assertEquals(MessageStatus.FAILED, conversationManager.messages.value[0].status)

        // Ensure no further retries run
        advanceTimeBy(20000L)
        val sendsAfterFinal = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals("No retries should be sent after reaching FAILED terminal state", 4, sendsAfterFinal)
    }

    @Test
    fun readAck_cancelsRunningRetryJob() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        conversationManager.sendMessage("Quick reply")
        val msgId = conversationManager.messages.value[0].id

        // Advance 5s -> 1st retry sent
        advanceTimeBy(5001L)
        assertEquals(2, fakeWsClient.sentFrames.filter { it.first == "send" }.size)

        // Peer sends read ack
        fakeWsClient.emitEvent(
            FadfedWsEvent.Ack(
                ref = msgId,
                id = "ack-read-1",
                status = "read",
                by = "peer-udid-1",
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        assertEquals(MessageStatus.READ, conversationManager.messages.value[0].status)

        // Advance another 20s -> verify no more retries were executed
        advanceTimeBy(20000L)
        assertEquals(
            "Retry job must be cancelled immediately when message reaches READ terminal state",
            2,
            fakeWsClient.sentFrames.filter { it.first == "send" }.size
        )
    }

    @Test
    fun failedMessage_manualRetryResetsRetryCount() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        conversationManager.sendMessage("Failed text")
        val msgId = conversationManager.messages.value[0].id

        // Advance 25s so it fails
        advanceTimeBy(25000L)
        assertEquals(MessageStatus.FAILED, conversationManager.messages.value[0].status)

        // User taps retry
        conversationManager.retryMessage(msgId)
        val retriedMsg = conversationManager.messages.value[0]
        assertEquals(MessageStatus.SENT, retriedMsg.status)
        assertEquals(0, retriedMsg.retryCount)
    }

    @Test
    fun staleEvents_fromOldConversationIgnored() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-NEW", "peer-udid-2", "Bob")

        // Inbound message with old chatId
        fakeWsClient.emitEvent(
            FadfedWsEvent.Message(
                id = "peer-udid-1:stale-msg",
                content = "Stale chat message",
                by = "peer-udid-1",
                chatId = "chat-OLD",
                ts = 1690000000000L,
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        assertTrue("Stale message from old conversation must be ignored", conversationManager.messages.value.isEmpty())
    }

    @Test
    fun peerLeave_cleansUpRetryJobsAndEndsConversation() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        conversationManager.sendMessage("Unacknowledged message")

        // Peer leaves
        fakeWsClient.emitEvent(
            FadfedWsEvent.Leave(
                chatId = "chat-100",
                shouldKeepProfile = false,
                friends = false,
                raw = JsonObject()
            )
        )
        advanceUntilIdle()

        assertNull("activeChatId should be cleared on peer leave", conversationManager.activeChatId)

        // Advance time: no retry loops should continue after conversation ends
        val sendsBefore = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        advanceTimeBy(20000L)
        val sendsAfter = fakeWsClient.sentFrames.filter { it.first == "send" }.size
        assertEquals("All retry jobs must be cancelled when peer leaves", sendsBefore, sendsAfter)
    }

    @Test
    fun emptyMessage_isNotSent() = runTest(testDispatcher) {
        val conversationManager = ConversationManager(fakeWsClient, identityManager, backgroundScope, unconfinedScope)
        advanceUntilIdle()

        conversationManager.startConversation("chat-100", "peer-udid-1", "Sara")
        val sentBlank = conversationManager.sendMessage("   ")
        assertFalse("Blank message must not be sent", sentBlank)
        assertTrue(conversationManager.messages.value.isEmpty())
    }

    private class FakeWebSocketClient(identityManager: IdentityManager) : FadfedWebSocketClient(identityManager) {
        private val _mockConnectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
        override val connectionState: StateFlow<WsConnectionState> = _mockConnectionState.asStateFlow()

        private val _mockIncomingEvents = MutableSharedFlow<FadfedWsEvent>(replay = 10, extraBufferCapacity = 64)
        override val incomingEvents: SharedFlow<FadfedWsEvent> get() = _mockIncomingEvents

        val sentFrames = mutableListOf<Pair<String, JsonObject>>()

        fun setReady() {
            _mockConnectionState.value = WsConnectionState.Ready
        }

        fun emitEvent(event: FadfedWsEvent) {
            _mockIncomingEvents.tryEmit(event)
        }

        override fun send(event: String, payload: JsonObject): Boolean {
            sentFrames.add(Pair(event, payload))
            return true
        }

        override fun connect() {}
        override fun disconnect() {}
    }
}
