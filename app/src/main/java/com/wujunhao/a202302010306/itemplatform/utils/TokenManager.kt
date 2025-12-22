package com.wujunhao.a202302010306.itemplatform.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.Date

object TokenManager {
    private const val PREF_NAME = "ItemPlatformPrefs"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_ID_KEY = "user_id"
    private const val USERNAME_KEY = "username"
    private const val EMAIL_KEY = "email"
    private const val TOKEN_EXPIRY_KEY = "token_expiry"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveToken(context: Context, token: String, userId: Long = 0L, username: String? = null, email: String? = null, expiryIn: Long = 0L) {
        val prefs = getSharedPreferences(context)
        val expiryTime = if (expiryIn > 0) {
            System.currentTimeMillis() + (expiryIn * 1000)
        } else {
            System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000) // 默认7天过期
        }
        
        prefs.edit()
            .putString(TOKEN_KEY, token)
            .putLong(USER_ID_KEY, userId)
            .putString(USERNAME_KEY, username)
            .putString(EMAIL_KEY, email)
            .putLong(TOKEN_EXPIRY_KEY, expiryTime)
            .apply()
    }
    
    fun getToken(context: Context): String? {
        return getSharedPreferences(context).getString(TOKEN_KEY, null)
    }
    
    fun getUserId(context: Context): Long {
        return getSharedPreferences(context).getLong(USER_ID_KEY, 0L)
    }
    
    fun getUsername(context: Context): String? {
        return getSharedPreferences(context).getString(USERNAME_KEY, null)
    }
    
    fun getEmail(context: Context): String? {
        return getSharedPreferences(context).getString(EMAIL_KEY, null)
    }
    
    fun isTokenValid(context: Context): Boolean {
        val token = getToken(context) ?: return false
        val expiryTime = getSharedPreferences(context).getLong(TOKEN_EXPIRY_KEY, 0L)
        
        return token.isNotEmpty() && System.currentTimeMillis() < expiryTime
    }
    
    fun clearToken(context: Context) {
        getSharedPreferences(context).edit()
            .remove(TOKEN_KEY)
            .remove(USER_ID_KEY)
            .remove(USERNAME_KEY)
            .remove(EMAIL_KEY)
            .remove(TOKEN_EXPIRY_KEY)
            .apply()
    }
}