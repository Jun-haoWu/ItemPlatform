package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName

/**
 * 云端商品模型（用于API响应）
 */
data class CloudProduct(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("original_price")
    val originalPrice: Double? = null,
    
    @SerializedName("images")
    val images: String? = null,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("like_count")
    val likeCount: Int = 0,
    
    @SerializedName("view_count")
    val viewCount: Int = 0,
    
    @SerializedName("created_at")
    val createdAt: Long
) {
    /**
     * 转换为本地 Product 模型
     */
    fun toLocalProduct(sellerId: Long = -1L): Product {
        return Product(
            id = id,
            title = name,
            description = description,
            price = price,
            category = category,
            condition = "全新",
            location = location,
            images = images,
            sellerId = sellerId,
            status = Product.STATUS_ACTIVE,
            viewCount = viewCount,
            likeCount = likeCount,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}

/**
 * API通用响应
 */
data class ApiResponse<T>(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T? = null,
    
    @SerializedName("error")
    val error: String? = null
)

/**
 * 商品列表响应
 */
data class ProductsResponse(
    @SerializedName("products")
    val products: List<CloudProduct>,
    
    @SerializedName("pagination")
    val pagination: Pagination
)

/**
 * 商品详情响应
 */
data class ProductDetailResponse(
    @SerializedName("product")
    val product: CloudProduct
)

/**
 * 收藏列表响应
 */
data class FavoritesResponse(
    @SerializedName("favorites")
    val favorites: List<FavoriteItem>
)

/**
 * 收藏状态请求
 */
data class FavoritesStatusRequest(
    @SerializedName("productIds")
    val productIds: List<Long>
)

/**
 * 收藏状态响应
 */
data class FavoritesStatusResponse(
    @SerializedName("status")
    val status: Map<String, Boolean>
)

/**
 * 同步收藏请求
 */
data class SyncFavoritesRequest(
    @SerializedName("favorites")
    val favorites: List<SyncFavoriteItem>
)

/**
 * 同步收藏项
 */
data class SyncFavoriteItem(
    @SerializedName("productId")
    val productId: Long,
    
    @SerializedName("action")
    val action: String, // "add" or "remove"
    
    @SerializedName("timestamp")
    val timestamp: Long
)

/**
 * 同步响应
 */
data class SyncResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("syncResult")
    val syncResult: SyncResult
)

/**
 * 同步结果
 */
data class SyncResult(
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("success")
    val success: Int,
    
    @SerializedName("errors")
    val errors: Int,
    
    @SerializedName("results")
    val results: List<SyncItemResult>
)

/**
 * 同步项结果
 */
data class SyncItemResult(
    @SerializedName("productId")
    val productId: Long,
    
    @SerializedName("action")
    val action: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("error")
    val error: String? = null
)

/**
 * 收藏项
 */
data class FavoriteItem(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("productId")
    val productId: Long,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("originalPrice")
    val originalPrice: Double? = null,
    
    @SerializedName("images")
    val images: List<String>? = null,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("likeCount")
    val likeCount: Int = 0,
    
    @SerializedName("viewCount")
    val viewCount: Int = 0,
    
    @SerializedName("favoritedAt")
    val favoritedAt: Long
)

/**
 * 分页信息
 */
data class Pagination(
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("total")
    val total: Int,
    
    @SerializedName("pages")
    val pages: Int
)