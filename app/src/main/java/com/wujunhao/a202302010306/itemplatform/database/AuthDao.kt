package com.wujunhao.a202302010306.itemplatform.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import java.util.Date

data class AuthToken(
    val id: Long = 0,
    val userId: Long,
    val token: String,
    val expiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

class AuthDao(private val databaseHelper: DatabaseHelper) {
    
    fun insertAuthToken(authToken: AuthToken): Long {
        val db = databaseHelper.writableDatabase
        
        // 先清除之前的token
        clearAllTokens()
        
        val values = ContentValues().apply {
            put(DatabaseContract.AuthEntry.COLUMN_USER_ID, authToken.userId)
            put(DatabaseContract.AuthEntry.COLUMN_TOKEN, authToken.token)
            put(DatabaseContract.AuthEntry.COLUMN_EXPIRES_AT, authToken.expiresAt)
            put(DatabaseContract.AuthEntry.COLUMN_CREATED_AT, authToken.createdAt)
        }
        
        return db.insert(DatabaseContract.AuthEntry.TABLE_NAME, null, values)
    }
    
    fun getCurrentAuthToken(): AuthToken? {
        val db = databaseHelper.readableDatabase
        
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.AuthEntry.COLUMN_USER_ID,
            DatabaseContract.AuthEntry.COLUMN_TOKEN,
            DatabaseContract.AuthEntry.COLUMN_EXPIRES_AT,
            DatabaseContract.AuthEntry.COLUMN_CREATED_AT
        )
        
        val sortOrder = "${DatabaseContract.AuthEntry.COLUMN_CREATED_AT} DESC"
        val limit = "1"
        
        val cursor = db.query(
            DatabaseContract.AuthEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            sortOrder,
            limit
        )
        
        return if (cursor.moveToFirst()) {
            cursorToAuthToken(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }
    
    fun isTokenValid(): Boolean {
        val currentToken = getCurrentAuthToken()
        return currentToken?.let { token ->
            System.currentTimeMillis() < token.expiresAt
        } ?: false
    }
    
    fun getCurrentUserId(): Long? {
        return getCurrentAuthToken()?.userId
    }
    
    fun getCurrentToken(): String? {
        return getCurrentAuthToken()?.token
    }
    
    fun clearAllTokens() {
        val db = databaseHelper.writableDatabase
        db.delete(DatabaseContract.AuthEntry.TABLE_NAME, null, null)
    }
    
    fun clearExpiredTokens() {
        val db = databaseHelper.writableDatabase
        val currentTime = System.currentTimeMillis()
        val selection = "${DatabaseContract.AuthEntry.COLUMN_EXPIRES_AT} < ?"
        val selectionArgs = arrayOf(currentTime.toString())
        
        db.delete(DatabaseContract.AuthEntry.TABLE_NAME, selection, selectionArgs)
    }
    
    private fun cursorToAuthToken(cursor: Cursor): AuthToken {
        return AuthToken(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.AuthEntry.COLUMN_USER_ID)),
            token = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.AuthEntry.COLUMN_TOKEN)),
            expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.AuthEntry.COLUMN_EXPIRES_AT)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.AuthEntry.COLUMN_CREATED_AT))
        )
    }
}