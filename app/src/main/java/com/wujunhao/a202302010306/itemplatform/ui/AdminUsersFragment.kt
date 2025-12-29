package com.wujunhao.a202302010306.itemplatform.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.AdminUserAdapter
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentAdminUsersBinding
import com.wujunhao.a202302010306.itemplatform.model.AdminUser
import com.wujunhao.a202302010306.itemplatform.model.AdminPagination
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class AdminUsersFragment : Fragment() {
    
    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adminUserAdapter: AdminUserAdapter
    private var currentPage = 1
    private var totalPages = 1
    private var isLoading = false
    private var currentSearchQuery = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        checkAdminPermission()
        setupRecyclerView()
        setupSearch()
        loadUsers()
    }
    
    private fun checkAdminPermission() {
        val username = TokenManager.getUsername(requireContext())
        android.util.Log.d("AdminUsersFragment", "checkAdminPermission: username=$username")
        
        if (username != "admin") {
            android.util.Log.d("AdminUsersFragment", "非admin用户，返回上一页")
            Toast.makeText(context, "需要管理员权限", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed()
        } else {
            android.util.Log.d("AdminUsersFragment", "admin用户验证通过")
        }
    }
    
    private fun setupRecyclerView() {
        adminUserAdapter = AdminUserAdapter(emptyList())
        
        val layoutManager = LinearLayoutManager(requireContext())
        binding.usersRecyclerView.layoutManager = layoutManager
        binding.usersRecyclerView.adapter = adminUserAdapter
        
        binding.usersRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                if (!isLoading && currentPage < totalPages) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5) {
                        loadUsers()
                    }
                }
            }
        })
    }
    
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query ?: ""
                currentPage = 1
                loadUsers()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }
    
    private fun loadUsers() {
        if (isLoading) return
        
        isLoading = true
        showLoading()
        
        lifecycleScope.launch {
            try {
                val apiService = ApiClient.createApiService(requireContext())
                val response = withContext(Dispatchers.IO) {
                    apiService.getAdminUsers(
                        page = currentPage,
                        limit = 20,
                        search = if (currentSearchQuery.isEmpty()) null else currentSearchQuery
                    )
                }
                
                android.util.Log.d("AdminUsersFragment", "Response code: ${response.code()}")
                android.util.Log.d("AdminUsersFragment", "Response body: ${response.body()}")
                
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    android.util.Log.d("AdminUsersFragment", "Response code: ${body.code}, message: ${body.message}")
                    android.util.Log.d("AdminUsersFragment", "Data: ${body.data}")
                    
                    if (body.code == 200 && body.data != null) {
                        val newUsers = body.data.users
                        val pagination = body.data.pagination
                        
                        android.util.Log.d("AdminUsersFragment", "Users count: ${newUsers.size}")
                        android.util.Log.d("AdminUsersFragment", "Pagination: page=${pagination.page}, total=${pagination.total}")
                        
                        if (currentPage == 1) {
                            adminUserAdapter.updateUsers(newUsers)
                        } else {
                            adminUserAdapter.addUsers(newUsers)
                        }
                        
                        currentPage = pagination.page
                        totalPages = pagination.totalPages
                        
                        updatePaginationInfo(pagination)
                    } else {
                        android.util.Log.e("AdminUsersFragment", "Error: ${body.message}")
                        showError(body.message)
                    }
                } else {
                    android.util.Log.e("AdminUsersFragment", "Response not successful: ${response.code()}")
                    showError("获取用户列表失败")
                }
            } catch (e: IOException) {
                android.util.Log.e("AdminUsersFragment", "IOException: ${e.message}", e)
                showError("网络错误，请检查网络连接")
            } catch (e: HttpException) {
                android.util.Log.e("AdminUsersFragment", "HttpException: ${e.code()}", e)
                showError("服务器错误: ${e.code()}")
            } catch (e: Exception) {
                android.util.Log.e("AdminUsersFragment", "Exception: ${e.message}", e)
                showError("发生错误: ${e.message}")
            } finally {
                isLoading = false
                hideLoading()
            }
        }
    }
    
    private fun updatePaginationInfo(pagination: AdminPagination) {
        binding.paginationInfo.text = "第 ${pagination.page} / ${pagination.totalPages} 页，共 ${pagination.total} 个用户"
        
        binding.prevButton.isEnabled = pagination.hasPrev
        binding.nextButton.isEnabled = pagination.hasNext
        
        binding.prevButton.setOnClickListener {
            if (pagination.hasPrev) {
                currentPage--
                loadUsers()
            }
        }
        
        binding.nextButton.setOnClickListener {
            if (pagination.hasNext) {
                currentPage++
                loadUsers()
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.usersRecyclerView.visibility = View.GONE
        binding.emptyText.visibility = View.GONE
    }
    
    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        
        if (adminUserAdapter.itemCount > 0) {
            binding.usersRecyclerView.visibility = View.VISIBLE
            binding.emptyText.visibility = View.GONE
        } else {
            binding.usersRecyclerView.visibility = View.GONE
            binding.emptyText.visibility = View.VISIBLE
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        binding.emptyText.text = message
        binding.emptyText.visibility = View.VISIBLE
        binding.usersRecyclerView.visibility = View.GONE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}