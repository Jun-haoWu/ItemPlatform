package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.*
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

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
    val price: String,
    
    @SerializedName("original_price")
    val originalPrice: String? = null,
    
    @SerializedName("images")
    val images: List<String>? = null,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("location")
    val location: String,
    
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null,
    
    @SerializedName("like_count")
    val likeCount: Int = 0,
    
    @SerializedName("view_count")
    val viewCount: Int = 0,
    
    @SerializedName("created_at")
    val createdAt: String
) {
    /**
     * 转换为本地 Product 模型
     */
    fun toLocalProduct(sellerId: Long = -1L): Product {
        val timestamp = parseIsoDateTime(createdAt)
        val imagesList = images ?: emptyList()
        val imagesString = if (imagesList.isEmpty()) {
            null
        } else {
            imagesList.joinToString(",")
        }
        return Product(
            id = id,
            title = name,
            description = description,
            price = parsePrice(price),
            category = category,
            condition = "全新",
            location = location,
            latitude = latitude,
            longitude = longitude,
            images = imagesString,
            sellerId = sellerId,
            status = Product.STATUS_ACTIVE,
            viewCount = viewCount,
            likeCount = likeCount,
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }
    
    private fun parseIsoDateTime(isoDateTime: String): Long {
        return try {
            val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(isoDateTime)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun parsePrice(priceString: String): Double {
        return try {
            priceString.toDouble()
        } catch (e: Exception) {
            0.0
        }
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
 * 发布商品请求
 */
data class PublishProductRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("price")
    val price: Double,
    
    @SerializedName("original_price")
    val originalPrice: Double? = null,
    
    @SerializedName("images")
    val images: List<String>? = null,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("location")
    val location: String? = null,
    
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null
)

/**
 * 发布商品响应
 */
data class PublishProductResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("productId")
    val productId: Long
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

/**
 * 图片上传响应
 */
data class UploadImageResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String
)

/**
 * 批量图片上传响应
 */
data class UploadImagesResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("imageUrls")
    val imageUrls: List<String>
)

/**
 * CloudProduct 的自定义反序列化器
 * 处理 images 字段可能是字符串或字符串数组的情况
 */
class CloudProductDeserializer : JsonDeserializer<CloudProduct> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): CloudProduct {
        val jsonObject = json.asJsonObject
        
        val id = jsonObject.get("id").asLong
        val name = jsonObject.get("name").asString
        val description = jsonObject.get("description").asString
        val price = jsonObject.get("price").asString
        val originalPrice = if (jsonObject.has("original_price") && !jsonObject.get("original_price").isJsonNull) {
            jsonObject.get("original_price").asString
        } else {
            null
        }
        
        val images: List<String>? = if (jsonObject.has("images") && !jsonObject.get("images").isJsonNull) {
            val imagesElement = jsonObject.get("images")
            android.util.Log.d("CloudProductDeserializer", "images字段类型: ${imagesElement::class.simpleName}, 值: ${imagesElement}")
            
            when {
                imagesElement.isJsonArray -> {
                    android.util.Log.d("CloudProductDeserializer", "images是JSON数组")
                    context.deserialize(imagesElement, object : TypeToken<List<String>>() {}.type)
                }
                imagesElement.isJsonPrimitive && imagesElement.asString.startsWith("[") -> {
                    android.util.Log.d("CloudProductDeserializer", "images是字符串化的数组: ${imagesElement.asString}")
                    try {
                        val parsed = JsonParser.parseString(imagesElement.asString) as JsonElement
                        android.util.Log.d("CloudProductDeserializer", "解析后的JSON: $parsed")
                        val result: List<String> = context.deserialize(parsed, object : TypeToken<List<String>>() {}.type)
                        android.util.Log.d("CloudProductDeserializer", "反序列化结果: $result")
                        result
                    } catch (e: Exception) {
                        android.util.Log.e("CloudProductDeserializer", "解析字符串化数组失败: ${e.message}", e)
                        null
                    }
                }
                imagesElement.isJsonPrimitive -> {
                    android.util.Log.d("CloudProductDeserializer", "images是普通字符串: ${imagesElement.asString}")
                    listOf(imagesElement.asString)
                }
                else -> {
                    android.util.Log.w("CloudProductDeserializer", "images字段类型未知: ${imagesElement::class.simpleName}")
                    null
                }
            }
        } else {
            android.util.Log.d("CloudProductDeserializer", "images字段不存在或为null")
            null
        }
        
        val category = jsonObject.get("category").asString
        val location = jsonObject.get("location").asString
        val latitude = if (jsonObject.has("latitude") && !jsonObject.get("latitude").isJsonNull) {
            jsonObject.get("latitude").asDouble
        } else {
            null
        }
        val longitude = if (jsonObject.has("longitude") && !jsonObject.get("longitude").isJsonNull) {
            jsonObject.get("longitude").asDouble
        } else {
            null
        }
        val likeCount = if (jsonObject.has("like_count")) {
            jsonObject.get("like_count").asInt
        } else {
            0
        }
        val viewCount = if (jsonObject.has("view_count")) {
            jsonObject.get("view_count").asInt
        } else {
            0
        }
        val createdAt = jsonObject.get("created_at").asString
        
        return CloudProduct(
            id = id,
            name = name,
            description = description,
            price = price,
            originalPrice = originalPrice,
            images = images,
            category = category,
            location = location,
            latitude = latitude,
            longitude = longitude,
            likeCount = likeCount,
            viewCount = viewCount,
            createdAt = createdAt
        )
    }
}