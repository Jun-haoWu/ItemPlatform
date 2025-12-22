package com.wujunhao.a202302010306.itemplatform.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        
        return when {
            url.contains("/auth/register") -> {
                // 模拟注册成功响应
                val responseBody = """
                    {
                        "success": true,
                        "message": "注册成功",
                        "data": {
                            "user": {
                                "id": 1,
                                "username": "testuser",
                                "email": "test@example.com"
                            }
                        }
                    }
                """.trimIndent()
                
                Response.Builder()
                    .code(200)
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .message("OK")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
            }
            
            url.contains("/auth/login") -> {
                // 模拟登录成功响应
                val responseBody = """
                    {
                        "success": true,
                        "message": "登录成功",
                        "data": {
                            "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                            "user": {
                                "id": 1,
                                "username": "testuser",
                                "email": "test@example.com"
                            }
                        }
                    }
                """.trimIndent()
                
                Response.Builder()
                    .code(200)
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .message("OK")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
            }
            
            else -> {
                // 对于其他请求，返回404
                val responseBody = """
                    {
                        "success": false,
                        "message": "API 不存在"
                    }
                """.trimIndent()
                
                Response.Builder()
                    .code(404)
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .message("Not Found")
                    .body(responseBody.toResponseBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .build()
            }
        }
    }
}