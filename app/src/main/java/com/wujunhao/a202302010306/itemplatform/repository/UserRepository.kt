package com.wujunhao.a202302010306.itemplatform.repository

import android.content.Context
import com.wujunhao.a202302010306.itemplatform.database.*
import com.wujunhao.a202302010306.itemplatform.model.*
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import java.util.Date
import java.util.UUID

class UserRepository(private val context: Context) {
    private val apiService = ApiClient.createApiService(context)
    private val databaseHelper = DatabaseHelper(context)
    private val userDao = UserDao(databaseHelper)
    private val authDao = AuthDao(databaseHelper)
    
    suspend fun register(registerRequest: RegisterRequest): Result<RegisterResponse> {
        return try {
            val response = apiService.register(registerRequest)
            
            if (response.code == 200 && response.data != null) {
                val token = response.data.token
                val user = response.data.user
                val expiresIn = response.data.expiresIn
                
                TokenManager.saveToken(
                    context = context,
                    token = token,
                    userId = user.id,
                    username = user.username,
                    email = user.email,
                    expiryIn = expiresIn
                )
                
                Result.success(response)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(loginRequest: LoginRequest): Result<LoginResponse> {
        return try {
            android.util.Log.d("UserRepository", "开始登录API调用，用户名: ${loginRequest.username}")
            val response = apiService.login(loginRequest)
            android.util.Log.d("UserRepository", "登录API响应，状态码: ${response.code}, 消息: ${response.message}")
            
            if (response.code == 200 && response.data != null) {
                val token = response.data.token
                val user = response.data.user
                val expiresIn = response.data.expiresIn
                
                android.util.Log.d("UserRepository", "登录成功，用户ID: ${user.id}, 用户名: ${user.username}")
                
                TokenManager.saveToken(
                    context = context,
                    token = token,
                    userId = user.id,
                    username = user.username,
                    email = user.email,
                    expiryIn = expiresIn
                )
                
                Result.success(response)
            } else {
                android.util.Log.e("UserRepository", "登录失败，响应码: ${response.code}, 消息: ${response.message}")
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "登录API调用异常", e)
            Result.failure(e)
        }
    }
    

    
    fun isLoggedIn(): Boolean {
        // 优先检查SQLite数据库中的认证状态，如果SQLite中没有有效的token，再检查SharedPreferences
        val sqliteValid = authDao.isTokenValid()
        val sharedPrefsValid = TokenManager.isTokenValid(context)
        
        // 如果SQLite中没有有效token但SharedPreferences中有，同步到SQLite
        if (!sqliteValid && sharedPrefsValid) {
            // 从SharedPreferences获取用户信息并创建SQLite token
            val userId = TokenManager.getUserId(context)
            val token = TokenManager.getToken(context)
            val expiryTime = context.getSharedPreferences("ItemPlatformPrefs", Context.MODE_PRIVATE)
                .getLong("token_expiry", 0L)
            
            if (userId != 0L && token != null && expiryTime > System.currentTimeMillis()) {
                val authToken = AuthToken(
                    userId = userId,
                    token = token,
                    expiresAt = expiryTime
                )
                authDao.insertAuthToken(authToken)
                return true
            }
        }
        
        return sqliteValid || sharedPrefsValid
    }
    
    fun logout() {
        // 清除SQLite中的认证信息
        authDao.clearAllTokens()
        // 清除SharedPreferences中的Token
        TokenManager.clearToken(context)
    }
    
    suspend fun getUserProfile(): Result<User> {
        return try {
            // 从SQLite数据库获取当前用户信息
            val userId = authDao.getCurrentUserId()
            if (userId != null) {
                val localUser = userDao.getUserById(userId)
                if (localUser != null) {
                    Result.success(localUser.toUser())
                } else {
                    Result.failure(Exception("用户不存在"))
                }
            } else {
                Result.failure(Exception("未登录"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserProfile(user: User): Result<User> {
        return try {
            // 这里可以实现更新用户信息的逻辑
            // 为了简化，暂时返回成功
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateJwtToken(userId: Long, username: String): String {
        // 生成模拟的JWT Token
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // {"alg":"HS256","typ":"JWT"}
        val payload = java.util.Base64.getUrlEncoder().encodeToString(
            "{\"sub\":\"$userId\",\"name\":\"$username\",\"iat\":${System.currentTimeMillis() / 1000}}".toByteArray()
        )
        val signature = "signature" // 实际应用中应该使用密钥签名
        return "$header.$payload.$signature"
    }
}