package com.wujunhao.a202302010306.itemplatform.service

import android.content.Context
import android.util.Log
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.FavoriteDao
import com.wujunhao.a202302010306.itemplatform.network.CloudConfig
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class FavoriteSyncService(
    private val context: Context,
    private val databaseHelper: DatabaseHelper,
    private val favoriteDao: FavoriteDao
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val baseUrl = CloudConfig.getCurrentBaseUrl()
    
    companion object {
        private const val TAG = "FavoriteSyncService"
        private const val SYNC_BATCH_SIZE = 50
    }
    
    /**
     * 同步用户的收藏数据到云端
     */
    suspend fun syncFavoritesToCloud(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken(context)
                if (token == null) {
                    Log.e(TAG, "无法获取认证token")
                    return@withContext false
                }
                
                val localFavorites = favoriteDao.getUserFavorites(userId)
                
                if (localFavorites.isEmpty()) {
                    Log.d(TAG, "本地没有收藏数据需要同步")
                    return@withContext true
                }
                
                val jsonArray = JSONArray()
                localFavorites.forEach { productId ->
                    val jsonObject = JSONObject().apply {
                        put("productId", productId)
                        put("action", "add")
                        put("timestamp", System.currentTimeMillis())
                    }
                    jsonArray.put(jsonObject)
                }
                
                val requestBody = JSONObject().apply {
                    put("favorites", jsonArray)
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/sync/favorites")
                    .post(RequestBody.create(
                        "application/json; charset=utf-8".toMediaType(),
                        requestBody.toString()
                    ))
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseBody ->
                            val jsonResponse = JSONObject(responseBody)
                            val syncResult = jsonResponse.getJSONObject("syncResult")
                            val successCount = syncResult.getInt("success")
                            val errorCount = syncResult.getInt("errors")
                            
                            Log.d(TAG, "收藏数据同步到云端成功: 成功${successCount}条, 失败${errorCount}条")
                            updateLastSyncTime(userId)
                            true
                        } ?: false
                    } else {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "同步失败: ${response.code}, $errorBody")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "同步收藏数据到云端失败", e)
                false
            }
        }
    }
    
    /**
     * 从云端同步收藏数据到本地
     */
    suspend fun syncFavoritesFromCloud(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = TokenManager.getToken(context)
                if (token == null) {
                    Log.e(TAG, "无法获取认证token")
                    return@withContext false
                }
                
                val request = Request.Builder()
                    .url("$baseUrl/favorites")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseBody ->
                            val jsonResponse = JSONObject(responseBody)
                            val favoritesArray = jsonResponse.getJSONArray("favorites")
                            
                            processCloudFavorites(userId, favoritesArray)
                            
                            updateLastSyncTime(userId)
                            
                            Log.d(TAG, "从云端同步收藏数据成功，共${favoritesArray.length()}条")
                            true
                        } ?: false
                    } else {
                        val errorBody = response.body?.string()
                        Log.e(TAG, "获取云端收藏数据失败: ${response.code}, $errorBody")
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "从云端同步收藏数据失败", e)
                false
            }
        }
    }
    
    /**
     * 处理从云端获取的收藏数据
     */
    private suspend fun processCloudFavorites(userId: Long, favoritesArray: JSONArray) {
        withContext(Dispatchers.IO) {
            try {
                databaseHelper.writableDatabase.beginTransaction()
                
                for (i in 0 until favoritesArray.length()) {
                    val favoriteObj = favoritesArray.getJSONObject(i)
                    val productId = favoriteObj.getLong("id")
                    val favoritedAt = favoriteObj.optLong("favorited_at", System.currentTimeMillis())
                    
                    if (!favoriteDao.isProductFavorited(userId, productId)) {
                        favoriteDao.addFavorite(userId, productId, favoritedAt)
                    }
                }
                
                databaseHelper.writableDatabase.setTransactionSuccessful()
            } catch (e: Exception) {
                Log.e(TAG, "处理云端收藏数据失败", e)
                throw e
            } finally {
                databaseHelper.writableDatabase.endTransaction()
            }
        }
    }
    
    /**
     * 双向同步收藏数据
     */
    suspend fun syncFavoritesBidirectional(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 先从云端同步到本地
                val syncFromCloudSuccess = syncFavoritesFromCloud(userId)
                
                // 再将本地同步到云端
                val syncToCloudSuccess = syncFavoritesToCloud(userId)
                
                syncFromCloudSuccess && syncToCloudSuccess
            } catch (e: Exception) {
                Log.e(TAG, "双向同步收藏数据失败", e)
                false
            }
        }
    }
    
    /**
     * 获取最后同步时间
     */
    private fun getLastSyncTime(userId: Long): Long {
        val sharedPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("last_sync_time_$userId", 0L)
    }
    
    /**
     * 更新最后同步时间
     */
    private fun updateLastSyncTime(userId: Long) {
        val sharedPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_sync_time_$userId", System.currentTimeMillis()).apply()
    }
    
    /**
     * 检查是否需要同步
     */
    fun shouldSync(userId: Long): Boolean {
        val lastSyncTime = getLastSyncTime(userId)
        val currentTime = System.currentTimeMillis()
        val syncInterval = 5 * 60 * 1000 // 5分钟
        
        return currentTime - lastSyncTime > syncInterval
    }
    
    /**
     * 获取同步状态
     */
    data class SyncStatus(
        val isSyncing: Boolean,
        val lastSyncTime: Long,
        val pendingCount: Int,
        val error: String? = null
    )
    
    fun getSyncStatus(userId: Long): SyncStatus {
        val lastSyncTime = getLastSyncTime(userId)
        val pendingCount = favoriteDao.getUserFavorites(userId).size
        
        return SyncStatus(
            isSyncing = false,
            lastSyncTime = lastSyncTime,
            pendingCount = pendingCount
        )
    }
}