package com.wujunhao.a202302010306.itemplatform.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class ChatMessage(
    val id: Long,
    @SerializedName("sender_id")
    val senderId: Long,
    @SerializedName("receiver_id")
    val receiverId: Long,
    val message: String,
    @SerializedName("message_type")
    val messageType: String = "text",
    @SerializedName("is_read")
    val isRead: Boolean = false,
    @SerializedName("created_at")
    val createdAt: Date,
    @SerializedName("sender_username")
    val senderUsername: String,
    @SerializedName("receiver_username")
    val receiverUsername: String
)

data class ChatConversation(
    val id: Long,
    @SerializedName("user1_id")
    val user1Id: Long,
    @SerializedName("user2_id")
    val user2Id: Long,
    @SerializedName("last_message_time")
    val lastMessageTime: Date,
    @SerializedName("last_message")
    val lastMessage: String?,
    @SerializedName("last_sender_id")
    val lastSenderId: Long?,
    @SerializedName("other_username")
    val otherUsername: String,
    @SerializedName("other_user_id")
    val otherUserId: Long,
    @SerializedName("unread_count")
    val unreadCount: Int = 0
)

data class SendMessageRequest(
    @SerializedName("receiver_id")
    val receiverId: Long,
    val message: String
)

data class SendMessageResponse(
    val message: String,
    val data: ChatMessage
)

data class ChatHistoryData(
    @SerializedName("messages")
    val messages: List<ChatMessage>?,
    @SerializedName("pagination")
    val pagination: AdminPagination?
)

data class ChatHistoryResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: ChatHistoryData?
) {
    val messages: List<ChatMessage>?
        get() = data?.messages
}

data class ChatConversationsResponse(
    val conversations: List<ChatConversation>?,
    val pagination: AdminPagination?
)

data class UnreadCountResponse(
    @SerializedName("unread_count")
    val unreadCount: Int
)