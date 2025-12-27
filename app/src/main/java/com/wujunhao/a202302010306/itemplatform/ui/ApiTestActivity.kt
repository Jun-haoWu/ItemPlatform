package com.wujunhao.a202302010306.itemplatform.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.network.CloudConfig
import com.wujunhao.a202302010306.itemplatform.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ApiTestActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_test)

        tvLog = findViewById(R.id.tvLog)
        scrollView = findViewById(R.id.scrollView)

        apiService = ApiClient.createApiService(this)

        setupButtons()

        log("========================================")
        log("API 测试程序")
        log("========================================")
        log("当前服务器配置:")
        log("  - 本地服务器: ${CloudConfig.LOCAL_SERVER_URL}")
        log("  - 云服务器: ${CloudConfig.CLOUD_SERVER_URL}")
        log("  - 使用云服务器: ${CloudConfig.USE_CLOUD_SERVER}")
        log("  - 实际使用的URL: ${CloudConfig.getCurrentBaseUrl()}")
        log("========================================\n")
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnTestConnection).setOnClickListener {
            testConnection()
        }

        findViewById<Button>(R.id.btnGetProducts).setOnClickListener {
            getProducts()
        }

        findViewById<Button>(R.id.btnGetProductDetail).setOnClickListener {
            getProductDetail()
        }

        findViewById<Button>(R.id.btnTestLogin).setOnClickListener {
            testLogin()
        }

        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            tvLog.text = ""
            log("日志已清空\n")
        }
    }

    private fun testConnection() {
        log("\n--- 测试服务器连接 ---")
        log("正在连接服务器...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getProducts(
                        page = 1,
                        limit = 1
                    )
                }

                if (response.isSuccessful) {
                    log("✓ 服务器连接成功!")
                    log("  状态码: ${response.code()}")
                    log("  响应时间: ${System.currentTimeMillis()}")
                } else {
                    log("✗ 服务器连接失败")
                    log("  状态码: ${response.code()}")
                    log("  错误信息: ${response.message()}")
                }
            } catch (e: IOException) {
                log("✗ 网络连接错误")
                log("  错误信息: ${e.message}")
                log("  请检查:")
                log("  1. 服务器是否运行")
                log("  2. 网络连接是否正常")
                log("  3. 防火墙是否允许访问")
            } catch (e: HttpException) {
                log("✗ HTTP错误")
                log("  状态码: ${e.response()?.code()}")
                log("  错误信息: ${e.message}")
            } catch (e: Exception) {
                log("✗ 未知错误")
                log("  错误类型: ${e.javaClass.simpleName}")
                log("  错误信息: ${e.message}")
                Log.e("ApiTestActivity", "测试连接失败", e)
            }
        }
    }

    private fun getProducts() {
        log("\n--- 获取商品列表 ---")
        log("正在获取商品列表...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getProducts(
                        page = 1,
                        limit = 10
                    )
                }

                if (response.isSuccessful) {
                    val productsResponse = response.body()
                    log("✓ 获取商品列表成功!")
                    log("  状态码: ${response.code()}")
                    log("  商品总数: ${productsResponse?.pagination?.total ?: 0}")
                    log("  当前页商品数: ${productsResponse?.products?.size ?: 0}")

                    productsResponse?.products?.forEach { product ->
                        log("  - [${product.id}] ${product.name}")
                        log("    价格: ¥${product.price}")
                        log("    分类: ${product.category}")
                        log("    浏览: ${product.viewCount} | 收藏: ${product.likeCount}")
                    }
                } else {
                    log("✗ 获取商品列表失败")
                    log("  状态码: ${response.code()}")
                    log("  错误信息: ${response.message()}")
                }
            } catch (e: IOException) {
                log("✗ 网络连接错误")
                log("  错误信息: ${e.message}")
            } catch (e: HttpException) {
                log("✗ HTTP错误")
                log("  状态码: ${e.response()?.code()}")
                log("  错误信息: ${e.message}")
            } catch (e: Exception) {
                log("✗ 未知错误")
                log("  错误类型: ${e.javaClass.simpleName}")
                log("  错误信息: ${e.message}")
                Log.e("ApiTestActivity", "获取商品列表失败", e)
            }
        }
    }

    private fun getProductDetail() {
        log("\n--- 获取商品详情 ---")
        log("正在获取商品详情 (ID: 1)...")

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getProductDetail(1)
                }

                if (response.isSuccessful) {
                    val productResponse = response.body()
                    log("✓ 获取商品详情成功!")
                    log("  状态码: ${response.code()}")

                    productResponse?.product?.let { product ->
                        log("  商品ID: ${product.id}")
                        log("  商品名称: ${product.name}")
                        log("  价格: ¥${product.price}")
                        log("  分类: ${product.category}")
                        log("  地点: ${product.location}")
                        log("  浏览量: ${product.viewCount}")
                        log("  收藏数: ${product.likeCount}")
                        log("  描述: ${product.description?.take(50)}...")
                    }
                } else {
                    log("✗ 获取商品详情失败")
                    log("  状态码: ${response.code()}")
                    log("  错误信息: ${response.message()}")
                }
            } catch (e: IOException) {
                log("✗ 网络连接错误")
                log("  错误信息: ${e.message}")
            } catch (e: HttpException) {
                log("✗ HTTP错误")
                log("  状态码: ${e.response()?.code()}")
                log("  错误信息: ${e.message}")
            } catch (e: Exception) {
                log("✗ 未知错误")
                log("  错误类型: ${e.javaClass.simpleName}")
                log("  错误信息: ${e.message}")
                Log.e("ApiTestActivity", "获取商品详情失败", e)
            }
        }
    }

    private fun testLogin() {
        log("\n--- 测试登录 ---")
        log("正在测试登录...")

        lifecycleScope.launch {
            try {
                val loginRequest = com.wujunhao.a202302010306.itemplatform.model.LoginRequest(
                    username = "testuser",
                    password = "testpass"
                )

                val loginResponse = withContext(Dispatchers.IO) {
                    apiService.login(loginRequest)
                }

                log("✓ 登录请求成功!")
                log("  状态码: ${loginResponse.code}")
                log("  消息: ${loginResponse.message}")
                log("  Token: ${loginResponse.data?.token?.take(20)}...")
            } catch (e: IOException) {
                log("✗ 网络连接错误")
                log("  错误信息: ${e.message}")
            } catch (e: HttpException) {
                log("✗ HTTP错误")
                log("  状态码: ${e.response()?.code()}")
                log("  错误信息: ${e.message}")
            } catch (e: Exception) {
                log("✗ 未知错误")
                log("  错误类型: ${e.javaClass.simpleName}")
                log("  错误信息: ${e.message}")
                Log.e("ApiTestActivity", "测试登录失败", e)
            }
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            tvLog.append("[$timestamp] $message\n")
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }
}
