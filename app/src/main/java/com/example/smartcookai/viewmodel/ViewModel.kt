package com.example.smartcookai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecipeViewModel(private val repository: RecipeRepository) : ViewModel() {

    fun addRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.addRecipe(recipe)
        }
    }

    fun updateRecipe(recipe: RecipeEntity) {
        viewModelScope.launch (Dispatchers.IO){
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
    fun getFavouriteRecipes(): LiveData<List<RecipeEntity>> {
        return repository.getFavouriteRecipes()
    }

    // Добавьте этот метод в RecipeViewModel
    fun searchFavouriteRecipes(query: String): LiveData<List<RecipeEntity>> {
        return repository.searchFavouriteRecipes(query)
    }

    // Получить ВСЕ рецепты
    val allRecipes: LiveData<List<RecipeEntity>>
        get() = repository.getAllRecipes()

    fun getRecipeByIdLive(id: Int): LiveData<RecipeEntity> {
        return repository.getRecipeByIdLive(id)
    }
}