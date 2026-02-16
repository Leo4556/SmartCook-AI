package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

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
            onItemClick = { recipe ->
                val intent = Intent(this, RecipeDetailsActivity::class.java)
                intent.putExtra("recipe", recipe)
                startActivity(intent)
            },
            onFavoriteClick = { recipe ->
                // Сразу удаляем из избранного и обновляем UI
                removeFromFavoritesImmediately(recipe)
            }
        )

        binding.rvFavourites.adapter = adapter
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
            // Возвращаем рецепт в избранное
            undoRemove()
        }

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

            // Показываем КОРОТКОЕ уведомление об отмене над bottom bar
            val undoConfirmationSnackbar = Snackbar.make(
                binding.root,
                "Действие отменено",
                Snackbar.LENGTH_SHORT
            )

            // Привязываем к bottom bar
            undoConfirmationSnackbar.anchorView = binding.bottomBar.bottomBar

            // Настраиваем цвета
            undoConfirmationSnackbar.setActionTextColor(
                ContextCompat.getColor(this, R.color.snackbar_background)
            )

            val textView = undoConfirmationSnackbar.view.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text
            )

            val params = textView.layoutParams as? CoordinatorLayout.LayoutParams
            params?.apply {
                marginStart = 16
                marginEnd = 16
                bottomMargin = 16
            }

            undoConfirmationSnackbar.show()

            removedRecipe = null
        }
    }

    private fun setupSearch() {
        // Делаем SearchView видимым
        binding.searchLayout.visibility = android.view.View.VISIBLE

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                performSearch(currentSearchQuery)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

//        binding.btnClearSearch.setOnClickListener {
//            binding.etSearch.text?.clear()
//            binding.etSearch.clearFocus()
//        }

        // Показываем/скрываем кнопку очистки в зависимости от наличия текста
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) {
//                    android.view.View.GONE
//                } else {
//                    android.view.View.VISIBLE
//                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            // Если поиск пустой - показываем все избранные
            recipeViewModel.getFavouriteRecipes().observe(this) { favouriteRecipes ->
                val favoritesOnly = favouriteRecipes.filter { it.isFavorite }
                updateUI(favoritesOnly)
            }
        } else {
            // Ищем по избранным
            recipeViewModel.searchFavouriteRecipes(query).observe(this) { searchResults ->
                updateUI(searchResults)
            }
        }
    }

    private fun updateUI(recipes: List<RecipeEntity>) {
        adapter.updateList(recipes)

        // Показываем/скрываем сообщение о пустом списке
        if (recipes.isEmpty()) {
            binding.tvEmptyFavorites.visibility = android.view.View.VISIBLE
            if (currentSearchQuery.isNotEmpty()) {
                binding.tvEmptyFavorites.text =
                    "По запросу \"$currentSearchQuery\" ничего не найдено"
            } else {
                binding.tvEmptyFavorites.text = "Нет избранных рецептов"
            }
        } else {
            binding.tvEmptyFavorites.visibility = android.view.View.GONE
        }
    }

    private fun observeFavorites() {
        // Наблюдаем только за избранными рецептами
        recipeViewModel.getFavouriteRecipes().observe(this) { favouriteRecipes ->
            // Если нет активного поиска, обновляем список
            if (currentSearchQuery.isEmpty()) {
                val favoritesOnly = favouriteRecipes.filter { it.isFavorite }
                updateUI(favoritesOnly)
            }
        }
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