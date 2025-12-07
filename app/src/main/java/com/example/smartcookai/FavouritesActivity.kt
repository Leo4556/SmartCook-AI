package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityFavouritesBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel
        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        setupRecycler()
        observeFavorites() // Теперь наблюдаем только за избранными
        setupBottomNavigation()
    }

    private fun setupRecycler() {
        binding.rvFavourites.layoutManager = LinearLayoutManager(this)
        adapter = RecipeAdapter(emptyList()) { recipe ->
            recipeViewModel.removeFromFavorites(recipe)
        }

        binding.rvFavourites.adapter = adapter
    }

    private fun observeFavorites() {
        // Наблюдаем только за избранными рецептами
        recipeViewModel.getFavoriteRecipes().observe(this) { favoriteRecipes ->
            // Фильтруем, чтобы быть уверенными что показываем только isFavorite = true
            val favoritesOnly = favoriteRecipes.filter { it.isFavorite }
            adapter.updateList(favoritesOnly)

            // Можно добавить проверку на пустой список
            if (favoritesOnly.isEmpty()) {
                // Показать сообщение "Нет избранных рецептов"
            }
        }
    }

    private fun setupBottomNavigation() {
        // Подсветим текущую вкладку "Избранное"
        binding.bottomBar.tabFav.isSelected = true

        binding.bottomBar.tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Закрываем текущую активити
        }
        binding.bottomBar.tabAdd.setOnClickListener {
            startActivity(Intent(this, AddActivity::class.java))
            finish()
        }
        binding.bottomBar.tabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        // Избранное уже открыто, можно ничего не делать или обновить список
        binding.bottomBar.tabFav.setOnClickListener {
            // Просто обновляем список
            recipeViewModel.getFavoriteRecipes()
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        recipeViewModel.getFavoriteRecipes()
    }
}