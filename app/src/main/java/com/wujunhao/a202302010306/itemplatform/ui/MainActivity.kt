package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
    }
    
    private fun setupViews() {
        binding.apply {
            textView.text = "欢迎使用校园二手交易平台！"
            
            btnLogout.setOnClickListener {
                // Clear authentication data
                TokenManager.clearToken(this@MainActivity)
                
                // Clear SQLite authentication data
                val databaseHelper = com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper(this@MainActivity)
                val authDao = com.wujunhao.a202302010306.itemplatform.database.AuthDao(databaseHelper)
                authDao.clearAllTokens()
                
                // Navigate to login screen
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}