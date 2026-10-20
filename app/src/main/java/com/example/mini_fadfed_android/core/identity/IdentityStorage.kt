package com.example.mini_fadfed_android.core.identity

interface IdentityStorage {
    fun getDevId(): String?
    fun saveDevId(devId: String)

    fun isRegistered(): Boolean
    fun setRegistered(registered: Boolean)

    fun getUserName(): String?
    fun setUserName(name: String)

    fun getUserGender(): String?
    fun setUserGender(gender: String)

    fun clear()
}
