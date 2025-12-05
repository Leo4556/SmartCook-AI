package com.example.smartcookai.data

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

    suspend fun getAllRecipes(): List<RecipeEntity> {
        return dao.getAllRecipes()
    }

    suspend fun getRecipe(id: Int): RecipeEntity? {
        return dao.getRecipeById(id)
    }

    suspend fun getFavourites(): List<RecipeEntity> {
        return dao.getFavourites()
    }
}
