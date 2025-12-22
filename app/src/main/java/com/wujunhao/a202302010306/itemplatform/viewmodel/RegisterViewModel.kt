package com.wujunhao.a202302010306.itemplatform.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wujunhao.a202302010306.itemplatform.model.RegisterRequest
import com.wujunhao.a202302010306.itemplatform.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application)
    
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()
    
    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            
            userRepository.register(request)
                .onSuccess { response ->
                    _uiState.value = RegisterUiState.Success
                }
                .onFailure { exception ->
                    _uiState.value = RegisterUiState.Error(exception.message ?: "注册失败")
                }
        }
    }
    
    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return RegisterViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
    
    sealed class RegisterUiState {
        object Idle : RegisterUiState()
        object Loading : RegisterUiState()
        object Success : RegisterUiState()
        data class Error(val message: String) : RegisterUiState()
    }
}