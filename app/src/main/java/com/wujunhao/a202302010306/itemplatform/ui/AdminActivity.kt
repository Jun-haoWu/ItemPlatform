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
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.UserDao
import com.wujunhao.a202302010306.itemplatform.database.LocalUser
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityAdminBinding
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminBinding
    private lateinit var userAdapter: UserAdapter
    private lateinit var userDao: UserDao
    
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
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupRecyclerView() {
        userAdapter = UserAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = userAdapter
        }
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            showLoading(true)
            
            try {
                val databaseHelper = DatabaseHelper(this@AdminActivity)
                userDao = UserDao(databaseHelper)
                
                val users = withContext(Dispatchers.IO) {
                    userDao.getAllUsers()
                }
                
                userAdapter.submitList(users)
                binding.tvUserCount.text = "用户总数: ${users.size}"
                
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "加载用户列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}