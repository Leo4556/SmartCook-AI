package com.example.smartcookai

import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityFavouritesBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.snackbar.Snackbar

class FavouritesActivity : BaseActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

    private var allFavouriteRecipes: List<RecipeEntity> = emptyList()

    // Переменная для хранения рецепта, который удалили (на случай отмены)
    private var removedRecipe: RecipeEntity? = null

    // Переменная для отслеживания текущего поискового запроса
    private var currentSearchQuery: String = ""

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
        setupSearch()
        observeFavorites()
        setupBottomNavigation()
    }

    private fun setupRecycler() {
        binding.rvFavourites.layoutManager = LinearLayoutManager(this)

        adapter = RecipeAdapter(
            emptyList(),
            onItemClick = { recipe, imageView -> // ← ИЗМЕНЕНО: добавлены View параметры
                openRecipeDetails(recipe, imageView)
            },
            onFavoriteClick = { recipe ->
                removeFromFavoritesImmediately(recipe)
            }
        )

        binding.rvFavourites.adapter = adapter
    }

    // ← НОВЫЙ МЕТОД: открытие деталей с анимацией
    private fun openRecipeDetails(recipe: RecipeEntity, imageView: View) {
        val intent = Intent(this, RecipeDetailsActivity::class.java)
        intent.putExtra("recipe", recipe)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // ← ИЗМЕНЕНО: анимируем только изображение
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                imageView,
                "recipe_image_${recipe.id}"
            )
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
    }

    private fun removeFromFavoritesImmediately(recipe: RecipeEntity) {
        // Сохраняем копию удаленного рецепта для возможности отмены
        removedRecipe = recipe.copy()

        // Создаем обновленный рецепт с isFavorite = false
        val updatedRecipe = recipe.copy(isFavorite = false)

        // Обновляем в базе данных
        recipeViewModel.updateRecipe(updatedRecipe)

        // Показываем Snackbar с возможностью отмены
        showUndoSnackbar(recipe.title)
    }

    private fun showUndoSnackbar(recipeTitle: String) {
        val snackbar = Snackbar.make(
            binding.root,
            "Рецепт \"$recipeTitle\" удален из избранного",
            Snackbar.LENGTH_LONG
        )

        snackbar.anchorView = binding.bottomBar.bottomBar

        snackbar.setAction("ОТМЕНИТЬ") {
            undoRemove()
        }

        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.colorAccentDark)
        )

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                // Если Snackbar закрылся без нажатия "ОТМЕНИТЬ", очищаем сохраненный рецепт
                if (event != DISMISS_EVENT_ACTION) {
                    removedRecipe = null
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

    private fun undoRemove() {
        removedRecipe?.let { recipe ->
            // Возвращаем рецепт в избранное
            val restoredRecipe = recipe.copy(isFavorite = true)
            recipeViewModel.updateRecipe(restoredRecipe)
            removedRecipe = null
        }
    }

    private fun observeFavorites() {
        recipeViewModel.getFavouriteRecipes().observe(this) { favouriteRecipes ->
            allFavouriteRecipes = favouriteRecipes.filter { it.isFavorite }
            applySearch(currentSearchQuery)
        }
    }

    private fun setupSearch() {
        binding.searchLayout.visibility = android.view.View.VISIBLE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                applySearch(currentSearchQuery)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun applySearch(query: String) {
        val filteredRecipes = if (query.isNotBlank()) {
            val normalizedQuery = query.lowercase().trim()

            if (normalizedQuery.contains(",")) {
                // Поиск по ингредиентам через запятую
                val ingredientsQuery = normalizedQuery
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                allFavouriteRecipes.filter { recipe ->
                    val recipeIngredients = recipe.ingredients.lowercase()

                    ingredientsQuery.all { ingredient ->
                        recipeIngredients.contains(ingredient)
                    }
                }
            } else {
                // Обычный поиск по названию, описанию и ингредиентам
                val words = normalizedQuery
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }

                allFavouriteRecipes.filter { recipe ->
                    val text = (
                            recipe.title + " " +
                                    recipe.ingredients + " " +
                                    recipe.description
                            ).lowercase()

                    words.all { word ->
                        text.contains(word)
                    }
                }
            }
        } else {
            allFavouriteRecipes
        }

        updateUI(filteredRecipes)
    }

    private fun updateUI(recipes: List<RecipeEntity>) {
        adapter.updateList(recipes)

        if (recipes.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        binding.rvFavourites.visibility = android.view.View.GONE
        binding.tvEmptyFavorites.visibility = android.view.View.VISIBLE

        if (currentSearchQuery.isNotEmpty()) {
            binding.tvEmptyFavorites.text =
                "По запросу \"$currentSearchQuery\" ничего не найдено"
        } else {
            binding.tvEmptyFavorites.text = "Нет избранных рецептов"
        }
    }

    private fun hideEmptyState() {
        binding.rvFavourites.visibility = android.view.View.VISIBLE
        binding.tvEmptyFavorites.visibility = android.view.View.GONE
    }

    private fun setupBottomNavigation() {
        // Подсветим текущую вкладку "Избранное"
        binding.bottomBar.tabFav.isSelected = true

        binding.bottomBar.tabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
        if (currentSearchQuery.isEmpty()) {
            recipeViewModel.getFavouriteRecipes()
        } else {
            recipeViewModel.searchFavouriteRecipes(currentSearchQuery)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем сохраненный рецепт при уничтожении активности
        removedRecipe = null
    }
}