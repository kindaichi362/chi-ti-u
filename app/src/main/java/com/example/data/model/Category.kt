package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CategoryType(val displayName: String) {
    EXPENSE("Chi tiêu"),
    INCOME("Thu nhập")
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: CategoryType,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = true,
    val displayOrder: Int = 0,
    val parentId: Long? = null,
    val emoji: String? = null,
    val groupName: String? = null,
    val isPriority: Boolean = false
)
