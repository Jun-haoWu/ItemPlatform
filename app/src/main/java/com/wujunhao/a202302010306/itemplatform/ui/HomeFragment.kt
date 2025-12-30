package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.ProductAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.database.FavoriteDao
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentHomeBinding
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.model.FavoritesStatusRequest
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.network.ApiService
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productDao: ProductDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var productAdapter: ProductAdapter
    private lateinit var apiService: ApiService
    private var currentProducts: List<Product> = emptyList()
    private var isLoading = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize DAO
        val databaseHelper = DatabaseHelper(requireContext())
        databaseHelper.ensureProductsTableExists()
        productDao = ProductDao(databaseHelper)
        favoriteDao = FavoriteDao(databaseHelper)
        
        // Initialize API Service
        apiService = ApiClient.createApiService(requireContext())
        
        // Setup RecyclerView with StaggeredGridLayoutManager for waterfall effect
        setupRecyclerView()
        
        // Setup spinners
        setupSpinners()
        
        // Setup search
        setupSearch()
        
        // Setup pull-to-refresh
        setupSwipeRefresh()
        
        // Load products
        loadProducts()
        
        // Create sample products for testing if no products exist
        createSampleProductsIfNeeded()
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Handle product click - navigate to product detail page
            val intent = Intent(context, ProductDetailActivity::class.java).apply {
                putExtra("product_id", product.id)
            }
            startActivity(intent)
            
            // Increment view count
            lifecycleScope.launch {
                productDao.incrementViewCount(product.id)
            }
        }
        
        // Use StaggeredGridLayoutManager for waterfall effect
        val layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL)
        binding.productsRecyclerView.layoutManager = layoutManager
        binding.productsRecyclerView.adapter = productAdapter
    }
    
    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("全部类别", "电子产品", "书籍资料", "生活用品", "服装鞋帽", "运动器材", "其他")
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter
        
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterProducts()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Sort spinner
        val sortOptions = arrayOf("默认排序", "价格从低到高", "价格从高到低", "最新发布", "最多浏览")
        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.sortSpinner.adapter = sortAdapter
        
        binding.sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortProducts(position)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchProducts(query ?: "")
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadProducts()
                } else {
                    searchProducts(newText)
                }
                return true
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadProducts()
        }
        
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary_color,
            R.color.primary_dark_color,
            R.color.accent_color
        )
    }
    
    private fun loadProducts() {
        if (isLoading) {
            return
        }
        isLoading = true
        showLoading()
        lifecycleScope.launch {
            try {
                // 先尝试从云端获取商品列表
                val response = withContext(Dispatchers.IO) {
                    apiService.getProducts(
                        page = 1,
                        limit = 100
                    )
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val cloudProducts = response.body()!!.products
                    
                    // 将云端商品同步到本地数据库
                    withContext(Dispatchers.IO) {
                        // 获取云端商品ID列表
                        val cloudProductIds = cloudProducts.map { it.id }
                        
                        // 删除本地数据库中不在云端商品列表中的商品
                        val localProducts = productDao.getAllProducts()
                        for (localProduct in localProducts) {
                            if (localProduct.id !in cloudProductIds) {
                                productDao.deleteProduct(localProduct.id)
                            }
                        }
                        
                        // 同步云端商品到本地数据库
                        for (cloudProduct in cloudProducts) {
                            val existingProduct = productDao.getProductById(cloudProduct.id)
                            val localProduct = cloudProduct.toLocalProduct(-1L)
                            
                            if (existingProduct == null) {
                                // 如果本地不存在，插入新商品
                                productDao.insertProduct(localProduct)
                            } else {
                                // 如果本地已存在，更新商品信息
                                productDao.updateProduct(localProduct)
                            }
                        }
                    }
                    
                    // 从本地数据库加载所有商品
                    val products = withContext(Dispatchers.IO) {
                        productDao.getAllProducts()
                    }
                    android.util.Log.d("HomeFragment", "从本地数据库加载到 ${products.size} 个商品")
                    currentProducts = products
                    updateProductList(products)
                    
                    // 同步收藏状态和点赞数
                    syncFavoriteStatus(products)
                    
                    // 如果是下拉刷新触发的，显示成功提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "刷新成功，已更新${cloudProducts.size}个商品", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 云端获取失败，从本地数据库加载
                    val products = withContext(Dispatchers.IO) {
                        productDao.getAllProducts()
                    }
                    currentProducts = products
                    updateProductList(products)
                    
                    // 如果是下拉刷新触发的，显示失败提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "从服务器获取失败，显示本地数据", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                // 网络错误，从本地数据库加载
                try {
                    val products = withContext(Dispatchers.IO) {
                        productDao.getAllProducts()
                    }
                    currentProducts = products
                    updateProductList(products)
                    
                    // 如果是下拉刷新触发的，显示网络错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "网络错误，显示本地数据", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    currentProducts = emptyList()
                    updateProductList(emptyList())
                    
                    // 如果是下拉刷新触发的，显示错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: HttpException) {
                // HTTP错误，从本地数据库加载
                try {
                    val products = withContext(Dispatchers.IO) {
                        productDao.getAllProducts()
                    }
                    currentProducts = products
                    updateProductList(products)
                    
                    // 如果是下拉刷新触发的，显示HTTP错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "服务器错误，显示本地数据", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    currentProducts = emptyList()
                    updateProductList(emptyList())
                    
                    // 如果是下拉刷新触发的，显示错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // 其他错误，从本地数据库加载
                try {
                    val products = withContext(Dispatchers.IO) {
                        productDao.getAllProducts()
                    }
                    currentProducts = products
                    updateProductList(products)
                    
                    // 如果是下拉刷新触发的，显示错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "加载失败，显示本地数据", Toast.LENGTH_SHORT).show()
                    }
                } catch (e2: Exception) {
                    currentProducts = emptyList()
                    updateProductList(emptyList())
                    
                    // 如果是下拉刷新触发的，显示错误提示
                    if (binding.swipeRefreshLayout.isRefreshing) {
                        Toast.makeText(requireContext(), "加载失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                isLoading = false
                hideLoading()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun searchProducts(keyword: String) {
        if (keyword.isEmpty()) {
            loadProducts()
            return
        }
        
        showLoading()
        lifecycleScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productDao.searchProducts(keyword)
                }
                currentProducts = products
                updateProductList(products)
            } catch (e: Exception) {
                // Show empty state instead of error
                currentProducts = emptyList()
                updateProductList(emptyList())
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun filterProducts() {
        val selectedCategory = binding.categorySpinner.selectedItemPosition
        if (selectedCategory == 0) { // 全部类别
            loadProducts()
            return
        }
        
        val category = binding.categorySpinner.selectedItem.toString()
        showLoading()
        lifecycleScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productDao.getProductsByCategory(category)
                }
                currentProducts = products
                updateProductList(products)
            } catch (e: Exception) {
                // Show empty state instead of error
                currentProducts = emptyList()
                updateProductList(emptyList())
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun sortProducts(sortType: Int) {
        when (sortType) {
            0 -> loadProducts() // 默认排序
            1 -> sortByPrice(true) // 价格从低到高
            2 -> sortByPrice(false) // 价格从高到低
            3 -> sortByDate() // 最新发布
            4 -> sortByViews() // 最多浏览
        }
    }
    
    private fun sortByPrice(ascending: Boolean) {
        showLoading()
        lifecycleScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productDao.getProductsSortedByPrice(ascending)
                }
                currentProducts = products
                updateProductList(products)
            } catch (e: Exception) {
                // Show empty state instead of error
                currentProducts = emptyList()
                updateProductList(emptyList())
            } finally {
                hideLoading()
            }
        }
    }
    
    private fun sortByDate() {
        val sortedProducts = currentProducts.sortedByDescending { it.createdAt }
        updateProductList(sortedProducts)
    }
    
    private fun sortByViews() {
        val sortedProducts = currentProducts.sortedByDescending { it.viewCount }
        updateProductList(sortedProducts)
    }
    
    private fun updateProductList(products: List<Product>) {
        android.util.Log.d("HomeFragment", "updateProductList - 商品数量: ${products.size}")
        productAdapter.updateProducts(products)
        
        if (products.isEmpty()) {
            android.util.Log.d("HomeFragment", "商品列表为空，显示空提示")
            binding.emptyText.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            android.util.Log.d("HomeFragment", "商品列表不为空，显示RecyclerView")
            binding.emptyText.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun syncFavoriteStatus(products: List<Product>) {
        val userInfo = TokenManager.getUserInfo(requireContext())
        val currentUserId = userInfo?.userId ?: -1L
        
        if (currentUserId == -1L) {
            android.util.Log.d("HomeFragment", "用户未登录，跳过收藏状态同步")
            return
        }
        
        if (products.isEmpty()) {
            return
        }
        
        lifecycleScope.launch {
            try {
                val productIds = products.map { it.id }
                
                val response = withContext(Dispatchers.IO) {
                    try {
                        apiService.getFavoritesStatus(FavoritesStatusRequest(productIds))
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "获取收藏状态失败", e)
                        null
                    }
                }
                
                if (response != null && response.isSuccessful && response.body() != null) {
                    val statusMap = response.body()!!.status
                    
                    withContext(Dispatchers.IO) {
                        for (product in products) {
                            val isFavorited = statusMap[product.id.toString()] ?: false
                            
                            if (isFavorited) {
                                if (!favoriteDao.isProductFavorited(currentUserId, product.id)) {
                                    favoriteDao.addFavorite(currentUserId, product.id, System.currentTimeMillis())
                                }
                            } else {
                                if (favoriteDao.isProductFavorited(currentUserId, product.id)) {
                                    favoriteDao.removeFavorite(currentUserId, product.id)
                                }
                            }
                        }
                    }
                    
                    android.util.Log.d("HomeFragment", "收藏状态同步完成")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "同步收藏状态时发生错误", e)
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.productsRecyclerView.visibility = View.GONE
        binding.emptyText.visibility = View.GONE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        binding.emptyText.visibility = View.VISIBLE
        binding.productsRecyclerView.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        loadProducts()
    }
    
    private fun createSampleProductsIfNeeded() {
        lifecycleScope.launch {
            try {
                val existingProducts = withContext(Dispatchers.IO) {
                    productDao.getAllProducts()
                }
                
                if (existingProducts.isEmpty()) {
                    // Create sample products for testing
                    val sampleProducts = listOf(
                        Product(
                            title = "二手iPhone 12",
                            description = "九成新iPhone 12，64GB，蓝色，功能正常，无维修记录",
                            price = 3500.00,
                            category = "电子产品",
                            condition = "几乎全新",
                            location = "东区宿舍",
                            images = null,
                            sellerId = 1,
                            status = Product.STATUS_ACTIVE,
                            viewCount = 15,
                            likeCount = 3,
                            createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
                            updatedAt = System.currentTimeMillis() - 86400000
                        ),
                        Product(
                            title = "高等数学教材",
                            description = "同济大学版高等数学上下册，第七版，有少量笔记",
                            price = 45.00,
                            category = "书籍资料",
                            condition = "轻微使用痕迹",
                            location = "图书馆",
                            images = null,
                            sellerId = 2,
                            status = Product.STATUS_ACTIVE,
                            viewCount = 8,
                            likeCount = 1,
                            createdAt = System.currentTimeMillis() - 172800000, // 2 days ago
                            updatedAt = System.currentTimeMillis() - 172800000
                        ),
                        Product(
                            title = "宿舍台灯",
                            description = "护眼LED台灯，三档调光，USB充电，几乎全新",
                            price = 25.00,
                            category = "生活用品",
                            condition = "几乎全新",
                            location = "西区宿舍",
                            images = null,
                            sellerId = 3,
                            status = Product.STATUS_ACTIVE,
                            viewCount = 12,
                            likeCount = 2,
                            createdAt = System.currentTimeMillis() - 259200000, // 3 days ago
                            updatedAt = System.currentTimeMillis() - 259200000
                        ),
                        Product(
                            title = "篮球",
                            description = "斯伯丁篮球，7号标准尺寸，室内外通用，八成新",
                            price = 80.00,
                            category = "运动器材",
                            condition = "明显使用痕迹",
                            location = "体育馆",
                            images = null,
                            sellerId = 4,
                            status = Product.STATUS_ACTIVE,
                            viewCount = 20,
                            likeCount = 5,
                            createdAt = System.currentTimeMillis() - 345600000, // 4 days ago
                            updatedAt = System.currentTimeMillis() - 345600000
                        )
                    )
                    
                    for (product in sampleProducts) {
                        withContext(Dispatchers.IO) {
                            productDao.insertProduct(product)
                        }
                    }
                    
                    // Reload products
                    loadProducts()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}