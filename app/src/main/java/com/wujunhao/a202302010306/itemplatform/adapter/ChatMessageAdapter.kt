package com.wujunhao.a202302010306.itemplatform.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatMessageAdapter(
    private var messages: List<ChatMessage>,
    private var currentUserId: Long
) : RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
    
    abstract class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(message: ChatMessage)
    }
    
    inner class SentMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewSentMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewSentTime)
        
        override fun bind(message: ChatMessage) {
            textViewMessage.text = message.message
            textViewTime.text = dateFormat.format(message.createdAt)
        }
    }
    
    inner class ReceivedMessageViewHolder(itemView: View) : MessageViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewReceivedMessage)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewReceivedTime)
        
        override fun bind(message: ChatMessage) {
            textViewMessage.text = message.message
            textViewTime.text = dateFormat.format(message.createdAt)
        }
    }
    
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        android.util.Log.d("ChatMessageAdapter", "Binding message at position $position: ${messages[position].message}")
        holder.bind(messages[position])
    }
    
    override fun getItemCount() = messages.size
    
    fun updateMessages(newMessages: List<ChatMessage>) {
        android.util.Log.d("ChatMessageAdapter", "Updating adapter with ${newMessages.size} messages")
        messages = newMessages
        notifyDataSetChanged()
        android.util.Log.d("ChatMessageAdapter", "Adapter updated, item count: ${itemCount}")
    }
    
    fun updateCurrentUserId(newUserId: Long) {
        currentUserId = newUserId
    }
}