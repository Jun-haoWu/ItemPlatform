package com.wujunhao.a202302010306.itemplatform.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

object DatabaseContract {
    object UserEntry : BaseColumns {
        const val TABLE_NAME = "users"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PHONE = "phone"
        const val COLUMN_REAL_NAME = "real_name"
        const val COLUMN_STUDENT_ID = "student_id"
        const val COLUMN_DEPARTMENT = "department"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
    }
    
    object AuthEntry : BaseColumns {
        const val TABLE_NAME = "auth"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TOKEN = "token"
        const val COLUMN_EXPIRES_AT = "expires_at"
        const val COLUMN_CREATED_AT = "created_at"
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "ItemPlatform.db"
        const val DATABASE_VERSION = 1
        
        private const val SQL_CREATE_USERS_TABLE = """
            CREATE TABLE ${DatabaseContract.UserEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.UserEntry.COLUMN_USERNAME} TEXT UNIQUE NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_PASSWORD} TEXT NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_EMAIL} TEXT UNIQUE NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_PHONE} TEXT NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_REAL_NAME} TEXT NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_STUDENT_ID} TEXT NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_DEPARTMENT} TEXT NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_CREATED_AT} INTEGER NOT NULL,
                ${DatabaseContract.UserEntry.COLUMN_UPDATED_AT} INTEGER NOT NULL
            )
        """
        
        private const val SQL_CREATE_AUTH_TABLE = """
            CREATE TABLE ${DatabaseContract.AuthEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.AuthEntry.COLUMN_USER_ID} INTEGER NOT NULL,
                ${DatabaseContract.AuthEntry.COLUMN_TOKEN} TEXT UNIQUE NOT NULL,
                ${DatabaseContract.AuthEntry.COLUMN_EXPIRES_AT} INTEGER NOT NULL,
                ${DatabaseContract.AuthEntry.COLUMN_CREATED_AT} INTEGER NOT NULL,
                FOREIGN KEY(${DatabaseContract.AuthEntry.COLUMN_USER_ID}) 
                REFERENCES ${DatabaseContract.UserEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """
        
        private const val SQL_DELETE_USERS_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.UserEntry.TABLE_NAME}"
        private const val SQL_DELETE_AUTH_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.AuthEntry.TABLE_NAME}"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_USERS_TABLE)
        db.execSQL(SQL_CREATE_AUTH_TABLE)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_AUTH_TABLE)
        db.execSQL(SQL_DELETE_USERS_TABLE)
        onCreate(db)
    }
    
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}