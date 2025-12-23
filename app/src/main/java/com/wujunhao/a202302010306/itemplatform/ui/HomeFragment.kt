package com.wujunhao.a202302010306.itemplatform.ui

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
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.ProductAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentHomeBinding
import com.wujunhao.a202302010306.itemplatform.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productDao: ProductDao
    private lateinit var productAdapter: ProductAdapter
    private var currentProducts: List<Product> = emptyList()
    
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
        databaseHelper.ensureProductsTableExists() // Ensure products table exists
        productDao = ProductDao(databaseHelper)
        
        // Setup RecyclerView with StaggeredGridLayoutManager for waterfall effect
        setupRecyclerView()
        
        // Setup spinners
        setupSpinners()
        
        // Setup search
        setupSearch()
        
        // Load products
        loadProducts()
        
        // Create sample products for testing if no products exist
        createSampleProductsIfNeeded()
    }
    
    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->
            // Handle product click
            Toast.makeText(context, "点击了: ${product.title}", Toast.LENGTH_SHORT).show()
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
    
    private fun loadProducts() {
        showLoading()
        lifecycleScope.launch {
            try {
                val products = withContext(Dispatchers.IO) {
                    productDao.getAllProducts()
                }
                currentProducts = products
                updateProductList(products)
            } catch (e: Exception) {
                // Show empty state instead of error for missing table
                currentProducts = emptyList()
                updateProductList(emptyList())
            } finally {
                hideLoading()
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
        productAdapter.updateProducts(products)
        
        if (products.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyText.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
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