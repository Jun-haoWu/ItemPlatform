package com.wujunhao.a202302010306.itemplatform.ui

import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.wujunhao.a202302010306.itemplatform.adapter.ProductImageAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.FavoriteDao
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityProductDetailBinding
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.model.CloudProduct
import com.wujunhao.a202302010306.itemplatform.model.FavoritesStatusRequest
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.network.ApiService
import com.wujunhao.a202302010306.itemplatform.network.CloudConfig
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import com.wujunhao.a202302010306.itemplatform.service.FavoriteSyncService
import com.wujunhao.a202302010306.itemplatform.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProductDetailBinding
    private lateinit var productDao: ProductDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var favoriteSyncService: FavoriteSyncService
    private lateinit var apiService: ApiService
    private var productId: Long = -1L
    private var currentProduct: Product? = null
    private var currentUserId: Long = -1L
    private var isFavorite: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get product ID from intent
        productId = intent.getLongExtra("product_id", -1L)
        if (productId == -1L) {
            Toast.makeText(this, "商品ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Get current user
        val userInfo = TokenManager.getUserInfo(this)
        currentUserId = userInfo?.userId ?: -1L
        
        // Initialize DAO
        val databaseHelper = DatabaseHelper(this)
        productDao = ProductDao(databaseHelper)
        favoriteDao = FavoriteDao(databaseHelper)
        favoriteSyncService = FavoriteSyncService(this, databaseHelper, favoriteDao)
        
        // Initialize API service
        apiService = ApiClient.createApiService(this)
        
        setupToolbar()
        setupClickListeners()
        loadProductDetails()
        checkFavoriteStatus()
        
        // 同步收藏数据
        if (currentUserId != -1L) {
            syncFavorites()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 页面恢复时更新收藏状态
        if (currentUserId != -1L) {
            updateFavoriteStatusFromCloud()
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupClickListeners() {
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }
        
        binding.btnContactSeller.setOnClickListener {
            // TODO: 实现联系卖家功能
            Toast.makeText(this, "联系卖家功能开发中", Toast.LENGTH_SHORT).show()
        }
        
        binding.btnViewMap.setOnClickListener {
            currentProduct?.let { product ->
                if (product.hasLocation()) {
                    val intent = android.content.Intent(this, MapActivity::class.java).apply {
                        putExtra("product_id", product.id)
                        putExtra("product_name", product.title)
                        putExtra("latitude", product.latitude)
                        putExtra("longitude", product.longitude)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "该商品没有位置信息", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        binding.btnReport.setOnClickListener {
            // TODO: 实现举报功能
            Toast.makeText(this, "举报功能开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadProductDetails() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // 先尝试从云端获取商品详情
                val response = withContext(Dispatchers.IO) {
                    apiService.getProductDetail(productId)
                }
                
                showLoading(false)
                
                if (response.isSuccessful && response.body() != null) {
                    val cloudProduct = response.body()!!.product
                    val product = cloudProduct.toLocalProduct(currentUserId)
                    currentProduct = product
                    
                    // 将云端商品保存到本地数据库
                    withContext(Dispatchers.IO) {
                        val existingProduct = productDao.getProductById(productId)
                        if (existingProduct == null) {
                            // 如果本地不存在，插入新商品
                            productDao.insertProduct(product)
                        } else {
                            // 如果本地已存在，更新商品信息
                            productDao.updateProduct(product)
                        }
                    }
                    
                    displayProductDetails(product)
                } else {
                    // 云端获取失败，尝试从本地数据库获取
                    val localProduct = withContext(Dispatchers.IO) {
                        productDao.getProductById(productId)
                    }
                    
                    if (localProduct != null) {
                        currentProduct = localProduct
                        displayProductDetails(localProduct)
                    } else {
                        Toast.makeText(this@ProductDetailActivity, "商品不存在", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                
            } catch (e: IOException) {
                showLoading(false)
                // 网络错误，尝试从本地数据库获取
                try {
                    val localProduct = withContext(Dispatchers.IO) {
                        productDao.getProductById(productId)
                    }
                    
                    if (localProduct != null) {
                        currentProduct = localProduct
                        displayProductDetails(localProduct)
                    } else {
                        Toast.makeText(this@ProductDetailActivity, "网络错误，且本地无商品数据", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e2: Exception) {
                    Toast.makeText(this@ProductDetailActivity, "加载商品详情失败: ${e2.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                showLoading(false)
                // HTTP错误，尝试从本地数据库获取
                try {
                    val localProduct = withContext(Dispatchers.IO) {
                        productDao.getProductById(productId)
                    }
                    
                    if (localProduct != null) {
                        currentProduct = localProduct
                        displayProductDetails(localProduct)
                    } else {
                        Toast.makeText(this@ProductDetailActivity, "商品不存在", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e2: Exception) {
                    Toast.makeText(this@ProductDetailActivity, "加载商品详情失败: ${e2.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@ProductDetailActivity, "加载商品详情失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun displayProductDetails(product: Product) {
        // 设置标题
        supportActionBar?.title = product.title
        
        // 显示图片
        displayProductImages(product.images)
        
        // 显示基本信息
        binding.tvProductTitle.text = product.title
        binding.tvProductPrice.text = "¥${String.format("%.2f", product.price)}"
        binding.tvProductLocation.text = product.location
        binding.tvProductCategory.text = product.category
        binding.tvProductCondition.text = product.condition
        
        // 显示统计信息
        binding.tvViewCount.text = "${product.viewCount} 浏览"
        binding.tvLikeCount.text = "${product.likeCount} 收藏"
        
        // 显示时间
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.tvCreatedTime.text = "发布时间: ${dateFormat.format(Date(product.createdAt))}"
        
        // 显示描述
        binding.tvProductDescription.text = product.description
        
        // 显示状态
        when (product.status) {
            Product.STATUS_ACTIVE -> {
                binding.tvProductStatus.text = "在售"
                binding.tvProductStatus.setBackgroundResource(R.drawable.status_background)
            }
            Product.STATUS_SOLD -> {
                binding.tvProductStatus.text = "已售"
                binding.tvProductStatus.setBackgroundResource(android.R.color.darker_gray)
            }
            else -> {
                binding.tvProductStatus.text = "未知"
                binding.tvProductStatus.setBackgroundResource(android.R.color.darker_gray)
            }
        }
        
        // 显示卖家信息（TODO: 获取卖家详细信息）
        binding.tvSellerName.text = "卖家ID: ${product.sellerId}"
        // TODO: 获取并显示卖家头像、信誉等信息
    }
    
    private fun displayProductImages(imagesString: String?) {
        if (imagesString.isNullOrEmpty()) {
            binding.viewPagerImages.visibility = View.GONE
            binding.indicatorDots.visibility = View.GONE
            binding.layoutNoImages.visibility = View.VISIBLE
            return
        }
        
        val imagePaths = ImageUtils.getProductImagePaths(imagesString)
        if (imagePaths.isEmpty()) {
            binding.viewPagerImages.visibility = View.GONE
            binding.indicatorDots.visibility = View.GONE
            binding.layoutNoImages.visibility = View.VISIBLE
            return
        }
        
        // 加载图片
        val bitmaps = mutableListOf<Bitmap>()
        for (imagePath in imagePaths) {
            val imagePathToLoad = if (imagePath.startsWith("/uploads/")) {
                CloudConfig.getServerBaseUrl() + imagePath
            } else {
                imagePath
            }
            val bitmap = ImageUtils.loadImage(this, imagePathToLoad)
            if (bitmap != null) {
                bitmaps.add(bitmap)
            }
        }
        
        if (bitmaps.isEmpty()) {
            binding.viewPagerImages.visibility = View.GONE
            binding.indicatorDots.visibility = View.GONE
            binding.layoutNoImages.visibility = View.VISIBLE
            return
        }
        
        // 设置图片轮播
        val imageAdapter = ProductImageAdapter(bitmaps)
        binding.viewPagerImages.adapter = imageAdapter
        
        // 创建简单的指示器
        setupDotsIndicator(bitmaps.size)
        
        binding.viewPagerImages.visibility = View.VISIBLE
        binding.indicatorDots.visibility = View.VISIBLE
        binding.layoutNoImages.visibility = View.GONE
        
        // 设置图片切换监听器
        binding.viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDotsIndicator(position)
            }
        })
    }
    
    private fun checkFavoriteStatus() {
        if (currentUserId == -1L) {
            binding.btnFavorite.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            try {
                val isFavoritedInCloud = withContext(Dispatchers.IO) {
                    try {
                        val response = apiService.getFavoritesStatus(FavoritesStatusRequest(listOf(productId)))
                        if (response.isSuccessful && response.body() != null) {
                            val statusMap = response.body()!!.status
                            statusMap[productId.toString()] ?: false
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProductDetailActivity", "获取云端收藏状态失败", e)
                        false
                    }
                }
                
                isFavorite = isFavoritedInCloud
                updateFavoriteButton()
                
                withContext(Dispatchers.IO) {
                    if (isFavorite) {
                        if (!favoriteDao.isProductFavorited(currentUserId, productId)) {
                            favoriteDao.addFavorite(currentUserId, productId, System.currentTimeMillis())
                        }
                    } else {
                        if (favoriteDao.isProductFavorited(currentUserId, productId)) {
                            favoriteDao.removeFavorite(currentUserId, productId)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailActivity", "检查收藏状态失败", e)
            }
        }
    }
    
    private fun refreshProductDetail() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getProductDetail(productId)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val cloudProduct = response.body()!!.product
                    val product = cloudProduct.toLocalProduct(currentUserId)
                    currentProduct = product
                    
                    withContext(Dispatchers.IO) {
                        productDao.updateProduct(product)
                    }
                    
                    binding.tvLikeCount.text = "${product.likeCount} 收藏"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailActivity", "刷新商品详情失败", e)
            }
        }
    }
    
    private fun toggleFavorite() {
        if (currentUserId == -1L) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val isAddAction = !isFavorite
                
                val apiResult = withContext(Dispatchers.IO) {
                    try {
                        if (isAddAction) {
                            val response = apiService.addFavorite(productId)
                            Pair(response.isSuccessful, response.code())
                        } else {
                            val response = apiService.removeFavorite(productId)
                            Pair(response.isSuccessful, response.code())
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProductDetailActivity", "调用收藏API失败", e)
                        Pair(false, 0)
                    }
                }
                
                val (isSuccessful, statusCode) = apiResult
                
                if (isSuccessful || (isAddAction && statusCode == 409) || (!isAddAction && statusCode == 404)) {
                    val success = withContext(Dispatchers.IO) {
                        if (isAddAction) {
                            favoriteDao.addFavorite(currentUserId, productId, System.currentTimeMillis())
                        } else {
                            favoriteDao.removeFavorite(currentUserId, productId)
                        }
                    }
                    
                    if (success) {
                        isFavorite = isAddAction
                        updateFavoriteButton()
                        
                        val message = if (isFavorite) "已添加到收藏" else "已取消收藏"
                        Toast.makeText(this@ProductDetailActivity, message, Toast.LENGTH_SHORT).show()
                        
                        refreshProductDetail()
                    }
                } else {
                    Toast.makeText(this@ProductDetailActivity, "操作失败，请重试", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Toast.makeText(this@ProductDetailActivity, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateFavoriteButton() {
        if (isFavorite) {
            binding.btnFavorite.text = "取消收藏"
            binding.btnFavorite.setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
        } else {
            binding.btnFavorite.text = "收藏商品"
            binding.btnFavorite.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
        }
    }
    
    /**
     * 同步收藏数据
     */
    private fun syncFavorites() {
        lifecycleScope.launch {
            try {
                // 检查是否需要同步
                if (favoriteSyncService.shouldSync(currentUserId)) {
                    val syncSuccess = withContext(Dispatchers.IO) {
                        favoriteSyncService.syncFavoritesBidirectional(currentUserId)
                    }
                    
                    if (syncSuccess) {
                        // 同步成功后重新检查收藏状态
                        checkFavoriteStatus()
                        android.util.Log.d("ProductDetailActivity", "收藏数据同步成功")
                    } else {
                        android.util.Log.w("ProductDetailActivity", "收藏数据同步失败")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailActivity", "同步收藏数据时发生错误", e)
            }
        }
    }
    
    /**
     * 延迟同步收藏数据（避免频繁同步）
     */
    private fun syncFavoritesAfterDelay() {
        lifecycleScope.launch {
            delay(2000) // 延迟2秒同步
            syncFavorites()
        }
    }
    
    /**
     * 实时更新收藏状态（从云端获取最新状态）
     */
    private fun updateFavoriteStatusFromCloud() {
        if (currentUserId == -1L) return
        
        lifecycleScope.launch {
            try {
                // 从云端获取最新的收藏状态
                val isFavoritedInCloud = withContext(Dispatchers.IO) {
                    // 这里可以调用API获取云端收藏状态
                    // 暂时使用本地状态作为参考
                    favoriteDao.isProductFavorited(currentUserId, productId)
                }
                
                // 如果云端状态与本地状态不一致，更新UI
                if (isFavoritedInCloud != isFavorite) {
                    isFavorite = isFavoritedInCloud
                    updateFavoriteButton()
                    
                    // 更新收藏数显示
                    if (currentProduct != null) {
                        val updatedProduct = withContext(Dispatchers.IO) {
                            productDao.getProductById(productId)
                        }
                        updatedProduct?.let {
                            binding.tvLikeCount.text = "${it.likeCount} 收藏"
                        }
                    }
                    
                    // 显示状态更新提示
                    Toast.makeText(this@ProductDetailActivity, "收藏状态已更新", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductDetailActivity", "更新收藏状态失败", e)
            }
        }
    }
    
    /**
     * 设置指示器点
     */
    private fun setupDotsIndicator(count: Int) {
        binding.indicatorDots.removeAllViews()
        val dots = arrayOfNulls<View>(count)
        
        for (i in 0 until count) {
            dots[i] = View(this)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(4, 0, 4, 0)
            dots[i]?.layoutParams = params
            
            val drawable = GradientDrawable()
            drawable.setShape(GradientDrawable.OVAL)
            drawable.setSize(8, 8)
            drawable.setColor(if (i == 0) resources.getColor(android.R.color.white) else resources.getColor(android.R.color.darker_gray))
            dots[i]?.background = drawable
            
            binding.indicatorDots.addView(dots[i])
        }
    }
    
    /**
     * 更新指示器点
     */
    private fun updateDotsIndicator(position: Int) {
        for (i in 0 until binding.indicatorDots.childCount) {
            val dot = binding.indicatorDots.getChildAt(i)
            val drawable = GradientDrawable()
            drawable.setShape(GradientDrawable.OVAL)
            drawable.setSize(8, 8)
            drawable.setColor(if (i == position) resources.getColor(android.R.color.white) else resources.getColor(android.R.color.darker_gray))
            dot.background = drawable
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollViewContent.visibility = if (show) View.GONE else View.VISIBLE
    }
}