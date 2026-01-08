package com.wujunhao.a202302010306.itemplatform.network

import com.wujunhao.a202302010306.itemplatform.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // 认证相关
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @GET("auth/profile")
    suspend fun getUserProfile(): ApiResponse<User>
    
    @PUT("auth/profile")
    suspend fun updateUserProfile(@Body user: User): ApiResponse<User>
    
    // 商品相关
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null,
        @Query("search") search: String? = null
    ): Response<ProductsResponse>
    
    @POST("products")
    suspend fun postProduct(@Body request: PublishProductRequest): Response<PublishProductResponse>
    
    @GET("products/{id}")
    suspend fun getProductDetail(@Path("id") productId: Long): Response<ProductDetailResponse>
    
    // 收藏相关
    @GET("favorites")
    suspend fun getFavorites(): Response<FavoritesResponse>
    
    @POST("favorites/{productId}")
    suspend fun addFavorite(@Path("productId") productId: Long): Response<ApiResponse<String>>
    
    @DELETE("favorites/{productId}")
    suspend fun removeFavorite(@Path("productId") productId: Long): Response<ApiResponse<String>>
    
    @POST("favorites/status")
    suspend fun getFavoritesStatus(@Body request: FavoritesStatusRequest): Response<FavoritesStatusResponse>
    
    // 同步相关
    @POST("sync/favorites")
    suspend fun syncFavorites(@Body request: SyncFavoritesRequest): Response<SyncResponse>
    
    // 管理员相关
    @GET("admin/users")
    suspend fun getAdminUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<AdminUsersResponse>
    
    // 普通用户相关 - 获取用户列表
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<AdminUsersResponse>
    
    // 即时通讯相关
    @POST("chat/send")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<SendMessageResponse>
    
    @GET("chat/history/{userId}")
    suspend fun getChatHistory(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ChatHistoryResponse
    
    @GET("chat/conversations")
    suspend fun getChatConversations(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ChatConversationsResponse>
    
    @GET("chat/unread-count")
    suspend fun getUnreadCount(): Response<UnreadCountResponse>
    
    // 图片上传相关
    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(
        @Part image: okhttp3.MultipartBody.Part
    ): Response<UploadImageResponse>
    
    @Multipart
    @POST("upload/images")
    suspend fun uploadImages(
        @Part images: List<okhttp3.MultipartBody.Part>
    ): Response<UploadImagesResponse>
}