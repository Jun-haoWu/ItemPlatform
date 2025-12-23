package com.wujunhao.a202302010306.itemplatform.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.wujunhao.a202302010306.itemplatform.model.User
import java.util.Date

data class LocalUser(
    val id: Long = 0,
    val username: String,
    val password: String,
    val email: String,
    val phone: String,
    val realName: String,
    val studentId: String,
    val department: String,
    val avatar: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toUser(): User {
        return User(
            id = id,
            username = username,
            email = email,
            phone = phone,
            realName = realName,
            studentId = studentId,
            department = department,
            avatar = avatar,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }
}

class UserDao(private val databaseHelper: DatabaseHelper) {
    
    fun insertUser(user: LocalUser): Long {
        val db = databaseHelper.writableDatabase
        
        val values = ContentValues().apply {
            put(DatabaseContract.UserEntry.COLUMN_USERNAME, user.username)
            put(DatabaseContract.UserEntry.COLUMN_PASSWORD, user.password)
            put(DatabaseContract.UserEntry.COLUMN_EMAIL, user.email)
            put(DatabaseContract.UserEntry.COLUMN_PHONE, user.phone)
            put(DatabaseContract.UserEntry.COLUMN_REAL_NAME, user.realName)
            put(DatabaseContract.UserEntry.COLUMN_STUDENT_ID, user.studentId)
            put(DatabaseContract.UserEntry.COLUMN_DEPARTMENT, user.department)
            put(DatabaseContract.UserEntry.COLUMN_AVATAR, user.avatar)
            put(DatabaseContract.UserEntry.COLUMN_CREATED_AT, user.createdAt)
            put(DatabaseContract.UserEntry.COLUMN_UPDATED_AT, user.updatedAt)
        }
        //实际保存数据到数据库的位置
        return db.insert(DatabaseContract.UserEntry.TABLE_NAME, null, values)
    }
    
    fun getUserByUsername(username: String): LocalUser? {
        val db = databaseHelper.readableDatabase
        
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.UserEntry.COLUMN_USERNAME,
            DatabaseContract.UserEntry.COLUMN_PASSWORD,
            DatabaseContract.UserEntry.COLUMN_EMAIL,
            DatabaseContract.UserEntry.COLUMN_PHONE,
            DatabaseContract.UserEntry.COLUMN_REAL_NAME,
            DatabaseContract.UserEntry.COLUMN_STUDENT_ID,
            DatabaseContract.UserEntry.COLUMN_DEPARTMENT,
            DatabaseContract.UserEntry.COLUMN_AVATAR,
            DatabaseContract.UserEntry.COLUMN_CREATED_AT,
            DatabaseContract.UserEntry.COLUMN_UPDATED_AT
        )
        
        val selection = "${DatabaseContract.UserEntry.COLUMN_USERNAME} = ?"
        val selectionArgs = arrayOf(username)
        
        val cursor = db.query(
            DatabaseContract.UserEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        return if (cursor.moveToFirst()) {
            cursorToUser(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }
    
    fun getUserById(userId: Long): LocalUser? {
        val db = databaseHelper.readableDatabase
        
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.UserEntry.COLUMN_USERNAME,
            DatabaseContract.UserEntry.COLUMN_PASSWORD,
            DatabaseContract.UserEntry.COLUMN_EMAIL,
            DatabaseContract.UserEntry.COLUMN_PHONE,
            DatabaseContract.UserEntry.COLUMN_REAL_NAME,
            DatabaseContract.UserEntry.COLUMN_STUDENT_ID,
            DatabaseContract.UserEntry.COLUMN_DEPARTMENT,
            DatabaseContract.UserEntry.COLUMN_AVATAR,
            DatabaseContract.UserEntry.COLUMN_CREATED_AT,
            DatabaseContract.UserEntry.COLUMN_UPDATED_AT
        )
        
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf(userId.toString())
        
        val cursor = db.query(
            DatabaseContract.UserEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        return if (cursor.moveToFirst()) {
            cursorToUser(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }
    
    fun verifyUser(username: String, password: String): LocalUser? {
        val db = databaseHelper.readableDatabase
        
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.UserEntry.COLUMN_USERNAME,
            DatabaseContract.UserEntry.COLUMN_PASSWORD,
            DatabaseContract.UserEntry.COLUMN_EMAIL,
            DatabaseContract.UserEntry.COLUMN_PHONE,
            DatabaseContract.UserEntry.COLUMN_REAL_NAME,
            DatabaseContract.UserEntry.COLUMN_STUDENT_ID,
            DatabaseContract.UserEntry.COLUMN_DEPARTMENT,
            DatabaseContract.UserEntry.COLUMN_AVATAR,
            DatabaseContract.UserEntry.COLUMN_CREATED_AT,
            DatabaseContract.UserEntry.COLUMN_UPDATED_AT
        )
        
        val selection = "${DatabaseContract.UserEntry.COLUMN_USERNAME} = ? AND ${DatabaseContract.UserEntry.COLUMN_PASSWORD} = ?"
        val selectionArgs = arrayOf(username, password)
        
        val cursor = db.query(
            DatabaseContract.UserEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        return if (cursor.moveToFirst()) {
            cursorToUser(cursor)
        } else {
            null
        }.also {
            cursor.close()
        }
    }
    
    fun isUsernameExists(username: String): Boolean {
        val db = databaseHelper.readableDatabase
        
        val projection = arrayOf(BaseColumns._ID)
        val selection = "${DatabaseContract.UserEntry.COLUMN_USERNAME} = ?"
        val selectionArgs = arrayOf(username)
        
        val cursor = db.query(
            DatabaseContract.UserEntry.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
    
    fun getAllUsers(): List<LocalUser> {
        val db = databaseHelper.readableDatabase
        val users = mutableListOf<LocalUser>()
        
        val projection = arrayOf(
            BaseColumns._ID,
            DatabaseContract.UserEntry.COLUMN_USERNAME,
            DatabaseContract.UserEntry.COLUMN_PASSWORD,
            DatabaseContract.UserEntry.COLUMN_EMAIL,
            DatabaseContract.UserEntry.COLUMN_PHONE,
            DatabaseContract.UserEntry.COLUMN_REAL_NAME,
            DatabaseContract.UserEntry.COLUMN_STUDENT_ID,
            DatabaseContract.UserEntry.COLUMN_DEPARTMENT,
            DatabaseContract.UserEntry.COLUMN_AVATAR,
            DatabaseContract.UserEntry.COLUMN_CREATED_AT,
            DatabaseContract.UserEntry.COLUMN_UPDATED_AT
        )
        
        val cursor = db.query(
            DatabaseContract.UserEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            "${DatabaseContract.UserEntry.COLUMN_CREATED_AT} DESC"
        )
        
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor))
        }
        cursor.close()
        
        return users
    }
    
    fun updateUser(user: User): Boolean {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.UserEntry.COLUMN_USERNAME, user.username)
            put(DatabaseContract.UserEntry.COLUMN_PHONE, user.phone)
            put(DatabaseContract.UserEntry.COLUMN_REAL_NAME, user.realName)
            put(DatabaseContract.UserEntry.COLUMN_DEPARTMENT, user.department)
            put(DatabaseContract.UserEntry.COLUMN_AVATAR, user.avatar)
            put(DatabaseContract.UserEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        
        val rowsAffected = db.update(
            DatabaseContract.UserEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(user.id.toString())
        )
        
        return rowsAffected > 0
    }

    private fun cursorToUser(cursor: Cursor): LocalUser {
        return LocalUser(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
            username = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_USERNAME)),
            password = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_PASSWORD)),
            email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_EMAIL)),
            phone = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_PHONE)),
            realName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_REAL_NAME)),
            studentId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_STUDENT_ID)),
            department = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_DEPARTMENT)),
            avatar = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_AVATAR)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.UserEntry.COLUMN_UPDATED_AT))
        )
    }
}