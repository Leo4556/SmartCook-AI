package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityMainBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

    // Переменная для отслеживания последнего измененного рецепта для отмены
    private var lastModifiedRecipe: RecipeEntity? = null
    private var wasFavorite: Boolean = false
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Инициализация ViewModel
        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        setupRecyclerView()
        observeAllRecipes()
        setupSearch()
        setupBottomNavigation()

        binding.bottomBar.tabHome.isSelected = true
        binding.btnScan.setOnClickListener{
            startActivity(Intent(this, ScanActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(this, 2)
        binding.rvRecipes.layoutManager = layoutManager

        adapter = RecipeAdapter(
            emptyList(),
            onItemClick = { recipe ->
                val intent = Intent(this, RecipeDetailsActivity::class.java)
                intent.putExtra("recipe", recipe)
                startActivity(intent)
            },
            onFavoriteClick = { recipe ->
                // Сразу изменяем состояние избранного
                toggleFavoriteImmediately(recipe)
            }
        )

        binding.rvRecipes.adapter = adapter

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing)
        binding.rvRecipes.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels, true))
    }

    private fun toggleFavoriteImmediately(recipe: RecipeEntity) {
        // Сохраняем состояние для возможности отмены
        lastModifiedRecipe = recipe.copy()
        wasFavorite = recipe.isFavorite

        // Создаем обновленный рецепт с противоположным значением isFavorite
        val updatedRecipe = recipe.copy(isFavorite = !recipe.isFavorite)

        // Обновляем в базе данных
        recipeViewModel.updateRecipe(updatedRecipe)

        // Показываем Snackbar с возможностью отмены
        showUndoSnackbar(recipe.title, !recipe.isFavorite)
    }

    private fun showUndoSnackbar(
        recipeTitle: String,
        addedToFavorites: Boolean,
    ) {
        val message = if (addedToFavorites) {
            "Рецепт \"$recipeTitle\" добавлен в избранное"
        } else {
            "Рецепт \"$recipeTitle\" удален из избранного"
        }

        val snackbar = Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        )

        // Привязываем Snackbar к bottom bar, чтобы он появлялся над ним
        snackbar.anchorView = binding.bottomBar.bottomBar

        snackbar.setAction("ОТМЕНИТЬ") {
            // Отменяем действие - возвращаем предыдущее состояние
            undoLastFavoriteChange()
        }

        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.colorAccentDark)
        )

        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById<android.widget.TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 3

        val params = snackbarView.layoutParams as? CoordinatorLayout.LayoutParams
        params?.apply {
            marginStart = 16
            marginEnd = 16
            bottomMargin = 16
        }

        snackbar.show()
    }

    private fun undoLastFavoriteChange() {
        lastModifiedRecipe?.let { recipe ->
            // Возвращаем исходное состояние избранного
            val restoredRecipe = recipe.copy(isFavorite = wasFavorite)
            recipeViewModel.updateRecipe(restoredRecipe)

            // Показываем КОРОТКОЕ уведомление об отмене, тоже над bottom bar
            val undoConfirmationSnackbar = Snackbar.make(
                binding.root,
                if (wasFavorite) "Действие отменено" else "Действие отменено",
                Snackbar.LENGTH_SHORT
            )

            // Тоже привязываем к bottom bar
            undoConfirmationSnackbar.anchorView = binding.bottomBar.bottomBar
            val snackbarView = undoConfirmationSnackbar.view
            val params = snackbarView.layoutParams as? CoordinatorLayout.LayoutParams
            params?.apply {
                marginStart = 16
                marginEnd = 16
                bottomMargin = 16
            }

            undoConfirmationSnackbar.show()

            lastModifiedRecipe = null
        }
    }

    private fun observeAllRecipes() {
        recipeViewModel.allRecipes.observe(this) { recipes ->
            // Применяем текущий поиск, если он есть
            val filteredRecipes = if (currentSearchQuery.isNotEmpty()) {
                recipes.filter {
                    it.title.contains(currentSearchQuery, ignoreCase = true) ||
                            it.ingredients.contains(currentSearchQuery, ignoreCase = true) ||
                            it.description.contains(currentSearchQuery, ignoreCase = true)
                }
            } else {
                recipes
            }
            adapter.updateList(filteredRecipes)

            if (filteredRecipes.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                // Перезапускаем наблюдение для применения фильтрации
                recipeViewModel.allRecipes.observe(this@MainActivity) { recipes ->
                    val filteredRecipes = if (currentSearchQuery.isNotEmpty()) {
                        recipes.filter {
                            it.title.contains(currentSearchQuery, ignoreCase = true) ||
                                    it.ingredients.contains(currentSearchQuery, ignoreCase = true) ||
                                    it.description.contains(currentSearchQuery, ignoreCase = true)
                        }
                    } else {
                        recipes
                    }
                    adapter.updateList(filteredRecipes)

                    if (filteredRecipes.isEmpty()) {
                        showEmptyState()
                    } else {
                        hideEmptyState()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Показываем/скрываем кнопку очистки
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showEmptyState() {
        binding.rvRecipes.visibility = android.view.View.GONE
        binding.tvEmptyAll.visibility = android.view.View.VISIBLE
        if (currentSearchQuery.isNotEmpty()) {
            binding.tvEmptyAll.text = "По запросу \"$currentSearchQuery\" ничего не найдено"
        } else {
            binding.tvEmptyAll.text = "Нет рецептов\nНажмите + чтобы добавить"
        }
    }

    private fun hideEmptyState() {
        binding.rvRecipes.visibility = android.view.View.VISIBLE
        binding.tvEmptyAll.visibility = android.view.View.GONE
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.isSelected = true

        binding.bottomBar.tabHome.setOnClickListener {
            // Уже на главной, просто прокручиваем вверх
            binding.rvRecipes.smoothScrollToPosition(0)
        }

        binding.bottomBar.tabFav.setOnClickListener {
            val intent = Intent(this, FavouritesActivity::class.java)
            startActivity(intent)
        }

        binding.bottomBar.tabAdd.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }

        binding.bottomBar.tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    // Класс для отступов между карточками
    inner class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: android.view.View,
            parent: androidx.recyclerview.widget.RecyclerView,
            state: androidx.recyclerview.widget.RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                if (position < spanCount) outRect.top = spacing
                outRect.bottom = spacing
            } else {
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                if (position >= spanCount) outRect.top = spacing
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Обновляем список при возвращении на экран
        binding.bottomBar.tabHome.isSelected = true
        binding.bottomBar.tabFav.isSelected = false
        binding.bottomBar.tabAdd.isSelected = false
        binding.bottomBar.tabSettings.isSelected = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Очищаем сохраненные данные при уничтожении активности
        lastModifiedRecipe = null
    }
}