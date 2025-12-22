package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityRegisterBinding
import com.wujunhao.a202302010306.itemplatform.model.RegisterRequest
import com.wujunhao.a202302010306.itemplatform.viewmodel.RegisterViewModel
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val registerViewModel: RegisterViewModel by viewModels {
        RegisterViewModel.provideFactory(application)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        binding.apply {
            btnRegister.setOnClickListener {
                performRegister()
            }
            
            tvLogin.setOnClickListener {
                navigateToLogin()
            }
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            registerViewModel.uiState.collect { state ->
                when (state) {
                    is RegisterViewModel.RegisterUiState.Loading -> {
                        showLoading(true)
                    }
                    is RegisterViewModel.RegisterUiState.Success -> {
                        showLoading(false)
                        showMessage("注册成功！请登录")
                        navigateToLogin()
                    }
                    is RegisterViewModel.RegisterUiState.Error -> {
                        showLoading(false)
                        showMessage(state.message)
                    }
                    is RegisterViewModel.RegisterUiState.Idle -> {
                        showLoading(false)
                    }
                }
            }
        }
    }
    
    private fun performRegister() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val realName = binding.etRealName.text.toString().trim()
        val studentId = binding.etStudentId.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        
        if (validateInput(username, password, confirmPassword, email, phone, realName, studentId, department)) {
            val request = RegisterRequest(
                username = username,
                password = password,
                email = email,
                phone = phone,
                realName = realName,
                studentId = studentId,
                department = department
            )
            registerViewModel.register(request)
        }
    }
    
    private fun validateInput(
        username: String,
        password: String,
        confirmPassword: String,
        email: String,
        phone: String,
        realName: String,
        studentId: String,
        department: String
    ): Boolean {
        return when {
            username.isEmpty() -> {
                showMessage("请输入用户名")
                false
            }
            password.isEmpty() || password.length < 6 -> {
                showMessage("密码长度至少6位")
                false
            }
            password != confirmPassword -> {
                showMessage("两次输入的密码不一致")
                false
            }
            email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showMessage("请输入有效的邮箱地址")
                false
            }
            phone.isEmpty() || phone.length != 11 -> {
                showMessage("请输入11位手机号")
                false
            }
            realName.isEmpty() -> {
                showMessage("请输入真实姓名")
                false
            }
            studentId.isEmpty() -> {
                showMessage("请输入学号")
                false
            }
            department.isEmpty() -> {
                showMessage("请输入院系")
                false
            }
            else -> true
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.apply {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnRegister.isEnabled = !isLoading
        }
    }
    
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}