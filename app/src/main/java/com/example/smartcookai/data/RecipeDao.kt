package com.example.smartcookai.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY id DESC")
    fun getAllRecipes(): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavourites(): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE isFavorite = 1 AND title LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchFavourites(query: String): LiveData<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    fun getRecipeByIdLive(id: Int): LiveData<RecipeEntity>
}
