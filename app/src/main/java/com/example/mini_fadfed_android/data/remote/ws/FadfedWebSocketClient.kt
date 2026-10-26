package com.example.mini_fadfed_android.data.remote.ws

import android.util.Log
import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.data.remote.FadfedConfig
import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

open class FadfedWebSocketClient(
    private val identityManager: IdentityManager,
    private val wsBaseUrl: String = FadfedConfig.DEFAULT_WS_BASE_URL,
    customClient: OkHttpClient? = null
) {
    companion object {
        private const val TAG = "FadfedWS"
    }

    private val client: OkHttpClient = customClient ?: OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _connectionState = MutableStateFlow<WsConnectionState>(WsConnectionState.Disconnected)
    open val connectionState: StateFlow<WsConnectionState> = _connectionState.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<FadfedWsEvent>(replay = 5, extraBufferCapacity = 64)
    open val incomingEvents: SharedFlow<FadfedWsEvent> = _incomingEvents.asSharedFlow()

    private val activeWebSocket = AtomicReference<WebSocket?>(null)

    /**
     * Initiates a WebSocket connection using the protocol specifications:
     * - Subprotocol: v2_2.fadfedly.com
     * - User-Agent: Fadfed/2.4.3 Android/14
     * - Query parameters: udid, devid, token, session-id
     */
    @Synchronized
    open fun connect() {
        val currentState = _connectionState.value
        if (currentState is WsConnectionState.Connecting ||
            currentState is WsConnectionState.Connected ||
            currentState is WsConnectionState.Ready
        ) {
            Log.d(TAG, "connect() ignored: already in state $currentState")
            return
        }

        // Close any existing lingering socket
        disconnectInternal(code = 1000, reason = "New connection initiated")

        _connectionState.value = WsConnectionState.Connecting

        val token = identityManager.computeToken()
        val url = buildWebSocketUrl(token)

        Log.d(TAG, "Connecting to WebSocket at $wsBaseUrl with subprotocol ${FadfedConfig.WS_SUBPROTOCOL}")

        val request = Request.Builder()
            .url(url)
            .header("Sec-WebSocket-Protocol", FadfedConfig.WS_SUBPROTOCOL)
            .header("User-Agent", FadfedConfig.USER_AGENT)
            .build()

        val socket = client.newWebSocket(request, createSocketListener())
        activeWebSocket.set(socket)
    }

    /**
     * Cleanly closes the active WebSocket connection.
     */
    @Synchronized
    open fun disconnect() {
        Log.d(TAG, "disconnect() requested by client")
        disconnectInternal(code = 1000, reason = "Client disconnect")
        _connectionState.value = WsConnectionState.Disconnected
    }

    /**
     * Sends an application frame.
     * Enforces the READY gate:
     * If the session is not in the Ready state, the frame is REJECTED and NOT queued.
     */
    open fun send(event: String, payload: JsonObject): Boolean {
        if (_connectionState.value !is WsConnectionState.Ready) {
            Log.w(TAG, "send rejected for event '$event': state is ${_connectionState.value}")
            return false
        }
        return sendInternal(event, payload)
    }

    private fun sendInternal(event: String, payload: JsonObject): Boolean {
        val ws = activeWebSocket.get()
        if (ws == null) {
            Log.w(TAG, "sendInternal failed for event '$event': activeWebSocket is null")
            return false
        }
        val frameText = WebSocketFrameParser.serializeFrame(event, payload)
        Log.d(TAG, "Sending WS frame: $frameText")
        return ws.send(frameText)
    }

    private fun disconnectInternal(code: Int, reason: String) {
        val current = activeWebSocket.getAndSet(null)
        try {
            current?.close(code, reason)
        } catch (_: Exception) {
            current?.cancel()
        }
    }

    private fun buildWebSocketUrl(token: String): String {
        val baseUrl = wsBaseUrl.removeSuffix("/")
        val path = FadfedConfig.WS_PATH.removePrefix("/")
        return "$baseUrl/$path?udid=${identityManager.udid}&devid=${identityManager.devId}&token=$token&session-id=${identityManager.sessionId}"
    }

    private fun createSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket onOpen: HTTP response code ${response.code}")
                // Socket is connected, but inbound event emitter is paused during flush.
                // We MUST wait for ["session", {"state": "ready"}] before sending application frames.
                _connectionState.value = WsConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val event = WebSocketFrameParser.parseFrame(text)
                if (event == null) {
                    Log.w(TAG, "Failed to parse incoming WS frame: $text")
                    return
                }

                when (event) {
                    is FadfedWsEvent.Session -> {
                        Log.d(TAG, "Incoming event: Session(state=${event.state})")
                        if (event.state.equals("ready", ignoreCase = true)) {
                            _connectionState.value = WsConnectionState.Ready

                            // Immediately enable the accept gate before any other application frames (§8.5)
                            val setAcceptPayload = JsonObject().apply {
                                val features = JsonObject().apply {
                                    addProperty("accept", true)
                                }
                                add("features", features)
                            }
                            Log.d(TAG, "Session is READY. Sending initial set frame: [set, features.accept=true]")
                            sendInternal("set", setAcceptPayload)
                        } else if (event.state.equals("flush", ignoreCase = true)) {
                            _connectionState.value = WsConnectionState.Connected
                        }
                    }
                    is FadfedWsEvent.SessionReplaced -> {
                        Log.w(TAG, "Incoming event: SessionReplaced (terminal state)")
                        _connectionState.value = WsConnectionState.Replaced
                        disconnectInternal(code = 1000, reason = "Session replaced")
                    }
                    is FadfedWsEvent.Status -> {
                        Log.d(TAG, "Incoming event: Status(coins=${event.coins}, genderCost=${event.genderCost}, countryCost=${event.countryCost})")
                    }
                    is FadfedWsEvent.Matched -> {
                        Log.d(TAG, "Incoming event: Matched(chatId=${event.chatId}, peerUdid=${event.udid}, accepted=${event.accepted}, ref=${event.ref})")
                    }
                    is FadfedWsEvent.Sync -> {
                        Log.d(TAG, "Incoming event: Sync(dataset=${event.dataset}, item=${event.item})")
                    }
                    is FadfedWsEvent.Leave -> {
                        Log.d(TAG, "Incoming event: Leave(chatId=${event.chatId}, friends=${event.friends})")
                    }
                    is FadfedWsEvent.Error -> {
                        Log.e(TAG, "Incoming event: Error(code=${event.code}, ref=${event.ref})")
                    }
                    is FadfedWsEvent.Message -> {
                        Log.d(TAG, "Incoming event: Message(id=${event.id}, chatId=${event.chatId})")
                    }
                    is FadfedWsEvent.Ack -> {
                        Log.d(TAG, "Incoming event: Ack(id=${event.id}, status=${event.status}, ref=${event.ref})")
                    }
                    else -> {
                        Log.d(TAG, "Incoming event: ${event.javaClass.simpleName}")
                    }
                }

                _incomingEvents.tryEmit(event)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket onClosing: code=$code, reason=$reason")
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket onClosed: code=$code, reason=$reason")
                activeWebSocket.compareAndSet(webSocket, null)
                if (_connectionState.value !is WsConnectionState.Replaced) {
                    _connectionState.value = WsConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                activeWebSocket.compareAndSet(webSocket, null)
                val code = response?.code
                val message = when (code) {
                    421 -> "WebSocket subprotocol rejected by server (421)"
                    422 -> "Authentication or User-Agent format rejected (422)"
                    404 -> "Tenant not found on server (404)"
                    else -> t.localizedMessage ?: "WebSocket connection failure"
                }
                Log.e(TAG, "WebSocket onFailure: code=$code, message=$message, error=${t.message}")
                _connectionState.value = WsConnectionState.Error(
                    message = message,
                    code = code,
                    canRetry = code != 421 && code != 422
                )
            }
        }
    }
}
