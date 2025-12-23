package com.wujunhao.a202302010306.itemplatform.adapter

import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.wujunhao.a202302010306.itemplatform.R
import com.wujunhao.a202302010306.itemplatform.utils.ImageUtils

class SelectedImagesAdapter(
    private var images: List<Any> = emptyList(), // Can be Uri or String (file path)
    private val onImageClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.iv_selected_image)
        val deleteButton: ImageView = itemView.findViewById(R.id.iv_delete_image)
        
        fun bind(image: Any, position: Int) {
            when (image) {
                is Uri -> {
                    // Load from URI
                    itemView.context.contentResolver.openInputStream(image)?.use { stream ->
                        val bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                        imageView.setImageBitmap(bitmap)
                    }
                }
                is String -> {
                    // Load from file path
                    val bitmap = ImageUtils.loadImage(itemView.context, image)
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    } else {
                        imageView.setImageResource(R.drawable.ic_image_placeholder)
                    }
                }
                is Bitmap -> {
                    imageView.setImageBitmap(image)
                }
            }
            
            imageView.setOnClickListener {
                onImageClick(position)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }
    
    override fun getItemCount(): Int = images.size
    
    fun updateImages(newImages: List<Any>) {
        images = newImages
        notifyDataSetChanged()
    }
    
    fun addImage(image: Any) {
        val newList = images.toMutableList()
        newList.add(image)
        updateImages(newList)
    }
    
    fun removeImage(position: Int) {
        if (position in images.indices) {
            val newList = images.toMutableList()
            newList.removeAt(position)
            updateImages(newList)
        }
    }
}