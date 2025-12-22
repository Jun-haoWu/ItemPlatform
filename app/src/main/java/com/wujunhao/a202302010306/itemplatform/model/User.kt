package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("realName")
    val realName: String?,
    
    @SerializedName("studentId")
    val studentId: String?,
    
    @SerializedName("department")
    val department: String?,
    
    @SerializedName("avatar")
    val avatar: String?,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)