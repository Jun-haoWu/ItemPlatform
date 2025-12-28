package com.wujunhao.a202302010306.itemplatform.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.AdminUser
import java.text.SimpleDateFormat
import java.util.*

class AdminUserAdapter(private var users: List<AdminUser>) :
    RecyclerView.Adapter<AdminUserAdapter.UserViewHolder>() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val emailTextView: TextView = view.findViewById(R.id.emailTextView)
        val phoneTextView: TextView = view.findViewById(R.id.phoneTextView)
        val createdAtTextView: TextView = view.findViewById(R.id.createdAtTextView)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.usernameTextView.text = "用户名: ${user.username}"
        holder.emailTextView.text = "邮箱: ${user.email ?: "未设置"}"
        holder.phoneTextView.text = "手机: ${user.phone ?: "未设置"}"
        holder.createdAtTextView.text = "注册时间: ${dateFormat.format(Date(user.createdAt))}"
    }
    
    override fun getItemCount(): Int = users.size
    
    fun updateUsers(newUsers: List<AdminUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
    
    fun addUsers(newUsers: List<AdminUser>) {
        users = users + newUsers
        notifyDataSetChanged()
    }
}