package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.database.LocalUser
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityAdminBinding
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.network.ApiService
import com.wujunhao.a202302010306.itemplatform.model.AdminUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var apiService: ApiService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        setupRecyclerView()
        loadUsers()
    }
    
    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "管理员界面 - 用户管理"
            setDisplayHomeAsUpEnabled(true)
        }
        
        binding.btnRefresh.setOnClickListener {
            loadUsers()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                navigateToMain()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        navigateToMain()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = userAdapter
        }
    }
    
    private fun loadUsers() {
        android.util.Log.d("AdminActivity", "开始加载用户列表")
        lifecycleScope.launch {
            showLoading(true)
            
            try {
                android.util.Log.d("AdminActivity", "创建API服务")
                apiService = ApiClient.createApiService(this@AdminActivity)
                android.util.Log.d("AdminActivity", "调用API获取用户列表")
                val response = withContext(Dispatchers.IO) {
                    apiService.getAdminUsers(page = 1, limit = 100)
                }
                
                android.util.Log.d("AdminActivity", "Response code: ${response.code()}")
                android.util.Log.d("AdminActivity", "Response body: ${response.body()}")
                android.util.Log.d("AdminActivity", "Response error: ${response.errorBody()?.string()}")
                
                if (response.isSuccessful && response.body()?.code == 200) {
                    val adminUsers = response.body()?.data?.users ?: emptyList()
                    android.util.Log.d("AdminActivity", "Admin users count: ${adminUsers.size}")
                    
                    val users = adminUsers.map { adminUser ->
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val createdAt = try {
                            dateFormat.parse(adminUser.createdAt)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        LocalUser(
                            id = adminUser.id,
                            username = adminUser.username,
                            password = "",
                            realName = adminUser.realName ?: "",
                            studentId = adminUser.studentId ?: "",
                            department = adminUser.department ?: "",
                            email = adminUser.email ?: "",
                            phone = adminUser.phone ?: "",
                            createdAt = createdAt
                        )
                    }
                    
                    userAdapter.submitList(users)
                    binding.tvUserCount.text = "用户总数: ${users.size}"
                    android.util.Log.d("AdminActivity", "Users count: ${users.size}")
                    android.util.Log.d("AdminActivity", "用户列表加载成功")
                } else {
                    val errorMessage = response.body()?.message ?: "未知错误"
                    Toast.makeText(this@AdminActivity, "加载用户列表失败: $errorMessage", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("AdminActivity", "加载用户列表失败: $errorMessage")
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "加载用户列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("AdminActivity", "加载用户列表异常: ${e.message}", e)
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}