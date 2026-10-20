package com.example.mini_fadfed_android.data.repository

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import com.example.mini_fadfed_android.data.remote.FadfedRestClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var fakeStorage: FakeStorage
    private lateinit var identityManager: IdentityManager
    private lateinit var restClient: FadfedRestClient
    private lateinit var repository: AuthRepository

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()

        fakeStorage = FakeStorage()
        identityManager = IdentityManager(fakeStorage, initialSessionId = "test-session")
        restClient = FadfedRestClient(
            identityManager = identityManager,
            baseUrl = server.url("").toString().removeSuffix("/"),
            ioDispatcher = testDispatcher
        )
        repository = AuthRepository(identityManager, restClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun signup_validatesEmptyNameLocallyWithoutNetwork() = runTest {
        val result = repository.signup("   ", "m")
        assertTrue(result.isFailure)
        assertEquals("Name cannot be empty.", result.exceptionOrNull()?.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun signup_validatesInvalidGenderLocallyWithoutNetwork() = runTest {
        val result = repository.signup("Sara", "x")
        assertTrue(result.isFailure)
        assertEquals("Please select a valid gender.", result.exceptionOrNull()?.message)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun signup_callsAuthThenUserInOrderAndSavesProfile() = runTest {
        // Enqueue auth response
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        // Enqueue user response
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success": true, "message": "user updated successfully"}""")
        )

        val result = repository.signup("Sara", "f")

        assertTrue(result.isSuccess)
        assertEquals(2, server.requestCount)

        val firstRequest = server.takeRequest()
        assertEquals("/api/@fadfedx/auth", firstRequest.path)

        val secondRequest = server.takeRequest()
        assertEquals("/api/@fadfedx/user", secondRequest.path)

        assertTrue(repository.isRegistered())
        assertEquals("Sara", repository.getUserName())
        assertEquals("f", repository.getUserGender())
    }

    @Test
    fun signup_doesNotCallUserIfAuthFails() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":{"message":"forbidden"}}"""))

        val result = repository.signup("Sara", "f")

        assertTrue(result.isFailure)
        assertEquals(1, server.requestCount)
        assertFalse(repository.isRegistered())
    }

    private class FakeStorage : IdentityStorage {
        private var storedDevId: String? = "android-test-device"
        private var storedRegistered: Boolean = false
        private var storedName: String? = null
        private var storedGender: String? = null

        override fun getDevId(): String? = storedDevId
        override fun saveDevId(devId: String) { this.storedDevId = devId }
        override fun isRegistered(): Boolean = storedRegistered
        override fun setRegistered(registered: Boolean) { this.storedRegistered = registered }
        override fun getUserName(): String? = storedName
        override fun setUserName(name: String) { this.storedName = name }
        override fun getUserGender(): String? = storedGender
        override fun setUserGender(gender: String) { this.storedGender = gender }
        override fun clear() {
            storedDevId = null
            storedRegistered = false
            storedName = null
            storedGender = null
        }
    }
}
