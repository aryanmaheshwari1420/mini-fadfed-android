package com.example.mini_fadfed_android.data.remote.ws

import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSocketFrameParserTest {

    @Test
    fun serializeFrame_producesTwoElementJsonArray() {
        val payload = JsonObject().apply {
            addProperty("id", "req-123")
            addProperty("algo", "R")
        }
        val serialized = WebSocketFrameParser.serializeFrame("match", payload)

        assertEquals("""["match",{"id":"req-123","algo":"R"}]""", serialized)
    }

    @Test
    fun parseFrame_parsesSessionFlushAndReadyCorrectly() {
        val flushJson = """["session", {"state": "flush", "sessionId": "s-1", "udid": "u-1"}]"""
        val readyJson = """["session", {"state": "ready", "sessionId": "s-1", "udid": "u-1"}]"""

        val flushEvent = WebSocketFrameParser.parseFrame(flushJson)
        val readyEvent = WebSocketFrameParser.parseFrame(readyJson)

        assertTrue(flushEvent is FadfedWsEvent.Session)
        assertEquals("flush", (flushEvent as FadfedWsEvent.Session).state)

        assertTrue(readyEvent is FadfedWsEvent.Session)
        assertEquals("ready", (readyEvent as FadfedWsEvent.Session).state)
        assertEquals("s-1", readyEvent.sessionId)
        assertEquals("u-1", readyEvent.udid)
    }

    @Test
    fun parseFrame_parsesStatusEvent() {
        val statusJson = """["status", {"coins": 25, "genderCost": 9, "countryCost": 15, "premium": false}]"""
        val event = WebSocketFrameParser.parseFrame(statusJson)

        assertTrue(event is FadfedWsEvent.Status)
        val status = event as FadfedWsEvent.Status
        assertEquals(25, status.coins)
        assertEquals(9, status.genderCost)
        assertEquals(15, status.countryCost)
    }

    @Test
    fun parseFrame_parsesSessionReplaced() {
        val replacedJson = """["session:replaced", {}]"""
        val event = WebSocketFrameParser.parseFrame(replacedJson)

        assertTrue(event is FadfedWsEvent.SessionReplaced)
    }

    @Test
    fun parseFrame_parsesErrorEvent() {
        val errorJson = """["error", {"ref": "req-999", "code": "insufficient_coins"}]"""
        val event = WebSocketFrameParser.parseFrame(errorJson)

        assertTrue(event is FadfedWsEvent.Error)
        val error = event as FadfedWsEvent.Error
        assertEquals("req-999", error.ref)
        assertEquals("insufficient_coins", error.code)
    }

    @Test
    fun parseFrame_handlesUnknownEventsGracefullyWithoutCrashing() {
        val unknownJson = """["future_event", {"some_key": "some_value"}]"""
        val event = WebSocketFrameParser.parseFrame(unknownJson)

        assertTrue(event is FadfedWsEvent.Unknown)
        val unknown = event as FadfedWsEvent.Unknown
        assertEquals("future_event", unknown.eventName)
        assertEquals("some_value", unknown.raw.get("some_key")?.asString)
    }

    @Test
    fun parseFrame_handlesMalformedJsonGracefullyWithoutCrashing() {
        assertNull(WebSocketFrameParser.parseFrame("not json at all"))
        assertNull(WebSocketFrameParser.parseFrame("{'not':'an array'}"))
        assertNull(WebSocketFrameParser.parseFrame("[]"))
    }
}
