package com.wujunhao.a202302010306.itemplatform.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.wujunhao.a202302010306.itemplatform.adapter.SelectedImagesAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentPublishBinding
import com.wujunhao.a202302010306.itemplatform.model.PublishProductRequest
import com.wujunhao.a202302010306.itemplatform.model.PublishProductResponse
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class PublishFragment : Fragment() {
    
    private var _binding: FragmentPublishBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productDao: ProductDao
    private lateinit var imagesAdapter: SelectedImagesAdapter
    private val selectedImages = mutableListOf<Any>() // Can be Uri or String (file path)
    private var productId: Long = 0 // Will be set after product is created
    private var isEditMode: Boolean = false
    private var editingProduct: Product? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublishBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if we're in edit mode
        val editProductId = arguments?.getLong("edit_product_id", -1L)
        isEditMode = editProductId != null && editProductId != -1L
        
        android.util.Log.d("PublishFragment", "onViewCreated - editProductId: $editProductId, isEditMode: $isEditMode")
        
        // Initialize DAO
        val databaseHelper = DatabaseHelper(requireContext())
        productDao = ProductDao(databaseHelper)
        
        setupSpinners()
        setupImagesRecyclerView()
        setupClickListeners()
        
        // If in edit mode, load the existing product
        if (isEditMode && editProductId != null) {
            loadProductForEdit(editProductId)
        }
    }
    
    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("电子产品", "书籍资料", "生活用品", "服装鞋帽", "运动器材", "其他")
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.spinnerCategory.setAdapter(categoryAdapter)
        binding.spinnerCategory.setText(categories[0], false)
        
        // Condition spinner
        val conditions = arrayOf("全新", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "有瑕疵但仍可使用")
        val conditionAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditions
        )
        binding.spinnerCondition.setAdapter(conditionAdapter)
        binding.spinnerCondition.setText(conditions[0], false)
    }
    
    private fun setupClickListeners() {
        binding.btnPublish.setOnClickListener {
            if (isEditMode) {
                updateProduct()
            } else {
                publishProduct()
            }
        }
        
        binding.btnAddImage.setOnClickListener {
            openImagePicker()
        }
    }
    
    private fun publishProduct() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val category = binding.spinnerCategory.text.toString()
        val condition = binding.spinnerCondition.text.toString()
        val location = binding.etLocation.text.toString().trim()
        
        // Validate input
        if (title.isEmpty()) {
            binding.etTitle.error = "请输入商品标题"
            return
        }
        
        if (description.isEmpty()) {
            binding.etDescription.error = "请输入商品描述"
            return
        }
        
        if (priceText.isEmpty()) {
            binding.etPrice.error = "请输入价格"
            return
        }
        
        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            binding.etPrice.error = "请输入有效的价格"
            return
        }
        
        if (location.isEmpty()) {
            binding.etLocation.error = "请输入交易地点"
            return
        }
        
        // Get current user ID from token
        val userInfo = TokenManager.getUserInfo(requireContext())
        if (userInfo == null) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        android.util.Log.d("PublishFragment", "开始发布商品 - title: $title, price: $price")
        
        val currentTime = System.currentTimeMillis()
        val product = Product(
            title = title,
            description = description,
            price = price,
            category = category,
            condition = condition,
            location = location,
            images = null,
            sellerId = userInfo.userId,
            status = Product.STATUS_ACTIVE,
            viewCount = 0,
            likeCount = 0,
            createdAt = currentTime,
            updatedAt = currentTime
        )
        
        val apiService = ApiClient.createApiService(requireContext())
        val token = TokenManager.getToken(requireContext())
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    productDao.insertProduct(product)
                }
                
                if (result != -1L) {
                    productId = result
                    
                    var imagePaths: String? = null
                    if (selectedImages.isNotEmpty()) {
                        imagePaths = saveProductImages(productId)
                        if (imagePaths != null) {
                            val updatedProduct = product.copy(id = productId, images = imagePaths)
                            withContext(Dispatchers.IO) {
                                productDao.updateProductImages(productId, imagePaths)
                            }
                        }
                    }
                    
                    android.util.Log.d("PublishFragment", "本地保存成功，开始上传到服务器 - productId: $productId")
                    
                    val publishRequest = PublishProductRequest(
                        name = title,
                        description = description,
                        price = price,
                        originalPrice = null,
                        images = if (imagePaths != null) ImageUtils.getProductImagePaths(imagePaths) else null,
                        category = category,
                        location = location
                    )
                    
                    val response: Response<PublishProductResponse> = withContext(Dispatchers.IO) {
                        apiService.postProduct(publishRequest)
                    }
                    
                    if (response.isSuccessful && response.body() != null) {
                        val publishResponse = response.body()!!
                        android.util.Log.d("PublishFragment", "服务器发布成功 - serverProductId: ${publishResponse.productId}")
                        Toast.makeText(context, "商品发布成功！", Toast.LENGTH_SHORT).show()
                        clearForm()
                    } else {
                        android.util.Log.e("PublishFragment", "服务器发布失败 - code: ${response.code()}, message: ${response.message()}")
                        Toast.makeText(context, "商品已保存到本地，但上传服务器失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    android.util.Log.e("PublishFragment", "本地保存失败")
                    Toast.makeText(context, "商品发布失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("PublishFragment", "发布异常", e)
                Toast.makeText(context, "发布失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun clearForm() {
        binding.etTitle.text?.clear()
        binding.etDescription.text?.clear()
        binding.etPrice.text?.clear()
        binding.etLocation.text?.clear()
        
        // Reset spinners to default values
        val categories = arrayOf("电子产品", "书籍资料", "生活用品", "服装鞋帽", "运动器材", "其他")
        binding.spinnerCategory.setText(categories[0], false)
        
        val conditions = arrayOf("全新", "几乎全新", "轻微使用痕迹", "明显使用痕迹", "有瑕疵但仍可使用")
        binding.spinnerCondition.setText(conditions[0], false)
        
        // Clear selected images
        selectedImages.clear()
        imagesAdapter.updateImages(emptyList())
    }
    
    private fun loadProductForEdit(productId: Long) {
        android.util.Log.d("PublishFragment", "loadProductForEdit - 开始加载商品ID: $productId")
        lifecycleScope.launch {
            try {
                val product = withContext(Dispatchers.IO) {
                    productDao.getProductById(productId)
                }
                
                android.util.Log.d("PublishFragment", "loadProductForEdit - 查询结果: ${product?.title ?: "null"}")
                
                if (product != null) {
                    editingProduct = product
                    populateProductData(product)
                } else {
                    // Safely handle fragment state
                    if (isAdded && context != null) {
                        Toast.makeText(requireContext(), "商品不存在", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PublishFragment", "loadProductForEdit - 加载商品失败", e)
                // Safely handle fragment state
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "加载商品失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                }
            }
        }
    }
    
    private fun populateProductData(product: Product) {
        // Populate form fields
        binding.etTitle.setText(product.title)
        binding.etDescription.setText(product.description)
        binding.etPrice.setText(product.price.toString())
        binding.etLocation.setText(product.location)
        binding.spinnerCategory.setText(product.category, false)
        binding.spinnerCondition.setText(product.condition, false)
        
        // Change button text to indicate editing
        binding.btnPublish.text = "更新商品"
        
        // Load existing images
        if (!product.images.isNullOrEmpty()) {
            val imagePaths = ImageUtils.getProductImagePaths(product.images)
            for (imagePath in imagePaths) {
                selectedImages.add(imagePath) // Add as String path
            }
            imagesAdapter.updateImages(selectedImages)
        }
    }
    
    private fun updateProduct() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val category = binding.spinnerCategory.text.toString()
        val condition = binding.spinnerCondition.text.toString()
        val location = binding.etLocation.text.toString().trim()
        
        // Validate input
        if (title.isEmpty()) {
            binding.etTitle.error = "请输入商品标题"
            return
        }
        
        if (description.isEmpty()) {
            binding.etDescription.error = "请输入商品描述"
            return
        }
        
        if (priceText.isEmpty()) {
            binding.etPrice.error = "请输入价格"
            return
        }
        
        val price = try {
            priceText.toDouble()
        } catch (e: NumberFormatException) {
            binding.etPrice.error = "请输入有效的价格"
            return
        }
        
        if (location.isEmpty()) {
            binding.etLocation.error = "请输入交易地点"
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val updatedProduct = editingProduct!!.copy(
            title = title,
            description = description,
            price = price,
            category = category,
            condition = condition,
            location = location,
            updatedAt = currentTime
        )
        
        // Update product
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Update product in database
                    productDao.updateProduct(updatedProduct)
                    
                    // Handle image updates
                    if (selectedImages.isNotEmpty()) {
                        val imagePaths = saveProductImages(updatedProduct.id)
                        if (imagePaths != null) {
                            productDao.updateProductImages(updatedProduct.id, imagePaths)
                        }
                    }
                }
                
                // Safely handle fragment state
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "商品更新成功！", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed()
                }
                
            } catch (e: Exception) {
                // Safely handle fragment state
                if (isAdded && context != null) {
                    Toast.makeText(requireContext(), "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupImagesRecyclerView() {
        imagesAdapter = SelectedImagesAdapter(
            images = selectedImages,
            onImageClick = { position ->
                // Handle image click - could implement image preview
                Toast.makeText(context, "图片 ${position + 1}", Toast.LENGTH_SHORT).show()
            },
            onDeleteClick = { position ->
                // Handle delete click
                selectedImages.removeAt(position)
                imagesAdapter.updateImages(selectedImages)
            }
        )
        
        binding.rvSelectedImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.adapter = imagesAdapter
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "选择图片"), PICK_IMAGES_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.let { intent ->
                // Handle multiple image selection
                val clipData = intent.clipData
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedImages.add(uri)
                    }
                } else {
                    // Handle single image selection
                    intent.data?.let { uri ->
                        selectedImages.add(uri)
                    }
                }
                imagesAdapter.updateImages(selectedImages)
            }
        }
    }
    
    private fun saveProductImages(productId: Long): String? {
        if (selectedImages.isEmpty()) return null
        
        val savedImagePaths = mutableListOf<String>()
        
        for (image in selectedImages) {
            when (image) {
                is Uri -> {
                    val savedPath = ImageUtils.saveImageFromUri(requireContext(), image, productId)
                    savedPath?.let { savedImagePaths.add(it) }
                }
                is String -> {
                    // Already a saved path
                    savedImagePaths.add(image)
                }
            }
        }
        
        return if (savedImagePaths.isNotEmpty()) {
            ImageUtils.convertImagePathsToString(savedImagePaths)
        } else {
            null
        }
    }
    
    companion object {
        private const val PICK_IMAGES_REQUEST = 1001
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}