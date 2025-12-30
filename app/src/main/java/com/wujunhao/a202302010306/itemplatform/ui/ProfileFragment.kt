package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.database.UserDao
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentProfileBinding
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userDao: UserDao
    private lateinit var productDao: ProductDao
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize DAOs
        val databaseHelper = DatabaseHelper(requireContext())
        userDao = UserDao(databaseHelper)
        productDao = ProductDao(databaseHelper)
        
        // Load user info
        loadUserInfo()
        
        // Setup click listeners
        setupClickListeners()
    }
    
    private fun loadUserInfo() {
        val userInfo = TokenManager.getUserInfo(requireContext())
        if (userInfo == null) {
            Toast.makeText(context, "用户未登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Display basic info
        binding.tvUsername.text = userInfo.username
        
        // Show admin button only for admin user
        if (userInfo.username == "admin") {
            binding.btnAdminUsers.visibility = View.VISIBLE
        } else {
            binding.btnAdminUsers.visibility = View.GONE
        }
        
        // Load detailed user info
        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    userDao.getUserById(userInfo.userId)
                }
                
                if (user != null) {
                    binding.tvEmail.text = user.email
                    binding.tvPhone.text = user.phone
                    binding.tvStudentId.text = user.studentId
                    binding.tvDepartment.text = user.department
                }
                
                // Load user statistics
                loadUserStatistics(userInfo.userId)
            } catch (e: Exception) {
                Toast.makeText(context, "加载用户信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadUserStatistics(userId: Long) {
        lifecycleScope.launch {
            try {
                val userProducts = withContext(Dispatchers.IO) {
                    productDao.getProductsBySeller(userId)
                }
                
                val publishedCount = userProducts.size
                val soldCount = userProducts.count { it.status == Product.STATUS_SOLD }
                
                binding.tvPublishedCount.text = publishedCount.toString()
                binding.tvSoldCount.text = soldCount.toString()
            } catch (e: Exception) {
                // Use default values if loading fails
                binding.tvPublishedCount.text = "0"
                binding.tvSoldCount.text = "0"
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnMyProducts.setOnClickListener {
            // Navigate to user's products
            val intent = Intent(requireContext(), MyProductsActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnEditProfile.setOnClickListener {
            // Navigate to edit profile
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
        
        binding.btnUserList.setOnClickListener {
            // Navigate to user list for starting chats
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserListFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnAdminUsers.setOnClickListener {
            // Navigate to admin users list
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminUsersFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        // Clear token
        TokenManager.clearToken(requireContext())
        
        // Clear SQLite authentication data
        val databaseHelper = DatabaseHelper(requireContext())
        val authDao = com.wujunhao.a202302010306.itemplatform.database.AuthDao(databaseHelper)
        authDao.clearAllTokens()
        
        // Navigate to login activity
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}