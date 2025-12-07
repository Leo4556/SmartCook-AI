package com.example.smartcookai

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityMainBinding
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var recipeViewModel: RecipeViewModel

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
    }

    private fun setupRecyclerView() {
        // GridLayout с 2 колонками
        val layoutManager = GridLayoutManager(this, 2)
        binding.rvRecipes.layoutManager = layoutManager

        adapter = RecipeAdapter(emptyList(),
            onItemClick = { recipe ->
                // Открываем детали рецепта (заглушка)
                openRecipeDetails(recipe)
            },
            onFavoriteClick = { recipe ->
                // Переключаем избранное
                recipeViewModel.toggleFavorite(recipe)
            }
        )

        binding.rvRecipes.adapter = adapter

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing)
        binding.rvRecipes.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels, true))
    }

    private fun observeAllRecipes() {
        recipeViewModel.allRecipes.observe(this) { recipes ->
            adapter.updateList(recipes)

            if (recipes.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch(binding.etSearch.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: android.text.Editable?) {
                performSearch(s.toString())
            }
        })
    }

    private fun performSearch(query: String) {
        recipeViewModel.allRecipes.observe(this) { allRecipes ->
            val filtered = if (query.isBlank()) {
                allRecipes
            } else {
                allRecipes.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.ingredients.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true)
                }
            }
            adapter.updateList(filtered)
        }
    }

    private fun openRecipeDetails(recipe: RecipeEntity) {
        // Создаем Intent для открытия деталей рецепта
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Открыть детали: ${recipe.title}",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()

        // RecipeDetailActivity(позже):
        // val intent = Intent(this, RecipeDetailActivity::class.java).apply {
        //     putExtra("RECIPE_ID", recipe.id)
        // }
        // startActivity(intent)
    }

    private fun showEmptyState() {
        binding.rvRecipes.visibility = android.view.View.GONE
    }

    private fun hideEmptyState() {
        binding.rvRecipes.visibility = android.view.View.VISIBLE
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.setOnClickListener {
            recipeViewModel.allRecipes
            binding.bottomBar.tabHome.isSelected = true
            binding.bottomBar.tabFav.isSelected = false
            binding.bottomBar.tabAdd.isSelected = false
            binding.bottomBar.tabSettings.isSelected = false
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
        recipeViewModel.allRecipes
        binding.bottomBar.tabHome.isSelected = true
        binding.bottomBar.tabFav.isSelected = false
        binding.bottomBar.tabAdd.isSelected = false
        binding.bottomBar.tabSettings.isSelected = false
    }
}