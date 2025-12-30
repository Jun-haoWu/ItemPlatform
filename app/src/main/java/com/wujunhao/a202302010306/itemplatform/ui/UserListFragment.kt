package com.wujunhao.a202302010306.itemplatform.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.UserListAdapter
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentUserListBinding
import com.wujunhao.a202302010306.itemplatform.model.AdminUser
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.launch

class UserListFragment : Fragment() {
    
    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: UserListAdapter
    private val users = mutableListOf<AdminUser>()
    private var currentPage = 1
    private var isLoading = false
    private var hasMore = true
    private var currentUserId: Long = -1
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        loadUsers()
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            title = "用户列表"
            setNavigationIcon(android.R.drawable.ic_menu_revert) // 使用系统返回图标
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = UserListAdapter(users) { user ->
            // 跳转到聊天界面
            val fragment = ChatMessagesFragment.newInstance(user.id, user.username)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UserListFragment.adapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if (!isLoading && hasMore && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreUsers()
                    }
                }
            })
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshUsers()
        }
    }
    
    private fun loadUsers() {
        if (isLoading) return
        
        lifecycleScope.launch {
            try {
                isLoading = true
                showLoading()
                
                // 获取当前用户信息
                val userInfo = TokenManager.getUserInfo(requireContext())
                currentUserId = userInfo?.userId ?: -1
                
                val response = ApiClient.createApiService(requireContext()).getUsers(currentPage, 20)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val allUsers = body.data?.users ?: emptyList()
                        // 过滤掉当前用户
                        val otherUsers = allUsers.filter { it.id != currentUserId }
                        
                        if (currentPage == 1) {
                            users.clear()
                        }
                        
                        users.addAll(otherUsers)
                        adapter.notifyDataSetChanged()
                        
                        hasMore = body.data?.pagination?.page?.let { it < (body.data?.pagination?.totalPages ?: 0) } ?: false
                        
                        if (users.isEmpty()) {
                            showEmptyState()
                        } else {
                            showContent()
                        }
                    }
                } else {
                    showError("加载用户列表失败")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("网络错误: ${e.message}")
            } finally {
                isLoading = false
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun loadMoreUsers() {
        if (isLoading || !hasMore) return
        
        currentPage++
        loadUsers()
    }
    
    private fun refreshUsers() {
        currentPage = 1
        hasMore = true
        loadUsers()
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewUsers.visibility = View.GONE
        binding.textViewEmpty.visibility = View.GONE
    }
    
    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewUsers.visibility = View.VISIBLE
        binding.textViewEmpty.visibility = View.GONE
    }
    
    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewUsers.visibility = View.GONE
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.textViewEmpty.text = "暂无其他用户"
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewUsers.visibility = View.GONE
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.textViewEmpty.text = message
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}