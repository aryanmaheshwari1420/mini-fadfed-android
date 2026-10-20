package com.example.mini_fadfed_android.core.identity

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsIdentityStorage(context: Context) : IdentityStorage {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun getDevId(): String? = prefs.getString(KEY_DEV_ID, null)

    override fun saveDevId(devId: String) {
        prefs.edit().putString(KEY_DEV_ID, devId).apply()
    }

    override fun isRegistered(): Boolean = prefs.getBoolean(KEY_IS_REGISTERED, false)

    override fun setRegistered(registered: Boolean) {
        prefs.edit().putBoolean(KEY_IS_REGISTERED, registered).apply()
    }

    override fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    override fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    override fun getUserGender(): String? = prefs.getString(KEY_USER_GENDER, null)

    override fun setUserGender(gender: String) {
        prefs.edit().putString(KEY_USER_GENDER, gender).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "mini_fadfed_identity_prefs"
        private const val KEY_DEV_ID = "pref_dev_id"
        private const val KEY_IS_REGISTERED = "pref_is_registered"
        private const val KEY_USER_NAME = "pref_user_name"
        private const val KEY_USER_GENDER = "pref_user_gender"
    }
}
