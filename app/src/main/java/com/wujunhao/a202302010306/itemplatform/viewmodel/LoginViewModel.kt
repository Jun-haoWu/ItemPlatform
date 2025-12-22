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
            
            // 检查是否为管理员登录
            if (request.username == "admin" && request.password == "123456") {
                // 管理员登录成功
                _uiState.value = LoginUiState.AdminSuccess
                return@launch
            }
            
            userRepository.login(request)
                .onSuccess { response ->
                    _uiState.value = LoginUiState.Success
                }
                .onFailure { exception ->
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