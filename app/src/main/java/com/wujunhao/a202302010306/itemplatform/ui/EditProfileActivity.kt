package com.wujunhao.a202302010306.itemplatform.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.LocalUser
import com.wujunhao.a202302010306.itemplatform.database.UserDao
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityEditProfileBinding
import com.wujunhao.a202302010306.itemplatform.model.User
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var userDao: UserDao
    private var currentUser: LocalUser? = null
    private var selectedAvatarUri: Uri? = null
    private val PICK_AVATAR_REQUEST = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize
        setupToolbar()
        setupClickListeners()
        
        // Initialize DAO
        val databaseHelper = DatabaseHelper(this)
        userDao = UserDao(databaseHelper)
        
        // Load current user data
        loadUserData()
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
        binding.tvChangeAvatar.setOnClickListener {
            openAvatarPicker()
        }
        
        binding.ivAvatar.setOnClickListener {
            openAvatarPicker()
        }
        
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun loadUserData() {
        val userInfo = TokenManager.getUserInfo(this)
        if (userInfo == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                currentUser = withContext(Dispatchers.IO) {
                    userDao.getUserById(userInfo.userId)
                }
                
                currentUser?.let { user ->
                    // Fill form with current data
                    binding.etUsername.setText(user.username)
                    binding.etEmail.setText(user.email)
                    binding.etPhone.setText(user.phone)
                    binding.etStudentId.setText(user.studentId)
                    binding.etDepartment.setText(user.department)
                    
                    // Load avatar if exists
                    if (!user.avatar.isNullOrEmpty()) {
                        val bitmap = ImageUtils.loadImage(this@EditProfileActivity, user.avatar)
                        if (bitmap != null) {
                            binding.ivAvatar.setImageBitmap(bitmap)
                        }
                    }
                }
                
                showLoading(false)
                
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "加载用户信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openAvatarPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "选择头像"), PICK_AVATAR_REQUEST)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_AVATAR_REQUEST && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedAvatarUri = uri
                binding.ivAvatar.setImageURI(uri)
            }
        }
    }
    
    private fun saveProfile() {
        val userInfo = TokenManager.getUserInfo(this)
        if (userInfo == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show()
            return
        }
        
        val username = binding.etUsername.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val department = binding.etDepartment.text.toString().trim()
        
        // Validation
        if (username.isEmpty()) {
            binding.etUsername.error = "用户名不能为空"
            return
        }
        
        if (phone.isEmpty()) {
            binding.etPhone.error = "电话不能为空"
            return
        }
        
        if (department.isEmpty()) {
            binding.etDepartment.error = "院系不能为空"
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                var avatarPath: String? = currentUser?.avatar
                
                // Save new avatar if selected
                if (selectedAvatarUri != null) {
                    avatarPath = ImageUtils.saveImageFromUri(
                        this@EditProfileActivity,
                        selectedAvatarUri!!,
                        userInfo.userId
                    )
                }
                
                // Update user data
                val updatedLocalUser = currentUser!!.copy(
                    username = username,
                    phone = phone,
                    department = department,
                    updatedAt = System.currentTimeMillis()
                )
                
                withContext(Dispatchers.IO) {
                    // Convert LocalUser to User for the DAO update method
                    val userForUpdate = User(
                        id = updatedLocalUser.id,
                        username = updatedLocalUser.username,
                        email = updatedLocalUser.email,
                        phone = updatedLocalUser.phone,
                        realName = updatedLocalUser.realName,
                        studentId = updatedLocalUser.studentId,
                        department = updatedLocalUser.department,
                        avatar = avatarPath,
                        createdAt = updatedLocalUser.createdAt.toString(),
                        updatedAt = updatedLocalUser.updatedAt.toString()
                    )
                    userDao.updateUser(userForUpdate)
                }
                
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "资料已更新", Toast.LENGTH_SHORT).show()
                finish() // Return to profile
                
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@EditProfileActivity, "更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
    }
}