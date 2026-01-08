package com.wujunhao.a202302010306.itemplatform.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.MyLocationStyle
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.geocoder.GeocodeAddress
import com.amap.api.services.geocoder.GeocodeQuery
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.adapter.ProductListAdapter
import com.wujunhao.a202302010306.itemplatform.database.DatabaseHelper
import com.wujunhao.a202302010306.itemplatform.database.ProductDao
import com.wujunhao.a202302010306.itemplatform.model.Product
import android.app.AlertDialog
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MapActivity : AppCompatActivity(), AMap.OnMarkerClickListener {

    private lateinit var mapView: MapView
    private lateinit var aMap: AMap
    private lateinit var fabLocation: FloatingActionButton
    private lateinit var productDao: ProductDao
    
    private lateinit var locationClient: AMapLocationClient
    private var userLocation: LatLng? = null
    private var userCity: String? = null
    private val markers = mutableListOf<Marker>()
    private val nearbyProducts = mutableListOf<Product>()
    private var hasLoadedNearbyProducts = false
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startLocation()
        } else {
            Toast.makeText(this, "需要位置权限才能显示地图", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            AMapLocationClient.updatePrivacyShow(this, true, true)
            AMapLocationClient.updatePrivacyAgree(this, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        setContentView(R.layout.activity_map)
        
        mapView = findViewById(R.id.map_view)
        fabLocation = findViewById(R.id.fab_location)
        
        mapView.onCreate(savedInstanceState)
        
        val databaseHelper = DatabaseHelper(this)
        productDao = ProductDao(databaseHelper)
        
        initMap()
        setupLocation()
        setupFab()
        
        checkLocationPermission()
        
        val productId = intent.getLongExtra("product_id", -1L)
        val productName = intent.getStringExtra("product_name")
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        
        if (productId != -1L && latitude != 0.0 && longitude != 0.0) {
            addProductMarker(productName ?: "未知商品", latitude, longitude, productId)
            val latLng = LatLng(latitude, longitude)
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }

    private fun initMap() {
        aMap = mapView.map
        
        val myLocationStyle = MyLocationStyle()
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_foreground))
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0))
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0))
        myLocationStyle.interval(2000)
        myLocationStyle.showMyLocation(true)
        aMap.myLocationStyle = myLocationStyle
        aMap.isMyLocationEnabled = true
        
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.uiSettings.isCompassEnabled = true
        aMap.uiSettings.isMyLocationButtonEnabled = false
        
        aMap.setOnMarkerClickListener(this)
        
        val defaultLocation = LatLng(39.9042, 116.4074)
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
    }

    private fun setupLocation() {
        locationClient = AMapLocationClient(applicationContext)
        val locationOption = AMapLocationClientOption()
        locationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        locationOption.isOnceLocation = true
        locationOption.isNeedAddress = true
        locationClient.setLocationOption(locationOption)
        
        locationClient.setLocationListener { location ->
            if (location != null) {
                if (location.errorCode == 0) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    userCity = location.district ?: location.city ?: "杭州"
                    aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                    
                    android.util.Log.d("MapActivity", "获取当前位置成功: 纬度=${location.latitude}, 经度=${location.longitude}, 城市=${userCity}, 地址=${location.address}")
                    
                    Toast.makeText(this, "定位成功: ${location.address}", Toast.LENGTH_SHORT).show()
                    
                    if (!hasLoadedNearbyProducts) {
                        hasLoadedNearbyProducts = true
                        loadNearbyProducts()
                    }
                } else {
                    android.util.Log.e("MapActivity", "定位失败: errorCode=${location.errorCode}, errorInfo=${location.errorInfo}")
                    Toast.makeText(this, "定位失败: ${location.errorInfo}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupFab() {
        fabLocation.setOnClickListener {
            userLocation?.let { location ->
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } ?: run {
                Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show()
                startLocation()
            }
        }
    }

    private fun checkLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val needRequest = permissions.any { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needRequest) {
            locationPermissionLauncher.launch(permissions)
        } else {
            startLocation()
        }
    }
    
    private fun startLocation() {
        locationClient.startLocation()
    }
    
    private fun loadNearbyProducts() {
        try {
            android.util.Log.d("MapActivity", "开始加载附近商品")
            val allProducts = productDao.getAllProducts()
            android.util.Log.d("MapActivity", "总商品数: ${allProducts.size}")
            userLocation?.let { current ->
                android.util.Log.d("MapActivity", "用户当前位置: ${current.latitude}, ${current.longitude}")
                nearbyProducts.clear()
                clearMarkers()
                
                var pendingSearchCount = 0
                val handler = Handler(Looper.getMainLooper())
                var delay = 0L
                
                allProducts.forEach { product ->
                    android.util.Log.d("MapActivity", "检查商品: ${product.title}, 地点: ${product.location}, 经纬度: ${product.latitude}, ${product.longitude}")
                    if (product.hasLocation()) {
                        val distance = calculateDistance(
                            current.latitude,
                            current.longitude,
                            product.latitude!!,
                            product.longitude!!
                        )
                        
                        android.util.Log.d("MapActivity", "商品 ${product.title} 距离: $distance 公里")
                        
                        if (distance <= 1.2) {
                            nearbyProducts.add(product)
                            android.util.Log.d("MapActivity", "添加商品到附近列表: ${product.title}")
                        }
                    } else if (product.location.isNotEmpty()) {
                        android.util.Log.d("MapActivity", "商品 ${product.title} 需要搜索地点: ${product.location}")
                        pendingSearchCount++
                        
                        handler.postDelayed({
                            android.util.Log.d("MapActivity", "开始搜索商品 ${product.title} 的地点: ${product.location}")
                            searchProductLocation(product, current) {
                                pendingSearchCount--
                                android.util.Log.d("MapActivity", "搜索完成，剩余: $pendingSearchCount")
                                if (pendingSearchCount == 0) {
                                    android.util.Log.d("MapActivity", "所有搜索完成，开始显示标记")
                                    displayMarkers()
                                }
                            }
                        }, delay)
                        
                        delay += 300L
                    } else {
                        android.util.Log.d("MapActivity", "商品 ${product.title} 没有地点信息")
                    }
                }
                
                android.util.Log.d("MapActivity", "待搜索数量: $pendingSearchCount")
                if (pendingSearchCount == 0) {
                    android.util.Log.d("MapActivity", "没有需要搜索的商品，直接显示标记")
                    displayMarkers()
                }
            } ?: run {
                android.util.Log.e("MapActivity", "用户位置为空，无法加载附近商品")
                Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "加载商品失败", e)
            e.printStackTrace()
            Toast.makeText(this, "加载商品失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayMarkers() {
        if (nearbyProducts.isNotEmpty()) {
            val uniqueLocations = mutableSetOf<String>()
            
            nearbyProducts.forEach { product ->
                val locationKey = "${product.latitude}_${product.longitude}"
                if (!uniqueLocations.contains(locationKey)) {
                    addProductMarker(
                        product.title,
                        product.latitude!!,
                        product.longitude!!,
                        product.id
                    )
                    uniqueLocations.add(locationKey)
                }
            }
            
            Toast.makeText(
                this,
                "找到 ${nearbyProducts.size} 个附近商品（1.2公里范围内），${uniqueLocations.size} 个位置",
                Toast.LENGTH_SHORT
            ).show()
            
            adjustMapBounds()
        } else {
            Toast.makeText(this, "1.2公里范围内没有找到商品", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun searchProductLocation(product: Product, userLocation: LatLng, onComplete: () -> Unit) {
        try {
            val searchKeywords = generateSearchKeywords(product.location)
            android.util.Log.d("MapActivity", "生成搜索关键词: ${searchKeywords.joinToString(", ")}")
            
            searchWithKeywords(searchKeywords, 0, product, userLocation, onComplete)
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "POI搜索异常", e)
            e.printStackTrace()
            onComplete()
        }
    }
    
    private fun extractBuildingIdentifier(location: String): String? {
        val normalizedLocation = location.trim()
        
        when {
            normalizedLocation.matches(Regex("C\\d+号?")) || normalizedLocation.matches(Regex("C\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                return "C$number"
            }
            normalizedLocation.contains("活动中心") -> {
                return "活动中心"
            }
            normalizedLocation.matches(Regex("\\d+号学院楼")) || normalizedLocation.matches(Regex("学院楼\\d+号")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                return "${number}号学院楼"
            }
            normalizedLocation.matches(Regex("A\\d+号?")) || normalizedLocation.matches(Regex("A\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                return "A$number"
            }
            normalizedLocation.matches(Regex("B\\d+号?")) || normalizedLocation.matches(Regex("B\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                return "B$number"
            }
            normalizedLocation.matches(Regex("\\d+号教学楼")) || normalizedLocation.matches(Regex("教学楼\\d+号")) || 
            normalizedLocation.matches(Regex("教\\d+")) || normalizedLocation.matches(Regex("教\\d+号")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                return "教$number"
            }
            else -> {
                return null
            }
        }
    }
    
    private fun generateSearchKeywords(location: String): List<String> {
        val keywords = mutableListOf<String>()
        val normalizedLocation = location.trim()
        
        when {
            normalizedLocation.matches(Regex("C\\d+号?")) || normalizedLocation.matches(Regex("C\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                keywords.add("浙江农林大学东湖校区C${number}号楼")
                keywords.add("浙江农林大学C${number}号楼")
                keywords.add("浙江农林大学东湖校区C${number}号")
                keywords.add("浙江农林大学C${number}号")
                keywords.add("C${number}号楼")
                keywords.add("C${number}号")
                keywords.add("C${number}")
            }
            normalizedLocation.contains("活动中心") -> {
                keywords.add("浙江农林大学东湖校区活动中心")
                keywords.add("浙江农林大学活动中心")
                keywords.add("活动中心")
            }
            normalizedLocation.matches(Regex("\\d+号学院楼")) || normalizedLocation.matches(Regex("学院楼\\d+号")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                keywords.add("浙江农林大学东湖校区${number}号学院楼")
                keywords.add("浙江农林大学${number}号学院楼")
                keywords.add("${number}号学院楼")
                keywords.add("学院楼${number}号")
                keywords.add("学院楼${number}")
            }
            normalizedLocation.matches(Regex("A\\d+号?")) || normalizedLocation.matches(Regex("A\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                keywords.add("浙江农林大学东湖校区A${number}号楼")
                keywords.add("浙江农林大学A${number}号楼")
                keywords.add("浙江农林大学东湖校区A${number}号")
                keywords.add("浙江农林大学A${number}号")
                keywords.add("A${number}号楼")
                keywords.add("A${number}号")
                keywords.add("A${number}")
            }
            normalizedLocation.matches(Regex("B\\d+号?")) || normalizedLocation.matches(Regex("B\\d+")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                keywords.add("浙江农林大学东湖校区B${number}号楼")
                keywords.add("浙江农林大学B${number}号楼")
                keywords.add("浙江农林大学东湖校区B${number}号")
                keywords.add("浙江农林大学B${number}号")
                keywords.add("B${number}号楼")
                keywords.add("B${number}号")
                keywords.add("B${number}")
            }
            normalizedLocation.matches(Regex("\\d+号教学楼")) || normalizedLocation.matches(Regex("教学楼\\d+号")) || 
            normalizedLocation.matches(Regex("教\\d+")) || normalizedLocation.matches(Regex("教\\d+号")) -> {
                val number = normalizedLocation.replace(Regex("[^\\d]"), "")
                keywords.add("浙江农林大学东湖校区${number}号教学楼")
                keywords.add("浙江农林大学${number}号教学楼")
                keywords.add("${number}号教学楼")
                keywords.add("教${number}楼")
                keywords.add("教${number}")
                keywords.add("教${number}号")
            }
            else -> {
                keywords.add(normalizedLocation)
            }
        }
        
        return keywords
    }
    
    private fun searchWithKeywords(keywords: List<String>, index: Int, product: Product, userLocation: LatLng, onComplete: () -> Unit) {
        if (index >= keywords.size) {
            android.util.Log.d("MapActivity", "所有关键词搜索完成，未找到匹配POI")
            onComplete()
            return
        }
        
        val keyword = keywords[index]
        try {
            val poiSearch = PoiSearch(this, null)
            val query = PoiSearch.Query(keyword, "", "杭州")
            query.pageSize = 50
            query.pageNum = 0
            poiSearch.query = query
            
            android.util.Log.d("MapActivity", "POI搜索查询[$index]: $keyword, 城市: 杭州")
            
            poiSearch.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, errorCode: Int) {
                    android.util.Log.d("MapActivity", "POI搜索结果[$index]: errorCode=$errorCode, 结果数量: ${result?.pois?.size}")
                    
                    if (errorCode == 1000 && result != null && result.pois != null && result.pois.isNotEmpty()) {
                        var foundMatch = false
                        var closestDistance = Double.MAX_VALUE
                        var closestPoi: PoiItem? = null
                        
                        val buildingIdentifier = extractBuildingIdentifier(product.location)
                        android.util.Log.d("MapActivity", "提取建筑标识: $buildingIdentifier")
                        
                        for (poi in result.pois) {
                            android.util.Log.d("MapActivity", "POI: ${poi.title}, 地址: ${poi.snippet}, 经纬度: ${poi.latLonPoint?.latitude}, ${poi.latLonPoint?.longitude}")
                            
                            if (poi.latLonPoint != null) {
                                val latLng = LatLng(poi.latLonPoint.latitude, poi.latLonPoint.longitude)
                                
                                val distance = calculateDistance(
                                    userLocation.latitude,
                                    userLocation.longitude,
                                    latLng.latitude,
                                    latLng.longitude
                                )
                                
                                android.util.Log.d("MapActivity", "POI距离: $distance 公里, 是否在范围内: ${distance <= 1.2}")
                                
                                if (distance < closestDistance) {
                                    closestDistance = distance
                                    closestPoi = poi
                                }
                                
                                if (distance <= 1.2) {
                                    val isInSchool = poi.title.contains("浙江农林大学") || poi.title.contains("浙江农林")
                                    
                                    if (buildingIdentifier != null && !isInSchool && !poi.title.contains(buildingIdentifier)) {
                                        android.util.Log.d("MapActivity", "跳过POI: ${poi.title}, 不在学校内且不包含建筑标识: $buildingIdentifier")
                                        continue
                                    }
                                    
                                    val updatedProduct = product.copy(
                                        latitude = latLng.latitude,
                                        longitude = latLng.longitude
                                    )
                                    nearbyProducts.add(updatedProduct)
                                    foundMatch = true
                                    
                                    android.util.Log.d("MapActivity", "添加POI商品到列表: ${product.title}, 关键词: $keyword, POI: ${poi.title}")
                                    
                                    Toast.makeText(
                                        this@MapActivity,
                                        "找到商品位置: ${poi.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    break
                                }
                            }
                        }
                        
                        if (foundMatch) {
                            onComplete()
                        } else {
                            android.util.Log.d("MapActivity", "关键词[$index]未找到匹配POI, 最近距离: $closestDistance 公里, 最近POI: ${closestPoi?.title}")
                            
                            if (closestDistance < 5.0 && closestPoi != null) {
                                val isInSchool = closestPoi.title.contains("浙江农林大学") || closestPoi.title.contains("浙江农林")
                                
                                if (buildingIdentifier != null && !isInSchool && !closestPoi.title.contains(buildingIdentifier)) {
                                    android.util.Log.d("MapActivity", "最近POI不包含建筑标识: ${closestPoi.title}, 标识: $buildingIdentifier, 继续搜索下一个关键词")
                                    searchWithKeywords(keywords, index + 1, product, userLocation, onComplete)
                                } else {
                                    android.util.Log.d("MapActivity", "使用最近POI: ${closestPoi.title}, 距离: $closestDistance 公里")
                                    val updatedProduct = product.copy(
                                        latitude = closestPoi.latLonPoint.latitude,
                                        longitude = closestPoi.latLonPoint.longitude
                                    )
                                    nearbyProducts.add(updatedProduct)
                                    Toast.makeText(
                                        this@MapActivity,
                                        "找到商品位置: ${closestPoi.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onComplete()
                                }
                            } else {
                                searchWithKeywords(keywords, index + 1, product, userLocation, onComplete)
                            }
                        }
                    } else {
                        android.util.Log.d("MapActivity", "关键词[$index]搜索失败: errorCode=$errorCode")
                        searchWithKeywords(keywords, index + 1, product, userLocation, onComplete)
                    }
                }
                
                override fun onPoiItemSearched(poiItem: PoiItem?, errorCode: Int) {
                }
            })
            
            poiSearch.searchPOIAsyn()
        } catch (e: Exception) {
            android.util.Log.e("MapActivity", "POI搜索异常", e)
            e.printStackTrace()
            searchWithKeywords(keywords, index + 1, product, userLocation, onComplete)
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    private fun adjustMapBounds() {
        if (markers.isEmpty()) return
        
        val builder = LatLngBounds.builder()
        markers.forEach { marker ->
            builder.include(marker.position)
        }
        
        userLocation?.let { builder.include(it) }
        
        val bounds = builder.build()
        val padding = 100
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    fun addProductMarker(name: String, latitude: Double, longitude: Double, productId: Long) {
        val latLng = LatLng(latitude, longitude)
        val marker = aMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(name)
                .snippet("商品ID: $productId")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .draggable(false)
        )
        marker?.let {
            it.`object` = productId
            markers.add(it)
        }
    }

    fun clearMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val markerPosition = marker.position
        val productsAtLocation = nearbyProducts.filter { product ->
            product.latitude != null && product.longitude != null &&
            Math.abs(product.latitude!! - markerPosition.latitude) < 0.0001 &&
            Math.abs(product.longitude!! - markerPosition.longitude) < 0.0001
        }
        
        if (productsAtLocation.isNotEmpty()) {
            if (productsAtLocation.size == 1) {
                val p = productsAtLocation[0]
                val distance = userLocation?.let { current ->
                    calculateDistance(
                        current.latitude,
                        current.longitude,
                        p.latitude!!,
                        p.longitude!!
                    )
                } ?: 0.0
                
                Toast.makeText(
                    this,
                    "${p.title}\n价格: ¥${p.price}\n距离: ${String.format("%.2f", distance)}公里",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                showProductsDialog(productsAtLocation)
            }
        }
        return true
    }
    
    private fun showProductsDialog(products: List<Product>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_products_at_location, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.productsRecyclerView)
        
        val userLocationPair = userLocation?.let { android.util.Pair(it.latitude, it.longitude) }
        
        val adapter = ProductListAdapter(products, userLocationPair) { product ->
            Toast.makeText(
                this,
                "${product.title}\n价格: ¥${product.price}\n地点: ${product.location}",
                Toast.LENGTH_LONG
            ).show()
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()
        
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        locationClient.stopLocation()
        locationClient.onDestroy()
    }
}
