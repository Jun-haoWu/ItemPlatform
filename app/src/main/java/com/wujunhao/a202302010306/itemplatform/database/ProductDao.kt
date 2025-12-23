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
        
        return db.insert(DatabaseContract.ProductEntry.TABLE_NAME, null, values)
    }
    
    fun getAllProducts(): List<Product> {
        return try {
            val db = databaseHelper.readableDatabase
            val cursor = db.query(
                DatabaseContract.ProductEntry.TABLE_NAME,
                null,
                "${DatabaseContract.ProductEntry.COLUMN_STATUS} = ?",
                arrayOf(Product.STATUS_ACTIVE.toString()),
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
        
        return cursor.use { cursorToProductList(it) }
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