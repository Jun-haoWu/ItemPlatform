package com.wujunhao.a202302010306.itemplatform.model

data class Product(
    val id: Long = 0,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String,
    val location: String,
    val images: String? = null,
    val sellerId: Long,
    val status: Int = 0,
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val STATUS_ACTIVE = 0
        const val STATUS_SOLD = 1
        const val STATUS_HIDDEN = 2
        const val STATUS_DELETED = 3
    }
}