package com.example.mini_fadfed_android.data.remote

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FadfedRestClientTest {

    private lateinit var server: MockWebServer
    private lateinit var identityManager: IdentityManager
    private lateinit var restClient: FadfedRestClient

    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()

        val fakeStorage = object : IdentityStorage {
            private var devId: String? = "android-test-device-uuid"
            private var registered = false
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

        identityManager = IdentityManager(fakeStorage, initialSessionId = "test-session-uuid")
        restClient = FadfedRestClient(
            identityManager = identityManager,
            baseUrl = server.url("").toString().removeSuffix("/"),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun createAnonymousAccount_sendsCorrectHeadersAndMethod() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val result = restClient.createAnonymousAccount()

        assertTrue(result is ApiResult.Success)
        val recordedRequest = server.takeRequest()

        assertEquals("PUT", recordedRequest.method)
        assertEquals("/api/@fadfedx/auth", recordedRequest.path)
        assertEquals("Fadfed/2.4.3 Android/14", recordedRequest.getHeader("User-Agent"))
        assertTrue(recordedRequest.getHeader("Content-Type")?.startsWith("application/json") == true)
        assertEquals("test-session-uuid", recordedRequest.getHeader("x-session-id"))
        assertTrue(recordedRequest.getHeader("Authorization")?.startsWith("Basic ") == true)
        assertEquals("{}", recordedRequest.body.readUtf8())
    }

    @Test
    fun setProfile_sendsExpectedJsonBody() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success": true, "message": "user updated successfully"}""")
        )

        val result = restClient.setProfile(name = "Sara", gender = "f")

        assertTrue(result is ApiResult.Success)
        val recordedRequest = server.takeRequest()

        assertEquals("PUT", recordedRequest.method)
        assertEquals("/api/@fadfedx/user", recordedRequest.path)
        val body = recordedRequest.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Sara\""))
        assertTrue(body.contains("\"gender\":\"f\""))
    }

    @Test
    fun setProfile_handles406BadNameViolation() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(406)
                .setBody("""{"success": false, "error": "violation", "field": "name", "message": "bad_name"}""")
        )

        val result = restClient.setProfile(name = "badword", gender = "m")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(406, error.code)
        assertTrue(error.message.contains("not allowed", ignoreCase = true))
    }

    @Test
    fun setProfile_handles429NameCooldown() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"message": "name_cooldown"}""")
        )

        val result = restClient.setProfile(name = "John", gender = "m")

        assertTrue(result is ApiResult.Error)
        val error = result as ApiResult.Error
        assertEquals(429, error.code)
        assertTrue(error.message.contains("cooldown") || error.message.contains("frequently"))
    }
}
