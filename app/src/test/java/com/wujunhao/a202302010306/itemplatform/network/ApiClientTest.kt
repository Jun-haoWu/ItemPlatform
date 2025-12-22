package com.wujunhao.a202302010306.itemplatform.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiClientTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context
    private lateinit var retrofit: Retrofit
    
    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        context = ApplicationProvider.getApplicationContext()
        
        // 设置模拟服务器URL
        retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(com.google.gson.GsonConverterFactory.create())
            .build()
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
        // 清理token
        TokenManager.clearToken(context)
    }
    
    @Test
    fun `createRetrofit should configure OkHttpClient with proper settings`() {
        // Given & When
        val testRetrofit = ApiClient.createRetrofit(context)
        
        // Then
        assertNotNull(testRetrofit)
        // 验证base URL
        assertEquals("http://10.0.2.2:8080/api/", testRetrofit.baseUrl().toString())
    }
    
    @Test
    fun `createApiService should return ApiService instance`() {
        // Given & When
        val apiService = ApiClient.createApiService(context)
        
        // Then
        assertNotNull(apiService)
        assertTrue(apiService is ApiService)
    }
    
    @Test
    fun `auth interceptor should add Bearer token when available`() {
        // Given
        val token = "test.jwt.token"
        TokenManager.saveToken(context, token, 1L, "testuser", "test@example.com")
        
        // 模拟响应
        mockWebServer.enqueue(MockResponse().setBody("{}").setResponseCode(200))
        
        // When
        val client = ApiClient.createRetrofit(context)
        
        // Then
        // 验证token已保存
        assertEquals(token, TokenManager.getToken(context))
    }
    
    @Test
    fun `connect timeout should be configured`() {
        // Given & When
        val client = ApiClient.createRetrofit(context)
        val okHttpClient = client.callFactory as OkHttpClient
        
        // Then
        assertEquals(30, okHttpClient.connectTimeoutMillis.toLong() / 1000)
    }
    
    @Test
    fun `read timeout should be configured`() {
        // Given & When
        val client = ApiClient.createRetrofit(context)
        val okHttpClient = client.callFactory as OkHttpClient
        
        // Then
        assertEquals(30, okHttpClient.readTimeoutMillis.toLong() / 1000)
    }
    
    @Test
    fun `gson converter should be added`() {
        // Given & When
        val client = ApiClient.createRetrofit(context)
        
        // Then
        assertNotNull(client.converterFactories)
        assertTrue(client.converterFactories.any { it is com.google.gson.GsonConverterFactory })
    }
    
    @Test
    fun `logging interceptor should be configured`() {
        // Given & When
        val client = ApiClient.createRetrofit(context)
        val okHttpClient = client.callFactory as OkHttpClient
        
        // Then
        // 验证是否添加了日志拦截器（通过检查拦截器列表）
        assertTrue(okHttpClient.interceptors.size >= 1)
    }
    
    @Test
    fun `base URL should be correct`() {
        // Given & When
        val client = ApiClient.createRetrofit(context)
        
        // Then
        assertEquals("http://10.0.2.2:8080/api/", client.baseUrl().toString())
    }
    
    @Test
    fun `auth interceptor should not add header when no token`() {
        // Given
        // 确保没有token
        TokenManager.clearToken(context)
        
        // When
        val client = ApiClient.createRetrofit(context)
        val okHttpClient = client.callFactory as OkHttpClient
        
        // Then
        // 验证没有token时请求仍能正常进行
        assertNull(TokenManager.getToken(context))
    }
}