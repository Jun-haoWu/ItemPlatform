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
            // 检查用户名是否已存在
            if (userDao.isUsernameExists(registerRequest.username)) {
                return Result.failure(Exception("用户名已存在"))
            }
            
            // 创建本地用户对象
            val localUser = LocalUser(
                username = registerRequest.username,
                password = registerRequest.password, // 实际应用中应该加密密码
                email = registerRequest.email,
                phone = registerRequest.phone,
                realName = registerRequest.realName,
                studentId = registerRequest.studentId,
                department = registerRequest.department
            )
            
            // 插入到SQLite数据库
            val userId = userDao.insertUser(localUser)
            
            if (userId != -1L) {
                // 创建响应
                val user = localUser.toUser().copy(id = userId)
                val response = RegisterResponse(
                    code = 200,
                    message = "注册成功",
                    data = user
                )
                Result.success(response)
            } else {
                Result.failure(Exception("注册失败，请重试"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(loginRequest: LoginRequest): Result<LoginResponse> {
        return try {
            // 在SQLite数据库中验证用户
            val localUser = userDao.verifyUser(loginRequest.username, loginRequest.password)
            
            if (localUser != null) {
                // 生成模拟的JWT Token
                val token = generateJwtToken(localUser.id, localUser.username)
                val expiresIn = 7 * 24 * 60 * 60L // 7天
                
                // 保存认证信息到SQLite
                val authToken = AuthToken(
                    userId = localUser.id,
                    token = token,
                    expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
                )
                authDao.insertAuthToken(authToken)
                
                // 也保存到SharedPreferences保持兼容性
                TokenManager.saveToken(
                    context = context,
                    token = token,
                    userId = localUser.id,
                    username = localUser.username,
                    email = localUser.email,
                    expiryIn = expiresIn
                )
                
                // 创建响应
                val response = LoginResponse(
                    code = 200,
                    message = "登录成功",
                    data = LoginData(
                        token = token,
                        expiresIn = expiresIn,
                        user = localUser.toUser()
                    )
                )
                Result.success(response)
            } else {
                Result.failure(Exception("用户名或密码错误"))
            }
        } catch (e: Exception) {
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