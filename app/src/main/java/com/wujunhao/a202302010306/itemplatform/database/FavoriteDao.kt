package com.wujunhao.a202302010306.itemplatform.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns

class FavoriteDao(private val databaseHelper: DatabaseHelper) {
    
    fun addFavorite(userId: Long, productId: Long, createdAt: Long): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseContract.FavoriteEntry.COLUMN_USER_ID, userId)
                put(DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID, productId)
                put(DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT, createdAt)
                put(DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT, createdAt)
                put(DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED, 0) // 未同步
                put(DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION, "add")
            }
            
            val result = db.insertWithOnConflict(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            result != -1L
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "添加收藏失败", e)
            false
        }
    }
    
    fun removeFavorite(userId: Long, productId: Long): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            
            // 先检查是否存在
            val existing = getFavoriteByProduct(userId, productId)
            if (existing != null) {
                // 更新为删除状态
                val values = ContentValues().apply {
                    put(DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED, 0) // 未同步
                    put(DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION, "remove")
                    put(DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
                }
                
                val result = db.update(
                    DatabaseContract.FavoriteEntry.TABLE_NAME,
                    values,
                    "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ?",
                    arrayOf(userId.toString(), productId.toString())
                )
                result > 0
            } else {
                // 如果不存在，直接删除（理论上不应该发生）
                val result = db.delete(
                    DatabaseContract.FavoriteEntry.TABLE_NAME,
                    "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ?",
                    arrayOf(userId.toString(), productId.toString())
                )
                result > 0
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "删除收藏失败", e)
            false
        }
    }
    
    fun isProductFavorited(userId: Long, productId: Long): Boolean {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                arrayOf(BaseColumns._ID),
                "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION} != 'remove'",
                arrayOf(userId.toString(), productId.toString()),
                null,
                null,
                null
            )
            
            cursor.use {
                it.count > 0
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "检查收藏状态失败", e)
            false
        }
    }
    
    fun getUserFavorites(userId: Long): List<Long> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                arrayOf(DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID),
                "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION} != 'remove'",
                arrayOf(userId.toString()),
                null,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT} DESC"
            )
            
            cursor.use {
                val productIds = mutableListOf<Long>()
                while (it.moveToNext()) {
                    val productId = it.getLong(it.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID))
                    productIds.add(productId)
                }
                productIds
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "获取用户收藏列表失败", e)
            emptyList()
        }
    }
    
    fun getFavoriteCountByProduct(productId: Long): Int {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                arrayOf("COUNT(*)"),
                "${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION} != 'remove'",
                arrayOf(productId.toString()),
                null,
                null,
                null
            )
            
            cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(0)
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "获取商品收藏数失败", e)
            0
        }
    }
    
    fun clearUserFavorites(userId: Long): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            val result = db.delete(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ?",
                arrayOf(userId.toString())
            )
            result > 0
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "清空用户收藏失败", e)
            false
        }
    }
    
    // 同步相关方法
    
    fun getUnsyncedFavorites(): List<FavoriteEntity> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED} = 0",
                null,
                null,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT} ASC"
            )
            
            cursor.use {
                val favorites = mutableListOf<FavoriteEntity>()
                while (it.moveToNext()) {
                    val favorite = cursorToFavoriteEntity(it)
                    favorites.add(favorite)
                }
                favorites
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "获取未同步收藏失败", e)
            emptyList()
        }
    }
    
    fun markAsSynced(productId: Long): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED, 1)
            }
            
            val result = db.update(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                values,
                "${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ?",
                arrayOf(productId.toString())
            )
            result > 0
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "标记同步状态失败", e)
            false
        }
    }
    
    fun updateFavoriteStatus(productId: Long, isFavorited: Boolean): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION, if (isFavorited) "add" else "remove")
                put(DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
            }
            
            val result = db.update(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                values,
                "${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ?",
                arrayOf(productId.toString())
            )
            result > 0
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "更新收藏状态失败", e)
            false
        }
    }
    
    fun getAllFavoritesSync(): List<FavoriteEntity> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION} != 'remove'",
                null,
                null,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT} DESC"
            )
            
            cursor.use {
                val favorites = mutableListOf<FavoriteEntity>()
                while (it.moveToNext()) {
                    val favorite = cursorToFavoriteEntity(it)
                    favorites.add(favorite)
                }
                favorites
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "获取所有收藏失败", e)
            emptyList()
        }
    }
    
    fun insertFavorite(favorite: FavoriteEntity): Boolean {
        return try {
            val db = databaseHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseContract.FavoriteEntry.COLUMN_USER_ID, favorite.userId)
                put(DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID, favorite.productId)
                put(DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT, favorite.createdAt)
                put(DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT, favorite.updatedAt)
                put(DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED, if (favorite.isSynced) 1 else 0)
                put(DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION, favorite.syncAction)
            }
            
            val result = db.insertWithOnConflict(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            result != -1L
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "插入收藏失败", e)
            false
        }
    }
    
    private fun getFavoriteByProduct(userId: Long, productId: Long): FavoriteEntity? {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.FavoriteEntry.TABLE_NAME,
                null,
                "${DatabaseContract.FavoriteEntry.COLUMN_USER_ID} = ? AND ${DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID} = ?",
                arrayOf(userId.toString(), productId.toString()),
                null,
                null,
                null
            )
            
            cursor.use {
                if (it.moveToFirst()) {
                    cursorToFavoriteEntity(it)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FavoriteDao", "获取收藏失败", e)
            null
        }
    }
    
    private fun cursorToFavoriteEntity(cursor: Cursor): FavoriteEntity {
        return FavoriteEntity(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_USER_ID)),
            productId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_PRODUCT_ID)),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_CREATED_AT)),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_UPDATED_AT)),
            isSynced = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_IS_SYNCED)) == 1,
            syncAction = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.FavoriteEntry.COLUMN_SYNC_ACTION))
        )
    }
}