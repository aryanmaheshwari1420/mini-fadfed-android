package com.example.mini_fadfed_android.core.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import okio.ByteString.Companion.decodeBase64

class IdentityManagerTest {

    private lateinit var fakeStorage: FakeIdentityStorage

    @Before
    fun setup() {
        fakeStorage = FakeIdentityStorage()
    }

    @Test
    fun devId_isGeneratedAndPersisted() {
        val manager1 = IdentityManager(fakeStorage)
        val devId1 = manager1.devId

        assertNotNull(devId1)
        assertTrue("devId must match ^[\\w\\-{}]+$", devId1.matches(Regex("^[\\w\\-{}]+$")))

        // Second instance with same storage should retrieve the same devId
        val manager2 = IdentityManager(fakeStorage)
        assertEquals("devId must persist across sessions", devId1, manager2.devId)
    }

    @Test
    fun udid_equalsDevIdForAnonymousFlow() {
        val manager = IdentityManager(fakeStorage)
        assertEquals(manager.devId, manager.udid)
    }

    @Test
    fun computeToken_recalculatedWhenSessionIdRefreshes() {
        val manager = IdentityManager(fakeStorage)
        val session1 = manager.sessionId
        val token1 = manager.computeToken()

        val session2 = manager.refreshSessionId()
        val token2 = manager.computeToken()

        assertNotEquals(session1, session2)
        assertNotEquals(token1, token2)
        assertEquals(32, token1.length)
        assertEquals(32, token2.length)
    }

    @Test
    fun getAuthorizationHeader_formatsCorrectBasicAuthHeader() {
        val fixedSessionId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
        val fixedDevId = "web-11112222-3333-4444-5555-666677778888"
        fakeStorage.saveDevId(fixedDevId)

        val manager = IdentityManager(fakeStorage, initialSessionId = fixedSessionId)
        val authHeader = manager.getAuthorizationHeader()

        assertTrue(authHeader.startsWith("Basic "))
        val base64Part = authHeader.removePrefix("Basic ")
        val decoded = base64Part.decodeBase64()?.utf8()

        val expectedToken = "8b0cdb974f26653f9da5c88555605e13"
        assertEquals("$fixedDevId:$expectedToken", decoded)
    }

    @Test
    fun profileRegistration_persistsCorrectly() {
        val manager = IdentityManager(fakeStorage)
        assertFalse(manager.isRegistered())

        manager.saveProfile(name = "Sara", gender = "f")

        assertTrue(manager.isRegistered())
        assertEquals("Sara", manager.getUserName())
        assertEquals("f", manager.getUserGender())
    }

    private class FakeIdentityStorage : IdentityStorage {
        private var devId: String? = null
        private var isRegistered: Boolean = false
        private var userName: String? = null
        private var userGender: String? = null

        override fun getDevId(): String? = devId
        override fun saveDevId(devId: String) { this.devId = devId }
        override fun isRegistered(): Boolean = isRegistered
        override fun setRegistered(registered: Boolean) { this.isRegistered = registered }
        override fun getUserName(): String? = userName
        override fun setUserName(name: String) { this.userName = name }
        override fun getUserGender(): String? = userGender
        override fun setUserGender(gender: String) { this.userGender = gender }
        override fun clear() {
            devId = null
            isRegistered = false
            userName = null
            userGender = null
        }
    }
}
