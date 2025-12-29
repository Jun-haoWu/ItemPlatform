package com.wujunhao.a202302010306.itemplatform.network

import android.content.Context
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val BASE_URL = CloudConfig.getCurrentBaseUrl()
    private const val CONNECT_TIMEOUT = CloudConfig.NetworkConfig.CONNECT_TIMEOUT
    private const val READ_TIMEOUT = CloudConfig.NetworkConfig.READ_TIMEOUT
    
    // 模拟模式开关 - 在没有真实服务器时使用
    private const val USE_MOCK_MODE = false
    
    // 本地数据库模式 - 使用SQLite存储数据
    private const val USE_LOCAL_DATABASE = false
    
    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    private fun createAuthInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val token = TokenManager.getToken(context)
            android.util.Log.d("ApiClient", "Token from TokenManager: ${token?.take(20)}...")
            val request = if (token != null) {
                android.util.Log.d("ApiClient", "Adding Authorization header")
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                android.util.Log.d("ApiClient", "No token found, not adding Authorization header")
                chain.request()
            }
            chain.proceed(request)
        }
    }
    
    fun createRetrofit(context: Context): Retrofit {
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        
        // 配置网络拦截器
        when {
            USE_LOCAL_DATABASE -> {
                // 本地数据库模式：不需要网络拦截器，所有操作都在本地完成
                // 可以添加一个空拦截器或日志拦截器
                clientBuilder.addInterceptor(createLoggingInterceptor())
            }
            USE_MOCK_MODE -> {
                // 模拟模式：使用模拟拦截器
                clientBuilder.addInterceptor(MockInterceptor())
            }
            else -> {
                // 真实服务器模式：添加认证拦截器
                clientBuilder.addInterceptor(createAuthInterceptor(context))
                clientBuilder.addInterceptor(createLoggingInterceptor())
            }
        }
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    fun createApiService(context: Context): ApiService {
        return createRetrofit(context).create(ApiService::class.java)
    }
}