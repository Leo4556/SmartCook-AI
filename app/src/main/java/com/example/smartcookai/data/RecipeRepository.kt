package com.example.smartcookai.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map

class RecipeRepository(private val dao: RecipeDao) {

    suspend fun addRecipe(recipe: RecipeEntity) {
        dao.insert(recipe)
    }

    suspend fun updateRecipe(recipe: RecipeEntity) {
        dao.update(recipe)
    }

    suspend fun deleteRecipe(recipe: RecipeEntity) {
        dao.delete(recipe)
    }

    fun getAllRecipes(): LiveData<List<RecipeEntity>> {
        return dao.getAllRecipes()
    }

    fun getFavoriteRecipes(): LiveData<List<RecipeEntity>> {
        return dao.getAllRecipes().map { recipes ->
            recipes.filter { it.isFavorite }
        }
    }
}