package com.wujunhao.a202302010306.itemplatform.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.wujunhao.a202302010306.itemplatform.model.RegisterRequest
import com.wujunhao.a202302010306.itemplatform.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockUserRepository: UserRepository
    
    private lateinit var registerViewModel: RegisterViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var application: Application
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        application = ApplicationProvider.getApplicationContext()
        registerViewModel = RegisterViewModel(application)
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
    }
    
    @Test
    fun `register should update state to Loading then Success`() = testScope.runTest {
        // Given
        val registerRequest = RegisterRequest(
            username = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "13800138000",
            realName = "测试用户",
            studentId = "2023001",
            department = "计算机学院"
        )
        
        // 模拟成功的注册响应
        `when`(mockUserRepository.register(any())).thenReturn(
            Result.success(
                com.wujunhao.a202302010306.itemplatform.model.RegisterResponse(
                    code = 200,
                    message = "注册成功",
                    data = null
                )
            )
        )
        
        // When
        registerViewModel.register(registerRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = registerViewModel.uiState.value
        assertTrue(uiState is RegisterViewModel.RegisterUiState.Success)
    }
    
    @Test
    fun `register should update state to Loading then Error on failure`() = testScope.runTest {
        // Given
        val registerRequest = RegisterRequest(
            username = "existinguser",
            password = "password123",
            email = "test@example.com",
            phone = "13800138000",
            realName = "测试用户",
            studentId = "2023001",
            department = "计算机学院"
        )
        
        // 模拟失败的注册响应
        `when`(mockUserRepository.register(any())).thenReturn(
            Result.failure(Exception("用户名已存在"))
        )
        
        // When
        registerViewModel.register(registerRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = registerViewModel.uiState.value
        assertTrue(uiState is RegisterViewModel.RegisterUiState.Error)
        val errorState = uiState as RegisterViewModel.RegisterUiState.Error
        assertEquals("用户名已存在", errorState.message)
    }
    
    @Test
    fun `initial state should be Idle`() {
        // Given & When
        val initialState = registerViewModel.uiState.value
        
        // Then
        assertTrue(initialState is RegisterViewModel.RegisterUiState.Idle)
    }
    
    @Test
    fun `state should be Loading during registration`() = testScope.runTest {
        // Given
        val registerRequest = RegisterRequest(
            username = "testuser",
            password = "password123",
            email = "test@example.com",
            phone = "13800138000",
            realName = "测试用户",
            studentId = "2023001",
            department = "计算机学院"
        )
        
        // 模拟延迟响应
        `when`(mockUserRepository.register(any())).thenReturn(
            kotlinx.coroutines.delay(1000)
            Result.success(
                com.wujunhao.a202302010306.itemplatform.model.RegisterResponse(
                    code = 200,
                    message = "注册成功",
                    data = null
                )
            )
        )
        
        // When
        registerViewModel.register(registerRequest)
        
        // 验证加载状态
        val loadingState = registerViewModel.uiState.value
        assertTrue(loadingState is RegisterViewModel.RegisterUiState.Loading)
        
        // 等待完成
        testDispatcher.scheduler.advanceUntilIdle()
    }
}