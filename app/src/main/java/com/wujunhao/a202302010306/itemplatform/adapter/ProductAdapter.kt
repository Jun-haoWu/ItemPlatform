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
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils

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

        fun bind(product: Product) {
            productTitle.text = product.title
            productPrice.text = "¥${String.format("%.2f", product.price)}"
            productLocation.text = product.location
            productViews.text = "${product.viewCount}浏览"

            // Load product image if available
            if (!product.images.isNullOrEmpty()) {
                val imagePaths = ImageUtils.getProductImagePaths(product.images)
                if (imagePaths.isNotEmpty()) {
                    val firstImagePath = imagePaths[0]
                    val bitmap = ImageUtils.loadImage(itemView.context, firstImagePath)
                    if (bitmap != null) {
                        productImage.setImageBitmap(bitmap)
                    } else {
                        productImage.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    productImage.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
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