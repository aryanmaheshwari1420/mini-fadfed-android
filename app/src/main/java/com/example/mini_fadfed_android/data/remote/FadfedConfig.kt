package com.example.mini_fadfed_android.data.remote

object FadfedConfig {
    const val DEFAULT_BASE_URL = "https://dev.wefaaq.net"
    const val DEFAULT_WS_BASE_URL = "wss://dev.wefaaq.net"
    const val TENANT = "fadfedx"
    const val REST_PREFIX = "/api/@$TENANT"
    const val WS_PATH = "/@$TENANT"
    const val WS_SUBPROTOCOL = "v2_2.fadfedly.com"
    const val USER_AGENT = "Fadfed/2.4.3 Android/14"
}
