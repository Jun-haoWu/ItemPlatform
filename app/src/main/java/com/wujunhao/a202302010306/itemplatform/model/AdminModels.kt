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
    
    @SerializedName("real_name")
    val realName: String?,
    
    @SerializedName("student_id")
    val studentId: String?,
    
    @SerializedName("department")
    val department: String?,
    
    @SerializedName("created_at")
    val createdAt: String
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
    val pagination: AdminPagination
)

data class AdminPagination(
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