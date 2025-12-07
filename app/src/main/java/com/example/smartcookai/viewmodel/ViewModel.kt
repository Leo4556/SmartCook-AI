package com.example.smartcookai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    fun addRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.addRecipe(recipe)
        }
    }

    fun updateRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.updateRecipe(recipe)
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.deleteRecipe(recipe)
        }
    }

    // Удалить из избранного
    fun removeFromFavorites(recipe: RecipeEntity) {
        viewModelScope.launch {
            val updatedRecipe = recipe.copy(isFavorite = false)
            repository.updateRecipe(updatedRecipe)
        }
    }

    // Добавить/удалить из избранного (переключить)
    fun toggleFavorite(recipe: RecipeEntity) {
        viewModelScope.launch {
            val updatedRecipe = recipe.copy(isFavorite = !recipe.isFavorite)
            repository.updateRecipe(updatedRecipe)
        }
    }

    // Получить только избранные рецепты
    fun getFavoriteRecipes(): LiveData<List<RecipeEntity>> {
        return repository.getFavoriteRecipes()
    }

    // Получить ВСЕ рецепты
    val allRecipes: LiveData<List<RecipeEntity>>
        get() = repository.getAllRecipes()
}