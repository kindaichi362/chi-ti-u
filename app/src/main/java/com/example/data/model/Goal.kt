package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val monthlyTarget: Double = 0.0,
    val targetDate: Long? = null,
    val colorHex: String = "#0A5C36",
    val iconName: String = "flag",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
