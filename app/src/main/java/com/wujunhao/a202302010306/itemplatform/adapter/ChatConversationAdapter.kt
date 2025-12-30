package com.wujunhao.a202302010306.itemplatform.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.ChatConversation
import java.text.SimpleDateFormat
import java.util.*

class ChatConversationAdapter(
    private val conversations: List<ChatConversation>,
    private val onItemClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ChatConversationAdapter.ViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewUsername: TextView = itemView.findViewById(R.id.textViewUsername)
        private val textViewLastMessage: TextView = itemView.findViewById(R.id.textViewLastMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewUnreadCount: TextView = itemView.findViewById(R.id.textViewUnreadCount)
        
        fun bind(conversation: ChatConversation) {
            textViewUsername.text = conversation.otherUsername
            textViewLastMessage.text = conversation.lastMessage ?: "暂无消息"
            textViewTime.text = dateFormat.format(conversation.lastMessageTime)
            
            if (conversation.unreadCount > 0) {
                textViewUnreadCount.visibility = View.VISIBLE
                textViewUnreadCount.text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString()
            } else {
                textViewUnreadCount.visibility = View.GONE
            }
            
            itemView.setOnClickListener {
                onItemClick(conversation)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(conversations[position])
    }
    
    override fun getItemCount() = conversations.size
}