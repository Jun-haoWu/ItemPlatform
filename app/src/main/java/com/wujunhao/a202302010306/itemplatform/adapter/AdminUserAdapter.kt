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
        val realNameTextView: TextView = view.findViewById(R.id.realNameTextView)
        val studentIdTextView: TextView = view.findViewById(R.id.studentIdTextView)
        val departmentTextView: TextView = view.findViewById(R.id.departmentTextView)
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
        holder.realNameTextView.text = "真实姓名: ${user.realName ?: "未设置"}"
        holder.studentIdTextView.text = "学号: ${user.studentId ?: "未设置"}"
        holder.departmentTextView.text = "院系: ${user.department ?: "未设置"}"
        
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(user.createdAt)
            holder.createdAtTextView.text = "注册时间: ${dateFormat.format(date)}"
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = inputFormat.parse(user.createdAt)
                holder.createdAtTextView.text = "注册时间: ${dateFormat.format(date)}"
            } catch (e2: Exception) {
                holder.createdAtTextView.text = "注册时间: ${user.createdAt}"
            }
        }
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