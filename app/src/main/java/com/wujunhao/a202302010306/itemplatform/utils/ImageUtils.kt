package com.wujunhao.a202302010306.itemplatform.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    
    private const val PRODUCT_IMAGES_DIR = "product_images"
    private const val IMAGE_QUALITY = 80
    private const val MAX_IMAGE_SIZE = 1024 // Max dimension in pixels
    
    /**
     * Create a directory for storing product images
     */
    fun getProductImagesDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), PRODUCT_IMAGES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Generate a unique filename for product image
     */
    fun generateImageFileName(productId: Long): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "product_${productId}_${timestamp}.jpg"
    }
    
    /**
     * Save image from URI and return the file path
     */
    fun saveImageFromUri(context: Context, imageUri: Uri, productId: Long): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    saveImage(context, bitmap, productId)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Save bitmap to file and return the file path
     */
    fun saveImage(context: Context, bitmap: Bitmap, productId: Long): String? {
        return try {
            val imagesDir = getProductImagesDir(context)
            val fileName = generateImageFileName(productId)
            val file = File(imagesDir, fileName)
            
            // Resize image if needed
            val resizedBitmap = resizeBitmapIfNeeded(bitmap)
            
            FileOutputStream(file).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Load image from file path
     */
    fun loadImage(context: Context, imagePath: String?): Bitmap? {
        if (imagePath.isNullOrEmpty()) return null
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(imagePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Delete image file
     */
    fun deleteImage(context: Context, imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return true
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Resize bitmap if it exceeds maximum dimensions
     */
    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }
        
        val scaleFactor = if (width > height) {
            MAX_IMAGE_SIZE.toFloat() / width
        } else {
            MAX_IMAGE_SIZE.toFloat() / height
        }
        
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Delete image file
     */
    fun deleteImage(imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return false
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get all image paths for a product (assuming multiple images are stored as comma-separated paths)
     */
    fun getProductImagePaths(imagePathsString: String?): List<String> {
        return imagePathsString?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }
    
    /**
     * Convert multiple image paths to a single string for database storage
     */
    fun convertImagePathsToString(imagePaths: List<String>): String {
        return imagePaths.joinToString(",")
    }
}