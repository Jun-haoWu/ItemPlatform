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
        
        // Load default fragment
        if (savedInstanceState == null) {
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
        // Handle back navigation
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        
        // If we're not on the home fragment, go back to home
        if (currentFragment !is HomeFragment) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        } else {
            // If we're on home fragment, exit the app
            super.onBackPressed()
        }
    }
}