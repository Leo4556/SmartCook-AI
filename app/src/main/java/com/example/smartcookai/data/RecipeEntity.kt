package com.example.smartcookai.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
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

    val servings: Int = 1,

    val totalKcal: Double = 0.0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0
) : Parcelable