package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import com.example.mini_fadfed_android.data.remote.ws.FadfedWebSocketClient
import com.example.mini_fadfed_android.data.remote.ws.WsConnectionState
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.example.mini_fadfed_android.domain.model.MatchFilter
import com.example.mini_fadfed_android.domain.model.MatchUiState
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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MatchSessionManagerTest {

    private lateinit var sessionScope: CoroutineScope
    private lateinit var fakeWsClient: FakeWebSocketClient

    @Before
    fun setup() {
        sessionScope = CoroutineScope(Dispatchers.Unconfined + Job())
        val fakeStorage = object : IdentityStorage {
            override fun getDevId(): String = "test-device"
            override fun saveDevId(devId: String) {}
            override fun isRegistered(): Boolean = true
            override fun setRegistered(registered: Boolean) {}
            override fun getUserName(): String = "TestUser"
            override fun setUserName(name: String) {}
            override fun getUserGender(): String = "m"
            override fun setUserGender(gender: String) {}
            override fun clear() {}
        }
        val identityManager = IdentityManager(fakeStorage, initialSessionId = "session-123")
        fakeWsClient = FakeWebSocketClient(identityManager)
        fakeWsClient.setReady()
    }

    @After
    fun tearDown() {
        sessionScope.cancel()
    }

    @Test
    fun bothSearch_omitsGenderKeyEntirely() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)

        val lastSent = fakeWsClient.lastSentFrame
        assertNotNull(lastSent)
        assertEquals("match", lastSent!!.first)
        val payload = lastSent.second

        assertEquals("R", payload.get("algo")?.asString)
        assertNotNull(payload.get("id")?.asString)
        assertFalse("Free 'Both' search must completely omit the gender key", payload.has("gender"))
    }

    @Test
    fun maleAndFemaleSearch_includesExactGenderKey() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.MALE)
        assertEquals("m", fakeWsClient.lastSentFrame?.second?.get("gender")?.asString)

        sessionManager.cancelSearch()

        sessionManager.startSearch(MatchFilter.FEMALE)
        assertEquals("f", fakeWsClient.lastSentFrame?.second?.get("gender")?.asString)
    }

    @Test
    fun search_generatesUniqueMatchRequestIds() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)
        val id1 = fakeWsClient.lastSentFrame?.second?.get("id")?.asString

        sessionManager.cancelSearch()

        sessionManager.startSearch(MatchFilter.BOTH)
        val id2 = fakeWsClient.lastSentFrame?.second?.get("id")?.asString

        assertNotNull(id1)
        assertNotNull(id2)
        assertNotEquals("Each match search must generate a unique request ID", id1, id2)
    }

    @Test
    fun cancelSearch_sendsLeaveWithoutChatId() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)
        assertTrue(sessionManager.matchState.value is MatchUiState.Searching)

        sessionManager.cancelSearch()

        val lastSent = fakeWsClient.lastSentFrame
        assertNotNull(lastSent)
        assertEquals("leave", lastSent!!.first)
        assertFalse("Cancel search leave frame must not have chatId", lastSent.second.has("chatId"))
        assertTrue("State should return to Idle", sessionManager.matchState.value is MatchUiState.Idle)
    }

    @Test
    fun insufficientCoins_stopsSearchAndReturnsToHomeWithMessage() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.MALE)
        assertTrue(sessionManager.matchState.value is MatchUiState.Searching)

        fakeWsClient.emitEvent(FadfedWsEvent.Error(ref = "some-ref", code = "insufficient_coins", raw = JsonObject()))

        assertTrue("Should return to Idle state", sessionManager.matchState.value is MatchUiState.Idle)
        val errorMsg = sessionManager.errorMessage.value
        assertNotNull(errorMsg)
        assertTrue(errorMsg!!.contains("Insufficient coins", ignoreCase = true))
    }

    @Test
    fun matchProfileSynchronization_updatesPeerNameAndGender() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)

        // Matched arrives
        fakeWsClient.emitEvent(
            FadfedWsEvent.Matched(
                ref = "ref",
                chatId = "chat-100",
                udid = "peer-udid-1",
                accepted = false,
                raw = JsonObject()
            )
        )

        var current = sessionManager.matchState.value as MatchUiState.Matched
        assertEquals("Stranger", current.peerName)

        // Sync arrives
        val profileData = JsonObject().apply {
            addProperty("name", "Sara")
            addProperty("gender", "f")
        }
        fakeWsClient.emitEvent(
            FadfedWsEvent.Sync(
                dataset = "\$profile",
                item = "peer-udid-1",
                version = 1L,
                data = profileData,
                raw = JsonObject()
            )
        )

        current = sessionManager.matchState.value as MatchUiState.Matched
        assertEquals("Sara", current.peerName)
        assertEquals("f", current.peerGender)
    }

    @Test
    fun mutualAcceptance_flowRequiresBothParties() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)

        fakeWsClient.emitEvent(
            FadfedWsEvent.Matched(
                ref = "ref",
                chatId = "chat-200",
                udid = "peer-udid-2",
                accepted = false,
                raw = JsonObject()
            )
        )

        // 1. Neither accepted
        var matched = sessionManager.matchState.value as MatchUiState.Matched
        assertFalse(matched.youAccepted)
        assertFalse(matched.peerAccepted)

        // 2. Local user accepts only -> stays in Matched, does NOT open conversation yet
        sessionManager.acceptMatch()
        assertEquals("accept", fakeWsClient.lastSentFrame?.first)
        assertEquals("chat-200", fakeWsClient.lastSentFrame?.second?.get("chatId")?.asString)

        assertTrue(sessionManager.matchState.value is MatchUiState.Matched)
        matched = sessionManager.matchState.value as MatchUiState.Matched
        assertTrue(matched.youAccepted)
        assertFalse(matched.peerAccepted)

        // 3. Second matched arrives with peer accepted: true -> both accepted!
        fakeWsClient.emitEvent(
            FadfedWsEvent.Matched(
                ref = "ref",
                chatId = "chat-200",
                udid = "peer-udid-2",
                accepted = true,
                raw = JsonObject()
            )
        )

        // Now both true -> navigates to Connected
        assertTrue(
            "Conversation opens only when both accepted",
            sessionManager.matchState.value is MatchUiState.Connected
        )
        val connected = sessionManager.matchState.value as MatchUiState.Connected
        assertEquals("chat-200", connected.chatId)
    }

    @Test
    fun peerAlreadyAccepted_opensConversationWhenLocalAccepts() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)

        // Peer had gate off or already accepted -> first matched has accepted: true
        fakeWsClient.emitEvent(
            FadfedWsEvent.Matched(
                ref = "ref",
                chatId = "chat-300",
                udid = "peer-udid-3",
                accepted = true,
                raw = JsonObject()
            )
        )

        var matched = sessionManager.matchState.value as MatchUiState.Matched
        assertFalse("Local user has not accepted yet", matched.youAccepted)
        assertTrue("Peer is already accepted", matched.peerAccepted)

        // Now local user accepts
        sessionManager.acceptMatch()

        assertTrue(
            "Both now accepted -> moves to Connected",
            sessionManager.matchState.value is MatchUiState.Connected
        )
    }

    @Test
    fun peerSkips_automaticallyRestartsSearch() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)

        fakeWsClient.emitEvent(
            FadfedWsEvent.Matched(
                ref = "ref",
                chatId = "chat-400",
                udid = "peer-udid-4",
                accepted = false,
                raw = JsonObject()
            )
        )
        assertTrue(sessionManager.matchState.value is MatchUiState.Matched)

        // Peer leaves
        fakeWsClient.emitEvent(
            FadfedWsEvent.Leave(
                chatId = "chat-400",
                shouldKeepProfile = false,
                friends = false,
                raw = JsonObject()
            )
        )

        // Should automatically return to Searching
        assertTrue(
            "Peer leave during match must automatically restart search",
            sessionManager.matchState.value is MatchUiState.Searching
        )
    }

    @Test
    fun duplicateTaps_doNotSendMultipleMatchRequests() = runTest {
        val sessionManager = MatchSessionManager(fakeWsClient, sessionScope)
        sessionManager.startSearch(MatchFilter.BOTH)
        val countAfterFirst = fakeWsClient.sentCount

        // Tap again while already searching
        sessionManager.startSearch(MatchFilter.BOTH)
        assertEquals("Second tap while searching must be ignored", countAfterFirst, fakeWsClient.sentCount)
    }

    private class FakeWebSocketClient(identityManager: IdentityManager) : FadfedWebSocketClient(identityManager) {
        private val _mockConnectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
        override val connectionState: StateFlow<WsConnectionState> = _mockConnectionState.asStateFlow()

        private val _mockIncomingEvents = MutableSharedFlow<FadfedWsEvent>(replay = 10, extraBufferCapacity = 64)
        override val incomingEvents: SharedFlow<FadfedWsEvent> get() = _mockIncomingEvents

        var lastSentFrame: Pair<String, JsonObject>? = null
        var sentCount: Int = 0

        fun setReady() {
            _mockConnectionState.value = WsConnectionState.Ready
        }

        fun emitEvent(event: FadfedWsEvent) {
            _mockIncomingEvents.tryEmit(event)
        }

        override fun send(event: String, payload: JsonObject): Boolean {
            lastSentFrame = Pair(event, payload)
            sentCount++
            return true
        }

        override fun connect() {}
        override fun disconnect() {}
    }
}
