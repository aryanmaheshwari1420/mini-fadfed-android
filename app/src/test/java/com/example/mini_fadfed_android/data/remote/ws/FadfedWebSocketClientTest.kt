package com.example.mini_fadfed_android.data.remote.ws

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class FadfedWebSocketClientTest {

    private lateinit var server: MockWebServer
    private lateinit var fakeStorage: FakeStorage
    private lateinit var identityManager: IdentityManager
    private lateinit var wsClient: FadfedWebSocketClient

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        fakeStorage = FakeStorage()
        identityManager = IdentityManager(fakeStorage, initialSessionId = "test-session-123")
        val wsUrl = "ws://" + server.hostName + ":" + server.port

        wsClient = FadfedWebSocketClient(
            identityManager = identityManager,
            wsBaseUrl = wsUrl
        )
    }

    @After
    fun tearDown() {
        wsClient.disconnect()
        try {
            server.shutdown()
        } catch (_: Exception) {}
    }

    @Test
    fun connect_sendsCorrectPathQueryAndHeaders() {
        val serverReceivedFrames = LinkedBlockingQueue<String>()

        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    serverReceivedFrames.add(text)
                }
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }
            })
        )

        wsClient.connect()

        val recordedRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recordedRequest)

        val path = recordedRequest!!.path.orEmpty()
        assertTrue("Path must target /@fadfedx", path.startsWith("/@fadfedx"))
        assertTrue("Must include devid param", path.contains("devid=android-test-device"))
        assertTrue("Must include udid param", path.contains("udid=android-test-device"))
        assertTrue("Must include session-id param", path.contains("session-id=test-session-123"))
        assertTrue("Must include token param", path.contains("token="))
        assertEquals("v2_2.fadfedly.com", recordedRequest.getHeader("Sec-WebSocket-Protocol"))
        assertEquals("Fadfed/2.4.3 Android/14", recordedRequest.getHeader("User-Agent"))
    }

    @Test
    fun readyGate_preventsApplicationFramesBeforeReady_andSendsAcceptGateImmediatelyAfterReady() = runBlocking {
        val serverSentSocket = LinkedBlockingQueue<WebSocket>()
        val serverReceivedFrames = LinkedBlockingQueue<String>()

        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSentSocket.add(webSocket)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    serverReceivedFrames.add(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }
            })
        )

        wsClient.connect()
        val serverSocket = serverSentSocket.poll(5, TimeUnit.SECONDS)
        assertNotNull(serverSocket)

        // Wait until client registers connection
        val connectedState = withTimeoutOrNull(3000) {
            wsClient.connectionState.first { it is WsConnectionState.Connected }
        }
        assertTrue("Should transition to Connected on socket open", connectedState is WsConnectionState.Connected)

        // Server pushes flush session
        serverSocket!!.send("""["session", {"state": "flush", "sessionId": "s-1", "udid": "u-1"}]""")

        // Attempting to send application frame before READY MUST return false and not send
        val samplePayload = JsonObject().apply { addProperty("id", "req-1") }
        val sendResultBeforeReady = wsClient.send("match", samplePayload)
        assertFalse("Frames must not be sent before READY", sendResultBeforeReady)
        assertEquals("Server should not receive any application frame during flush", 0, serverReceivedFrames.size)

        // Now server pushes ready session
        serverSocket.send("""["session", {"state": "ready", "sessionId": "s-1", "udid": "u-1"}]""")

        // Wait until client transitions to Ready
        val readyState = withTimeoutOrNull(3000) {
            wsClient.connectionState.first { it is WsConnectionState.Ready }
        }
        assertTrue("Should transition to Ready after receiving session:ready", readyState is WsConnectionState.Ready)

        // Client must immediately send ["set", {"features": {"accept": true}}]
        val firstSentFrame = serverReceivedFrames.poll(5, TimeUnit.SECONDS)
        assertNotNull(firstSentFrame)
        assertTrue(
            "First frame after ready must be the accept feature set frame",
            firstSentFrame!!.contains("\"set\"") && firstSentFrame.contains("\"accept\":true")
        )

        // Now sending application frame succeeds
        val sendResultAfterReady = wsClient.send("match", samplePayload)
        assertTrue("Frames should be sent successfully when Ready", sendResultAfterReady)

        val secondSentFrame = serverReceivedFrames.poll(5, TimeUnit.SECONDS)
        assertNotNull(secondSentFrame)
        assertTrue("Server should receive match frame", secondSentFrame!!.contains("\"match\""))
    }

    @Test
    fun sessionReplaced_transitionsToReplacedState() = runBlocking {
        val serverSentSocket = LinkedBlockingQueue<WebSocket>()

        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    serverSentSocket.add(webSocket)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }
            })
        )

        wsClient.connect()
        val serverSocket = serverSentSocket.poll(5, TimeUnit.SECONDS)
        assertNotNull(serverSocket)

        // Server sends session:replaced
        serverSocket!!.send("""["session:replaced", {}]""")

        val replacedState = withTimeoutOrNull(3000) {
            wsClient.connectionState.first { it is WsConnectionState.Replaced }
        }
        assertTrue("Should transition to Replaced terminal state", replacedState is WsConnectionState.Replaced)
    }

    private fun assertNotNull(obj: Any?) {
        org.junit.Assert.assertNotNull(obj)
    }

    private class FakeStorage : IdentityStorage {
        private var devId: String? = "android-test-device"
        private var registered: Boolean = false
        private var name: String? = null
        private var gender: String? = null

        override fun getDevId(): String? = devId
        override fun saveDevId(devId: String) { this.devId = devId }
        override fun isRegistered(): Boolean = registered
        override fun setRegistered(registered: Boolean) { this.registered = registered }
        override fun getUserName(): String? = name
        override fun setUserName(name: String) { this.name = name }
        override fun getUserGender(): String? = gender
        override fun setUserGender(gender: String) { this.gender = gender }
        override fun clear() {}
    }
}
