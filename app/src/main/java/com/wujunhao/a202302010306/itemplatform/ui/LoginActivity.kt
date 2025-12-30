package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityLoginBinding
import com.wujunhao.a202302010306.itemplatform.model.LoginRequest
import com.wujunhao.a202302010306.itemplatform.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.provideFactory(application)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
        
        // 检查是否已登录
        if (intent?.flags?.and(Intent.FLAG_ACTIVITY_CLEAR_TASK) == 0 && loginViewModel.isLoggedIn()) {
            // 只有在不是从CLEAR_TASK启动且已登录时才跳转
            navigateToMain()
        }
    }
    
    private fun setupViews() {
        binding.apply {
            btnLogin.setOnClickListener {
                performLogin()
            }
            
            tvRegister.setOnClickListener {
                navigateToRegister()
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            loginViewModel.uiState.collect { state ->
                when (state) {
                    is LoginViewModel.LoginUiState.Loading -> {
                        showLoading(true)
                    }
                    is LoginViewModel.LoginUiState.Success -> {
                        showLoading(false)
                        showMessage("登录成功！")
                        android.util.Log.d("LoginActivity", "导航到主界面")
                        navigateToMain()
                    }
                    is LoginViewModel.LoginUiState.AdminSuccess -> {
                        showLoading(false)
                        showMessage("管理员登录成功！")
                        android.util.Log.d("LoginActivity", "导航到管理员界面")
                        navigateToAdmin()
                    }
                    is LoginViewModel.LoginUiState.Error -> {
                        showLoading(false)
                        showMessage(state.message)
                    }
                    is LoginViewModel.LoginUiState.Idle -> {
                        showLoading(false)
                    }
                }
            }
        }
    }
    
    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (validateInput(username, password)) {
            val request = LoginRequest(
                username = username,
                password = password
            )
            loginViewModel.login(request)
        }
    }
    
    private fun validateInput(username: String, password: String): Boolean {
        return when {
            username.isEmpty() -> {
                showMessage("请输入用户名")
                false
            }
            password.isEmpty() -> {
                showMessage("请输入密码")
                false
            }
            else -> true
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToMain() {
        android.util.Log.d("LoginActivity", "开始导航到MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        android.util.Log.d("LoginActivity", "完成导航到MainActivity")
    }
    
    private fun navigateToAdmin() {
        android.util.Log.d("LoginActivity", "开始导航到AdminActivity")
        val intent = Intent(this, AdminActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        android.util.Log.d("LoginActivity", "完成导航到AdminActivity")
    }
}