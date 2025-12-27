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
        const val COLUMN_AVATAR = "avatar"
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
    
    object ProductEntry : BaseColumns {
        const val TABLE_NAME = "products"
        const val COLUMN_TITLE = "title"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_PRICE = "price"
        const val COLUMN_CATEGORY = "category"
        const val COLUMN_CONDITION = "condition"
        const val COLUMN_LOCATION = "location"
        const val COLUMN_IMAGES = "images"
        const val COLUMN_SELLER_ID = "seller_id"
        const val COLUMN_STATUS = "status"
        const val COLUMN_VIEW_COUNT = "view_count"
        const val COLUMN_LIKE_COUNT = "like_count"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
    }
    
    object FavoriteEntry : BaseColumns {
        const val TABLE_NAME = "favorites"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_PRODUCT_ID = "product_id"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_IS_SYNCED = "is_synced"  // 同步状态
        const val COLUMN_SYNC_ACTION = "sync_action"  // 同步操作: add/remove
    }
}

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val DATABASE_NAME = "ItemPlatform.db"
        const val DATABASE_VERSION = 3
        
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
                ${DatabaseContract.UserEntry.COLUMN_AVATAR} TEXT,
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
        
        private const val SQL_CREATE_PRODUCTS_TABLE = """
            CREATE TABLE ${DatabaseContract.ProductEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.ProductEntry.COLUMN_TITLE} TEXT NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_DESCRIPTION} TEXT NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_PRICE} REAL NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_CATEGORY} TEXT NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_CONDITION} TEXT NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_LOCATION} TEXT NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_IMAGES} TEXT,
                ${DatabaseContract.ProductEntry.COLUMN_SELLER_ID} INTEGER NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_STATUS} INTEGER NOT NULL DEFAULT 0,
                ${DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT} INTEGER NOT NULL DEFAULT 0,
                ${DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT} INTEGER NOT NULL DEFAULT 0,
                ${DatabaseContract.ProductEntry.COLUMN_CREATED_AT} INTEGER NOT NULL,
                ${DatabaseContract.ProductEntry.COLUMN_UPDATED_AT} INTEGER NOT NULL,
                FOREIGN KEY(${DatabaseContract.ProductEntry.COLUMN_SELLER_ID}) 
                REFERENCES ${DatabaseContract.UserEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """
        
        private const val SQL_CREATE_FAVORITES_TABLE = """
            CREATE TABLE ${DatabaseContract.FavoriteEntry.TABLE_NAME} (
                ${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} INTEGER NOT NULL,
                ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} INTEGER NOT NULL,
                ${DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT} INTEGER NOT NULL,
                ${DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT} INTEGER NOT NULL,
                ${DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED} INTEGER NOT NULL DEFAULT 0,
                ${DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION} TEXT,
                UNIQUE(${DatabaseContract.FavoriteEntry.COLUMN_USER_ID}, ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID}),
                FOREIGN KEY(${DatabaseContract.FavoriteEntry.COLUMN_USER_ID}) 
                REFERENCES ${DatabaseContract.UserEntry.TABLE_NAME}(${BaseColumns._ID}),
                FOREIGN KEY(${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID}) 
                REFERENCES ${DatabaseContract.ProductEntry.TABLE_NAME}(${BaseColumns._ID})
            )
        """
        
        private const val SQL_DELETE_USERS_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.UserEntry.TABLE_NAME}"
        private const val SQL_DELETE_AUTH_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.AuthEntry.TABLE_NAME}"
        private const val SQL_DELETE_PRODUCTS_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.ProductEntry.TABLE_NAME}"
        private const val SQL_DELETE_FAVORITES_TABLE = "DROP TABLE IF EXISTS ${DatabaseContract.FavoriteEntry.TABLE_NAME}"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_USERS_TABLE)
        db.execSQL(SQL_CREATE_AUTH_TABLE)
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE)
        db.execSQL(SQL_CREATE_FAVORITES_TABLE)
    }
    
    fun ensureProductsTableExists() {
        val db = writableDatabase
        try {
            // Check if products table exists
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(DatabaseContract.ProductEntry.TABLE_NAME)
            )
            if (!cursor.moveToFirst()) {
                // Table doesn't exist, create it
                db.execSQL(SQL_CREATE_PRODUCTS_TABLE)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle migration from version 1 to 2 (add avatar column)
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE ${DatabaseContract.UserEntry.TABLE_NAME} ADD COLUMN ${DatabaseContract.UserEntry.COLUMN_AVATAR} TEXT")
            } catch (e: Exception) {
                // Column might already exist, ignore error
                e.printStackTrace()
            }
        }
        
        // Handle migration from version 2 to 3 (add favorites table)
        if (oldVersion < 3) {
            try {
                db.execSQL(SQL_CREATE_FAVORITES_TABLE)
            } catch (e: Exception) {
                // Table might already exist, ignore error
                e.printStackTrace()
            }
        }
    }
    
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}