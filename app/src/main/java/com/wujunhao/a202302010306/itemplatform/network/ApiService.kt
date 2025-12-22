package com.wujunhao.a202302010306.itemplatform.network

import com.wujunhao.a202302010306.itemplatform.model.*
import retrofit2.http.*

interface ApiService {
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @GET("auth/profile")
    suspend fun getUserProfile(): ApiResponse<User>
    
    @PUT("auth/profile")
    suspend fun updateUserProfile(@Body user: User): ApiResponse<User>
}