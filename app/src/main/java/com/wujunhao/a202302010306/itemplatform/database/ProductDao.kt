package com.wujunhao.a202302010306.itemplatform.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import com.wujunhao.a202302010306.itemplatform.model.Product

class ProductDao(private val databaseHelper: DatabaseHelper) {
    
    fun insertProduct(product: Product): Long {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.ProductEntry.COLUMN_TITLE, product.title)
            put(DatabaseContract.ProductEntry.COLUMN_DESCRIPTION, product.description)
            put(DatabaseContract.ProductEntry.COLUMN_PRICE, product.price)
            put(DatabaseContract.ProductEntry.COLUMN_CATEGORY, product.category)
            put(DatabaseContract.ProductEntry.COLUMN_CONDITION, product.condition)
            put(DatabaseContract.ProductEntry.COLUMN_LOCATION, product.location)
            put(DatabaseContract.ProductEntry.COLUMN_IMAGES, product.images)
            put(DatabaseContract.ProductEntry.COLUMN_SELLER_ID, product.sellerId)
            put(DatabaseContract.ProductEntry.COLUMN_STATUS, product.status)
            put(DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT, product.viewCount)
            put(DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT, product.likeCount)
            put(DatabaseContract.ProductEntry.COLUMN_CREATED_AT, product.createdAt)
            put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, product.updatedAt)
        }
        
        val result = db.insert(DatabaseContract.ProductEntry.TABLE_NAME, null, values)
        android.util.Log.d("ProductDao", "insertProduct - 插入商品: ${product.title}, id: $result, status: ${product.status}")
        return result
    }
    
    fun getAllProducts(): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            android.util.Log.d("ProductDao", "getAllProducts - 开始查询，status = ${Product.STATUS_ACTIVE}")
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_STATUS} = ?",
                arrayOf(Product.STATUS_ACTIVE.toString()),
                null,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_CREATED_AT} DESC"
            )
            android.util.Log.d("ProductDao", "getAllProducts - 查询到记录数: ${cursor.count}")
            
            cursor.use {
                val products = cursorToProductList(it)
                android.util.Log.d("ProductDao", "getAllProducts - 转换后的商品数: ${products.size}")
                products
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductDao", "getAllProducts - 查询失败", e)
            emptyList()
        }
    }
    
    fun getProductsByCategory(category: String): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_CATEGORY} = ? AND ${DatabaseContract.ProductEntry.COLUMN_STATUS} = ?",
                arrayOf(category, Product.STATUS_ACTIVE.toString()),
                null,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_CREATED_AT} DESC"
            )
            
            cursor.use { cursorToProductList(it) }
        } catch (e: Exception) {
            // Table doesn't exist yet, return empty list
            emptyList()
        }
    }
    
    fun searchProducts(keyword: String): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            val searchPattern = "%$keyword%"
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "(${DatabaseContract.ProductEntry.COLUMN_TITLE} LIKE ? OR ${DatabaseContract.ProductEntry.COLUMN_DESCRIPTION} LIKE ?) AND ${DatabaseContract.ProductEntry.COLUMN_STATUS} = ?",
                arrayOf(searchPattern, searchPattern, Product.STATUS_ACTIVE.toString()),
                null,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_CREATED_AT} DESC"
            )
            
            cursor.use { cursorToProductList(it) }
        } catch (e: Exception) {
            // Table doesn't exist yet, return empty list
            emptyList()
        }
    }
    
    fun getProductsSortedByPrice(ascending: Boolean = true): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            val orderBy = if (ascending) {
                "${DatabaseContract.ProductEntry.COLUMN_PRICE} ASC"
            } else {
                "${DatabaseContract.ProductEntry.COLUMN_PRICE} DESC"
            }
            
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_STATUS} = ?",
                arrayOf(Product.STATUS_ACTIVE.toString()),
                null,
                null,
                orderBy
            )
            
            cursor.use { cursorToProductList(it) }
        } catch (e: Exception) {
            // Table doesn't exist yet, return empty list
            emptyList()
        }
    }
    
    fun getProductsBySeller(sellerId: Long): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_SELLER_ID} = ?",
                arrayOf(sellerId.toString()),
                null,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_CREATED_AT} DESC"
            )
            
            cursor.use { cursorToProductList(it) }
        } catch (e: Exception) {
            // Table doesn't exist yet, return empty list
            emptyList()
        }
    }
    
    fun incrementViewCount(productId: Long) {
        val db = databaseHelper.writableDatabase
        db.execSQL(
            "UPDATE ${DatabaseContract.ProductEntry.TABLE_NAME} SET ${DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT} = ${DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT} + 1 WHERE ${BaseColumns._ID} = ?",
            arrayOf(productId)
        )
    }
    
    fun updateProductStatus(productId: Long, status: Int) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.ProductEntry.COLUMN_STATUS, status)
            put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        
        db.update(
            DatabaseContract.ProductEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(productId.toString())
        )
    }
    
    fun deleteProduct(productId: Long) {
        updateProductStatus(productId, Product.STATUS_DELETED)
    }
    
    fun updateProductImages(productId: Long, imagePaths: String?) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.ProductEntry.COLUMN_IMAGES, imagePaths)
            put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
        }
        
        db.update(
            DatabaseContract.ProductEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(productId.toString())
        )
    }
    
    fun getProductById(productId: Long): Product? {
        return try {
            val db = databaseHelper.readableDatabase
            android.util.Log.d("ProductDao", "getProductById - 查询商品ID: $productId")
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${BaseColumns._ID} = ?",
                arrayOf(productId.toString()),
                null,
                null,
                null
            )
            
            cursor.use {
                android.util.Log.d("ProductDao", "getProductById - 查询到记录数: ${it.count}")
                val products = cursorToProductList(it)
                android.util.Log.d("ProductDao", "getProductById - 转换后的商品数: ${products.size}")
                if (products.isNotEmpty()) products[0] else null
            }
        } catch (e: Exception) {
            android.util.Log.e("ProductDao", "getProductById - 查询失败", e)
            null
        }
    }
    
    fun updateProduct(product: Product) {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseContract.ProductEntry.COLUMN_TITLE, product.title)
            put(DatabaseContract.ProductEntry.COLUMN_DESCRIPTION, product.description)
            put(DatabaseContract.ProductEntry.COLUMN_PRICE, product.price)
            put(DatabaseContract.ProductEntry.COLUMN_CATEGORY, product.category)
            put(DatabaseContract.ProductEntry.COLUMN_CONDITION, product.condition)
            put(DatabaseContract.ProductEntry.COLUMN_LOCATION, product.location)
            put(DatabaseContract.ProductEntry.COLUMN_IMAGES, product.images)
            put(DatabaseContract.ProductEntry.COLUMN_STATUS, product.status)
            put(DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT, product.viewCount)
            put(DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT, product.likeCount)
            put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, product.updatedAt)
        }
        
        val rowsAffected = db.update(
            DatabaseContract.ProductEntry.TABLE_NAME,
            values,
            "${BaseColumns._ID} = ?",
            arrayOf(product.id.toString())
        )
        android.util.Log.d("ProductDao", "updateProduct - 更新商品: ${product.title}, id: ${product.id}, status: ${product.status}, likeCount: ${product.likeCount}, viewCount: ${product.viewCount}, images: ${product.images}, 影响行数: $rowsAffected")
    }
    
    fun incrementLikeCount(productId: Long): Int {
        val db = databaseHelper.writableDatabase
        return try {
            val currentCount = getProductById(productId)?.likeCount ?: 0
            val newCount = currentCount + 1
            val values = ContentValues().apply {
                put(DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT, newCount)
                put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
            }
            
            db.update(
                DatabaseContract.ProductEntry.TABLE_NAME,
                values,
                "${BaseColumns._ID} = ?",
                arrayOf(productId.toString())
            )
            newCount
        } catch (e: Exception) {
            android.util.Log.e("ProductDao", "增加收藏数失败", e)
            getProductById(productId)?.likeCount ?: 0
        }
    }
    
    fun decrementLikeCount(productId: Long): Int {
        val db = databaseHelper.writableDatabase
        return try {
            val currentCount = getProductById(productId)?.likeCount ?: 0
            val newCount = maxOf(0, currentCount - 1)
            val values = ContentValues().apply {
                put(DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT, newCount)
                put(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT, System.currentTimeMillis())
            }
            
            db.update(
                DatabaseContract.ProductEntry.TABLE_NAME,
                values,
                "${BaseColumns._ID} = ?",
                arrayOf(productId.toString())
            )
            newCount
        } catch (e: Exception) {
            android.util.Log.e("ProductDao", "减少收藏数失败", e)
            getProductById(productId)?.likeCount ?: 0
        }
    }
    
    private fun cursorToProductList(cursor: Cursor): List<Product> {
        val products = mutableListOf<Product>()
        
        while (cursor.moveToNext()) {
            val product = Product(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_TITLE)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_DESCRIPTION)),
                price = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_PRICE)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_CATEGORY)),
                condition = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_CONDITION)),
                location = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_LOCATION)),
                images = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_IMAGES)),
                sellerId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_SELLER_ID)),
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_STATUS)),
                viewCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_VIEW_COUNT)),
                likeCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_LIKE_COUNT)),
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseContract.ProductEntry.COLUMN_UPDATED_AT))
            )
            products.add(product)
        }
        
        return products
    }
}