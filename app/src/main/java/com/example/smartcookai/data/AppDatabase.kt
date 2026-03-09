package com.example.smartcookai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RecipeEntity::class,
    ],
    version = 3
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipes.db"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}
