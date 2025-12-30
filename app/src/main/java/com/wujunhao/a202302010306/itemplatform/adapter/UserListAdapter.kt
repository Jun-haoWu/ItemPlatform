package com.wujunhao.a202302010306.itemplatform.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.AdminUser

class UserListAdapter(
    private var users: List<AdminUser>,
    private val onUserClick: (AdminUser) -> Unit
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvEmail: TextView = itemView.findViewById(R.id.tv_email)
        val tvDepartment: TextView = itemView.findViewById(R.id.tv_department)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_list, parent, false)
        return UserViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.tvUsername.text = user.username
        holder.tvEmail.text = user.email
        holder.tvDepartment.text = user.department ?: "未设置院系"
        
        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }
    
    override fun getItemCount(): Int = users.size
    
    fun updateUsers(newUsers: List<AdminUser>) {
        users = newUsers
        notifyDataSetChanged()
    }
}