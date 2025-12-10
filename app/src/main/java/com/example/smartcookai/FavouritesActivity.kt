package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityFavouritesBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.snackbar.Snackbar

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

    // Переменная для хранения рецепта, который пытаемся удалить
    private var pendingRecipeToRemove: RecipeEntity? = null

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
        observeFavorites()
        setupBottomNavigation()
    }

    private fun setupRecycler() {
        binding.rvFavourites.layoutManager = LinearLayoutManager(this)

        adapter = RecipeAdapter(
            emptyList(),
            onItemClick = { recipe ->
                val intent = Intent(this, RecipeDetailsActivity::class.java)
                intent.putExtra("title", recipe.title)
                intent.putExtra("time", recipe.cookingTime)
                intent.putExtra("ingredients", recipe.ingredients)
                intent.putExtra("description", recipe.description)
                intent.putExtra("imagePath", recipe.imagePath)
                startActivity(intent)
            },
            onFavoriteClick = { recipe ->
                pendingRecipeToRemove = recipe
                showRemoveConfirmation(recipe)
            }
        )


        binding.rvFavourites.adapter = adapter
    }

    private fun showRemoveConfirmation(recipe: RecipeEntity) {
        pendingRecipeToRemove = recipe

        val snackbar = Snackbar.make(
            binding.root,
            "Удалить \"${recipe.title}\" из избранного?",
            Snackbar.LENGTH_LONG
        )

        snackbar.anchorView = binding.bottomBar.bottomBar

        snackbar.setAction("ОТМЕНА") {
            pendingRecipeToRemove = null
        }

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)

                if (event != DISMISS_EVENT_ACTION && pendingRecipeToRemove != null) {
                    pendingRecipeToRemove?.let { recipeToRemove ->
                        recipeViewModel.removeFromFavorites(recipeToRemove)
                    }
                    pendingRecipeToRemove = null
                }
            }
        })

        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as? CoordinatorLayout.LayoutParams
        params?.apply {
            marginStart = 16
            marginEnd = 16
            bottomMargin = 16
        }

        snackbar.show()
    }

    private fun observeFavorites() {
        // Наблюдаем только за избранными рецептами
        recipeViewModel.getFavouriteRecipes().observe(this) { favouriteRecipes ->
            // Фильтруем, чтобы быть уверенными что показываем только isFavorite = true
            val favoritesOnly = favouriteRecipes.filter { it.isFavorite }
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
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        recipeViewModel.getFavouriteRecipes()
    }
}