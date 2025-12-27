package com.wujunhao.a202302010306.itemplatform.network

object CloudConfig {
    // 服务器配置
    const val LOCAL_SERVER_URL = "http://10.0.2.2:8080/api/"
    const val CLOUD_SERVER_URL = "http://223.6.254.237:80/api/"
    
    // 使用模式配置
    const val USE_CLOUD_SERVER = true  // 设置为true使用云服务器，false使用本地服务器
    
    // 获取当前使用的服务器地址
    fun getCurrentBaseUrl(): String {
        return if (USE_CLOUD_SERVER) {
            CLOUD_SERVER_URL
        } else {
            LOCAL_SERVER_URL
        }
    }
    
    // 获取完整的服务器地址（不带/api/后缀）
    fun getServerBaseUrl(): String {
        return getCurrentBaseUrl().removeSuffix("api/")
    }
    
    // 配置信息
    data class ServerInfo(
        val name: String,
        val url: String,
        val description: String
    )
    
    // 获取当前服务器信息
    fun getCurrentServerInfo(): ServerInfo {
        return if (USE_CLOUD_SERVER) {
            ServerInfo(
                name = "阿里云服务器",
                url = CLOUD_SERVER_URL,
                description = "部署在阿里云的正式服务器"
            )
        } else {
            ServerInfo(
                name = "本地服务器",
                url = LOCAL_SERVER_URL,
                description = "本地开发服务器"
            )
        }
    }
    
    // 同步配置
    object SyncConfig {
        const val SYNC_INTERVAL = 5 * 60 * 1000L  // 5分钟同步一次
        const val MAX_RETRY_COUNT = 3
        const val SYNC_TIMEOUT = 30L  // 同步超时时间（秒）
        
        // 同步策略
        const val ENABLE_AUTO_SYNC = true  // 启用自动同步
        const val SYNC_ON_WIFI_ONLY = false  // 仅在WiFi下同步
        const val SYNC_WHEN_CHARGING = false  // 仅在充电时同步
    }
    
    // 网络配置
    object NetworkConfig {
        const val CONNECT_TIMEOUT = 30L  // 连接超时（秒）
        const val READ_TIMEOUT = 30L     // 读取超时（秒）
        const val WRITE_TIMEOUT = 30L    // 写入超时（秒）
        const val MAX_RETRIES = 3        // 最大重试次数
    }
}