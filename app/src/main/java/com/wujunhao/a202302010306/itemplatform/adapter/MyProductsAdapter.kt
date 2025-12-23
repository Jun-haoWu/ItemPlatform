package com.wujunhao.a202302010306.itemplatform.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.model.Product
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils

class MyProductsAdapter(
    private var products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<MyProductsAdapter.MyProductViewHolder>() {

    inner class MyProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.iv_product_image)
        private val productTitle: TextView = itemView.findViewById(R.id.tv_product_title)
        private val productPrice: TextView = itemView.findViewById(R.id.tv_product_price)
        private val productLocation: TextView = itemView.findViewById(R.id.tv_product_location)
        private val productViews: TextView = itemView.findViewById(R.id.tv_product_views)
        private val productStatus: TextView = itemView.findViewById(R.id.tv_product_status)
        private val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        fun bind(product: Product) {
            productTitle.text = product.title
            productPrice.text = "¥${String.format("%.2f", product.price)}"
            productLocation.text = product.location
            productViews.text = "${product.viewCount}浏览"

            // Set status text and color
            when (product.status) {
                Product.STATUS_ACTIVE -> {
                    productStatus.text = "在售"
                    productStatus.setBackgroundResource(R.drawable.status_background)
                }
                Product.STATUS_SOLD -> {
                    productStatus.text = "已售"
                    productStatus.setBackgroundResource(android.R.color.darker_gray)
                }
                else -> {
                    productStatus.text = "未知"
                    productStatus.setBackgroundResource(android.R.color.darker_gray)
                }
            }

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

            // Set click listeners
            btnEdit.setOnClickListener { onEditClick(product) }
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_product, parent, false)
        return MyProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}