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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smartcookai.data.AppDatabase
import com.example.smartcookai.utils.filter.QuickFilter
import com.example.smartcookai.data.RecipeEntity
import com.example.smartcookai.utils.filter.RecipeFilter
import com.example.smartcookai.data.RecipeRepository
import com.example.smartcookai.databinding.ActivityMainBinding
import com.example.smartcookai.utils.filter.FilterBottomSheet
import com.example.smartcookai.utils.filter.QuickFilterAdapter
import com.example.smartcookai.viewmodel.RecipeViewModel
import com.example.smartcookai.viewmodel.RecipeViewModelFactory
import com.google.android.material.snackbar.Snackbar

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: RecipeAdapter
    private lateinit var quickFilterAdapter: QuickFilterAdapter
    private lateinit var recipeViewModel: RecipeViewModel

    private var allRecipes: List<RecipeEntity> = emptyList()

    private var lastModifiedRecipe: RecipeEntity? = null
    private var wasFavorite: Boolean = false

    private var currentSearchQuery: String = ""
    private var currentFilter: RecipeFilter = RecipeFilter()
    private var activeQuickFilters = mutableSetOf<QuickFilter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getInstance(this)
        val repo = RecipeRepository(db.recipeDao())
        val factory = RecipeViewModelFactory(repo)
        recipeViewModel = ViewModelProvider(this, factory).get(RecipeViewModel::class.java)

        setupRecyclerView()
        setupQuickFilters()
        observeAllRecipes()
        setupSearch()
        setupBottomNavigation()

        binding.bottomBar.tabHome.isSelected = true
        binding.btnScan.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
        }
    }

    private fun setupQuickFilters() {
        quickFilterAdapter = QuickFilterAdapter(
            filters = QuickFilter.getAll(),
            onFilterClick = { filter ->
                toggleQuickFilter(filter)
            },
            onAllFiltersClick = {
                showFullFilterBottomSheet()
            }
        )

        binding.rvQuickFilters.apply {
            adapter = quickFilterAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
                this@MainActivity,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                false
            )

            addItemDecoration(object : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: android.view.View,
                    parent: androidx.recyclerview.widget.RecyclerView,
                    state: androidx.recyclerview.widget.RecyclerView.State
                ) {
                    outRect.right = 8
                }
            })
        }
    }

    private fun toggleQuickFilter(filter: QuickFilter) {
        if (activeQuickFilters.contains(filter)) {
            activeQuickFilters.remove(filter)
        } else {
            activeQuickFilters.add(filter)
        }
        applyAllFilters()
        showFilterAppliedHint(filter)
    }

    private fun showFilterAppliedHint(filter: QuickFilter) {
        if (activeQuickFilters.contains(filter)) {
            // Применяем фильтр к текущему списку (с учётом поиска)
            var tempRecipes = allRecipes

            // Сначала поиск
            if (currentSearchQuery.isNotBlank()) {
                tempRecipes = applySearchToRecipes(tempRecipes, currentSearchQuery)
            }

            // Потом фильтры
            val count = tempRecipes.count { filter.matches(it) }
            val message = "Фильтр «${filter.label}»: найдено $count рецептов"

            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .setAnchorView(binding.bottomBar.bottomBar)
                .show()
        }
    }

    private fun showFullFilterBottomSheet() {
        val filterSheet = FilterBottomSheet(
            currentFilter = currentFilter,
            onApplyFilter = { newFilter ->
                currentFilter = newFilter
                activeQuickFilters.clear()
                quickFilterAdapter.clearFilters()
                applyAllFilters()
            },
            onDismissWithoutChanges = {
                quickFilterAdapter.notifyItemChanged(0)
            }
        )
        filterSheet.show(supportFragmentManager, FilterBottomSheet.TAG)
    }

    private fun setupRecyclerView() {
        val layoutManager = GridLayoutManager(this, 2)
        binding.rvRecipes.layoutManager = layoutManager

        adapter = RecipeAdapter(
            emptyList(),
            onItemClick = { recipe, imageView ->
                openRecipeDetails(recipe, imageView)
            },
            onFavoriteClick = { recipe ->
                toggleFavoriteImmediately(recipe)
            }
        )

        binding.rvRecipes.adapter = adapter

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing)
        binding.rvRecipes.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels, true))
    }

    private fun openRecipeDetails(recipe: RecipeEntity, imageView: View) {
        val intent = Intent(this, RecipeDetailsActivity::class.java)
        intent.putExtra("recipe", recipe)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                android.util.Pair(imageView, "recipe_image_${recipe.id}"),
            )
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
    }

    private fun toggleFavoriteImmediately(recipe: RecipeEntity) {
        lastModifiedRecipe = recipe.copy()
        wasFavorite = recipe.isFavorite

        val updatedRecipe = recipe.copy(isFavorite = !recipe.isFavorite)
        recipeViewModel.updateRecipe(updatedRecipe)

        showUndoSnackbar(recipe.title, !recipe.isFavorite)
    }

    private fun showUndoSnackbar(recipeTitle: String, addedToFavorites: Boolean) {
        val message = if (addedToFavorites) {
            "Рецепт \"$recipeTitle\" добавлен в избранное"
        } else {
            "Рецепт \"$recipeTitle\" удален из избранного"
        }

        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
        snackbar.anchorView = binding.bottomBar.bottomBar

        snackbar.setAction("ОТМЕНИТЬ") {
            undoLastFavoriteChange()
        }

        snackbar.setActionTextColor(
            ContextCompat.getColor(this, R.color.colorAccentDark)
        )

        val snackbarView = snackbar.view
//        val textView =
//            snackbarView.findViewById<android.widget.TextView>(com.google.android.material.R.id.snackbar_text)
//        textView.maxLines = 3

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
            val restoredRecipe = recipe.copy(isFavorite = wasFavorite)
            recipeViewModel.updateRecipe(restoredRecipe)

            val undoConfirmationSnackbar = Snackbar.make(
                binding.root,
                "Действие отменено",
                Snackbar.LENGTH_SHORT
            )

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
            allRecipes = recipes
            applyAllFilters()
        }
    }

    private fun applyAllFilters() {
        var filteredRecipes = allRecipes

        if (currentSearchQuery.isNotBlank()) {
            filteredRecipes = applySearchToRecipes(filteredRecipes, currentSearchQuery)
        }

        if (activeQuickFilters.isNotEmpty()) {
            filteredRecipes = filteredRecipes.filter { recipe ->
                activeQuickFilters.all { filter ->
                    filter.matches(recipe)
                }
            }
        }

        if (currentFilter.isActive()) {
            filteredRecipes = filteredRecipes.filter { recipe ->
                currentFilter.matches(recipe)
            }
        }

        updateUI(filteredRecipes)
    }

    private fun applySearchToRecipes(
        recipes: List<RecipeEntity>,
        query: String
    ): List<RecipeEntity> {
        val normalizedQuery = query.lowercase().trim()

        return if (normalizedQuery.contains(",")) {
            // Поиск по ингредиентам через запятую
            val ingredientsQuery = normalizedQuery
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            recipes.filter { recipe ->
                val recipeIngredients = recipe.ingredients.lowercase()
                ingredientsQuery.all { ingredient ->
                    recipeIngredients.contains(ingredient)
                }
            }
        } else {
            // Обычный поиск по словам
            val words = normalizedQuery
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }

            recipes.filter { recipe ->
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
    }

    private fun updateUI(recipes: List<RecipeEntity>) {
        adapter.updateList(recipes)

        if (recipes.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString() ?: ""
                applyAllFilters()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showEmptyState() {
        binding.rvRecipes.visibility = android.view.View.GONE
        binding.tvEmptyAll.visibility = android.view.View.VISIBLE

        val hasQuickFilters = activeQuickFilters.isNotEmpty()
        val hasFullFilters = currentFilter.isActive()
        val hasSearch = currentSearchQuery.isNotEmpty()

        binding.tvEmptyAll.text = when {
            hasSearch && (hasQuickFilters || hasFullFilters) -> {
                val filterDesc = getActiveFiltersDescription()
                "По запросу \"$currentSearchQuery\"\nс фильтрами $filterDesc\nничего не найдено"
            }
            hasSearch ->
                "По запросу \"$currentSearchQuery\"\nничего не найдено"
            hasQuickFilters || hasFullFilters -> {
                val filterDesc = getActiveFiltersDescription()
                "Нет рецептов с фильтрами:\n$filterDesc"
            }
            else ->
                "Нет рецептов\nНажмите + чтобы добавить"
        }
    }

    private fun getActiveFiltersDescription(): String {
        val parts = mutableListOf<String>()

        if (activeQuickFilters.isNotEmpty()) {
            val labels = activeQuickFilters.joinToString(", ") {
                "${it.emoji} ${it.label}"
            }
            parts.add(labels)
        }

        if (currentFilter.isActive()) {
            parts.add(currentFilter.getActiveFiltersDescription())
        }

        return parts.joinToString(" • ")
    }

    private fun hideEmptyState() {
        binding.rvRecipes.visibility = android.view.View.VISIBLE
        binding.tvEmptyAll.visibility = android.view.View.GONE
    }

    private fun setupBottomNavigation() {
        binding.bottomBar.tabHome.isSelected = true

        binding.bottomBar.tabHome.setOnClickListener {
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
        binding.bottomBar.tabHome.isSelected = true
        binding.bottomBar.tabFav.isSelected = false
        binding.bottomBar.tabAdd.isSelected = false
        binding.bottomBar.tabSettings.isSelected = false
    }

    override fun onDestroy() {
        super.onDestroy()
        lastModifiedRecipe = null
    }
}