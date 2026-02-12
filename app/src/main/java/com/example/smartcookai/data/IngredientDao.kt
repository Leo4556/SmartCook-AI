package com.example.smartcookai.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface IngredientDao{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun getAllIngredients(): LiveData<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchIngredients(query: String): List<IngredientEntity>

    @Query("SELECT * FROM ingredients WHERE id = :id LIMIT 1")
    suspend fun getIngredientById(id: Int): IngredientEntity?
}