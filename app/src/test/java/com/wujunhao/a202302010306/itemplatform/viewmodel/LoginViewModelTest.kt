package com.wujunhao.a202302010306.itemplatform.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.wujunhao.a202302010306.itemplatform.model.LoginRequest
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
class LoginViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockUserRepository: UserRepository
    
    private lateinit var loginViewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var application: Application
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        application = ApplicationProvider.getApplicationContext()
        loginViewModel = LoginViewModel(application)
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
    }
    
    @Test
    fun `login should update state to Loading then Success`() = testScope.runTest {
        // Given
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )
        
        // 模拟成功的登录响应
        `when`(mockUserRepository.login(any())).thenReturn(
            Result.success(
                com.wujunhao.a202302010306.itemplatform.model.LoginResponse(
                    code = 200,
                    message = "登录成功",
                    data = com.wujunhao.a202302010306.itemplatform.model.LoginData(
                        token = "test.jwt.token",
                        expiresIn = 3600,
                        user = com.wujunhao.a202302010306.itemplatform.model.User(
                            id = 1L,
                            username = "testuser",
                            email = "test@example.com",
                            phone = "13800138000",
                            realName = "测试用户",
                            studentId = "2023001",
                            department = "计算机学院",
                            avatar = null,
                            createdAt = "2024-01-01T00:00:00Z",
                            updatedAt = "2024-01-01T00:00:00Z"
                        )
                    )
                )
            )
        )
        
        // When
        loginViewModel.login(loginRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = loginViewModel.uiState.value
        assertTrue(uiState is LoginViewModel.LoginUiState.Success)
    }
    
    @Test
    fun `login should update state to Loading then Error on failure`() = testScope.runTest {
        // Given
        val loginRequest = LoginRequest(
            username = "wronguser",
            password = "wrongpassword"
        )
        
        // 模拟失败的登录响应
        `when`(mockUserRepository.login(any())).thenReturn(
            Result.failure(Exception("用户名或密码错误"))
        )
        
        // When
        loginViewModel.login(loginRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = loginViewModel.uiState.value
        assertTrue(uiState is LoginViewModel.LoginUiState.Error)
        val errorState = uiState as LoginViewModel.LoginUiState.Error
        assertEquals("用户名或密码错误", errorState.message)
    }
    
    @Test
    fun `initial state should be Idle`() {
        // Given & When
        val initialState = loginViewModel.uiState.value
        
        // Then
        assertTrue(initialState is LoginViewModel.LoginUiState.Idle)
    }
    
    @Test
    fun `state should be Loading during login`() = testScope.runTest {
        // Given
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )
        
        // 模拟延迟响应
        `when`(mockUserRepository.login(any())).thenReturn(
            kotlinx.coroutines.delay(1000)
            Result.success(
                com.wujunhao.a202302010306.itemplatform.model.LoginResponse(
                    code = 200,
                    message = "登录成功",
                    data = com.wujunhao.a202302010306.itemplatform.model.LoginData(
                        token = "test.jwt.token",
                        expiresIn = 3600,
                        user = com.wujunhao.a202302010306.itemplatform.model.User(
                            id = 1L,
                            username = "testuser",
                            email = "test@example.com",
                            phone = "13800138000",
                            realName = "测试用户",
                            studentId = "2023001",
                            department = "计算机学院",
                            avatar = null,
                            createdAt = "2024-01-01T00:00:00Z",
                            updatedAt = "2024-01-01T00:00:00Z"
                        )
                    )
                )
            )
        )
        
        // When
        loginViewModel.login(loginRequest)
        
        // 验证加载状态
        val loadingState = loginViewModel.uiState.value
        assertTrue(loadingState is LoginViewModel.LoginUiState.Loading)
        
        // 等待完成
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    @Test
    fun `login with empty username should show validation error`() = testScope.runTest {
        // Given
        val loginRequest = LoginRequest(
            username = "",
            password = "password123"
        )
        
        // When
        loginViewModel.login(loginRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = loginViewModel.uiState.value
        assertTrue(uiState is LoginViewModel.LoginUiState.Error)
        val errorState = uiState as LoginViewModel.LoginUiState.Error
        assertTrue(errorState.message.contains("用户名不能为空") || errorState.message.contains("用户名"))
    }
    
    @Test
    fun `login with empty password should show validation error`() = testScope.runTest {
        // Given
        val loginRequest = LoginRequest(
            username = "testuser",
            password = ""
        )
        
        // When
        loginViewModel.login(loginRequest)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val uiState = loginViewModel.uiState.value
        assertTrue(uiState is LoginViewModel.LoginUiState.Error)
        val errorState = uiState as LoginViewModel.LoginUiState.Error
        assertTrue(errorState.message.contains("密码不能为空") || errorState.message.contains("密码"))
    }
    
    @Test
    fun `provideFactory should create correct ViewModel`() {
        // Given
        val application = ApplicationProvider.getApplicationContext<Application>()
        
        // When
        val factory = LoginViewModel.provideFactory(application)
        val viewModel = factory.create(LoginViewModel::class.java)
        
        // Then
        assertNotNull(viewModel)
        assertTrue(viewModel is LoginViewModel)
    }
}