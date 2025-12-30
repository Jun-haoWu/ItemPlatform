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
import com.wujunhao.a202302010306.itemplatform.adapter.ChatMessageAdapter
import com.wujunhao.a202302010306.itemplatform.databinding.FragmentChatMessagesBinding
import com.wujunhao.a202302010306.itemplatform.model.ChatMessage
import com.wujunhao.a202302010306.itemplatform.model.SendMessageRequest
import com.wujunhao.a202302010306.itemplatform.network.ApiClient
import com.wujunhao.a202302010306.itemplatform.utils.TokenManager
import kotlinx.coroutines.launch
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap

class ChatMessagesFragment : Fragment() {
    
    private var _binding: FragmentChatMessagesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ChatMessageAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var currentUserId: Long = -1
    private var otherUserId: Long = -1
    private var otherUsername: String = ""
    private var currentPage = 1
    private var isLoading = false
    private var hasMore = true
    private var isDestroyed = false
    
    // 缓存相关
    private val messageCache = ConcurrentHashMap<Long, MutableList<ChatMessage>>()
    private val cacheTimestamp = ConcurrentHashMap<Long, Long>()
    private val cacheTimeout = 30000L // 缓存有效期30秒
    
    // 请求去重控制
    private var pendingRequest: Boolean = false
    
