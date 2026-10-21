package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Category
import com.example.data.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY displayOrder ASC, id ASC")
    fun getCategoriesByType(type: CategoryType): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY displayOrder ASC, id ASC")
    fun getRootCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY displayOrder ASC, id ASC")
    fun getSubcategories(parentId: Long): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT name FROM categories")
    suspend fun getAllCategoryNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Query("UPDATE categories SET isPriority = :isPriority WHERE id = :categoryId")
    suspend fun setCategoryPriority(categoryId: Long, isPriority: Boolean)

    @Query("SELECT * FROM categories WHERE isPriority = 1 ORDER BY displayOrder ASC, id ASC")
    fun getPriorityCategories(): Flow<List<Category>>

    @Query("DELETE FROM categories WHERE isDefault = 1")
    suspend fun clearDefaultCategories()

    @Delete
    suspend fun deleteCategory(category: Category)
}
