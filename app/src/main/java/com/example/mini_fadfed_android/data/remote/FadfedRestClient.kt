package com.example.mini_fadfed_android.data.remote

import com.example.mini_fadfed_android.core.identity.IdentityManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class FadfedRestClient(
    private val identityManager: IdentityManager,
    private val baseUrl: String = FadfedConfig.DEFAULT_BASE_URL,
    customClient: OkHttpClient? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient = customClient ?: OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestWithHeaders = original.newBuilder()
                .header("User-Agent", FadfedConfig.USER_AGENT)
                .header("Content-Type", "application/json")
                .header("x-session-id", identityManager.sessionId)
                .header("Authorization", identityManager.getAuthorizationHeader())
                .build()
            chain.proceed(requestWithHeaders)
        }
        .build()

    /**
     * PUT /api/@fadfedx/auth
     * Creates an anonymous user account with empty JSON body {}.
     */
    suspend fun createAnonymousAccount(): ApiResult<Unit> = withContext(ioDispatcher) {
        val url = "$baseUrl${FadfedConfig.REST_PREFIX}/auth"
        val requestBody = "{}".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        executeRequest(request)
    }

    /**
     * PUT /api/@fadfedx/user
     * Updates user profile with name and gender ("m" or "f").
     */
    suspend fun setProfile(name: String, gender: String): ApiResult<Unit> = withContext(ioDispatcher) {
        val url = "$baseUrl${FadfedConfig.REST_PREFIX}/user"
        val payload = JsonObject().apply {
            addProperty("name", name)
            addProperty("gender", gender)
        }
        val requestBody = payload.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        executeRequest(request)
    }

    private fun executeRequest(request: Request): ApiResult<Unit> {
        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    val parsedMessage = parseErrorMessage(response.code, responseBody)
                    ApiResult.Error(
                        code = response.code,
                        message = parsedMessage,
                        rawBody = responseBody
                    )
                }
            }
        } catch (e: IOException) {
            ApiResult.Error(
                code = null,
                message = "Network error: ${e.localizedMessage ?: "Unable to reach server"}"
            )
        } catch (e: Exception) {
            ApiResult.Error(
                code = null,
                message = "Unexpected error: ${e.localizedMessage ?: "Something went wrong"}"
            )
        }
    }

    private fun parseErrorMessage(code: Int, responseBody: String): String {
        return try {
            val json = JsonParser.parseString(responseBody).asJsonObject
            when (code) {
                406 -> {
                    val message = json.get("message")?.asString
                    if (message == "bad_name") {
                        "This name is not allowed or violates community standards."
                    } else {
                        "Invalid profile data (406)."
                    }
                }
                429 -> {
                    val message = json.get("message")?.asString
                    if (message == "name_cooldown") {
                        "Name was updated too frequently. Please wait before updating again."
                    } else {
                        "Too many requests. Please wait a moment."
                    }
                }
                404 -> "User account does not exist. Please re-register."
                422 -> "Authentication or format error (422). Please restart the app."
                else -> json.get("message")?.asString ?: "Server returned error ($code)"
            }
        } catch (e: Exception) {
            when (code) {
                406 -> "This name is not allowed or violates community standards."
                429 -> "Too many requests. Please try again later."
                404 -> "User account does not exist."
                422 -> "Authentication error (422)."
                else -> "Server error ($code)."
            }
        }
    }
}
