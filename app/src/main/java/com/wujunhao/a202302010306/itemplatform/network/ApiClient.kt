package com.wujunhao.a202302010306.itemplatform.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wujunhao.a202302010306.itemplatform.model.CloudProduct
import com.wujunhao.a202302010306.itemplatform.model.CloudProductDeserializer
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
            
            val response = chain.proceed(request)
            
            if (response.code == 403) {
                android.util.Log.w("ApiClient", "Token无效或已过期，清除token")
                TokenManager.clearToken(context)
            }
            
            response
        }
    }
    
    fun createRetrofit(context: Context): Retrofit {
        android.util.Log.d("ApiClient", "创建Retrofit实例，基础URL: $BASE_URL")
        android.util.Log.d("ApiClient", "USE_MOCK_MODE: $USE_MOCK_MODE, USE_LOCAL_DATABASE: $USE_LOCAL_DATABASE")
        
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        
        val gson = GsonBuilder()
            .registerTypeAdapter(CloudProduct::class.java, CloudProductDeserializer())
            .create()
        
        android.util.Log.d("ApiClient", "已注册CloudProductDeserializer处理images字段")
        
        // 配置网络拦截器
        when {
            USE_LOCAL_DATABASE -> {
                // 本地数据库模式：不需要网络拦截器，所有操作都在本地完成
                // 可以添加一个空拦截器或日志拦截器
                android.util.Log.d("ApiClient", "使用本地数据库模式")
                clientBuilder.addInterceptor(createLoggingInterceptor())
            }
            USE_MOCK_MODE -> {
                // 模拟模式：使用模拟拦截器
                android.util.Log.d("ApiClient", "使用模拟模式")
                clientBuilder.addInterceptor(MockInterceptor())
            }
            else -> {
                // 真实服务器模式：添加认证拦截器
                android.util.Log.d("ApiClient", "使用真实服务器模式")
                clientBuilder.addInterceptor(createAuthInterceptor(context))
                clientBuilder.addInterceptor(createLoggingInterceptor())
            }
        }
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    fun createApiService(context: Context): ApiService {
        return createRetrofit(context).create(ApiService::class.java)
    }
}