package com.wujunhao.a202302010306.itemplatform.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wujunhao.a202302010306.itemplatform.model.LoginRequest
import com.wujunhao.a202302010306.itemplatform.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application)
    
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            android.util.Log.d("LoginViewModel", "开始登录请求，用户名: ${request.username}")
            
            userRepository.login(request)
                .onSuccess { response ->
                    android.util.Log.d("LoginViewModel", "登录成功，响应码: ${response.code}")
                    android.util.Log.d("LoginViewModel", "响应数据: ${response.data}")
                    // 使用服务器返回的用户名来判断是否为管理员，而不是请求中的用户名
                    val username = response.data?.user?.username ?: request.username
                    android.util.Log.d("LoginViewModel", "判断管理员身份，用户名: $username")
                    if (username == "admin") {
                        _uiState.value = LoginUiState.AdminSuccess
                        android.util.Log.d("LoginViewModel", "管理员登录成功")
                    } else {
                        _uiState.value = LoginUiState.Success
                        android.util.Log.d("LoginViewModel", "普通用户登录成功")
                    }
                }
                .onFailure { exception ->
                    android.util.Log.e("LoginViewModel", "登录失败: ${exception.message}", exception)
                    _uiState.value = LoginUiState.Error(exception.message ?: "登录失败")
                }
        }
    }
    
    fun isLoggedIn(): Boolean {
        return userRepository.isLoggedIn()
    }
    
    fun logout() {
        userRepository.logout()
    }
    
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return LoginViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
    
    sealed class LoginUiState {
        object Idle : LoginUiState()
        object Loading : LoginUiState()
        object Success : LoginUiState()
        object AdminSuccess : LoginUiState()
        data class Error(val message: String) : LoginUiState()
    }
}