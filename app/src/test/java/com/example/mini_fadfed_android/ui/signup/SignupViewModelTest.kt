package com.example.mini_fadfed_android.ui.signup

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.example.mini_fadfed_android.core.identity.IdentityStorage
import com.example.mini_fadfed_android.data.remote.ApiResult
import com.example.mini_fadfed_android.data.remote.FadfedRestClient
import com.example.mini_fadfed_android.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var fakeStorage: FakeStorage
    private lateinit var identityManager: IdentityManager
    private lateinit var restClient: FadfedRestClient
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: SignupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()

        fakeStorage = FakeStorage()
        identityManager = IdentityManager(fakeStorage, initialSessionId = "session-test")
        restClient = FadfedRestClient(
            identityManager = identityManager,
            baseUrl = server.url("").toString().removeSuffix("/"),
            ioDispatcher = testDispatcher
        )
        repository = AuthRepository(identityManager, restClient)
        viewModel = SignupViewModel(repository)
    }

    @After
    fun tearDown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_defaultsCorrectly() {
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("m", state.gender)
        assertFalse(state.isLoading)
        assertFalse(state.isRegistered)
    }

    @Test
    fun onContinueClicked_showsErrorIfNameIsBlank() = runTest(testDispatcher) {
        viewModel.onNameChanged("   ")
        viewModel.onContinueClicked()

        val state = viewModel.uiState.value
        assertEquals("Please enter your name.", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun onContinueClicked_successUpdatesIsRegistered() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"success": true}"""))

        viewModel.onNameChanged("Sara")
        viewModel.onGenderChanged("f")
        viewModel.onContinueClicked()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isRegistered)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun onContinueClicked_handlesServerError() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(
            MockResponse()
                .setResponseCode(406)
                .setBody("""{"success": false, "error": "violation", "message": "bad_name"}""")
        )

        viewModel.onNameChanged("badname")
        viewModel.onContinueClicked()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isRegistered)
        assertNotNull(state.errorMessage)
    }

    private class FakeStorage : IdentityStorage {
        private var storedDevId: String? = "device-123"
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
