package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String
)

data class LoginResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: LoginData?
)

data class LoginData(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("user")
    val user: User,
    
    @SerializedName("expiresIn")
    val expiresIn: Long
)