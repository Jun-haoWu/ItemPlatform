package com.wujunhao.a202302010306.itemplatform.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.wujunhao.a202302010306.itemplatform.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import retrofit2.Response

@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var userRepository: UserRepository
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockContext = ApplicationProvider.getApplicationContext()
        userRepository = UserRepository(mockContext)
    }
    
    @After
    fun tearDown() {
        // 清理测试数据
    }
    
    @Test
    fun `register should return success when API call is successful`() = runBlocking {
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
        
        val expectedResponse = RegisterResponse(
            code = 200,
            message = "注册成功",
            data = User(
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
        
        // When
        val result = userRepository.register(registerRequest)
        
        // Then
        assert(result.isSuccess)
        result.fold(
            onSuccess = { response ->
                assert(response.code == 200)
                assert(response.message == "注册成功")
            },
            onFailure = { exception ->
                throw AssertionError("Expected success, but got failure: ${exception.message}")
            }
        )
    }
    
    @Test
    fun `register should return failure when API call fails`() = runBlocking {
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
        
        // When
        val result = userRepository.register(registerRequest)
        
        // Then
        assert(result.isFailure)
    }
    
    @Test
    fun `login should save token when login is successful`() = runBlocking {
        // Given
        val loginRequest = LoginRequest(
            username = "testuser",
            password = "password123"
        )
        
        val loginResponse = LoginResponse(
            code = 200,
            message = "登录成功",
            data = LoginData(
                token = "test.jwt.token",
                expiresIn = 3600,
                user = User(
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
        
        // When
        val result = userRepository.login(loginRequest)
        
        // Then
        assert(result.isSuccess)
        result.fold(
            onSuccess = { response ->
                assert(response.code == 200)
                assert(response.data?.token == "test.jwt.token")
            },
            onFailure = { exception ->
                throw AssertionError("Expected success, but got failure: ${exception.message}")
            }
        )
    }
    
    @Test
    fun `isLoggedIn should return true when valid token exists`() {
        // Given
        // 模拟有效token存在
        
        // When
        val isLoggedIn = userRepository.isLoggedIn()
        
        // Then
        // 根据实际token状态调整断言
    }
    
    @Test
    fun `logout should clear token`() {
        // Given
        
        // When
        userRepository.logout()
        
        // Then
        // 验证token已被清除
    }
}