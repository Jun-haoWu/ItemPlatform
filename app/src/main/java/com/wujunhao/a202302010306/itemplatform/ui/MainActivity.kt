package com.wujunhao.a202302010306.itemplatform.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.databinding.ActivityMainBinding
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        // Check if we need to navigate to publish fragment for editing
        val navigateToPublish = intent.getBooleanExtra("navigate_to_publish", false)
        val editProductId = intent.getLongExtra("edit_product_id", -1L)
        
        android.util.Log.d("MainActivity", "接收到的参数: navigateToPublish=$navigateToPublish, editProductId=$editProductId")
        
        if (navigateToPublish && editProductId != -1L) {
            android.util.Log.d("MainActivity", "进入编辑模式，商品ID: $editProductId")
            // Navigate to publish fragment in edit mode
            val publishFragment = PublishFragment().apply {
                arguments = Bundle().apply {
                    putLong("edit_product_id", editProductId)
                }
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, publishFragment)
                .commit()
            // Select the publish tab
            binding.bottomNavigation.selectedItemId = R.id.nav_publish
        } else if (savedInstanceState == null) {
            // Load default fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_publish -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PublishFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onBackPressed() {
        val backStackEntryCount = supportFragmentManager.backStackEntryCount
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        android.util.Log.d("MainActivity", "onBackPressed: backStackEntryCount=$backStackEntryCount")
        android.util.Log.d("MainActivity", "当前Fragment: ${currentFragment?.javaClass?.simpleName}")
        android.util.Log.d("MainActivity", "当前Fragment实例: $currentFragment")
        
        if (backStackEntryCount > 0) {
            android.util.Log.d("MainActivity", "弹出返回栈")
            supportFragmentManager.popBackStack()
        } else {
            android.util.Log.d("MainActivity", "返回栈为空，检查当前Fragment")
            
            if (currentFragment is AdminUsersFragment) {
                android.util.Log.d("MainActivity", "在AdminUsersFragment，导航到ProfileFragment")
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .addToBackStack(null)
                    .commit()
            } else if (currentFragment !is HomeFragment && currentFragment !is ProfileFragment) {
                android.util.Log.d("MainActivity", "导航到首页")
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else if (currentFragment is ProfileFragment) {
                android.util.Log.d("MainActivity", "在ProfileFragment，导航到首页")
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else {
                android.util.Log.d("MainActivity", "退出应用")
                super.onBackPressed()
            }
        }
    }
}