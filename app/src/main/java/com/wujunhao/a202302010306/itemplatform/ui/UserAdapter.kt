package com.wujunhao.a202302010306.itemplatform.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.database.LocalUser
import com.wujunhao.a202302010306.itemplatform.databinding.ItemUserBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserAdapter : ListAdapter<LocalUser, UserAdapter.UserViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(user: LocalUser) {
            binding.apply {
                tvUsername.text = "用户名: ${user.username}"
                tvRealName.text = "姓名: ${user.realName}"
                tvStudentId.text = "学号: ${user.studentId}"
                tvDepartment.text = "院系: ${user.department}"
                tvEmail.text = "邮箱: ${user.email}"
                tvPhone.text = "电话: ${user.phone}"
                
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                tvCreatedAt.text = "注册时间: ${dateFormat.format(Date(user.createdAt))}"
            }
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<LocalUser>() {
        override fun areItemsTheSame(oldItem: LocalUser, newItem: LocalUser): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: LocalUser, newItem: LocalUser): Boolean {
            return oldItem == newItem
        }
    }
}