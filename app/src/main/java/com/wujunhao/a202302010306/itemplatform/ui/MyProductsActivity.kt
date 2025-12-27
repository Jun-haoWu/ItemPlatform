package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wujunhao.a202302010306.itemplatform.adapter.MyProductsAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityMyProductsBinding
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyProductsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMyProductsBinding
    private lateinit var productDao: ProductDao
    private lateinit var adapter: MyProductsAdapter
    private var currentUserId: Long = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get current user first
        val userInfo = TokenManager.getUserInfo(this)
        if (userInfo == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        currentUserId = userInfo.userId
        
        // Initialize DAO
        val databaseHelper = DatabaseHelper(this)
        productDao = ProductDao(databaseHelper)
        
        // Initialize UI components
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        
        // Load products
        loadMyProducts()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = MyProductsAdapter(
            products = emptyList(),
            onEditClick = { product ->
                editProduct(product)
            },
            onDeleteClick = { product ->
                confirmDeleteProduct(product)
            },
            onItemClick = { product ->
                // Navigate to product detail page
                val intent = Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra("product_id", product.id)
                }
                startActivity(intent)
            }
        )
        
        binding.rvMyProducts.layoutManager = LinearLayoutManager(this)
        binding.rvMyProducts.adapter = adapter
    }
    
    private fun setupClickListeners() {
        binding.fabAddProduct.setOnClickListener {
            // Navigate to publish fragment (via main activity)
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("navigate_to_publish", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun loadMyProducts() {
        if (currentUserId == -1L) return
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productDao.getProductsBySeller(currentUserId)
                }
                
                showLoading(false)
                
                if (products.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    adapter.updateProducts(products)
                }
                
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@MyProductsActivity, "加载商品失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvMyProducts.visibility = if (show) View.GONE else View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }
    
    private fun showEmptyState(show: Boolean) {
        binding.layoutEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvMyProducts.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun editProduct(product: Product) {
        // Navigate to edit product (reuse publish fragment)
        android.util.Log.d("MyProductsActivity", "编辑商品: ID=${product.id}, 标题=${product.title}")
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("navigate_to_publish", true)
            putExtra("edit_product_id", product.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
    
    private fun confirmDeleteProduct(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("删除商品")
            .setMessage("确定要删除商品\"${product.title}\"吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteProduct(product)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun deleteProduct(product: Product) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Delete product images first
                    if (!product.images.isNullOrEmpty()) {
                        val imagePaths = ImageUtils.getProductImagePaths(product.images)
                        for (imagePath in imagePaths) {
                            ImageUtils.deleteImage(this@MyProductsActivity, imagePath)
                        }
                    }
                    
                    // Delete product from database
                    productDao.deleteProduct(product.id)
                }
                
                Toast.makeText(this@MyProductsActivity, "商品已删除", Toast.LENGTH_SHORT).show()
                loadMyProducts() // Refresh the list
                
            } catch (e: Exception) {
                Toast.makeText(this@MyProductsActivity, "删除失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadMyProducts() // Refresh when returning to this activity
    }
}