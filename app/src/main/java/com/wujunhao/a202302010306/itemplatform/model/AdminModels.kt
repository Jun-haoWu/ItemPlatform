package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName

data class AdminUser(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("username")
    val username: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("phone")
    val phone: String?,
    
    @SerializedName("created_at")
    val createdAt: Long
)

data class AdminUsersResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: AdminUsersData?
)

data class AdminUsersData(
    @SerializedName("users")
    val users: List<AdminUser>,
    
    @SerializedName("pagination")
    val pagination: Pagination
)

data class Pagination(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("totalPages")
    val totalPages: Int,
    
    @SerializedName("hasNext")
    val hasNext: Boolean,
    
    @SerializedName("hasPrev")
    val hasPrev: Boolean
)