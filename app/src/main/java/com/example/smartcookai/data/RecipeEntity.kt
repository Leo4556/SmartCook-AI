package com.example.smartcookai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val cookingTime: Int,
    val ingredients: String,
    val description: String,
    val imagePath: String?,     // uri фото блюда
    val isFavorite: Boolean = true,
)
