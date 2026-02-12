package com.example.smartcookai.data

import androidx.room.Entity

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["recipeId", "ingredientId"]
)
data class RecipeIngredientCrossRef(
    val recipeId: Int,
    val ingredientId: Int,
    val weightInGrams: Double
)
