package com.wujunhao.a202302010306.itemplatform.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.util.Log
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.FavoriteDao
import com.wujunhao.a202302010306.itemplatform.database.FavoriteEntity
import com.wujunhao.a202302010306.itemplatform.model.SyncFavoriteItem
import com.wujunhao.a202302010306.itemplatform.model.SyncFavoritesRequest
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.network.CloudConfig
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 云同步管理器
 * 负责处理本地数据与云端数据的同步
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    
    // 同步状态
    enum class SyncStatus {
        IDLE,
        SYNCING,
        SUCCESS,
        FAILED,
        NETWORK_ERROR,
        AUTH_ERROR
    }
    
    // 同步监听器
    interface SyncListener {
        fun onSyncStart()
        fun onSyncSuccess(changes: Int)
        fun onSyncFailed(error: String)
        fun onSyncStatusChanged(status: SyncStatus)
    }
    
    private var syncListener: SyncListener? = null
    private var currentStatus: SyncStatus = SyncStatus.IDLE
    
    /**
     * 设置同步监听器
     */
    fun setSyncListener(listener: SyncListener?) {
        syncListener = listener
    }
    
    /**
     * 获取当前同步状态
     */
    fun getCurrentStatus(): SyncStatus = currentStatus
    
    /**
     * 执行完整的数据同步
     */
    suspend fun performFullSync(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!shouldSync(context)) {
            Log.d(TAG, "不满足同步条件，跳过同步")
            return@withContext false
        }
        
        updateStatus(SyncStatus.SYNCING)
        syncListener?.onSyncStart()
        
        try {
            val success = syncFavorites(context)
            if (success) {
                updateStatus(SyncStatus.SUCCESS)
                syncListener?.onSyncSuccess(0) // TODO: 返回实际的变更数量
            } else {
                updateStatus(SyncStatus.FAILED)
                syncListener?.onSyncFailed("同步失败")
            }
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "同步异常", e)
            val errorType = when (e) {
                is IOException -> SyncStatus.NETWORK_ERROR
                is HttpException -> if (e.code() == 401) SyncStatus.AUTH_ERROR else SyncStatus.FAILED
                else -> SyncStatus.FAILED
            }
            updateStatus(errorType)
            syncListener?.onSyncFailed(e.message ?: "未知错误")
            return@withContext false
        }
    }
    
    /**
     * 同步收藏数据
     */
    private suspend fun syncFavorites(context: Context): Boolean {
        val databaseHelper = DatabaseHelper(context)
        val favoriteDao = FavoriteDao(databaseHelper)
        val apiService = ApiClient.createApiService(context)
        val token = TokenManager.getToken(context)
        
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "用户未登录，跳过同步")
            return false
        }
        
        // 获取当前用户ID（这里需要根据实际情况获取）
        val userId = getCurrentUserId(context) ?: run {
            Log.w(TAG, "无法获取用户ID，跳过同步")
            return false
        }
        
        try {
            // 1. 获取本地未同步的收藏数据
            val unsyncedFavorites = favoriteDao.getUnsyncedFavorites()
            Log.d(TAG, "发现 ${unsyncedFavorites.size} 个未同步的收藏")
            
            if (unsyncedFavorites.isNotEmpty()) {
                // 2. 将本地数据同步到云端
                val syncItems = unsyncedFavorites.map { favorite ->
                    SyncFavoriteItem(
                        productId = favorite.productId,
                        action = if (favorite.isFavorited) "add" else "remove",
                        timestamp = favorite.updatedAt
                    )
                }
                
                val syncRequest = SyncFavoritesRequest(favorites = syncItems)
                val response = apiService.syncFavorites(syncRequest)
                if (response.isSuccessful) {
                    Log.d(TAG, "云端同步成功")
                    // 标记本地数据为已同步
                    unsyncedFavorites.forEach { favorite ->
                        favoriteDao.markAsSynced(favorite.productId)
                    }
                } else {
                    Log.e(TAG, "云端同步失败: ${response.errorBody()?.string()}")
                    return false
                }
            }
            
            // 3. 获取云端收藏数据
            val cloudFavoritesResponse = apiService.getFavorites()
            if (cloudFavoritesResponse.isSuccessful) {
                val cloudFavorites = cloudFavoritesResponse.body()?.favorites ?: emptyList()
                Log.d(TAG, "获取到 ${cloudFavorites.size} 个云端收藏")
                
                // 4. 合并数据（云端优先）
                mergeFavorites(context, cloudFavorites)
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "收藏同步失败", e)
            throw e
        }
    }
    
    /**
     * 合并本地和云端收藏数据
     */
    private suspend fun mergeFavorites(context: Context, cloudFavorites: List<Any>) {
        val databaseHelper = DatabaseHelper(context)
        val favoriteDao = FavoriteDao(databaseHelper)
        val userId = getCurrentUserId(context) ?: return
        
        // 获取本地收藏
        val localFavorites = favoriteDao.getAllFavoritesSync()
        
        // 创建云端收藏的ID集合
        val cloudFavoriteProductIds = cloudFavorites.mapNotNull { favorite ->
            when (favorite) {
                is Map<*, *> -> (favorite["productId"] as? Number)?.toLong()
                else -> null
            }
        }.toSet()
        
        // 删除本地存在但云端不存在的收藏
        localFavorites.forEach { localFavorite ->
            if (localFavorite.productId !in cloudFavoriteProductIds) {
                favoriteDao.removeFavorite(userId, localFavorite.productId)
            }
        }
        
        // 添加云端存在但本地不存在的收藏
        cloudFavoriteProductIds.forEach { productId ->
            val existingFavorite = localFavorites.find { it.productId == productId }
            if (existingFavorite == null) {
                val newFavorite = FavoriteEntity(
                    userId = userId,
                    productId = productId,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = true,
                    isFavorited = true
                )
                favoriteDao.insertFavorite(newFavorite)
            } else if (!existingFavorite.isFavorited) {
                // 更新本地状态
                favoriteDao.updateFavoriteStatus(productId, true)
                favoriteDao.markAsSynced(productId)
            }
        }
        
        Log.d(TAG, "收藏数据合并完成")
    }
    
    /**
     * 获取当前用户ID
     */
    private fun getCurrentUserId(context: Context): Long? {
        // 这里应该从TokenManager或UserRepository获取当前用户ID
        // 暂时返回一个默认值，实际使用时需要根据实际情况实现
        return 1L // 临时默认值
    }
    
    /**
     * 检查是否应该执行同步
     */
    fun shouldSync(context: Context): Boolean {
        if (!CloudConfig.SyncConfig.ENABLE_AUTO_SYNC) {
            Log.d(TAG, "自动同步已禁用")
            return false
        }
        
        if (!isNetworkAvailable(context)) {
            Log.d(TAG, "网络不可用")
            return false
        }
        
        if (CloudConfig.SyncConfig.SYNC_ON_WIFI_ONLY && !isWifiConnected(context)) {
            Log.d(TAG, "需要WiFi连接")
            return false
        }
        
        if (CloudConfig.SyncConfig.SYNC_WHEN_CHARGING && !isCharging(context)) {
            Log.d(TAG, "需要充电状态")
            return false
        }
        
        // 检查是否登录
        val token = TokenManager.getToken(context)
        if (token.isNullOrEmpty()) {
            Log.d(TAG, "用户未登录")
            return false
        }
        
        return true
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    /**
     * 检查是否连接到WiFi
     */
    private fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * 检查是否正在充电
     */
    private fun isCharging(context: Context): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        // 注意：BatteryManager.isCharging() 需要API 23+
        // 这里简化处理，实际项目中可能需要更复杂的逻辑
        return true
    }
    
    /**
     * 更新同步状态
     */
    private fun updateStatus(status: SyncStatus) {
        currentStatus = status
        syncListener?.onSyncStatusChanged(status)
    }
    
    /**
     * 获取同步状态描述
     */
    fun getStatusDescription(): String {
        return when (currentStatus) {
            SyncStatus.IDLE -> "空闲"
            SyncStatus.SYNCING -> "同步中..."
            SyncStatus.SUCCESS -> "同步成功"
            SyncStatus.FAILED -> "同步失败"
            SyncStatus.NETWORK_ERROR -> "网络错误"
            SyncStatus.AUTH_ERROR -> "认证失败"
        }
    }
}