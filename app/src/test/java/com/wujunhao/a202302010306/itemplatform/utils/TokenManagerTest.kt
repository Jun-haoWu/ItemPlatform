package com.wujunhao.a202302010306.itemplatform.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.TimeUnit

class TokenManagerTest {
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 清理之前的测试数据
        TokenManager.clearToken(context)
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
        TokenManager.clearToken(context)
    }
    
    @Test
    fun `saveToken should store token and user info`() {
        // Given
        val token = "test.jwt.token"
        val userId = 123L
        val username = "testuser"
        val email = "test@example.com"
        val expiryIn = 3600L // 1小时
        
        // When
        TokenManager.saveToken(context, token, userId, username, email, expiryIn)
        
        // Then
        assertEquals(token, TokenManager.getToken(context))
        assertEquals(userId, TokenManager.getUserId(context))
        assertEquals(username, TokenManager.getUsername(context))
        assertEquals(email, TokenManager.getEmail(context))
    }
    
    @Test
    fun `saveToken with default expiry should work`() {
        // Given
        val token = "test.token"
        
        // When
        TokenManager.saveToken(context, token)
        
        // Then
        assertEquals(token, TokenManager.getToken(context))
        // 验证默认7天过期时间是否设置
        val expiryTime = context.getSharedPreferences("ItemPlatformPrefs", Context.MODE_PRIVATE)
            .getLong("token_expiry", 0L)
        assertTrue(expiryTime > System.currentTimeMillis())
    }
    
    @Test
    fun `isTokenValid should return false when no token exists`() {
        // Given
        // 确保没有token
        
        // When
        val isValid = TokenManager.isTokenValid(context)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `isTokenValid should return false when token is expired`() {
        // Given
        val expiredToken = "expired.token"
        val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) // 1小时前
        TokenManager.saveToken(context, expiredToken)
        
        // 手动设置过期时间为过去时间
        context.getSharedPreferences("ItemPlatformPrefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("token_expiry", pastTime)
            .apply()
        
        // When
        val isValid = TokenManager.isTokenValid(context)
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `isTokenValid should return true when token is valid`() {
        // Given
        val validToken = "valid.token"
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1) // 1小时后
        TokenManager.saveToken(context, validToken)
        
        // 手动设置过期时间为未来时间
        context.getSharedPreferences("ItemPlatformPrefs", Context.MODE_PRIVATE)
            .edit()
            .putLong("token_expiry", futureTime)
            .apply()
        
        // When
        val isValid = TokenManager.isTokenValid(context)
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `clearToken should remove all token data`() {
        // Given
        TokenManager.saveToken(context, "test.token", 123L, "user", "email@test.com")
        
        // When
        TokenManager.clearToken(context)
        
        // Then
        assertNull(TokenManager.getToken(context))
        assertEquals(0L, TokenManager.getUserId(context))
        assertNull(TokenManager.getUsername(context))
        assertNull(TokenManager.getEmail(context))
    }
    
    @Test
    fun `getUserId should return default value when not set`() {
        // Given
        // 确保没有设置userId
        
        // When
        val userId = TokenManager.getUserId(context)
        
        // Then
        assertEquals(0L, userId)
    }
    
    @Test
    fun `getUsername should return null when not set`() {
        // Given
        // 确保没有设置username
        
        // When
        val username = TokenManager.getUsername(context)
        
        // Then
        assertNull(username)
    }
    
    @Test
    fun `getEmail should return null when not set`() {
        // Given
        // 确保没有设置email
        
        // When
        val email = TokenManager.getEmail(context)
        
        // Then
        assertNull(email)
    }
}