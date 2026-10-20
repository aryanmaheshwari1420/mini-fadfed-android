package com.example.mini_fadfed_android.data.remote

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int? = null, val message: String, val rawBody: String? = null) : ApiResult<Nothing>()
}
