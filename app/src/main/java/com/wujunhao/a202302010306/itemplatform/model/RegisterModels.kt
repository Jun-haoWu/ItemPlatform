package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("username")
    val username: String,
    
    @SerializedName("password")
    val password: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("realName")
    val realName: String,
    
    @SerializedName("studentId")
    val studentId: String,
    
    @SerializedName("department")
    val department: String
)

data class RegisterResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: User?
)