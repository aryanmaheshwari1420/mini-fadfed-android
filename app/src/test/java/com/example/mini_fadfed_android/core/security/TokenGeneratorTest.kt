package com.example.mini_fadfed_android.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenGeneratorTest {

    @Test
    fun makeToken_matchesAppendixATestVector() {
        val sessionId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        val devid = "web-11112222-3333-4444-5555-666677778888"
        val expectedToken = "8b0cdb974f26653f9da5c88555605e13"

        val actualToken = TokenGenerator.makeToken(sessionId, devid)

        assertEquals("Token should match expected HMAC-MD5 test vector", expectedToken, actualToken)
        assertEquals("Token must be exactly 32 characters long", 32, actualToken.length)
        assertTrue("Token must be lowercase hex", actualToken.matches(Regex("^[0-9a-f]{32}$")))
    }

    @Test
    fun makeToken_generatesConsistentOutputForSameInputs() {
        val sessionId = "session-12345"
        val devid = "device-67890"

        val token1 = TokenGenerator.makeToken(sessionId, devid)
        val token2 = TokenGenerator.makeToken(sessionId, devid)

        assertEquals(token1, token2)
        assertEquals(32, token1.length)
    }

    @Test
    fun makeToken_changesWhenSessionIdChanges() {
        val devid = "stable-device-id"
        val token1 = TokenGenerator.makeToken("session-one", devid)
        val token2 = TokenGenerator.makeToken("session-two", devid)

        assertTrue(token1 != token2)
    }
}
