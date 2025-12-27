package com.wujunhao.a202302010306.itemplatform.database

/**
 * 收藏实体类
 * 用于表示用户的收藏数据
 */
data class FavoriteEntity(
    val id: Long = 0,
    val userId: Long,
    val productId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncAction: String? = null,
    val isFavorited: Boolean = true // 用于表示收藏状态
) {
    /**
     * 检查是否需要同步
     */
    fun needsSync(): Boolean = !isSynced
    
    /**
     * 获取同步操作类型
     */
    fun getSyncActionType(): String = syncAction ?: "add"
}