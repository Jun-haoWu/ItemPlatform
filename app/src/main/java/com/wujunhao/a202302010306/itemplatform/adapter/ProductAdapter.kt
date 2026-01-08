package com.wujunhao.a202302010306.itemplatform.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.network.CloudConfig
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.product_image)
        private val productTitle: TextView = itemView.findViewById(R.id.product_title)
        private val productPrice: TextView = itemView.findViewById(R.id.product_price)
        private val productLocation: TextView = itemView.findViewById(R.id.product_location)
        private val productViews: TextView = itemView.findViewById(R.id.product_views)
        private val productLikes: TextView = itemView.findViewById(R.id.product_likes)

        fun bind(product: Product) {
            productTitle.text = product.title
            productPrice.text = "¥${String.format("%.2f", product.price)}"
            productLocation.text = product.location
            productViews.text = "${product.viewCount}浏览"
            productLikes.text = "${product.likeCount}收藏"

            android.util.Log.d("ProductAdapter", "开始加载商品图片: id=${product.id}, title=${product.title}, images=${product.images}")

            // Load product image if available
            if (!product.images.isNullOrEmpty()) {
                val imagePaths = ImageUtils.getProductImagePaths(product.images)
                android.util.Log.d("ProductAdapter", "解析到的图片路径数量: ${imagePaths.size}, 路径: $imagePaths")
                
                if (imagePaths.isNotEmpty()) {
                    val firstImagePath = imagePaths[0]
                    val imagePathToLoad = if (firstImagePath.startsWith("/uploads/")) {
                        CloudConfig.getServerBaseUrl() + firstImagePath.substring(1)
                    } else {
                        firstImagePath
                    }
                    
                    android.util.Log.d("ProductAdapter", "准备加载图片: $imagePathToLoad")
                    
                    // 使用协程异步加载图片
                    GlobalScope.launch(Dispatchers.Main) {
                        productImage.setImageResource(R.drawable.ic_image_placeholder)
                        
                        val bitmap = withContext(Dispatchers.IO) {
                            ImageUtils.loadImage(itemView.context, imagePathToLoad)
                        }
                        
                        if (bitmap != null) {
                            android.util.Log.d("ProductAdapter", "图片加载成功: id=${product.id}")
                            productImage.setImageBitmap(bitmap)
                        } else {
                            // 图片加载失败，显示占位图
                            android.util.Log.w("ProductAdapter", "图片加载失败: $imagePathToLoad")
                            productImage.setImageResource(R.drawable.ic_image_placeholder)
                        }
                    }
                } else {
                    android.util.Log.w("ProductAdapter", "图片路径列表为空，显示占位图")
                    productImage.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                android.util.Log.d("ProductAdapter", "商品没有图片，显示占位图")
                productImage.setImageResource(R.drawable.ic_image_placeholder)
            }

            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}