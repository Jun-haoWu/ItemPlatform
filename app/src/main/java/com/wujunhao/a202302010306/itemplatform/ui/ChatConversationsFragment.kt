package com.wujunhao.a202302010306.itemplatform.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.ChatConversationAdapter
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentChatConversationsBinding
import com.wujunhao.a202302010306.itemplatform.model.ChatConversation
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatConversationsFragment : Fragment() {
    
    private var _binding: FragmentChatConversationsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ChatConversationAdapter
    private val conversations = mutableListOf<ChatConversation>()
    private var currentPage = 1
    private var isLoading = false
    private var hasMore = true
    
    // 请求去重控制
    private var isInitialLoadDone = false
    private var isDestroyed = false
    
    // 缓存相关
    private val cacheTimeout = 60000L // 缓存有效期60秒
    private var lastCacheTime = 0L
    private var cachedConversations: MutableList<ChatConversation>? = null
    
    // 实时更新相关
    private val conversationUpdateHandler = Handler(Looper.getMainLooper())
    private val conversationUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && isAdded) {
                checkForNewConversations()
                conversationUpdateHandler.postDelayed(this, 30000) // 每30秒检查一次，避免触发服务器限流
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDestroyed = false
        
        setupRecyclerView()
        setupSwipeRefresh()
        
        // 延迟加载，等用户进入该页面后再请求
        loadConversationsIfNeeded()
        
        // 开始实时更新
        startRealTimeUpdates()
    }
    
    private fun loadConversationsIfNeeded() {
        if (isInitialLoadDone) {
            // 如果已经有缓存且未过期，直接使用缓存
            val currentTime = System.currentTimeMillis()
            if (cachedConversations != null && (currentTime - lastCacheTime) < cacheTimeout) {
                Log.d("ChatConversationsFragment", "Using cached conversations")
                conversations.clear()
                conversations.addAll(cachedConversations!!)
                adapter.notifyDataSetChanged()
                if (conversations.isEmpty()) {
                    showEmptyState()
                } else {
                    showContent()
                }
                return
            }
        }
        
        // 首次加载或缓存已过期
        loadConversations()
    }
    
    private fun setupRecyclerView() {
        adapter = ChatConversationAdapter(conversations) { conversation ->
            // 点击会话时，更新本地列表中的未读计数，然后跳转到聊天界面
            val index = conversations.indexOfFirst { it.id == conversation.id }
            if (index >= 0 && conversations[index].unreadCount > 0) {
                conversations[index] = conversations[index].copy(unreadCount = 0)
                adapter.notifyItemChanged(index)
            }
            
            // 跳转到聊天界面
            val fragment = ChatMessagesFragment.newInstance(
                conversation.otherUserId,
                conversation.otherUsername
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        
        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatConversationsFragment.adapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if (!isLoading && hasMore && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreConversations()
                    }
                }
            })
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            // 下拉刷新时强制重新加载
            refreshConversations()
        }
    }
    
    private fun loadConversations() {
        // 请求去重检查
        if (isLoading) {
            Log.d("ChatConversationsFragment", "Already loading, skipping duplicate request")
            return
        }
        
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                isLoading = true
                showLoading()
                
                val response = ApiClient.createApiService(requireContext()).getChatConversations(currentPage)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // 只有首次加载时才清空列表
                        if (currentPage == 1) {
                            conversations.clear()
                        }
                        
                        val newConversations = body.conversations ?: emptyList()
                        conversations.addAll(newConversations)
                        
                        // 更新缓存
                        cachedConversations = conversations.toMutableList()
                        lastCacheTime = System.currentTimeMillis()
                        isInitialLoadDone = true
                        
                        adapter.notifyDataSetChanged()
                        
                        hasMore = body.pagination?.page?.let { it < (body.pagination?.totalPages ?: 0) } ?: false
                        
                        if (conversations.isEmpty()) {
                            showEmptyState()
                        } else {
                            showContent()
                        }
                        
                        Log.d("ChatConversationsFragment", "Loaded ${newConversations.size} conversations, total: ${conversations.size}")
                    }
                } else {
                    when (response.code()) {
                        429 -> {
                            Log.d("ChatConversationsFragment", "Rate limited, will retry later")
                            showError("请求过于频繁，请稍后再试")
                        }
                        else -> showError("加载失败，请重试")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("网络错误，请检查网络连接")
            } finally {
                isLoading = false
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun loadMoreConversations() {
        if (isLoading || !hasMore) return
        
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                isLoading = true
                currentPage++
                
                val response = ApiClient.createApiService(requireContext()).getChatConversations(currentPage)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val oldSize = conversations.size
                        val newConversations = body.conversations ?: emptyList()
                        conversations.addAll(newConversations)
                        adapter.notifyItemRangeInserted(oldSize, newConversations.size)
                        
                        hasMore = body.pagination?.page?.let { it < (body.pagination?.totalPages ?: 0) } ?: false
                        
                        Log.d("ChatConversationsFragment", "Loaded more ${newConversations.size} conversations")
                    }
                } else {
                    when (response.code()) {
                        429 -> {
                            Log.d("ChatConversationsFragment", "Rate limited in loadMoreConversations, will retry later")
                        }
                        else -> {
                            Log.d("ChatConversationsFragment", "Error in loadMoreConversations: ${response.code()}")
                            currentPage-- // 恢复页码
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentPage-- // 恢复页码
            } finally {
                isLoading = false
            }
        }
    }
    
    private fun refreshConversations() {
        currentPage = 1
        hasMore = true
        isInitialLoadDone = false // 强制重新加载
        cachedConversations = null // 清除缓存
        loadConversations()
    }
    
    private fun startRealTimeUpdates() {
        conversationUpdateHandler.post(conversationUpdateRunnable)
    }
    
    private fun stopRealTimeUpdates() {
        conversationUpdateHandler.removeCallbacks(conversationUpdateRunnable)
    }
    
    private fun checkForNewConversations() {
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                // 获取最新的会话列表
                val response = ApiClient.createApiService(requireContext()).getChatConversations(1)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val newConversations = body.conversations ?: emptyList()
                        
                        if (newConversations.isNotEmpty()) {
                            // 检查是否有新消息或更新的会话
                            val hasUpdates = newConversations.any { newConv ->
                                val existingConv = conversations.find { it.id == newConv.id }
                                existingConv == null || 
                                existingConv.lastMessage != newConv.lastMessage ||
                                existingConv.lastMessageTime != newConv.lastMessageTime ||
                                existingConv.unreadCount != newConv.unreadCount
                            }
                            
                            if (hasUpdates) {
                                // 更新会话列表
                                conversations.clear()
                                conversations.addAll(newConversations)
                                adapter.notifyDataSetChanged()
                                
                                // 更新缓存
                                cachedConversations = conversations.toMutableList()
                                lastCacheTime = System.currentTimeMillis()
                                
                                // 如果有更多页面，重置分页状态
                                hasMore = body.pagination?.page?.let { it < (body.pagination?.totalPages ?: 0) } ?: false
                                
                                Log.d("ChatConversationsFragment", "Updated conversations with new messages")
                            }
                        }
                    }
                } else {
                    when (response.code()) {
                        429 -> {
                            Log.d("ChatConversationsFragment", "Rate limited in checkForNewConversations, will retry later")
                        }
                        else -> {
                            Log.d("ChatConversationsFragment", "Error in checkForNewConversations: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("ChatConversationsFragment", "Exception in checkForNewConversations: ${e.message}")
            }
        }
    }
    
    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewConversations.visibility = View.GONE
        binding.textViewEmpty.visibility = View.GONE
    }
    
    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewConversations.visibility = View.VISIBLE
        binding.textViewEmpty.visibility = View.GONE
    }
    
    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewConversations.visibility = View.GONE
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.textViewEmpty.text = "暂无聊天记录"
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewConversations.visibility = View.GONE
        binding.textViewEmpty.visibility = View.VISIBLE
        binding.textViewEmpty.text = message
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        isDestroyed = true
        stopRealTimeUpdates()
        _binding = null
    }
}