    // 实时更新相关 - 智能轮询策略
    private val messageUpdateHandler = Handler(Looper.getMainLooper())
    private var pollInterval = 3000L // 初始轮询间隔3秒
    private var consecutiveEmptyPolls = 0 // 连续空轮询次数
    private var lastMessageCount = 0 // 用于检测消息变化
    private val messageUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isDestroyed && isAdded) {
                checkForNewMessages()
                pollInterval = when {
                    consecutiveEmptyPolls > 5 -> 10000L // 10秒
                    consecutiveEmptyPolls > 2 -> 5000L  // 5秒
                    else -> 3000L // 3秒
                }
                messageUpdateHandler.postDelayed(this, pollInterval)
            }
        }
    }
    
    companion object {
        private const val ARG_OTHER_USER_ID = "other_user_id"
        private const val ARG_OTHER_USERNAME = "other_username"
        
        fun newInstance(otherUserId: Long, otherUsername: String): ChatMessagesFragment {
            val fragment = ChatMessagesFragment()
            val args = Bundle().apply {
                putLong(ARG_OTHER_USER_ID, otherUserId)
                putString(ARG_OTHER_USERNAME, otherUsername)
            }
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            otherUserId = it.getLong(ARG_OTHER_USER_ID)
            otherUsername = it.getString(ARG_OTHER_USERNAME, "")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDestroyed = false
        
        Log.d("ChatMessagesFragment", "onViewCreated called with otherUserId=$otherUserId, otherUsername=$otherUsername")
        
        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        
        // 加载消息（优先使用缓存）
        loadMessagesIfNeeded()
        
        // 开始实时更新
        startRealTimeUpdates()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        isDestroyed = true
        stopRealTimeUpdates()
        _binding = null
    }
    
    private fun loadMessagesIfNeeded() {
        val cacheKey = otherUserId
        val cachedMessages = messageCache[cacheKey]
        val cachedTime = cacheTimestamp[cacheKey] ?: 0L
        val currentTime = System.currentTimeMillis()
        
        // 如果有缓存且未过期（30秒内），直接使用缓存
        if (cachedMessages != null && (currentTime - cachedTime) < cacheTimeout) {
            Log.d("ChatMessagesFragment", "Using cached messages for user $otherUserId")
            messages.clear()
            messages.addAll(cachedMessages)
            
            currentUserId = TokenManager.getUserId(requireContext())
            adapter.updateCurrentUserId(currentUserId)
            adapter.notifyDataSetChanged()
            
            if (messages.isNotEmpty()) {
                scrollToBottom()
            }
            showContent()
            Log.d("ChatMessagesFragment", "Loaded ${messages.size} messages from cache")
            return
        }
        
        // 缓存已过期或不存在，重新加载
        loadMessages()
    }
    
    private fun setupToolbar() {
        binding.toolbar.apply {
            title = otherUsername
            setNavigationIcon(android.R.drawable.ic_menu_revert) // 使用系统返回图标
            setNavigationOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ChatMessageAdapter(messages, currentUserId)
        
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // 消息从底部开始显示
            }
            adapter = this@ChatMessagesFragment.adapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    if (!isLoading && hasMore && firstVisibleItemPosition <= 5) {
                        loadMoreMessages()
                    }
                }
            })
        }
    }
    
    private fun setupMessageInput() {
        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
            }
        }
    }
    
    private fun loadMessages() {
        // 请求去重检查
        if (isLoading || pendingRequest) {
            Log.d("ChatMessagesFragment", "Already loading or pending request, skipping duplicate")
            return
        }
        
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                isLoading = true
                pendingRequest = true
                showLoading()
                
                // 获取当前用户ID
                currentUserId = TokenManager.getUserId(requireContext())
                
                val response = ApiClient.createApiService(requireContext()).getChatHistory(otherUserId, currentPage)
                
                if (response.code == 200) {
                    Log.d("ChatMessagesFragment", "Loaded ${response.messages?.size ?: 0} messages from server")
                    
                    // 只有首次加载时才清空列表
                    if (currentPage == 1) {
                        messages.clear()
                    }
                    
                    val newMessages = response.messages ?: emptyList()
                    messages.addAll(newMessages)
                    
                    // 更新缓存
                    messageCache[otherUserId] = messages.toMutableList()
                    cacheTimestamp[otherUserId] = System.currentTimeMillis()
                    
                    adapter.updateCurrentUserId(currentUserId)
                    adapter.notifyDataSetChanged()
                    Log.d("ChatMessagesFragment", "Adapter notified with ${messages.size} messages")
                    
                    hasMore = response.data?.pagination?.page?.let { it < (response.data?.pagination?.totalPages ?: 0) } ?: false
                    lastMessageCount = messages.size
                    
                    if (messages.isNotEmpty()) {
                        Log.d("ChatMessagesFragment", "Scrolling to bottom with ${messages.size} messages")
                        scrollToBottom()
                    }
                    
                    showContent()
                    Log.d("ChatMessagesFragment", "Content shown with ${messages.size} messages")
                } else {
                    when (response.code) {
                        429 -> {
                            Log.d("ChatMessagesFragment", "Rate limited when loading messages")
                            showError("请求过于频繁，请稍后再试")
                        }
                        else -> {
                            showError("加载消息失败，请重试")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("网络错误，请检查网络连接")
            } finally {
                isLoading = false
                pendingRequest = false
            }
        }
    }
    
    private fun loadMoreMessages() {
        if (isLoading || !hasMore || pendingRequest) return
        
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                isLoading = true
                pendingRequest = true
                currentPage++
                
                val response = ApiClient.createApiService(requireContext()).getChatHistory(otherUserId, currentPage)
                
                if (response.code == 200) {
                    val oldSize = messages.size
                    val newMessages = response.messages ?: emptyList()
                    messages.addAll(0, newMessages) // 添加到顶部
                    adapter.notifyItemRangeInserted(0, newMessages.size)
                    
                    hasMore = response.data?.pagination?.page?.let { it < (response.data?.pagination?.totalPages ?: 0) } ?: false
                    
                    // 更新缓存
                    messageCache[otherUserId] = messages.toMutableList()
                    cacheTimestamp[otherUserId] = System.currentTimeMillis()
                    
                    Log.d("ChatMessagesFragment", "Loaded more ${newMessages.size} messages")
                } else {
                    when (response.code) {
                        429 -> {
                            Log.d("ChatMessagesFragment", "Rate limited when loading more messages")
                            currentPage-- // 恢复页码
                        }
                        else -> {
                            currentPage-- // 恢复页码
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentPage-- // 恢复页码
            } finally {
                isLoading = false
                pendingRequest = false
            }
        }
    }
    
    private fun sendMessage(message: String) {
        binding.buttonSend.isEnabled = false
        binding.editTextMessage.text?.clear()
        
        lifecycleScope.launch {
            try {
                val request = SendMessageRequest(otherUserId, message)
                val response = ApiClient.createApiService(requireContext()).sendMessage(request)
                
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // 添加消息到列表
                        messages.add(body.data)
                        adapter.notifyItemInserted(messages.size - 1)
                        scrollToBottom()
                        
                        // 更新缓存
                        messageCache[otherUserId] = messages.toMutableList()
                        cacheTimestamp[otherUserId] = System.currentTimeMillis()
                        
                        // 发送成功后立即检查新消息（可能对方同时发送了消息）
                        consecutiveEmptyPolls = 0
                        pollInterval = 3000L // 重置为快速轮询
                        
                        Log.d("ChatMessagesFragment", "Message sent successfully")
                    }
                } else {
                    when (response.code()) {
                        429 -> {
                            showError("发送过于频繁，请稍后再试")
                        }
                        else -> {
                            showError("发送消息失败，请重试")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError("网络错误，请检查网络连接")
            } finally {
                binding.buttonSend.isEnabled = true
            }
        }
    }
    
    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
        }
    }
    
    private fun startRealTimeUpdates() {
        messageUpdateHandler.post(messageUpdateRunnable)
    }
    
    private fun stopRealTimeUpdates() {
        messageUpdateHandler.removeCallbacks(messageUpdateRunnable)
    }
    
    private fun checkForNewMessages() {
        if (pendingRequest) {
            Log.d("ChatMessagesFragment", "Skipping poll, request already in progress")
            return
        }
        
        lifecycleScope.launch {
            try {
                if (!isAdded || isDestroyed || context == null) {
                    return@launch
                }
                
                val lastMessageTime = messages.lastOrNull()?.createdAt?.time ?: 0
                val response = ApiClient.createApiService(requireContext()).getChatHistory(otherUserId, 1)
                
                if (response.code == 200) {
                    val newMessages = (response.messages ?: emptyList()).filter { message ->
                        message.createdAt.time > lastMessageTime
                    }
                    
                    if (newMessages.isNotEmpty()) {
                        consecutiveEmptyPolls = 0
                        messages.addAll(newMessages)
                        adapter.notifyItemRangeInserted(messages.size - newMessages.size, newMessages.size)
                        messageCache[otherUserId] = messages.toMutableList()
                        cacheTimestamp[otherUserId] = System.currentTimeMillis()
                        
                        val layoutManager = binding.recyclerViewMessages.layoutManager as LinearLayoutManager
                        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                        if (lastVisibleItemPosition >= messages.size - newMessages.size - 1) {
                            scrollToBottom()
                        }
                        
                        Log.d("ChatMessagesFragment", "Received ${newMessages.size} new messages")
                    } else {
                        consecutiveEmptyPolls++
                    }
                    
                    if (response.messages?.size != lastMessageCount && (response.messages?.size ?: 0) > lastMessageCount) {
                        val allNewMessages = (response.messages ?: emptyList()).filter { message ->
                            message.createdAt.time > lastMessageTime
                        }
                        if (allNewMessages.isNotEmpty()) {
                            messages.addAll(allNewMessages)
                            adapter.notifyItemRangeInserted(messages.size - allNewMessages.size, allNewMessages.size)
                            scrollToBottom()
                            messageCache[otherUserId] = messages.toMutableList()
                            cacheTimestamp[otherUserId] = System.currentTimeMillis()
                        }
                    }
                    lastMessageCount = messages.size
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("ChatMessagesFragment", "Exception in checkForNewMessages: ${e.message}")
            }
        }
    }
    
    private fun showLoading() {
        Log.d("ChatMessagesFragment", "Showing loading state")
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewMessages.visibility = View.GONE
        binding.layoutMessageInput.visibility = View.GONE
    }
    
    private fun showContent() {
        Log.d("ChatMessagesFragment", "Showing content state")
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewMessages.visibility = View.VISIBLE
        binding.layoutMessageInput.visibility = View.VISIBLE
    }
    
    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.recyclerViewMessages.visibility = View.GONE
        binding.layoutMessageInput.visibility = View.GONE
        // 这里可以添加错误提示
    }
}