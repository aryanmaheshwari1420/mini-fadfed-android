package com.example.mini_fadfed_android.data.remote.ws

import com.example.mini_fadfed_android.data.remote.ws.models.FadfedWsEvent
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive

object WebSocketFrameParser {

    /**
     * Serializes an event name and payload into the required 2-element JSON array:
     * ["event_name", { ... }]
     */
    fun serializeFrame(event: String, payload: JsonObject): String {
        val array = JsonArray().apply {
            add(JsonPrimitive(event))
            add(payload)
        }
        return array.toString()
    }

    /**
     * Parses an incoming wire frame into a typed FadfedWsEvent safely.
     * Returns null if the frame is completely malformed or unparseable.
     * Unknown events are returned as FadfedWsEvent.Unknown and do not throw.
     */
    fun parseFrame(text: String): FadfedWsEvent? {
        return try {
            val element = JsonParser.parseString(text)
            if (!element.isJsonArray) {
                // Non-array frames (bare strings or objects)
                return null
            }
            val array = element.asJsonArray
            if (array.size() < 1) return null

            val eventName = array.get(0).asString
            val data = if (array.size() > 1 && array.get(1).isJsonObject) {
                array.get(1).asJsonObject
            } else {
                JsonObject()
            }

            when (eventName) {
                "session" -> {
                    FadfedWsEvent.Session(
                        state = data.get("state")?.asString.orEmpty(),
                        sessionId = data.get("sessionId")?.asString,
                        udid = data.get("udid")?.asString,
                        raw = data
                    )
                }
                "session:replaced" -> {
                    FadfedWsEvent.SessionReplaced(raw = data)
                }
                "status" -> {
                    FadfedWsEvent.Status(
                        coins = data.get("coins")?.asInt ?: 0,
                        genderCost = data.get("genderCost")?.asInt ?: 0,
                        countryCost = data.get("countryCost")?.asInt ?: 0,
                        raw = data
                    )
                }
                "matched" -> {
                    FadfedWsEvent.Matched(
                        ref = data.get("ref")?.asString,
                        chatId = data.get("chatId")?.asString,
                        udid = data.get("udid")?.asString,
                        accepted = data.get("accepted")?.asBoolean ?: false,
                        raw = data
                    )
                }
                "sync" -> {
                    FadfedWsEvent.Sync(
                        dataset = data.get("dataset")?.asString,
                        item = data.get("item")?.asString,
                        version = data.get("version")?.asLong,
                        data = data.get("data")?.takeIf { it.isJsonObject }?.asJsonObject,
                        raw = data
                    )
                }
                "message" -> {
                    FadfedWsEvent.Message(
                        id = data.get("id")?.asString,
                        content = data.get("content")?.asString,
                        by = data.get("by")?.asString,
                        chatId = data.get("chatId")?.asString,
                        ts = data.get("ts")?.asLong,
                        raw = data
                    )
                }
                "ack" -> {
                    FadfedWsEvent.Ack(
                        ref = data.get("ref")?.asString,
                        id = data.get("id")?.asString,
                        status = data.get("status")?.asString,
                        by = data.get("by")?.asString,
                        raw = data
                    )
                }
                "seen" -> {
                    FadfedWsEvent.Seen(
                        ref = data.get("ref")?.asString,
                        raw = data
                    )
                }
                "leave" -> {
                    FadfedWsEvent.Leave(
                        chatId = data.get("chatId")?.asString,
                        shouldKeepProfile = data.get("shouldKeepProfile")?.asBoolean,
                        friends = data.get("friends")?.asBoolean,
                        raw = data
                    )
                }
                "type" -> {
                    FadfedWsEvent.Type(
                        to = data.get("to")?.asString,
                        by = data.get("by")?.asString,
                        chatId = data.get("chatId")?.asString,
                        raw = data
                    )
                }
                "error" -> {
                    FadfedWsEvent.Error(
                        ref = data.get("ref")?.asString,
                        code = data.get("code")?.asString,
                        raw = data
                    )
                }
                else -> {
                    FadfedWsEvent.Unknown(
                        eventName = eventName,
                        raw = data
                    )
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
