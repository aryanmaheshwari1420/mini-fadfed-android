package com.example.mini_fadfed_android.data.remote.ws.models

import com.google.gson.JsonObject

sealed class FadfedWsEvent {
    data class Session(
        val state: String,
        val sessionId: String?,
        val udid: String?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Status(
        val coins: Int,
        val genderCost: Int,
        val countryCost: Int,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class SessionReplaced(val raw: JsonObject) : FadfedWsEvent()

    data class Matched(
        val ref: String?,
        val chatId: String?,
        val udid: String?,
        val accepted: Boolean,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Sync(
        val dataset: String?,
        val item: String?,
        val version: Long?,
        val data: JsonObject?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Message(
        val id: String?,
        val content: String?,
        val by: String?,
        val chatId: String?,
        val ts: Long?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Ack(
        val ref: String?,
        val id: String?,
        val status: String?,
        val by: String?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Seen(
        val ref: String?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Leave(
        val chatId: String?,
        val shouldKeepProfile: Boolean?,
        val friends: Boolean?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Type(
        val to: String?,
        val by: String?,
        val chatId: String?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Error(
        val ref: String?,
        val code: String?,
        val raw: JsonObject
    ) : FadfedWsEvent()

    data class Unknown(
        val eventName: String,
        val raw: JsonObject
    ) : FadfedWsEvent()
}
