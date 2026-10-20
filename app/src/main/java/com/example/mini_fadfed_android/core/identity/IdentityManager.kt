package com.example.mini_fadfed_android.core.identity

import com.example.mini_fadfed_android.core.security.TokenGenerator
import okio.ByteString.Companion.encodeUtf8
import java.util.UUID

class IdentityManager(
    private val storage: IdentityStorage,
    initialSessionId: String? = null
) {
    /**
     * Stable device identifier. Generated once and persisted forever.
     * Matches ^[\w\-{}]+$
     */
    val devId: String = storage.getDevId() ?: run {
        val newId = "android-" + UUID.randomUUID().toString()
        storage.saveDevId(newId)
        newId
    }

    /**
     * Account identifier. For the anonymous flow, udid = devid.
     */
    val udid: String
        get() = devId

    /**
     * A random UUID, generated per app session (or socket connection).
     */
    var sessionId: String = initialSessionId ?: UUID.randomUUID().toString()
        private set

    /**
     * Generates a new session-id when needed (e.g., on app reconnect or new session).
     */
    fun refreshSessionId(): String {
        val newSessionId = UUID.randomUUID().toString()
        sessionId = newSessionId
        return newSessionId
    }

    /**
     * Computes the HMAC-MD5 token on demand from current sessionId and devId.
     */
    fun computeToken(): String {
        return TokenGenerator.makeToken(sessionId = sessionId, devid = devId)
    }

    /**
     * Computes the HTTP Authorization header: Basic <base64(devid:token)>
     */
    fun getAuthorizationHeader(): String {
        val token = computeToken()
        val rawCredentials = "$devId:$token"
        val base64Credentials = rawCredentials.encodeUtf8().base64()
        return "Basic $base64Credentials"
    }

    fun isRegistered(): Boolean = storage.isRegistered()

    fun getUserName(): String? = storage.getUserName()

    fun getUserGender(): String? = storage.getUserGender()

    fun saveProfile(name: String, gender: String) {
        storage.setUserName(name)
        storage.setUserGender(gender)
        storage.setRegistered(true)
    }

    fun clear() {
        storage.clear()
    }
}
