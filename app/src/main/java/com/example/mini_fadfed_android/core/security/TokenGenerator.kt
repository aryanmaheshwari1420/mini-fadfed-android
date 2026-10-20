package com.example.mini_fadfed_android.core.security

import java.math.BigInteger
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Generates the FadFed HMAC-MD5 authentication token.
 * Formula: HMAC-MD5(key = session-id, message = devid)
 * Output: 32 lowercase hexadecimal characters.
 *
 * Keep this independent of Android framework classes to facilitate direct unit testing.
 */
object TokenGenerator {
    fun makeToken(sessionId: String, devid: String): String {
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(sessionId.toByteArray(Charsets.UTF_8), "HmacMD5"))
        val bytes = mac.doFinal(devid.toByteArray(Charsets.UTF_8))
        return String.format(Locale.ROOT, "%032x", BigInteger(1, bytes))
    }
}
